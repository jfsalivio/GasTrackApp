import { mkdir, writeFile, readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { createRequire } from 'node:module';

// Connect only to the debug WebView forwarded by ADB; never export session data.
const targets = await fetch('http://127.0.0.1:9222/json').then(response => response.json());
const target = targets.find(item => item.type === 'page' && item.url.includes('appassets.androidplatform.net'));
if (!target) throw new Error('No GasTrack WebView found. Forward its devtools socket to TCP 9222 first.');
const socket = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
let nextId = 0;
const pending = new Map();
const errors = [];
socket.onmessage = event => {
  const message = JSON.parse(event.data);
  if (message.id) {
    const request = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) request?.reject(new Error(message.error.message));
    else request?.resolve(message.result);
  }
  if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
};
function command(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, {resolve, reject});
    socket.send(JSON.stringify({id, method, params}));
  });
}
async function evaluate(expression) {
  const result = await command('Runtime.evaluate', {expression, returnByValue: true, awaitPromise: true, userGesture: true});
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
  return result.result.value;
}
const pause = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
await command('Runtime.enable');
await command('Page.enable');
if (process.argv.includes('--local-review-session') && !await evaluate("!!localStorage.getItem('token')")) {
  const require = createRequire(resolve('Backend/package.json'));
  require('dotenv').config({path: resolve('Backend/.env')});
  if (!['localhost', '127.0.0.1', '::1'].includes(process.env.DB_HOST) || !process.env.DB_NAME?.startsWith('gastrack_android_review_'))
    throw new Error('Temporary review sessions are restricted to the local snapshot review database.');
  const pool = require(resolve('Backend/config/db.js'));
  try {
    const [users] = await pool.query(`SELECT u.UserID AS userId FROM User u JOIN Role r ON r.RoleID=u.RoleID
      WHERE u.Status='Active' AND r.RoleName IN ('Admin','Administrator') LIMIT 1`);
    if (!users.length) throw new Error('No active review administrator found.');
    const token = require('jsonwebtoken').sign({userId: users[0].userId}, process.env.JWT_SECRET, {expiresIn: '30m'});
    await evaluate(`localStorage.setItem('token', ${JSON.stringify(token)}); location.reload();`);
    await pause(2500);
    console.log('Using a temporary local review session; password login is not tested.');
  } finally { await pool.end(); }
}
console.log(JSON.stringify(await evaluate(`({url: location.href, apiBase: window.__GASTRACK_API_BASE_URL__, sessionPresent: !!localStorage.getItem('token'), message: document.querySelector('[role="alert"]')?.textContent})`)));
const expressionFile = process.argv.indexOf('--evaluate');
if (expressionFile !== -1) {
  console.log(JSON.stringify(await evaluate(await readFile(process.argv[expressionFile + 1], 'utf8'))));
  socket.close();
  process.exit(0);
}
const output = resolve('android/review');
await mkdir(output, {recursive: true});
const modules = ['dashboard', 'pos', 'inventory', 'products', 'sales', 'restocking', 'orders', 'suppliers', 'report', 'data', 'users', 'settings'];
const results = [];
for (const module of modules) {
  await evaluate(`location.hash=${JSON.stringify('#/' + module)}`);
  await pause(1800);
  const result = await evaluate(`(() => ({
    module: ${JSON.stringify(module)},
    title: document.querySelector('.module-title')?.textContent,
    loginVisible: !!document.querySelector('input[type="password"]') && !document.querySelector('.sidebar'),
    pageWidth: innerWidth,
    documentWidth: document.documentElement.scrollWidth,
    imageCount: document.images.length,
    placeholderProducts: [...document.querySelectorAll('.product-image-img')].filter(image => image.src.startsWith('data:image/svg+xml')).length,
    brokenImages: [...document.images].filter(image => image.complete && !image.naturalWidth).map(image => image.getAttribute('src')),
    errors: [...document.querySelectorAll('[role="alert"], .error-message')].map(element => element.textContent),
    actions: [...document.querySelectorAll('main button')].slice(0, 14).map(button => button.textContent.trim())
  }))()`);
  results.push(result);
  console.log(JSON.stringify(result));
  const screenshot = await command('Page.captureScreenshot', {format: 'png'});
  await writeFile(resolve(output, `${module}.png`), Buffer.from(screenshot.data, 'base64'));
  if (result.loginVisible) break;
}
await writeFile(resolve(output, 'parity-check.json'), JSON.stringify({results, javascriptErrors: errors}, null, 2));
socket.close();
if (errors.length || results.some(result => result.loginVisible || result.brokenImages.length || result.documentWidth > result.pageWidth + 1)) process.exitCode = 1;

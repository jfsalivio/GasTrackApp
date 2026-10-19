// Restore a user-supplied snapshot into a NEW database. Never overwrites an existing DB.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {spawn} = require('node:child_process');
const {pipeline} = require('node:stream/promises');

async function main() {
  const source = path.resolve(process.argv[2]);
  const snapshot = path.join(source, 'db/snapshots/current.sql');
  const dotenv = require(path.join(source, 'node_modules/dotenv'));
  const mysql = require(path.join(source, 'node_modules/mysql2/promise'));
  const settings = dotenv.parse(fs.readFileSync(path.join(source, '.env')));
  if (!['localhost', '127.0.0.1', '::1'].includes(settings.DB_HOST)) throw new Error('Review restore requires a local database host.');
  const sql = fs.readFileSync(snapshot, 'utf8');
  if (/\b(?:USE\s+|CREATE\s+DATABASE|DROP\s+DATABASE)/i.test(sql) || sql.includes('`' + settings.DB_NAME + '`.')) throw new Error('Snapshot changes database scope; manual review required.');
  const database = 'gastrack_android_review_' + new Date().toISOString().replace(/\D/g, '').slice(0, 14);
  if (database === settings.DB_NAME) throw new Error('Refusing to use the source database.');
  const connection = await mysql.createConnection({host: settings.DB_HOST, port: settings.DB_PORT || 3306, user: settings.DB_USER, password: settings.DB_PASSWORD, ssl: {rejectUnauthorized: false}});
  try {
    // No IF NOT EXISTS: collisions fail before any snapshot SQL is executed.
    await connection.query('CREATE DATABASE `' + database + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');
    const executable = 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
    const child = spawn(executable, ['--host', settings.DB_HOST, '--port', settings.DB_PORT || '3306', '--user', settings.DB_USER, '--ssl-mode=REQUIRED', '--default-character-set=utf8mb4', database], {env: {...process.env, MYSQL_PWD: settings.DB_PASSWORD || ''}, stdio: ['pipe', 'ignore', 'pipe'], windowsHide: true});
    let errors = '';
    child.stderr.on('data', chunk => { errors += chunk; });
    const completed = new Promise((resolve, reject) => {
      child.on('error', reject);
      child.on('close', code => code === 0 ? resolve() : reject(new Error('Snapshot import failed: ' + errors.trim())));
    });
    await Promise.all([completed, pipeline(fs.createReadStream(snapshot), child.stdin)]);
    const [tables] = await connection.query('SHOW TABLES FROM `' + database + '`');
    if (!tables.length) throw new Error('Imported database contains no tables.');
    const backend = path.resolve(__dirname, '..');
    const destinationEnv = path.join(backend, '.env');
    if (fs.existsSync(destinationEnv)) throw new Error('Backend/.env already exists; refusing to overwrite connection settings.');
    const configured = {...settings, DB_NAME: database, JWT_SECRET: crypto.randomBytes(32).toString('hex'), PORT: '4000', ML_SERVICE_URL: 'http://127.0.0.1:5001', CORS_ORIGIN: 'http://localhost:5173'};
    fs.writeFileSync(destinationEnv, Object.entries(configured).map(([key, value]) => key + '=' + JSON.stringify(String(value))).join('\n') + '\n', {flag: 'wx'});
    fs.mkdirSync(path.join(backend, 'db/snapshots'), {recursive: true});
    fs.copyFileSync(snapshot, path.join(backend, 'db/snapshots/current.sql'));
    console.log(JSON.stringify({database, restoredTables: tables.length, configuration: 'Backend/.env', sourceDatabaseChanged: false}));
  } finally { await connection.end(); }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });

// Read-only API smoke check using a short-lived local review session.
// This does not test password login or write business records.
const path = require('node:path');
require('dotenv').config({path: path.join(__dirname, '../.env')});
const pool = require('../config/db');
const jwt = require('jsonwebtoken');

async function main() {
  const [users] = await pool.query(`SELECT u.UserID AS userId FROM User u
    JOIN Role r ON r.RoleID=u.RoleID WHERE u.Status='Active'
    AND r.RoleName IN ('Admin','Administrator') LIMIT 1`);
  if (!users.length) throw new Error('No active review administrator found.');
  const token = jwt.sign({userId: users[0].userId}, process.env.JWT_SECRET, {expiresIn: '5m'});
  for (const route of ['/auth/me', '/dashboard/summary', '/products', '/inventory',
    '/orders', '/deliveries', '/restocking', '/users?status=Active', '/users/activity-log?status=Active']) {
    const response = await fetch(`http://127.0.0.1:${process.env.PORT || 4000}${route}`, {
      headers: {Authorization: `Bearer ${token}`}, signal: AbortSignal.timeout(15000)
    });
    await response.text();
    console.log(`${response.status} ${route}`);
    if (!response.ok) process.exitCode = 1;
  }
}
main().catch(error => {console.error(error.message); process.exitCode = 1;})
  .finally(() => pool.end());

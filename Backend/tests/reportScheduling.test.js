const test = require('node:test');
const assert = require('node:assert/strict');
const writes = [];
function mock(module, exports) {
  const id = require.resolve(module);
  require.cache[id] = {id, filename: id, loaded: true, exports};
}
mock('../config/db', {query: async (sql, parameters) => {
  writes.push({sql, parameters});
  if (sql.includes('INSERT INTO ComplianceReport') && !parameters.reportId) throw new Error('ReportID has no default');
  return [{insertId: 0}];
}});
mock('../utils/entityIds', {nextEntityId: async (_, table) => {
  assert.equal(table, 'ComplianceReport');
  return 'REP-123';
}});
const router = require('../routes/reportRoutes');
const handler = router.stack.find(layer => layer.route?.path === '/' && layer.route.methods.post).route.stack.at(-1).handle;
function call(body) {
  return new Promise(resolve => {
    const response = {statusCode: 200, status(code) {this.statusCode = code; return this;}, json(value) {resolve({status: this.statusCode, body: value});}};
    handler({body}, response, error => resolve({status: error.statusCode || 500}));
  });
}
test('report scheduling supplies the schema-required text ID and returns that ID', async () => {
  writes.length = 0;
  const result = await call({reportName: 'Monthly review', reportType: 'Sales Summary', periodLabel: 'October', periodStart: '2026-10-01', periodEnd: '2026-10-31', dueDate: '2026-11-05'});
  assert.deepEqual(result, {status: 201, body: {id: 'REP-123'}});
  assert.match(writes[0].sql, /\(ReportID, ReportName/);
  assert.equal(writes[0].parameters.reportId, 'REP-123');
});
test('incomplete reports fail validation without writing a report', async () => {
  writes.length = 0;
  assert.equal((await call({reportName: 'Incomplete'})).status, 400);
  assert.equal(writes.length, 0);
});

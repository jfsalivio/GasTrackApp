import test from 'node:test';
import assert from 'node:assert/strict';

test('Android downloads preserve CSV bytes when the web helper immediately revokes the object URL', async () => {
  const create = URL.createObjectURL;
  const revoke = URL.revokeObjectURL;
  const previousFetch = globalThis.fetch;
  let save;
  const saved = new Promise(resolve => { save = resolve; });
  globalThis.window = {GasTrackAndroid: {saveFile: (name, type, base64) => save({name, type, base64})}, alert: message => assert.fail(message)};
  globalThis.document = {addEventListener() {}};
  globalThis.HTMLAnchorElement = class {
    hasAttribute(name) { return name === 'download'; }
    click() { assert.fail('Native downloads must use the Android bridge.'); }
  };
  globalThis.FileReader = class {
    readAsDataURL(blob) {
      blob.arrayBuffer().then(bytes => {
        this.result = 'data:' + blob.type + ';base64,' + Buffer.from(bytes).toString('base64');
        this.onload();
      });
    }
  };
  globalThis.fetch = () => { assert.fail('A revoked blob must not be fetched.'); };
  try {
    const {installAndroidPlatform} = await import('../src/mobilePlatform.js');
    installAndroidPlatform();
    const csv = 'ProductID,WarehouseID,Quantity,Mode\nP-001,WH-001,1,Set\n';
    const anchor = new HTMLAnchorElement();
    anchor.href = URL.createObjectURL(new Blob([csv], {type: 'text/csv'}));
    anchor.download = 'inventory_import_template.csv';
    anchor.click();
    URL.revokeObjectURL(anchor.href);
    const result = await saved;
    assert.equal(result.name, anchor.download);
    assert.equal(result.type, 'text/csv');
    assert.equal(Buffer.from(result.base64, 'base64').toString(), csv);
  } finally {
    URL.createObjectURL = create;
    URL.revokeObjectURL = revoke;
    globalThis.fetch = previousFetch;
    delete globalThis.window;
    delete globalThis.document;
    delete globalThis.HTMLAnchorElement;
    delete globalThis.FileReader;
  }
});

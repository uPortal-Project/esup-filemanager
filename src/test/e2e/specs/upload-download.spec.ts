import { test, expect } from '../fixtures/test';
import { drives } from '../support/native';
import { binary, text, names, manifest } from '../fixtures/data';
import { unzipSync } from 'fflate';
for (const drive of drives) {
    test(`${drive}: upload and download Unicode, binary and empty files @smoke`, async ({ files, workspace }) => {
        await workspace.prepare(drive, 'uploads', {});
        await files.open(drive, workspace.root + '/uploads');
        const entries = { [names[0]]: text, [names[1]]: Buffer.alloc(0), [names[2]]: binary };
        await files.upload(Object.entries(entries).map(([name, buffer]) => ({ name, buffer, mimeType: 'application/octet-stream' })));
        await files.refresh();
        expect(await workspace.snapshot(drive, 'uploads')).toEqual(manifest(entries));
        for (const [name, bytes] of Object.entries(entries)) {
            await files.select([name]);
            await files.download(name, bytes);
        }
    });
}
test('ZIP includes nested and empty directories', async ({ files, workspace }) => {
    const expected = await workspace.prepare('tmp', 'zip', { 'tree/empty/': null, 'tree/nested/hello.txt': text, 'binary.bin': binary, 'é 東京.txt': text });
    await files.open('tmp', workspace.root + '/zip');
    await files.select(['tree', 'binary.bin', 'é 東京.txt']);
    const archive = unzipSync(await files.download('selection.zip', undefined, true));
    expect(Object.keys(archive).sort()).toEqual(['binary.bin', 'tree/', 'tree/empty/', 'tree/nested/', 'tree/nested/hello.txt', 'e 東京.txt'].sort());
    expect(Buffer.from(archive['e 東京.txt'])).toEqual(text);
    expect(Buffer.from(archive['binary.bin'])).toEqual(binary);
    expect(Buffer.from(archive['tree/nested/hello.txt'])).toEqual(text);
    expect(await workspace.snapshot('tmp', 'zip')).toEqual(expected);
});
for (const drive of ['tmp', 's3'] as const) {
    for (const overwrite of [false, true])
        test(`${drive}: upload collision ${overwrite ? 'override' : 'cancel'}`, async ({ files, workspace }) => {
            await workspace.prepare(drive, 'collision', { 'hello.txt': text });
            await files.open(drive, workspace.root + '/collision');
            await files.page.locator('#upload-file-input').setInputFiles({ name: 'hello.txt', buffer: binary, mimeType: 'application/octet-stream' });
            await files.dialogButton(overwrite ? /Écraser|Remplacer|Overwrite/ : /Annuler|Cancel/).click();
            await expect.poll(() => workspace.snapshot(drive, 'collision')).toEqual(manifest({ 'hello.txt': overwrite ? binary : text }));
        });
}
test('upload size limit reports an error and preserves stored files', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'limits', {});
    await files.open('tmp', workspace.root + '/limits');
    const below = Buffer.alloc(1024 * 1024 - 1, 0x31), above = Buffer.alloc(1024 * 1024 + 1, 0x32);
    await files.upload([{ name: 'below.bin', buffer: below, mimeType: 'application/octet-stream' }]);
    const failed = files.page.waitForResponse(r => new URL(r.url()).pathname === '/uploadFile');
    await files.page.locator('#upload-file-input').setInputFiles({ name: 'above.bin', buffer: above, mimeType: 'application/octet-stream' });
    expect((await failed).status()).toBe(413);
    await expect(files.page.getByRole('alert').first()).toBeVisible();
    expect(await workspace.snapshot('tmp', 'limits')).toEqual(manifest({ 'below.bin': below }));
});
test('browser drop event uploads files', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'drop', {});
    await files.open('tmp', workspace.root + '/drop');
    // Dispatch a browser DataTransfer/drop event rather than calling application upload methods.
    const transfer = await files.page.evaluateHandle(bytes => {
        const transfer = new DataTransfer();
        transfer.items.add(new File([new Uint8Array(bytes)], 'dropped.bin', { type: 'application/octet-stream' }));
        return transfer;
    }, [...binary]);
    await files.page.locator('#browserArea').dispatchEvent('drop', { dataTransfer: transfer });
    await expect(files.listing.getByRole('checkbox', { name: 'dropped.bin', exact: true })).toBeVisible({ timeout: 60000 });
    expect(await workspace.snapshot('tmp', 'drop')).toEqual(manifest({ 'dropped.bin': binary }));
    await transfer.dispose();
});
test('denied upload reports a business failure', async ({ files, workspace }) => {
    const { native } = await import('../support/native');
    await workspace.prepare('tmp', 'denied-upload', {});
    await native('protect', 'tmp', workspace.root + '/denied-upload');
    await files.open('tmp', workspace.root + '/denied-upload');
    const pending = files.page.waitForResponse(r => new URL(r.url()).pathname === '/uploadFile');
    await files.page.locator('#upload-file-input').setInputFiles({ name: 'denied.txt', buffer: text, mimeType: 'text/plain' });
    const response = await pending;
    expect(response.status()).toBe(200);
    expect((await response.json()).success).toBeFalsy();
    await expect(files.page.getByRole('alert').first()).toBeVisible();
    expect(await workspace.snapshot('tmp', 'denied-upload')).toEqual({});
});

import { test, expect } from '../fixtures/test';
import { encoded } from '../pages/browser';
import { text, manifest } from '../fixtures/data';
for (const drive of ['tmp', 'webdav'] as const)
    test(`${drive}: hostile names stay inert inside the test directory`, async ({ files, workspace }) => {
        await workspace.prepare(drive, 'hostile', { 'keep.txt': text });
        await files.open(drive, workspace.root + '/hostile');
        const token = await files.page.locator('meta[name="_csrf"]').getAttribute('content');
        for (const title of ['..', '../escape', 'child/../../escape']) {
            const response = await files.page.request.post('/createFile', { form: {
                    parentDir: encoded(drive, workspace.root + '/hostile/'), title, type: 'folder', _csrf: token!,
                } });
            expect(response.status()).toBe(200);
            expect(Boolean((await response.json()).status), `Unsafe name accepted: ${title}`).toBeFalsy();
            expect(await workspace.snapshot(drive, 'hostile')).toEqual(manifest({ 'keep.txt': text }));
        }
        for (const suffix of ['../escape', '%2e%2e/escape', '..\\escape']) {
            const response = await files.page.request.post('/createFile', { form: {
                    parentDir: encoded(drive, workspace.root + '/hostile/' + suffix), title: 'outside', type: 'folder', _csrf: token!,
                } });
            expect(response.status()).toBe(403);
            expect(await workspace.snapshot(drive, 'hostile')).toEqual(manifest({ 'keep.txt': text }));
        }
    });
test('HTML in a filename is rendered as text without script execution', async ({ files, workspace }) => {
    const name = '<img src=x onerror=alert(1)>.txt';
    await workspace.prepare('tmp', 'html', { [name]: text });
    let dialog = false;
    files.page.on('dialog', async (event) => { dialog = true; await event.dismiss(); });
    await files.open('tmp', workspace.root + '/html');
    await files.select([name]);
    await expect(files.listing.getByRole('link', { name: 'Télécharger ' + name, exact: true })).toBeVisible();
    await expect(files.listing.locator('img[src="x"]')).toHaveCount(0);
    // Chromium sanitizes characters that cannot be used in filenames on every OS.
    await files.download(name.replace(/[<>]/g, '_'), text, false,
        () => files.listing.getByRole('link', { name: 'Télécharger ' + name, exact: true }).click());
    await expect(files.page.locator('#download-status')).toContainText(name);
    await expect(files.page.locator('#download-status img')).toHaveCount(0);
    expect(dialog).toBeFalsy();
});
test('mutations reject missing and invalid CSRF without changing storage', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'csrf', { 'keep.txt': text });
    await files.open('tmp', workspace.root + '/csrf');
    for (const token of [undefined, 'invalid']) {
        const response = await files.page.request.post('/removeFiles', { form: {
                dirs: encoded('tmp', workspace.root + '/csrf/keep.txt'), ...(token ? { _csrf: token } : {})
            } });
        expect(response.status()).toBe(403);
        expect(await workspace.snapshot('tmp', 'csrf')).toEqual(manifest({ 'keep.txt': text }));
    }
});

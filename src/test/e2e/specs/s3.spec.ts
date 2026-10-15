import { test, expect } from '../fixtures/test';
import { text, manifest } from '../fixtures/data';
const direct = process.env.S3_MODE !== 'classic';
test(`S3 ${direct ? 'presigned' : 'classic'} PUT and GET proven on network @smoke`, async ({ files, workspace }) => {
    await workspace.prepare('s3', 'network', {});
    await files.open('s3', workspace.root + '/network');
    const requests: {
        host: string;
        method: string;
        path: string;
        status: number;
    }[] = [];
    files.page.on('response', response => { const url = new URL(response.url()); requests.push({ host: url.host, method: response.request().method(), path: url.pathname, status: response.status() }); });
    await files.upload([{ name: 'network é + %.txt', buffer: text, mimeType: 'text/plain' }]);
    expect(await workspace.snapshot('s3', 'network')).toEqual(manifest({ 'network é + %.txt': text }));
    await files.select(['network é + %.txt']);
    await files.download('network é + %.txt', text);
    if (direct) {
        expect(requests.some(r => r.host === 's3:9000' && r.method === 'PUT' && r.status === 200)).toBeTruthy();
        expect(requests.some(r => r.host === 's3:9000' && r.method === 'GET' && r.status === 200)).toBeTruthy();
        expect(requests.some(r => r.path === '/uploadFile')).toBeFalsy();
        expect(requests.some(r => r.path === '/downloadFile' && r.status === 200)).toBeFalsy();
    }
    else {
        expect(requests.some(r => r.path === '/uploadFile' && r.status === 200)).toBeTruthy();
        expect(requests.some(r => r.path === '/downloadFile' && r.status === 200)).toBeTruthy();
        expect(requests.some(r => r.host === 's3:9000')).toBeFalsy();
    }
});
test(direct ? 'S3 targeted signing failure falls back to classic upload' : 'S3 classic upload avoids signing requests', async ({ files, workspace }) => {
    await workspace.prepare('s3', 'fallback', {});
    await files.open('s3', workspace.root + '/fallback');
    let signingRequests = 0;
    files.page.on('request', request => { if (new URL(request.url()).pathname === '/getPresignedUploadUrl')
        signingRequests++; });
    if (direct)
        await files.page.route('**/getPresignedUploadUrl*', route => route.fulfill({ status: 503, body: 'Deliberate signing failure' }));
    const fallback = files.page.waitForResponse(r => new URL(r.url()).pathname === '/uploadFile');
    await files.upload([{ name: 'fallback.txt', buffer: text, mimeType: 'text/plain' }]);
    expect((await fallback).ok()).toBeTruthy();
    expect(signingRequests).toBe(direct ? 1 : 0);
    expect(await workspace.snapshot('s3', 'fallback')).toEqual(manifest({ 'fallback.txt': text }));
});
test('S3 pagination includes more than 1000 objects', async ({ files, workspace }) => {
    const entries = Object.fromEntries(Array.from({ length: 1005 }, (_, i) => [`object-${String(i).padStart(4, '0')}.txt`, text]));
    await workspace.prepare('s3', 'pagination', entries);
    await files.open('s3', workspace.root + '/pagination');
    await expect(files.listing.getByRole('checkbox')).toHaveCount(1005);
    expect(await workspace.snapshot('s3', 'pagination')).toEqual(manifest(entries));
});
test('S3 multipart transfer preserves all bytes', async ({ files, workspace }) => {
    const buffer = Buffer.alloc(11 * 1024 * 1024, 0x61);
    await workspace.prepare('tmp', 'multipart-source', { 'multipart.bin': buffer });
    await workspace.prepare('s3', 'multipart', {});
    await files.open('tmp', workspace.root + '/multipart-source');
    await files.select(['multipart.bin']);
    await files.clipboard('copy');
    await files.navigate('s3', workspace.root + '/multipart');
    await files.paste();
    expect(await workspace.snapshot('tmp', 'multipart-source')).toEqual(manifest({ 'multipart.bin': buffer }));
    await files.select(['multipart.bin']);
    await files.download('multipart.bin', buffer);
    expect(await workspace.snapshot('s3', 'multipart')).toEqual(manifest({ 'multipart.bin': buffer }));
});

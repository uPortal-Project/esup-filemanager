import { test, expect } from '../fixtures/test';
import { text, binary, manifest } from '../fixtures/data';
import { native, drives, Entries } from '../support/native';
import { encoded, FileBrowser } from '../pages/browser';
import { login } from '../pages/auth';
for (const drive of drives)
    test(`${drive}: native file collision cut has an explicit policy`, async ({ files, workspace }) => {
        await workspace.prepare(drive, 'native-source', { 'same.txt': text });
        await workspace.prepare(drive, 'native-destination', { 'same.txt': binary });
        await files.open(drive, workspace.root + '/native-source');
        await files.select(['same.txt']);
        await files.clipboard('cut');
        await files.navigate(drive, workspace.root + '/native-destination');
        await files.paste(false);
        expect(await workspace.snapshot(drive, 'native-destination')).toEqual(manifest({ 'same.txt': binary }));
        expect(await workspace.snapshot(drive, 'native-source')).toEqual(manifest({ 'same.txt': text }));
    });
for (const sourceType of ['file', 'folder'] as const)
    for (const targetType of ['file', 'folder'] as const)
        test(`inter-mount collision ${sourceType}/${targetType} preserves both contents`, async ({ files, workspace }) => {
            const source: Entries = sourceType === 'file' ? { 'same': text } : { 'same/child.txt': text };
            const destination: Entries = targetType === 'file' ? { 'same': binary } : { 'same/child.txt': binary };
            const beforeSource = await workspace.prepare('tmp', 'typed-source', source);
            const beforeDestination = await workspace.prepare('webdav', 'typed-destination', destination);
            await files.open('tmp', workspace.root + '/typed-source');
            await files.select(['same']);
            await files.clipboard('cut');
            await files.navigate('webdav', workspace.root + '/typed-destination');
            await files.paste(false);
            expect(await workspace.snapshot('tmp', 'typed-source')).toEqual(beforeSource);
            expect(await workspace.snapshot('webdav', 'typed-destination')).toEqual(beforeDestination);
        });
for (const target of ['current', 'self', 'descendant'] as const)
    test(`refuse clipboard paste into ${target}`, async ({ files, workspace }) => {
        const expected = await workspace.prepare('tmp', 'recursive', { 'tree/child/': null, 'tree/hello.txt': text });
        await files.open('tmp', workspace.root + '/recursive');
        await files.select(['tree']);
        await files.clipboard('copy');
        const destination = workspace.root + '/recursive' + (target === 'self' ? '/tree' : target === 'descendant' ? '/tree/child' : '');
        if (target !== 'current')
            await files.navigate('tmp', destination);
        await files.paste(false);
        expect(await workspace.snapshot('tmp', 'recursive')).toEqual(expected);
    });
test('deleted clipboard source reports failure and preserves destination', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'source', { 'gone/hello.txt': text });
    await workspace.prepare('s3', 'destination', {});
    await files.open('tmp', workspace.root + '/source');
    await files.select(['gone']);
    await files.clipboard('cut');
    await native('remove', 'tmp', workspace.root + '/source/gone');
    await files.navigate('s3', workspace.root + '/destination');
    await files.paste(false);
    expect(await workspace.snapshot('s3', 'destination')).toEqual({});
});
test('tabs share BasketSession and independent logins isolate it', async ({ files, workspace, browser }, info) => {
    await workspace.prepare('tmp', 'source', { 'hello.txt': text });
    await workspace.prepare('tmp', 'same-session', {});
    await workspace.prepare('tmp', 'other-session', {});
    await files.open('tmp', workspace.root + '/source');
    await files.select(['hello.txt']);
    await files.clipboard('copy');
    const tab = await files.page.context().newPage();
    const second = new FileBrowser(tab, info);
    await second.open('tmp', workspace.root + '/same-session');
    const csrf = await tab.locator('meta[name="_csrf"]').getAttribute('content');
    // A second tab has no local clipboard; this authenticated request characterizes the server basket.
    const shared = await tab.request.post('/pastFiles', { form: { dir: encoded('tmp', workspace.root + '/same-session/'), _csrf: csrf! } });
    expect(shared.ok()).toBeTruthy();
    expect(Boolean((await shared.json()).status)).toBeTruthy();
    expect(await workspace.snapshot('tmp', 'same-session')).toEqual(manifest({ 'hello.txt': text }));
    const context = await browser.newContext({ baseURL: 'http://app-e2e:8080' });
    try {
        const independent = await context.newPage();
        await login(independent);
        await independent.goto('/?dir=' + encoded('tmp', workspace.root + '/other-session/'));
        const token = await independent.locator('meta[name="_csrf"]').getAttribute('content');
        const isolated = await independent.request.post('/pastFiles', { form: { dir: encoded('tmp', workspace.root + '/other-session/'), _csrf: token! } });
        expect(isolated.ok()).toBeTruthy();
        expect(Boolean((await isolated.json()).status)).toBeFalsy();
        expect(await workspace.snapshot('tmp', 'other-session')).toEqual({});
    }
    finally {
        await context.close();
    }
});
test('write-protected destination preserves a failed move source', async ({ files, workspace }) => {
    const expected = await workspace.prepare('s3', 'protected-source', { 'hello.txt': text });
    await workspace.prepare('tmp', 'protected-destination', {});
    await native('protect', 'tmp', workspace.root + '/protected-destination');
    await files.open('s3', workspace.root + '/protected-source');
    await files.select(['hello.txt']);
    await files.clipboard('cut');
    await files.navigate('tmp', workspace.root + '/protected-destination');
    await files.paste(false);
    expect(await workspace.snapshot('s3', 'protected-source')).toEqual(expected);
    expect(await workspace.snapshot('tmp', 'protected-destination')).toEqual({});
});
test('destination removed after navigation rejects a cut and preserves its source', async ({ files, workspace }) => {
    const expected = await workspace.prepare('s3', 'source', { 'keep.txt': text });
    await workspace.prepare('tmp', 'parent/destination', {});
    await files.open('s3', workspace.root + '/source');
    await files.select(['keep.txt']);
    await files.clipboard('cut');
    await files.navigate('tmp', workspace.root + '/parent/destination');
    await native('remove', 'tmp', workspace.root + '/parent/destination');
    await files.paste(false);
    expect(await workspace.snapshot('s3', 'source')).toEqual(expected);
    expect(await workspace.snapshot('tmp', 'parent')).toEqual({});
});
test('new copy replaces clipboard and successful paste clears UI clipboard', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'source', { 'a.txt': text, 'b.txt': Buffer.from('B') });
    await workspace.prepare('tmp', 'destination', {});
    await files.open('tmp', workspace.root + '/source');
    await files.select(['a.txt']);
    await files.clipboard('copy');
    await files.select(['b.txt']);
    await files.clipboard('copy');
    await files.navigate('tmp', workspace.root + '/destination');
    await files.paste();
    expect(await workspace.snapshot('tmp', 'destination')).toEqual(manifest({ 'b.txt': Buffer.from('B') }));
    await expect(files.page.locator('#toolbar-paste')).toHaveAttribute('aria-disabled', 'true');
});
for (const entry of ['keyboard', 'context'] as const)
    test(`clipboard entry point: ${entry}`, async ({ files, workspace }) => {
        await workspace.prepare('tmp', 'source', { 'hello.txt': text });
        await workspace.prepare('tmp', 'destination', {});
        await files.open('tmp', workspace.root + '/source');
        await files.select(['hello.txt']);
        await files.clipboard('copy', entry);
        await files.navigate('tmp', workspace.root + '/destination');
        await files.paste();
        expect(await workspace.snapshot('tmp', 'destination')).toEqual(manifest({ 'hello.txt': text }));
    });

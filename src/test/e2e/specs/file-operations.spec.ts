import { test, expect } from '../fixtures/test';
import { drives } from '../support/native';
import { text, binary, manifest } from '../fixtures/data';
for (const drive of drives)
    test(`${drive}: create, rename, cancel and delete @smoke`, async ({ files, workspace }) => {
        await workspace.prepare(drive, 'crud', { 'keep.txt': text, 'nested/child.txt': text });
        await files.open(drive, workspace.root + '/crud');
        await files.create('cancelled', true);
        expect(await workspace.snapshot(drive, 'crud')).toEqual(manifest({ 'keep.txt': text, 'nested/child.txt': text }));
        await files.create('new é 東京');
        await files.select(['new é 東京']);
        await files.rename('renamed');
        await files.refresh();
        expect(await workspace.snapshot(drive, 'crud')).toEqual(manifest({ 'keep.txt': text, 'nested/child.txt': text, 'renamed/': null }));
        await files.select(['keep.txt']);
        await files.rename('cancelled.txt', true);
        await files.rename('new 東京.txt');
        expect(await workspace.snapshot(drive, 'crud')).toEqual(manifest({
            'new 東京.txt': text, 'nested/child.txt': text, 'renamed/': null,
        }));
        await files.select(['renamed']);
        await files.remove(true);
        expect(await workspace.snapshot(drive, 'crud')).toHaveProperty('renamed/');
        await files.select(['new 東京.txt', 'renamed', 'nested']);
        await files.remove();
        await files.refresh();
        expect(await workspace.snapshot(drive, 'crud')).toEqual({});
        await files.open(drive, workspace.root + '/crud');
        await expect(files.listing.getByRole('checkbox')).toHaveCount(0);
    });
for (const drive of ['tmp', 's3'] as const)
    test(`${drive}: creation and rename collisions preserve data, nested deletion is exact`, async ({ files, workspace }) => {
        const entries = { 'existing/keep.txt': text, 'first.txt': text, 'second.txt': binary };
        const original = await workspace.prepare(drive, 'boundaries', entries);
        await files.open(drive, workspace.root + '/boundaries');
        await files.action('new_folder');
        await files.page.locator('#newFolderName').fill('existing');
        await files.business('createFile', () => files.dialogButton(/Créer|Create/).click(), false);
        expect(await workspace.snapshot(drive, 'boundaries')).toEqual(original);
        await files.select(['first.txt']);
        await files.action('rename');
        await files.page.locator('#newFileName').fill('second.txt');
        await files.business('renameFile', () => files.dialogButton(/Renommer|Rename/).click(), false);
        expect(await workspace.snapshot(drive, 'boundaries')).toEqual(original);
        await files.open(drive, workspace.root + '/boundaries/existing');
        await files.create('nested');
        expect(await workspace.snapshot(drive, 'boundaries')).toEqual(manifest({ ...entries, 'existing/nested/': null }));
        await files.open(drive, workspace.root + '/boundaries');
        await files.select(['existing']);
        await files.remove();
        await files.refresh();
        expect(await workspace.snapshot(drive, 'boundaries')).toEqual(manifest({ 'first.txt': text, 'second.txt': binary }));
    });

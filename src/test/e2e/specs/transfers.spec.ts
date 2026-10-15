import { test, expect } from '../fixtures/test';
import { drives, Drive } from '../support/native';
import { transferEntries } from '../fixtures/data';
// Exercise native operations on every connector, then the common transfer path
// with every connector as source and destination. tmp is the shared reference.
const pairs: [
    Drive,
    Drive
][] = drives.map(drive => [drive, drive]);
for (const drive of drives) {
    if (drive !== 'tmp')
        pairs.push(['tmp', drive], [drive, 'tmp']);
}
for (const [source, destination] of pairs) {
    for (const operation of ['copy', 'cut'] as const) {
        const smoke = ['tmp', 's3'].includes(source) && ['tmp', 's3'].includes(destination)
            && !(source === 's3' && destination === 's3');
        test(`${source} → ${destination}: ${operation}${smoke ? ' @smoke' : ''}`, async ({ files, workspace }) => {
            const expected = await workspace.prepare(source, 'source', transferEntries);
            await workspace.prepare(destination, 'destination', {});
            const names = [...new Set(Object.keys(transferEntries).map(name => name.split('/')[0]))];
            await files.open(source, workspace.root + '/source');
            await files.select(names);
            await files.clipboard(operation);
            await files.navigate(destination, workspace.root + '/destination');
            await files.paste();
            await files.refresh();
            for (const name of names) {
                await expect(files.listing.getByRole('checkbox', { name, exact: true })).toBeVisible();
            }
            expect(await workspace.snapshot(destination, 'destination')).toEqual(expected);
            expect(await workspace.snapshot(source, 'source')).toEqual(operation === 'copy' ? expected : {});
        });
    }
}

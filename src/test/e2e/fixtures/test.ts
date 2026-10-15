import { test as base, expect } from '@playwright/test';
import { createHash } from 'node:crypto';
import { FileBrowser } from '../pages/browser';
import { login } from '../pages/auth';
import { Drive, Entries, native, Snapshot } from '../support/native';
import { manifest } from './data';
type Workspace = {
    root: string;
    prepare: (drive: Drive, suffix: string, entries: Entries) => Promise<Snapshot>;
    snapshot: (drive: Drive, suffix: string) => Promise<Snapshot>;
};
export const test = base.extend<{
    files: FileBrowser;
    workspace: Workspace;
}>({
    files: async ({ page }, use, info) => {
        // A separate application session keeps each test's clipboard isolated.
        await login(page);
        await use(new FileBrowser(page, info));
    },
    workspace: async ({}, use, info) => {
        const run = process.env.E2E_RUN || 'local';
        const id = createHash('sha256').update(info.testId).digest('hex').slice(0, 16);
        const root = `e2e/${run}/${id}/${info.retry}`;
        await use({
            root,
            prepare: async (drive, suffix, entries) => {
                const expected = manifest(entries);
                const observed = await native<Snapshot>('prepare', drive, root + '/' + suffix, entries);
                expect(observed, 'Test data must be ready before exercising the application').toEqual(expected);
                return expected;
            },
            snapshot: (drive, suffix) => native<Snapshot>('snapshot', drive, root + '/' + suffix),
        });
        // The wrapper removes this run's volumes, or retains them with --keep.
    },
});
export { expect };

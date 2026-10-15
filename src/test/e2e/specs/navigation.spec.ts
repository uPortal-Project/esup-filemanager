import { test, expect } from '../fixtures/test';
import { text } from '../fixtures/data';
test('navigation, sort, parent and selection @smoke', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'navigation', { 'child/': null, 'a.txt': text, 'z.txt': text });
    await files.open('tmp', workspace.root + '/navigation');
    await files.listing.getByRole('link', { name: 'child', exact: true }).click();
    await expect(files.page.locator('#bigdirectory')).toHaveText('child');
    await files.listing.locator('a.fileTreeRefCrumbs').filter({ hasText: 'navigation' }).click();
    await files.select(['a.txt', 'z.txt']);
    await expect(files.listing.getByRole('checkbox', { checked: true })).toHaveCount(2);
    await files.refresh();
    const sort = files.listing.locator('a.sortTable').first();
    await sort.click();
    await expect(files.listing.getByRole('columnheader').first()).toHaveAttribute('aria-sort', 'descending');
});
test('switching language preserves file names', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'language', { 'é 東京.txt': text });
    await files.open('tmp', workspace.root + '/language');
    await files.page.locator('button[data-lang="en"]').filter({ visible: true }).click();
    await expect(files.listing.getByRole('checkbox', { name: 'é 東京.txt', exact: true })).toBeVisible();
    await expect(files.page.locator('html')).toHaveAttribute('lang', /^en/);
});

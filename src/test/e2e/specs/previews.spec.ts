import { test, expect } from '../fixtures/test';
import { media, png } from '../fixtures/data';
for (const drive of ['tmp', 's3'] as const)
    test(`${drive}: image, audio and video previews`, async ({ files, workspace }) => {
        await workspace.prepare(drive, 'previews', media());
        await files.open(drive, workspace.root + '/previews');
        await files.select(['pixel.png']);
        const image = files.page.locator('#detailArea img').filter({ visible: true }).last();
        await expect(image).toBeVisible();
        await expect.poll(() => image.evaluate((element: HTMLImageElement) => element.complete && element.naturalWidth > 0)).toBeTruthy();
        await files.select(['tone.mp3']);
        await expect(files.page.locator('#detailArea audio')).toBeVisible();
        await expect.poll(() => files.page.locator('#detailArea audio').evaluate((element: HTMLMediaElement) => element.readyState)).toBeGreaterThanOrEqual(1);
        await files.select(['clip.webm']);
        await expect(files.page.locator('#detailArea video')).toBeVisible();
        await expect.poll(() => files.page.locator('#detailArea video').evaluate((element: HTMLMediaElement) => element.readyState)).toBeGreaterThanOrEqual(1);
        await files.select(['pixel.png']);
        await expect(files.page.locator('#detailArea video')).toHaveCount(0);
    });
test('oversized image disables preview while preserving download', async ({ files, workspace }) => {
    const bytes = Buffer.concat([png, Buffer.alloc(10 * 1024 * 1024)]);
    await workspace.prepare('tmp', 'preview-limit', { 'large.png': bytes });
    await files.open('tmp', workspace.root + '/preview-limit');
    await files.select(['large.png']);
    await expect(files.page.locator('#detailArea')).toContainText('large.png');
    await expect(files.page.locator('#detailArea .details-image')).toHaveCount(0);
    await files.download('large.png', bytes);
});

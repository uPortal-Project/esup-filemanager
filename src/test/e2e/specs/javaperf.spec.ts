import { test, expect } from '../fixtures/test';
import { checkJavaPerf } from '../pages/javaperf';
test('JavaPerf displays metrics and fits a narrow viewport', async ({ files }) => {
    await files.open('s3');
    await checkJavaPerf(files.page);
    expect(await files.page.getByTestId('storage-card').count()).toBeGreaterThan(0);
    await files.page.setViewportSize({ width: 390, height: 844 });
    await files.page.evaluate(() => document.fonts.ready.then(() => true));
    await expect.poll(() => files.page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
});

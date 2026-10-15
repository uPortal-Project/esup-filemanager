import { expect, Page } from '@playwright/test';
export async function checkJavaPerf(page: Page) {
    await page.goto('/javaperf');
    await expect(page.locator('#javaperf-main')).toBeVisible();
    const values: number[] = [];
    const french = (await page.locator('html').getAttribute('lang')) === 'fr';
    for (const key of ['max', 'total', 'free', 'used']) {
        const label = await page.getByTestId('memory-' + key).innerText();
        expect(label).toMatch(french ? / Mo$/ : / MB$/);
        const value = Number(label.replace(/[^0-9]/g, ''));
        expect(Number.isFinite(value)).toBeTruthy();
        expect(value).toBeGreaterThanOrEqual(0);
        values.push(value);
    }
    const [max, total, free, used] = values;
    expect(free).toBeLessThanOrEqual(total);
    expect(total).toBeLessThanOrEqual(max);
    expect(used).toBe(total - free);
    const progress = Number((await page.getByRole('progressbar').getAttribute('aria-valuenow'))!.replace(',', '.'));
    expect(progress).toBeGreaterThanOrEqual(0);
    expect(progress).toBeLessThanOrEqual(100);
    expect(Math.abs(progress - used / max * 100)).toBeLessThanOrEqual(0.11);
    const rows = page.getByTestId('thread-table').locator('tbody tr');
    expect(await rows.count()).toBeGreaterThan(0);
    for (const row of await rows.all())
        await expect(row.locator('td').nth(2)).toContainText(/NEW|RUNNABLE|BLOCKED|WAITING|TIMED_WAITING|TERMINATED/);
    await expect(page.locator('.row-current')).toHaveCount(1);
    for (const card of await page.getByTestId('storage-card').all()) {
        const active = Number(await card.getAttribute('data-active'));
        const peak = Number(await card.getAttribute('data-peak'));
        expect(active).toBeGreaterThanOrEqual(0);
        expect(peak).toBeGreaterThanOrEqual(active);
    }
}

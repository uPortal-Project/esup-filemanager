import { expect, Page } from '@playwright/test';
export async function login(page: Page, user = 'joe', password = 'pass', target = '/') {
    await page.goto(target);
    await expect(page).toHaveURL(/http:\/\/cas:8080\/cas\/login/);
    await page.locator('input[name="username"]').fill(user);
    await page.locator('input[name="password"]').fill(password);
    await page.locator('button[name="submit"], input[name="submit"], button[type="submit"]').first().click();
    await expect(page).toHaveURL(/http:\/\/app-e2e:8080\//);
    await expect(page.locator('#javaperf-main, #browserMain')).toBeVisible();
}
export async function logout(page: Page) {
    if (!await page.locator('a[href*="logout"]').filter({ visible: true }).count()) {
        await page.getByRole('button', { name: 'Toggle navigation', exact: true }).click();
    }
    await page.locator('a[href*="logout"]').filter({ visible: true }).first().click();
    await expect(page).toHaveURL(/cas/);
}

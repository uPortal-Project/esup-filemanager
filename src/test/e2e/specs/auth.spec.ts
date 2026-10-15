import { test, expect } from '../fixtures/test';
import { login, logout } from '../pages/auth';
import { encoded } from '../pages/browser';
test('jack cannot see tmp or access JavaPerf @smoke', async ({ page }) => {
    await login(page, 'jack');
    await page.goto('/?dir=path_' + Buffer.from('FS:Stockage').toString('base64url') + '#path=path_' + Buffer.from('FS:Stockage').toString('base64url'));
    await expect(page.locator('#browserMain').getByRole('link', { name: 's3', exact: true })).toBeVisible();
    await expect(page.locator('#browserMain').getByRole('link', { name: 'tmp', exact: true })).toHaveCount(0);
    const denied = await page.request.get('/javaperf');
    expect(denied.status()).toBe(403);
    const token = await page.locator('meta[name="_csrf"]').getAttribute('content');
    const response = await page.request.post('/htmlFileTree', { form: { dir: encoded('tmp'), _csrf: token! } });
    expect(response.status()).toBe(403);
    expect(await response.text()).not.toContain('id="bigdirectory"');
});
test('logout invalidates dedicated application session', async ({ page }) => {
    await login(page);
    await logout(page);
    await page.goto('/');
    await expect(page).toHaveURL(/cas:8080\/cas\/login/);
});
test('anonymous application access requires authentication', async ({ page }) => {
    const response = await page.request.get('/javaperf', { maxRedirects: 0 });
    expect(response.status()).toBe(302);
    expect(response.headers().location).toContain('/cas/login');
});
test('secondary authentication rejects bad password then accepts correction', async ({ files }) => {
    await files.open('webdav');
    const token = await files.page.locator('meta[name="_csrf"]').getAttribute('content');
    const directory = await files.page.locator('#bigdirectory').getAttribute('rel');
    const rejected = await files.page.request.post('/authenticate', { form: { dir: directory!, username: 'webdav', password: 'wrong', _csrf: token! } });
    expect(rejected.status()).toBe(401);
    await files.page.reload();
    await expect(files.page.locator('#authenticationForm')).toBeVisible();
    await files.page.locator('#username').fill('webdav');
    await files.page.locator('#password').fill('wrong');
    const failed = files.page.waitForResponse(response => new URL(response.url()).pathname === '/authenticate');
    await files.page.locator('#authenticate').click();
    expect((await failed).status()).toBe(401);
    await expect(files.page.locator('#auth-error-message')).toBeVisible();
    await files.page.locator('#password').fill('webdav');
    await files.business('authenticate', () => files.page.locator('#authenticate').click());
    await expect(files.page.locator('#bigdirectory')).toBeVisible();
});

import { expect, Page, TestInfo } from '@playwright/test';
import fs from 'node:fs/promises';
import { Drive } from '../support/native';
import { hash } from '../fixtures/data';
export const encoded = (drive: Drive, relative = '') => 'path_' + Buffer.from(`FS:Stockage~${drive}${relative ? '~' + relative : ''}`).toString('base64url');
const credentials: Record<Drive, [
    string,
    string
]> = { tmp: ['', ''], s3: ['minioadmin', 'minioadmin'],
    samba: ['sambauser', 'sambapass'], webdav: ['webdav', 'webdav'], sftp: ['sftpuser', 'sftppass'] };
export class FileBrowser {
    constructor(readonly page: Page, readonly info: TestInfo) { }
    get listing() { return this.page.locator('#browserMain'); }
    async open(drive: Drive, relative = '') {
        const target = encoded(drive, relative ? relative.replace(/\/$/, '') + (drive === 'webdav' ? '/' : '') : '');
        await this.page.goto('/#path=' + target);
        // A fragment change alone does not reload the application or its stored directory.
        await this.page.reload();
        await this.authenticateIfNeeded(drive);
        await expect(this.page.locator('#bigdirectory')).toHaveAttribute('rel', target);
        await expect(this.listing).toHaveAttribute('aria-busy', 'false');
    }
    async navigate(drive: Drive, relative = '') {
        const target = encoded(drive, relative ? relative.replace(/\/$/, '') + (drive === 'webdav' ? '/' : '') : '');
        await this.listing.locator('a.fileTreeRefCrumbs').first().click();
        await expect(this.listing).toHaveAttribute('aria-busy', 'false');
        await this.listing.getByRole('link', { name: 'Stockage', exact: true }).click();
        await expect(this.listing).toHaveAttribute('aria-busy', 'false');
        await this.listing.getByRole('link', { name: drive, exact: true }).click();
        await this.authenticateIfNeeded(drive);
        await expect(this.listing).toHaveAttribute('aria-busy', 'false');
        for (const part of relative.split('/').filter(Boolean)) {
            await this.listing.getByRole('link', { name: part, exact: true }).click();
            await expect(this.listing).toHaveAttribute('aria-busy', 'false');
        }
        await expect(this.page.locator('#bigdirectory')).toHaveAttribute('rel', target);
        await expect(this.listing).toHaveAttribute('aria-busy', 'false');
    }
    async authenticateIfNeeded(drive: Drive) {
        await expect(this.page.locator('#bigdirectory, #authenticationForm')).toBeAttached();
        if (await this.page.locator('#authenticationForm').isVisible()) {
            await this.page.locator('#username').fill(credentials[drive][0]);
            await this.page.locator('#password').fill(credentials[drive][1]);
            await this.business('authenticate', () => this.page.locator('#authenticate').click());
        }
    }
    async action(name: string) {
        const toolbar = this.page.locator('#toolbar-' + name);
        if (await toolbar.isVisible())
            await toolbar.click();
        else {
            const menu = this.page.getByRole('button', { name: 'Toggle navigation', exact: true });
            const action = this.page.locator('#mobile-' + ({ new_folder: 'new-folder' }[name] || name));
            await menu.click();
            await action.click();
            await expect(this.page.locator('#mobileMenu')).not.toBeVisible();
        }
    }
    async business(endpoint: string, trigger: () => Promise<unknown>, success = true) {
        const responsePromise = this.page.waitForResponse(response => new URL(response.url()).pathname === '/' + endpoint && response.request().method() === 'POST');
        const [response] = await Promise.all([responsePromise, trigger()]);
        expect(response.ok(), endpoint + ': HTTP failure').toBeTruthy();
        const result = await response.json();
        expect(Boolean(result.status), `${endpoint}: ${JSON.stringify(result)}`).toBe(success);
        if (!success)
            await expect(this.page.getByRole('alert').first()).toBeVisible();
        return result;
    }
    async select(names: string[]) {
        await expect(this.listing).toHaveAttribute('aria-busy', 'false');
        for (const checkbox of await this.listing.getByRole('checkbox').all())
            await checkbox.uncheck();
        for (const name of names)
            await this.listing.getByRole('checkbox', { name, exact: true }).check();
    }
    async clipboard(operation: 'copy' | 'cut', entry: 'toolbar' | 'keyboard' | 'context' = 'toolbar') {
        await this.business(operation === 'copy' ? 'prepareCopyFiles' : 'prepareCutFiles', async () => {
            if (entry === 'toolbar')
                await this.action(operation);
            else if (entry === 'keyboard') {
                await this.page.locator('body').click({ position: { x: 1, y: 1 } });
                await this.page.keyboard.press(operation === 'copy' ? 'Control+c' : 'Control+x');
            }
            else {
                await this.listing.getByRole('checkbox').filter({ visible: true }).first().click({ button: 'right' });
                await this.page.locator('.context-menu').getByText(operation === 'copy' ? /Copier|Copy/ : /Couper|Cut/, { exact: true }).click();
            }
        });
    }
    async paste(success = true) { await this.business('pastFiles', () => this.action('paste'), success); }
    async refresh() {
        const response = this.page.waitForResponse(r => r.url().endsWith('/htmlFileTree'));
        await this.action('refresh');
        expect((await response).ok()).toBeTruthy();
        await expect(this.page.locator('#bigdirectory')).toBeVisible();
    }
    async create(name: string, cancel = false) {
        await this.action('new_folder');
        await this.page.locator('#newFolderName').fill(name);
        if (cancel)
            await this.dialogButton(/Non|Annuler|Cancel/).click();
        else
            await this.business('createFile', () => this.dialogButton(/Créer|Create/).click());
    }
    dialogButton(name: RegExp) {
        const exactName = new RegExp(`^(?:${name.source})$`, name.flags);
        return this.page.getByRole('dialog').getByRole('button', { name: exactName });
    }
    async rename(name: string, cancel = false) {
        await this.action('rename');
        await this.page.locator('#newFileName').fill(name);
        if (cancel)
            await this.dialogButton(/Non|Annuler|Cancel/).click();
        else
            await this.business('renameFile', () => this.dialogButton(/Renommer|Rename/).click());
    }
    async remove(cancel = false) {
        await this.action('delete');
        if (cancel)
            await this.dialogButton(/Non|Annuler|Cancel/).click();
        else
            await this.business('removeFiles', () => this.dialogButton(/Supprimer|Delete/).click());
    }
    async upload(files: {
        name: string;
        buffer: Buffer;
        mimeType: string;
    }[]) {
        await this.page.locator('#upload-file-input').setInputFiles(files);
        for (const file of files)
            await expect(this.listing.getByRole('checkbox', { name: file.name, exact: true })).toBeVisible({ timeout: 60000 });
    }
    async download(name: string, bytes?: Buffer, zip = false, trigger?: () => Promise<unknown>) {
        const pending = this.page.waitForEvent('download');
        if (trigger) await trigger();
        else await this.action('download');
        const download = await pending;
        expect(await download.failure()).toBeNull();
        expect(download.suggestedFilename()).toBe(zip ? 'export.zip' : name);
        const file = this.info.outputPath(name);
        await download.saveAs(file);
        const data = await fs.readFile(file);
        if (bytes)
            expect(hash(data)).toBe(hash(bytes));
        return data;
    }
}

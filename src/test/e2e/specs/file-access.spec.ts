import { test, expect } from '../fixtures/test';
import { binary, media, png, text } from '../fixtures/data';
import { unzipSync } from 'fflate';
import type { Locator } from '@playwright/test';

// These scenarios run on desktop Chromium and touch-enabled Android/iPhone profiles.
test('file names download every format without changing selection @smoke', async ({ files, workspace, isMobile }) => {
    const entries = { ...media(), 'document.txt': text, 'binary.bin': binary };
    await workspace.prepare('tmp', 'access', entries);
    await files.open('tmp', workspace.root + '/access');
    await files.select(['document.txt']);
    for (const [name, bytes] of Object.entries(entries)) {
        const link = files.listing.getByRole('link', { name: 'Télécharger ' + name, exact: true });
        await expect(link).toHaveAttribute('href', /downloadFile\?dir=/);
        await files.download(name, bytes!, false, () => isMobile ? link.tap() : link.click());
        await expect(files.listing.getByRole('checkbox', { checked: true })).toHaveCount(1);
        await expect(files.listing.getByRole('checkbox', { name: 'document.txt', exact: true })).toBeChecked();
        await expect(files.page.locator('#download-status')).toContainText('Téléchargement demandé : ' + name);
        await expect(files.page.locator('#mobileDetails')).not.toBeVisible();
    }
});

test('explicit preview preserves selection and mobile focus @smoke', async ({ files, workspace, isMobile }) => {
    await workspace.prepare('tmp', 'preview-access', { ...media(), 'document.txt': text });
    await files.open('tmp', workspace.root + '/preview-access');
    await files.select(['document.txt']);
    let downloads = 0;
    files.page.on('download', () => downloads++);
    const button = files.listing.getByRole('button', { name: 'Aperçu : pixel.png', exact: true });
    if (isMobile) await button.tap();
    else { await button.focus(); await files.page.keyboard.press('Enter'); }
    const image = files.page.locator('#detailArea .details-image');
    await expect(image).toBeVisible();
    await expect.poll(() => image.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0)).toBeTruthy();
    await expect(files.listing.getByRole('checkbox', { checked: true })).toHaveCount(1);
    await expect(files.listing.getByRole('checkbox', { name: 'document.txt', exact: true })).toBeChecked();
    expect(downloads).toBe(0);
    if (!isMobile) await expect(files.page.locator('#detailArea')).toBeFocused();
    await files.download('pixel.png', png, false, () => files.page.locator('#detailArea .btn-download').click());
    await expect(files.page.locator(isMobile ? '#mobile-download-status' : '#download-status')).toContainText('pixel.png');
    if (isMobile) {
        await expect(files.page.locator('#mobileDetails')).toBeVisible();
        await files.page.getByRole('button', { name: 'Retour à la liste', exact: true }).tap();
        await expect(button).toBeFocused();
    }
    expect(downloads).toBe(1);
});

test('folder and mixed selections download ZIP; deselection resets actions @smoke', async ({ files, workspace, isMobile }) => {
    await workspace.prepare('tmp', 'selection-access', { 'folder/empty/': null, 'folder/inside.txt': text, 'document.txt': text });
    await files.open('tmp', workspace.root + '/selection-access');
    const activate = (control: Locator) => isMobile ? control.tap() : control.click();
    const download = files.page.locator(isMobile ? '#mobile-toolbar-download' : '#toolbar-download');
    await expect(download).toBeDisabled();
    // Use the label's outer edge to check that the enlarged touch target works.
    const folder = files.listing.getByRole('checkbox', { name: 'folder', exact: true });
    await folder.locator('..').click({ position: { x: 2, y: 2 } });
    await expect(folder).toBeChecked();
    await expect(files.page.locator('#selection-count')).toHaveText('Éléments sélectionnés : 1');
    await expect(download).toHaveAccessibleName('Télécharger (.zip)');
    const single = unzipSync(await files.download('folder.zip', undefined, true, () => activate(download)));
    expect(Object.keys(single).sort()).toEqual(['folder/', 'folder/empty/', 'folder/inside.txt']);
    expect(Buffer.from(single['folder/inside.txt'])).toEqual(text);
    await files.listing.getByRole('checkbox', { name: 'document.txt', exact: true }).check();
    await expect(files.page.locator('#selection-count')).toHaveText('Éléments sélectionnés : 2');
    const mixed = unzipSync(await files.download('selection.zip', undefined, true, () => activate(download)));
    expect(Object.keys(mixed).sort()).toEqual(['document.txt', 'folder/', 'folder/empty/', 'folder/inside.txt']);
    await activate(files.page.getByRole('button', { name: 'Tout désélectionner', exact: true }));
    await expect(files.listing.getByRole('checkbox', { checked: true })).toHaveCount(0);
    await expect(files.page.locator('#selection-count')).toHaveText('Aucun élément sélectionné');
    await expect(download).toBeDisabled();
    await expect(download).toHaveAccessibleName('Télécharger');
    // Opening a folder remains a single activation, without downloading it.
    await activate(files.listing.getByRole('link', { name: 'folder', exact: true }));
    await expect(files.page.locator('#bigdirectory')).toHaveText('folder');
});

test('keyboard separates checkbox selection and filename download', async ({ files, workspace }) => {
    await workspace.prepare('tmp', 'keyboard-access', { 'document.txt': text });
    await files.open('tmp', workspace.root + '/keyboard-access');
    const checkbox = files.listing.getByRole('checkbox', { name: 'document.txt', exact: true });
    await checkbox.focus();
    await files.page.keyboard.press('Space');
    await expect(checkbox).toBeChecked();
    const link = files.listing.getByRole('link', { name: 'Télécharger document.txt', exact: true });
    await link.focus();
    await files.download('document.txt', text, false, () => files.page.keyboard.press('Enter'));
    await expect(link).toBeFocused();
    await expect(checkbox).toBeChecked();
});

test('file controls fit phone, landscape, tablet and desktop widths', async ({ files, workspace }) => {
    const name = 'NomTrèsLongSansEspaces'.repeat(6) + '.txt';
    test.setTimeout(180_000);
    await workspace.prepare('tmp', 'responsive-access', { [name]: text });
    await files.open('tmp', workspace.root + '/responsive-access');
    for (const [width, height] of [[320, 640], [360, 640], [640, 360], [768, 600], [992, 700], [1440, 900]]) {
        await files.page.setViewportSize({ width, height });
        for (const language of ['fr', 'de', 'es', 'en']) {
            await files.page.goto('/?lang=' + language + '#path=' + await files.page.locator('#bigdirectory').getAttribute('rel'));
            await expect(files.listing.getByRole('checkbox', { name, exact: true })).toBeVisible();
            for (const selector of ['.file-select', '.file', '.file-preview']) {
                const control = files.listing.locator(selector).first();
                await expect(control).toBeInViewport();
                const box = (await control.boundingBox())!;
                expect(box.width).toBeGreaterThanOrEqual(44);
                expect(box.height).toBeGreaterThanOrEqual(44);
                expect(box.x).toBeGreaterThanOrEqual(0);
                expect(box.x + box.width).toBeLessThanOrEqual(width);
            }
            expect(await files.page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
        }
        // A long filename in the persistent feedback must leave the list usable.
        await files.download(name, text, false, () => files.listing.locator('.file').click());
        const feedback = files.page.locator('#download-status');
        await expect(feedback).toContainText(name);
        expect((await feedback.boundingBox())!.height).toBeLessThan(height / 3);
        expect(await files.page.locator('#browserArea').evaluate(element => element.clientHeight)).toBeGreaterThan(44);
    }
});

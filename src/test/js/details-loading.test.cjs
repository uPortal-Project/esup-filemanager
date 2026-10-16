// Run with: node --test src/test/js/details-loading.test.cjs
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { runInNewContext } = require('node:vm');

const scripts = resolve(__dirname, '../../main/webapp/js');

function setup() {
    const element = () => ({
        children: [], attributes: {}, html: '',
        setAttribute(key, value) { this.attributes[key] = value; },
        removeAttribute(key) { delete this.attributes[key]; },
        replaceChildren(...nodes) { this.children = nodes; this.html = ''; },
        append(node) { this.children.push(node); },
        set innerHTML(html) { this.html = html; this.children = []; },
        get innerHTML() { return this.html; },
        addEventListener(event, listener) { this[event] = listener; }
    });
    const root = element();
    root.innerHTML = 'previous selection';
    const browser = element();
    const downloadStatus = element();
    const requests = [];
    const errors = [];
    const downloads = [];
    const notices = [];
    const timers = [];
    const navigation = [];
    const lifecycle = [];
    const context = {
        URL, URLSearchParams, FormData, AbortController,
        requestAnimationFrame: callback => callback(),
        setTimeout: callback => timers.push(callback),
        UIComponents: { showInfo: message => notices.push(message) },
        console: { log() {}, error: (...args) => errors.push(args) },
        document: {
            getElementById: id => ({ detailArea: root, browserMain: browser, 'download-status': downloadStatus })[id],
            querySelectorAll: () => [],
            querySelector: () => ({ content: 'csrf-token' }),
            createElement: () => {
                const node = element();
                node.remove = () => downloads.splice(downloads.indexOf(node), 1);
                return node;
            },
            body: { appendChild: node => downloads.push(node) }
        },
        window: {
            location: { href: 'https://files.example.test/' },
            addEventListener: (event, handler) => navigation.push(handler),
            i18n: { detailsLoading: 'Chargement', detailsLoadingError: 'Erreur', detailsRetry: 'Réessayer', detailsDownloadRequested: 'Téléchargement demandé' },
            detailsManager: {
                destroy: () => lifecycle.push(['destroy', root.innerHTML]),
                mount: node => lifecycle.push(['mount', node.innerHTML])
            }
        },
        fetch: (url, config) => new Promise((resolve, reject) => {
            requests.push({ url, config, resolve: html => resolve({
                ok: true, headers: { get: () => 'text/html' }, text: async () => html
            }), reject });
        })
    };
    const ajax = readFileSync(resolve(scripts, 'ajax-manager.js'), 'utf8').replace('export class', 'class');
    const app = readFileSync(resolve(scripts, 'esup-stock-modern.js'), 'utf8')
        .replace(/^import .*;\n/gm, '').split('// Initialize application on DOM load')[0];
    const manager = runInNewContext(`${ajax}\n${app}\nObject.create(EsupFileManager.prototype)`, context);
    manager.config = { detailsAreaURL: '/details', htmlFileTreeURL: '/directory' };
    return { manager, root, browser, requests, errors, lifecycle, downloads, downloadStatus, notices, timers, navigation };
}

test('only the latest selection renders, even when fetch ignores cancellation', async () => {
    const { manager, root, requests, lifecycle, errors } = setup();
    const first = manager.loadDetailsArea(['/A']);
    const second = manager.loadDetailsArea(['/B & x', '/C=é']);
    assert.equal(requests[0].config.signal.aborted, true);
    assert.equal(root.innerHTML, '');
    assert.equal(root.children[0].textContent, 'Chargement');
    assert.equal(root.attributes['aria-busy'], 'true');
    assert.deepEqual(new URLSearchParams(requests[1].config.body).getAll('dirs'), ['/B & x', '/C=é']);
    assert.equal(requests[1].config.method, 'POST');
    assert.equal(requests[1].config.headers['Accept'], 'text/html');
    assert.equal(requests[1].config.headers['X-XSRF-TOKEN'], 'csrf-token');

    requests[1].resolve('B and C');
    await second;
    requests[0].resolve('A');
    await first;
    assert.equal(root.innerHTML, 'B and C');
    assert.equal(root.attributes['aria-busy'], undefined);
    assert.deepEqual(lifecycle, [
        ['destroy', 'previous selection'], ['destroy', ''], ['destroy', ''], ['mount', 'B and C']
    ]);
    assert.equal(errors.length, 0);
});

test('a stale failure cannot clear busy state or show an error; deselection supersedes all details', async () => {
    const { manager, root, requests, errors } = setup();
    const first = manager.loadDetailsArea(['/A']);
    const empty = manager.loadDetailsArea([]);
    requests[0].reject(new Error('late network failure'));
    await first;
    assert.equal(root.children[0].textContent, 'Chargement');
    assert.equal(root.children.length, 1);
    assert.equal(root.attributes['aria-busy'], 'true');
    assert.deepEqual(new URLSearchParams(requests[1].config.body).getAll('dirs'), []);
    requests[1].resolve('empty selection');
    await empty;
    assert.equal(root.innerHTML, 'empty selection');
    assert.equal(errors.length, 0);
});

test('intentional cancellation is silent', async () => {
    const { manager, root, requests, errors } = setup();
    const first = manager.loadDetailsArea(['/A']);
    const second = manager.loadDetailsArea(['/B']);
    requests[0].reject(Object.assign(new Error('cancelled'), { name: 'AbortError' }));
    await first;
    assert.equal(root.children.length, 1);
    assert.equal(errors.length, 0);
    requests[1].resolve('B');
    await second;
});

test('errors offer a translated retry for the same parameters, superseded retries are inert', async () => {
    const { manager, root, requests } = setup();
    const loading = manager.loadDetailsArea(['/A', '/B']);
    requests[0].reject(new Error('offline'));
    await loading;
    assert.equal(root.attributes['aria-busy'], undefined);
    assert.equal(root.children[0].textContent, 'Erreur');
    const retry = root.children[1];
    assert.equal(retry.textContent, 'Réessayer');
    retry.click();
    assert.equal(root.children[0].textContent, 'Chargement');
    assert.equal(requests[1].config.body, requests[0].config.body);
    requests[1].resolve('recovered');
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(root.innerHTML, 'recovered');
    const next = manager.loadDetailsArea(['/C']);
    retry.click();
    assert.equal(requests.length, 3);
    requests[2].resolve('C');
    await next;
});

test('navigation clears pending details immediately and late directory responses do not reload them', async () => {
    const { manager, root, browser, requests } = setup();
    manager.saveCurrentPath = () => {};
    manager.updateDriveContextButtons = () => {};
    manager.fileTree = { selectedNode: { dataset: { type: 'folder' } } };
    const old = manager.loadDetailsArea(['/old']);
    const navigation = manager.loadDirectoryContent('/folder', false);
    assert.equal(requests[0].config.signal.aborted, true);
    assert.equal(root.innerHTML, '');
    assert.equal(requests[1].url, '/details');
    const selection = manager.loadDetailsArea(['/folder/file']);
    requests[3].resolve('file');
    await selection;
    requests[2].resolve('directory listing');
    await navigation;
    requests[1].resolve('folder');
    requests[0].resolve('old');
    await old;
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(root.innerHTML, 'file');
    assert.equal(browser.innerHTML, 'directory listing');
    assert.equal(requests.length, 4);

    manager.fileTree.selectedNode.dataset.type = 'category';
    const category = manager.loadDirectoryContent('/category', false);
    assert.equal(requests[4].config.body, '');
    requests[4].resolve('empty');
    requests[5].resolve('category listing');
    await category;
    assert.equal(root.innerHTML, 'empty');
});


test('slow downloads remain alive until navigation and only report a request', () => {
    const { manager, downloads, downloadStatus, timers, navigation } = setup();
    manager.triggerDownload('/downloadZip?dirs=large-folder');
    assert.match(downloadStatus.textContent, /Download requested: export.zip/);
    manager.triggerDownload('/downloadFile?dir=s3-file');
    assert.deepEqual(downloads.map(frame => frame.src), [
        '/downloadZip?dirs=large-folder', '/downloadFile?dir=s3-file'
    ]);
    assert.ok(downloads.every(frame => frame.hidden && frame.tabIndex === -1));
    assert.equal(downloadStatus.textContent, 'Téléchargement demandé');
    // Advancing every scheduled timeout cannot interrupt response preparation.
    timers.forEach(callback => callback());
    assert.equal(downloads.length, 2);
    navigation.forEach(handler => handler());
    assert.equal(downloads.length, 0);
});

// Run with: node --test src/test/js/details-manager.test.cjs
// Requires Chromium (CHROME_BIN can override the executable).
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { readFileSync, writeFileSync, mkdtempSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { resolve, join } = require('node:path');

const webapp = resolve(__dirname, '../../main/webapp');

test('details lifecycle and keyboard accessibility in Chromium', async () => {
    const directory = mkdtempSync(join(tmpdir(), 'details-lifecycle-'));
    const fixture = `
        <div class="details-card">
            <button class="image-preview-container" aria-label="View One"><img class="details-image" alt="One" data-fullsize="https://example.test/image"></button>
            <button class="btn-view-fullsize">Image</button>
            <a class="btn-download" href="/download">Download</a>
            <button id="detail-zip-download">ZIP</button>
            <button id="detail-view-images">Slideshow</button>
        </div>
        <dialog id="imageModal" class="image-modal" aria-label="Image"><button class="image-modal-close" autofocus>Close</button><img id="modalImage"></dialog>
        <div id="slideshowData">
            <div class="slideshow-image-data" data-src="/one" data-title="One"></div>
            <div class="slideshow-image-data" data-src="/two" data-title="Two"></div>
            <div class="slideshow-image-data" data-src="/three" data-title="Three"></div>
        </div>
        <dialog id="slideshowModal" class="slideshow-modal" aria-label="Slideshow">
            <button class="slideshow-close" autofocus>Close</button>
            <button class="slideshow-prev">Previous</button><button class="slideshow-next">Next</button>
            <img id="slideshowImage"><span id="slideCounter" aria-live="polite"></span>
            <button id="slideshowAutoplay" data-play="▶ Lecture auto" data-pause="⏸ Pause" aria-pressed="false">▶ Lecture auto</button>
        </dialog>
        <audio id="playlistAudio"></audio><span id="playlistCounter"></span>
        <button id="playlistPrev">Previous</button><button id="playlistNext">Next</button>
        <ul id="playlistTracks">
            <li><button class="playlist-track" data-index="0">One</button></li>
            <li><button class="playlist-track" data-index="1">Two</button></li>
            <li><button class="playlist-track" data-index="2">Three</button></li>
        </ul>
        <div id="playlistData">
            <div class="playlist-track-data" data-src="/audio-one"></div>
            <div class="playlist-track-data" data-src="/audio-two"></div>
            <div class="playlist-track-data" data-src="/audio-three"></div>
        </div>
        <div class="media-player-container"><video><source src="/video"></video></div>`;
    const checks = function(fixture) {
        const manager = window.detailsManager;
        const root = document.getElementById('detailArea');
        const check = (condition, message) => { if (!condition) throw new Error(message); };
        const click = selector => root.querySelector(selector).click();
        const key = value => root.querySelector('#slideshowModal').dispatchEvent(new KeyboardEvent('keydown', { key: value, bubbles: true }));
        let downloads = 0;
        let zips = 0;
        window.esupFileManager = { triggerDownload: () => downloads++ };
        document.getElementById('toolbar-zip').addEventListener('click', () => zips++);
        let detachedAudio;
        let detachedButton;

        for (let i = 0; i < 20; i++) {
            manager.mount(root);
            const button = root.querySelector('#detail-view-images');
            const audio = root.querySelector('#playlistAudio');
            const initialLoads = audio.loads;
            manager.mount(root);
            check(button === root.querySelector('#detail-view-images'), 'mount replaced button');
            check(audio.loads === initialLoads, 'same-root mount restarted media');
            check(getComputedStyle(root.querySelector('#slideshowModal')).display === 'none', 'hidden slideshow visible');
            click('#detail-view-images');
            check(getComputedStyle(root.querySelector('#slideshowModal')).display === 'flex', 'open slideshow hidden');
            key('ArrowRight');
            check(manager.imageIndex === 1, 'arrow advanced more than one image');
            audio.dispatchEvent(new Event('ended'));
            check(manager.trackIndex === 1 && audio.plays === 1, 'ended advanced more than one track');
            click('.btn-download');
            click('#detail-zip-download');
            check(downloads === i + 1 && zips === i + 1, 'duplicated download action');
            click('#slideshowAutoplay');
            check(window.intervals.size === 1, 'duplicated autoplay interval');
            check(root.querySelector('#slideshowAutoplay').getAttribute('aria-pressed') === 'true', 'autoplay state missing');
            check(root.querySelector('#slideCounter').getAttribute('aria-live') === 'off', 'autoplay counter too noisy');
            click('#slideshowAutoplay');
            check(window.intervals.size === 0, 'autoplay toggle failed');
            check(root.querySelector('#slideshowAutoplay').textContent === '▶ Lecture auto', 'autoplay label changed language');
            check(root.querySelector('#slideCounter').getAttribute('aria-live') === 'polite', 'manual counter silent');
            click('#slideshowAutoplay');
            if (detachedAudio) {
                detachedAudio.dispatchEvent(new Event('ended'));
                detachedButton.click();
                check(manager.trackIndex === 1 && manager.imageIndex === 1, 'detached handlers remain active');
            }
            detachedAudio = audio;
            detachedButton = button;
            const video = root.querySelector('video');
            manager.destroy();
            check(window.intervals.size === 0, 'interval survived destroy');
            check(!document.body.classList.contains('modal-open'), 'scroll locked after destroy');
            check(audio.pauses === 1 && video.pauses === 1, 'media not paused');
            check(!audio.hasAttribute('src') && !video.querySelector('source').hasAttribute('src'), 'media sources retained');
            check(!root.querySelector('#slideshowImage').hasAttribute('src'), 'slideshow source retained');
            check(manager.images.length === 0 && manager.tracks.length === 0, 'collections retained');
            key('ArrowRight');
            check(manager.imageIndex === 0, 'global handler survived destroy');
            manager.destroy();
            root.innerHTML = fixture;
        }

        manager.mount(root);
        click('.btn-view-fullsize');
        check(root.querySelector('#imageModal').open, 'image did not open');
        root.querySelector('#imageModal').dispatchEvent(new Event('cancel', { cancelable: true }));
        check(!root.querySelector('#imageModal').open && !document.body.classList.contains('modal-open'), 'image close left scroll locked');
        click('#detail-view-images');
        click('#slideshowAutoplay');
        click('.slideshow-close');
        check(window.intervals.size === 0 && !document.body.classList.contains('modal-open'), 'slideshow close retained work');
        click('#detail-view-images');
        click('#slideshowAutoplay');
        window.dispatchEvent(new PageTransitionEvent('pagehide', { persisted: true }));
        check(window.intervals.size === 0 && manager.root === root, 'cached navigation did not suspend');
        window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
        click('#playlistNext');
        check(manager.trackIndex === 1, 'cached navigation broke playlist');
        window.dispatchEvent(new PageTransitionEvent('pagehide'));
        check(manager.root === null && !document.body.classList.contains('modal-open'), 'navigation did not destroy');
        document.getElementById('result').textContent = 'PASS';
    };
    const setup = `
        window.intervals = new Map();
        let timer = 0;
        window.setInterval = callback => { window.intervals.set(++timer, callback); return timer; };
        window.clearInterval = id => window.intervals.delete(id);
        HTMLMediaElement.prototype.pause = function() { this.pauses = (this.pauses || 0) + 1; };
        HTMLMediaElement.prototype.load = function() { this.loads = (this.loads || 0) + 1; };
        HTMLMediaElement.prototype.play = function() { this.plays = (this.plays || 0) + 1; return Promise.resolve(); };`;
    const keyboardHandler = readFileSync(join(webapp, 'js/esup-stock-modern.js'), 'utf8')
        .match(/    bindKeyboardEvents\(\) {[\s\S]*?\n    }(?=\n\n    initContextMenus)/)[0];
    const shortcuts = `
        window.globalActions = 0;
        const action = () => window.globalActions++;
        ({ ${keyboardHandler} }).bindKeyboardEvents.call({
            copyFiles: action, cutFiles: action, pasteFiles: action,
            deleteFiles: action, handleRename: action, handleEnterKey: action
        });`;
    const html = `<!doctype html><style>${readFileSync(join(webapp, 'css/details-modern.css'), 'utf8')}</style>
        <button id="treeSplitter">Navigation</button><button id="toolbar-zip">ZIP</button><div id="detailArea">${fixture}</div><pre id="result">PENDING</pre>
        <script>${setup}${shortcuts}</script><script>${readFileSync(join(webapp, 'js/details-manager.js'), 'utf8')}</script>
        <script>document.addEventListener('DOMContentLoaded', () => {
            try { (${checks})(${JSON.stringify(fixture)}); }
            catch (error) { document.getElementById('result').textContent = error.stack; }
        });</script>`;
    try {
        const file = join(directory, 'test.html');
        writeFileSync(file, html);
        await checkKeyboard(file, directory, fixture);
    } finally {
        rmSync(directory, { recursive: true, force: true });
    }
});

// CDP sends trusted keyboard events; synthetic keydown cannot exercise native
// dialog tab containment or button activation.
async function checkKeyboard(file, directory, fixture) {
    const browser = spawn(process.env.CHROME_BIN || 'chromium', [
        '--headless', '--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage',
        `--user-data-dir=${join(directory, 'keyboard-profile')}`, '--remote-debugging-pipe'
    ], { stdio: ['ignore', 'ignore', 'ignore', 'pipe', 'pipe'] });
    let id = 0;
    let buffer = '';
    const pending = new Map();
    browser.stdio[4].setEncoding('utf8');
    browser.stdio[4].on('data', chunk => {
        buffer += chunk.toString();
        let end;
        while ((end = buffer.indexOf('\0')) !== -1) {
            const message = JSON.parse(buffer.slice(0, end));
            buffer = buffer.slice(end + 1);
            const request = pending.get(message.id);
            if (!request) continue;
            pending.delete(message.id);
            if (message.error) request.reject(new Error(message.error.message));
            else request.resolve(message.result);
        }
    });
    const send = (method, params = {}, sessionId) => new Promise((resolve, reject) => {
        const requestId = ++id;
        pending.set(requestId, { resolve, reject });
        browser.stdio[3].write(JSON.stringify({ id: requestId, method, params, sessionId }) + '\0');
    });
    const timeout = setTimeout(() => {
        for (const request of pending.values()) request.reject(new Error('Keyboard test timed out'));
        browser.kill();
    }, 20000);
    try {
        const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
        const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true });
        const evaluate = async expression => {
            const result = await send('Runtime.evaluate', {
                expression, returnByValue: true, awaitPromise: true
            }, sessionId);
            assert.ok(!result.exceptionDetails, JSON.stringify(result.exceptionDetails));
            return result.result.value;
        };
        await send('Page.navigate', { url: `file://${file}` }, sessionId);
        // Wait for the fixture's lifecycle checks to finish, not a fixed delay.
        const lifecycleResult = await evaluate(`new Promise(resolve => {
            const done = () => {
                const result = document.getElementById('result');
                if (result && result.textContent !== 'PENDING') resolve(result.textContent);
                else setTimeout(done, 10);
            };
            done();
        })`);
        assert.equal(lifecycleResult, 'PASS');
        await evaluate(`window.detailsManager.destroy();
            document.getElementById('detailArea').innerHTML = ${JSON.stringify(fixture)};
            window.detailsManager.mount(document.getElementById('detailArea'));
            document.querySelector('.image-preview-container').focus();`);
        const key = async (key, code, modifiers = 0) => {
            const windowsVirtualKeyCode = { Enter: 13, Space: 32, Tab: 9, Escape: 27, ArrowRight: 39, Delete: 46 }[code];
            await send('Input.dispatchKeyEvent', { type: 'keyDown', key, code, modifiers, windowsVirtualKeyCode, text: code === 'Enter' ? '\r' : code === 'Space' ? ' ' : '' }, sessionId);
            await send('Input.dispatchKeyEvent', { type: 'keyUp', key, code, modifiers, windowsVirtualKeyCode }, sessionId);
        };
        const check = async (expression, message) => assert.equal(await evaluate(expression), true, message);
        await key('Enter', 'Enter');
        await check(`document.querySelector('#imageModal').open && document.activeElement.matches('.image-modal-close')`, 'preview Enter opens and focuses close');
        await check(`document.querySelector('#modalImage').alt === 'One'`, 'image alt identifies file');
        await evaluate(`document.getElementById('toolbar-zip').focus()`);
        await check(`document.activeElement.closest('#imageModal') !== null`, 'background focus blocked');
        await key('Tab', 'Tab');
        await key('Tab', 'Tab', 8);
        await check(`document.activeElement.closest('#imageModal') !== null`, 'image contains tab and shift-tab');
        await key('Escape', 'Escape');
        await check(`!document.querySelector('#imageModal').open && document.activeElement.matches('.image-preview-container')`, 'Escape restores trigger');
        await evaluate(`document.getElementById('detail-view-images').focus()`);
        await key(' ', 'Space');
        await check(`document.querySelector('#slideshowModal').open && document.activeElement.matches('.slideshow-close')`, 'Space opens slideshow');
        await key('Tab', 'Tab', 8);
        await check(`document.activeElement.id === 'slideshowAutoplay'`, 'reverse tab wraps to last control');
        await key('Tab', 'Tab');
        // Native dialogs allow a stop in browser chrome, then return inside.
        if (await evaluate(`document.activeElement === document.body`)) await key('Tab', 'Tab');
        await check(`document.activeElement.matches('.slideshow-close')`, 'tab returns to first control');
        await key('Delete', 'Delete');
        await check(`window.globalActions === 0`, 'modal blocks file commands');
        await key('ArrowRight', 'ArrowRight');
        await check(`window.detailsManager.imageIndex === 1`, 'arrow advances once');
        await evaluate(`document.getElementById('slideshowAutoplay').focus()`);
        await key(' ', 'Space');
        await check(`window.intervals.size === 1 && document.getElementById('slideshowAutoplay').textContent === '⏸ Pause'`, 'Space starts autoplay once');
        await key('Enter', 'Enter');
        await check(`window.intervals.size === 0`, 'Enter stops autoplay once');
        await key('Escape', 'Escape');
        await check(`document.activeElement.id === 'detail-view-images'`, 'slideshow restores trigger');
        await evaluate(`document.querySelector('[data-index="1"]').focus()`);
        await key('Enter', 'Enter');
        await check(`window.detailsManager.trackIndex === 1 && document.querySelectorAll('.playlist-track[aria-current="true"]').length === 1`, 'Enter selects track with current state');
        await key('Tab', 'Tab');
        await key(' ', 'Space');
        await check(`window.detailsManager.trackIndex === 2 && document.querySelector('[data-index="2"]').getAttribute('aria-current') === 'true'`, 'Tab and Space select next track');
        await evaluate(`document.getElementById('playlistAudio').dispatchEvent(new Event('ended'))`);
        await check(`window.detailsManager.trackIndex === 0 && document.querySelector('[data-index="0"]').getAttribute('aria-current') === 'true'`, 'ended updates current track');
        await check(`window.globalActions === 0`, 'details Enter leaves file shortcuts untouched');
        await evaluate(`document.getElementById('detail-view-images').click(); document.getElementById('detail-view-images').remove()`);
        await key('Escape', 'Escape');
        await check(`document.activeElement.id === 'treeSplitter'`, 'missing trigger restores navigation focus');
        await key('Enter', 'Enter');
        await check(`window.globalActions === 1`, 'file shortcuts remain active outside details');
        await evaluate(`window.detailsManager.openModal(document.querySelector('#slideshowModal')); window.detailsManager.destroy()`);
        await check(`!document.querySelector('#slideshowModal').open && document.activeElement.id === 'treeSplitter'`, 'replacement restores navigation focus');
    } finally {
        clearTimeout(timeout);
        browser.kill();
        await new Promise(resolve => browser.once('exit', resolve));
    }
}

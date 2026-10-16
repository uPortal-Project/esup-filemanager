// Run with: node --test src/test/js/details-manager.test.cjs
// Requires Chromium (CHROME_BIN can override the executable).
const { test } = require('node:test');
const { createServer } = require('node:http');
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const { readFileSync, writeFileSync, mkdtempSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { resolve, join } = require('node:path');

const webapp = resolve(__dirname, '../../main/webapp');

test('details lifecycle and keyboard accessibility in Chromium', async () => {
    const directory = mkdtempSync(join(tmpdir(), 'details-lifecycle-'));
    const server = createServer((request, response) => {
        response.writeHead(200, { 'Content-Type': 'image/svg+xml' });
        response.end('<svg xmlns="http://www.w3.org/2000/svg" width="12" height="9"/>');
    });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const origin = `http://127.0.0.1:${server.address().port}`;
    const fixture = `
        <div class="details-card">
            <button class="image-preview-container" aria-label="View One"><img class="details-image" alt="One" data-fullsize="${origin}/image"></button>
            <button class="btn-view-fullsize">Image</button>
            <a class="btn-download" href="/download">Download</a>
            <button id="detail-zip-download">ZIP</button>
            <button id="detail-view-images">Slideshow</button>
        </div>
        <dialog id="imageModal" class="image-modal" aria-label="Image"><button class="image-modal-close" autofocus>Close</button><img id="modalImage"></dialog>
        <div id="slideshowData">
            <div class="slideshow-image-data" data-src="${origin}/one" data-title="One"></div>
            <div class="slideshow-image-data" data-src="${origin}/two" data-title="Two"></div>
            <div class="slideshow-image-data" data-src="${origin}/three" data-title="Three"></div>
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
            <div class="playlist-track-data" data-src="${origin}/audio-one"></div>
            <div class="playlist-track-data" data-src="${origin}/audio-two"></div>
            <div class="playlist-track-data" data-src="${origin}/audio-three"></div>
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
        document.getElementById('toolbar-download').addEventListener('click', () => zips++);
        let detachedAudio;
        let detachedButton;

        for (let i = 0; i < 20; i++) {
            manager.mount(root);
            const button = root.querySelector('#detail-view-images');
            const audio = root.querySelector('#playlistAudio');
            const initialLoads = audio.loads;
            const initialPauses = audio.pauses;
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
            check(audio.pauses === initialPauses + 2 && video.pauses === 1, 'media not paused');
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
    const html = `<!doctype html><base href="${origin}/"><style>${readFileSync(join(webapp, 'css/details-modern.css'), 'utf8')}</style>
        <div id="treeSplitter" tabindex="0" role="separator">Navigation</div><button id="toolbar-download">ZIP</button><div id="detailArea">${fixture}</div><pre id="result">PENDING</pre>
        <script>${setup}${shortcuts}</script><script>${readFileSync(join(webapp, 'js/details-manager.js'), 'utf8')}</script>
        <script>document.addEventListener('DOMContentLoaded', () => {
            try { (${checks})(${JSON.stringify(fixture)}); }
            catch (error) { document.getElementById('result').textContent = error.stack; }
        }, { once: true });</script>`;
    try {
        const file = join(directory, 'test.html');
        writeFileSync(file, html);
        await checkKeyboard(file, directory, fixture);
    } finally {
        server.closeAllConnections();
        await new Promise(resolve => server.close(resolve));
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
    }, 40000);
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
        // Real DOM states, with controlled media events and play() rejections.
        const templates = resolve(__dirname, '../../main/resources/templates');
        for (const locale of ['_fr', '']) {
            const properties = readFileSync(join(templates, `../messages${locale}.properties`), 'utf8');
            const translations = Object.fromEntries(['mediaLoading', 'imageError', 'mediaError', 'mediaPlayBlocked', 'retry'].map(key => {
                const value = properties.match(new RegExp(`^details\\.${key}\\s*=\\s*(.*)$`, 'm'))[1];
                return ['i18nDetails' + key[0].toUpperCase() + key.slice(1), value];
            }));
            await evaluate(`{ const config = document.getElementById('esup-app-config') || document.body.appendChild(document.createElement('div'));
                config.id = 'esup-app-config';
                Object.assign(config.dataset, ${JSON.stringify(translations)});
                ${readFileSync(join(webapp, 'js/esup-app-config.js'), 'utf8')}
                window.detailsManager.destroy();
                window.detailsManager.mount(document.getElementById('detailArea')); }`);
            await evaluate(`(async () => {
                const manager = window.detailsManager;
                const root = manager.root;
                const check = (condition, message) => { if (!condition) throw new Error(message); };
                const preview = root.querySelector('.details-image');
                const dimensions = document.createElement('dd');
                dimensions.id = 'image_dimensions'; root.append(dimensions);
                manager.setImage(preview, 'javascript:invalid');
                const imageState = manager.mediaStates.get(preview);
                check(!preview.hasAttribute('src') && preview.dataset.state === 'error', 'invalid image retains old source');
                check(imageState.message.textContent === window.i18n.detailsImageError && dimensions.textContent === '—', 'image error or dimensions not translated');
                preview.dispatchEvent(new Event('error'));
                preview.dispatchEvent(new Event('error'));
                check(root.querySelectorAll('.details-media-status').length === 5, 'duplicated media messages');
                check(imageState.retry.textContent === window.i18n.detailsRetry, 'retry not translated');
                manager.setImage(preview, ${JSON.stringify(fixture.match(/data-fullsize="([^"]+)"/)[1])});
                check(preview.getAttribute('aria-busy') === 'true', 'image not busy while loading');
                await new Promise(resolve => { if (preview.dataset.state === 'ready') resolve(); else preview.addEventListener('load', resolve, { once: true }); });
                check(imageState.status.hidden && dimensions.textContent === '12 px × 9 px', 'successful image did not clear error or dimensions');
                const audio = root.querySelector('#playlistAudio');
                const audioState = manager.mediaStates.get(audio);
                const originalPlay = audio.play;
                audio.dispatchEvent(new Event('error'));
                audio.dispatchEvent(new Event('error'));
                check(audioState.message.textContent === window.i18n.detailsMediaError, 'media error not translated');
                audio.dispatchEvent(new Event('canplay'));
                check(audioState.status.hidden, 'recovery did not clear error');
                audio.play = () => Promise.reject(new DOMException('Blocked', 'NotAllowedError'));
                manager.loadTrack(1, true);
                await Promise.resolve();
                check(audioState.message.textContent === window.i18n.detailsMediaPlayBlocked, 'blocked play hidden');
                audio.dispatchEvent(new Event('canplay'));
                check(!audioState.status.hidden, 'canplay cleared manual playback advice');
                audio.play = originalPlay;
                audioState.retry.click();
                audio.dispatchEvent(new Event('playing'));
                check(audioState.status.hidden, 'retry did not recover playback');
                audio.play = () => Promise.reject(new DOMException('Interrupted', 'AbortError'));
                manager.loadTrack(0, true);
                await Promise.resolve();
                check(audio.dataset.state !== 'error', 'expected interruption reported');
                let rejectPlay;
                audio.play = () => new Promise((resolve, reject) => { rejectPlay = reject; });
                manager.loadTrack(1, true);
                manager.loadTrack(2, false);
                rejectPlay(new DOMException('Old failure', 'NotSupportedError'));
                await Promise.resolve();
                check(audio.dataset.state !== 'error', 'old play rejection overwrote current track');
                manager.loadTrack(NaN, true);
                check(manager.trackIndex === 2, 'invalid track index changed selection');
                const trackSource = manager.tracks[0].src;
                manager.tracks[0].src = '';
                manager.loadTrack(0, false);
                check(!audio.hasAttribute('src') && audio.dataset.state === 'error', 'missing track retained old audio');
                manager.tracks[0].src = trackSource;
                audio.play = originalPlay;
                manager.loadTrack(0, false);
                manager.openImage(root.querySelector('.image-preview-container'));
                const fullsize = preview.dataset.fullsize;
                preview.dataset.fullsize = 'javascript:invalid';
                manager.openImage(root.querySelector('.image-preview-container'));
                check(!root.querySelector('#modalImage').hasAttribute('src') && root.querySelector('#modalImage').dataset.state === 'error', 'invalid fullsize image retained old content');
                preview.dataset.fullsize = fullsize;
                manager.closeModal(root.querySelector('#imageModal'));
                manager.showImage(0);
                manager.toggleAutoplay();
                root.querySelector('#slideshowImage').dispatchEvent(new Event('error'));
                check(window.intervals.size === 0, 'slideshow continued after image error');
                dimensions.remove();
                manager.destroy();
                manager.mount(root);
                check(root.querySelectorAll('.details-media-status').length === 5, 'remount duplicated status');
                root.querySelector('.image-preview-container').focus();
            })()`);
        }
        const key = async (key, code, modifiers = 0) => {
            const windowsVirtualKeyCode = { Enter: 13, Space: 32, Tab: 9, Escape: 27, ArrowRight: 39, Delete: 46 }[code];
            await send('Input.dispatchKeyEvent', { type: 'keyDown', key, code, modifiers, windowsVirtualKeyCode, text: code === 'Enter' ? '\r' : code === 'Space' ? ' ' : '' }, sessionId);
            await send('Input.dispatchKeyEvent', { type: 'keyUp', key, code, modifiers, windowsVirtualKeyCode }, sessionId);
        };
        const check = async (expression, message) => assert.equal(await evaluate(expression), true, message);
        await key('Enter', 'Enter');
        await check(`document.querySelector('#imageModal').open && document.activeElement.matches('.image-modal-close')`, 'preview Enter opens and focuses close');
        await check(`document.querySelector('#modalImage').alt === 'One'`, 'image alt identifies file');
        await evaluate(`document.getElementById('toolbar-download').focus()`);
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
        // Exercise the real mobile shell and viewer markup, with all local CSS layers.
        const viewer = name => readFileSync(join(templates, name), 'utf8').match(/<dialog[\s\S]*?<\/dialog>/)[0];
        const imageSource = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="900"><rect width="1200" height="900" fill="blue"/></svg>');
        const mobileFixture = fixture.replace(/<dialog id="imageModal"[\s\S]*?<\/dialog>/, viewer('details_image.html'))
            .replace(/<dialog id="slideshowModal"[\s\S]*?<\/dialog>/, viewer('details_files.html'))
            .replace(/data-src="http:\/\/127\.0\.0\.1:\d+\/(one|two|three)"/g, `data-src="${imageSource}"`);
        const styles = ['esup-responsive.css', 'details-modern.css']
            .map(name => readFileSync(join(webapp, 'css', name), 'utf8')).join('\n');
        // Bootstrap utilities used by the shell; no external stylesheet in this fixture.
        const utilities = `
            body { margin: 0; }
            .offcanvas:not(.show) { display: none; }
            .fixed-bottom { position: fixed; bottom: 0; left: 0; right: 0; }
            .btn-group { display: flex; }
            .fixed-bottom .btn { flex: 1; }
            .d-none { display: none !important; }
            @media (min-width: 992px) {
                .d-lg-none { display: none !important; }
                .d-lg-flex { display: flex !important; }
            }`;
        await evaluate(`window.detailsManager.destroy();
            document.body.innerHTML = ${JSON.stringify(readFileSync(join(templates, 'body-responsive.html'), 'utf8'))};
            const style = document.createElement('style');
            style.textContent = ${JSON.stringify(styles + utilities)};
            document.head.appendChild(style);
            document.getElementById('detailArea').innerHTML = ${JSON.stringify(mobileFixture)};
            document.getElementById('browserMain').innerHTML = '<input id="selection" type="checkbox" checked><div style="height:2000px">Files</div>';
            window.savedDetails = document.getElementById('detailArea');
            window.savedParent = savedDetails.parentNode;
            window.detailsManager.mount(savedDetails);
            ${readFileSync(join(webapp, 'js/esup-mobile-support.js'), 'utf8')}
            document.dispatchEvent(new Event('DOMContentLoaded'));`);
        const viewport = async (width, height) => {
            await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false }, sessionId);
            await evaluate(`new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))`);
        };
        const clickControl = async selector => {
            const { x, y } = await evaluate(`(() => {
                const control = document.querySelector(${JSON.stringify(selector)});
                control.scrollIntoView({ block: 'center' });
                const rect = control.getBoundingClientRect();
                return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 };
            })()`);
            await send('Input.dispatchMouseEvent', { type: 'mousePressed', x, y, button: 'left', clickCount: 1 }, sessionId);
            await send('Input.dispatchMouseEvent', { type: 'mouseReleased', x, y, button: 'left', clickCount: 1 }, sessionId);
        };
        const visible = selector => `(() => {
            const rect = document.querySelector(${JSON.stringify(selector)}).getBoundingClientRect();
            return rect.width > 0 && rect.height > 0 && rect.left >= 0 && rect.top >= 0 &&
                rect.right <= innerWidth && rect.bottom <= innerHeight;
        })()`;
        for (const [width, height] of [[360, 640], [768, 600], [640, 240], [360, 220]]) {
            await viewport(width, height);
            await evaluate(`document.getElementById('browserArea').scrollTop = 120;
                document.getElementById('mobile-details').focus();`);
            await key('Enter', 'Enter');
            await check(`mobileDetails.open && savedDetails.parentNode === mobileDetails && document.querySelectorAll('#detailArea').length === 1`, 'mobile reuses the single details root');
            await check(`document.activeElement.id === 'mobile-details-close'`, 'mobile focuses return button');
            await check(visible('#mobile-details-close'), 'return control fits viewport');
            await evaluate(`document.getElementById('selection').focus()`);
            await check(`document.activeElement.closest('#mobileDetails') !== null`, 'mobile background is inert');
            await clickControl('.image-preview-container');
            await evaluate(`new Promise(resolve => {
                const image = document.getElementById('modalImage');
                image.onload = resolve; image.src = ${JSON.stringify(imageSource)};
                if (image.complete) resolve();
            })`);
            await check(visible('#modalImage'), 'large image fits viewport');
            await check(visible('.image-modal-close'), 'image close fits viewport');
            await key('Escape', 'Escape');
            await check(`mobileDetails.open && !imageModal.open && document.activeElement.matches('.image-preview-container')`, 'nested Escape returns to details');
            await clickControl('#detail-view-images');
            await check(visible('.slideshow-close'), 'slideshow close fits viewport');
            await check(visible('.slideshow-prev'), 'previous image fits viewport');
            await check(visible('#slideshowAutoplay'), 'autoplay fits viewport');
            await key('ArrowRight', 'ArrowRight');
            await check(`window.detailsManager.imageIndex === 1`, 'mobile slideshow advances once');
            await key('Escape', 'Escape');
            await evaluate(`document.querySelector('[data-index="1"]').click()`);
            await check(`window.detailsManager.trackIndex === 1`, 'mobile playlist remains usable');
            await key('Escape', 'Escape');
            await check(`!mobileDetails.open && savedDetails.parentNode === savedParent && document.activeElement.id === 'mobile-details'`, 'return restores panel and focus');
            await check(`document.getElementById('selection').checked && document.getElementById('browserArea').scrollTop === 120`, 'return preserves selection and list scroll');
            await check(`!document.body.classList.contains('modal-open')`, 'return releases viewer scroll lock');
        }
        await clickControl('#mobile-details');
        await clickControl('#mobile-details-close');
        await check(`!mobileDetails.open && window.detailsManager.trackIndex === 1 && document.getElementById('playlistAudio').pauses > 0`, 'return button suspends playback and keeps current track');
        await clickControl('#mobile-details');
        await evaluate(`window.detailsManager.destroy(); savedDetails.innerHTML = ${JSON.stringify(mobileFixture)};
            window.detailsManager.mount(savedDetails); document.getElementById('detail-view-images').click();`);
        await viewport(992, 700);
        await check(`!mobileDetails.open && !slideshowModal.open && savedDetails.parentNode === savedParent && document.activeElement.id === 'treeSplitter'`, 'desktop transition closes nested viewers and restores panel');
        await viewport(1440, 900);
        await check(`getComputedStyle(document.getElementById('leftPanel')).display === 'flex'`, 'desktop details remain visible');
        // Use the listing template's real nodes, filling server-side values locally.
        const listing = readFileSync(join(templates, 'fileTree.html'), 'utf8');
        const application = readFileSync(join(webapp, 'js/esup-stock-modern.js'), 'utf8');
        const methods = ['initEventListeners', 'bindBrowserAreaEvents', 'getSelectedFilePaths',
            'handleFileClick', 'getFileTypeFromPath', 'updateToolbarButtons',
            'updatePasteButtonState', 'updateDriveContextButtons'].map(name => application.match(
                new RegExp(`    ${name}\\([^]*?\\n    }`))[0]).join(',');
        await evaluate(`document.getElementById('browserMain').innerHTML = ${JSON.stringify(listing)};
            const app = { ${methods}, config: {}, isInDrive: true, clipboard: { files: ['copied'] },
                bindToolbarEvents() {}, bindKeyboardEvents() {},
                handleFileSelection() { window.selectionCalls++; window.selectedPaths = this.getSelectedFilePaths(); },
                downloadFile(path) { window.downloadedPath = path; },
                sortFiles(field) { window.sortedBy = field; },
                handleFolderClick(link) { this.navigateToPath(link.getAttribute('rel')); },
                navigateToPath(path) { window.navigatedTo = path; }
            };
            window.selectionCalls = 0;
            app.initEventListeners();
            const listing = document.querySelector('#browserMain table');
            window.savedListing = listing; window.listingApp = app;
            window.savedCheckbox = listing.querySelector('.file').closest('tr').querySelector('.browsercheck');
            savedCheckbox.value = 'file-path';
            window.savedFileLink = listing.querySelector('.file');
            savedFileLink.querySelector('span').textContent = 'VeryLongFileName'.repeat(20) + '.txt';
            savedFileLink.setAttribute('rel', 'file-path');
            savedFileLink.href = '#';
            listing.querySelector('.fileTreeRef').setAttribute('rel', 'folder-path');
            listing.querySelector('.fileTreeRef').closest('tr').querySelector('.browsercheck').value = 'folder-path';
            listing.querySelectorAll('thead a').forEach((link, index) => {
                link.textContent = ['Filename', 'Size', 'Modified'][index]; link.setAttribute('rel', ['titleAsc', 'sizeAsc', 'lastModifiedAsc'][index]);
            });
            listing.querySelectorAll('tbody tr').forEach(row => {
                row.cells[3].textContent = '14/10/2026 12:30';
            });
            const crumbs = document.querySelector('.breadcrumbs');
            crumbs.innerHTML = Array.from({length: 6}, (_, index) =>
                '<div class="d-inline"><a href="#">Level ' + index + '</a></div>').join('');`);
        for (const [width, height] of [[360, 640], [640, 360], [768, 600], [992, 700], [1440, 900], [360, 640]]) {
            const selectedBeforeResize = await evaluate(`savedCheckbox.checked`);
            await viewport(width, height);
            await check(`savedCheckbox.checked === ${selectedBeforeResize}`, 'resize preserves selection');
            await check(`savedListing === document.querySelector('#browserMain table') &&
                savedCheckbox === document.querySelector('.file').closest('tr').querySelector('.browsercheck') &&
                document.querySelectorAll('.browsercheck').length === 2`, 'resize preserves the original listing controls');
            await check(`getComputedStyle(savedFileLink.closest('tr')).display === ${JSON.stringify(width < 768 ? 'grid' : 'table-row')}`, 'cards are a CSS layout of the original rows');
            await check(`Array.from(document.querySelectorAll('.breadcrumbs .d-inline')).filter(item => item.getBoundingClientRect().width > 0).length === ${width < 768 ? 3 : 6}`, 'breadcrumbs restore all ancestors on desktop');
            await check(`document.querySelector('#browserMain table').scrollWidth <= document.getElementById('browserArea').clientWidth`, 'long filename does not overflow the listing');
            await evaluate(`savedCheckbox.focus()`);
            await key(' ', 'Space');
            await check(`window.selectedPaths.length === (savedCheckbox.checked ? 1 : 0) &&
                window.selectionCalls > 0`, 'native checkbox updates the single selection');
            const selectionBeforeDownload = await evaluate(`savedCheckbox.checked`);
            await evaluate(`savedFileLink.focus()`);
            await key('Enter', 'Enter');
            await check(`window.downloadedPath === 'file-path' && savedCheckbox.checked === ${selectionBeforeDownload} &&
                savedFileLink === document.activeElement`, 'native file link downloads without changing selection');
            await evaluate(`savedCheckbox.checked = true`);
            await evaluate(`document.querySelector('.fileTreeRef').focus()`);
            await key('Enter', 'Enter');
            await check(`window.navigatedTo === 'folder-path'`, 'native folder link navigates by keyboard');
            await evaluate(`document.querySelectorAll('thead a')[1].focus()`);
            await key('Enter', 'Enter');
            await check(`window.sortedBy === 'sizeAsc'`, 'sorting remains available on mobile');
            const { nodes } = await send('Accessibility.getFullAXTree', {}, sessionId);
            assert.ok(nodes.some(node => node.role?.value === 'table'), 'responsive listing retains table semantics');
        }
        await evaluate(`listingApp.isInDrive = false; listingApp.updateDriveContextButtons(false)`);
        await check(`['toolbar-download', 'mobile-download', 'mobile-toolbar-download', 'mobile-paste'].every(id => document.getElementById(id).disabled)`, 'desktop and mobile share disabled drive permissions');
        await evaluate(`listingApp.isInDrive = true; listingApp.updateDriveContextButtons(true)`);
        await check(`['toolbar-download', 'mobile-download', 'mobile-toolbar-download', 'mobile-paste'].every(id => !document.getElementById(id).disabled)`, 'desktop and mobile share active file permissions');
        await evaluate(`document.querySelector('.fileTreeRef').closest('tr').querySelector('.browsercheck').checked = true;
            listingApp.updateToolbarButtons(true)`);
        await check(`['toolbar-download', 'mobile-download', 'mobile-toolbar-download'].every(id => !document.getElementById(id).disabled && /zip/.test(document.getElementById(id).getAttribute('aria-label')))`, 'mixed selection offers ZIP download in every view');
        await viewport(360, 640);
        await evaluate(`document.getElementById('browserMain').innerHTML = ${JSON.stringify(listing)};`);
        await check(`getComputedStyle(document.querySelector('#browserMain tbody tr')).display === 'grid' &&
            !document.querySelector('.mobile-file-list')`, 'AJAX replacement immediately uses cards without copies');

    } finally {
        clearTimeout(timeout);
        browser.kill();
        await new Promise(resolve => browser.once('exit', resolve));
    }
}

// Run with: node --test src/test/js/details-manager.test.cjs
// Requires Chromium (CHROME_BIN can override the executable).
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const { readFileSync, writeFileSync, mkdtempSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { resolve, join } = require('node:path');

const webapp = resolve(__dirname, '../../main/webapp');

test('details lifecycle in Chromium: repeated mounts, media and modal cleanup', () => {
    const directory = mkdtempSync(join(tmpdir(), 'details-lifecycle-'));
    const fixture = `
        <div class="details-card">
            <div class="image-preview-container"><img class="details-image" data-fullsize="https://example.test/image"></div>
            <button class="btn-view-fullsize">Image</button>
            <a class="btn-download" href="/download">Download</a>
            <button id="detail-zip-download">ZIP</button>
            <button id="detail-view-images">Slideshow</button>
        </div>
        <div id="imageModal" class="image-modal" hidden><button class="image-modal-close">Close</button><img id="modalImage"></div>
        <div id="slideshowData">
            <div class="slideshow-image-data" data-src="/one" data-title="One"></div>
            <div class="slideshow-image-data" data-src="/two" data-title="Two"></div>
            <div class="slideshow-image-data" data-src="/three" data-title="Three"></div>
        </div>
        <div id="slideshowModal" class="slideshow-modal" hidden>
            <button class="slideshow-close">Close</button>
            <button class="slideshow-prev">Previous</button><button class="slideshow-next">Next</button>
            <img id="slideshowImage"><span id="slideCounter"></span>
            <button id="slideshowAutoplay" aria-pressed="false">▶ Lecture auto</button>
        </div>
        <audio id="playlistAudio"></audio><span id="playlistCounter"></span>
        <button id="playlistPrev">Previous</button><button id="playlistNext">Next</button>
        <ul id="playlistTracks">
            <li class="playlist-track" data-index="0">One</li>
            <li class="playlist-track" data-index="1">Two</li>
            <li class="playlist-track" data-index="2">Three</li>
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
        const key = value => document.dispatchEvent(new KeyboardEvent('keydown', { key: value, bubbles: true }));
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
            click('#slideshowAutoplay');
            check(window.intervals.size === 0, 'autoplay toggle failed');
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
        check(!root.querySelector('#imageModal').hidden, 'image did not open');
        key('Escape');
        check(root.querySelector('#imageModal').hidden && !document.body.classList.contains('modal-open'), 'image close left scroll locked');
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
    const html = `<!doctype html><style>${readFileSync(join(webapp, 'css/details-modern.css'), 'utf8')}</style>
        <button id="toolbar-zip">ZIP</button><div id="detailArea">${fixture}</div><pre id="result">PENDING</pre>
        <script>${setup}</script><script>${readFileSync(join(webapp, 'js/details-manager.js'), 'utf8')}</script>
        <script>document.addEventListener('DOMContentLoaded', () => {
            try { (${checks})(${JSON.stringify(fixture)}); }
            catch (error) { document.getElementById('result').textContent = error.stack; }
        });</script>`;
    try {
        const file = join(directory, 'test.html');
        writeFileSync(file, html);
        const output = execFileSync(process.env.CHROME_BIN || 'chromium', [
            '--headless', '--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage',
            `--user-data-dir=${join(directory, 'profile')}`, '--dump-dom', `file://${file}`
        ], { encoding: 'utf8', timeout: 20000, stdio: ['ignore', 'pipe', 'pipe'] });
        const result = output.match(/<pre id="result">([\s\S]*?)<\/pre>/)?.[1];
        assert.equal(result, 'PASS');
    } finally {
        rmSync(directory, { recursive: true, force: true });
    }
});

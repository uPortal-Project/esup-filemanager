/** Details panel: mount once, destroy before replacing its contents. */
(function() {
    'use strict';

    function sanitizeMediaUrl(url) {
        if (typeof url !== 'string' || !url.trim()) return null;
        try {
            const parsed = new URL(url.trim(), window.location.href);
            return ['http:', 'https:'].includes(parsed.protocol) ? parsed.href : null;
        } catch (e) {
            return null;
        }
    }

    const DetailsManager = {
        root: null,
        events: null,
        images: [],
        tracks: [],
        imageIndex: 0,
        trackIndex: 0,
        autoplayInterval: null,

        find(selector) {
            return this.root?.querySelector(selector);
        },

        listen(target, type, handler) {
            target?.addEventListener(type, handler, { signal: this.events.signal });
        },

        mount(root) {
            if (root === this.root) return;
            this.destroy();
            if (!root) return;

            this.root = root;
            this.events = new AbortController();
            this.mediaStates = new WeakMap();
            this.images = Array.from(root.querySelectorAll('.slideshow-image-data'), el => ({
                src: el.dataset.src,
                title: el.dataset.title
            }));
            this.tracks = Array.from(root.querySelectorAll('.playlist-track-data'), el => ({
                src: el.dataset.src
            }));

            this.listen(root, 'click', event => this.handleClick(event));
            this.listen(root, 'keydown', event => this.handleKeydown(event));
            root.querySelectorAll('dialog').forEach(modal => {
                this.listen(modal, 'cancel', event => {
                    event.preventDefault();
                    this.closeModal(modal);
                });
            });
            this.listen(this.find('#playlistAudio'), 'ended', () => this.changeTrack(1));

            root.querySelectorAll('.details-image, #modalImage, #slideshowImage, audio, video').forEach(media => {
                const image = media.tagName === 'IMG';
                const status = document.createElement('div');
                status.className = 'details-media-status';
                const message = document.createElement('span');
                message.setAttribute('role', 'status');
                const retry = document.createElement('button');
                retry.type = 'button';
                retry.className = 'btn-action';
                retry.textContent = window.i18n?.detailsRetry || 'Retry';
                status.append(message, retry);
                const container = media.closest('.image-preview-container') || media;
                container.after(status);
                this.mediaStates.set(media, { status, message, retry, request: 0 });
                this.listen(retry, 'click', () => {
                    if (image) this.setImage(media, media.dataset.source);
                    else {
                        this.setMediaState(media, 'loading');
                        media.load();
                        this.play(media);
                    }
                });
                this.listen(media, image ? 'load' : 'canplay', () => {
                    if (!this.mediaStates.get(media).blocked) this.setMediaState(media, 'ready');
                });
                if (!image) {
                    this.listen(media, 'loadstart', () => this.setMediaState(media, 'loading'));
                    this.listen(media, 'waiting', () => this.setMediaState(media, 'loading'));
                    this.listen(media, 'playing', () => this.setMediaState(media, 'ready'));
                }
                this.listen(media, 'error', () => {
                    if (media.id === 'slideshowImage') this.stopAutoplay();
                    if (!image || media.getAttribute('src')) this.setMediaState(media, 'error');
                });
                if (image && media.getAttribute('src')) {
                    media.dataset.source = media.getAttribute('src');
                    this.setMediaState(media, media.complete ? (media.naturalWidth ? 'ready' : 'error') : 'loading');
                } else {
                    this.setMediaState(media, image ? 'idle' : media.error ? 'error' :
                        media.readyState >= 3 ? 'ready' : 'loading');
                }
            });
            this.loadTrack(0, false);
        },

        destroy() {
            // Detach handlers before load() emits media events or the DOM is replaced.
            this.events?.abort();
            this.suspend(false);
            this.root?.querySelectorAll('audio, video').forEach(player => {
                player.removeAttribute('src');
                player.querySelectorAll('source').forEach(source => source.removeAttribute('src'));
                player.load();
            });
            this.root?.querySelectorAll('.details-media-status').forEach(status => status.remove());
            this.mediaStates = null;
            this.root = null;
            this.events = null;
            this.images = [];
            this.tracks = [];
            this.imageIndex = 0;
            this.trackIndex = 0;
        },

        suspend(restoreFocus = true) {
            this.closeModal(this.find('#imageModal'), restoreFocus);
            this.closeModal(this.find('#slideshowModal'), restoreFocus);
            this.root?.querySelectorAll('audio, video').forEach(player => {
                this.mediaStates.get(player).request++;
                player.pause();
            });
        },

        openModal(modal, trigger) {
            if (!modal || modal.open) return;
            this.returnFocus = trigger;
            modal.showModal();
            document.body.classList.add('modal-open');
        },

        closeModal(modal, restoreFocus = true) {
            if (modal?.open) {
                modal.close();
                const img = modal.querySelector('img');
                img?.removeAttribute('src');
                if (img) this.setMediaState(img, 'idle');
                const target = restoreFocus && this.returnFocus?.isConnected
                    ? this.returnFocus : document.getElementById('treeSplitter');
                target?.focus();
                this.returnFocus = null;
            }
            if (modal?.id === 'slideshowModal' || !modal) this.stopAutoplay();
            // Bootstrap dialogs may also own the body scroll lock.
            if (!document.querySelector('dialog[open], .modal.show')) {
                document.body.classList.remove('modal-open');
            }
        },

        setMediaState(media, state, key) {
            const entry = this.mediaStates?.get(media);
            if (!entry) return;
            const image = media.tagName === 'IMG';
            entry.blocked = key === 'detailsMediaPlayBlocked';
            media.dataset.state = state;
            media.setAttribute('aria-busy', String(state === 'loading'));
            entry.status.hidden = state === 'ready' || state === 'idle';
            entry.status.classList.toggle('media-error', state === 'error');
            entry.retry.hidden = state !== 'error';
            entry.message.textContent = entry.status.hidden ? '' : window.i18n?.[key ||
                (state === 'loading' ? 'detailsMediaLoading' : image ? 'detailsImageError' : 'detailsMediaError')] ||
                (state === 'loading' ? 'Loading preview…' : 'Preview unavailable. Retry or download the file.');
            if (media.matches('.details-image')) {
                const dimensions = this.find('#image_dimensions');
                if (dimensions) dimensions.textContent = state === 'ready'
                    ? `${media.naturalWidth} px × ${media.naturalHeight} px` : '—';
            }
        },

        setImage(img, source) {
            if (!img) return;
            img.removeAttribute('src');
            img.dataset.source = source || '';
            const url = sanitizeMediaUrl(source);
            this.setMediaState(img, url ? 'loading' : 'error');
            if (url) img.src = url;
            else if (img.id === 'slideshowImage') this.stopAutoplay();
        },

        play(media) {
            const events = this.events;
            const entry = this.mediaStates.get(media);
            const request = ++entry.request;
            media.play().catch(error => {
                // Replacing a track or destroying the panel interrupts pending play().
                if (events.signal.aborted || request !== entry.request || error.name === 'AbortError') return;
                this.setMediaState(media, 'error', error.name === 'NotAllowedError'
                    ? 'detailsMediaPlayBlocked' : 'detailsMediaError');
            });
        },

        openImage(control) {
            const img = control.closest('.details-card')?.querySelector('.details-image');
            const modalImg = this.find('#modalImage');
            if (!modalImg) return;
            modalImg.alt = img?.alt || '';
            this.setImage(modalImg, img?.dataset.fullsize);
            this.openModal(this.find('#imageModal'), control);
        },

        showImage(index) {
            if (!Number.isInteger(index) || index < 0 || index >= this.images.length) return;
            this.imageIndex = index;
            const img = this.find('#slideshowImage');
            if (img) {
                this.setImage(img, this.images[index].src);
                img.alt = this.images[index].title;
            }
            const counter = this.find('#slideCounter');
            if (counter) counter.textContent = `${index + 1} / ${this.images.length}`;
        },

        changeImage(delta) {
            this.showImage((this.imageIndex + delta + this.images.length) % this.images.length);
        },

        toggleAutoplay() {
            if (this.autoplayInterval !== null) this.stopAutoplay();
            else {
                this.autoplayInterval = setInterval(() => this.changeImage(1), 3000);
                this.renderAutoplay();
            }
        },

        stopAutoplay() {
            clearInterval(this.autoplayInterval);
            this.autoplayInterval = null;
            this.renderAutoplay();
        },

        renderAutoplay() {
            const playing = this.autoplayInterval !== null;
            const button = this.find('#slideshowAutoplay');
            if (button) {
                button.textContent = playing ? button.dataset.pause : button.dataset.play;
                button.classList.toggle('playing', playing);
                button.setAttribute('aria-pressed', String(playing));
            }
            this.find('#slideCounter')?.setAttribute('aria-live', playing ? 'off' : 'polite');
        },

        loadTrack(index, autoplay) {
            const audio = this.find('#playlistAudio');
            if (!audio || !Number.isInteger(index) || index < 0 || index >= this.tracks.length) return;
            const source = sanitizeMediaUrl(this.tracks[index]?.src);
            this.mediaStates.get(audio).request++;
            audio.pause();
            audio.removeAttribute('src');
            this.trackIndex = index;
            this.setMediaState(audio, source ? 'loading' : 'error');
            if (source) audio.src = source;
            audio.load();
            if (source && autoplay) this.play(audio);

            const counter = this.find('#playlistCounter');
            if (counter) counter.textContent = `${index + 1} / ${this.tracks.length}`;
            this.root.querySelectorAll('#playlistTracks .playlist-track').forEach((item, i) => {
                if (i === index) item.setAttribute('aria-current', 'true');
                else item.removeAttribute('aria-current');
            });
        },

        changeTrack(delta) {
            this.loadTrack((this.trackIndex + delta + this.tracks.length) % this.tracks.length, true);
        },

        handleClick(event) {
            const control = event.target.closest('button, .btn-download, dialog');
            if (!control || !this.root.contains(control)) return;

            if (control.matches('.btn-download')) {
                const href = control.getAttribute('href');
                if (href && window.esupFileManager?.triggerDownload) {
                    event.preventDefault();
                    window.esupFileManager.triggerDownload(href);
                }
            } else if (control.matches('.btn-view-fullsize, .image-preview-container')) {
                this.openImage(control);
            } else if (control.matches('.image-modal-close, .slideshow-close')) {
                this.closeModal(control.closest('.image-modal, .slideshow-modal'));
            } else if (control.matches('.image-modal, .slideshow-modal') && event.target === control) {
                this.closeModal(control);
            } else if (control.id === 'detail-view-images' && this.images.length) {
                this.showImage(0);
                this.openModal(this.find('#slideshowModal'), control);
            } else if (control.matches('.slideshow-prev, .slideshow-next')) {
                this.changeImage(control.matches('.slideshow-prev') ? -1 : 1);
            } else if (control.id === 'slideshowAutoplay') {
                this.toggleAutoplay();
            } else if (control.matches('.playlist-track')) {
                this.loadTrack(Number(control.dataset.index), true);
            } else if (control.id === 'playlistPrev' || control.id === 'playlistNext') {
                this.changeTrack(control.id === 'playlistPrev' ? -1 : 1);
            } else if (control.id === 'detail-zip-download') {
                const button = document.getElementById('toolbar-download');
                if (button && !button.disabled) button.click();
            }
        },

        handleKeydown(event) {
            const slideshow = this.find('#slideshowModal');
            if (!slideshow?.open || event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
            if (event.target.closest('input, select, textarea, audio, video, [contenteditable]')) return;
            if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
                event.preventDefault();
                this.changeImage(event.key === 'ArrowLeft' ? -1 : 1);
            }
        }
    };

    window.detailsManager = DetailsManager;
    const mount = () => DetailsManager.mount(document.getElementById('detailArea'));
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mount, { once: true });
    } else {
        mount();
    }
    window.addEventListener('pagehide', event => {
        if (event.persisted) DetailsManager.suspend();
        else DetailsManager.destroy();
    });
    window.addEventListener('pageshow', mount);
})();

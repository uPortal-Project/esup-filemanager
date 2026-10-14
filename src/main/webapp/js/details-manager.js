/** Details panel: mount once, destroy before replacing its contents. */
(function() {
    'use strict';

    function sanitizeImageUrl(url) {
        if (!url || typeof url !== 'string') return null;
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
            this.images = Array.from(root.querySelectorAll('.slideshow-image-data'), el => ({
                src: el.dataset.src,
                title: el.dataset.title
            }));
            this.tracks = Array.from(root.querySelectorAll('.playlist-track-data'), el => ({
                src: el.dataset.src,
                mime: el.dataset.mime
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

            const img = this.find('.details-image');
            const updateDimensions = () => {
                const width = this.find('#image_width');
                const height = this.find('#image_height');
                if (width) width.textContent = img.naturalWidth;
                if (height) height.textContent = img.naturalHeight;
            };
            this.listen(img, 'load', updateDimensions);
            if (img?.complete) updateDimensions();

            root.querySelectorAll('audio, video').forEach(player => {
                this.listen(player, 'error', () => {
                    const container = player.closest('.media-player-container');
                    if (!container || container.querySelector('.media-error')) return;
                    const message = document.createElement('div');
                    message.className = 'media-error';
                    message.textContent = 'Media file playback error';
                    container.appendChild(message);
                });
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
            this.root?.querySelectorAll('audio, video').forEach(player => player.pause());
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
                modal.querySelector('img')?.removeAttribute('src');
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

        openImage(control) {
            const img = control.closest('.details-card')?.querySelector('.details-image');
            const modalImg = this.find('#modalImage');
            const safeUrl = sanitizeImageUrl(img?.dataset.fullsize);
            if (!modalImg || !safeUrl) return;
            modalImg.src = safeUrl;
            modalImg.alt = img.alt;
            this.openModal(this.find('#imageModal'), control);
        },

        showImage(index) {
            if (!Number.isInteger(index) || index < 0 || index >= this.images.length) return;
            this.imageIndex = index;
            const img = this.find('#slideshowImage');
            if (img) {
                img.src = this.images[index].src;
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
            this.trackIndex = index;
            const track = this.tracks[index];
            audio.src = track.src;
            if (track.mime) audio.type = track.mime;
            audio.load();
            if (autoplay) audio.play().catch(() => {});

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
                const button = document.getElementById('toolbar-zip');
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

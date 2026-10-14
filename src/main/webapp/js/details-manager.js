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
            this.autoplayLabel = this.find('#slideshowAutoplay')?.textContent;

            this.listen(root, 'click', event => this.handleClick(event));
            this.listen(document, 'keydown', event => this.handleKeydown(event));
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
            this.suspend();
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
            this.autoplayLabel = null;
        },

        suspend() {
            this.closeModal(this.find('#imageModal'));
            this.closeModal(this.find('#slideshowModal'));
            this.root?.querySelectorAll('audio, video').forEach(player => player.pause());
        },

        openModal(modal) {
            if (!modal) return;
            modal.hidden = false;
            document.body.classList.add('modal-open');
        },

        closeModal(modal) {
            if (modal) {
                modal.hidden = true;
                modal.querySelector('img')?.removeAttribute('src');
            }
            if (modal?.id === 'slideshowModal' || !modal) this.stopAutoplay();
            // Bootstrap dialogs may also own the body scroll lock.
            if (!document.querySelector('.image-modal:not([hidden]), .slideshow-modal:not([hidden]), .modal.show')) {
                document.body.classList.remove('modal-open');
            }
        },

        openImage(control) {
            const img = control.closest('.details-card')?.querySelector('.details-image');
            const modalImg = this.find('#modalImage');
            const safeUrl = sanitizeImageUrl(img?.dataset.fullsize);
            if (!modalImg || !safeUrl) return;
            modalImg.src = safeUrl;
            this.openModal(this.find('#imageModal'));
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
            if (this.autoplayInterval !== null) {
                this.stopAutoplay();
                return;
            }
            this.autoplayInterval = setInterval(() => this.changeImage(1), 3000);
            const button = this.find('#slideshowAutoplay');
            if (button) {
                button.textContent = '⏸ Pause';
                button.classList.add('playing');
                button.setAttribute('aria-pressed', 'true');
            }
        },

        stopAutoplay() {
            clearInterval(this.autoplayInterval);
            this.autoplayInterval = null;
            const button = this.find('#slideshowAutoplay');
            if (button) {
                button.textContent = this.autoplayLabel;
                button.classList.remove('playing');
                button.setAttribute('aria-pressed', 'false');
            }
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
                item.classList.toggle('active', i === index);
                item.setAttribute('aria-selected', String(i === index));
            });
        },

        changeTrack(delta) {
            this.loadTrack((this.trackIndex + delta + this.tracks.length) % this.tracks.length, true);
        },

        handleClick(event) {
            const control = event.target.closest('button, .btn-download, .image-preview-container, .playlist-track, .image-modal, .slideshow-modal');
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
                this.openModal(this.find('#slideshowModal'));
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
            const image = this.find('#imageModal');
            if (event.key === 'Escape') {
                if (slideshow && !slideshow.hidden) this.closeModal(slideshow);
                if (image && !image.hidden) this.closeModal(image);
            } else if (slideshow && !slideshow.hidden) {
                if (event.key === 'ArrowLeft') this.changeImage(-1);
                if (event.key === 'ArrowRight') this.changeImage(1);
                if (event.key === ' ' && !event.target.closest('button, input, select, textarea, audio, video, [contenteditable]')) {
                    event.preventDefault();
                    this.toggleAutoplay();
                }
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

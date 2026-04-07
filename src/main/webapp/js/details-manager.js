/**
 * Details Manager - Vanilla JavaScript
 * Modern details block management without libraries
 */

(function() {
    'use strict';

    // ==================== Image Viewer ====================

    /**
     * Handler for displaying images in full size
     */
    window.viewImageFullsize = function(button) {
        const card = button.closest('.details-card');
        const img = card.querySelector('.details-image');

        if (!img) return;

        const modal = document.getElementById('imageModal');
        const modalImg = document.getElementById('modalImage');

        if (modal && modalImg) {
            modal.style.display = 'flex';
            modalImg.src = img.getAttribute('data-fullsize') || img.src;
            document.body.style.overflow = 'hidden';
        }
    };

    /**
     * Initializes image viewer on preview click
     */
    function initImagePreviewClick() {
        const imageContainer = document.querySelector('.image-preview-container');
        if (imageContainer) {
            imageContainer.addEventListener('click', function() {
                const img = this.querySelector('.details-image');
                if (img) {
                    const modal = document.getElementById('imageModal');
                    const modalImg = document.getElementById('modalImage');

                    if (modal && modalImg) {
                        modal.style.display = 'flex';
                        modalImg.src = img.getAttribute('data-fullsize') || img.src;
                        document.body.style.overflow = 'hidden';
                    }
                }
            });
        }
    }

    /**
     * Image modal close handler
     */
    function initImageModalClose() {
        const modal = document.getElementById('imageModal');
        if (!modal) return;

        const closeBtn = modal.querySelector('.image-modal-close');

        if (closeBtn) {
            closeBtn.addEventListener('click', function() {
                modal.style.display = 'none';
                document.body.style.overflow = '';
            });
        }

        // Close on click outside image
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                modal.style.display = 'none';
                document.body.style.overflow = '';
            }
        });

        // Close with Escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && modal.style.display === 'flex') {
                modal.style.display = 'none';
                document.body.style.overflow = '';
            }
        });
    }

    /**
     * Detects and displays image dimensions
     */
    function initImageDimensionDetection() {
        const img = document.querySelector('.details-image');
        const widthSpan = document.getElementById('image_width');
        const heightSpan = document.getElementById('image_height');

        if (img && widthSpan && heightSpan) {
            img.addEventListener('load', function() {
                widthSpan.textContent = this.naturalWidth;
                heightSpan.textContent = this.naturalHeight;
            });

            // If image is already loaded
            if (img.complete) {
                widthSpan.textContent = img.naturalWidth;
                heightSpan.textContent = img.naturalHeight;
            }
        }
    }

    // ==================== Slideshow Manager ====================

    const SlideshowManager = {
        images: [],
        currentIndex: 0,
        autoplayInterval: null,
        autoplayDelay: 3000,

        /**
         * Initializes slideshow
         */
        init: function() {
            const viewImagesBtn = document.getElementById('detail-view-images');
            if (!viewImagesBtn) {
                console.log('Slideshow: button not found');
                return;
            }

            console.log('Slideshow: initializing...');

            // Load image data
            this.loadImages();

            console.log(`Slideshow: ${this.images.length} image(s) loaded`);

            // Remove old listeners
            const newBtn = viewImagesBtn.cloneNode(true);
            viewImagesBtn.parentNode.replaceChild(newBtn, viewImagesBtn);

            // Event to open slideshow
            newBtn.addEventListener('click', () => {
                console.log('Slideshow: opening...');
                this.open();
            });

            // Modal events
            this.initModalEvents();
        },

        /**
         * Loads images from data
         */
        loadImages: function() {
            const slideshowData = document.getElementById('slideshowData');
            if (!slideshowData) return;

            const imageElements = slideshowData.querySelectorAll('.slideshow-image-data');
            this.images = Array.from(imageElements).map(el => ({
                src: el.getAttribute('data-src'),
                title: el.getAttribute('data-title')
            }));
        },

        /**
         * Opens slideshow
         */
        open: function() {
            if (this.images.length === 0) return;

            const modal = document.getElementById('slideshowModal');
            if (!modal) return;

            this.currentIndex = 0;
            modal.style.display = 'flex';
            document.body.style.overflow = 'hidden';
            this.showImage(0);
        },

        /**
         * Closes slideshow
         */
        close: function() {
            const modal = document.getElementById('slideshowModal');
            if (modal) {
                modal.style.display = 'none';
                document.body.style.overflow = '';
                this.stopAutoplay();
            }
        },

        /**
         * Displays image at given index
         */
        showImage: function(index) {
            if (index < 0 || index >= this.images.length) return;

            this.currentIndex = index;
            const img = document.getElementById('slideshowImage');
            const counter = document.getElementById('slideCounter');

            if (img) {
                img.src = this.images[index].src;
                img.alt = this.images[index].title;
            }

            if (counter) {
                counter.textContent = `${index + 1} / ${this.images.length}`;
            }
        },

        /**
         * Previous image
         */
        prev: function() {
            const newIndex = this.currentIndex - 1;
            this.showImage(newIndex < 0 ? this.images.length - 1 : newIndex);
        },

        /**
         * Next image
         */
        next: function() {
            const newIndex = this.currentIndex + 1;
            this.showImage(newIndex >= this.images.length ? 0 : newIndex);
        },

        /**
         * Toggle autoplay
         */
        toggleAutoplay: function() {
            if (this.autoplayInterval) {
                this.stopAutoplay();
            } else {
                this.startAutoplay();
            }
        },

        /**
         * Starts automatic playback
         */
        startAutoplay: function() {
            const btn = document.getElementById('slideshowAutoplay');
            if (btn) {
                btn.textContent = '⏸ Pause';
                btn.classList.add('playing');
            }

            this.autoplayInterval = setInterval(() => {
                this.next();
            }, this.autoplayDelay);
        },

        /**
         * Stops automatic playback
         */
        stopAutoplay: function() {
            const btn = document.getElementById('slideshowAutoplay');
            if (btn) {
                btn.textContent = '▶ Auto play';
                btn.classList.remove('playing');
            }

            if (this.autoplayInterval) {
                clearInterval(this.autoplayInterval);
                this.autoplayInterval = null;
            }
        },

        /**
         * Initializes modal events
         */
        initModalEvents: function() {
            const modal = document.getElementById('slideshowModal');
            if (!modal) return;

            // Close button
            const closeBtn = modal.querySelector('.slideshow-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', () => this.close());
            }

            // Navigation buttons
            const prevBtn = modal.querySelector('.slideshow-prev');
            const nextBtn = modal.querySelector('.slideshow-next');

            if (prevBtn) {
                prevBtn.addEventListener('click', () => this.prev());
            }

            if (nextBtn) {
                nextBtn.addEventListener('click', () => this.next());
            }

            // Autoplay button
            const autoplayBtn = document.getElementById('slideshowAutoplay');
            if (autoplayBtn) {
                autoplayBtn.addEventListener('click', () => this.toggleAutoplay());
            }

            // Close on click outside
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.close();
                }
            });

            // Keyboard navigation
            document.addEventListener('keydown', (e) => {
                if (modal.style.display !== 'flex') return;

                switch(e.key) {
                    case 'Escape':
                        this.close();
                        break;
                    case 'ArrowLeft':
                        this.prev();
                        break;
                    case 'ArrowRight':
                        this.next();
                        break;
                    case ' ':
                        e.preventDefault();
                        this.toggleAutoplay();
                        break;
                }
            });
        }
    };

    // ==================== Audio Playlist Manager ====================

    const AudioPlaylistManager = {
        tracks: [],
        currentIndex: 0,

        /**
         * Initializes the audio playlist
         */
        init: function() {
            const audioEl = document.getElementById('playlistAudio');
            if (!audioEl) return;

            // Load track data
            this.loadTracks();
            if (this.tracks.length === 0) return;

            console.log(`Playlist: ${this.tracks.length} track(s) loaded`);

            // Load first track
            this.loadTrack(0, false);

            // Bind track list click
            const trackList = document.getElementById('playlistTracks');
            if (trackList) {
                const newList = trackList.cloneNode(true);
                trackList.parentNode.replaceChild(newList, trackList);
                newList.addEventListener('click', (e) => {
                    const li = e.target.closest('.playlist-track');
                    if (li) {
                        const index = parseInt(li.getAttribute('data-index'), 10);
                        this.loadTrack(index, true);
                    }
                });
            }

            // Bind prev / next buttons
            const prevBtn = document.getElementById('playlistPrev');
            const nextBtn = document.getElementById('playlistNext');

            if (prevBtn) {
                const newPrev = prevBtn.cloneNode(true);
                prevBtn.parentNode.replaceChild(newPrev, prevBtn);
                newPrev.addEventListener('click', () => this.prev());
            }
            if (nextBtn) {
                const newNext = nextBtn.cloneNode(true);
                nextBtn.parentNode.replaceChild(newNext, nextBtn);
                newNext.addEventListener('click', () => this.next());
            }

            // Auto-advance to next track when current ends
            audioEl.addEventListener('ended', () => this.next());
        },

        /**
         * Loads track metadata from hidden DOM elements
         */
        loadTracks: function() {
            const playlistData = document.getElementById('playlistData');
            if (!playlistData) return;

            const items = playlistData.querySelectorAll('.playlist-track-data');
            this.tracks = Array.from(items).map(el => ({
                src:   el.getAttribute('data-src'),
                title: el.getAttribute('data-title'),
                mime:  el.getAttribute('data-mime')
            }));
        },

        /**
         * Loads and optionally plays the track at the given index
         * @param {number} index
         * @param {boolean} autoplay
         */
        loadTrack: function(index, autoplay) {
            if (index < 0 || index >= this.tracks.length) return;
            this.currentIndex = index;

            const track = this.tracks[index];
            const audioEl = document.getElementById('playlistAudio');
            if (audioEl) {
                audioEl.src = track.src;
                if (track.mime) audioEl.type = track.mime;
                audioEl.load();
                if (autoplay) audioEl.play().catch(() => {});
            }

            // Update counter
            const counter = document.getElementById('playlistCounter');
            if (counter) {
                counter.textContent = `${index + 1} / ${this.tracks.length}`;
            }

            // Highlight active track in list
            const items = document.querySelectorAll('#playlistTracks .playlist-track');
            items.forEach((li, i) => {
                if (i === index) {
                    li.classList.add('active');
                } else {
                    li.classList.remove('active');
                }
            });
        },

        /**
         * Previous track
         */
        prev: function() {
            const newIndex = this.currentIndex - 1;
            this.loadTrack(newIndex < 0 ? this.tracks.length - 1 : newIndex, true);
        },

        /**
         * Next track
         */
        next: function() {
            const newIndex = this.currentIndex + 1;
            this.loadTrack(newIndex >= this.tracks.length ? 0 : newIndex, true);
        }
    };

    // ==================== Zip Download Manager ====================

    /**
     * ZIP download management
     */
    function initZipDownload() {
        const zipBtn = document.getElementById('detail-zip-download');
        if (!zipBtn) return;

        // Remove old listeners
        const newBtn = zipBtn.cloneNode(true);
        zipBtn.parentNode.replaceChild(newBtn, zipBtn);

        newBtn.addEventListener('click', function() {
            console.log('ZIP download requested from details');

            // Trigger ZIP download via main toolbar button
            const toolbarZipBtn = document.getElementById('toolbar-zip');
            if (toolbarZipBtn && !toolbarZipBtn.disabled) {
                toolbarZipBtn.click();
            } else {
                console.warn('Toolbar ZIP button not available');
            }
        });
    }

    // ==================== Media Player Enhancements ====================

    /**
     * Enhances audio/video players with additional features
     */
    function enhanceMediaPlayers() {
        const audioPlayers = document.querySelectorAll('.audio-player');
        const videoPlayers = document.querySelectorAll('.video-player');

        // Error handling for players
        [...audioPlayers, ...videoPlayers].forEach(player => {
            player.addEventListener('error', function(e) {
                console.error('Media playback error:', e);
                const container = this.closest('.media-player-container');
                if (container) {
                    const errorMsg = document.createElement('div');
                    errorMsg.className = 'media-error';
                    errorMsg.style.cssText = 'color: #f44336; padding: 10px; text-align: center;';
                    errorMsg.textContent = 'Media file playback error';
                    container.appendChild(errorMsg);
                }
            });

            // Log when media is ready
            player.addEventListener('loadedmetadata', function() {
                console.log('Media loaded:', this.duration, 'seconds');
            });
        });
    }

    // ==================== Initialization ====================

    /**
     * Initialization on DOM load
     */
    function init() {
        initImagePreviewClick();
        initImageModalClose();
        initImageDimensionDetection();
        SlideshowManager.init();
        AudioPlaylistManager.init();
        initZipDownload();
        enhanceMediaPlayers();
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Reinitialization after AJAX loading of detailsArea
    window.reinitDetailsManager = init;

})();


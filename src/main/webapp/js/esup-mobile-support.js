/**
 * ESUP File Manager - Mobile and Responsive Support
 * Mobile-specific interactions and responsive adaptations management
 */

(function() {
    'use strict';

    /**
     * Device type detection
     */
    const isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);

    /**
     * Initialization on DOM load
     */
    document.addEventListener('DOMContentLoaded', function() {
        initMobileAdaptations();
        initMobileToolbar();
        initMobileDetails();
        initMobileMenu();
        initTouchGestures();
        syncTreeWithMobile();
    });

    /**
     * Mobile-specific adaptations
     */
    function initMobileAdaptations() {
        // Viewport height fix for mobile (URL bar issue)
        const setVH = () => {
            const viewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
            const vh = viewportHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);
        };

        setVH();
        window.addEventListener('resize', setVH);
        window.addEventListener('orientationchange', setVH);
        if (window.visualViewport) {
            window.visualViewport.addEventListener('resize', setVH);
        }
    }

    /**
     * Mobile toolbar (bottom)
     */
    function initMobileToolbar() {
        // Reserve the actual height, including wrapped labels and the safe area.
        const toolbar = document.querySelector('.fixed-bottom');
        if (toolbar) {
            const observer = new ResizeObserver(() => {
                const height = toolbar.getBoundingClientRect().height;
                if (height > 0) {
                    document.documentElement.style.setProperty('--mobile-toolbar-offset', `${height}px`);
                }
            });
            observer.observe(toolbar, { box: 'border-box' });
        }

        // Mobile upload
        const uploadBtn = document.getElementById('mobile-toolbar-upload');
        if (uploadBtn) {
            uploadBtn.addEventListener('click', function() {
                const desktopUpload = document.getElementById('toolbar-upload') ||
                                     document.querySelector('#file-uploader input');
                if (desktopUpload) {
                    desktopUpload.click();
                }
            });
        }

        // Mobile new folder
        const newBtn = document.getElementById('mobile-toolbar-new');
        if (newBtn) {
            newBtn.addEventListener('click', function() {
                const desktopNew = document.getElementById('toolbar-new_folder');
                if (desktopNew) {
                    desktopNew.click();
                }
            });
        }

        // Mobile download
        const downloadBtn = document.getElementById('mobile-toolbar-download');
        if (downloadBtn) {
            downloadBtn.addEventListener('click', function() {
                const desktopDownload = document.getElementById('toolbar-download');
                if (desktopDownload && !desktopDownload.classList.contains('disabled')) {
                    desktopDownload.click();
                }
            });
        }

        // Note: le bouton "Menu" (4ème bouton de la barre bas) ouvre l'offcanvas #mobileMenu
        // directement via data-bs-toggle Bootstrap — aucun handler JS nécessaire.
    }

    function initMobileDetails() {
        const button = document.getElementById('mobile-details');
        const dialog = document.getElementById('mobileDetails');
        const details = document.getElementById('detailArea');
        if (!button || !dialog || !details) return;

        const desktopParent = details.parentNode;
        const mobile = window.matchMedia('(max-width: 991.98px)');
        let opener = button;
        const open = source => {
            if (!mobile.matches || dialog.open) return;
            opener = source;
            // Move the existing panel: AJAX and media keep the same root and state.
            dialog.appendChild(details);
            dialog.showModal();
        };
        button.addEventListener('click', () => open(button));
        dialog.addEventListener('esup:open-details', event => open(event.detail.opener));
        const close = () => {
            window.detailsManager?.suspend(false);
            desktopParent.appendChild(details);
            dialog.close();
            if (!document.querySelector('dialog[open], .modal.show')) {
                document.body.classList.remove('modal-open');
            }
            (mobile.matches ? (opener?.isConnected ? opener : button) : document.getElementById('treeSplitter'))?.focus();
        };
        document.getElementById('mobile-details-close').addEventListener('click', close);
        dialog.addEventListener('cancel', event => {
            if (event.target !== dialog) return;
            event.preventDefault();
            close();
        });
        mobile.addEventListener('change', () => {
            if (!mobile.matches && dialog.open) close();
        });
    }

    /**
     * Mobile offcanvas menu
     */
    function initMobileMenu() {
        // Mobile menu quick actions
        const mobileActions = {
            'mobile-upload': 'toolbar-upload',
            'mobile-new-folder': 'toolbar-new_folder',
            'mobile-refresh': 'toolbar-refresh',
            'mobile-download': 'toolbar-download',
            'mobile-copy': 'toolbar-copy',
            'mobile-cut': 'toolbar-cut',
            'mobile-paste': 'toolbar-paste',
            'mobile-rename': 'toolbar-rename',
            'mobile-delete': 'toolbar-delete'
        };

        Object.keys(mobileActions).forEach(mobileId => {
            const mobileBtn = document.getElementById(mobileId);
            const desktopId = mobileActions[mobileId];

            if (mobileBtn) {
                mobileBtn.addEventListener('click', function() {
                    const desktopBtn = document.getElementById(desktopId);
                    if (desktopBtn) {
                        desktopBtn.click();
                    }

                    // Close offcanvas after action
                    const offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById('mobileMenu'));
                    if (offcanvas) {
                        offcanvas.hide();
                    }
                });
            }
        });
    }

    /**
     * Touch gestures
     */
    function initTouchGestures() {
        if (!isTouchDevice) return;

        // Swipe to open/close mobile menu
        let touchStartX = 0;
        let touchEndX = 0;

        document.addEventListener('touchstart', function(e) {
            if (e.target.closest('dialog')) return;
            touchStartX = e.changedTouches[0].screenX;
        }, false);

        document.addEventListener('touchend', function(e) {
            if (e.target.closest('dialog')) return;
            touchEndX = e.changedTouches[0].screenX;
            handleSwipe();
        }, false);

        function handleSwipe() {
            const swipeThreshold = 50;
            const diff = touchEndX - touchStartX;

            // Swipe right (open menu)
            if (diff > swipeThreshold && touchStartX < 50) {
                const offcanvas = new bootstrap.Offcanvas(document.getElementById('mobileMenu'));
                offcanvas.show();
            }

            // Swipe left (close menu)
            if (diff < -swipeThreshold) {
                const offcanvasElement = document.getElementById('mobileMenu');
                const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasElement);
                if (offcanvas) {
                    offcanvas.hide();
                }
            }
        }

        // Long press to show actions (alternative to right-click)
        let pressTimer;
        document.addEventListener('touchstart', function(e) {
            const target = e.target.closest('tr.selectable');
            if (!target) return;

            // Long press → ouvre le menu offcanvas (même comportement que le bouton "Menu")
            pressTimer = setTimeout(function() {
                // Ouvrir le menu offcanvas
                const offcanvasEl = document.getElementById('mobileMenu');
                if (offcanvasEl) {
                    const offcanvas = bootstrap.Offcanvas.getOrCreateInstance(offcanvasEl);
                    offcanvas.show();
                }

                // Vibration feedback if supported
                if ('vibrate' in navigator) {
                    navigator.vibrate(50);
                }
            }, 500);
        });

        document.addEventListener('touchend', function() {
            clearTimeout(pressTimer);
        });

        document.addEventListener('touchmove', function() {
            clearTimeout(pressTimer);
        });
    }

    /**
     * Synchronize mobile tree with desktop
     */
    function syncTreeWithMobile() {
        const desktopTree = document.getElementById('fileTree');
        const mobileTree = document.getElementById('fileTreeMobile');

        if (!desktopTree || !mobileTree) return;

        const copyDesktopTree = function() {
            const fragment = document.createDocumentFragment();
            desktopTree.childNodes.forEach(node => {
                const clone = node.cloneNode(true);
                if (clone.nodeType === Node.ELEMENT_NODE) {
                    clone.removeAttribute('style');
                    clone.querySelectorAll('[style]').forEach(el => el.removeAttribute('style'));
                }
                fragment.appendChild(clone);
            });
            mobileTree.replaceChildren(fragment);
        };

        // Watch for changes in the desktop tree
        const observer = new MutationObserver(function() {
            copyDesktopTree();
        });

        observer.observe(desktopTree, {
            childList: true,
            subtree: true
        });

        // Initial synchronization
        copyDesktopTree();
    }


})();

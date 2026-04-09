/**
 * ESUP File Manager - Mobile and Responsive Support
 * Mobile-specific interactions and responsive adaptations management
 */

(function() {
    'use strict';

    /**
     * Device type detection
     */
    const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    const isTouchDevice = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);

    /**
     * Initialization on DOM load
     */
    document.addEventListener('DOMContentLoaded', function() {
        initMobileAdaptations();
        initMobileToolbar();
        initMobileMenu();
        initTouchGestures();
        initResponsiveBehaviors();
        syncTreeWithMobile();

        console.log('Mobile/Responsive support initialized', {
            isMobile,
            isTouchDevice,
            screenWidth: window.innerWidth
        });
    });

    /**
     * Mobile-specific adaptations
     */
    function initMobileAdaptations() {
        // Add class for CSS detection
        if (isMobile) {
            document.body.classList.add('is-mobile');
        }

        if (isTouchDevice) {
            document.body.classList.add('is-touch');
        }

        // Viewport height fix for mobile (URL bar issue)
        const setVH = () => {
            const vh = window.innerHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);
        };

        setVH();
        window.addEventListener('resize', setVH);
        window.addEventListener('orientationchange', setVH);

        // Prevent double-tap zoom on iOS
        let lastTouchEnd = 0;
        document.addEventListener('touchend', function(event) {
            const now = Date.now();
            if (now - lastTouchEnd <= 300) {
                event.preventDefault();
            }
            lastTouchEnd = now;
        }, false);
    }

    /**
     * Mobile toolbar (bottom)
     */
    function initMobileToolbar() {
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

        // Synchronize button disabled state
        syncMobileToolbarState();
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

        // Synchronize button state
        syncMobileMenuState();
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
            touchStartX = e.changedTouches[0].screenX;
        }, false);

        document.addEventListener('touchend', function(e) {
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
            const target = e.target.closest('.file-card-mobile, tr.selectable');
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
     * Responsive behaviors
     */
    function initResponsiveBehaviors() {
        // Listen for screen size changes
        const mediaQuery = window.matchMedia('(max-width: 991.98px)');

        function handleMediaChange(e) {
            if (e.matches) {
                // Mobile/tablet mode
                adaptForMobile();
            } else {
                // Desktop mode
                adaptForDesktop();
            }
        }

        handleMediaChange(mediaQuery);
        mediaQuery.addListener(handleMediaChange);

        // Adapt tables for mobile
        if (window.innerWidth < 768) {
            convertTableToCards();
        }

        window.addEventListener('resize', debounce(function() {
            if (window.innerWidth < 768) {
                convertTableToCards();
            } else {
                restoreTableLayout();
            }
        }, 250));
    }

    /**
     * Update mobile breadcrumbs (shorten if needed)
     */
    function updateMobileBreadcrumbs() {
        const breadcrumbs = document.querySelectorAll('.breadcrumb-item');
        if (breadcrumbs.length > 3) {
            // Hide intermediate items to save space
            for (let i = 1; i < breadcrumbs.length - 2; i++) {
                breadcrumbs[i].classList.add('d-none');
            }
        }
    }

    /**
     * Adapt the interface for mobile
     */
    function adaptForMobile() {
        console.log('Switching to mobile layout');

        // Hide non-essential columns
        document.querySelectorAll('table th:nth-child(n+3), table td:nth-child(n+3)').forEach(el => {
            if (!el.classList.contains('essential')) {
                el.classList.add('d-none-mobile');
            }
        });

        // Adjust breadcrumbs
        updateMobileBreadcrumbs();
    }

    /**
     * Adapt the interface for desktop
     */
    function adaptForDesktop() {
        console.log('Switching to desktop layout');

        // Show all columns again
        document.querySelectorAll('.d-none-mobile').forEach(el => {
            el.classList.remove('d-none-mobile');
        });
    }

    /**
     * Convert table to cards for mobile
     */
    function convertTableToCards() {
        const table = document.querySelector('#browserArea table');
        if (!table || table.dataset.mobileConverted === 'true') return;

        const rows = table.querySelectorAll('tbody tr');
        const container = document.createElement('div');
        container.className = 'mobile-file-list';

        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            if (cells.length === 0) return;

            const card = document.createElement('div');
            card.className = 'file-card-mobile';

            // Checkbox
            const checkbox = row.querySelector('input[type="checkbox"]');
            if (checkbox) {
                const checkboxClone = checkbox.cloneNode(true);
                card.appendChild(checkboxClone);
            }

            // Icon and name
            const icon = row.querySelector('img');
            const name = row.querySelector('.fileTreeRef, .file');

            if (icon) {
                const iconClone = icon.cloneNode(true);
                card.appendChild(iconClone);
            }

            const info = document.createElement('div');
            info.className = 'file-info';

            const fileName = document.createElement('div');
            fileName.className = 'file-name';
            fileName.textContent = name ? name.textContent.trim() : '';
            info.appendChild(fileName);

            // Metadata (size, date)
            const size = cells[1]?.textContent.trim();
            const date = cells[3]?.textContent.trim();

            if (size || date) {
                const meta = document.createElement('div');
                meta.className = 'file-meta';
                meta.textContent = [size, date].filter(Boolean).join(' • ');
                info.appendChild(meta);
            }

            card.appendChild(info);

            // Actions
            const actions = document.createElement('div');
            actions.className = 'file-actions';
            actions.innerHTML = '<i class="bi bi-three-dots-vertical"></i>';
            card.appendChild(actions);

            container.appendChild(card);
        });

        table.style.display = 'none';
        table.after(container);
        table.dataset.mobileConverted = 'true';
    }

    /**
     * Restore table layout
     */
    function restoreTableLayout() {
        const table = document.querySelector('#browserArea table');
        const mobileList = document.querySelector('.mobile-file-list');

        if (table && mobileList) {
            table.style.display = '';
            mobileList.remove();
            delete table.dataset.mobileConverted;
        }
    }

    /**
     * Synchronize mobile tree with desktop
     */
    function syncTreeWithMobile() {
        const desktopTree = document.getElementById('fileTree');
        const mobileTree = document.getElementById('fileTreeMobile');

        if (!desktopTree || !mobileTree) return;

        // Watch for changes in the desktop tree
        const observer = new MutationObserver(function() {
            mobileTree.innerHTML = desktopTree.innerHTML;
        });

        observer.observe(desktopTree, {
            childList: true,
            subtree: true
        });

        // Initial synchronization
        mobileTree.innerHTML = desktopTree.innerHTML;
    }


    /**
     * Synchronize mobile button state with desktop
     */
    function syncMobileToolbarState() {
        const syncBtn = (mobileId, desktopId) => {
            const mobileBtn = document.getElementById(mobileId);
            const desktopBtn = document.getElementById(desktopId);

            if (mobileBtn && desktopBtn) {
                if (desktopBtn.classList.contains('disabled') || desktopBtn.disabled) {
                    mobileBtn.classList.add('disabled');
                    mobileBtn.disabled = true;
                } else {
                    mobileBtn.classList.remove('disabled');
                    mobileBtn.disabled = false;
                }
            }
        };

        // Synchronize buttons
        syncBtn('mobile-toolbar-download', 'toolbar-download');

        // Watch desktop button state changes to keep mobile buttons in sync in real time
        const observeDesktopBtn = (desktopId, syncFn) => {
            const desktopBtn = document.getElementById(desktopId);
            if (desktopBtn) {
                new MutationObserver(syncFn).observe(desktopBtn, { attributes: true, attributeFilter: ['class', 'disabled'] });
            }
        };
        observeDesktopBtn('toolbar-download', () => syncBtn('mobile-toolbar-download', 'toolbar-download'));
    }

    /**
     * Synchronize mobile menu button state.
     * Uses MutationObserver (reactive) instead of a polling loop.
     */
    function syncMobileMenuState() {
        const buttons = [
            'download', 'copy', 'cut', 'paste', 'rename', 'delete'
        ];

        const syncOne = (action) => {
            const mobileBtn = document.getElementById(`mobile-${action}`);
            const desktopBtn = document.getElementById(`toolbar-${action}`);

            if (!mobileBtn || !desktopBtn) return;

            const isDisabled = desktopBtn.classList.contains('disabled') || desktopBtn.disabled;
            if (isDisabled) {
                mobileBtn.classList.add('disabled');
                mobileBtn.disabled = true;
                mobileBtn.setAttribute('aria-disabled', 'true');
            } else {
                mobileBtn.classList.remove('disabled');
                mobileBtn.disabled = false;
                mobileBtn.removeAttribute('aria-disabled');
            }
        };

        buttons.forEach(action => {
            // Initial sync
            syncOne(action);

            // Observe desktop button for attribute changes (reactive, no polling)
            const desktopBtn = document.getElementById(`toolbar-${action}`);
            if (desktopBtn) {
                new MutationObserver(() => syncOne(action))
                    .observe(desktopBtn, { attributes: true, attributeFilter: ['class', 'disabled'] });
            }
        });
    }

    /**
     * Debounce utility
     */
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Export for global use
    window.EsupMobileSupport = {
        isMobile,
        isTouchDevice,
        updateMobileBreadcrumbs,
        syncMobileToolbarState,
        convertTableToCards,
        restoreTableLayout
    };

    console.log('ESUP Mobile Support loaded');
})();


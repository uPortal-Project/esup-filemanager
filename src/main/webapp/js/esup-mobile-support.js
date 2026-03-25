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

        // Mobile more actions (bottom sheet)
        const moreBtn = document.getElementById('mobile-toolbar-more');
        if (moreBtn) {
            moreBtn.addEventListener('click', function() {
                const modal = new bootstrap.Modal(document.getElementById('mobileActionsSheet'));
                modal.show();
            });
        }

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

            // Swipe left (fermer menu)
            if (diff < -swipeThreshold) {
                const offcanvasElement = document.getElementById('mobileMenu');
                const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasElement);
                if (offcanvas) {
                    offcanvas.hide();
                }
            }
        }

        // Long press pour afficher actions (alternative au clic droit)
        let pressTimer;
        document.addEventListener('touchstart', function(e) {
            const target = e.target.closest('.file-card-mobile, tr.selectable');
            if (!target) return;

            pressTimer = setTimeout(function() {
                // Afficher le bottom sheet d'actions
                const modal = new bootstrap.Modal(document.getElementById('mobileActionsSheet'));
                modal.show();

                // Vibration feedback si supporté
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
     * Comportements responsifs
     */
    function initResponsiveBehaviors() {
        // Observer les changements de taille d'écran
        const mediaQuery = window.matchMedia('(max-width: 991.98px)');

        function handleMediaChange(e) {
            if (e.matches) {
                // Mode mobile/tablette
                adaptForMobile();
            } else {
                // Mode desktop
                adaptForDesktop();
            }
        }

        handleMediaChange(mediaQuery);
        mediaQuery.addListener(handleMediaChange);

        // Adapter les tables pour mobile
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
     * Adapter l'interface pour mobile
     */
    function adaptForMobile() {
        console.log('Switching to mobile layout');

        // Cacher colonnes non essentielles
        document.querySelectorAll('table th:nth-child(n+3), table td:nth-child(n+3)').forEach(el => {
            if (!el.classList.contains('essential')) {
                el.classList.add('d-none-mobile');
            }
        });

        // Ajuster breadcrumbs
        updateMobileBreadcrumbs();
    }

    /**
     * Adapter l'interface pour desktop
     */
    function adaptForDesktop() {
        console.log('Switching to desktop layout');

        // Réafficher toutes les colonnes
        document.querySelectorAll('.d-none-mobile').forEach(el => {
            el.classList.remove('d-none-mobile');
        });
    }

    /**
     * Convertir table en cards pour mobile
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

            // Icon et nom
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

            // Métadonnées (taille, date)
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
     * Restaurer layout table
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
     * Synchroniser arborescence mobile
     */
    function syncTreeWithMobile() {
        const desktopTree = document.getElementById('fileTree');
        const mobileTree = document.getElementById('fileTreeMobile');

        if (!desktopTree || !mobileTree) return;

        // Observer changements dans l'arborescence desktop
        const observer = new MutationObserver(function() {
            mobileTree.innerHTML = desktopTree.innerHTML;
        });

        observer.observe(desktopTree, {
            childList: true,
            subtree: true
        });

        // Synchronisation initiale
        mobileTree.innerHTML = desktopTree.innerHTML;
    }


    /**
     * Synchroniser état des boutons mobile avec desktop
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

        // Synchroniser les boutons
        syncBtn('mobile-toolbar-download', 'toolbar-download');
        syncBtn('mobile-toolbar-more', 'toolbar-copy');
    }

    /**
     * Synchroniser état des boutons du menu mobile
     */
    function syncMobileMenuState() {
        const buttons = [
            'download', 'copy', 'cut', 'paste', 'rename', 'delete'
        ];

        buttons.forEach(action => {
            const mobileBtn = document.getElementById(`mobile-${action}`);
            const desktopBtn = document.getElementById(`toolbar-${action}`);

            if (mobileBtn && desktopBtn) {
                if (desktopBtn.classList.contains('disabled')) {
                    mobileBtn.classList.add('disabled');
                } else {
                    mobileBtn.classList.remove('disabled');
                }
            }
        });

        // Re-synchroniser régulièrement
        setTimeout(syncMobileMenuState, 1000);
    }

    /**
     * Utilitaire debounce
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

    // Export pour utilisation globale
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


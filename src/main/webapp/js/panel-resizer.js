/**
 * ESUP File Manager - Panel Resizer
 * Gestion moderne et simple du redimensionnement des panneaux
 * WCAG 2.1 AA : navigation clavier + mise à jour aria-valuenow (WCAG 2.1.1, 4.1.2)
 */

(function() {
    'use strict';

    class PanelResizer {
        constructor(options = {}) {
            this.leftPanel = document.getElementById('leftPanel');
            this.splitter  = document.getElementById('treeSplitter');

            if (!this.leftPanel || !this.splitter) {
                console.warn('PanelResizer: Required elements not found');
                return;
            }

            this.minWidth     = options.minWidth     || 200;
            this.maxWidth     = options.maxWidth     || 500;
            this.defaultWidth = options.defaultWidth || 280;

            this.isResizing = false;
            this.startX     = 0;
            this.startWidth = 0;

            this.init();
        }

        init() {
            // Charger la largeur sauvegardée ou utiliser la valeur par défaut
            const savedWidth = localStorage.getItem('esup-left-panel-width');
            if (savedWidth) {
                this.setWidth(parseInt(savedWidth, 10));
            }

            // Événements souris du splitter
            this.splitter.addEventListener('mousedown', (e) => this.startResize(e));

            // Navigation clavier du splitter (WCAG 2.1.1)
            this.splitter.addEventListener('keydown', (e) => this.handleKeydown(e));

            // Événements globaux
            document.addEventListener('mousemove', (e) => this.resize(e));
            document.addEventListener('mouseup',  ()  => this.stopResize());

            // Synchroniser aria-valuenow avec la largeur initiale
            this._updateAriaValue();

            console.log('PanelResizer initialized');
        }

        /**
         * Keyboard resize handler — WCAG 2.1.1
         * ArrowRight / ArrowLeft : ±10px (±50px avec Shift)
         * Home : largeur minimale
         * End  : largeur maximale
         */
        handleKeydown(e) {
            const step = e.shiftKey ? 50 : 10;
            const current = this.leftPanel.offsetWidth;
            let newWidth = current;

            switch (e.key) {
                case 'ArrowRight':
                    newWidth = Math.min(this.maxWidth, current + step);
                    break;
                case 'ArrowLeft':
                    newWidth = Math.max(this.minWidth, current - step);
                    break;
                case 'Home':
                    newWidth = this.minWidth;
                    break;
                case 'End':
                    newWidth = this.maxWidth;
                    break;
                default:
                    return; // Ignorer les autres touches
            }

            e.preventDefault();
            this.setWidth(newWidth);
            localStorage.setItem('esup-left-panel-width', newWidth);
            this._updateAriaValue();
        }

        startResize(e) {
            e.preventDefault();
            this.isResizing = true;
            this.startX     = e.clientX;
            this.startWidth = this.leftPanel.offsetWidth;

            this.splitter.classList.add('resizing');
            document.body.style.cursor    = 'ew-resize';
            document.body.style.userSelect = 'none';

            this.createOverlay();
        }

        resize(e) {
            if (!this.isResizing) return;

            const delta = e.clientX - this.startX;
            const newWidth = this.startWidth + delta;

            const constrained = Math.max(this.minWidth, Math.min(this.maxWidth, newWidth));
            this.setWidth(constrained);
        }

        stopResize() {
            if (!this.isResizing) return;

            this.isResizing = false;
            this.splitter.classList.remove('resizing');
            document.body.style.cursor    = '';
            document.body.style.userSelect = '';

            this.removeOverlay();

            localStorage.setItem('esup-left-panel-width', this.leftPanel.offsetWidth);
            this._updateAriaValue();
        }

        setWidth(width) {
            this.leftPanel.style.width = width + 'px';
            document.documentElement.style.setProperty('--left-panel-width', width + 'px');
        }

        /** Met à jour aria-valuenow sur le splitter (WCAG 4.1.2) */
        _updateAriaValue() {
            this.splitter.setAttribute('aria-valuenow', this.leftPanel.offsetWidth);
        }

        createOverlay() {
            const overlay = document.createElement('div');
            overlay.className = 'resize-overlay active';
            overlay.id = 'resize-overlay';
            document.body.appendChild(overlay);
        }

        removeOverlay() {
            const overlay = document.getElementById('resize-overlay');
            if (overlay) overlay.remove();
        }
    }

    // Initialisation au chargement du DOM
    document.addEventListener('DOMContentLoaded', function() {
        window.panelResizer = new PanelResizer({
            minWidth:     200,
            maxWidth:     500,
            defaultWidth: 280
        });

        console.log('Panel Resizer loaded');
    });

    window.PanelResizer = PanelResizer;

})();

/**
 * ESUP File Manager - Panel Resizer
 * Gestion moderne et simple du redimensionnement des panneaux
 */

(function() {
    'use strict';

    class PanelResizer {
        constructor(options = {}) {
            this.leftPanel = document.getElementById('leftPanel');
            this.splitter = document.getElementById('treeSplitter');

            if (!this.leftPanel || !this.splitter) {
                console.warn('PanelResizer: Required elements not found');
                return;
            }

            this.minWidth = options.minWidth || 200;
            this.maxWidth = options.maxWidth || 500;
            this.defaultWidth = options.defaultWidth || 280;

            this.isResizing = false;
            this.startX = 0;
            this.startWidth = 0;

            this.init();
        }

        init() {
            // Charger la largeur sauvegardée ou utiliser la valeur par défaut
            const savedWidth = localStorage.getItem('esup-left-panel-width');
            if (savedWidth) {
                this.setWidth(parseInt(savedWidth, 10));
            }

            // Événements du splitter
            this.splitter.addEventListener('mousedown', (e) => this.startResize(e));


            // Événements globaux
            document.addEventListener('mousemove', (e) => this.resize(e));
            document.addEventListener('mouseup', () => this.stopResize());

            console.log('PanelResizer initialized');
        }

        startResize(e) {
            e.preventDefault();
            this.isResizing = true;
            this.startX = e.clientX;
            this.startWidth = this.leftPanel.offsetWidth;

            this.splitter.classList.add('resizing');
            document.body.style.cursor = 'ew-resize';
            document.body.style.userSelect = 'none';

            // Créer un overlay pour capturer tous les événements de souris
            this.createOverlay();
        }

        resize(e) {
            if (!this.isResizing) return;

            const delta = e.clientX - this.startX;
            const newWidth = this.startWidth + delta;

            // Contraindre entre min et max
            const constrainedWidth = Math.max(
                this.minWidth,
                Math.min(this.maxWidth, newWidth)
            );

            this.setWidth(constrainedWidth);
        }

        stopResize() {
            if (!this.isResizing) return;

            this.isResizing = false;
            this.splitter.classList.remove('resizing');
            document.body.style.cursor = '';
            document.body.style.userSelect = '';

            // Retirer l'overlay
            this.removeOverlay();

            // Sauvegarder la largeur
            localStorage.setItem('esup-left-panel-width', this.leftPanel.offsetWidth);
        }

        setWidth(width) {
            this.leftPanel.style.width = width + 'px';
            // Mettre à jour la variable CSS pour une utilisation ailleurs si nécessaire
            document.documentElement.style.setProperty('--left-panel-width', width + 'px');
        }

        createOverlay() {
            const overlay = document.createElement('div');
            overlay.className = 'resize-overlay active';
            overlay.id = 'resize-overlay';
            document.body.appendChild(overlay);
        }

        removeOverlay() {
            const overlay = document.getElementById('resize-overlay');
            if (overlay) {
                overlay.remove();
            }
        }

    }

    // Initialisation au chargement du DOM
    document.addEventListener('DOMContentLoaded', function() {
        // Initialiser le resizer
        window.panelResizer = new PanelResizer({
            minWidth: 200,
            maxWidth: 500,
            defaultWidth: 280
        });

        console.log('Panel Resizer loaded');
    });

    // Export pour utilisation globale
    window.PanelResizer = PanelResizer;

})();


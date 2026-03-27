/**
 * Drag & Drop Manager - HTML5 API drag & drop management
 * Native implementation of drag & drop functionality
 */

export class DragDropManager {
    constructor(options = {}) {
        this.options = {
            onDragStart: options.onDragStart || (() => {}),
            onDrop: options.onDrop || (() => {}),
            validateDrop: options.validateDrop || (() => true),
            ...options
        };

        this.draggedElement = null;
        this.draggedData = null;
    }

    /**
     * Makes an element draggable
     */
    makeDraggable(element) {
        element.draggable = true;
        element.classList.add('draggable');

        element.addEventListener('dragstart', (e) => {
            this.draggedElement = element;
            this.draggedData = {
                path: element.dataset.path || element.getAttribute('rel'),
                type: element.dataset.type || 'file',
                element: element
            };

            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', this.draggedData.path);

            element.classList.add('dragging');
            this.options.onDragStart(this.draggedData);
        });

        element.addEventListener('dragend', (e) => {
            element.classList.remove('dragging');
            this.draggedElement = null;
        });
    }

    /**
     * Makes an element droppable
     */
    makeDroppable(element) {
        element.classList.add('droppable');

        element.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';

            if (this.draggedElement && this.draggedElement !== element) {
                element.classList.add('drag-over');
            }
        });

        element.addEventListener('dragleave', (e) => {
            element.classList.remove('drag-over');
        });

        element.addEventListener('drop', (e) => {
            e.preventDefault();
            e.stopPropagation();

            element.classList.remove('drag-over');

            if (!this.draggedElement) return;

            const targetPath = element.dataset.path || element.getAttribute('rel');

            // Drop validation
            if (this.options.validateDrop(this.draggedData, targetPath)) {
                this.options.onDrop(this.draggedData, targetPath, element);
            }
        });
    }

    /**
     * Initializes drag & drop on a set of elements
     */
    initializeDragDrop(config) {
        const {
            draggableSelector,
            droppableSelector,
            container = document
        } = config;

        // Make elements draggable
        if (draggableSelector) {
            const draggables = container.querySelectorAll(draggableSelector);
            draggables.forEach(el => this.makeDraggable(el));
        }

        // Make elements droppable
        if (droppableSelector) {
            const droppables = container.querySelectorAll(droppableSelector);
            droppables.forEach(el => this.makeDroppable(el));
        }
    }

    /**
     * Reinitializes drag & drop (useful after dynamically adding elements)
     */
    refresh(container = document) {
        const draggables = container.querySelectorAll('.draggable');
        draggables.forEach(el => {
            // Supprimer les anciens listeners en clonant l'élément
            const newEl = el.cloneNode(true);
            el.parentNode.replaceChild(newEl, el);
            this.makeDraggable(newEl);
        });

        const droppables = container.querySelectorAll('.droppable');
        droppables.forEach(el => {
            const newEl = el.cloneNode(true);
            el.parentNode.replaceChild(newEl, el);
            this.makeDroppable(newEl);
        });
    }
}


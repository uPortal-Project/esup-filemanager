/**
 * UI Components - Vanilla JS UI components
 * Replaces Bootstrap UI components functionality (dialogs, resizable) and context menus
 */

export class UIComponents {

    /**
     * Displays a simple modal dialog
     */
    static showDialog(options) {
        const {
            title = 'Dialog',
            message = '',
            buttons = [],
            onClose = () => {}
        } = options;

        // Créer le backdrop
        const backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop fade show';
        document.body.appendChild(backdrop);

        // Créer le modal
        const modal = document.createElement('div');
        modal.className = 'modal fade show';
        modal.style.display = 'block';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('tabindex', '-1');

        modal.innerHTML = `
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">${title}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        ${message}
                    </div>
                    <div class="modal-footer">
                        ${buttons.map((btn, idx) => 
                            `<button type="button" class="btn btn-${btn.type || 'secondary'}" data-action="${idx}">
                                ${btn.text}
                            </button>`
                        ).join('')}
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);

        // Handle buttons
        buttons.forEach((btn, idx) => {
            const button = modal.querySelector(`[data-action="${idx}"]`);
            button.addEventListener('click', () => {
                if (btn.action) btn.action();
                this.closeDialog(modal, backdrop);
                onClose();
            });
        });

        // Close button
        const closeBtn = modal.querySelector('.btn-close');
        closeBtn.addEventListener('click', () => {
            this.closeDialog(modal, backdrop);
            onClose();
        });

        return { modal, backdrop };
    }

    static closeDialog(modal, backdrop) {
        modal.remove();
        backdrop.remove();
    }

    /**
     * Displays an error message
     */
    static showError(message, duration = 5000) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-danger alert-dismissible fade show position-fixed top-0 end-0 m-3';
        alert.style.zIndex = '9999';
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 150);
        }, duration);

        alert.querySelector('.btn-close').addEventListener('click', () => {
            alert.remove();
        });
    }

    /**
     * Displays an information message
     */
    static showInfo(message, duration = 5000) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-info alert-dismissible fade show position-fixed top-0 end-0 m-3';
        alert.style.zIndex = '9999';
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 150);
        }, duration);
    }

    /**
     * Displays a success message
     */
    static showSuccess(message, duration = 5000) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-success alert-dismissible fade show position-fixed top-0 end-0 m-3';
        alert.style.zIndex = '9999';
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 150);
        }, duration);

        alert.querySelector('.btn-close').addEventListener('click', () => {
            alert.remove();
        });
    }

    /**
     * Makes an element resizable
     */
    static makeResizable(element, options = {}) {
        const {
            minWidth = 122,
            maxWidth = 738,
            minHeight = 122,
            maxHeight = 600,
            onResize = () => {}
        } = options;

        const handle = document.createElement('div');
        handle.className = 'resize-handle';
        handle.style.cssText = `
            position: absolute;
            bottom: 0;
            right: 0;
            width: 15px;
            height: 15px;
            cursor: se-resize;
            background: linear-gradient(135deg, transparent 50%, #999 50%);
        `;

        element.style.position = 'relative';
        element.appendChild(handle);

        let isResizing = false;
        let startX, startY, startWidth, startHeight;

        handle.addEventListener('mousedown', (e) => {
            isResizing = true;
            startX = e.clientX;
            startY = e.clientY;
            startWidth = element.offsetWidth;
            startHeight = element.offsetHeight;

            e.preventDefault();
        });

        document.addEventListener('mousemove', (e) => {
            if (!isResizing) return;

            const width = startWidth + (e.clientX - startX);
            const height = startHeight + (e.clientY - startY);

            if (width >= minWidth && width <= maxWidth) {
                element.style.width = width + 'px';
            }

            if (height >= minHeight && height <= maxHeight) {
                element.style.height = height + 'px';
            }

            onResize({ width: element.offsetWidth, height: element.offsetHeight });
        });

        document.addEventListener('mouseup', () => {
            if (isResizing) {
                isResizing = false;
            }
        });
    }

    /**
     * Creates a context menu
     */
    static createContextMenu(items) {
        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.style.cssText = `
            position: fixed;
            background: white;
            border: 1px solid #ccc;
            border-radius: 4px;
            padding: 4px 0;
            box-shadow: 0 2px 10px rgba(0,0,0,0.2);
            z-index: 10000;
            display: none;
        `;

        items.forEach(item => {
            if (item.separator) {
                const separator = document.createElement('hr');
                separator.style.margin = '4px 0';
                menu.appendChild(separator);
            } else {
                const menuItem = document.createElement('div');
                menuItem.className = 'context-menu-item';
                menuItem.style.cssText = `
                    padding: 8px 24px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                `;

                if (item.icon) {
                    const icon = document.createElement('img');
                    icon.src = item.icon;
                    icon.style.width = '16px';
                    icon.style.height = '16px';
                    menuItem.appendChild(icon);
                }

                const label = document.createElement('span');
                label.textContent = item.label;
                menuItem.appendChild(label);

                if (item.disabled) {
                    menuItem.style.opacity = '0.5';
                    menuItem.style.cursor = 'not-allowed';
                } else {
                    menuItem.addEventListener('mouseover', () => {
                        menuItem.style.background = '#f0f0f0';
                    });
                    menuItem.addEventListener('mouseout', () => {
                        menuItem.style.background = 'white';
                    });
                    menuItem.addEventListener('click', () => {
                        if (item.action) item.action();
                        menu.style.display = 'none';
                    });
                }

                menu.appendChild(menuItem);
            }
        });

        document.body.appendChild(menu);

        // Close on click elsewhere
        document.addEventListener('click', () => {
            menu.style.display = 'none';
        });

        return menu;
    }

    /**
     * Displays a context menu at a specific position
     */
    static showContextMenu(menu, x, y) {
        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
        menu.style.display = 'block';

        // Adjust if off-screen
        const rect = menu.getBoundingClientRect();
        if (rect.right > window.innerWidth) {
            menu.style.left = (x - rect.width) + 'px';
        }
        if (rect.bottom > window.innerHeight) {
            menu.style.top = (y - rect.height) + 'px';
        }
    }

    /**
     * Shows a wait cursor
     */
    static showWaitCursor() {
        document.body.style.cursor = 'wait';
    }

    /**
     * Hides the wait cursor
     */
    static hideWaitCursor() {
        document.body.style.cursor = 'default';
    }
}


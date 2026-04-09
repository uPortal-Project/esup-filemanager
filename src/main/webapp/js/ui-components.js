/**
 * UI Components - Vanilla JS UI components
 * Replaces Bootstrap UI components functionality (dialogs, resizable) and context menus
 * WCAG 2.1 AA compliant: focus management, ARIA roles, keyboard navigation
 */

export class UIComponents {

    /** Unique ID counter for modals */
    static _modalCounter = 0;

    /**
     * Returns all focusable elements within a container
     */
    static _getFocusableElements(container) {
        return Array.from(container.querySelectorAll(
            'button:not([disabled]):not([aria-disabled="true"]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter(el => !el.closest('[hidden]'));
    }

    /**
     * Installs a focus trap within a container
     */
    static _installFocusTrap(container) {
        const handler = (e) => {
            if (e.key !== 'Tab') return;
            const focusable = UIComponents._getFocusableElements(container);
            if (focusable.length === 0) return;
            const first = focusable[0];
            const last  = focusable[focusable.length - 1];
            if (e.shiftKey) {
                if (document.activeElement === first) {
                    last.focus();
                    e.preventDefault();
                }
            } else {
                if (document.activeElement === last) {
                    first.focus();
                    e.preventDefault();
                }
            }
        };
        container.addEventListener('keydown', handler);
        return handler; // return so caller can remove it
    }

    /**
     * Displays a simple modal dialog (WCAG 4.1.2, 2.4.3, 2.1.1)
     */
    static showDialog(options) {
        const {
            title = 'Dialog',
            message = '',
            buttons = [],
            onClose = () => {}
        } = options;

        const id = ++UIComponents._modalCounter;
        const titleId = `modal-title-${id}`;
        const bodyId  = `modal-body-${id}`;

        // Save currently focused element to restore on close
        const previousFocus = document.activeElement;

        // Créer le backdrop
        const backdrop = document.createElement('div');
        backdrop.className = 'modal-backdrop fade show';
        document.body.appendChild(backdrop);

        // Créer le modal
        const modal = document.createElement('div');
        modal.className = 'modal fade show';
        modal.style.display = 'block';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-labelledby', titleId);
        modal.setAttribute('aria-describedby', bodyId);
        modal.setAttribute('tabindex', '-1');

        modal.innerHTML = `
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="${titleId}">${title}</h5>
                        <button type="button" class="btn-close" aria-label="Close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body" id="${bodyId}">
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

        // Close helper
        const closeAll = () => {
            UIComponents.closeDialog(modal, backdrop);
            onClose();
            document.removeEventListener('keydown', escHandler);
            // Restore focus
            if (previousFocus && typeof previousFocus.focus === 'function') {
                previousFocus.focus();
            }
        };

        // Handle buttons
        buttons.forEach((btn, idx) => {
            const button = modal.querySelector(`[data-action="${idx}"]`);
            if (button) {
                button.addEventListener('click', () => {
                    if (btn.action) btn.action();
                    closeAll();
                });
            }
        });

        // Close button
        const closeBtn = modal.querySelector('.btn-close');
        if (closeBtn) closeBtn.addEventListener('click', closeAll);

        // Escape key
        const escHandler = (e) => {
            if (e.key === 'Escape') closeAll();
        };
        document.addEventListener('keydown', escHandler);

        // Focus trap
        UIComponents._installFocusTrap(modal);

        // Move focus to first focusable element
        const focusable = UIComponents._getFocusableElements(modal);
        if (focusable.length > 0) {
            setTimeout(() => focusable[0].focus(), 50);
        }

        return { modal, backdrop };
    }

    static closeDialog(modal, backdrop) {
        modal.remove();
        backdrop.remove();
    }

    /**
     * Displays an error message (role="alert" = live region assertive — WCAG 4.1.3)
     */
    static showError(message, duration = 5000) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-danger alert-dismissible fade show position-fixed top-0 end-0 m-3';
        alert.style.zIndex = '9999';
        alert.setAttribute('role', 'alert');
        alert.setAttribute('aria-live', 'assertive');
        alert.setAttribute('aria-atomic', 'true');
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" aria-label="Close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 150);
        }, duration);

        const closeBtn = alert.querySelector('.btn-close');
        if (closeBtn) closeBtn.addEventListener('click', () => alert.remove());
    }

    /**
     * Displays an information message (role="status" = live region polite — WCAG 4.1.3)
     */
    static showInfo(message, duration = 5000) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-info alert-dismissible fade show position-fixed top-0 end-0 m-3';
        alert.style.zIndex = '9999';
        alert.setAttribute('role', 'status');
        alert.setAttribute('aria-live', 'polite');
        alert.setAttribute('aria-atomic', 'true');
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" aria-label="Close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 150);
        }, duration);
    }

    /**
     * Displays a success message (role="status" — WCAG 4.1.3)
     */
    static showSuccess(message, duration = 5000) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-success alert-dismissible fade show position-fixed top-0 end-0 m-3';
        alert.style.zIndex = '9999';
        alert.setAttribute('role', 'status');
        alert.setAttribute('aria-live', 'polite');
        alert.setAttribute('aria-atomic', 'true');
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" aria-label="Close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        setTimeout(() => {
            alert.classList.remove('show');
            setTimeout(() => alert.remove(), 150);
        }, duration);

        const closeBtn = alert.querySelector('.btn-close');
        if (closeBtn) closeBtn.addEventListener('click', () => alert.remove());
    }


    /**
     * Creates a context menu (WCAG 2.1.1 – keyboard accessible, 4.1.2 – ARIA roles)
     */
    static createContextMenu(items) {
        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.setAttribute('role', 'menu');
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

        const menuItems = []; // list of focusable menu items

        items.forEach(item => {
            if (item.separator) {
                const separator = document.createElement('hr');
                separator.setAttribute('role', 'separator');
                separator.style.margin = '4px 0';
                menu.appendChild(separator);
            } else {
                const menuItem = document.createElement('div');
                menuItem.className = 'context-menu-item';
                menuItem.setAttribute('role', 'menuitem');
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
                    icon.setAttribute('aria-hidden', 'true');
                    menuItem.appendChild(icon);
                }

                const label = document.createElement('span');
                label.textContent = item.label;
                menuItem.appendChild(label);

                if (item.disabled) {
                    menuItem.setAttribute('aria-disabled', 'true');
                    menuItem.setAttribute('tabindex', '-1');
                    menuItem.style.opacity = '0.5';
                    menuItem.style.cursor = 'not-allowed';
                } else {
                    menuItem.setAttribute('tabindex', '-1'); // managed via roving tabindex
                    menuItem.addEventListener('mouseover', () => {
                        menuItem.style.background = '#f0f0f0';
                        menuItem.focus();
                    });
                    menuItem.addEventListener('mouseout', () => {
                        menuItem.style.background = 'white';
                    });
                    menuItem.addEventListener('click', () => {
                        if (item.action) item.action();
                        menu.style.display = 'none';
                    });
                    menuItems.push(menuItem);
                }

                menu.appendChild(menuItem);
            }
        });

        // Keyboard navigation within the menu (WCAG 2.1.1)
        menu.addEventListener('keydown', (e) => {
            const activeItems = menuItems.filter(mi => mi.getAttribute('aria-disabled') !== 'true');
            const currentIdx  = activeItems.indexOf(document.activeElement);

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                const next = (currentIdx + 1) % activeItems.length;
                activeItems[next].focus();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                const prev = (currentIdx - 1 + activeItems.length) % activeItems.length;
                activeItems[prev].focus();
            } else if (e.key === 'Escape') {
                e.preventDefault();
                menu.style.display = 'none';
            } else if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                const focused = document.activeElement;
                if (focused && focused.getAttribute('role') === 'menuitem') {
                    focused.click();
                }
            } else if (e.key === 'Home') {
                e.preventDefault();
                if (activeItems.length > 0) activeItems[0].focus();
            } else if (e.key === 'End') {
                e.preventDefault();
                if (activeItems.length > 0) activeItems[activeItems.length - 1].focus();
            }
        });

        document.body.appendChild(menu);

        // Close on click outside
        document.addEventListener('click', () => { menu.style.display = 'none'; });

        return menu;
    }

    /**
     * Displays a context menu at a specific position and focuses first item
     */
    static showContextMenu(menu, x, y) {
        menu.style.left = x + 'px';
        menu.style.top  = y + 'px';
        menu.style.display = 'block';

        // Adjust if off-screen
        const rect = menu.getBoundingClientRect();
        if (rect.right  > window.innerWidth)  menu.style.left = (x - rect.width)  + 'px';
        if (rect.bottom > window.innerHeight) menu.style.top  = (y - rect.height) + 'px';

        // Focus first focusable menu item
        const firstItem = menu.querySelector('[role="menuitem"]:not([aria-disabled="true"])');
        if (firstItem) setTimeout(() => firstItem.focus(), 10);
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


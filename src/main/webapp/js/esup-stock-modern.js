/**
 * Main ESUP File Manager Application
 * Modern version - ES6 module
 */

import { AjaxManager } from './ajax-manager.js';
import { FileTree } from './file-tree.js';
import { DragDropManager } from './drag-drop-manager.js';
import { UIComponents } from './ui-components.js';
import { FileUploadManager } from './file-upload-manager.js';

class EsupFileManager {
    constructor() {
        this.fileTree = null;
        this.dragDropManager = null;
        this.uploadManager = null;
        this.selectedFiles = [];
        this.clipboard = { files: [], operation: null }; // 'copy' or 'cut'
        this.currentSortField = 'titleAsc'; // Default sort
        this.currentPath = null; // Memorize current path to avoid depending on DOM
        this.uploadStats = { total: 0, completed: 0, failed: 0 }; // Upload progress tracking

        // Configuration from global variables
        this.config = {
            defaultPath: window.defaultPath || '/',
            htmlFileTreeURL: window.htmlFileTreeURL || '/htmlFileTree',
            fileChildrenURL: window.fileChildrenURL || '/fileChildren',
            uploadFileURL: window.uploadFileURL || '/uploadFile',
            prepareCopyFilesURL: window.prepareCopyFilesURL || '/prepareCopyFiles',
            prepareCutFilesURL: window.prepareCutFilesURL || '/prepareCutFiles',
            pastFilesURL: window.pastFilesURL || '/pastFiles',
            detailsAreaURL: window.detailsAreaURL || '/detailsArea',
            createFileURL: window.createFileURL || '/createFile',
            renameFileURL: window.renameFileURL || '/renameFile',
            downloadFileURL: window.downloadFileURL || '/downloadFile',
            downloadZipURL: window.downloadZipURL || '/downloadZip',
            removeFilesURL: window.removeFilesURL || '/removeFiles',
            useDoubleClick: window.useDoubleClick === 'true'
        };

        // Restore path from URL or localStorage
        this.restoreSavedPath();

        this.init();
    }

    async init() {
        console.log('Initializing ESUP File Manager (vanilla JS)');

        // Initialize file tree
        this.initFileTree();

        // Initialize drag & drop
        this.initDragDrop();

        // Initialize upload manager
        this.initUploadManager();

        // Initialize events
        this.initEventListeners();

        // Initialize authentication form handler
        this.initAuthenticationFormHandler();

        // Initialize context menus
        this.initContextMenus();

        // Note: Panel resizing is now handled by panel-resizer.js
        // initResizable() is no longer necessary

        // Load initial interface
        await this.loadInitialView();
    }

    initFileTree() {
        const treeContainer = document.getElementById('fileTree');
        if (!treeContainer) {
            console.error('File tree container #fileTree not found');
            return;
        }

        this.fileTree = new FileTree('fileTree', {
            ajaxUrl: this.config.fileChildrenURL,
            defaultPath: this.config.defaultPath,
            onSelect: (node, path) => this.handleTreeNodeSelect(node, path),
            onOpen: (node, data) => this.handleTreeNodeOpen(node, data),
            onLoad: (data) => this.handleTreeLoad(data)
        });

        console.log('File tree initialized');
    }

    initDragDrop() {
        this.dragDropManager = new DragDropManager({
            onDragStart: (data) => {
                console.log('Drag started:', data);
            },
            onDrop: (sourceData, targetPath, targetElement) => {
                this.handleFileDrop(sourceData, targetPath);
            },
            validateDrop: (sourceData, targetPath) => {
                return this.isValidPaste(sourceData.path, targetPath);
            }
        });

        // Apply drag & drop to existing elements
        this.refreshDragDrop();
    }

    refreshDragDrop() {
        // Make folders draggable and droppable
        const folders = document.querySelectorAll('.fileTreeRef, [rel="folder"]');
        folders.forEach(folder => {
            this.dragDropManager.makeDraggable(folder);
            this.dragDropManager.makeDroppable(folder);
        });

        // Make files draggable
        const files = document.querySelectorAll('.file');
        files.forEach(file => {
            this.dragDropManager.makeDraggable(file);
        });
    }

    initUploadManager() {
        this.uploadStats = { total: 0, completed: 0, failed: 0 };

        this.uploadManager = new FileUploadManager({
            uploadUrl: this.config.uploadFileURL,
            getCurrentPath: () => this.getCurrentPath(),
            maxConcurrentUploads: 3,
            onFilesQueued: (files) => {
                // Afficher tous les fichiers en attente dans le panneau
                this.uploadStats.total += files.length;
                files.forEach(file => this.addFileToUploadPanel(file));
                this.showUploadPanel();
                this.updateUploadPanelStats();
            },
            onUploadStart: (file, path) => {
                console.log(`Starting upload of ${file.name} to ${path}`);
                this.setUploadItemState(file, 'uploading');
            },
            onUploadProgress: (file, percentComplete) => {
                this.updateUploadProgress(file, percentComplete);
            },
            onUploadComplete: (file, result) => {
                console.log(`Upload complete: ${file.name}`, result);
                this.uploadStats.completed++;
                this.setUploadItemState(file, 'complete');
                this.updateUploadPanelStats();
            },
            onUploadError: (file, error) => {
                console.error(`Upload error: ${file.name}`, error);
                this.uploadStats.failed++;
                this.setUploadItemState(file, 'error');
                this.updateUploadPanelStats();
                UIComponents.showError(
                    (window.i18n?.uploadFileFailed || 'Error uploading file "{0}".').replace('{0}', file.name)
                );
            },
            onAllUploadsComplete: () => {
                this.updateUploadPanelStats();

                const { total, completed, failed } = this.uploadStats;
                if (failed === 0) {
                    const msg = completed === 1
                        ? (window.i18n?.uploadFileSuccess || 'File uploaded successfully.')
                        : (window.i18n?.uploadFilesSuccess || `${completed} files uploaded successfully.`);
                    UIComponents.showSuccess(msg);
                } else {
                    UIComponents.showError(
                        window.i18n?.uploadFilesPartial || `${completed} file${completed > 1 ? 's' : ''} uploaded, ${failed} error${failed > 1 ? 's' : ''}.`
                    );
                }

                // Réinitialiser les stats pour le prochain lot
                this.uploadStats = { total: 0, completed: 0, failed: 0 };

                // Fermer automatiquement le panneau après 4 secondes
                setTimeout(() => this.closeUploadPanel(), 4000);

                // Rafraîchir le contenu sans toucher à l'arborescence
                this.refreshCurrentView(true, false, false);
            }
        });

        console.log('Upload manager initialized');
    }

    initEventListeners() {
        // Toolbar events
        this.bindToolbarEvents();

        // Browser area events
        this.bindBrowserAreaEvents();

        // Keyboard events
        this.bindKeyboardEvents();

        // Event delegation for dynamic elements
        document.addEventListener('click', (e) => {
            // Navigation links in breadcrumbs
            if (e.target.closest('.fileTreeRefCrumbs')) {
                e.preventDefault();
                const link = e.target.closest('.fileTreeRefCrumbs');
                const path = link.getAttribute('rel');
                this.navigateToPath(path);
            }

            // Folder links in navigation area
            if (e.target.closest('.fileTreeRef, .fileCatRef')) {
                e.preventDefault();
                const link = e.target.closest('.fileTreeRef, .fileCatRef');
                this.handleFolderClick(link, e);
            }

            // File links
            if (e.target.closest('.file')) {
                e.preventDefault();
                const link = e.target.closest('.file');
                this.handleFileClick(link, e);
            }
        });
    }

    bindToolbarEvents() {
        // Refresh
        const refreshBtn = document.getElementById('toolbar-refresh');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => this.handleRefresh());
        }

        // Upload
        const uploadBtn = document.getElementById('toolbar-upload');
        if (uploadBtn) {
            uploadBtn.addEventListener('click', () => this.handleUpload());
        }

        // New folder
        const newFolderBtn = document.getElementById('toolbar-new_folder');
        if (newFolderBtn) {
            newFolderBtn.addEventListener('click', () => this.handleCreateDirectory());
        }

        // Copy
        const copyBtn = document.getElementById('toolbar-copy');
        if (copyBtn) {
            copyBtn.addEventListener('click', () => this.copyFiles());
        }

        // Cut
        const cutBtn = document.getElementById('toolbar-cut');
        if (cutBtn) {
            cutBtn.addEventListener('click', () => this.cutFiles());
        }

        // Paste
        const pasteBtn = document.getElementById('toolbar-paste');
        if (pasteBtn) {
            pasteBtn.addEventListener('click', () => this.pasteFiles());
        }

        // Delete
        const deleteBtn = document.getElementById('toolbar-delete');
        if (deleteBtn) {
            deleteBtn.addEventListener('click', () => this.deleteFiles());
        }

        // Rename
        const renameBtn = document.getElementById('toolbar-rename');
        if (renameBtn) {
            renameBtn.addEventListener('click', () => this.handleRename());
        }

        // Download
        const downloadBtn = document.getElementById('toolbar-download');
        if (downloadBtn) {
            downloadBtn.addEventListener('click', () => this.downloadSelectedFiles());
        }

        // Zip
        const zipBtn = document.getElementById('toolbar-zip');
        if (zipBtn) {
            zipBtn.addEventListener('click', () => this.downloadSelectedAsZip());
        }
    }

    bindBrowserAreaEvents() {
        const browserArea = document.getElementById('browserArea');
        if (!browserArea) return;

        // Column sorting
        browserArea.addEventListener('click', (e) => {
            if (e.target.closest('.sortTable')) {
                e.preventDefault();
                const sortLink = e.target.closest('.sortTable');
                const sortField = sortLink.getAttribute('rel');
                this.sortFiles(sortField);
            }
        });

        // Checkbox selection
        browserArea.addEventListener('change', (e) => {
            if (e.target.classList.contains('browsercheck')) {
                this.handleFileSelection();
            }
        });
    }

    bindKeyboardEvents() {
        let isCtrl = false;
        let isShift = false;

        document.addEventListener('keyup', (e) => {
            if (e.key === 'Control') isCtrl = false;
            if (e.key === 'Shift') isShift = false;
        });

        document.addEventListener('keydown', (e) => {
            // Ignore if in a form
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
                return;
            }

            if (e.key === 'Control') { isCtrl = true; return; }
            if (e.key === 'Shift') { isShift = true; return; }

            // Ctrl+C: Copy
            if (e.key === 'c' && isCtrl) {
                e.preventDefault();
                this.copyFiles();
            }

            // Ctrl+X: Cut
            if (e.key === 'x' && isCtrl) {
                e.preventDefault();
                this.cutFiles();
            }

            // Ctrl+V: Paste
            if (e.key === 'v' && isCtrl) {
                e.preventDefault();
                this.pasteFiles();
            }

            // Delete: Delete
            if (e.key === 'Delete') {
                e.preventDefault();
                this.deleteFiles();
            }

            // F2: Rename
            if (e.key === 'F2') {
                e.preventDefault();
                this.handleRename();
            }

            // Enter: Open/download
            if (e.key === 'Enter') {
                e.preventDefault();
                this.handleEnterKey();
            }
        });
    }

    initContextMenus() {
        // Context menu for tree
        const fileTree = document.getElementById('fileTree');
        if (fileTree) {
            const treeMenu = UIComponents.createContextMenu([
                {
                    label: window.i18n?.refresh || 'Refresh',
                    icon: '/img/flaticons/refresh_16px.png',
                    action: () => this.refreshTree()
                },
                { separator: true },
                {
                    label: window.i18n?.newFolder || 'New Folder',
                    icon: '/img/flaticons/new_folder_16px.png',
                    action: () => this.handleCreateDirectory()
                },
                {
                    label: window.i18n?.paste || 'Paste',
                    icon: '/img/flaticons/paste_16px.png',
                    action: () => this.pasteFiles()
                }
            ]);

            fileTree.addEventListener('contextmenu', (e) => {
                e.preventDefault();
                UIComponents.showContextMenu(treeMenu, e.pageX, e.pageY);
            });
        }

        // Context menu for browser area
        const browserArea = document.getElementById('browserArea');
        if (browserArea) {
            const browserMenu = UIComponents.createContextMenu([
                {
                    label: window.i18n?.download || 'Download',
                    icon: '/img/flaticons/download_16px.png',
                    action: () => this.downloadSelectedFiles()
                },
                {
                    label: window.i18n?.copy || 'Copy',
                    icon: '/img/flaticons/copy_16px.png',
                    action: () => this.copyFiles()
                },
                {
                    label: window.i18n?.cut || 'Cut',
                    icon: '/img/flaticons/cut_16px.png',
                    action: () => this.cutFiles()
                },
                {
                    label: window.i18n?.paste || 'Paste',
                    icon: '/img/flaticons/paste_16px.png',
                    action: () => this.pasteFiles()
                },
                { separator: true },
                {
                    label: window.i18n?.rename || 'Rename',
                    icon: '/img/flaticons/rename_16px.png',
                    action: () => this.handleRename()
                },
                {
                    label: window.i18n?.delete_ || 'Delete',
                    icon: '/img/flaticons/delete_16px.png',
                    action: () => this.deleteFiles()
                }
            ]);

            browserArea.addEventListener('contextmenu', (e) => {
                if (e.target.closest('.selectable')) {
                    e.preventDefault();
                    UIComponents.showContextMenu(browserMenu, e.pageX, e.pageY);
                }
            });
        }
    }

    /**
     * Initialize authentication form handler with event delegation
     * Works with AJAX-loaded authentication forms
     */
    initAuthenticationFormHandler() {
        console.log('Initializing authentication form handler (event delegation)');
        
        // Use event delegation for dynamically loaded authentication form
        document.addEventListener('submit', async (e) => {
            // Check if it's the authentication form
            if (e.target && e.target.id === 'authenticationForm') {
                e.preventDefault();
                
                const form = e.target;
                const usernameInput = form.querySelector('#username');
                const passwordInput = form.querySelector('#password');
                const currentDirInput = form.querySelector('#currentDir');
                const errorDiv = form.querySelector('#auth-error-message');
                
                if (!usernameInput || !passwordInput || !currentDirInput) {
                    console.error('Authentication form fields not found');
                    return;
                }
                
                const currentDir = currentDirInput.value;
                const username = usernameInput.value.trim();
                const password = passwordInput.value;
                
                if (!username || !password) {
                    this.showAuthError(errorDiv, window.i18n?.authMissingCredentials || 'Please enter your username and password.');
                    return;
                }
                
                try {
                    // Send authentication request
                    const response = await fetch('/authenticate', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                        },
                        body: new URLSearchParams({
                            dir: currentDir,
                            username: username,
                            password: password
                        })
                    });
                    
                    // Parse JSON response (available even with 401 status)
                    const result = await response.json();
                    
                    // Check HTTP status code
                    if (response.status === 401) {
                        const errorMessage = result.msg || window.i18n?.authBad || 'Authentication failed';
                        this.showAuthError(errorDiv, errorMessage);
                        passwordInput.value = '';
                        passwordInput.focus();
                    } else if (response.ok && result.status === 1) {
                        this.hideAuthError(errorDiv);
                        await this.loadDirectoryContent(currentDir, true, false);
                        UIComponents.showSuccess(window.i18n?.authOk || 'Authentication successful');
                    } else {
                        const errorMessage = result.msg || window.i18n?.authError || 'Authentication error.';
                        this.showAuthError(errorDiv, errorMessage);
                        passwordInput.value = '';
                        passwordInput.focus();
                    }
                } catch (error) {
                    console.error('Authentication error:', error);
                    this.showAuthError(errorDiv, window.i18n?.authError || 'Authentication error. Please try again.');
                }
            }
        });
        
        // Handle input events to hide error messages
        document.addEventListener('input', (e) => {
            if (e.target && (e.target.id === 'username' || e.target.id === 'password')) {
                const form = e.target.closest('#authenticationForm');
                if (form) {
                    const errorDiv = form.querySelector('#auth-error-message');
                    this.hideAuthError(errorDiv);
                }
            }
        });
        
        // Auto-focus username field when authentication form is loaded
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === 1) { // Element node
                        const authForm = node.querySelector ? node.querySelector('#authenticationForm') : null;
                        if (authForm || node.id === 'authenticationForm') {
                            const usernameInput = (authForm || node).querySelector('#username');
                            if (usernameInput) {
                                setTimeout(() => usernameInput.focus(), 100);
                            }
                        }
                    }
                });
            });
        });
        
        // Observe the browser area for authentication form injection
        const browserArea = document.getElementById('browserArea');
        if (browserArea) {
            observer.observe(browserArea, { childList: true, subtree: true });
        }
    }

    /**
     * Show authentication error message
     */
    showAuthError(errorDiv, message) {
        if (errorDiv) {
            // Remove HTML tags from message if present
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = message;
            errorDiv.textContent = tempDiv.textContent || tempDiv.innerText || message;
            errorDiv.style.display = 'block';
        }
    }

    /**
     * Hide authentication error message
     */
    hideAuthError(errorDiv) {
        if (errorDiv) {
            errorDiv.style.display = 'none';
        }
    }

    /*
     * DEPRECATED - Old resizing system
     * Now handled by panel-resizer.js
     */
    /*
    initResizable() {
        const arborescentArea = document.getElementById('arborescentArea');
        if (!arborescentArea) return;

        UIComponents.makeResizable(arborescentArea.parentElement, {
            minWidth: 122,
            maxWidth: 738,
            minHeight: 122,
            maxHeight: 600,
            onResize: (size) => {
                const browserArea = document.getElementById('browserArea');
                if (browserArea) {
                    const totalWidth = document.getElementById('leftArea')?.offsetWidth || 0;
                    browserArea.style.width = (totalWidth - size.width) + 'px';
                }
            }
        });
    }
    */

    async loadInitialView() {
        // Use saved path if available, otherwise defaultPath
        const savedPath = this.getSavedPath();
        let pathToLoad = savedPath || this.config.defaultPath;

        console.log('Loading initial view for path:', pathToLoad);

        // If saved path exists, check if it's still valid
        if (savedPath && savedPath !== this.config.defaultPath) {
            try {
                await this.navigateToPath(savedPath);
                console.log('✅ Successfully restored saved path:', savedPath);
            } catch (error) {
                console.warn('⚠️ Saved path no longer exists, loading default path:', error);
                // If path no longer exists, load default path
                await this.navigateToPath(this.config.defaultPath);
            }
        } else {
            // No saved path, load default path
            await this.navigateToPath(pathToLoad);
        }
    }

    // Event handlers

    async handleTreeNodeSelect(node, path) {
        console.log('Tree node selected:', path);

        if (!path) {
            console.warn('No path provided for selection');
            return;
        }

        // Load selected directory content
        // Disable tree sync since we're coming from the tree
        await this.loadDirectoryContent(path, false);
    }

    async handleTreeNodeOpen(node, data) {
        console.log('Tree node opened:', node);
        // Apply drag & drop to new elements
        this.refreshDragDrop();
    }

    handleTreeLoad(data) {
        console.log('Tree loaded:', data);
        this.refreshDragDrop();

        // Auto-select first drive or category
        setTimeout(() => {
            const firstNode = this.fileTree.container.querySelector('.tree-node[data-type="drive"], .tree-node[data-type="category"]');
            if (firstNode) {
                console.log('Auto-selecting first node:', firstNode.dataset.encPath);
                this.fileTree.selectNode(firstNode);
            } else {
                // Otherwise load default path
                console.log('No drive found, loading default path');
                this.loadDirectoryContent(this.config.defaultPath);
            }
        }, 100);
    }

    handleFolderClick(link, event) {
        const path = link.getAttribute('rel');

        if (this.config.useDoubleClick) {
            // Handle double-click
            const clicks = (link.dataset.clicks || 0) + 1;
            link.dataset.clicks = clicks;

            if (clicks === 1) {
                link.dataset.clickTimeout = setTimeout(() => {
                    link.dataset.clicks = 0;
                    this.selectFolder(link);
                }, 300);
            } else {
                clearTimeout(link.dataset.clickTimeout);
                link.dataset.clicks = 0;
                this.navigateToPath(path);
            }
        } else {
            this.navigateToPath(path);
        }
    }

    handleFileClick(link, event) {
        const path = link.getAttribute('rel');

        if (this.config.useDoubleClick) {
            const clicks = (link.dataset.clicks || 0) + 1;
            link.dataset.clicks = clicks;

            if (clicks === 1) {
                link.dataset.clickTimeout = setTimeout(() => {
                    link.dataset.clicks = 0;
                    this.selectFile(link);
                }, 300);
            } else {
                clearTimeout(link.dataset.clickTimeout);
                link.dataset.clicks = 0;
                this.downloadFile(path);
            }
        } else {
            this.downloadFile(path);
        }
    }

    async navigateToPath(path) {
        console.log('Navigating to:', path);

        UIComponents.showWaitCursor();

        try {
            // Load directory content (with tree sync)
            await this.loadDirectoryContent(path, true);
        } finally {
            UIComponents.hideWaitCursor();
        }
    }

    async loadDirectoryContent(path, syncTree = true, restoreScroll = false) {
        console.log('Loading directory content for path:', path);

        try {
            // Memorize current path
            this.currentPath = path;

            // Save path for next session
            this.saveCurrentPath(path);

            // Save scroll position if we need to restore it
            let savedScrollTop = 0;
            let savedScrollLeft = 0;
            const browserArea = document.getElementById('browserArea');

            if (restoreScroll && browserArea) {
                savedScrollTop = browserArea.scrollTop;
                savedScrollLeft = browserArea.scrollLeft;
                console.log('Saved scroll position:', savedScrollTop, savedScrollLeft);
            }

            // Use htmlFileTreeURL to load file list
            const html = await AjaxManager.loadHTML(this.config.htmlFileTreeURL, {
                dir: path,
                sortField: this.currentSortField || 'titleAsc'
            });

            console.log('Received HTML content, length:', html?.length);

            // Insert content into browserMain (which is inside browserArea)
            const browserMain = document.getElementById('browserMain');

            if (browserMain) {
                browserMain.innerHTML = html;
                console.log('Content loaded into browserMain');
            } else if (browserArea) {
                // Fallback if browserMain doesn't exist
                browserArea.innerHTML = html;
                console.log('Content loaded into browserArea (fallback)');
            } else {
                console.error('Neither browserMain nor browserArea found!');
                return;
            }

            // Restore scroll position if necessary
            if (restoreScroll && browserArea) {
                // Use requestAnimationFrame to ensure DOM is updated
                requestAnimationFrame(() => {
                    browserArea.scrollTop = savedScrollTop;
                    browserArea.scrollLeft = savedScrollLeft;
                    console.log('Restored scroll position:', savedScrollTop, savedScrollLeft);
                });
            }

            // Synchronize tree if requested
            if (syncTree && this.fileTree) {
                await this.syncTreeWithPath(path);
            }

            // Refresh drag & drop after loading content
            this.refreshDragDrop();

            // Also load details in detailArea if not root or category
            const nodeType = this.fileTree?.selectedNode?.dataset?.type;
            if (nodeType && nodeType !== 'root' && nodeType !== 'category') {
                await this.loadDetailsArea(path, nodeType);
            }

        } catch (error) {
            console.error('Failed to load directory content:', error);
            UIComponents.showError(window.i18n?.directoryLoadingError || 'Error loading directory.');
        }
    }

    async loadDetailsArea(path, type) {
        try {
            const html = await AjaxManager.loadHTML(this.config.detailsAreaURL, {
                dirs: path,
                type: type
            });

            const detailArea = document.getElementById('detailArea');
            if (detailArea) {
                detailArea.innerHTML = html;

                // Reinitialize details manager (images, slideshow, etc.)
                if (window.reinitDetailsManager) {
                    window.reinitDetailsManager();
                }

                console.log('Details loaded into detailArea');
            }
        } catch (error) {
            console.error('Failed to load details:', error);
        }
    }

    /**
     * Synchronizes tree with current path
     * Opens parent nodes and selects the node corresponding to the path
     */
    async syncTreeWithPath(path) {
        if (!this.fileTree || !path) {
            console.warn('syncTreeWithPath: Invalid parameters', { fileTree: !!this.fileTree, path });
            return;
        }

        console.log('🔄 Syncing tree with path:', path);

        try {
            // Find node corresponding to path
            let node = this.fileTree.getNodeByPath(path);

            if (node) {
                // If node already exists, select it
                console.log('✅ Node already in tree, selecting it');
                this.fileTree.selectNode(node);
            } else {
                // If node doesn't exist, need to open parents to load it
                console.log('⚠️ Node not found in tree, will try to load it by opening parents');
                const foundNode = await this.openParentNodes(path);

                // Wait a bit for DOM to update
                await new Promise(resolve => setTimeout(resolve, 200));

                // Try again to find node
                node = foundNode || this.fileTree.getNodeByPath(path);
                if (node) {
                    console.log('✅ Node found after opening parents, selecting it');
                    this.fileTree.selectNode(node);
                } else {
                    console.error('❌ Could not find or load node for path:', path);
                    console.log('Available nodes in tree:',
                        Array.from(this.fileTree.container.querySelectorAll('.tree-node'))
                            .map(n => n.dataset.encPath || n.dataset.path)
                            .filter(Boolean)
                    );
                }
            }
        } catch (error) {
            console.error('Error syncing tree with path:', error);
        }
    }

    /**
     * Opens parent nodes to make a path visible in the tree
     * Uses /fileChildren with hierarchy to load the entire hierarchy at once
     */
    async openParentNodes(path) {
        if (!path || !this.fileTree) {
            console.warn('openParentNodes: Invalid path or fileTree', path);
            return;
        }

        console.log('🔍 Opening parent nodes for path:', path);

        // Try first to find node directly by rel
        let node = this.fileTree.container.querySelector(`[rel="${path}"]`);
        if (node) {
            console.log('✅ Node already in tree:', path);
            return node;
        }

        // If node doesn't exist, load entire hierarchy via AJAX
        // using hierarchy parameter to get all parents
        console.log('📥 Loading hierarchy from server for path:', path);

        try {
            const data = await AjaxManager.post(this.config.fileChildrenURL, {
                dir: path,
                hierarchy: 'all'  // Request entire hierarchy up to this node
            });

            console.log('📦 Received hierarchy data:', data);

            // Now need to open and render all hierarchy nodes
            // Response contains complete structure with children
            if (data && Array.isArray(data) && data.length > 0) {
                await this.expandHierarchy(data[0]);
            }

            // Try again to find node now
            node = this.fileTree.container.querySelector(`[rel="${path}"]`);
            if (node) {
                console.log('✅ Node found after loading hierarchy:', path);
                return node;
            }

            console.warn('⚠️ Node still not found after loading hierarchy:', path);
            return null;

        } catch (error) {
            console.error('❌ Error loading hierarchy:', error);
            return null;
        }
    }

    /**
     * Recursively expands a hierarchy of nodes in the tree
     */
    async expandHierarchy(hierarchyNode) {
        if (!hierarchyNode) return;

        const nodeId = hierarchyNode.encPath || hierarchyNode.metadata?.encPath || hierarchyNode.attr?.id;
        console.log('📂 Expanding hierarchy for:', nodeId);

        // Find node in tree
        let treeNode = this.fileTree.container.querySelector(`[rel="${nodeId}"]`);

        if (!treeNode) {
            console.warn('⚠️ Tree node not found for:', nodeId);
            return;
        }

        // If node has children in hierarchy, open it
        if (hierarchyNode.children && hierarchyNode.children.length > 0) {
            console.log(`  Opening node with ${hierarchyNode.children.length} children`);

            // Open node (this will load and render children)
            if (treeNode.classList.contains('closed')) {
                await this.fileTree.openNode(treeNode);
                await new Promise(resolve => setTimeout(resolve, 200));
            }

            // Recursively expand children
            for (const child of hierarchyNode.children) {
                await this.expandHierarchy(child);
            }
        }
    }


    // File operations

    async copyFiles() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length === 0) {
            UIComponents.showError(window.i18n?.noFileSelected || 'No file selected.');
            return;
        }

        try {
            await AjaxManager.post(this.config.prepareCopyFilesURL, {
                dirs: selectedPaths.join(',')
            });

            this.clipboard = { files: selectedPaths, operation: 'copy' };

            UIComponents.showInfo((window.i18n?.itemsCopied || '{0} item(s) copied.').replace('{0}', selectedPaths.length));

            this.updatePasteButtonState();
        } catch (error) {
            console.error('Copy failed:', error);
            UIComponents.showError(window.i18n?.copyFailed || 'Copy failed.');
        }
    }

    async cutFiles() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length === 0) {
            UIComponents.showError(window.i18n?.noFileSelected || 'No file selected.');
            return;
        }

        try {
            await AjaxManager.post(this.config.prepareCutFilesURL, {
                dirs: selectedPaths.join(',')
            });

            this.clipboard = { files: selectedPaths, operation: 'cut' };

            UIComponents.showInfo((window.i18n?.itemsCut || '{0} item(s) cut.').replace('{0}', selectedPaths.length));

            this.updatePasteButtonState();
        } catch (error) {
            console.error('Cut failed:', error);
            UIComponents.showError(window.i18n?.cutFailed || 'Cut failed.');
        }
    }

    async pasteFiles() {
        if (this.clipboard.files.length === 0) {
            UIComponents.showError(window.i18n?.clipboardEmpty || 'Clipboard is empty.');
            return;
        }

        const currentPath = this.getCurrentPath();

        UIComponents.showWaitCursor();

        try {
            const result = await AjaxManager.post(this.config.pastFilesURL, {
                dir: currentPath
            });

            if (result.status) {
                UIComponents.showInfo(result.msg || window.i18n?.pasteOk || '');
                this.clipboard = { files: [], operation: null };

                // Disable paste button after pasting
                this.updatePasteButtonState();

                // Refresh only content, no tree sync needed (already in right place)
                await this.refreshCurrentView(true, false, false);
            } else {
                UIComponents.showError(result.msg || window.i18n?.pasteFailed || '');
            }
        } catch (error) {
            console.error('Paste failed:', error);
            UIComponents.showError(window.i18n?.pasteFailed || 'Paste error.');
        } finally {
            UIComponents.hideWaitCursor();
        }
    }

    async deleteFiles() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length === 0) {
            UIComponents.showError(window.i18n?.noFileSelected || 'No file selected.');
            return;
        }

        UIComponents.showDialog({
            title: window.i18n?.deleteConfirmTitle || 'Delete confirmation',
            message: window.i18n?.deleteConfirmMessage || 'These items will be deleted permanently. Are you sure?',
            buttons: [
                {
                    text: window.i18n?.deleteConfirmCancel || 'Cancel',
                    type: 'secondary'
                },
                {
                    text: window.i18n?.deleteButton || 'Delete',
                    type: 'danger',
                    action: async () => {
                        try {
                            // Save current path before deletion
                            const currentPath = this.getCurrentPath();

                            // Check if folders are selected
                            const hasFolders = selectedPaths.some(path => {
                                const checkbox = document.querySelector(`.browsercheck[value="${path}"]`);
                                const tr = checkbox?.closest('tr');
                                return tr?.querySelector('.fileTreeRef') !== null;
                            });

                            const result = await AjaxManager.post(this.config.removeFilesURL, {
                                dirs: selectedPaths.join(',')
                            });

                            if (result.status) {
                                UIComponents.showInfo(result.msg || window.i18n?.removeOk || '');

                                // Refresh only content
                                await this.refreshCurrentView(true, false, false);

                                // If folders were deleted, refresh parent node in tree
                                if (hasFolders) {
                                    await this.refreshTreeNode(currentPath);
                                }
                            } else {
                                UIComponents.showError(result.msg || window.i18n?.removeFailed || '');
                            }
                        } catch (error) {
                            console.error('Delete failed:', error);
                            UIComponents.showError(window.i18n?.removeFailed || 'Deletion error.');
                        }
                    }
                }
            ]
        });
    }

    async handleCreateDirectory() {
        const currentPath = this.getCurrentPath();

        UIComponents.showDialog({
            title: window.i18n?.newFolder || 'New Folder',
            message: `<input type="text" id="newFolderName" class="form-control" placeholder="${window.i18n?.newFolderPlaceholder || 'Folder name'}">`,
            buttons: [
                { text: window.i18n?.deleteConfirmCancel || 'Cancel', type: 'secondary' },
                {
                    text: window.i18n?.createButton || 'Create',
                    type: 'primary',
                    action: async () => {
                        const folderName = document.getElementById('newFolderName')?.value;
                        if (!folderName) {
                            UIComponents.showError(window.i18n?.enterFolderName || 'Please enter a folder name.');
                            return;
                        }

                        try {
                            const result = await AjaxManager.post(this.config.createFileURL, {
                                parentDir: currentPath,
                                title: folderName,
                                type: 'folder'
                            });

                            if (result.status) {
                                UIComponents.showInfo(result.msg || window.i18n?.createSuccess || '');

                                // Refresh only content and parent node in tree
                                await this.refreshCurrentView(true, false, false);

                                // Refresh parent node in tree to see new folder
                                await this.refreshTreeNode(currentPath);
                            } else {
                                UIComponents.showError(result.msg || window.i18n?.createFailed || '');
                            }
                        } catch (error) {
                            console.error('Create directory failed:', error);
                            UIComponents.showError(window.i18n?.createFailed || 'Folder creation error.');
                        }
                    }
                }
            ]
        });
    }

    async handleRename() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length !== 1) {
            UIComponents.showError(window.i18n?.selectOneItem || 'Please select exactly one item.');
            return;
        }

        const path = selectedPaths[0];

        // Get file name from DOM
        const checkboxes = document.querySelectorAll('.browsercheck');
        let checkbox = null;
        for (const cb of checkboxes) {
            if (cb.value === path) {
                checkbox = cb;
                break;
            }
        }

        if (!checkbox) {
            UIComponents.showError(window.i18n?.cannotFindFile || 'Cannot find selected file.');
            return;
        }

        const tr = checkbox.closest('tr');
        const link = tr?.querySelector('.fileTreeRef, .file');
        const titleSpan = link?.querySelector('span');
        const currentName = titleSpan?.textContent || '';

        if (!currentName) {
            UIComponents.showError(window.i18n?.cannotRetrieveFileName || 'Cannot retrieve file name.');
            return;
        }

        UIComponents.showDialog({
            title: window.i18n?.rename || 'Rename',
            message: `<input type="text" id="newFileName" class="form-control" value="${this.escapeHtml(currentName)}">`,
            buttons: [
                { text: window.i18n?.deleteConfirmCancel || 'Cancel', type: 'secondary' },
                {
                    text: window.i18n?.rename || 'Rename',
                    type: 'primary',
                    action: async () => {
                        const newName = document.getElementById('newFileName')?.value;
                        if (!newName) {
                            UIComponents.showError(window.i18n?.enterNewName || 'Please enter a new name.');
                            return;
                        }

                        if (newName === currentName) {
                            UIComponents.showInfo(window.i18n?.nameUnchanged || 'Name has not changed.');
                            return;
                        }

                        try {
                            const result = await AjaxManager.post(this.config.renameFileURL, {
                                dir: path,
                                title: newName
                            });

                            if (result.status) {
                                UIComponents.showInfo(result.msg || window.i18n?.renameSuccess || '');

                                // Check if it's a folder to refresh tree
                                const isFolder = tr?.querySelector('.fileTreeRef') !== null;

                                // Refresh content
                                await this.refreshCurrentView(true, false, false);

                                // If it's a folder, also refresh tree
                                if (isFolder) {
                                    const currentPath = this.getCurrentPath();
                                    await this.refreshTreeNode(currentPath);
                                }
                            } else {
                                UIComponents.showError(result.msg || window.i18n?.renameFailed || '');
                            }
                        } catch (error) {
                            console.error('Rename failed:', error);
                            UIComponents.showError(window.i18n?.renameFailed || 'Rename error.');
                        }
                    }
                }
            ]
        });
    }

    downloadFile(path) {
        window.location.href = this.config.downloadFileURL + '?dir=' + encodeURIComponent(path);
    }

    downloadSelectedFiles() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length === 0) {
            UIComponents.showError(window.i18n?.noFileSelected || 'No file selected.');
            return;
        }

        if (selectedPaths.length === 1) {
            this.downloadFile(selectedPaths[0]);
        } else {
            // Download as ZIP
            window.location.href = this.config.downloadZipURL + '?dirs=' +
                selectedPaths.map(p => encodeURIComponent(p)).join(',');
        }
    }

    downloadSelectedAsZip() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length === 0) {
            UIComponents.showError(window.i18n?.noFileSelected || 'No file selected.');
            return;
        }

        // Always download as ZIP
        window.location.href = this.config.downloadZipURL + '?dirs=' +
            selectedPaths.map(p => encodeURIComponent(p)).join(',');
    }

    handleFileDrop(sourceData, targetPath) {
        console.log('File dropped:', sourceData, 'to', targetPath);

        this.clipboard = {
            files: [sourceData.path],
            operation: 'cut'
        };

        // Simulate paste on target directory
        const currentPath = this.getCurrentPath();
        this.navigateToPath(targetPath).then(() => {
            this.pasteFiles().then(() => {
                this.navigateToPath(currentPath);
            });
        });
    }

    // Utilities

    getSelectedFilePaths() {
        const checkboxes = document.querySelectorAll('.browsercheck:checked');
        return Array.from(checkboxes).map(cb => cb.value);
    }

    getCurrentPath() {
        // Use memorized path first if available
        if (this.currentPath) {
            return this.currentPath;
        }

        // Fallback to DOM
        const bigDirectory = document.getElementById('bigdirectory');
        const path = bigDirectory ? bigDirectory.getAttribute('rel') : this.config.defaultPath;

        // Memorize for next calls
        if (path) {
            this.currentPath = path;
        }

        return path;
    }

    /**
     * Refreshes current view optimally
     * @param {boolean} preserveScroll - Preserve scroll position (default: true)
     * @param {boolean} syncTree - Synchronize tree (default: false, since already in right place)
     * @param {boolean} refreshTree - Reload entire tree (default: false, very expensive)
     */
    async refreshCurrentView(preserveScroll = true, syncTree = false, refreshTree = false) {
        // Save current path BEFORE any DOM manipulation
        const currentPath = this.getCurrentPath();
        console.log('🔄 Refreshing current view for path:', currentPath, { preserveScroll, syncTree, refreshTree });

        // If no valid path, do nothing
        if (!currentPath) {
            console.warn('No valid path to refresh');
            return;
        }

        // Reload only current directory content
        await this.loadDirectoryContent(currentPath, syncTree, preserveScroll);

        // Refresh tree ONLY if explicitly requested (very rare)
        if (refreshTree && this.fileTree) {
            console.log('🔄 Full tree refresh requested');
            this.fileTree.refresh();
        }
    }

    /**
     * Handles refresh button click
     */
    async handleRefresh() {
        console.log('🔄 Refresh button clicked');
        UIComponents.showWaitCursor();
        try {
            // Refresh current view with scroll preservation
            await this.refreshCurrentView(true, true, false);
            UIComponents.showInfo(window.i18n?.viewRefreshed || 'View refreshed.');
        } catch (error) {
            console.error('Refresh failed:', error);
            UIComponents.showError(window.i18n?.refreshFailed || 'Error refreshing view.');
        } finally {
            UIComponents.hideWaitCursor();
        }
    }

    /**
     * Saves current path in localStorage and URL hash
     * @param {string} path - Path to save
     */
    saveCurrentPath(path) {
        if (!path) return;

        try {
            // Save in localStorage
            localStorage.setItem('esupfm.currentPath', path);

            // Save in URL hash (without reloading page)
            const encodedPath = encodeURIComponent(path);
            if (window.location.hash !== `#path=${encodedPath}`) {
                window.history.replaceState(null, '', `#path=${encodedPath}`);
            }

            console.log('💾 Path saved:', path);
        } catch (error) {
            console.error('Failed to save path:', error);
        }
    }

    /**
     * Gets saved path from URL hash or localStorage
     * @returns {string|null} Saved path or null
     */
    getSavedPath() {
        try {
            // First try URL hash (higher priority)
            const hash = window.location.hash;
            if (hash && hash.startsWith('#path=')) {
                const path = decodeURIComponent(hash.substring(6));
                console.log('📖 Path restored from URL hash:', path);
                return path;
            }

            // Otherwise try localStorage
            const path = localStorage.getItem('esupfm.currentPath');
            if (path) {
                console.log('📖 Path restored from localStorage:', path);
                return path;
            }
        } catch (error) {
            console.error('Failed to get saved path:', error);
        }

        return null;
    }

    /**
     * Restores saved path at startup
     */
    restoreSavedPath() {
        const savedPath = this.getSavedPath();
        if (savedPath) {
            this.currentPath = savedPath;
            console.log('✅ Path restored on startup:', savedPath);
        }
    }

    isValidPaste(sourcePath, targetPath) {
        if (sourcePath === targetPath) return false;

        const sourceParent = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
        if (sourceParent === targetPath) return false;

        return true;
    }

    refreshTree() {
        if (this.fileTree) {
            this.fileTree.refresh();
        }
    }

    /**
     * Refreshes a specific tree node (and its children)
     * @param {string} path - Path of node to refresh
     */
    async refreshTreeNode(path) {
        if (!this.fileTree || !path) {
            console.warn('refreshTreeNode: Invalid parameters');
            return;
        }

        console.log('🔄 Refreshing tree node for path:', path);

        try {
            // Find node corresponding to path
            const node = this.fileTree.getNodeByPath(path);

            if (node) {
                console.log('✅ Found node, refreshing it');
                // Refresh only this node
                this.fileTree.refresh(node);
            } else {
                console.warn('⚠️ Node not found in tree, cannot refresh:', path);
            }
        } catch (error) {
            console.error('Error refreshing tree node:', error);
        }
    }

    async handleFileSelection() {
        // Update interface based on selection
        const selectedPaths = this.getSelectedFilePaths();
        const selectedCount = selectedPaths.length;
        console.log(`${selectedCount} file(s) selected`);

        // Enable/disable toolbar buttons
        this.updateToolbarButtons(selectedCount > 0);

        // Update details
        await this.updateDetailsForSelection(selectedPaths);
    }

    async updateDetailsForSelection(selectedPaths) {
        const selectedCount = selectedPaths.length;

        if (selectedCount === 0) {
            // No selection: display details_empty
            await this.loadDetailsArea('', 'empty');
        } else if (selectedCount === 1) {
            // Single item: determine its type
            const path = selectedPaths[0];
            const type = this.getFileTypeFromPath(path);
            await this.loadDetailsArea(path, type);
        } else {
            // Multiple items: display details_files
            await this.loadDetailsAreaMultiple(selectedPaths);
        }
    }

    getFileTypeFromPath(path) {
        const checkbox = document.querySelector(`.browsercheck[value="${path}"]`);
        if (!checkbox) return 'file';

        const tr = checkbox.closest('tr');
        if (!tr) return 'file';

        // Check if it's a folder (has a .fileTreeRef link)
        if (tr.querySelector('.fileTreeRef')) {
            return 'folder';
        }

        // Check if it's a link
        if (tr.classList.contains('link')) {
            return 'link';
        }

        return 'file';
    }

    async loadDetailsAreaMultiple(paths) {
        try {
            console.log('Loading details for multiple files:', paths.length, paths);

            // Create URLSearchParams with all paths
            const params = new URLSearchParams();
            paths.forEach(path => {
                params.append('dirs', path);
            });

            console.log('POST params:', params.toString());

            // Use fetch with URLSearchParams for POST
            const response = await fetch(this.config.detailsAreaURL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                },
                body: params.toString()
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const html = await response.text();

            const detailArea = document.getElementById('detailArea');
            if (detailArea) {
                detailArea.innerHTML = html;

                // Reinitialize details manager
                if (window.reinitDetailsManager) {
                    window.reinitDetailsManager();
                }

                console.log('Multiple details loaded into detailArea');
            }
        } catch (error) {
            console.error('Failed to load multiple details:', error);
        }
    }

    updateToolbarButtons(hasSelection) {
        const buttons = ['copy', 'cut', 'delete', 'rename', 'download', 'zip'];
        buttons.forEach(btnId => {
            const btn = document.getElementById(`toolbar-${btnId}`);
            if (btn) {
                btn.disabled = !hasSelection;
                if (hasSelection) {
                    btn.classList.remove('disabled');
                } else {
                    btn.classList.add('disabled');
                }
            }
        });

        // Also update mobile buttons
        const mobileButtons = ['download', 'copy', 'cut', 'delete', 'rename'];
        mobileButtons.forEach(btnId => {
            const mobileBtn = document.getElementById(`mobile-${btnId}`);
            if (mobileBtn) {
                mobileBtn.disabled = !hasSelection;
                if (hasSelection) {
                    mobileBtn.classList.remove('disabled');
                } else {
                    mobileBtn.classList.add('disabled');
                }
            }

            // Mobile toolbar buttons at bottom
            const mobileToolbarBtn = document.getElementById(`mobile-toolbar-${btnId}`);
            if (mobileToolbarBtn) {
                mobileToolbarBtn.disabled = !hasSelection;
                if (hasSelection) {
                    mobileToolbarBtn.classList.remove('disabled');
                } else {
                    mobileToolbarBtn.classList.add('disabled');
                }
            }
        });
    }

    updatePasteButtonState() {
        // Paste button must be enabled if clipboard contains files
        const hasClipboardContent = this.clipboard.files.length > 0;

        // Desktop toolbar button
        const pasteBtn = document.getElementById('toolbar-paste');
        if (pasteBtn) {
            pasteBtn.disabled = !hasClipboardContent;
            if (hasClipboardContent) {
                pasteBtn.classList.remove('disabled');
            } else {
                pasteBtn.classList.add('disabled');
            }
        }

        // Mobile button
        const mobilePasteBtn = document.getElementById('mobile-paste');
        if (mobilePasteBtn) {
            mobilePasteBtn.disabled = !hasClipboardContent;
            if (hasClipboardContent) {
                mobilePasteBtn.classList.remove('disabled');
            } else {
                mobilePasteBtn.classList.add('disabled');
            }
        }

        console.log('Paste button state updated:', hasClipboardContent ? 'enabled' : 'disabled');
    }

    selectFolder(link) {
        // Visually select folder
        const tr = link.closest('tr');
        if (tr) {
            const checkbox = tr.querySelector('.browsercheck');
            if (checkbox) {
                checkbox.checked = !checkbox.checked;
                this.handleFileSelection();
            }
        }
    }

    selectFile(link) {
        // Visually select file
        const tr = link.closest('tr');
        if (tr) {
            const checkbox = tr.querySelector('.browsercheck');
            if (checkbox) {
                checkbox.checked = !checkbox.checked;
                this.handleFileSelection();
            }
        }
    }

    handleEnterKey() {
        const selectedPaths = this.getSelectedFilePaths();
        if (selectedPaths.length === 1) {
            const path = selectedPaths[0];
            // Check if it's a folder or file
            const checkbox = document.querySelector(`.browsercheck[value="${path}"]`);
            const tr = checkbox?.closest('tr');
            const isFolder = tr?.querySelector('.fileTreeRef') !== null;

            if (isFolder) {
                this.navigateToPath(path);
            } else {
                this.downloadFile(path);
            }
        }
    }

    handleUpload() {
        // Trigger file selection via button
        if (this.uploadManager) {
            this.uploadManager.triggerFileSelection();
        } else {
            console.error('Upload manager not initialized');
        }
    }

    async sortFiles(sortField) {
        const currentPath = this.getCurrentPath();

        // Save sort field
        this.currentSortField = sortField;

        try {
            // Save scroll position
            const browserArea = document.getElementById('browserArea');
            const savedScrollTop = browserArea ? browserArea.scrollTop : 0;
            const savedScrollLeft = browserArea ? browserArea.scrollLeft : 0;

            const html = await AjaxManager.loadHTML(this.config.htmlFileTreeURL, {
                dir: currentPath,
                sortField: sortField
            });

            const browserMain = document.getElementById('browserMain');

            if (browserMain) {
                browserMain.innerHTML = html;
            } else if (browserArea) {
                browserArea.innerHTML = html;
            }

            // Restore scroll position
            if (browserArea) {
                requestAnimationFrame(() => {
                    browserArea.scrollTop = savedScrollTop;
                    browserArea.scrollLeft = savedScrollLeft;
                });
            }

            this.refreshDragDrop();
        } catch (error) {
            console.error('Sort failed:', error);
            UIComponents.showError(window.i18n?.sortFailed || 'Error sorting files.');
        }
    }

    /**
     * Crée le panneau flottant de progression d'upload
     */
    createUploadPanel() {
        const existing = document.getElementById('upload-progress-panel');
        if (existing) existing.remove();

        const panel = document.createElement('div');
        panel.id = 'upload-progress-panel';
        panel.className = 'upload-panel card shadow-lg';
        panel.setAttribute('role', 'status');
        panel.setAttribute('aria-live', 'polite');
        panel.innerHTML = `
            <div class="upload-panel-header card-header d-flex justify-content-between align-items-center py-2 px-3">
                <span class="fw-semibold small text-white">
                    <i class="bi bi-cloud-upload me-1"></i>
                    <span id="upload-panel-title">Uploading…</span>
                </span>
                <div class="d-flex gap-1 ms-2">
                    <button class="btn btn-sm upload-panel-btn upload-panel-minimize-btn"
                            title="Minimize" aria-label="Minimize upload panel">
                        <i class="bi bi-dash-lg"></i>
                    </button>
                    <button class="btn btn-sm upload-panel-btn upload-panel-close-btn"
                            title="Close" aria-label="Close upload panel">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>
            </div>
            <div class="upload-panel-body" id="upload-panel-body">
                <ul class="list-group list-group-flush" id="upload-file-list" aria-label="File list"></ul>
            </div>
            <div class="upload-panel-footer card-footer py-2 px-3">
                <div class="progress mb-1" style="height:5px;" role="progressbar"
                     aria-label="Overall progress" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">
                    <div id="upload-total-progress-bar"
                         class="progress-bar progress-bar-striped progress-bar-animated bg-primary"
                         style="width:0%"></div>
                </div>
                <div class="text-muted small text-center" id="upload-total-label">0 / 0 file</div>
            </div>
        `;

        // Réduire / restaurer
        panel.querySelector('.upload-panel-minimize-btn').addEventListener('click', () => {
            const minimized = panel.classList.toggle('minimized');
            panel.querySelector('.upload-panel-minimize-btn i').className =
                minimized ? 'bi bi-plus-lg' : 'bi bi-dash-lg';
        });

        // Fermer
        panel.querySelector('.upload-panel-close-btn').addEventListener('click', () => {
            panel.remove();
        });

        document.body.appendChild(panel);
        return panel;
    }

    /**
     * Retourne le panneau existant ou en crée un nouveau
     */
    getUploadPanel() {
        return document.getElementById('upload-progress-panel') || this.createUploadPanel();
    }

    /**
     * Affiche le panneau d'upload
     */
    showUploadPanel() {
        const panel = this.getUploadPanel();
        panel.classList.remove('d-none', 'minimized');
        panel.style.display = '';
    }

    /**
     * Ferme le panneau d'upload
     */
    closeUploadPanel() {
        const panel = document.getElementById('upload-progress-panel');
        if (panel) panel.remove();
    }

    /**
     * Ajoute une entrée "en attente" pour un fichier dans le panneau
     */
    addFileToUploadPanel(file) {
        const panel = this.getUploadPanel();
        const list = panel.querySelector('#upload-file-list');
        if (!list) return;

        const itemId = `upload-item-${this.sanitizeFilename(file.name)}`;
        if (document.getElementById(itemId)) return; // avoid duplicates

        const item = document.createElement('li');
        item.id = itemId;
        item.className = 'list-group-item upload-file-item py-2 px-3';
        item.innerHTML = `
            <div class="d-flex justify-content-between align-items-center mb-1">
                <span class="upload-filename small fw-medium text-truncate me-2"
                      title="${this.escapeHtml(file.name)}">${this.escapeHtml(file.name)}</span>
                <span class="upload-percent small text-muted">Pending…</span>
            </div>
            <div class="progress" style="height:3px;">
                <div class="progress-bar bg-secondary" role="progressbar"
                     style="width:0%" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
        `;
        list.appendChild(item);
    }

    /**
     * Change l'état visuel d'un fichier dans le panneau
     * @param {File} file
     * @param {'uploading'|'complete'|'error'} state
     */
    setUploadItemState(file, state) {
        const item = document.getElementById(`upload-item-${this.sanitizeFilename(file.name)}`);
        if (!item) return;

        const bar = item.querySelector('.progress-bar');
        const pct = item.querySelector('.upload-percent');

        switch (state) {
            case 'uploading':
                bar.className = 'progress-bar progress-bar-striped progress-bar-animated bg-primary';
                bar.style.width = '1%';
                if (pct) { pct.textContent = '0%'; pct.className = 'upload-percent small text-primary'; }
                break;
            case 'complete':
                bar.className = 'progress-bar bg-success';
                bar.style.width = '100%';
                bar.setAttribute('aria-valuenow', 100);
                if (pct) pct.innerHTML = '<i class="bi bi-check-circle-fill text-success"></i>';
                break;
            case 'error':
                bar.className = 'progress-bar bg-danger';
                bar.style.width = '100%';
                bar.setAttribute('aria-valuenow', 100);
                if (pct) pct.innerHTML = '<i class="bi bi-x-circle-fill text-danger"></i>';
                break;
        }
    }

    /**
     * Met à jour la barre de progression globale et le compteur dans le panneau
     */
    updateUploadPanelStats() {
        const panel = document.getElementById('upload-progress-panel');
        if (!panel) return;

        const { total, completed, failed } = this.uploadStats;
        const done = completed + failed;

        // Titre
        const titleEl = panel.querySelector('#upload-panel-title');
        if (titleEl) titleEl.textContent = `Uploading (${done}/${total})`;

        // Libellé pied de panneau
        const labelEl = panel.querySelector('#upload-total-label');
        if (labelEl) {
            let text = `${done} / ${total} file${total > 1 ? 's' : ''}`;
            if (failed > 0) {
                labelEl.innerHTML = `${text} — <span class="text-danger">${failed} error${failed > 1 ? 's' : ''}</span>`;
            } else {
                labelEl.textContent = text;
            }
        }

        // Barre globale
        const totalBar = panel.querySelector('#upload-total-progress-bar');
        if (totalBar && total > 0) {
            const percent = Math.round((done / total) * 100);
            totalBar.style.width = `${percent}%`;
            totalBar.setAttribute('aria-valuenow', percent);
            if (done === total) {
                totalBar.classList.remove('progress-bar-animated');
                totalBar.classList.add(failed > 0 ? 'bg-warning' : 'bg-success');
                totalBar.classList.remove('bg-primary');
            }
        }
    }

    /**
     * Met à jour la barre de progression d'un fichier individuel
     */
    showUploadProgress(file, percent) {
        // L'item est déjà créé par addFileToUploadPanel ; on passe juste en mode "uploading"
        this.setUploadItemState(file, 'uploading');
    }

    /**
     * Met à jour le pourcentage affiché pour un fichier
     */
    updateUploadProgress(file, percent) {
        const item = document.getElementById(`upload-item-${this.sanitizeFilename(file.name)}`);
        if (!item) return;

        const bar = item.querySelector('.progress-bar');
        const pct = item.querySelector('.upload-percent');

        if (bar) {
            bar.style.width = `${percent}%`;
            bar.setAttribute('aria-valuenow', Math.round(percent));
        }
        if (pct) pct.textContent = `${Math.round(percent)}%`;
    }

    /**
     * Marque un fichier comme terminé dans le panneau (appelé après succès)
     */
    hideUploadProgress(file) {
        this.setUploadItemState(file, 'complete');
    }

    /**
     * Sanitizes filename for use as ID
     */
    sanitizeFilename(filename) {
        return filename.replace(/[^a-zA-Z0-9]/g, '_');
    }

    /**
     * Escapes HTML characters to prevent XSS
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize application on DOM load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.esupFileManager = new EsupFileManager();
    });
} else {
    window.esupFileManager = new EsupFileManager();
}

export default EsupFileManager;


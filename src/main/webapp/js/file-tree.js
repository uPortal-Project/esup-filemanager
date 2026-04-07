/**
 * File Tree Manager - Vanilla JS file tree
 * Native file tree implementation with lazy loading, selection, and custom events
 */

import { AjaxManager } from './ajax-manager.js';

export class FileTree {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.options = {
            ajaxUrl: options.ajaxUrl || '/fileChildren',
            defaultPath: options.defaultPath || '/',
            onSelect: options.onSelect || (() => {}),
            onOpen: options.onOpen || (() => {}),
            onLoad: options.onLoad || (() => {}),
            ...options
        };
        this.selectedNode = null;
        this.loadingNodes = new Set();
        this.init();
    }

    init() {
        this.container.innerHTML = '<ul class="file-tree-root"></ul>';
        this.rootList = this.container.querySelector('.file-tree-root');
        this.loadRootNodes();
    }

    async loadRootNodes() {
        try {
            const data = await AjaxManager.post(this.options.ajaxUrl, {
                dir: this.options.defaultPath,
                hierarchy: 'all'
            });

            console.log('Root nodes loaded:', data);

            // Le serveur retourne un tableau avec potentiellement une structure hiérarchique
            this.renderNodes(data, this.rootList, null, true);
            this.options.onLoad(data);
        } catch (error) {
            console.error('Failed to load root nodes:', error);
        }
    }

    renderNodes(nodes, parentElement, parentPath, autoExpand = false) {
        if (!Array.isArray(nodes)) {
            console.warn('renderNodes expects an array, got:', nodes);
            return;
        }

        nodes.forEach(node => {
            const li = this.createNodeElement(node, parentPath, autoExpand);
            parentElement.appendChild(li);
        });
    }

    createNodeElement(node, parentPath, autoExpand = false) {
        const li = document.createElement('li');
        li.className = 'tree-node';

        // Store node information
        const encPath = node.encPath || node.metadata?.encPath || node.attr?.id;
        const nodeId = node.attr?.id || encPath;  // Unique node ID
        const nodeType = node.type || node.attr?.rel || 'folder';
        const nodeTitle = node.title || node.data?.title || node.data || '';
        const nodeIcon = node.icon || node.data?.icon || '/img/folder.png';

        li.dataset.path = node.path || encPath;
        li.dataset.encPath = encPath;
        li.dataset.type = nodeType;

        // Add rel attribute with ID
        if (nodeId) {
            li.setAttribute('rel', nodeId);
            li.id = 'tree-node-' + nodeId.replace(/[^a-zA-Z0-9_-]/g, '_');
        }

        // Determine if node can have children
        const hasChildren = node.children ||
                          nodeType === 'root' ||
                          nodeType === 'category' ||
                          nodeType === 'drive' ||
                          nodeType === 'folder' ||
                          node.state === 'closed';

        if (hasChildren) {
            li.classList.add('has-children');
            // If we already have children and autoExpand, open by default
            if (node.children && autoExpand) {
                li.classList.add('open');
            } else {
                li.classList.add('closed');
            }
        } else {
            li.classList.add('leaf');
        }

        // Create node content
        const content = document.createElement('div');
        content.className = 'tree-node-content';

        // Toggle for nodes with children
        if (hasChildren) {
            const toggle = document.createElement('span');
            toggle.className = 'tree-toggle';
            toggle.onclick = (e) => {
                e.stopPropagation();
                this.toggleNode(li);
            };
            content.appendChild(toggle);
        } else {
            // Spacer for alignment
            const spacer = document.createElement('span');
            spacer.className = 'tree-toggle-spacer';
            content.appendChild(spacer);
        }

        // Icon
        const icon = document.createElement('img');
        icon.src = nodeIcon;
        icon.className = 'tree-node-icon';
        icon.alt = 'icon';
        content.appendChild(icon);

        // Label
        const label = document.createElement('span');
        label.className = 'tree-node-label';
        label.textContent = nodeTitle;
        content.appendChild(label);

        li.appendChild(content);

        // Container for children
        if (hasChildren) {
            const childrenContainer = document.createElement('ul');
            childrenContainer.className = 'tree-node-children';

            // If we already have children in response, display them
            if (node.children && Array.isArray(node.children)) {
                this.renderNodes(node.children, childrenContainer, encPath, autoExpand);
                if (autoExpand) {
                    childrenContainer.style.display = 'block';
                } else {
                    childrenContainer.style.display = 'none';
                }
            } else {
                childrenContainer.style.display = 'none';
            }

            li.appendChild(childrenContainer);
        }

        // Event listener for selection (except for invisible root)
        if (nodeType !== 'root' || nodeTitle) {
            content.onclick = () => this.selectNode(li);
        }

        return li;
    }

    async toggleNode(nodeElement) {
        if (nodeElement.classList.contains('open')) {
            this.closeNode(nodeElement);
        } else {
            await this.openNode(nodeElement);
        }
    }

    async openNode(nodeElement) {
        if (nodeElement.classList.contains('open')) return;

        const path = nodeElement.dataset.encPath;
        const childrenContainer = nodeElement.querySelector('.tree-node-children');

        if (!childrenContainer) return;

        // Load children only if not already present
        if (childrenContainer.children.length === 0) {
            try {
                const data = await AjaxManager.post(this.options.ajaxUrl, {
                    dir: path,
                    hierarchy: '' // No hierarchy for sub-levels
                });

                console.log(`📥 Loaded children for ${path}`);
                console.log(`   Response type: ${Array.isArray(data) ? 'Array' : typeof data}`);
                console.log(`   Children count: ${Array.isArray(data) ? data.length : 'N/A'}`);

                if (Array.isArray(data) && data.length > 0) {
                    console.log(`   First child:`, data[0].title || data[0].data?.title);
                    console.log(`   Children titles:`, data.map(d => d.title || d.data?.title || '(no title)'));
                }

                // If response contains children (hierarchical format)
                if (Array.isArray(data)) {
                    this.renderNodes(data, childrenContainer, path, false);
                    console.log(`   ✅ Rendered ${data.length} nodes into DOM`);
                } else {
                    console.warn(`   ⚠️ Data is not an array:`, data);
                }

                this.options.onOpen(nodeElement, data);
            } catch (error) {
                console.error('Failed to load node children:', error);
            }
        } else {
            console.log(`♻️ Children already loaded for ${path} (${childrenContainer.children.length} nodes)`);
        }

        // Open node
        nodeElement.classList.remove('closed');
        nodeElement.classList.add('open');
        childrenContainer.style.display = 'block';
    }

    closeNode(nodeElement) {
        nodeElement.classList.remove('open');
        nodeElement.classList.add('closed');
        const childrenContainer = nodeElement.querySelector('.tree-node-children');
        if (childrenContainer) childrenContainer.style.display = 'none';
    }

    selectNode(nodeElement) {
        if (this.selectedNode) this.selectedNode.classList.remove('selected');
        nodeElement.classList.add('selected');
        this.selectedNode = nodeElement;
        const path = nodeElement.dataset.encPath;
        this.options.onSelect(nodeElement, path);
    }

    refresh(nodeElement = null) {
        if (!nodeElement) {
            this.rootList.innerHTML = '';
            this.loadRootNodes();
        } else {
            const childrenContainer = nodeElement.querySelector('.tree-node-children');
            if (childrenContainer) {
                childrenContainer.innerHTML = '';
                nodeElement.classList.remove('open');
                nodeElement.classList.add('closed');
                this.openNode(nodeElement);
            }
        }
    }

    getNodeByPath(path) {
        // Search first by rel attribute (ID)
        let node = this.container.querySelector(`[rel="${path}"]`);
        if (node) return node;

        // Search by data-enc-path
        node = this.container.querySelector(`[data-enc-path="${path}"]`);
        if (node) return node;

        // Search by data-path
        node = this.container.querySelector(`[data-path="${path}"]`);
        if (node) return node;

        // Iterate through all nodes to find a match
        const allNodes = this.container.querySelectorAll('.tree-node');
        for (const n of allNodes) {
            const rel = n.getAttribute('rel');
            if (rel === path) return n;
            if (n.dataset.encPath === path) return n;
            if (n.dataset.path === path) return n;
        }

        return null;
    }

    /**
     * Opens and selects a node by its path
     */
    async openAndSelectNode(path) {
        console.log('Opening and selecting node:', path);

        // Find the node
        let node = this.getNodeByPath(path);

        if (node) {
            // Open parents if necessary
            let parent = node.parentElement.closest('.tree-node');
            while (parent) {
                if (parent.classList.contains('closed')) {
                    await this.openNode(parent);
                }
                parent = parent.parentElement.closest('.tree-node');
            }

            // Select the node
            this.selectNode(node);
        } else {
            console.warn('Node not found for path:', path);
        }
    }
}


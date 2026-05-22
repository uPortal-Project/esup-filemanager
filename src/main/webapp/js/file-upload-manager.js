/**
 * File Upload Manager - Gestion de l'upload de fichiers
 * Support XHR, multiples fichiers, bouton et drag & drop depuis le bureau
 */
export class FileUploadManager {
    constructor(options = {}) {
        this.options = {
            uploadUrl: options.uploadUrl || './uploadFile',
            onUploadStart: options.onUploadStart || (() => {}),
            onUploadProgress: options.onUploadProgress || (() => {}),
            onUploadComplete: options.onUploadComplete || (() => {}),
            onUploadError: options.onUploadError || (() => {}),
            onAllUploadsComplete: options.onAllUploadsComplete || (() => {}),
            onFilesQueued: options.onFilesQueued || (() => {}),
            onFileExists: options.onFileExists || (() => {}),
            getCurrentPath: options.getCurrentPath || (() => './'),
            maxFileSize: options.maxFileSize || null, // null = pas de limite
            allowedExtensions: options.allowedExtensions || null, // null = toutes les extensions
            maxConcurrentUploads: options.maxConcurrentUploads || 3, // uploads simultanés
            ...options
        };

        this.activeUploads = [];
        this.uploadQueue = [];
        this.isUploading = false;
        this.concurrentCount = 0; // nombre d'uploads en cours
        this.conflictFiles = []; // files returned with fileExists=true

        this.init();
    }

    /**
     * Initialise le gestionnaire d'upload
     */
    init() {
        this.createFileInput();
        this.setupDropZone();
        console.log('File Upload Manager initialized');
    }

    /**
     * Crée l'input file caché pour l'upload via bouton
     */
    createFileInput() {
        // Supprimer l'ancien input s'il existe
        const oldInput = document.getElementById('upload-file-input');
        if (oldInput) {
            oldInput.remove();
        }

        // Créer un nouvel input file
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.id = 'upload-file-input';
        fileInput.multiple = true;
        fileInput.style.display = 'none';

        // Gérer la sélection de fichiers
        fileInput.addEventListener('change', (e) => {
            const files = Array.from(e.target.files);
            if (files.length > 0) {
                this.handleFiles(files);
            }
            // Réinitialiser l'input pour permettre de sélectionner le même fichier
            fileInput.value = '';
        });

        document.body.appendChild(fileInput);
    }

    /**
     * Configure la zone de drop pour les fichiers depuis le bureau
     */
    setupDropZone() {
        const browserArea = document.getElementById('browserArea');
        const browserMain = document.getElementById('browserMain');
        const dropZone = browserArea || browserMain || document.body;

        if (!dropZone) {
            console.warn('No drop zone found for file uploads');
            return;
        }

        // Empêcher le comportement par défaut du navigateur
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, (e) => {
                // Ne capturer que les événements de fichiers externes
                if (e.dataTransfer && e.dataTransfer.types && e.dataTransfer.types.includes('Files')) {
                    e.preventDefault();
                    e.stopPropagation();
                }
            }, false);
        });

        // Ajouter une classe visuelle lors du survol
        ['dragenter', 'dragover'].forEach(eventName => {
            dropZone.addEventListener(eventName, (e) => {
                if (e.dataTransfer && e.dataTransfer.types && e.dataTransfer.types.includes('Files')) {
                    dropZone.classList.add('drag-over-upload');
                }
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, () => {
                dropZone.classList.remove('drag-over-upload');
            }, false);
        });

        // Gérer le drop de fichiers
        dropZone.addEventListener('drop', (e) => {
            if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                const files = Array.from(e.dataTransfer.files);
                this.handleFiles(files);
            }
        }, false);

        console.log('Drop zone configured for file uploads');
    }

    /**
     * Déclenche la sélection de fichiers via le bouton
     */
    triggerFileSelection() {
        const fileInput = document.getElementById('upload-file-input');
        if (fileInput) {
            fileInput.click();
        }
    }

    /**
     * Gère les fichiers sélectionnés ou droppés
     */
    handleFiles(files) {
        if (!files || files.length === 0) {
            return;
        }

        console.log(`Processing ${files.length} file(s) for upload`);

        // Valider et filtrer les fichiers
        const validFiles = files.filter(file => this.validateFile(file));

        if (validFiles.length === 0) {
            console.warn('No valid files to upload');
            return;
        }

        // Ajouter les fichiers à la queue
        validFiles.forEach(file => {
            this.uploadQueue.push(file);
        });

        // Notifier l'UI de tous les fichiers mis en attente
        this.options.onFilesQueued(validFiles);

        // Démarrer / compléter les slots d'upload disponibles
        this.processQueue();
    }

    /**
     * Valide un fichier avant l'upload
     */
    validateFile(file) {
        // Vérifier la taille
        if (this.options.maxFileSize && file.size > this.options.maxFileSize) {
            console.error(`File ${file.name} exceeds maximum size of ${this.options.maxFileSize} bytes`);
            this.options.onUploadError(file, new Error('File too large'));
            return false;
        }

        // Vérifier l'extension
        if (this.options.allowedExtensions) {
            const extension = file.name.split('.').pop().toLowerCase();
            if (!this.options.allowedExtensions.includes(extension)) {
                console.error(`File ${file.name} has disallowed extension`);
                this.options.onUploadError(file, new Error('File type not allowed'));
                return false;
            }
        }

        return true;
    }

    /**
     * Traite la queue d'upload avec gestion de la concurrence
     */
    processQueue() {
        const maxConcurrent = this.options.maxConcurrentUploads;

        // Lancer des uploads jusqu'à atteindre le maximum de slots simultanés
        while (this.uploadQueue.length > 0 && this.concurrentCount < maxConcurrent) {
            const file = this.uploadQueue.shift();
            this.isUploading = true;
            this.concurrentCount++;

            this.uploadFile(file)
                .catch(error => {
                    console.error('Upload error:', error);
                })
                .finally(() => {
                    this.concurrentCount--;

                    if (this.concurrentCount === 0 && this.uploadQueue.length === 0) {
                        // Tous les uploads sont terminés
                        this.isUploading = false;
                        // If some files triggered a conflict, resolve them before signalling completion
                        if (this.conflictFiles.length > 0) {
                            const conflicts = [...this.conflictFiles];
                            this.conflictFiles = [];
                            this.options.onFileExists(conflicts);
                        } else {
                            this.options.onAllUploadsComplete();
                        }
                    } else if (this.uploadQueue.length > 0) {
                        // Il reste des fichiers en attente, lancer le suivant
                        this.processQueue();
                    }
                    // Si concurrentCount > 0 et queue vide : les autres uploads en cours
                    // appelleront processQueue() à leur tour via finally()
                });
        }
    }

    /**
     * Upload un fichier via XHR
     */
    uploadFile(file) {
        return new Promise((resolve, reject) => {
            const currentPath = this.options.getCurrentPath();

            console.log(`Uploading ${file.name} to ${currentPath}`);

            this.options.onUploadStart(file, currentPath);

            if (window.EsupPresignedUrls) {
                window.EsupPresignedUrls.uploadFile(currentPath, file, {
                    onProgress: (percentComplete, loaded, total) => {
                        this.options.onUploadProgress(file, percentComplete, loaded, total);
                    },
                    onSuccess: (result) => {
                        // Classic fallback inside EsupPresignedUrls: check fileExists in inner response
                        if (result && result.response && result.response.fileExists) {
                            console.log(`File conflict detected (classic fallback) for ${file.name}`);
                            this.conflictFiles.push({ file, dir: currentPath });
                            resolve({ fileExists: true });
                        } else {
                            console.log(`Upload successful for ${file.name}`, result);
                            this.options.onUploadComplete(file, result);
                            resolve(result);
                        }
                    },
                    onError: (error) => {
                        console.error(`Upload failed for ${file.name}`, error);
                        this.options.onUploadError(file, error);
                        reject(error);
                    },
                    onFileExists: () => {
                        // Presigned URL path (S3): file exists detected server-side before upload
                        console.log(`File conflict detected (presigned/S3) for ${file.name}`);
                        this.conflictFiles.push({ file, dir: currentPath });
                        resolve({ fileExists: true });
                    }
                });
            } else {
                // Direct classic upload (no EsupPresignedUrls module)
                this.uploadFileClassic(file, currentPath)
                    .then(result => {
                        if (result && result.fileExists) {
                            // File conflict: accumulate for later confirmation dialog
                            console.log(`File conflict detected (classic) for ${file.name}`);
                            this.conflictFiles.push({ file, dir: currentPath });
                        }
                        resolve(result);
                    })
                    .catch(reject);
            }
        });
    }

    /**
     * Upload classique sans presigned URLs
     */
    uploadFileClassic(file, dir, uploadOption) {
        return new Promise((resolve, reject) => {
            const formData = new FormData();
            formData.append('dir', dir);
            formData.append('qqfile', file);
            if (uploadOption) {
                formData.append('uploadOption', uploadOption);
            }

            const xhr = new XMLHttpRequest();

            if (xhr.upload) {
                xhr.upload.addEventListener('progress', (e) => {
                    if (e.lengthComputable) {
                        const percentComplete = (e.loaded / e.total) * 100;
                        this.options.onUploadProgress(file, percentComplete, e.loaded, e.total);
                    }
                });
            }

            xhr.addEventListener('load', () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        if (response.fileExists) {
                            // File already exists – do not call onUploadError, let caller handle
                            resolve({ fileExists: true, response });
                        } else {
                            this.options.onUploadComplete(file, response);
                            resolve(response);
                        }
                    } catch (e) {
                        const error = new Error('Error parsing response');
                        this.options.onUploadError(file, error);
                        reject(error);
                    }
                } else {
                    const error = new Error(`Upload failed: ${xhr.status}`);
                    this.options.onUploadError(file, error);
                    reject(error);
                }
            });

            xhr.addEventListener('error', () => {
                const error = new Error('Network error during upload');
                this.options.onUploadError(file, error);
                reject(error);
            });

            xhr.addEventListener('abort', () => {
                const error = new Error('Upload aborted');
                this.options.onUploadError(file, error);
                reject(error);
            });

            xhr.open('POST', this.options.uploadUrl, true);
            xhr.setRequestHeader('X-XSRF-TOKEN', this.getCsrf());
            xhr.send(formData);

            this.activeUploads.push({ file, xhr });
        });
    }

    /**
     * Re-upload a list of conflict files with OVERRIDE option.
     * Calls onAllUploadsComplete when all done.
     */
    reuploadConflictsWithOverride(conflictEntries) {
        if (!conflictEntries || conflictEntries.length === 0) {
            this.options.onAllUploadsComplete();
            return;
        }

        let remaining = conflictEntries.length;

        conflictEntries.forEach(({ file, dir }) => {
            this.options.onUploadStart(file, dir);

            if (window.EsupPresignedUrls) {
                window.EsupPresignedUrls.uploadFile(dir, file, {
                    uploadOption: 'OVERRIDE',
                    onProgress: (percentComplete, loaded, total) => {
                        this.options.onUploadProgress(file, percentComplete, loaded, total);
                    },
                    onSuccess: (result) => {
                        this.options.onUploadComplete(file, result);
                        if (--remaining === 0) this.options.onAllUploadsComplete();
                    },
                    onError: (error) => {
                        this.options.onUploadError(file, error);
                        if (--remaining === 0) this.options.onAllUploadsComplete();
                    }
                    // No onFileExists here: we sent OVERRIDE so the server won't block
                });
            } else {
                this.uploadFileClassic(file, dir, 'OVERRIDE')
                    .then(result => {
                        this.options.onUploadComplete(file, result || {});
                        if (--remaining === 0) this.options.onAllUploadsComplete();
                    })
                    .catch((error) => {
                        this.options.onUploadError(file, error);
                        if (--remaining === 0) this.options.onAllUploadsComplete();
                    });
            }
        });
    }

    /**
     * Annule tous les uploads en cours
     */
    cancelAll() {
        this.activeUploads.forEach(upload => {
            if (upload.xhr) {
                upload.xhr.abort();
            }
        });
        this.activeUploads = [];
        this.uploadQueue = [];
        this.conflictFiles = [];
        this.isUploading = false;
        console.log('All uploads cancelled');
    }

    /**
     * Annule l'upload d'un fichier spécifique
     */
    cancelUpload(file) {
        const upload = this.activeUploads.find(u => u.file === file);
        if (upload && upload.xhr) {
            upload.xhr.abort();
            this.activeUploads = this.activeUploads.filter(u => u !== upload);
        }
    }

    getCsrf() {
        return document.querySelector('meta[name="_csrf"]').content;
    }
}

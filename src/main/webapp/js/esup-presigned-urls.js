/**
 * ESUP File Manager - Presigned URLs Support
 *
 * JavaScript module to handle downloads and uploads via S3 presigned URLs
 * with automatic fallback to classic methods.
 *
 * @version 2.0.0 (Vanilla JS)
 * @author ESUP-Portail
 */

(function() {
    'use strict';

    /**
     * Global configuration
     */
    const config = {
        // Enable debug logs
        debug: false,

        // Support checks cache (per drive)
        supportCache: {},

        // Cache duration in milliseconds (5 minutes)
        cacheDuration: 5 * 60 * 1000,

        // API endpoints
        endpoints: {
            supportsPresignedUrls: '/supportsPresignedUrls',
            getPresignedDownloadUrl: '/getPresignedDownloadUrl',
            getPresignedUploadUrl: '/getPresignedUploadUrl',
            downloadFile: '/downloadFile',
            uploadFile: '/uploadFile'
        }
    };

    /**
     * Utility logger
     */
    const logger = {
        log: function() {
            if (config.debug && console && console.log) {
                console.log('[PresignedURLs]', ...arguments);
            }
        },
        info: function() {
            if (console && console.info) {
                console.info('[PresignedURLs]', ...arguments);
            }
        },
        warn: function() {
            if (console && console.warn) {
                console.warn('[PresignedURLs]', ...arguments);
            }
        },
        error: function() {
            if (console && console.error) {
                console.error('[PresignedURLs]', ...arguments);
            }
        }
    };

    /**
     * Checks if presigned URLs are supported for a given path
     * @param {string} dir - The encoded file/directory path
     * @param {Function} successCallback - Callback called with true/false
     * @param {Function} errorCallback - Callback called on error (optional)
     */
    function supportsPresignedUrls(dir, successCallback, errorCallback) {
        // Check cache
        const cacheKey = dir;
        const cached = config.supportCache[cacheKey];
        if (cached && (Date.now() - cached.timestamp) < config.cacheDuration) {
            logger.log('Support check (cached):', dir, '->', cached.supported);
            if (successCallback) {
                successCallback(cached.supported);
            }
            return;
        }

        logger.log('Checking presigned URL support for:', dir);

        // Prepare data
        const formData = new URLSearchParams();
        formData.append('dir', dir);

        fetch(config.endpoints.supportsPresignedUrls, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            const supported = data.supported === true;

            // Cache result
            config.supportCache[cacheKey] = {
                supported: supported,
                timestamp: Date.now()
            };

            logger.log('Presigned URLs supported:', supported, 'for', dir);
            if (successCallback) {
                successCallback(supported);
            }
        })
        .catch(error => {
            logger.warn('Error checking presigned URL support:', error);
            // On error, consider not supported
            if (successCallback) {
                successCallback(false);
            } else if (errorCallback) {
                errorCallback(error);
            }
        });
    }

    /**
     * Downloads a file via presigned URL or classic method
     * @param {string} dir - The encoded file path
     * @param {Object} options - Download options (optional)
     */
    function downloadFile(dir, options) {
        options = options || {};

        logger.log('Starting download for:', dir);

        supportsPresignedUrls(dir, function(supported) {
            if (supported) {
                downloadWithPresignedUrl(dir, options);
            } else {
                downloadClassic(dir, options);
            }
        }, function(error) {
            logger.error('Download error:', error);
            // Fallback to classic method
            downloadClassic(dir, options);
        });
    }

    /**
     * Downloads a file via presigned URL
     * @private
     */
    function downloadWithPresignedUrl(dir, options) {
        logger.info('Downloading with presigned URL:', dir);

        const formData = new URLSearchParams();
        formData.append('dir', dir);

        fetch(config.endpoints.getPresignedDownloadUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.url) {
                logger.info('Got presigned URL, expires in', data.expiresIn, 'seconds');

                // Start download via presigned URL
                if (options.iframe) {
                    // Download via iframe (to avoid leaving the page)
                    const iframe = document.createElement('iframe');
                    iframe.src = data.url;
                    iframe.style.display = 'none';
                    document.body.appendChild(iframe);

                    setTimeout(() => {
                        iframe.remove();
                    }, 10000); // Clean up after 10 seconds
                } else {
                    // Direct download
                    window.location.href = data.url;
                }

                if (options.onSuccess) {
                    options.onSuccess({
                        method: 'presigned',
                        url: data.url,
                        filename: data.filename
                    });
                }
            } else {
                logger.warn('Failed to get presigned URL:', data.error);
                // Fallback to classic method
                downloadClassic(dir, options);
            }
        })
        .catch(error => {
            logger.error('Error getting presigned URL:', error);
            // Fallback to classic method
            downloadClassic(dir, options);
        });
    }

    /**
     * Downloads a file via classic method
     * @private
     */
    function downloadClassic(dir, options) {
        logger.info('Downloading with classic method:', dir);

        const url = config.endpoints.downloadFile + '?dir=' + encodeURIComponent(dir);

        if (options.iframe) {
            const iframe = document.createElement('iframe');
            iframe.src = url;
            iframe.style.display = 'none';
            document.body.appendChild(iframe);

            setTimeout(() => {
                iframe.remove();
            }, 10000);
        } else {
            window.location.href = url;
        }

        if (options.onSuccess) {
            options.onSuccess({
                method: 'classic',
                url: url
            });
        }
    }

    /**
     * Uploads a file via presigned URL or classic method
     * @param {string} dir - The encoded target directory path
     * @param {File} file - The File object to upload
     * @param {Object} options - Upload options
     */
    function uploadFile(dir, file, options) {
        options = options || {};

        logger.log('Starting upload for:', file.name, 'to', dir);

        supportsPresignedUrls(dir, function(supported) {
            if (supported) {
                uploadWithPresignedUrl(dir, file, options);
            } else {
                uploadClassic(dir, file, options);
            }
        }, function(error) {
            logger.error('Upload error:', error);
            // Fallback to classic method
            uploadClassic(dir, file, options);
        });
    }

    /**
     * Uploads a file via presigned URL
     * @private
     */
    function uploadWithPresignedUrl(dir, file, options) {
        logger.info('Uploading with presigned URL:', file.name);

        // Step 1: Get presigned URL
        const formData = new URLSearchParams();
        formData.append('dir', dir);
        formData.append('filename', file.name);

        fetch(config.endpoints.getPresignedUploadUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData.toString()
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(response => {
            if (response.success && response.url) {
                logger.info('Got presigned upload URL, expires in', response.expiresIn, 'seconds');

                // Step 2: Direct upload to S3
                uploadToS3(response.url, file, {
                    onProgress: options.onProgress,
                    onXhrCreated: options.onXhrCreated,
                    onSuccess: function(result) {
                        result.method = 'presigned';
                        result.filename = file.name;
                        if (options.onSuccess) {
                            options.onSuccess(result);
                        }
                    },
                    onError: function(error) {
                        logger.warn('S3 upload failed, falling back to classic:', error);
                        // Fallback to classic method
                        uploadClassic(dir, file, options);
                    }
                });
            } else {
                logger.warn('Failed to get presigned upload URL:', response.error);
                // Fallback to classic method
                uploadClassic(dir, file, options);
            }
        })
        .catch(error => {
            logger.error('Error getting presigned upload URL:', error);
            // Fallback to classic method
            uploadClassic(dir, file, options);
        });
    }

    /**
     * Uploads a file directly to S3 via PUT
     * @private
     */
    function uploadToS3(presignedUrl, file, options) {
        logger.log('Uploading to S3:', file.name);

        const xhr = new XMLHttpRequest();

        // Progress handling
        if (options.onProgress && xhr.upload) {
            xhr.upload.addEventListener('progress', function(e) {
                if (e.lengthComputable) {
                    const percentComplete = (e.loaded / e.total) * 100;
                    options.onProgress(percentComplete, e.loaded, e.total);
                }
            });
        }

        xhr.addEventListener('load', function() {
            if (xhr.status >= 200 && xhr.status < 300) {
                logger.info('S3 upload successful');
                if (options.onSuccess) {
                    options.onSuccess({
                        success: true,
                        status: xhr.status
                    });
                }
            } else {
                logger.error('S3 upload failed with status:', xhr.status);
                if (options.onError) {
                    options.onError(new Error('S3 upload failed: ' + xhr.status));
                }
            }
        });

        xhr.addEventListener('error', function() {
            logger.error('S3 upload network error');
            if (options.onError) {
                options.onError(new Error('Network error during S3 upload'));
            }
        });

        xhr.addEventListener('abort', function() {
            logger.warn('S3 upload aborted');
            if (options.onError) {
                options.onError(new Error('Upload aborted'));
            }
        });

        // Open PUT connection to presigned URL
        xhr.open('PUT', presignedUrl, true);

        // Set Content-Type
        const contentType = file.type || 'application/octet-stream';
        xhr.setRequestHeader('Content-Type', contentType);

        // Send file
        xhr.send(file);

        // Save xhr to allow cancellation
        if (options.onXhrCreated) {
            options.onXhrCreated(xhr);
        }
    }

    /**
     * Uploads a file via classic method
     * @private
     */
    function uploadClassic(dir, file, options) {
        logger.info('Uploading with classic method:', file.name);

        const formData = new FormData();
        formData.append('dir', dir);
        formData.append('qqfile', file);

        if (options.uploadOption) {
            formData.append('uploadOption', options.uploadOption);
        }

        const xhr = new XMLHttpRequest();

        // Progress handling
        if (options.onProgress && xhr.upload) {
            xhr.upload.addEventListener('progress', function(e) {
                if (e.lengthComputable) {
                    const percentComplete = (e.loaded / e.total) * 100;
                    options.onProgress(percentComplete, e.loaded, e.total);
                }
            });
        }

        xhr.addEventListener('load', function() {
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const response = JSON.parse(xhr.responseText);
                    logger.info('Classic upload successful');
                    if (options.onSuccess) {
                        options.onSuccess({
                            method: 'classic',
                            success: true,
                            response: response
                        });
                    }
                } catch (e) {
                    logger.error('Error parsing upload response:', e);
                    if (options.onError) {
                        options.onError(new Error('Error parsing response'));
                    }
                }
            } else {
                logger.error('Classic upload failed with status:', xhr.status);
                if (options.onError) {
                    options.onError(new Error('Upload failed: ' + xhr.status));
                }
            }
        });

        xhr.addEventListener('error', function() {
            logger.error('Classic upload network error');
            if (options.onError) {
                options.onError(new Error('Network error during upload'));
            }
        });

        xhr.open('POST', config.endpoints.uploadFile, true);
        xhr.send(formData);

        // Save xhr to allow cancellation
        if (options.onXhrCreated) {
            options.onXhrCreated(xhr);
        }
    }

    /**
     * Configures the module
     * @param {Object} options - Configuration options
     */
    function configure(options) {
        if (options) {
            if (typeof options.debug !== 'undefined') {
                config.debug = options.debug;
            }
            if (options.endpoints) {
                Object.assign(config.endpoints, options.endpoints);
            }
            if (typeof options.cacheDuration !== 'undefined') {
                config.cacheDuration = options.cacheDuration;
            }
        }
    }

    /**
     * Clears the support cache
     */
    function clearCache() {
        config.supportCache = {};
        logger.log('Cache cleared');
    }

    // Export public API
    window.EsupPresignedUrls = {
        // Main methods
        downloadFile: downloadFile,
        uploadFile: uploadFile,
        supportsPresignedUrls: supportsPresignedUrls,

        // Configuration
        configure: configure,
        clearCache: clearCache,

        // Utilities
        version: '2.0.0',
        logger: logger
    };

    // Initialization
    logger.log('ESUP Presigned URLs module loaded, version', window.EsupPresignedUrls.version);

})();


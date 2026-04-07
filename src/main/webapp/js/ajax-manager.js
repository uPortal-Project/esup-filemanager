/**
 * AJAX Manager - Fetch API wrapper
 * @module ajax-manager
 */

export class AjaxManager {

    /**
     * Executes an AJAX request
     * @param {Object} options - Request options
     * @returns {Promise}
     */
    static async request(options) {
        const {
            url: baseUrl,
            method = 'GET',
            data = null,
            headers = {}
        } = options;

        let requestUrl = baseUrl;

        const config = {
            method: method.toUpperCase(),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                ...headers
            }
        };

        // Data conversion for POST
        if (data && (method.toUpperCase() === 'POST' || method.toUpperCase() === 'PUT')) {
            if (typeof data === 'string') {
                config.body = data;
            } else if (data instanceof FormData) {
                delete config.headers['Content-Type']; // Let browser set it with boundary
                config.body = data;
            } else {
                // Convert object to URLSearchParams
                const params = new URLSearchParams();
                Object.keys(data).forEach(key => {
                    params.append(key, data[key]);
                });
                config.body = params.toString();
            }
        } else if (data && method.toUpperCase() === 'GET') {
            // Add parameters to URL for GET
            let paramsInput = data;
            if (typeof paramsInput === 'string' && paramsInput.startsWith('?')) {
                paramsInput = paramsInput.slice(1);
            }
            const params = new URLSearchParams(paramsInput);
            const separator = baseUrl.includes('?') ? '&' : '?';
            requestUrl = baseUrl + separator + params.toString();
        }

        try {
            const response = await fetch(requestUrl, config);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const contentType = response.headers.get('content-type');

            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else if (contentType && contentType.includes('text/html')) {
                return await response.text();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error('AJAX request failed:', error);
            throw error;
        }
    }

    /**
     * Shortcut for GET
     */
    static get(url, data = null) {
        return this.request({ url, method: 'GET', data });
    }

    /**
     * Shortcut for POST
     */
    static post(url, data = null) {
        return this.request({ url, method: 'POST', data });
    }

    /**
     * Loads an HTML fragment
     */
    static async loadHTML(url, data = null) {
        const html = await this.request({
            url,
            method: data ? 'POST' : 'GET',
            data,
            headers: {
                'Accept': 'text/html'
            }
        });
        return html;
    }
}


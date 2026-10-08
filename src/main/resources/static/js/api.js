// api.js
// Thin wrapper around fetch() for the /api/containers REST endpoints
// built in ContainerController. Every frontend page uses these functions
// instead of calling fetch() directly, so error handling stays consistent.

const API_BASE = '/api/containers';

async function handleResponse(response) {
    if (response.status === 204) {
        return null; // No Content (delete)
    }
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        const message = (data && data.message) || `Request failed (${response.status})`;
        const error = new Error(message);
        if (data && data.validationErrors) {
            error.validationErrors = data.validationErrors;
        }
        throw error;
    }
    return data;
}

const ContainerApi = {
    getAll: () =>
        fetch(API_BASE).then(handleResponse),

    getById: (id) =>
        fetch(`${API_BASE}/${id}`).then(handleResponse),

    create: (container) =>
        fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(container)
        }).then(handleResponse),

    update: (id, container) =>
        fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(container)
        }).then(handleResponse),

    remove: (id) =>
        fetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        }).then(handleResponse),

    searchByTruckNumber: (query) =>
        fetch(`${API_BASE}/search?truckNumber=${encodeURIComponent(query)}`).then(handleResponse),

    searchByVendorCode: (query) =>
        fetch(`${API_BASE}/search?vendorCode=${encodeURIComponent(query)}`).then(handleResponse),

    exportUrl: () => `${API_BASE}/export`,

    importExcel: (file) => {
        const formData = new FormData();
        formData.append('file', file);
        return fetch(`${API_BASE}/import`, {
            method: 'POST',
            body: formData
        }).then(handleResponse);
    }
};

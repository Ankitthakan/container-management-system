// container-list.js

let pendingDeleteId = null;
let deleteModalInstance = null;
let importResultModalInstance = null;
let searchDebounceTimer = null;

document.addEventListener('DOMContentLoaded', () => {
    loadContainers();
    deleteModalInstance = new bootstrap.Modal(document.getElementById('deleteModal'));
    importResultModalInstance = new bootstrap.Modal(document.getElementById('importResultModal'));
    document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);

    showSuccessFromRedirect();
    wireImportControls();

    const truckInput = document.getElementById('searchTruckNumber');
    const vendorInput = document.getElementById('searchVendorCode');
    const clearBtn = document.getElementById('clearSearchBtn');

    truckInput.addEventListener('input', () => {
        vendorInput.value = '';
        debounceSearch();
    });

    vendorInput.addEventListener('input', () => {
        truckInput.value = '';
        debounceSearch();
    });

    clearBtn.addEventListener('click', () => {
        truckInput.value = '';
        vendorInput.value = '';
        loadContainers();
    });
});

function wireImportControls() {
    const importBtn = document.getElementById('importBtn');
    const fileInput = document.getElementById('importFileInput');

    // Clicking the visible button opens the hidden native file picker.
    importBtn.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', async () => {
        const file = fileInput.files[0];
        if (!file) return;

        importBtn.disabled = true;
        importBtn.textContent = 'Importing...';

        try {
            const result = await ContainerApi.importExcel(file);
            showImportResult(result);
            await loadContainers();
        } catch (err) {
            showAlert('actionAlert', `Import failed: ${err.message}`, 'danger');
        } finally {
            importBtn.disabled = false;
            importBtn.innerHTML = `
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="me-1" style="margin-top:-2px;">
                    <path d="M12 15V3M12 3L8 7M12 3L16 7" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M4 17V19C4 20.1 4.9 21 6 21H18C19.1 21 20 20.1 20 19V17" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>
                Import Excel
            `;
            fileInput.value = ''; // allow re-selecting the same file later
        }
    });
}

function showImportResult(result) {
    document.getElementById('importSuccessCount').textContent = result.importedCount;
    document.getElementById('importFailCount').textContent = result.failedCount;

    const detailsBox = document.getElementById('importFailureDetails');
    const list = document.getElementById('importFailureList');

    if (result.failedCount > 0 && result.failureReasons && result.failureReasons.length > 0) {
        list.innerHTML = result.failureReasons.map((reason) => `<li>${escapeHtml(reason)}</li>`).join('');
        detailsBox.classList.remove('d-none');
    } else {
        list.innerHTML = '';
        detailsBox.classList.add('d-none');
    }

    importResultModalInstance.show();
}

// Reads ?success=added / ?success=updated set by container-form.js after a
// successful save, shows a confirmation banner, then cleans the URL so a
// page refresh doesn't re-show the message.
function showSuccessFromRedirect() {
    const params = new URLSearchParams(window.location.search);
    const success = params.get('success');
    if (!success) return;

    const messages = {
        added: 'Container added successfully.',
        updated: 'Container updated successfully.'
    };
    showAlert('actionAlert', messages[success] || 'Saved successfully.', 'success');

    window.history.replaceState({}, '', window.location.pathname);
}

function debounceSearch() {
    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(runSearch, 350);
}

async function runSearch() {
    const truckQuery = document.getElementById('searchTruckNumber').value.trim();
    const vendorQuery = document.getElementById('searchVendorCode').value.trim();

    try {
        let results;
        if (truckQuery) {
            results = await ContainerApi.searchByTruckNumber(truckQuery);
        } else if (vendorQuery) {
            results = await ContainerApi.searchByVendorCode(vendorQuery);
        } else {
            results = await ContainerApi.getAll();
        }
        renderTable(results, /* isSearchResult */ Boolean(truckQuery || vendorQuery));
    } catch (err) {
        showAlert('loadError', `Search failed: ${err.message}`, 'danger');
    }
}

async function loadContainers() {
    try {
        const containers = await ContainerApi.getAll();
        renderTable(containers);
    } catch (err) {
        showAlert('loadError', `Could not load containers: ${err.message}`, 'danger');
    }
}

function renderTable(containers, isSearchResult) {
    const tbody = document.getElementById('containerTableBody');
    const emptyState = document.getElementById('listEmptyState');
    const recordCount = document.getElementById('recordCount');

    recordCount.textContent = `${containers.length} record${containers.length === 1 ? '' : 's'}`;

    if (containers.length === 0) {
        tbody.innerHTML = '';
        document.querySelector('.table-responsive').classList.add('d-none');
        emptyState.classList.remove('d-none');
        emptyState.querySelector('div').textContent = isSearchResult
            ? 'No containers match your search.'
            : 'No containers recorded yet.';
        return;
    }

    document.querySelector('.table-responsive').classList.remove('d-none');
    emptyState.classList.add('d-none');

    const sorted = [...containers].sort((a, b) => b.id - a.id);

    tbody.innerHTML = sorted.map((c) => `
        <tr>
            <td class="fw-semibold">${escapeHtml(c.truckNumber)}</td>
            <td>${escapeHtml(c.vendorCode)}</td>
            <td>${c.length}</td>
            <td>${c.width}</td>
            <td>${c.height}</td>
            <td>${volumeBadge(c.volume)}</td>
            <td class="text-end">
                <a href="/containers/edit/${c.id}" class="action-icon-btn me-1" title="Edit">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M12 20H21" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                        <path d="M16.5 3.5C17.33 2.67 18.67 2.67 19.5 3.5C20.33 4.33 20.33 5.67 19.5 6.5L7 19L3 20L4 16L16.5 3.5Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
                    </svg>
                </a>
                <button type="button" class="action-icon-btn danger" title="Delete"
                        onclick="openDeleteModal(${c.id}, '${escapeHtml(c.truckNumber)}')">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M4 7H20" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
                        <path d="M9 7V4C9 3.4 9.4 3 10 3H14C14.6 3 15 3.4 15 4V7" stroke="currentColor" stroke-width="1.6"/>
                        <path d="M6 7L7 20C7 20.6 7.4 21 8 21H16C16.6 21 17 20.6 17 20L18 7" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/>
                    </svg>
                </button>
            </td>
        </tr>
    `).join('');
}

function openDeleteModal(id, truckNumber) {
    pendingDeleteId = id;
    document.getElementById('deleteTruckNumber').textContent = truckNumber;
    deleteModalInstance.show();
}

async function confirmDelete() {
    if (pendingDeleteId === null) return;

    const btn = document.getElementById('confirmDeleteBtn');
    btn.disabled = true;
    btn.textContent = 'Deleting...';

    try {
        await ContainerApi.remove(pendingDeleteId);
        deleteModalInstance.hide();
        showAlert('actionAlert', 'Container deleted successfully.', 'success');
        await loadContainers();
    } catch (err) {
        deleteModalInstance.hide();
        showAlert('actionAlert', `Could not delete container: ${err.message}`, 'danger');
    } finally {
        pendingDeleteId = null;
        btn.disabled = false;
        btn.textContent = 'Delete';
    }
}

function volumeBadge(volume) {
    return `
        <span class="volume-badge">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3 7L12 3L21 7V17L12 21L3 17V7Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                <path d="M3 7L12 11L21 7" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                <path d="M12 11V21" stroke="currentColor" stroke-width="1.8"/>
            </svg>
            ${Number(volume).toFixed(2)} m³
        </span>
    `;
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

function showAlert(elementId, message, type) {
    const box = document.getElementById(elementId);
    box.textContent = message;
    box.className = `alert alert-${type}`;
    setTimeout(() => box.classList.add('d-none'), 4000);
}

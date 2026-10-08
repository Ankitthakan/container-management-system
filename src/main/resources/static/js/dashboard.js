// dashboard.js
// Loads all containers once and derives every dashboard stat client-side.
// No dedicated /api/dashboard endpoint exists - this reuses GET /api/containers.

document.addEventListener('DOMContentLoaded', loadDashboard);

async function loadDashboard() {
    try {
        const containers = await ContainerApi.getAll();
        renderStats(containers);
        renderLatest(containers);
    } catch (err) {
        showError(err.message);
    }
}

function renderStats(containers) {
    const total = containers.length;
    document.getElementById('statTotal').textContent = total;

    if (total === 0) {
        document.getElementById('statAvgVolume').textContent = '0.00 m³';
        document.getElementById('statLatest').textContent = '—';
        return;
    }

    const totalVolume = containers.reduce((sum, c) => sum + Number(c.volume), 0);
    const avgVolume = totalVolume / total;
    document.getElementById('statAvgVolume').textContent = `${avgVolume.toFixed(2)} m³`;

    const mostRecent = [...containers].sort((a, b) => b.id - a.id)[0];
    document.getElementById('statLatest').textContent = mostRecent.truckNumber;
}

function renderLatest(containers) {
    const tbody = document.getElementById('latestContainersBody');
    const emptyState = document.getElementById('dashboardEmptyState');

    if (containers.length === 0) {
        tbody.innerHTML = '';
        document.querySelector('.table-responsive').classList.add('d-none');
        emptyState.classList.remove('d-none');
        return;
    }

    const latestFive = [...containers]
        .sort((a, b) => b.id - a.id)
        .slice(0, 5);

    tbody.innerHTML = latestFive.map((c) => `
        <tr>
            <td class="fw-semibold">${escapeHtml(c.truckNumber)}</td>
            <td>${escapeHtml(c.vendorCode)}</td>
            <td class="text-muted">${c.length} × ${c.width} × ${c.height} m</td>
            <td>${volumeBadge(c.volume)}</td>
        </tr>
    `).join('');
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

function showError(message) {
    const box = document.getElementById('loadError');
    box.textContent = `Could not load dashboard data: ${message}`;
    box.classList.remove('d-none');
}

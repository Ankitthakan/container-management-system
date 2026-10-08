// layout.js
// Handles opening/closing the sidebar on small screens (below 900px, see
// style.css). Included on every page that shows the sidebar fragment.

document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('appSidebar');
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (!sidebar || !toggleBtn || !backdrop) return;

    function openSidebar() {
        sidebar.classList.add('show');
        backdrop.classList.add('show');
    }

    function closeSidebar() {
        sidebar.classList.remove('show');
        backdrop.classList.remove('show');
    }

    toggleBtn.addEventListener('click', () => {
        sidebar.classList.contains('show') ? closeSidebar() : openSidebar();
    });

    backdrop.addEventListener('click', closeSidebar);

    // Close automatically when a nav link is tapped (mobile UX expectation)
    sidebar.querySelectorAll('.nav-link').forEach((link) => {
        link.addEventListener('click', closeSidebar);
    });
});

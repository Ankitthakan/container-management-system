// login.js
// NOTE: There is no backend authentication yet (no Spring Security, no user
// table). This only validates that the fields are filled in, then redirects
// to the dashboard. Replace the redirect below with a real fetch() call to
// an auth endpoint once one exists.

document.getElementById('loginForm').addEventListener('submit', function (e) {
    e.preventDefault();

    const username = document.getElementById('username');
    const password = document.getElementById('password');
    const alertBox = document.getElementById('loginAlert');

    let valid = true;

    [username, password].forEach((field) => {
        if (!field.value.trim()) {
            field.classList.add('is-invalid');
            valid = false;
        } else {
            field.classList.remove('is-invalid');
        }
    });

    if (!valid) {
        alertBox.textContent = 'Please fill in both fields.';
        alertBox.classList.remove('d-none');
        return;
    }

    alertBox.classList.add('d-none');
    window.location.href = '/dashboard';
});

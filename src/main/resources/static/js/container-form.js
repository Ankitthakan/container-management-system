// container-form.js
// Shared by add-container.html and edit-container.html.
// Mode ('add' | 'edit') is read from <body data-mode="...">.
// The volume shown here is a client-side preview for the user's convenience
// only - the value actually saved is always recalculated by the backend
// in ContainerServiceImpl, never taken from this form.

const mode = document.body.dataset.mode;
const containerId = mode === 'edit' ? getIdFromUrl() : null;

const form = document.getElementById('containerForm');
const dimInputs = document.querySelectorAll('.dim-input');
const volumeField = document.getElementById('volume');

document.addEventListener('DOMContentLoaded', init);

async function init() {
    dimInputs.forEach((input) => input.addEventListener('input', updateVolumePreview));
    form.addEventListener('submit', handleSubmit);

    if (mode === 'edit') {
        await loadExistingContainer();
    } else {
        updateVolumePreview();
    }
}

function getIdFromUrl() {
    const parts = window.location.pathname.split('/');
    return parts[parts.length - 1];
}

async function loadExistingContainer() {
    try {
        const container = await ContainerApi.getById(containerId);
        document.getElementById('truckNumber').value = container.truckNumber;
        document.getElementById('vendorCode').value = container.vendorCode;
        document.getElementById('length').value = container.length;
        document.getElementById('width').value = container.width;
        document.getElementById('height').value = container.height;

        updateVolumePreview();

        document.getElementById('loadingState').classList.add('d-none');
        form.classList.remove('d-none');
    } catch (err) {
        showAlert(`Could not load this container: ${err.message}`, 'danger');
        document.getElementById('loadingState').textContent = 'Failed to load container.';
    }
}

function updateVolumePreview() {
    const length = parseFloat(document.getElementById('length').value) || 0;
    const width = parseFloat(document.getElementById('width').value) || 0;
    const height = parseFloat(document.getElementById('height').value) || 0;
    const volume = length * width * height;

    // volume field is readonly - this is a client-side preview only.
    // The authoritative value is always calculated by ContainerServiceImpl
    // on the backend when the form is submitted.
    volumeField.value = `${volume.toFixed(2)} m³`;
}

function validateForm() {
    let valid = true;

    const truckNumber = document.getElementById('truckNumber');
    const vendorCode = document.getElementById('vendorCode');

    [truckNumber, vendorCode].forEach((field) => {
        if (!field.value.trim()) {
            field.classList.add('is-invalid');
            valid = false;
        } else {
            field.classList.remove('is-invalid');
        }
    });

    dimInputs.forEach((field) => {
        if (!field.value || parseFloat(field.value) <= 0) {
            field.classList.add('is-invalid');
            valid = false;
        } else {
            field.classList.remove('is-invalid');
        }
    });

    return valid;
}

async function handleSubmit(e) {
    e.preventDefault();

    if (!validateForm()) {
        return;
    }

    const payload = {
        truckNumber: document.getElementById('truckNumber').value.trim(),
        vendorCode: document.getElementById('vendorCode').value.trim(),
        length: parseFloat(document.getElementById('length').value),
        width: parseFloat(document.getElementById('width').value),
        height: parseFloat(document.getElementById('height').value)
        // volume intentionally omitted - the backend always calculates it
    };

    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = mode === 'edit' ? 'Saving...' : 'Saving...';

    try {
        if (mode === 'edit') {
            await ContainerApi.update(containerId, payload);
            window.location.href = '/containers?success=updated';
        } else {
            await ContainerApi.create(payload);
            window.location.href = '/containers?success=added';
        }
    } catch (err) {
        if (err.validationErrors) {
            applyFieldErrors(err.validationErrors);
            showAlert('Please fix the highlighted fields below.', 'danger');
        } else {
            showAlert(`Could not save container: ${err.message}`, 'danger');
        }
        submitBtn.disabled = false;
        submitBtn.textContent = mode === 'edit' ? 'Save Changes' : 'Save Container';
    }
}

// Highlights fields the backend rejected and shows its exact message,
// even though the form already validates the same rules client-side.
function applyFieldErrors(validationErrors) {
    Object.entries(validationErrors).forEach(([field, message]) => {
        const input = document.getElementById(field);
        if (!input) return;
        input.classList.add('is-invalid');
        const feedback = input.parentElement.querySelector('.invalid-feedback')
            || input.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.textContent = message;
        }
    });
}

function showAlert(message, type) {
    const box = document.getElementById('formAlert');
    box.textContent = message;
    box.className = `alert alert-${type}`;
}

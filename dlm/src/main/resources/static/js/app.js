
(function () {
    const html = document.documentElement;

    const THEME_KEY = 'dm-theme';
    function applyTheme(t) { html.setAttribute('data-theme', t); }
    const saved = localStorage.getItem(THEME_KEY);
    if (saved === 'light' || saved === 'dark') applyTheme(saved);

    const toggleBtn = document.getElementById('themeToggle');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const cur = html.getAttribute('data-theme');
            const next = cur === 'dark' ? 'light' : 'dark';
            applyTheme(next);
            localStorage.setItem(THEME_KEY, next);
            toggleBtn.setAttribute('aria-pressed', String(next === 'dark'));
        });
    }

    document.querySelectorAll('[data-toggle-password]').forEach(btn => {
        const sel = btn.getAttribute('data-toggle-password');
        const input = document.querySelector(sel);
        if (!input) return;
        btn.addEventListener('click', () => {
            const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
            input.setAttribute('type', type);
            btn.setAttribute('aria-label', type === 'password' ? 'Показати пароль' : 'Приховати пароль');
        });
    });

    document.querySelectorAll('form[novalidate]').forEach(form => {
        form.addEventListener('submit', (e) => {
            if (!form.checkValidity()) {
                e.preventDefault();
                Array.from(form.elements).forEach(el => {
                    if ('reportValidity' in el) el.reportValidity();
                });
            }
        });
    });

    const modal = document.getElementById('confirmModal');
    const modalText = document.getElementById('confirmText');
    const confirmOk = document.getElementById('confirmOk');
    let pendingForm = null;

    function openModal(message) {
        if (!modal) return true;
        modal.setAttribute('open', '');
        modal.removeAttribute('aria-hidden');
        if (message) modalText.textContent = message;
    }
    function closeModal() {
        if (!modal) return;
        modal.removeAttribute('open');
        modal.setAttribute('aria-hidden', 'true');
    }

    document.addEventListener('click', (e) => {
        if (e.target.matches('[data-close]')) closeModal();
        if (e.target.classList?.contains('modal__backdrop')) closeModal();
    });

    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', (e) => {
            if (!form.hasAttribute('data-confirm')) return;
            if (!modal) return;

            const msg = form.getAttribute('data-confirm') || 'Підтвердити дію?';
            e.preventDefault();
            pendingForm = form;
            openModal(msg);
        }, { capture: true });
    });

    if (confirmOk) {
        confirmOk.addEventListener('click', () => {
            if (pendingForm) {
                pendingForm.removeAttribute('data-confirm');

                pendingForm.dataset.confirmBypass = 'true';

                if (pendingForm.requestSubmit) pendingForm.requestSubmit();
                else pendingForm.submit();

                pendingForm = null;
            }
            closeModal();
        });
    }

    function updateProgressBars() {
        document.querySelectorAll('.progress__bar-fill[data-received]').forEach(el => {
            const rec = Number(el.getAttribute('data-received') || '0');
            const tot = Number(el.getAttribute('data-total') || '0');
            let pct = 0;
            if (tot > 0 && Number.isFinite(rec)) {
                pct = Math.max(0, Math.min(100, (rec / tot) * 100));
            }
            el.style.width = pct.toFixed(2) + '%';
            el.setAttribute('aria-valuenow', pct.toFixed(0));
            if (pct >= 100) el.closest('.progress')?.querySelector('[data-wait]')?.remove();
        });
    }
    updateProgressBars();
})();

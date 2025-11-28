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

    //Перемикач видимості пароля
    document.querySelectorAll('[data-toggle-password]').forEach(btn => {
        const sel = btn.getAttribute('data-toggle-password');
        const input = document.querySelector(sel);
        if (!input) return;
        btn.addEventListener('click', () => {
            const type = input.getAttribute('type') === 'password' ? 'text' : 'password';
            input.setAttribute('type', type);
            btn.setAttribute(
                'aria-label',
                type === 'password' ? 'Показати пароль' : 'Приховати пароль'
            );
        });
    });

    //Клієнтська перевірка форм
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

    //Модалка підтвердження
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
        }, {capture: true});
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

    //Прогрес-бари
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
            if (pct >= 100) {
                el.closest('.progress')?.querySelector('[data-wait]')?.remove();
            }
        });
    }

    updateProgressBars();

    //Допоміжні форматтери для статистики

    function formatSpeed(bps) {
        if (!bps || bps <= 0 || !Number.isFinite(bps)) return '—';
        const units = ['B/s', 'KB/s', 'MB/s', 'GB/s'];
        let value = bps;
        let idx = 0;
        while (value >= 1024 && idx < units.length - 1) {
            value /= 1024;
            idx++;
        }
        const decimals = idx === 0 ? 0 : 2;
        return value.toFixed(decimals) + ' ' + units[idx];
    }

    function formatTime(isoString) {
        if (!isoString) return '—';
        const d = new Date(isoString);
        if (Number.isNaN(d.getTime())) return '—';
        return d.toLocaleTimeString('uk-UA', {hour: '2-digit', minute: '2-digit'});
    }

    // ПОЛЛІНГ ПРОГРЕСУ ДЛЯ СТОРІНКИ /downloads

    const page = document.body.getAttribute('data-page');

    if (page === 'downloads') {
        const POLL_INTERVAL_MS = 1000;

        const BADGE_CLASSES = [
            'badge--running',
            'badge--paused',
            'badge--canceled',
            'badge--default'
        ];

        const ALLOW_DELETE = ['COMPLETED', 'CANCELED', 'ERROR'];

        async function pollProgress() {
            try {
                const resp = await fetch('/downloads/progress', {
                    headers: {'Accept': 'application/json'}
                });
                if (!resp.ok) return;
                const data = await resp.json(); // масив DTO

                data.forEach(item => {
                    const row = document.querySelector(`tr[data-download-id="${item.id}"]`);
                    if (!row) return;

                    const recSpan = row.querySelector('.progress__nums .js-rec');
                    const totSpan = row.querySelector('.progress__nums .js-tot');
                    if (recSpan) recSpan.textContent = item.receivedBytes;
                    if (totSpan) totSpan.textContent = item.totalBytes;

                    const bar = row.querySelector('.progress__bar-fill');
                    if (bar) {
                        bar.setAttribute('data-received', item.receivedBytes);
                        bar.setAttribute('data-total', item.totalBytes);
                    }

                    const badge = row.querySelector('.badge');
                    if (badge) {
                        badge.textContent = item.status;

                        BADGE_CLASSES.forEach(c => badge.classList.remove(c));
                        let cls = 'badge--default';
                        if (item.status === 'RUNNING') cls = 'badge--running';
                        else if (item.status === 'PAUSED') cls = 'badge--paused';
                        else if (item.status === 'CANCELED') cls = 'badge--canceled';
                        badge.classList.add(cls);
                    }

                    const deleteForm = row.querySelector('.js-delete-form');
                    if (deleteForm) {
                        const canDelete = ALLOW_DELETE.includes(item.status);
                        deleteForm.classList.toggle('is-hidden', !canDelete);
                    }

                    const currentEl = row.querySelector('.js-speed-current');
                    const maxEl = row.querySelector('.js-speed-max');
                    const startEl = row.querySelector('.js-start-time');
                    const lastStartEl = row.querySelector('.js-last-start');
                    const finishEl = row.querySelector('.js-finish-time');
                    const retriesEl = row.querySelector('.js-retries');

                    if (currentEl) currentEl.textContent = formatSpeed(item.avgSpeedBps);
                    if (maxEl) maxEl.textContent = formatSpeed(item.maxSpeedBps);
                    if (startEl) startEl.textContent = formatTime(item.createdAt);
                    if (lastStartEl) lastStartEl.textContent = formatTime(item.lastStartedAt);
                    if (finishEl) finishEl.textContent = formatTime(item.lastFinishedAt);
                    if (retriesEl) retriesEl.textContent = (item.retries ?? 0);
                });

                updateProgressBars();
            } catch (e) {
                console.error('progress poll failed', e);
            }
        }

        setInterval(pollProgress, POLL_INTERVAL_MS);
    }
})();

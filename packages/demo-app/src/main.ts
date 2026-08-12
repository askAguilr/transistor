import './style.css';
import { WrapperCore } from '@transistorjs/wrapper-core';
import { ToastPlugin } from '@transistorjs/toast-plugin';
import { AppsPlugin, type AppItem } from '@transistorjs/apps-plugin';

const core = new WrapperCore({
  onFallbackToast: (message) => {
    console.log('[Fallback Toast Notification]:', message);
  }
});

const wrapperStatusEl = document.getElementById('wrapper-status');
const appVersionEl = document.getElementById('app-version');
const enabledPluginsEl = document.getElementById('enabled-plugins');
const btnShowToast = document.getElementById('btn-show-toast');
const toastMessageInput = document.getElementById('toast-message') as HTMLInputElement;

const btnRefreshApps = document.getElementById('btn-refresh-apps');
const appSearchInput = document.getElementById('app-search-input') as HTMLInputElement;
const appsGridEl = document.getElementById('apps-grid');

let installedApps: AppItem[] = [];

if (wrapperStatusEl && appVersionEl && enabledPluginsEl) {
  const isNative = core.isNativeWrapper();
  
  wrapperStatusEl.textContent = isNative ? 'Yes' : 'No';
  wrapperStatusEl.className = `badge ${isNative ? 'badge-true' : 'badge-false'}`;
  
  appVersionEl.textContent = core.getAppVersion();
  
  const plugins = core.getEnabledPlugins();
  enabledPluginsEl.textContent = plugins.length > 0 ? plugins.join(', ') : 'None';
}

if (btnShowToast && toastMessageInput) {
  btnShowToast.addEventListener('click', () => {
    const msg = toastMessageInput.value || 'Hello!';
    ToastPlugin.show(msg, () => {
      // Custom Web Fallback: display custom HTML toast instead of native alert
      const toastEl = document.createElement('div');
      toastEl.textContent = `[Web Fallback Toast]: ${msg}`;
      toastEl.style.position = 'fixed';
      toastEl.style.bottom = '20px';
      toastEl.style.left = '50%';
      toastEl.style.transform = 'translateX(-50%)';
      toastEl.style.background = 'rgba(99, 102, 241, 0.9)';
      toastEl.style.color = '#fff';
      toastEl.style.padding = '12px 24px';
      toastEl.style.borderRadius = '8px';
      toastEl.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
      toastEl.style.zIndex = '9999';
      document.body.appendChild(toastEl);
      setTimeout(() => toastEl.remove(), 3000);
    });
  });
}

function renderApps(apps: AppItem[]) {
  if (!appsGridEl) return;

  if (apps.length === 0) {
    appsGridEl.innerHTML = `<p class="empty-state">No matching applications found.</p>`;
    return;
  }

  appsGridEl.innerHTML = '';
  apps.forEach((app) => {
    const card = document.createElement('div');
    card.className = 'app-item-card';

    const iconImg = document.createElement('img');
    iconImg.className = 'app-icon';
    iconImg.alt = app.appName;
    iconImg.src = app.icon || "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='64' height='64' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2'><rect x='4' y='4' width='16' height='16' rx='2'/></svg>";

    const nameEl = document.createElement('div');
    nameEl.className = 'app-name';
    nameEl.textContent = app.appName;

    const pkgEl = document.createElement('div');
    pkgEl.className = 'app-package';
    pkgEl.textContent = app.packageName;

    const actionsEl = document.createElement('div');
    actionsEl.className = 'app-actions';

    const btnLaunch = document.createElement('button');
    btnLaunch.className = 'btn-launch';
    btnLaunch.textContent = 'Launch';
    btnLaunch.addEventListener('click', (e) => {
      e.stopPropagation();
      AppsPlugin.launchApp(app.packageName);
    });

    actionsEl.appendChild(btnLaunch);

    if (!app.isSystemApp) {
      const btnUninstall = document.createElement('button');
      btnUninstall.className = 'btn-uninstall';
      btnUninstall.textContent = 'Uninstall';
      btnUninstall.addEventListener('click', (e) => {
        e.stopPropagation();
        AppsPlugin.uninstallApp(app.packageName);
      });
      actionsEl.appendChild(btnUninstall);
    }

    card.appendChild(iconImg);
    card.appendChild(nameEl);
    card.appendChild(pkgEl);
    card.appendChild(actionsEl);

    appsGridEl.appendChild(card);
  });
}

function loadAndDisplayApps() {
  installedApps = AppsPlugin.getApps();
  filterAndRenderApps();
}

function filterAndRenderApps() {
  const query = appSearchInput?.value.trim().toLowerCase() || '';
  if (!query) {
    renderApps(installedApps);
  } else {
    const filtered = installedApps.filter(
      (a) => a.appName.toLowerCase().includes(query) || a.packageName.toLowerCase().includes(query)
    );
    renderApps(filtered);
  }
}

if (btnRefreshApps) {
  btnRefreshApps.addEventListener('click', () => {
    loadAndDisplayApps();
  });
}

if (appSearchInput) {
  appSearchInput.addEventListener('input', () => {
    filterAndRenderApps();
  });
}

// Automatically load apps on startup
loadAndDisplayApps();

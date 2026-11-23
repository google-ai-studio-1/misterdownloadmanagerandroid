import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const PORT = Number(process.env.DEFAULT_APP_PORT || 3000);

const APK_CANDIDATES = [
  path.join(ROOT, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'),
  path.join(ROOT, '.build-outputs', 'app-debug.apk'),
];

function findApk() {
  for (const candidate of APK_CANDIDATES) {
    try {
      const stat = fs.statSync(candidate);
      if (stat.isFile() && stat.size > 0) {
        return { path: candidate, size: stat.size, mtime: stat.mtime.toISOString() };
      }
    } catch {
      // try next
    }
  }
  return null;
}

const HTML = `<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>misterdownloadmanager — Android Port</title>
  <style>
    :root {
      color-scheme: light dark;
      --primary: #2A78D6;
      --primary-dark: #1E5FA8;
      --green: #2E9E5B;
      --red: #D64545;
      --amber: #E8A13A;
      --bg: #F6F8FC;
      --surface: #FFFFFF;
      --surface-variant: #EDF2FA;
      --text: #141B24;
      --text-muted: #5A6577;
      --border: #DCE3EE;
      --nav-bg: #FFFFFF;
    }
    @media (prefers-color-scheme: dark) {
      :root {
        --primary: #7FB3EC;
        --primary-dark: #9CC6F0;
        --green: #6CC48F;
        --red: #F08080;
        --bg: #111418;
        --surface: #1A1F26;
        --surface-variant: #232A35;
        --text: #E9EEF6;
        --text-muted: #9AA6B8;
        --border: #2C3542;
        --nav-bg: #161B22;
      }
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: system-ui, -apple-system, Roboto, "Segoe UI", sans-serif;
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 16px;
    }
    .layout {
      width: 100%;
      max-width: 960px;
      display: grid;
      grid-template-columns: minmax(320px, 412px) minmax(260px, 1fr);
      gap: 24px;
      align-items: start;
    }
    @media (max-width: 780px) {
      .layout { grid-template-columns: 1fr; }
    }
    .phone {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 24px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      height: 760px;
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
    }
    .appbar {
      padding: 16px 18px 12px;
      border-bottom: 1px solid var(--border);
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
    }
    .appbar-title {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
    }
    .screen {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }
    .nav {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      border-top: 1px solid var(--border);
      background: var(--nav-bg);
    }
    .nav button {
      border: 0;
      background: transparent;
      color: var(--text-muted);
      padding: 10px 4px 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      font-weight: 500;
      cursor: pointer;
    }
    .nav button.active {
      color: var(--primary);
      font-weight: 600;
    }
    .nav svg { width: 22px; height: 22px; }
    .field {
      width: 100%;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid var(--border);
      background: var(--surface);
      color: var(--text);
      font-size: 14px;
      outline: none;
    }
    .field:focus { border-color: var(--primary); }
    .btn {
      width: 100%;
      padding: 12px 16px;
      border: 0;
      border-radius: 10px;
      background: var(--primary);
      color: #fff;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    @media (prefers-color-scheme: dark) {
      .btn { color: #0b1623; }
    }
    .btn-secondary {
      background: var(--surface-variant);
      color: var(--text);
    }
    .btn-danger {
      background: var(--red);
      color: #fff;
    }
    .card {
      background: var(--surface-variant);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 12px 14px;
      margin-bottom: 10px;
    }
    .card-clickable { cursor: pointer; }
    .row {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .grow { flex: 1; min-width: 0; }
    .title {
      font-size: 15px;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .muted {
      font-size: 12px;
      color: var(--text-muted);
    }
    .progress {
      width: 100%;
      height: 6px;
      border-radius: 99px;
      background: var(--border);
      overflow: hidden;
      margin: 8px 0 6px;
    }
    .progress > div {
      height: 100%;
      background: var(--primary);
      transition: width 0.25s ease;
    }
    .icon-btn {
      border: 0;
      background: transparent;
      color: var(--text);
      width: 34px;
      height: 34px;
      border-radius: 8px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
    }
    .icon-btn:hover { background: rgba(127, 127, 127, 0.14); }
    .section-title {
      font-size: 15px;
      font-weight: 600;
      margin: 16px 0 8px;
    }
    .section-title:first-child { margin-top: 0; }
    .switch-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 0;
      gap: 12px;
      font-size: 14px;
    }
    .label {
      display: block;
      font-size: 12px;
      color: var(--text-muted);
      margin: 10px 0 4px;
    }
    .aside {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 20px;
    }
    .aside h2 { margin: 0 0 6px; font-size: 20px; }
    .meta-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin: 16px 0 18px;
    }
    .meta-item {
      background: var(--surface-variant);
      border-radius: 12px;
      padding: 10px 12px;
    }
    .meta-label { font-size: 11px; color: var(--text-muted); }
    .meta-val { font-size: 14px; font-weight: 600; margin-top: 2px; word-break: break-all; }
    .error-text { color: var(--red); font-size: 13px; margin-top: 6px; }
    .spacer { height: 12px; }
    .spacer-lg { height: 20px; }
  </style>
</head>
<body>
  <div class="layout">
    <main class="phone" aria-label="misterdownloadmanager Android Preview">
      <header class="appbar">
        <h1 class="appbar-title" id="screen-title">misterdownloadmanager</h1>
        <button class="icon-btn" id="back-btn" style="display:none" aria-label="Back to downloads">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
      </header>
      <section class="screen" id="screen"></section>
      <nav class="nav" aria-label="Primary navigation">
        <button data-tab="DOWNLOADS" class="active">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M11 3h2v8h3l-4 4-4-4h3V3zm-6 14h14v2H5v-2z"/></svg>
          <span>Downloads</span>
        </button>
        <button data-tab="ADD">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z"/></svg>
          <span>Add download</span>
        </button>
        <button data-tab="CATEGORIES">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 4h6v6H4V4zm10 0h6v6h-6V4zM4 14h6v6H4v-6zm10 0h6v6h-6v-6z"/></svg>
          <span>Categories</span>
        </button>
        <button data-tab="SETTINGS">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3.2"/><circle cx="12" cy="12" r="6.5"/><path d="M18.5 12H21M3 12h2.5M12 18.5V21M12 3v2.5M16.6 16.6l1.8 1.8M5.6 5.6l1.8 1.8M7.4 16.6l-1.8 1.8M18.4 5.6l-1.8 1.8"/></svg>
          <span>Settings</span>
        </button>
      </nav>
    </main>

    <aside class="aside">
      <h2>misterdownloadmanager</h2>
      <p class="muted" style="margin:0;line-height:1.5">
        Native Android port (Kotlin + Jetpack Compose) of Mister Download Manager v0.5.2.
      </p>
      <div class="meta-grid">
        <div class="meta-item">
          <div class="meta-label">Application ID</div>
          <div class="meta-val">com.misterdownloadmanager.port</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">Version</div>
          <div class="meta-val">0.5.2 (API 24–36)</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">APK Artifact</div>
          <div class="meta-val" id="apk-size">Checking...</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">Built At</div>
          <div class="meta-val" id="apk-time">—</div>
        </div>
      </div>
      <a class="btn" id="apk-download" href="/app-debug.apk" download="misterdownloadmanager-debug.apk">
        Download app-debug.apk
      </a>
    </aside>
  </div>

  <script>
    const CATEGORIES = [
      { id: 'all', name: 'All downloads', extensions: [] },
      { id: 'general', name: 'General', extensions: [] },
      { id: 'compressed', name: 'Compressed', extensions: ['zip','rar','r0*','r1*','arj','gz','sit','sitx','sea','ace','bz2','7z'] },
      { id: 'documents', name: 'Documents', extensions: ['doc','pdf','ppt','pps','docx','pptx'] },
      { id: 'music', name: 'Music', extensions: ['mp3','wav','wma','mpa','ram','ra','aac','aif','m4a','tsa'] },
      { id: 'programs', name: 'Programs', extensions: ['exe','msi'] },
      { id: 'video', name: 'Video', extensions: ['avi','mpg','mpe','mpeg','asf','wmv','mov','qt','rm','mp4','flv','m4v','webm','ogv','ogg','mkv','ts','tsv'] },
      { id: 'torrents', name: 'Torrents', extensions: [] },
    ];

    const state = {
      tab: 'DOWNLOADS',
      selectedId: null,
      searchQuery: '',
      selectedCategoryId: 'all',
      nextId: 4,
      addError: '',
      items: [
        {
          id: 1,
          url: 'https://releases.ubuntu.com/24.04/ubuntu-24.04.2-desktop-amd64.iso',
          fileName: 'ubuntu-24.04.2-desktop-amd64.iso',
          categoryId: 'general',
          kind: 'HTTP',
          status: 'DOWNLOADING',
          totalBytes: 6200000000,
          downloadedBytes: 2480000000,
          speedBytesPerSecond: 14800000,
          connections: 8,
          maxConnections: 8,
          checksum: 'sha256:9b5f...31c8',
          error: null,
        },
        {
          id: 2,
          url: 'https://example.org/files/project-handbook.pdf',
          fileName: 'project-handbook.pdf',
          categoryId: 'documents',
          kind: 'HTTP',
          status: 'COMPLETED',
          totalBytes: 18432000,
          downloadedBytes: 18432000,
          speedBytesPerSecond: 0,
          connections: 0,
          maxConnections: 8,
          checksum: null,
          error: null,
        },
        {
          id: 3,
          url: 'magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=sample-archive',
          fileName: 'sample-archive.torrent',
          categoryId: 'torrents',
          kind: 'TORRENT',
          status: 'STOPPED',
          totalBytes: 940000000,
          downloadedBytes: 310000000,
          speedBytesPerSecond: 0,
          connections: 0,
          maxConnections: 8,
          checksum: null,
          error: null,
        },
      ],
      settings: {
        autoStartDownloads: true,
        resumeOnStart: true,
        showNotifications: true,
        followSystemTheme: true,
        maxSimultaneousDownloads: 4,
        maxConnectionsPerDownload: 8,
        downloadFolder: '/Android/data/com.misterdownloadmanager.port/files/Download',
        speedLimitOn: false,
        speedLimitKbPerSecond: 10,
        proxyEnabled: false,
        verifyChecksumAfterDownload: true,
      },
    };

    function formatBytes(bytes) {
      if (bytes < 0) return 'Unknown';
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
      return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    }

    function statusText(item) {
      if (item.status === 'QUEUED') return 'Queued';
      if (item.status === 'DOWNLOADING') return 'Downloading ' + formatBytes(item.speedBytesPerSecond) + '/s';
      if (item.status === 'STOPPED') return 'Stopped';
      if (item.status === 'COMPLETED') return 'Completed';
      return item.error || 'Failed';
    }

    function categoryOf(name) {
      const ext = (name.split('.').pop() || '').toLowerCase();
      for (const cat of CATEGORIES) {
        if (cat.id === 'all' || cat.id === 'general' || cat.id === 'torrents') continue;
        if (cat.extensions.some(p => p.endsWith('*') ? ext.startsWith(p.slice(0, -1)) : p === ext)) return cat.id;
      }
      return 'general';
    }

    function escapeHtml(s) {
      return String(s).replace(/[&<>"']/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c]));
    }

    function addDownload(rawUrl, forceTorrent) {
      const url = rawUrl.trim();
      const valid = url.startsWith('http://') || url.startsWith('https://') || url.startsWith('ftp://') || url.startsWith('magnet:');
      if (!valid) {
        state.addError = 'Enter a valid URL or magnet link.';
        render();
        return;
      }
      state.addError = '';
      const isMagnet = forceTorrent || url.startsWith('magnet:');
      const fallbackName = isMagnet ? 'torrent-' + state.nextId + '.torrent' : (url.split('/').pop().split('?')[0] || 'download');
      const item = {
        id: state.nextId++,
        url,
        fileName: fallbackName,
        categoryId: isMagnet ? 'torrents' : categoryOf(fallbackName),
        kind: isMagnet ? 'TORRENT' : (url.startsWith('ftp://') ? 'FTP' : 'HTTP'),
        status: state.settings.autoStartDownloads ? 'DOWNLOADING' : 'QUEUED',
        totalBytes: 120 * 1024 * 1024,
        downloadedBytes: 0,
        speedBytesPerSecond: state.settings.autoStartDownloads ? 3200000 : 0,
        connections: state.settings.autoStartDownloads ? state.settings.maxConnectionsPerDownload : 0,
        maxConnections: state.settings.maxConnectionsPerDownload,
        checksum: null,
        error: null,
      };
      state.items.unshift(item);
      state.tab = 'DOWNLOADS';
      state.selectedId = null;
      render();
    }

    function render() {
      const screen = document.getElementById('screen');
      const backBtn = document.getElementById('back-btn');
      const title = document.getElementById('screen-title');

      document.querySelectorAll('.nav button').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === state.tab && state.selectedId === null);
      });

      const selected = state.items.find(i => i.id === state.selectedId);
      if (selected) {
        backBtn.style.display = 'inline-flex';
        title.textContent = selected.fileName;
        const pct = selected.totalBytes > 0 ? Math.min(100, Math.round((selected.downloadedBytes / selected.totalBytes) * 100)) : 0;
        screen.innerHTML = \`
          <div class="title" style="font-size:18px;white-space:normal">\${escapeHtml(selected.fileName)}</div>
          <div class="muted" style="margin-top:6px;word-break:break-all">\${escapeHtml(selected.url)}</div>
          <div class="progress" style="margin-top:16px"><div style="width:\${pct}%"></div></div>
          <div class="card" style="margin-top:12px">
            <div class="muted">Status: <strong style="color:var(--text)">\${escapeHtml(statusText(selected))}</strong></div>
            <div class="muted" style="margin-top:4px">Size: <strong style="color:var(--text)">\${formatBytes(selected.totalBytes)}</strong></div>
            <div class="muted" style="margin-top:4px">Downloaded: <strong style="color:var(--text)">\${formatBytes(selected.downloadedBytes)}</strong></div>
            <div class="muted" style="margin-top:4px">Speed: <strong style="color:var(--text)">\${formatBytes(selected.speedBytesPerSecond)}/s</strong></div>
            <div class="muted" style="margin-top:4px">Connections: <strong style="color:var(--text)">\${selected.connections} / \${selected.maxConnections}</strong></div>
            \${selected.checksum ? '<div class="muted" style="margin-top:4px">Checksum: <strong style="color:var(--text)">' + escapeHtml(selected.checksum) + '</strong></div>' : ''}
          </div>
          \${(selected.status === 'DOWNLOADING' || selected.status === 'QUEUED') ? '<button class="btn" id="detail-pause">Pause</button><div class="spacer"></div>' : ''}
          \${(selected.status === 'STOPPED' || selected.status === 'ERROR') ? '<button class="btn" id="detail-resume">Resume</button><div class="spacer"></div>' : ''}
          <button class="btn btn-danger" id="detail-delete">Delete</button>
        \`;
        document.getElementById('detail-pause')?.addEventListener('click', () => {
          selected.status = 'STOPPED'; selected.speedBytesPerSecond = 0; selected.connections = 0; render();
        });
        document.getElementById('detail-resume')?.addEventListener('click', () => {
          selected.status = 'DOWNLOADING'; selected.speedBytesPerSecond = 4500000; selected.connections = selected.maxConnections; render();
        });
        document.getElementById('detail-delete')?.addEventListener('click', () => {
          state.items = state.items.filter(i => i.id !== selected.id); state.selectedId = null; render();
        });
        return;
      }

      backBtn.style.display = 'none';
      title.textContent = 'misterdownloadmanager';

      if (state.tab === 'DOWNLOADS') {
        const q = state.searchQuery.trim().toLowerCase();
        const filtered = state.items.filter(item => {
          const catOk = state.selectedCategoryId === 'all' || item.categoryId === state.selectedCategoryId;
          const qOk = !q || item.fileName.toLowerCase().includes(q) || item.url.toLowerCase().includes(q);
          return catOk && qOk;
        });
        screen.innerHTML = \`
          <input class="field" id="search-input" type="search" placeholder="Search downloads" value="\${escapeHtml(state.searchQuery)}" />
          <div class="spacer"></div>
          \${filtered.length === 0
            ? '<div class="title">No downloads yet</div><div class="muted" style="margin-top:4px">Add a link or a torrent file to start downloading.</div>'
            : filtered.map(item => {
                const pct = item.totalBytes > 0 ? Math.min(100, Math.round((item.downloadedBytes / item.totalBytes) * 100)) : 0;
                const canPause = item.status === 'DOWNLOADING' || item.status === 'QUEUED';
                const canResume = item.status === 'STOPPED' || item.status === 'ERROR';
                return \`
                  <div class="card card-clickable" data-select="\${item.id}">
                    <div class="row">
                      <div class="grow">
                        <div class="title">\${escapeHtml(item.fileName)}</div>
                        <div class="muted">\${escapeHtml(statusText(item))}</div>
                      </div>
                      \${canPause ? '<button class="icon-btn" data-pause="' + item.id + '" aria-label="Pause">&#10074;&#10074;</button>' : ''}
                      \${canResume ? '<button class="icon-btn" data-resume="' + item.id + '" aria-label="Resume">&#9654;</button>' : ''}
                      <button class="icon-btn" data-delete="\${item.id}" aria-label="Delete">&#10005;</button>
                    </div>
                    <div class="progress"><div style="width:\${pct}%"></div></div>
                    <div class="muted">\${formatBytes(item.downloadedBytes)} / \${formatBytes(item.totalBytes)}</div>
                  </div>
                \`;
              }).join('')}
        \`;
        const searchInput = document.getElementById('search-input');
        searchInput.addEventListener('input', e => {
          state.searchQuery = e.target.value;
          const pos = e.target.selectionStart;
          render();
          const el = document.getElementById('search-input');
          el.focus();
          el.setSelectionRange(pos, pos);
        });
        screen.querySelectorAll('[data-select]').forEach(el => {
          el.addEventListener('click', () => { state.selectedId = Number(el.dataset.select); render(); });
        });
        screen.querySelectorAll('[data-pause]').forEach(el => {
          el.addEventListener('click', e => {
            e.stopPropagation();
            const item = state.items.find(i => i.id === Number(el.dataset.pause));
            if (item) { item.status = 'STOPPED'; item.speedBytesPerSecond = 0; item.connections = 0; render(); }
          });
        });
        screen.querySelectorAll('[data-resume]').forEach(el => {
          el.addEventListener('click', e => {
            e.stopPropagation();
            const item = state.items.find(i => i.id === Number(el.dataset.resume));
            if (item) { item.status = 'DOWNLOADING'; item.speedBytesPerSecond = 4500000; item.connections = item.maxConnections; render(); }
          });
        });
        screen.querySelectorAll('[data-delete]').forEach(el => {
          el.addEventListener('click', e => {
            e.stopPropagation();
            state.items = state.items.filter(i => i.id !== Number(el.dataset.delete));
            render();
          });
        });
      } else if (state.tab === 'ADD') {
        screen.innerHTML = \`
          <label class="label" for="url-input">Download URL</label>
          <input class="field" id="url-input" type="url" placeholder="https://example.com/archive.zip" />
          <div class="spacer"></div>
          <button class="btn" id="btn-add-url">Add link</button>
          <div class="spacer-lg"></div>
          <label class="label" for="magnet-input">Magnet link</label>
          <input class="field" id="magnet-input" type="text" placeholder="magnet:?xt=urn:btih:..." />
          <div class="spacer"></div>
          <button class="btn" id="btn-add-magnet">Add torrent</button>
          \${state.addError ? '<div class="error-text">' + escapeHtml(state.addError) + '</div>' : ''}
        \`;
        document.getElementById('btn-add-url').addEventListener('click', () => {
          addDownload(document.getElementById('url-input').value, false);
        });
        document.getElementById('btn-add-magnet').addEventListener('click', () => {
          addDownload(document.getElementById('magnet-input').value, true);
        });
      } else if (state.tab === 'CATEGORIES') {
        screen.innerHTML = CATEGORIES.map(cat => \`
          <div class="card card-clickable" data-cat="\${cat.id}">
            <div class="title">\${escapeHtml(cat.name)}</div>
            \${cat.extensions.length ? '<div class="muted" style="margin-top:4px">' + escapeHtml(cat.extensions.join(', ')) + '</div>' : ''}
            \${cat.id === state.selectedCategoryId ? '<div style="color:var(--primary);font-size:12px;font-weight:600;margin-top:4px">Selected</div>' : ''}
          </div>
        \`).join('');
        screen.querySelectorAll('[data-cat]').forEach(el => {
          el.addEventListener('click', () => {
            state.selectedCategoryId = el.dataset.cat;
            state.tab = 'DOWNLOADS';
            render();
          });
        });
      } else if (state.tab === 'SETTINGS') {
        const s = state.settings;
        screen.innerHTML = \`
          <div class="section-title">General</div>
          <label class="switch-row"><span>Start downloads automatically</span><input type="checkbox" data-bool="autoStartDownloads" \${s.autoStartDownloads ? 'checked' : ''} /></label>
          <label class="switch-row"><span>Resume downloads on start</span><input type="checkbox" data-bool="resumeOnStart" \${s.resumeOnStart ? 'checked' : ''} /></label>
          <label class="switch-row"><span>Show notifications</span><input type="checkbox" data-bool="showNotifications" \${s.showNotifications ? 'checked' : ''} /></label>
          <label class="switch-row"><span>Follow system theme</span><input type="checkbox" data-bool="followSystemTheme" \${s.followSystemTheme ? 'checked' : ''} /></label>
          <div class="section-title">Downloads</div>
          <label class="label">Maximum simultaneous downloads</label>
          <input class="field" type="number" min="1" max="32" data-num="maxSimultaneousDownloads" value="\${s.maxSimultaneousDownloads}" />
          <label class="label">Maximum connections per download</label>
          <input class="field" type="number" min="1" max="128" data-num="maxConnectionsPerDownload" value="\${s.maxConnectionsPerDownload}" />
          <label class="label">Download folder</label>
          <input class="field" type="text" data-str="downloadFolder" value="\${escapeHtml(s.downloadFolder)}" />
          <div class="section-title">Speed limits</div>
          <label class="switch-row"><span>Speed limit</span><input type="checkbox" data-bool="speedLimitOn" \${s.speedLimitOn ? 'checked' : ''} /></label>
          <label class="label">Speed limit (KB/s)</label>
          <input class="field" type="number" min="1" data-num="speedLimitKbPerSecond" value="\${s.speedLimitKbPerSecond}" />
          <div class="section-title">Connection</div>
          <label class="switch-row"><span>Use proxy</span><input type="checkbox" data-bool="proxyEnabled" \${s.proxyEnabled ? 'checked' : ''} /></label>
          <label class="switch-row"><span>Verify checksum after download</span><input type="checkbox" data-bool="verifyChecksumAfterDownload" \${s.verifyChecksumAfterDownload ? 'checked' : ''} /></label>
        \`;
        screen.querySelectorAll('[data-bool]').forEach(el => {
          el.addEventListener('change', () => { state.settings[el.dataset.bool] = el.checked; });
        });
        screen.querySelectorAll('[data-num]').forEach(el => {
          el.addEventListener('change', () => { const v = Number(el.value); if (!Number.isNaN(v)) state.settings[el.dataset.num] = v; });
        });
        screen.querySelectorAll('[data-str]').forEach(el => {
          el.addEventListener('change', () => { state.settings[el.dataset.str] = el.value; });
        });
      }
    }

    document.querySelectorAll('.nav button').forEach(btn => {
      btn.addEventListener('click', () => {
        state.tab = btn.dataset.tab;
        state.selectedId = null;
        render();
      });
    });
    document.getElementById('back-btn').addEventListener('click', () => {
      state.selectedId = null;
      render();
    });

    setInterval(() => {
      let changed = false;
      for (const item of state.items) {
        if (item.status === 'DOWNLOADING' && item.totalBytes > 0) {
          item.downloadedBytes = Math.min(item.totalBytes, item.downloadedBytes + item.speedBytesPerSecond);
          if (item.downloadedBytes >= item.totalBytes) {
            item.status = 'COMPLETED';
            item.speedBytesPerSecond = 0;
            item.connections = 0;
          }
          changed = true;
        }
      }
      if (changed && (state.tab === 'DOWNLOADS' || state.selectedId !== null)) {
        if (document.activeElement?.id !== 'search-input') render();
      }
    }, 1000);

    fetch('/api/apk-info').then(r => r.json()).then(info => {
      const sizeEl = document.getElementById('apk-size');
      const timeEl = document.getElementById('apk-time');
      const dlEl = document.getElementById('apk-download');
      if (info.available) {
        sizeEl.textContent = (info.size / (1024 * 1024)).toFixed(2) + ' MB';
        timeEl.textContent = new Date(info.mtime).toLocaleString();
      } else {
        sizeEl.textContent = 'Not built yet';
        dlEl.style.opacity = '0.5';
        dlEl.style.pointerEvents = 'none';
      }
    }).catch(() => {});

    render();
  </script>
</body>
</html>
`;

const server = http.createServer((req, res) => {
  const urlPath = (req.url || '/').split('?')[0];

  if (urlPath === '/health' || urlPath === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok' }));
    return;
  }

  if (urlPath === '/api/apk-info') {
    const apk = findApk();
    res.writeHead(200, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
    res.end(JSON.stringify(apk ? { available: true, size: apk.size, mtime: apk.mtime } : { available: false }));
    return;
  }

  if (urlPath === '/app-debug.apk') {
    const apk = findApk();
    if (!apk) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('APK not found. Build the project first.');
      return;
    }
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': apk.size,
      'Content-Disposition': 'attachment; filename="misterdownloadmanager-debug.apk"',
      'Cache-Control': 'no-store',
    });
    fs.createReadStream(apk.path).pipe(res);
    return;
  }

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(HTML);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`misterdownloadmanager preview server listening on http://0.0.0.0:${PORT}`);
});

<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<%
  if (session.getAttribute("user") == null) {
    response.sendRedirect(request.getContextPath() + "/login.jsp");
    return;
  }
  request.setAttribute("currentPage", "search");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SoundWave — Search</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@700;800;900&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
  <style>
    .genre-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 0.875rem;
      margin-bottom: 2rem;
    }
    .genre-card {
      border-radius: var(--radius);
      padding: 1rem 1.25rem;
      cursor: pointer;
      font-family: var(--font-display);
      font-weight: 700;
      font-size: 1rem;
      height: 90px;
      display: flex; align-items: flex-end;
      position: relative; overflow: hidden;
      transition: transform var(--transition), box-shadow var(--transition);
    }
    .genre-card:hover { transform: scale(1.04); box-shadow: var(--shadow-lg); }
    .genre-card .g-icon {
      position: absolute; right: 12px; top: 12px;
      font-size: 2rem; opacity: 0.85;
    }
    .genre-card .g-name { position: relative; z-index: 1; }

    .search-results-section { display: none; margin-bottom: 2rem; }
    .search-results-section.show { display: block; }
    .no-results { color: var(--text3); font-size: 0.875rem; padding: 1rem 0.5rem; }
  </style>
</head>
<body>

<%@ include file="includes/layout-top.jspf" %>

      <h1 style="font-family:var(--font-display); font-size:1.5rem; font-weight:800; margin-bottom:1.5rem">Search</h1>

      <!-- Genre browse -->
      <div id="browseSection">
        <p style="font-size:1rem; font-weight:600; margin-bottom:1rem; color:var(--text2)">Browse all</p>
        <div class="genre-grid">
          <div class="genre-card" style="background:#1a001a;color:#fca5a5" onclick="filterGenre('Pop')">
            <span class="g-icon">🎶</span><span class="g-name">Pop</span>
          </div>
          <div class="genre-card" style="background:#2a1015;color:#fda4af" onclick="filterGenre('Ballad')">
            <span class="g-icon">🎵</span><span class="g-name">Ballad</span>
          </div>
          <div class="genre-card" style="background:#1e102a;color:#c084fc" onclick="filterGenre('R&B')">
            <span class="g-icon">🎷</span><span class="g-name">R&B</span>
          </div>
          <div class="genre-card" style="background:#261805;color:#fbbf24" onclick="filterGenre('Hip Hop')">
            <span class="g-icon">🎤</span><span class="g-name">Hip Hop</span>
          </div>
          <div class="genre-card" style="background:#2d0a0a;color:#f87171" onclick="filterGenre('Rock')">
            <span class="g-icon">🎸</span><span class="g-name">Rock</span>
          </div>
          <div class="genre-card" style="background:#0c2333;color:#38bdf8" onclick="filterGenre('Electronic / EDM')">
            <span class="g-icon">⚡</span><span class="g-name">Electronic / EDM</span>
          </div>
          <div class="genre-card" style="background:#06281e;color:#34d399" onclick="filterGenre('Acoustic')">
            <span class="g-icon">🪕</span><span class="g-name">Acoustic</span>
          </div>
          <div class="genre-card" style="background:#160d2b;color:#a78bfa" onclick="filterGenre('Lo-Fi / Chill')">
            <span class="g-icon">☕</span><span class="g-name">Lo-Fi / Chill</span>
          </div>
          <div class="genre-card" style="background:#2e1025;color:#f472b6" onclick="filterGenre('Barbie Playlist')">
            <span class="g-icon">👸</span><span class="g-name">Barbie</span>
          </div>
        </div>
      </div>

      <!-- Search results (shown when query entered) -->
      <div class="search-results-section" id="searchResultsSection">
        <div class="section-title">
          <span id="resultsTitle">Results</span>
          <span class="section-more" onclick="clearSearch()">← Back</span>
        </div>
        <div class="track-list" id="searchResultsList">
          <div class="no-results">No results found.</div>
        </div>
      </div>

<%@ include file="includes/layout-bottom.jspf" %>

<script>
// Tìm kiếm trên trang Search hiển thị kết quả inline (thay vì overlay toàn cục).
async function searchPageGlobalSearch(val) {
  if (!val.trim()) { clearSearch(); return; }
  // Hide global overlay
  document.getElementById('search-overlay').style.display = 'none';

  const res = await App.API.get('/api/songs/search?q=' + encodeURIComponent(val));
  const section = document.getElementById('searchResultsSection');
  const list = document.getElementById('searchResultsList');
  const title = document.getElementById('resultsTitle');
  document.getElementById('browseSection').style.display = 'none';
  section.classList.add('show');

  if (!res || !res.songs || !res.songs.length) {
    title.textContent = `No results for "${val}"`;
    list.innerHTML = '<div class="no-results">No songs found. Try another keyword.</div>';
    return;
  }
  title.textContent = `Results for "${val}" (${res.songs.length})`;
  list.innerHTML = res.songs.map((t, i) => renderTrackItem(t, i + 1)).join('');
  App.fillMissingCovers();
}

async function filterGenre(genre, pushState = true) {
  if (pushState) {
    history.pushState({ spa: true, url: window.location.href }, '', '?genre=' + encodeURIComponent(genre));
  }
  document.getElementById('browseSection').style.display = 'none';
  const section = document.getElementById('searchResultsSection');
  const list = document.getElementById('searchResultsList');
  const title = document.getElementById('resultsTitle');
  section.classList.add('show');
  title.textContent = `Genre: ${genre}`;
  list.innerHTML = '<div style="color:var(--text3);font-size:.85rem;padding:.5rem">Loading...</div>';

  const res = await App.API.get('/api/songs/search?genre=' + encodeURIComponent(genre));
  if (!res || !res.songs || !res.songs.length) {
    list.innerHTML = '<div class="no-results">No songs in this genre yet.</div>';
    return;
  }
  title.textContent = `Genre: ${genre} (${res.songs.length})`;
  list.innerHTML = res.songs.map((t, i) => renderTrackItem(t, i + 1)).join('');
  App.fillMissingCovers();
}

function clearSearch(pushState = true) {
  if (pushState) {
    history.pushState({ spa: true, url: window.location.pathname }, '', window.location.pathname);
  }
  document.getElementById('browseSection').style.display = 'block';
  document.getElementById('searchResultsSection').classList.remove('show');
  document.getElementById('globalSearch').value = '';
}

// Đồng bộ trạng thái filter từ URL (?genre=...). Dùng cho cả init lẫn popstate.
function syncSearchFromUrl() {
  const genre = new URLSearchParams(window.location.search).get('genre');
  if (genre) filterGenre(genre, false);
  else clearSearch(false);
}

function initSearch() {
  // Override ô tìm kiếm toàn cục, lưu lại bản gốc để khôi phục khi rời trang.
  if (!window.__defaultGlobalSearch) window.__defaultGlobalSearch = window.handleGlobalSearch;
  window.handleGlobalSearch = searchPageGlobalSearch;
  syncSearchFromUrl();
}

function cleanupSearch() {
  // Trả lại ô tìm kiếm toàn cục về hành vi mặc định.
  if (window.__defaultGlobalSearch) window.handleGlobalSearch = window.__defaultGlobalSearch;
}

// Phase 3: genre filter dùng History API được gộp vào router global qua onPopState.
App.Router.register('search', { init: initSearch, cleanup: cleanupSearch, onPopState: syncSearchFromUrl });
</script>

</body>
</html>

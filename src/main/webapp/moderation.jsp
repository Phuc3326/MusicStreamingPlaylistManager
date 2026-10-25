<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    request.setAttribute("currentPage", "moderation");
    models.User currentUser = (models.User) session.getAttribute("user");
    if (currentUser == null || !currentUser.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/home.jsp");
        return;
    }

    java.util.List<models.Song> pendingSongs = (java.util.List<models.Song>) request.getAttribute("pendingSongs");
    if (pendingSongs == null) {
        models.SongDAO sDao = new models.SongDAO();
        pendingSongs = sDao.getPendingSongs();
        request.setAttribute("pendingSongs", pendingSongs);
    }

    String modMsg = (String) session.getAttribute("modMessage");
    String modErr = (String) session.getAttribute("modError");
    if (modMsg != null) session.removeAttribute("modMessage");
    if (modErr != null) session.removeAttribute("modError");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SoundWave — Song Moderation</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@700;800;900&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
  <style>
    .mod-header {
      margin-bottom: 2rem;
    }
    .mod-header h1 {
      font-size: 1.8rem;
      font-weight: 800;
      color: var(--text);
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .badge-count {
      background: #7c3aed;
      color: #fff;
      font-size: 0.85rem;
      padding: 3px 10px;
      border-radius: 999px;
      font-weight: 800;
    }
    .mod-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      overflow: hidden;
      box-shadow: 0 10px 30px rgba(0,0,0,0.25);
    }
    .mod-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    .mod-table th {
      padding: 1rem 1.25rem;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: var(--text3);
      border-bottom: 1px solid var(--border);
      background: rgba(255,255,255,0.015);
    }
    .mod-table td {
      padding: 1rem 1.25rem;
      font-size: 0.9rem;
      border-bottom: 1px solid rgba(255,255,255,0.04);
      vertical-align: middle;
    }
    .mod-table tr:hover {
      background: rgba(255,255,255,0.02);
    }
    .track-cell {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .track-img {
      width: 48px; height: 48px;
      border-radius: 8px;
      object-fit: cover;
      background: var(--bg2);
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.25rem;
    }
    .track-meta .track-title {
      font-weight: 700;
      color: var(--text);
      font-size: 0.95rem;
    }
    .track-meta .track-artist {
      font-size: 0.8rem;
      color: var(--text2);
    }
    .uploader-tag {
      display: inline-block;
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--border);
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 0.75rem;
      color: var(--accent);
      font-weight: 600;
    }
    .btn-action-group {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 0.5rem;
    }
    .btn-approve {
      background: #10b981;
      color: #fff;
      border: none;
      padding: 0.45rem 0.9rem;
      border-radius: 6px;
      font-weight: 700;
      font-size: 0.8rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: all 0.15s;
    }
    .btn-approve:hover {
      background: #059669;
      transform: translateY(-1px);
    }
    .btn-reject {
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.3);
      padding: 0.45rem 0.9rem;
      border-radius: 6px;
      font-weight: 700;
      font-size: 0.8rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: all 0.15s;
    }
    .btn-reject:hover {
      background: #ef4444;
      color: #fff;
    }
    .btn-preview {
      background: rgba(124, 58, 237, 0.15);
      color: #c4b5fd;
      border: 1px solid rgba(167, 139, 250, 0.3);
      padding: 0.45rem 0.75rem;
      border-radius: 6px;
      font-weight: 700;
      font-size: 0.8rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: all 0.15s;
    }
    .btn-preview:hover {
      background: var(--accent);
      color: #fff;
    }
    .empty-state {
      padding: 4.5rem 2rem;
      text-align: center;
    }
    .empty-state svg {
      width: 60px; height: 60px;
      stroke: #10b981;
      margin-bottom: 1rem;
    }
    .empty-state h3 {
      font-size: 1.3rem;
      font-weight: 700;
      color: var(--text);
      margin-bottom: 0.4rem;
    }
    .empty-state p {
      color: var(--text2);
      font-size: 0.9rem;
    }
  </style>
</head>
<body>

<%@ include file="/includes/layout-top.jspf" %>

<div class="mod-header">
  <h1>
    Pending Moderation
    <span class="badge-count">${pendingSongs.size()}</span>
  </h1>
  <p style="color:var(--text2); font-size:0.9rem; margin-top:0.3rem;">
    Review user-submitted music tracks before approving them for the public catalog.
  </p>
</div>

<% if (modMsg != null) { %>
  <div style="background:rgba(16,185,129,0.15); border:1px solid rgba(16,185,129,0.4); color:#34d399; padding:0.85rem 1rem; border-radius:8px; margin-bottom:1.25rem; font-weight:600;">
    ✔ <%= modMsg %>
  </div>
<% } %>
<% if (modErr != null) { %>
  <div style="background:rgba(239,68,68,0.15); border:1px solid rgba(239,68,68,0.4); color:#f87171; padding:0.85rem 1rem; border-radius:8px; margin-bottom:1.25rem; font-weight:600;">
    ⚠ <%= modErr %>
  </div>
<% } %>

<div class="mod-card">
  <c:choose>
    <c:when test="${empty pendingSongs}">
      <div class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <h3>All caught up!</h3>
        <p>There are no songs awaiting moderation at this moment.</p>
      </div>
    </c:when>
    <c:otherwise>
      <table class="mod-table">
        <thead>
          <tr>
            <th style="width: 50px;">#</th>
            <th>Track Info</th>
            <th>Genre</th>
            <th>Uploader</th>
            <th style="text-align: right; width: 260px;">Moderation Actions</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="s" items="${pendingSongs}" varStatus="status">
            <tr>
              <td>${status.index + 1}</td>
              <td>
                <div class="track-cell">
                  <c:choose>
                    <c:when test="${not empty s.coverPath}">
                      <img src="${s.coverPath}" class="track-img" alt="${s.title}">
                    </c:when>
                    <c:otherwise>
                      <div class="track-img">🎵</div>
                    </c:otherwise>
                  </c:choose>
                  <div class="track-meta">
                    <div class="track-title">${s.title}</div>
                    <div class="track-artist">${s.artist}</div>
                  </div>
                </div>
              </td>
              <td>${s.genre}</td>
              <td>
                <span class="uploader-tag">@${not empty s.uploaderName ? s.uploaderName : 'User #'.concat(s.uploadedBy)}</span>
              </td>
              <td style="text-align: right;">
                <div class="btn-action-group">
                  <button type="button" class="btn-preview" title="Listen preview"
                          data-id="${s.songId}"
                          data-title="<c:out value='${s.title}'/>"
                          data-artist="<c:out value='${s.artist}'/>"
                          data-genre="<c:out value='${s.genre}'/>"
                          data-duration="${s.duration}"
                          data-file="<c:out value='${s.filePath}'/>"
                          data-cover="<c:out value='${s.coverPath}'/>"
                          onclick="handlePreview(this)">
                    ▶ Preview
                  </button>

                  <form action="<%= request.getContextPath() %>/moderation" method="POST" style="display:inline;">
                    <input type="hidden" name="songId" value="${s.songId}">
                    <input type="hidden" name="action" value="approve">
                    <button type="submit" class="btn-approve">
                      ✔ Approve
                    </button>
                  </form>

                  <form action="<%= request.getContextPath() %>/moderation" method="POST" style="display:inline;" onsubmit="return confirm('Reject this song?');">
                    <input type="hidden" name="songId" value="${s.songId}">
                    <input type="hidden" name="action" value="reject">
                    <button type="submit" class="btn-reject">
                      ✖ Reject
                    </button>
                  </form>
                </div>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</div>

<script>
  function handlePreview(btn) {
    if (!btn) return;
    const track = {
      songId: parseInt(btn.dataset.id, 10),
      title: btn.dataset.title,
      artist: btn.dataset.artist,
      genre: btn.dataset.genre,
      duration: parseInt(btn.dataset.duration, 10) || 180,
      filePath: btn.dataset.file,
      coverPath: btn.dataset.cover || ''
    };
    const player = window.App || (typeof App !== 'undefined' ? App : null);
    if (player && typeof player.playTrack === 'function') {
      window.App.playTrack(track);
    }
  }

  function previewTrack(id, title, artist, genre, duration, filePath, coverPath) {
    const track = {
      songId: id,
      title: title,
      artist: artist,
      genre: genre,
      duration: duration,
      filePath: filePath,
      coverPath: coverPath
    };
    const player = window.App || (typeof App !== 'undefined' ? App : null);
    if (player && typeof player.playTrack === 'function') {
      window.App.playTrack(track);
    }
  }
</script>

<%@ include file="/includes/layout-bottom.jspf" %>
</body>
</html>

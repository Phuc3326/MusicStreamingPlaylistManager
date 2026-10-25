<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    request.setAttribute("currentPage", "my-uploads");
    models.User currentUser = (models.User) session.getAttribute("user");
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    java.util.List<models.Song> mySongs = (java.util.List<models.Song>) request.getAttribute("mySongs");
    if (mySongs == null) {
        models.SongDAO sDao = new models.SongDAO();
        mySongs = sDao.getSongsByUploader(currentUser.getUserId());
        request.setAttribute("mySongs", mySongs);
    }

    String successMsg = (String) session.getAttribute("uploadSuccess");
    String errorMsg = (String) session.getAttribute("uploadError");
    if (successMsg != null) session.removeAttribute("uploadSuccess");
    if (errorMsg != null) session.removeAttribute("uploadError");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SoundWave — My Uploads</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@700;800;900&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
  <style>
    .uploads-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.75rem;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .header-title-wrap h1 {
      font-size: 1.8rem;
      font-weight: 800;
      color: var(--text);
      margin-bottom: 0.3rem;
    }
    .header-title-wrap p {
      color: var(--text2);
      font-size: 0.9rem;
    }
    .btn-upload-new {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      background: linear-gradient(135deg, #7c3aed, #9333ea);
      color: #fff;
      padding: 0.65rem 1.25rem;
      border-radius: 999px;
      font-weight: 700;
      font-size: 0.9rem;
      text-decoration: none;
      box-shadow: 0 4px 15px rgba(124,58,237,0.3);
      transition: all 0.2s;
    }
    .btn-upload-new:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(124,58,237,0.5);
    }
    .btn-upload-new svg {
      width: 18px; height: 18px; stroke: currentColor; fill: none; stroke-width: 2.2;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.3px;
    }
    .status-approved {
      background: rgba(16, 185, 129, 0.15);
      color: #34d399;
      border: 1px solid rgba(16, 185, 129, 0.35);
    }
    .status-pending {
      background: rgba(245, 158, 11, 0.15);
      color: #fbbf24;
      border: 1px solid rgba(245, 158, 11, 0.35);
    }
    .status-rejected {
      background: rgba(239, 68, 68, 0.15);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.35);
    }
    .visibility-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 0.75rem;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 6px;
      background: rgba(255,255,255,0.05);
      border: 1px solid var(--border);
      color: var(--text2);
    }
    .visibility-private {
      border-color: rgba(167, 139, 250, 0.3);
      color: #c4b5fd;
      background: rgba(124, 58, 237, 0.1);
    }
    .uploads-table-wrap {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      overflow: hidden;
    }
    .uploads-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }
    .uploads-table th {
      padding: 1rem 1.25rem;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: var(--text3);
      border-bottom: 1px solid var(--border);
      background: rgba(255,255,255,0.015);
    }
    .uploads-table td {
      padding: 0.85rem 1.25rem;
      font-size: 0.9rem;
      border-bottom: 1px solid rgba(255,255,255,0.04);
      vertical-align: middle;
    }
    .uploads-table tr:hover {
      background: rgba(255,255,255,0.03);
    }
    .track-cell {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .track-img {
      width: 44px; height: 44px;
      border-radius: 8px;
      object-fit: cover;
      background: var(--bg2);
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.2rem;
    }
    .track-meta .track-title {
      font-weight: 700;
      color: var(--text);
      margin-bottom: 2px;
    }
    .track-meta .track-artist {
      font-size: 0.8rem;
      color: var(--text2);
    }
    .action-btn {
      background: transparent;
      border: none;
      color: var(--text3);
      cursor: pointer;
      padding: 6px;
      border-radius: 6px;
      transition: all 0.15s;
    }
    .action-btn:hover {
      color: var(--text);
      background: var(--surface2);
    }
    .action-btn.btn-play-song:hover {
      color: #34d399;
    }
    .action-btn.btn-delete:hover {
      color: #f87171;
    }
    .action-btn svg {
      width: 18px; height: 18px;
      stroke: currentColor; fill: none; stroke-width: 2;
    }
    .empty-state {
      padding: 4rem 2rem;
      text-align: center;
    }
    .empty-state svg {
      width: 56px; height: 56px;
      stroke: var(--text3);
      margin-bottom: 1rem;
    }
    .empty-state h3 {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text);
      margin-bottom: 0.5rem;
    }
    .empty-state p {
      color: var(--text2);
      font-size: 0.9rem;
      margin-bottom: 1.5rem;
    }
  </style>
</head>
<body>

<%@ include file="/includes/layout-top.jspf" %>

<div class="uploads-header">
  <div class="header-title-wrap">
    <h1>My Music Library</h1>
    <p>Manage your private vault and community-shared audio tracks</p>
  </div>
  <a href="<%= request.getContextPath() %>/upload.jsp" class="btn-upload-new">
    <svg><use href="#ic-plus"/></svg> Upload New Song
  </a>
</div>

<% if (successMsg != null) { %>
  <div style="background:rgba(16,185,129,0.15); border:1px solid rgba(16,185,129,0.4); color:#34d399; padding:0.85rem 1rem; border-radius:8px; margin-bottom:1.25rem; font-weight:600;">
    ✔ <%= successMsg %>
  </div>
<% } %>

<div class="uploads-table-wrap">
  <c:choose>
    <c:when test="${empty mySongs}">
      <div class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke-width="1.5"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
        <h3>No songs uploaded yet</h3>
        <p>You haven't contributed any tracks to SoundWave. Store your music privately or share with everyone!</p>
        <a href="<%= request.getContextPath() %>/upload.jsp" class="btn-upload-new">
          <svg><use href="#ic-upload"/></svg> Upload Your First Song
        </a>
      </div>
    </c:when>
    <c:otherwise>
      <table class="uploads-table">
        <thead>
          <tr>
            <th style="width: 50px;">#</th>
            <th>Title & Artist</th>
            <th>Genre</th>
            <th>Visibility</th>
            <th>Review Status</th>
            <th style="text-align: right; width: 120px;">Actions</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="s" items="${mySongs}" varStatus="status">
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
                <c:choose>
                  <c:when test="${s.visibility == 'PRIVATE'}">
                    <span class="visibility-tag visibility-private">🔒 Private</span>
                  </c:when>
                  <c:otherwise>
                    <span class="visibility-tag">🌐 Public</span>
                  </c:otherwise>
                </c:choose>
              </td>
              <td>
                <c:choose>
                  <c:when test="${s.visibility == 'PRIVATE'}">
                    <span class="status-badge status-approved">● Available</span>
                  </c:when>
                  <c:when test="${s.status == 'APPROVED'}">
                    <span class="status-badge status-approved">● Approved</span>
                  </c:when>
                  <c:when test="${s.status == 'PENDING'}">
                    <span class="status-badge status-pending">● In Review</span>
                  </c:when>
                  <c:otherwise>
                    <span class="status-badge status-rejected" title="${s.rejectReason}">● Rejected</span>
                  </c:otherwise>
                </c:choose>
              </td>
              <td style="text-align: right;">
                <button class="action-btn btn-play-song" title="Play track"
                        data-id="${s.songId}"
                        data-title="<c:out value='${s.title}'/>"
                        data-artist="<c:out value='${s.artist}'/>"
                        data-genre="<c:out value='${s.genre}'/>"
                        data-duration="${s.duration}"
                        data-file="<c:out value='${s.filePath}'/>"
                        data-cover="<c:out value='${s.coverPath}'/>"
                        onclick="handlePlayUploaded(this)">
                  <svg viewBox="0 0 24 24"><polygon points="5,3 19,12 5,21"/></svg>
                </button>
                <form action="<%= request.getContextPath() %>/api/songs/delete" method="POST" style="display:inline;" onsubmit="return confirm('Are you sure you want to delete this song?');">
                  <input type="hidden" name="songId" value="${s.songId}">
                  <button type="submit" class="action-btn btn-delete" title="Delete song">
                    <svg><use href="#ic-trash"/></svg>
                  </button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:otherwise>
  </c:choose>
</div>

<script>
  function handlePlayUploaded(btn) {
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
    } else {
      console.error('App player is not available');
    }
  }

  function playUploadedTrack(id, title, artist, genre, duration, filePath, coverPath) {
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
    } else {
      console.error('App player is not available');
    }
  }
</script>

<%@ include file="/includes/layout-bottom.jspf" %>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    request.setAttribute("currentPage", "upload");
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
  <title>SoundWave — Upload Song</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@700;800;900&family=DM+Sans:wght@300;400;500;600&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css">
  <style>
    .upload-container {
      max-width: 680px;
      margin: 1.5rem auto 3rem;
    }
    .upload-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 2rem 2.5rem;
      box-shadow: 0 10px 30px rgba(0,0,0,0.3);
    }
    .upload-header {
      margin-bottom: 1.5rem;
      text-align: center;
    }
    .upload-title {
      font-size: 1.6rem;
      font-weight: 800;
      color: var(--text);
      margin-bottom: 0.4rem;
    }
    .upload-subtitle {
      color: var(--text2);
      font-size: 0.9rem;
    }
    .form-group {
      margin-bottom: 1.25rem;
    }
    .form-group label {
      display: block;
      font-weight: 600;
      font-size: 0.85rem;
      color: var(--text2);
      margin-bottom: 0.5rem;
    }
    .form-input, .form-select {
      width: 100%;
      background: var(--bg2);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 0.75rem 1rem;
      color: var(--text);
      font-size: 0.95rem;
      outline: none;
      transition: border-color 0.2s;
    }
    .form-input:focus, .form-select:focus {
      border-color: var(--accent);
    }

    /* Privacy Selector Cards */
    .privacy-options {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-top: 0.4rem;
    }
    .privacy-card {
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 1rem;
      background: var(--bg2);
      cursor: pointer;
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      transition: all 0.2s;
    }
    .privacy-card:hover {
      border-color: var(--border2);
      background: rgba(255,255,255,0.03);
    }
    .privacy-card.active {
      border-color: var(--accent);
      background: rgba(124,58,237,0.12);
    }
    .privacy-card input[type="radio"] {
      margin-top: 3px;
      accent-color: var(--accent);
    }
    .privacy-info h4 {
      font-size: 0.9rem;
      font-weight: 700;
      color: var(--text);
      margin-bottom: 2px;
    }
    .privacy-info p {
      font-size: 0.75rem;
      color: var(--text3);
      line-height: 1.3;
    }

    .file-drop-zone {
      border: 2px dashed var(--border2);
      border-radius: 12px;
      padding: 1.5rem;
      text-align: center;
      background: rgba(255,255,255,0.02);
      cursor: pointer;
      transition: all 0.2s;
      position: relative;
    }
    .file-drop-zone:hover {
      border-color: var(--accent);
      background: rgba(124,58,237,0.06);
    }
    .file-drop-zone input[type="file"] {
      position: absolute;
      top: 0; left: 0; width: 100%; height: 100%;
      opacity: 0;
      cursor: pointer;
    }
    .drop-icon {
      width: 38px; height: 38px;
      margin: 0 auto 0.6rem;
      color: var(--accent);
    }
    .drop-text {
      font-weight: 600;
      font-size: 0.9rem;
      color: var(--text);
      margin-bottom: 0.25rem;
    }
    .drop-hint {
      font-size: 0.75rem;
      color: var(--text3);
    }
    .file-selected-name {
      margin-top: 0.5rem;
      font-size: 0.85rem;
      color: #34d399;
      font-weight: 700;
      display: none;
    }
    .btn-submit {
      width: 100%;
      background: linear-gradient(135deg, #7c3aed, #9333ea);
      border: none;
      border-radius: 999px;
      padding: 0.85rem;
      color: #fff;
      font-weight: 800;
      font-size: 1rem;
      cursor: pointer;
      margin-top: 1.5rem;
      box-shadow: 0 4px 15px rgba(124,58,237,0.4);
      transition: all 0.2s;
    }
    .btn-submit:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(124,58,237,0.6);
    }
    .alert-box {
      padding: 0.85rem 1rem;
      border-radius: 8px;
      margin-bottom: 1.25rem;
      font-size: 0.9rem;
      font-weight: 600;
    }
    .alert-success { background: rgba(16,185,129,0.15); border: 1px solid rgba(16,185,129,0.4); color: #34d399; }
    .alert-error { background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.4); color: #f87171; }
  </style>
</head>
<body>

<%@ include file="/includes/layout-top.jspf" %>

<div class="upload-container">

  <% if (successMsg != null) { %>
    <div class="alert-box alert-success">✔ <%= successMsg %></div>
  <% } %>
  <% if (errorMsg != null) { %>
    <div class="alert-box alert-error">⚠ <%= errorMsg %></div>
  <% } %>

  <div class="upload-card">
    <div class="upload-header">
      <div class="upload-title">Upload New Music</div>
      <div class="upload-subtitle">Store in your personal vault or share with SoundWave listeners</div>
    </div>

    <form action="<%= request.getContextPath() %>/upload" method="POST" enctype="multipart/form-data" id="uploadForm">
      
      <div class="form-group">
        <label for="title">Song Title *</label>
        <input type="text" id="title" name="title" class="form-input" placeholder="e.g. Starboy, Shape of You..." required maxlength="255">
      </div>

      <div class="form-group">
        <label for="artist">Artist Name *</label>
        <input type="text" id="artist" name="artist" class="form-input" placeholder="e.g. The Weeknd, Adele..." required maxlength="255">
      </div>

      <div class="form-group">
        <label for="genre">Genre</label>
        <select id="genre" name="genre" class="form-select">
          <option value="Pop">Pop</option>
          <option value="Ballad">Ballad</option>
          <option value="R&B">R&B</option>
          <option value="Hip Hop">Hip Hop</option>
          <option value="Rock">Rock</option>
          <option value="Electronic / EDM">Electronic / EDM</option>
          <option value="Acoustic">Acoustic</option>
          <option value="Lo-Fi / Chill">Lo-Fi / Chill</option>
        </select>
      </div>

      <!-- Visibility / Privacy Mode Selection -->
      <div class="form-group">
        <label>Privacy & Distribution Mode *</label>
        <div class="privacy-options">
          <label class="privacy-card active" id="cardPublic" onclick="setPrivacy('PUBLIC')">
            <input type="radio" name="visibility" value="PUBLIC" checked>
            <div class="privacy-info">
              <h4>🌐 Public (Community)</h4>
              <p>Will be submitted for Admin review before appearing on SoundWave Explore & Search.</p>
            </div>
          </label>

          <label class="privacy-card" id="cardPrivate" onclick="setPrivacy('PRIVATE')">
            <input type="radio" name="visibility" value="PRIVATE">
            <div class="privacy-info">
              <h4>🔒 Private (My Vault)</h4>
              <p>Instant access in your personal library. No admin review required.</p>
            </div>
          </label>
        </div>
      </div>

      <!-- Hidden input for auto-detected audio duration -->
      <input type="hidden" id="duration" name="duration" value="180">

      <div class="form-group">
        <label>Audio File (.mp3, .wav, .ogg) *</label>
        <div class="file-drop-zone" id="audioDropZone">
          <svg class="drop-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
          <div class="drop-text">Click or drag & drop audio track here</div>
          <div class="drop-hint">Max file size: 15 MB. Supported: MP3, WAV, OGG</div>
          <div class="file-selected-name" id="audioSelectedName"></div>
          <input type="file" id="audioFile" name="audioFile" accept="audio/mpeg,audio/mp3,audio/wav,audio/ogg" required onchange="handleAudioSelect(this)">
        </div>
      </div>

      <div class="form-group">
        <label>Cover Artwork (Optional)</label>
        <div class="file-drop-zone">
          <svg class="drop-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
          <div class="drop-text">Choose square cover image</div>
          <div class="drop-hint">Max file size: 5 MB (JPG, PNG, WebP)</div>
          <div class="file-selected-name" id="coverSelectedName"></div>
          <input type="file" id="coverFile" name="coverFile" accept="image/jpeg,image/png,image/webp" onchange="handleCoverSelect(this)">
        </div>
      </div>

      <button type="submit" class="btn-submit" id="btnSubmit">
        Upload Song to SoundWave
      </button>

    </form>
  </div>
</div>

<script>
  function setPrivacy(mode) {
    const cardPub = document.getElementById('cardPublic');
    const cardPriv = document.getElementById('cardPrivate');
    if (mode === 'PUBLIC') {
      cardPub.classList.add('active');
      cardPriv.classList.remove('active');
      cardPub.querySelector('input').checked = true;
    } else {
      cardPriv.classList.add('active');
      cardPub.classList.remove('active');
      cardPriv.querySelector('input').checked = true;
    }
  }

  function handleAudioSelect(input) {
    const file = input.files[0];
    const label = document.getElementById('audioSelectedName');
    if (!file) {
      label.style.display = 'none';
      return;
    }
    label.innerText = '🎵 Selected: ' + file.name + ' (' + (file.size / (1024*1024)).toFixed(2) + ' MB)';
    label.style.display = 'block';

    const titleInput = document.getElementById('title');
    if (!titleInput.value) {
      const cleanName = file.name.replace(/\.[^/.]+$/, '').replace(/_/g, ' ');
      titleInput.value = cleanName;
    }

    try {
      const tempAudio = new Audio();
      tempAudio.src = URL.createObjectURL(file);
      tempAudio.addEventListener('loadedmetadata', function() {
        if (tempAudio.duration && !isNaN(tempAudio.duration)) {
          document.getElementById('duration').value = Math.round(tempAudio.duration);
        }
      });
    } catch(e) {}
  }

  function handleCoverSelect(input) {
    const file = input.files[0];
    const label = document.getElementById('coverSelectedName');
    if (!file) {
      label.style.display = 'none';
      return;
    }
    label.innerText = '🖼 Artwork: ' + file.name + ' (' + (file.size / 1024).toFixed(1) + ' KB)';
    label.style.display = 'block';
  }

  document.getElementById('uploadForm').addEventListener('submit', function() {
    const btn = document.getElementById('btnSubmit');
    btn.innerText = 'Uploading to Cloud Storage... Please wait';
    btn.disabled = true;
    btn.style.opacity = '0.7';
  });
</script>

<%@ include file="/includes/layout-bottom.jspf" %>
</body>
</html>

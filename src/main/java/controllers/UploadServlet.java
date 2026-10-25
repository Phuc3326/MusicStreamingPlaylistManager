package controllers;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import models.Song;
import models.SongDAO;
import models.User;
import utils.FileStorageService;
import utils.SongLibraryUtils;

@WebServlet(name = "UploadServlet", urlPatterns = {"/upload", "/api/songs/upload", "/api/songs/delete"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1,  // 1 MB
    maxFileSize = 1024 * 1024 * 20,       // 20 MB (Cho file nhạc MP3)
    maxRequestSize = 1024 * 1024 * 25     // 25 MB
)
public class UploadServlet extends HttpServlet {

    private final SongDAO songDAO = new SongDAO();
    private final FileStorageService fileStorageService = FileStorageService.getInstance();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        request.getRequestDispatcher("/upload.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            respondOrRedirect(request, response, false, "Vui lòng đăng nhập để thực hiện thao tác này.", "/login.jsp");
            return;
        }

        User currentUser = (User) session.getAttribute("user");
        String path = request.getServletPath();

        try {
            if ("/api/songs/delete".equals(path)) {
                handleDelete(request, response, currentUser);
            } else {
                handleUpload(request, response, currentUser);
            }
        } catch (Exception e) {
            e.printStackTrace();
            respondOrRedirect(request, response, false, "Lỗi xử lý: " + e.getMessage(), "/upload.jsp");
        }
    }

    private void handleUpload(HttpServletRequest request, HttpServletResponse response, User user)
            throws Exception {
        String title = request.getParameter("title");
        String artist = request.getParameter("artist");
        String genre = request.getParameter("genre");
        String durationStr = request.getParameter("duration");
        String visibility = request.getParameter("visibility"); // "PUBLIC" hoặc "PRIVATE"

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên bài hát.");
        }
        if (artist == null || artist.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên nghệ sĩ / ca sĩ.");
        }
        if (genre == null || genre.trim().isEmpty()) {
            genre = "Pop";
        }

        if (visibility == null || (!visibility.equalsIgnoreCase("PRIVATE") && !visibility.equalsIgnoreCase("PUBLIC"))) {
            visibility = "PUBLIC";
        }
        visibility = visibility.toUpperCase();

        int duration = 0;
        if (durationStr != null && !durationStr.trim().isEmpty()) {
            try {
                duration = Math.round(Float.parseFloat(durationStr.trim()));
            } catch (Exception ignored) {}
        }
        if (duration <= 0) {
            duration = 180; // Mặc định 3 phút nếu không trích xuất được thời lượng
        }

        Part audioPart = request.getPart("audioFile");
        Part coverPart = request.getPart("coverFile");

        if (audioPart == null || audioPart.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn file âm thanh (.mp3, .wav, .ogg).");
        }

        // Lưu file audio (Cloudinary hoặc Local Disk qua FileStorageService)
        String audioUrl = fileStorageService.storeAudio(audioPart, "soundwave/songs");

        // Lưu ảnh bìa nếu có tải lên
        String coverUrl = null;
        if (coverPart != null && coverPart.getSize() > 0) {
            coverUrl = fileStorageService.storeImage(coverPart, "soundwave/covers");
        }

        // ==========================================
        // QUY TẮC NGHIỆP VỤ (BUSINESS LOGIC):
        // 1. Nếu PRIVATE (Kho nhạc riêng):
        //    -> Không cần duyệt (status = APPROVED), chỉ mình user nghe được.
        // 2. Nếu PUBLIC (Chia sẻ cộng đồng):
        //    -> ADMIN đăng: status = APPROVED (xuất hiện ngay trên Home/Search)
        //    -> USER đăng: status = PENDING (chờ Admin kiểm duyệt)
        // ==========================================
        String initialStatus;
        String successMessage;

        if ("PRIVATE".equals(visibility)) {
            initialStatus = "APPROVED";
            successMessage = "Tải lên thành công vào Kho nhạc cá nhân (Private)! Chỉ riêng bạn có thể nghe bài hát này.";
        } else {
            // PUBLIC
            if (user.isAdmin()) {
                initialStatus = "APPROVED";
                successMessage = "Tải lên thành công! Bài hát đã được công khai trên Trang chủ SoundWave.";
            } else {
                initialStatus = "PENDING";
                successMessage = "Tải lên thành công! Bài hát đang chờ Admin kiểm duyệt để xuất hiện công khai trên SoundWave.";
            }
        }

        Song newSong = new Song(
            0,
            title.trim(),
            artist.trim(),
            genre.trim(),
            duration,
            audioUrl,
            coverUrl,
            user.getUserId(),
            user.getUsername(),
            visibility,
            initialStatus,
            null,
            null
        );

        int songId = songDAO.insertSong(newSong);
        if (songId <= 0) {
            fileStorageService.delete(audioUrl);
            fileStorageService.delete(coverUrl);
            throw new RuntimeException("Không thể lưu bài hát vào cơ sở dữ liệu.");
        }

        // Nếu bài hát công khai và đã duyệt, nạp lại thư viện tìm kiếm
        if ("PUBLIC".equals(visibility) && "APPROVED".equals(initialStatus)) {
            try {
                SongLibraryUtils.reload(getServletContext());
            } catch (Exception ex) {
                log("Không thể reload song library: " + ex.getMessage());
            }
        }

        respondOrRedirect(request, response, true, successMessage, "/my-uploads.jsp");
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, User user)
            throws Exception {
        String songIdStr = request.getParameter("songId");
        if (songIdStr == null || songIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã bài hát cần xóa.");
        }

        int songId = Integer.parseInt(songIdStr.trim());
        Song song = songDAO.getById(songId);
        if (song == null) {
            throw new IllegalArgumentException("Bài hát không tồn tại.");
        }

        // Chỉ Admin hoặc chính người tải lên mới được xóa
        boolean isOwner = (song.getUploadedBy() != null && song.getUploadedBy() == user.getUserId());
        if (!user.isAdmin() && !isOwner) {
            throw new SecurityException("Bạn không có quyền xóa bài hát này!");
        }

        // Xóa file vật lý / Cloudinary
        fileStorageService.delete(song.getFilePath());
        fileStorageService.delete(song.getCoverPath());

        // Xóa trong database
        songDAO.deleteSong(songId);

        // Nạp lại library nếu bài đã từng public
        if (song.isPublic() && song.isApproved()) {
            try {
                SongLibraryUtils.reload(getServletContext());
            } catch (Exception ignored) {}
        }

        respondOrRedirect(request, response, true, "Đã xóa bài hát thành công.", "/my-uploads.jsp");
    }

    private void respondOrRedirect(HttpServletRequest request, HttpServletResponse response,
                                   boolean success, String message, String redirectPath) throws IOException {
        String accept = request.getHeader("Accept");
        boolean isAjax = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        if (isAjax) {
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            JsonObject json = new JsonObject();
            json.addProperty("success", success);
            json.addProperty("message", message);
            out.print(json.toString());
            out.flush();
        } else {
            HttpSession session = request.getSession();
            if (success) {
                session.setAttribute("uploadSuccess", message);
            } else {
                session.setAttribute("uploadError", message);
            }
            response.sendRedirect(request.getContextPath() + redirectPath);
        }
    }
}

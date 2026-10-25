package controllers;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import models.Song;
import models.SongDAO;
import models.User;
import utils.SongLibraryUtils;

@WebServlet(name = "ModerationServlet", urlPatterns = {"/moderation", "/api/admin/moderation"})
public class ModerationServlet extends HttpServlet {

    private final SongDAO songDAO = new SongDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/home.jsp");
            return;
        }

        try {
            List<Song> pendingSongs = songDAO.getPendingSongs();
            request.setAttribute("pendingSongs", pendingSongs);
            request.setAttribute("pendingCount", pendingSongs.size());
            request.getRequestDispatcher("/moderation.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/home.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        String songIdStr = request.getParameter("songId");
        String reason = request.getParameter("reason");

        try {
            if (songIdStr == null || action == null) {
                throw new IllegalArgumentException("Dữ liệu yêu cầu không hợp lệ.");
            }

            int songId = Integer.parseInt(songIdStr.trim());
            boolean success = false;
            String message = "";

            if ("approve".equalsIgnoreCase(action)) {
                success = songDAO.approveSong(songId);
                if (success) {
                    message = "Đã duyệt bài hát! Bài hát hiện đã có mặt trên trang chủ và tìm kiếm.";
                    SongLibraryUtils.reload(getServletContext());
                }
            } else if ("reject".equalsIgnoreCase(action)) {
                if (reason == null || reason.trim().isEmpty()) {
                    reason = "Bài hát không đáp ứng tiêu chuẩn cộng đồng của SoundWave.";
                }
                success = songDAO.rejectSong(songId, reason.trim());
                if (success) {
                    message = "Đã từ chối duyệt bài hát này.";
                }
            }

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
                session.setAttribute("modMessage", message);
                response.sendRedirect(request.getContextPath() + "/moderation");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (response.getContentType() != null && response.getContentType().contains("json")) {
                PrintWriter out = response.getWriter();
                out.print("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                out.flush();
            } else {
                session.setAttribute("modError", "Lỗi: " + e.getMessage());
                response.sendRedirect(request.getContextPath() + "/moderation");
            }
        }
    }

    private boolean isAdmin(HttpSession session) {
        if (session == null) return false;
        User user = (User) session.getAttribute("user");
        return user != null && user.isAdmin();
    }
}

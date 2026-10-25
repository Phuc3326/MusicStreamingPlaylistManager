package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet phục vụ các tập tin tĩnh trong thư mục uploads (nhạc MP3, ảnh bìa)
 * khi hệ thống chạy ở chế độ lưu trữ cục bộ (Local Storage Fallback).
 * Ánh xạ tất cả các request có tiền tố /uploads/*
 */
@WebServlet(name = "MediaServeServlet", urlPatterns = {"/uploads/*"})
public class MediaServeServlet extends HttpServlet {

    private Path uploadRoot;

    @Override
    public void init() throws ServletException {
        String uploadDir = System.getenv("UPLOAD_DIR");
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            uploadDir = "./uploads";
        }
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo(); // ví dụ: /songs/uuid.mp3
        if (pathInfo == null || pathInfo.contains("..")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File file = uploadRoot.resolve(pathInfo.substring(1)).toFile();
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = getServletContext().getMimeType(file.getName());
        if (mimeType == null) {
            if (file.getName().endsWith(".mp3")) mimeType = "audio/mpeg";
            else if (file.getName().endsWith(".wav")) mimeType = "audio/wav";
            else if (file.getName().endsWith(".ogg")) mimeType = "audio/ogg";
            else mimeType = "application/octet-stream";
        }

        long fileLength = file.length();
        String rangeHeader = request.getHeader("Range");

        // Xử lý HTTP 206 Partial Content khi trình duyệt gửi header Range (đặc thù thẻ <audio> trên Chrome/Edge)
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String rangeValue = rangeHeader.substring(6).trim();
            long start = 0;
            long end = fileLength - 1;

            int dashPos = rangeValue.indexOf('-');
            if (dashPos != -1) {
                String startStr = rangeValue.substring(0, dashPos).trim();
                String endStr = rangeValue.substring(dashPos + 1).trim();

                try {
                    if (!startStr.isEmpty()) {
                        start = Long.parseLong(startStr);
                    }
                    if (!endStr.isEmpty()) {
                        end = Long.parseLong(endStr);
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (start > end || start >= fileLength) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }

            if (end >= fileLength) {
                end = fileLength - 1;
            }

            long contentLength = end - start + 1;
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setContentType(mimeType);

            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
                 OutputStream out = response.getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[16384];
                long remaining = contentLength;
                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int read = raf.read(buffer, 0, toRead);
                    if (read == -1) break;
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_OK); // 200
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(fileLength));
            response.setContentType(mimeType);

            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}

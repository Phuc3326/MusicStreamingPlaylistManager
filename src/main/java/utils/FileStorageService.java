package utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Part;

/**
 * Dịch vụ lưu trữ tập tin tập trung (Centralized File Storage Service) cho SoundWave.
 * 
 * Hỗ trợ linh hoạt 2 chế độ (Two Backends):
 * 1. Cloudinary (khi biến môi trường CLOUDINARY_URL được thiết lập):
 *    Tải thẳng file lên Cloudinary CDN, trả về HTTPS URL để phát trực tiếp.
 * 2. Ổ cứng cục bộ (Local Disk - chế độ mặc định khi chưa cấu hình Cloudinary):
 *    Lưu trữ trong thư mục máy chủ (mặc định ./uploads hoặc UPLOAD_DIR).
 *
 * Kiểm duyệt chặt chẽ: Dung lượng tối đa, định dạng MIME và Magic Bytes thực tế của tập tin.
 */
public class FileStorageService {

    private static final long MAX_AUDIO_BYTES = 15L * 1024 * 1024; // 15 MB
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;  // 5 MB

    private static final Map<String, String> AUDIO_EXTENSIONS = new HashMap<String, String>() {{
        put("audio/mpeg", ".mp3");
        put("audio/mp3", ".mp3");
        put("audio/wav", ".wav");
        put("audio/x-wav", ".wav");
        put("audio/ogg", ".ogg");
        put("application/ogg", ".ogg");
    }};

    private static final Map<String, String> IMAGE_EXTENSIONS = new HashMap<String, String>() {{
        put("image/jpeg", ".jpg");
        put("image/jpg", ".jpg");
        put("image/png", ".png");
        put("image/webp", ".webp");
    }};

    private static final Pattern CLOUDINARY_URL_PATTERN = Pattern.compile(
            "^https://res\\.cloudinary\\.com/[^/]+/(image|video|raw)/upload/(?:v\\d+/)?(.+)\\.([a-zA-Z0-9]+)$");

    private static FileStorageService instance;

    private final Path uploadRoot;
    private final Cloudinary cloudinary;

    private FileStorageService() {
        String uploadDir = System.getenv("UPLOAD_DIR");
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            uploadDir = "./uploads";
        }
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();

        String cloudinaryUrl = System.getenv("CLOUDINARY_URL");
        if (cloudinaryUrl != null && !cloudinaryUrl.trim().isEmpty()) {
            this.cloudinary = new Cloudinary(cloudinaryUrl.trim());
            System.out.println("[FileStorageService] Khởi tạo lưu trữ Cloudinary: " + cloudinary.config.cloudName);
            warnWhenCredentialsAreRejected();
        } else {
            this.cloudinary = null;
            System.out.println("[FileStorageService] Chưa cấu hình CLOUDINARY_URL -> Sử dụng ổ đĩa cục bộ: " + uploadRoot);
        }
    }

    public static synchronized FileStorageService getInstance() {
        if (instance == null) {
            instance = new FileStorageService();
        }
        return instance;
    }

    public boolean isCloudinaryEnabled() {
        return this.cloudinary != null;
    }

    // ==========================================
    // 1. LƯU TRỮ VÀ XỬ LÝ FILE NHẠC (AUDIO)
    // ==========================================

    public void validateAudio(Part part) throws IllegalArgumentException, IOException {
        if (part == null || part.getSize() == 0) {
            throw new IllegalArgumentException("Vui lòng chọn tập tin âm thanh.");
        }
        if (part.getSize() > MAX_AUDIO_BYTES) {
            throw new IllegalArgumentException("Dung lượng bài hát không được vượt quá 15 MB.");
        }

        String mimeType = part.getContentType() != null ? part.getContentType().toLowerCase(Locale.ROOT) : "";
        String ext = getExtensionFromSubmittedName(part.getSubmittedFileName());

        // Kiểm tra mime hoặc đuôi mở rộng
        boolean validMime = AUDIO_EXTENSIONS.containsKey(mimeType);
        boolean validExt = ext.equals(".mp3") || ext.equals(".wav") || ext.equals(".ogg");

        if (!validMime && !validExt) {
            throw new IllegalArgumentException("Định dạng âm thanh không hợp lệ! Chỉ chấp nhận file MP3, WAV hoặc OGG.");
        }

        // Đọc magic bytes đầu file để phòng chống giả mạo extension
        byte[] head = readLeadingBytes(part, 12);
        if (!isRealAudioContent(head)) {
            throw new IllegalArgumentException("Nội dung tập tin không phải âm thanh hợp lệ (Fake extension).");
        }
    }

    public String storeAudio(Part part, String folder) throws IOException {
        validateAudio(part);
        String cleanFolder = (folder == null || folder.trim().isEmpty()) ? "soundwave/songs" : folder.replaceAll("^/+|/+$", "");

        if (cloudinary != null) {
            try (InputStream is = part.getInputStream()) {
                byte[] bytes = readAllBytes(is);
                Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                        "public_id", cleanFolder + "/" + UUID.randomUUID(),
                        "asset_folder", cleanFolder,
                        "resource_type", "video" // Cloudinary quản lý audio dưới định dạng video
                ));
                return (String) result.get("secure_url");
            }
        } else {
            return storeLocally(part, "songs", AUDIO_EXTENSIONS);
        }
    }

    // ==========================================
    // 2. LƯU TRỮ VÀ XỬ LÝ ẢNH BÌA (COVER IMAGE)
    // ==========================================

    public void validateImage(Part part) throws IllegalArgumentException, IOException {
        if (part == null || part.getSize() == 0) {
            return; // Ảnh bìa là tùy chọn (optional)
        }
        if (part.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Ảnh bìa không được vượt quá 5 MB.");
        }

        String mimeType = part.getContentType() != null ? part.getContentType().toLowerCase(Locale.ROOT) : "";
        String ext = getExtensionFromSubmittedName(part.getSubmittedFileName());

        boolean validMime = IMAGE_EXTENSIONS.containsKey(mimeType);
        boolean validExt = ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp");

        if (!validMime && !validExt) {
            throw new IllegalArgumentException("Ảnh bìa chỉ chấp nhận định dạng JPG, PNG hoặc WebP.");
        }

        byte[] head = readLeadingBytes(part, 12);
        if (!isRealImageContent(head)) {
            throw new IllegalArgumentException("Tập tin ảnh bìa không đúng định dạng ảnh thực tế.");
        }
    }

    public String storeImage(Part part, String folder) throws IOException {
        if (part == null || part.getSize() == 0) {
            return null;
        }
        validateImage(part);
        String cleanFolder = (folder == null || folder.trim().isEmpty()) ? "soundwave/covers" : folder.replaceAll("^/+|/+$", "");

        if (cloudinary != null) {
            try (InputStream is = part.getInputStream()) {
                byte[] bytes = readAllBytes(is);
                Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                        "public_id", cleanFolder + "/" + UUID.randomUUID(),
                        "asset_folder", cleanFolder,
                        "resource_type", "image"
                ));
                return (String) result.get("secure_url");
            }
        } else {
            return storeLocally(part, "covers", IMAGE_EXTENSIONS);
        }
    }

    // ==========================================
    // 3. XÓA TẬP TIN (CLEAN UP / REMOVE)
    // ==========================================

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return;

        // Nếu là URL Cloudinary
        if (cloudinary != null && fileUrl.contains("cloudinary.com")) {
            Matcher matcher = CLOUDINARY_URL_PATTERN.matcher(fileUrl);
            if (matcher.matches()) {
                String resourceType = matcher.group(1);
                String publicId = matcher.group(2);
                try {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType, "invalidate", true));
                } catch (Exception ex) {
                    System.err.println("[FileStorageService] Không thể xóa file trên Cloudinary: " + ex.getMessage());
                }
            }
            return;
        }

        // Nếu là file local (/uploads/...)
        if (fileUrl.startsWith("/uploads/") || fileUrl.startsWith("uploads/")) {
            try {
                String relativePath = fileUrl.replaceFirst("^/+", "");
                Path target = uploadRoot.resolve(relativePath.replace("uploads/", "")).normalize();
                Files.deleteIfExists(target);
            } catch (Exception ignored) {
            }
        }
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH HỖ TRỢ (HELPERS)
    // ==========================================

    private String storeLocally(Part part, String subDir, Map<String, String> extensionMap) throws IOException {
        Path targetDir = uploadRoot.resolve(subDir);
        Files.createDirectories(targetDir);

        String ext = getExtensionFromSubmittedName(part.getSubmittedFileName());
        if (ext.isEmpty()) {
            ext = extensionMap.getOrDefault(part.getContentType(), ".bin");
        }

        String filename = UUID.randomUUID().toString() + ext;
        Path destination = targetDir.resolve(filename);

        try (InputStream is = part.getInputStream()) {
            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/" + subDir + "/" + filename;
    }

    private static String getExtensionFromSubmittedName(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private static byte[] readLeadingBytes(Part part, int maxBytes) throws IOException {
        try (InputStream is = part.getInputStream()) {
            byte[] buffer = new byte[maxBytes];
            int read = is.read(buffer);
            if (read <= 0) return new byte[0];
            return read == maxBytes ? buffer : Arrays.copyOf(buffer, read);
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private static boolean isRealImageContent(byte[] head) {
        if (head == null || head.length < 3) return false;
        // JPEG: FF D8 FF
        if (head[0] == (byte) 0xFF && head[1] == (byte) 0xD8 && head[2] == (byte) 0xFF) return true;
        // PNG: 89 50 4E 47
        if (head.length >= 4 && head[0] == (byte) 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47) return true;
        // WebP: RIFF....WEBP
        if (head.length >= 12 && startsWith(head, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && Arrays.equals(Arrays.copyOfRange(head, 8, 12), "WEBP".getBytes(StandardCharsets.US_ASCII))) {
            return true;
        }
        return false;
    }

    private static boolean isRealAudioContent(byte[] head) {
        if (head == null || head.length < 3) return false;
        // ID3 tag (MP3 with ID3v2 metadata): "ID3"
        if (startsWith(head, "ID3".getBytes(StandardCharsets.US_ASCII))) return true;
        // MP3 raw frame header sync: 0xFF followed by 0xE0 or 0xF0 range
        if ((head[0] & 0xFF) == 0xFF && ((head[1] & 0xE0) == 0xE0)) return true;
        // WAV: "RIFF....WAVE"
        if (head.length >= 12 && startsWith(head, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && Arrays.equals(Arrays.copyOfRange(head, 8, 12), "WAVE".getBytes(StandardCharsets.US_ASCII))) {
            return true;
        }
        // OGG: "OggS"
        if (head.length >= 4 && startsWith(head, "OggS".getBytes(StandardCharsets.US_ASCII))) return true;

        return false;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
    }

    private void warnWhenCredentialsAreRejected() {
        try {
            cloudinary.api().ping(ObjectUtils.emptyMap());
        } catch (Exception ex) {
            System.err.println("[FileStorageService] CẢNH BÁO: CLOUDINARY_URL không hợp lệ hoặc bị từ chối: " + ex.getMessage());
        }
    }
}

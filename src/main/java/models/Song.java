package models;

import java.sql.Timestamp;

public class Song {
    private int songId;       
    private String title;     
    private String artist;    
    private String genre;     
    private int duration;
    private String filePath;
    private String coverPath;
    private Integer uploadedBy;
    private String uploaderName;
    private String visibility;  // "PUBLIC" hoặc "PRIVATE"
    private String status;      // "APPROVED", "PENDING", "REJECTED"
    private String rejectReason;
    private Timestamp createdAt;

    public Song() {
        this.visibility = "PUBLIC";
        this.status = "APPROVED";
    }

    public Song(int songId, String title, String artist, String genre, int duration) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.duration = duration;
        this.visibility = "PUBLIC";
        this.status = "APPROVED";
    }
    
    public Song(int songId, String title, String artist, String genre,
                int duration, String filePath, String coverPath) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.duration = duration;
        this.filePath = filePath;
        this.coverPath = coverPath;
        this.visibility = "PUBLIC";
        this.status = "APPROVED";
    }

    public Song(int songId, String title, String artist, String genre,
                int duration, String filePath, String coverPath,
                Integer uploadedBy, String uploaderName,
                String visibility, String status, String rejectReason, Timestamp createdAt) {
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.duration = duration;
        this.filePath = filePath;
        this.coverPath = coverPath;
        this.uploadedBy = uploadedBy;
        this.uploaderName = uploaderName;
        this.visibility = (visibility != null && !visibility.trim().isEmpty()) ? visibility.toUpperCase() : "PUBLIC";
        this.status = (status != null && !status.trim().isEmpty()) ? status.toUpperCase() : "APPROVED";
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
    }

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = (visibility != null) ? visibility.toUpperCase() : "PUBLIC";
    }

    public boolean isPublic() {
        return "PUBLIC".equalsIgnoreCase(this.visibility);
    }

    public boolean isPrivate() {
        return "PRIVATE".equalsIgnoreCase(this.visibility);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status != null) ? status.toUpperCase() : "APPROVED";
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(this.status);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(this.status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(this.status);
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

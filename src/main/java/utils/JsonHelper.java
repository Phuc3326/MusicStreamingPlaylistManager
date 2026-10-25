package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lib.DynamicArrayList;
import lib.IndexedDoublyLinkedList;
import models.Song;

public class JsonHelper {

    public static JsonObject songToJson(Song s) {
        JsonObject obj = new JsonObject();
        obj.addProperty("songId", s.getSongId());
        obj.addProperty("title", s.getTitle());
        obj.addProperty("artist", s.getArtist());
        obj.addProperty("genre", s.getGenre());
        obj.addProperty("duration", s.getDuration());
        obj.addProperty("filePath", s.getFilePath());
        obj.addProperty("coverPath", s.getCoverPath());

        if (s.getUploadedBy() != null) {
            obj.addProperty("uploadedBy", s.getUploadedBy());
        }
        if (s.getUploaderName() != null) {
            obj.addProperty("uploaderName", s.getUploaderName());
        }
        obj.addProperty("status", s.getStatus() != null ? s.getStatus() : "APPROVED");

        int min = s.getDuration() / 60;
        int sec = s.getDuration() % 60;
        obj.addProperty("durationStr", min + ":" + (sec < 10 ? "0" + sec : sec));
        return obj;
    }

    public static JsonArray songsToJsonArray(DynamicArrayList songs) {
        JsonArray jsonArray = new JsonArray();
        if (songs == null) return jsonArray;

        for (int i = 0; i < songs.size(); i++) {
            jsonArray.add(songToJson(songs.get(i)));
        }
        return jsonArray;
    }

    public static JsonArray waitListToJson(IndexedDoublyLinkedList waitingList) {
        return songsToJsonArray(waitingList != null ? waitingList.toSongList() : null);
    }
}

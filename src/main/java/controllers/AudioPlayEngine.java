package controllers;

import java.util.Random;
import lib.DoublyLinkedList;
import lib.DynamicArrayList;
import lib.Node;
import models.Song;

/**
 * Cỗ máy quản lý phiên phát nhạc.
 *
 * KHÔNG truy vấn Database. Toàn bộ dữ liệu bài hát được lấy từ thư viện đã
 * pre-load sẵn trong RAM (DynamicArrayList songLibrary, nạp lúc server khởi
 * động qua SongLibraryUtils/ServletContext).
 *
 * Hàng chờ phát là một DoublyLinkedList nguyên bản (không HashMap).
 */
public class AudioPlayEngine {
    private Node currentTrackPointer;
    private DoublyLinkedList playlist;
    private boolean isRepeatAllEnabled = false;
    private boolean repeatOne = false;

    // Tham chiếu tới thư viện bài hát trong RAM (dùng cho autoAppendSongs).
    private final DynamicArrayList songLibrary;

    public AudioPlayEngine(DoublyLinkedList playlist, DynamicArrayList songLibrary) {
        this.playlist = playlist;
        this.songLibrary = songLibrary;
        this.currentTrackPointer = playlist.getHead();
    }

    public Song getCurrentSong() {
        return currentTrackPointer != null ? currentTrackPointer.data : null;
    }

    public DoublyLinkedList getPlaylist() {
        return playlist;
    }

    /**
     * Thêm một bài vào CUỐI hàng chờ hiện tại (không tạo hàng chờ mới, không ngắt
     * bài đang phát). append() tự chống trùng theo songId. Nếu hàng chờ đang rỗng
     * (chưa phát gì) thì đặt con trỏ vào bài vừa thêm để nó thành bài hiện tại.
     */
    public Song addToQueue(Song song) {
        if (song == null) return getCurrentSong();
        playlist.append(song);
        if (currentTrackPointer == null) {
            currentTrackPointer = playlist.getHead();
        }
        return getCurrentSong();
    }

    public void playFromSong(Song song) {
        if (song == null) return;
        Node node = playlist.getNodeById(song.getSongId());
        if (node != null) {
            currentTrackPointer = node;
        }
    }

    /**
     * Dời con trỏ phát tới một bài đã có trong hàng chờ (tìm bằng Linear Search).
     */
    public Song jumpTo(int songId) {
        Node node = playlist.getNodeById(songId);
        if (node == null) return null;
        currentTrackPointer = node;
        return node.data;
    }

    /**
     * Xóa một bài khỏi hàng chờ và giữ con trỏ phát hợp lệ.
     */
    public boolean removeSong(int songId) {
        Node node = playlist.getNodeById(songId);
        if (node == null) return false;

        if (currentTrackPointer == node) {
            currentTrackPointer = (node.next != null) ? node.next : node.prev;
        }
        return playlist.removeById(songId);
    }

    public Song nextTrack() {
        if (currentTrackPointer == null) {
            currentTrackPointer = playlist.getHead();
            return currentTrackPointer != null ? currentTrackPointer.data : null;
        }

        if (repeatOne) {
            return currentTrackPointer.data;
        }

        if (currentTrackPointer.next != null) {
            currentTrackPointer = currentTrackPointer.next;
            return currentTrackPointer.data;
        }

        if (isRepeatAllEnabled) {
            currentTrackPointer = playlist.getHead();
            return currentTrackPointer != null ? currentTrackPointer.data : null;
        }

        // Hết hàng chờ → tự nạp thêm 10 bài cùng thể loại TỪ RAM.
        autoAppendSongs();
        if (currentTrackPointer.next != null) {
            currentTrackPointer = currentTrackPointer.next;
            return currentTrackPointer.data;
        }

        return null;
    }

    /**
     * Previous: chỉ dùng con trỏ lùi (prev) của danh sách liên kết đôi để quay về
     * bài ĐỨNG NGAY TRƯỚC trong hàng chờ — KHÔNG dựa vào lịch sử nghe. Nếu đang ở
     * đầu danh sách thì không có bài trước, trả về null.
     */
    public Song previousTrack() {
        if (currentTrackPointer == null || currentTrackPointer.prev == null) {
            return null;
        }
        currentTrackPointer = currentTrackPointer.prev;
        return currentTrackPointer.data;
    }

    public void setRepeatAll(boolean status) {
        this.isRepeatAllEnabled = status;
        if (status) {
            this.repeatOne = false;
        }
    }

    public void setRepeatOne(boolean status) {
        this.repeatOne = status;
        if (status) {
            this.isRepeatAllEnabled = false;
        }
    }

    public boolean isRepeatAllEnabled() {
        return isRepeatAllEnabled;
    }

    public boolean isRepeatOne() {
        return repeatOne;
    }

    public void shuffleUpcoming() {
        if (currentTrackPointer == null || currentTrackPointer.next == null) {
            return;
        }

        DynamicArrayList songs = new DynamicArrayList();
        Node temp = currentTrackPointer.next;
        while (temp != null) {
            songs.add(temp.data);
            temp = temp.next;
        }

        // Fisher–Yates
        Random random = new Random();
        for (int i = songs.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Song tempSong = songs.get(i);
            songs.set(i, songs.get(j));
            songs.set(j, tempSong);
        }

        // Ghi ngược lại vào các node phía sau con trỏ. Không cần dựng lại chỉ mục
        // vì DoublyLinkedList tra cứu bằng Linear Search trên chính data hiện tại.
        temp = currentTrackPointer.next;
        int index = 0;
        while (temp != null) {
            temp.data = songs.get(index);
            temp = temp.next;
            index++;
        }
    }

    /**
     * Lọc 10 bài cùng thể loại với bài hiện tại TỪ THƯ VIỆN TRONG RAM
     * (loại trừ các bài đã có trong hàng chờ) rồi nối vào cuối. KHÔNG chạm DB.
     */
    public void autoAppendSongs() {
        if (currentTrackPointer == null || songLibrary == null) return;

        String genre = currentTrackPointer.data.getGenre();
        if (genre == null) return;

        int added = 0;
        for (int i = 0; i < songLibrary.size() && added < 10; i++) {
            Song song = songLibrary.get(i);
            if (song.getGenre() != null
                    && song.getGenre().equalsIgnoreCase(genre)
                    && !playlist.contains(song.getSongId())) {
                playlist.append(song);
                added++;
            }
        }
    }

    /**
     * Khi phát một bài lẻ (Home / Search): tạo một DoublyLinkedList MỚI chỉ gồm
     * đúng bài đó. Các bài kế tiếp do autoAppendSongs() tự nạp thêm khi hết hàng.
     */
    public static DoublyLinkedList buildQuickPlayQueue(Song selected) {
        DoublyLinkedList list = new DoublyLinkedList();
        list.append(selected);
        return list;
    }
}

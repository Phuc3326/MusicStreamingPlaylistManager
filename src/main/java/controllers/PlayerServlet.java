package controllers;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lib.DoublyLinkedList;
import lib.DynamicArrayList;
import listeners.SongLibraryListener;
import models.Song;
import utils.JsonHelper;

/**
 * Trung tâm điều khiển phát nhạc. KHÔNG truy vấn Database khi phát/nạp bài —
 * mọi bài hát lấy từ thư viện đã pre-load trong RAM (ServletContext).
 */
@WebServlet(name = "PlayerServlet", urlPatterns = {
    "/api/player/play",
    "/api/player/next",
    "/api/player/prev",
    "/api/player/shuffle",
    "/api/player/loop",
    "/api/player/waitlist",
    "/api/player/add",
    "/api/player/remove",
    "/api/player/reorder",
    "/api/player/jump",
    "/api/player/current"
})
public class PlayerServlet extends HttpServlet {

    public static final String SESSION_ENGINE = "playEngine";
    public static final String SESSION_WAITING_LIST = "sessionWaitingList";

    /** Lấy thư viện bài hát đã pre-load trong RAM (ServletContext). */
    private DynamicArrayList getSongLibrary() {
        Object attr = getServletContext().getAttribute(SongLibraryListener.SONG_LIBRARY_ATTR);
        return (attr instanceof DynamicArrayList) ? (DynamicArrayList) attr : null;
    }

    /** Duyệt tuyến tính tìm bài theo id trong thư viện RAM. */
    private Song findSongInLibrary(int songId) {
        DynamicArrayList library = getSongLibrary();
        if (library == null) return null;
        for (int i = 0; i < library.size(); i++) {
            if (library.get(i).getSongId() == songId) {
                return library.get(i);
            }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isLoggedIn(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String path = request.getServletPath();
        HttpSession session = request.getSession();

        try {
            if ("/api/player/current".equals(path)) {
                AudioPlayEngine currentEngine = (AudioPlayEngine) session.getAttribute(SESSION_ENGINE);
                if (currentEngine == null) {
                    out.print("{}");
                } else {
                    writeCurrentResponse(out, currentEngine);
                }
                return;
            }

            AudioPlayEngine engine = getOrCreateEngine(session);

            if ("/api/player/next".equals(path)) {
                Song track = engine.nextTrack();
                writeTrackResponse(out, engine, track);
            } else if ("/api/player/prev".equals(path)) {
                Song track = engine.previousTrack();
                writeTrackResponse(out, engine, track);
            } else if ("/api/player/waitlist".equals(path)) {
                JsonObject root = new JsonObject();
                root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
                out.print(root.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isLoggedIn(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String path = request.getServletPath();
        HttpSession session = request.getSession();

        try {
            if ("/api/player/play".equals(path)) {
                handlePlay(request, session, out);
            } else if ("/api/player/shuffle".equals(path)) {
                AudioPlayEngine engine = getOrCreateEngine(session);
                engine.shuffleUpcoming();
                syncWaitingList(session, engine);
                JsonObject root = new JsonObject();
                root.addProperty("success", true);
                root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
                out.print(root.toString());
            } else if ("/api/player/jump".equals(path)) {
                handleJump(request, session, out);
            } else if ("/api/player/loop".equals(path)) {
                AudioPlayEngine engine = getOrCreateEngine(session);
                String mode = request.getParameter("mode");
                engine.setRepeatAll("all".equals(mode));
                engine.setRepeatOne("one".equals(mode));
                JsonObject root = new JsonObject();
                root.addProperty("success", true);
                out.print(root.toString());
            } else if ("/api/player/add".equals(path)) {
                handleAdd(request, session, out);
            } else if ("/api/player/remove".equals(path)) {
                handleRemove(request, session, out);
            } else if ("/api/player/reorder".equals(path)) {
                handleReorder(request, session, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        } finally {
            out.flush();
        }
    }

    /**
     * Phát một bài lẻ: xóa hàng chờ cũ, tạo DoublyLinkedList MỚI chỉ chứa bài đó.
     * Bài hát lấy từ thư viện RAM, KHÔNG query Database.
     */
    private void handlePlay(HttpServletRequest request, HttpSession session, PrintWriter out)
            throws Exception {
        String songIdParam = request.getParameter("songId");
        if (songIdParam == null || songIdParam.trim().isEmpty()) {
            responseError(out, "Missing songId");
            return;
        }

        int songId = Integer.parseInt(songIdParam);
        Song song = findSongInLibrary(songId);
        if (song == null) {
            responseError(out, "Song not found");
            return;
        }

        DynamicArrayList library = getSongLibrary();
        DoublyLinkedList playlist = AudioPlayEngine.buildQuickPlayQueue(song);

        AudioPlayEngine engine = new AudioPlayEngine(playlist, library);
        engine.playFromSong(song);
        session.setAttribute(SESSION_ENGINE, engine);
        session.setAttribute(SESSION_WAITING_LIST, playlist);

        JsonObject root = new JsonObject();
        root.addProperty("success", true);
        root.add("track", JsonHelper.songToJson(song));
        root.add("waitList", JsonHelper.waitListToJson(playlist));
        out.print(root.toString());
    }

    private AudioPlayEngine getOrCreateEngine(HttpSession session) {
        AudioPlayEngine engine = (AudioPlayEngine) session.getAttribute(SESSION_ENGINE);
        if (engine == null) {
            DoublyLinkedList playlist = new DoublyLinkedList();
            engine = new AudioPlayEngine(playlist, getSongLibrary());
            session.setAttribute(SESSION_ENGINE, engine);
            session.setAttribute(SESSION_WAITING_LIST, playlist);
        }
        return engine;
    }

    private void syncWaitingList(HttpSession session, AudioPlayEngine engine) {
        session.setAttribute(SESSION_WAITING_LIST, engine.getPlaylist());
    }

    private void writeCurrentResponse(PrintWriter out, AudioPlayEngine engine) {
        Song track = engine.getCurrentSong();
        JsonObject root = new JsonObject();
        if (track != null) {
            root.add("track", JsonHelper.songToJson(track));
        }
        root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
        if (engine.isRepeatOne()) {
            root.addProperty("loop", "one");
        } else if (engine.isRepeatAllEnabled()) {
            root.addProperty("loop", "all");
        } else {
            root.addProperty("loop", "off");
        }
        out.print(root.toString());
    }

    private void writeTrackResponse(PrintWriter out, AudioPlayEngine engine, Song track) {
        JsonObject root = new JsonObject();
        if (track != null) {
            root.add("track", JsonHelper.songToJson(track));
        }
        root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
        out.print(root.toString());
    }

    private void responseError(PrintWriter out, String message) {
        JsonObject root = new JsonObject();
        root.addProperty("error", message);
        out.print(root.toString());
    }

    private boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("user") != null;
    }

    /**
     * Thêm một bài vào CUỐI hàng chờ hiện tại (không tạo hàng chờ mới).
     * Nếu chưa có phiên phát thì tạo engine rỗng rồi thêm bài vào.
     */
    private void handleAdd(HttpServletRequest request, HttpSession session, PrintWriter out)
            throws Exception {
        int songId = Integer.parseInt(request.getParameter("songId"));
        Song song = findSongInLibrary(songId);
        AudioPlayEngine engine = getOrCreateEngine(session);

        boolean alreadyInQueue = engine.getPlaylist().contains(songId);
        Song current = engine.addToQueue(song);
        syncWaitingList(session, engine);

        JsonObject root = new JsonObject();
        root.addProperty("success", song != null);
        root.addProperty("alreadyInQueue", alreadyInQueue);
        if (current != null) {
            root.add("track", JsonHelper.songToJson(current));
        }
        root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
        out.print(root.toString());
    }

    private void handleRemove(HttpServletRequest request, HttpSession session, PrintWriter out)
            throws Exception {
        AudioPlayEngine engine = getOrCreateEngine(session);
        int songId = Integer.parseInt(request.getParameter("songId"));

        Song before = engine.getCurrentSong();
        boolean wasCurrent = before != null && before.getSongId() == songId;
        boolean removed = engine.removeSong(songId);
        Song newCurrent = engine.getCurrentSong();

        JsonObject root = new JsonObject();
        root.addProperty("success", removed);
        root.addProperty("removedCurrent", wasCurrent && removed);
        if (wasCurrent && removed && newCurrent != null) {
            root.add("track", JsonHelper.songToJson(newCurrent));
        }
        root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
        out.print(root.toString());
    }

    private void handleJump(HttpServletRequest request, HttpSession session, PrintWriter out)
            throws Exception {
        AudioPlayEngine engine = getOrCreateEngine(session);
        int songId = Integer.parseInt(request.getParameter("songId"));

        Song track = engine.jumpTo(songId);

        JsonObject root = new JsonObject();
        root.addProperty("success", track != null);
        if (track != null) {
            root.add("track", JsonHelper.songToJson(track));
        }
        root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
        out.print(root.toString());
    }

    private void handleReorder(HttpServletRequest request, HttpSession session, PrintWriter out)
            throws Exception {
        AudioPlayEngine engine = getOrCreateEngine(session);
        int songIdToMove = Integer.parseInt(request.getParameter("songIdToMove"));
        int targetSongId = Integer.parseInt(request.getParameter("targetSongId"));

        boolean moved = engine.getPlaylist().moveSongBefore(songIdToMove, targetSongId);

        JsonObject root = new JsonObject();
        root.addProperty("success", moved);
        root.add("waitList", JsonHelper.waitListToJson(engine.getPlaylist()));
        out.print(root.toString());
    }
}

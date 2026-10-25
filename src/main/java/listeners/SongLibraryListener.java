package listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import models.UserDAO;
import utils.SongLibraryUtils;

@WebListener
public class SongLibraryListener implements ServletContextListener {

    public static final String SONG_LIBRARY_ATTR = "songLibrary";
    public static final String SEARCH_ENGINE_ATTR = "songSearchEngine";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 1. Tự động khởi tạo hoặc đồng bộ tài khoản Admin từ biến môi trường
        try {
            new UserDAO().seedAdminFromEnv();
        } catch (Exception e) {
            sce.getServletContext().log("Failed to seed admin account from env: " + e.getMessage());
        }

        // 2. Preload thư viện bài hát vào bộ nhớ
        try {
            SongLibraryUtils.reload(sce.getServletContext());
        } catch (Exception e) {
            sce.getServletContext().log("Failed to preload song library: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        context.removeAttribute(SONG_LIBRARY_ATTR);
        context.removeAttribute(SEARCH_ENGINE_ATTR);
    }
}

import com.formdev.flatlaf.FlatLightLaf;
import config.AppConfig;
import database.DatabaseManager;
import ui.LoginDialog;
import ui.MainFrame;

import javax.swing.*;

public class ClientMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        } catch (Exception ignored) {}

        // Đảm bảo CSDL cục bộ sẵn sàng
        DatabaseManager.getInstance();

        SwingUtilities.invokeLater(() -> launchClient());
    }

    public static void launchClient() {
        AppConfig config = AppConfig.getInstance();

        // Nếu chưa đăng nhập Gmail thật, hiện màn hình Đăng nhập
        if (!config.isLoggedIn()) {
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            if (!login.isLoginSuccessful()) {
                System.out.println("Người dùng đã hủy đăng nhập.");
                return;
            }
        }

        // Mở giao diện Gmail Client
        MainFrame clientFrame = new MainFrame(null, null);
        clientFrame.setVisible(true);
    }
}


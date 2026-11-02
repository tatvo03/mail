import com.formdev.flatlaf.FlatLightLaf;
import config.AppConfig;
import database.DatabaseManager;
import protocol.LocalPop3Server;
import protocol.LocalSmtpServer;
import protocol.NetworkUtils;
import ui.LoginDialog;
import ui.MainFrame;
import ui.ServerFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        } catch (Exception ignored) {}

        // Khởi tạo Database SQLite
        System.out.println("==================================================");
        System.out.println("   HỆ THỐNG GMAIL CLIENT & SERVER ĐA MÁY (JAVA)   ");
        System.out.println("==================================================");
        DatabaseManager.getInstance();

        AppConfig config = AppConfig.getInstance();
        int smtpPort = config.getServerSmtpPort();
        int pop3Port = config.getServerPop3Port();

        System.out.println("[SERVER] Khởi động SMTP Server: " + smtpPort);
        LocalSmtpServer smtpServer = new LocalSmtpServer(smtpPort);
        smtpServer.start();

        System.out.println("[SERVER] Khởi động POP3 Server: " + pop3Port);
        LocalPop3Server pop3Server = new LocalPop3Server(pop3Port);
        pop3Server.start();

        String ip = NetworkUtils.getPrimaryIpAddress();
        System.out.println(">>> ĐỊA CHỈ IP MÁY CHỦ: " + ip);
        System.out.println(">>> Các máy khác trong LAN có thể kết nối tới IP này.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            smtpServer.stop();
            pop3Server.stop();
        }));

        SwingUtilities.invokeLater(() -> {
            // Mở Server Frame để quản trị
            ServerFrame serverFrame = new ServerFrame(smtpServer, pop3Server);
            serverFrame.setVisible(true);

            // Mở Client nếu người dùng muốn
            if (!config.isLoggedIn()) {
                LoginDialog login = new LoginDialog(serverFrame);
                login.setVisible(true);
                if (login.isLoginSuccessful()) {
                    MainFrame clientFrame = new MainFrame(smtpServer, pop3Server);
                    clientFrame.setVisible(true);
                }
            } else {
                MainFrame clientFrame = new MainFrame(smtpServer, pop3Server);
                clientFrame.setVisible(true);
            }
        });
    }
}

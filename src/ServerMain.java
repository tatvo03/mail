import com.formdev.flatlaf.FlatLightLaf;
import config.AppConfig;
import database.DatabaseManager;
import protocol.LocalPop3Server;
import protocol.LocalSmtpServer;
import protocol.NetworkUtils;
import ui.ServerFrame;

import javax.swing.*;

public class ServerMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        } catch (Exception ignored) {}

        // 1. Khởi tạo Database SQLite của Server
        System.out.println("==================================================");
        System.out.println("   MÁY CHỦ MAIL SERVER - GIAO THỨC SMTP & POP3   ");
        System.out.println("==================================================");
        System.out.println("[DB] Đang khởi tạo Database SQLite (mail_server.db)...");
        DatabaseManager.getInstance();

        // 2. Khởi động Local Mail Server
        AppConfig config = AppConfig.getInstance();
        int smtpPort = config.getServerSmtpPort();
        int pop3Port = config.getServerPop3Port();

        System.out.println("[SERVER] Đang khởi động SMTP Server trên cổng: " + smtpPort);
        LocalSmtpServer smtpServer = new LocalSmtpServer(smtpPort);
        smtpServer.start();

        System.out.println("[SERVER] Đang khởi động POP3 Server trên cổng: " + pop3Port);
        LocalPop3Server pop3Server = new LocalPop3Server(pop3Port);
        pop3Server.start();

        String ip = NetworkUtils.getPrimaryIpAddress();
        System.out.println("--------------------------------------------------");
        System.out.println(">>> ĐỊA CHỈ IP MÁY CHỦ CỦA BẠN: " + ip);
        System.out.println(">>> Các máy Client khác hãy kết nối tới IP: " + ip);
        System.out.println("--------------------------------------------------");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Đang tắt Server...");
            smtpServer.stop();
            pop3Server.stop();
        }));

        // 3. Khởi chạy Giao diện Quản trị Server
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame(smtpServer, pop3Server);
            frame.setVisible(true);
        });
    }
}


package config;

import java.io.*;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE = "config.properties";
    private static AppConfig instance;

    // Server IP & Ports (Dành cho Client kết nối tới Server)
    private String serverHost = "localhost";
    private int serverSmtpPort = 2525;
    private int serverPop3Port = 1110;

    // User Credentials (Email thật và App Password)
    private String userEmail = "";
    private String userAppPassword = ""; // 16 ký tự Google App Password
    private String displayName = "Người dùng Gmail";

    // Gmail Direct Relay settings (Gửi thông báo thật qua Google SMTP)
    private String gmailHostSmtp = "smtp.gmail.com";
    private int gmailPortSmtp = 587;
    private String gmailHostPop3 = "pop.gmail.com";
    private int gmailPortPop3 = 995;

    // Server-wide Relay Email (Dành cho Server tự động phát thông báo tới mail thật cho mọi client)
    private String serverRelayEmail = "";
    private String serverRelayAppPassword = "";

    private AppConfig() {
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            serverHost = props.getProperty("server.host", "localhost");
            serverSmtpPort = Integer.parseInt(props.getProperty("server.smtp.port", "2525"));
            serverPop3Port = Integer.parseInt(props.getProperty("server.pop3.port", "1110"));

            userEmail = props.getProperty("user.email", "");
            userAppPassword = props.getProperty("user.app.password", "");
            displayName = props.getProperty("user.display.name", "Người dùng Gmail");

            gmailHostSmtp = props.getProperty("gmail.host.smtp", "smtp.gmail.com");
            gmailPortSmtp = Integer.parseInt(props.getProperty("gmail.port.smtp", "587"));
            gmailHostPop3 = props.getProperty("gmail.host.pop3", "pop.gmail.com");
            gmailPortPop3 = Integer.parseInt(props.getProperty("gmail.port.pop3", "995"));

            serverRelayEmail = props.getProperty("server.relay.email", "");
            serverRelayAppPassword = props.getProperty("server.relay.password", "");
        } catch (Exception e) {
            System.err.println("Không thể đọc file config: " + e.getMessage());
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("server.host", serverHost != null ? serverHost : "localhost");
        props.setProperty("server.smtp.port", String.valueOf(serverSmtpPort));
        props.setProperty("server.pop3.port", String.valueOf(serverPop3Port));

        props.setProperty("user.email", userEmail != null ? userEmail : "");
        props.setProperty("user.app.password", userAppPassword != null ? userAppPassword : "");
        props.setProperty("user.display.name", displayName != null ? displayName : "");

        props.setProperty("gmail.host.smtp", gmailHostSmtp);
        props.setProperty("gmail.port.smtp", String.valueOf(gmailPortSmtp));
        props.setProperty("gmail.host.pop3", gmailHostPop3);
        props.setProperty("gmail.port.pop3", String.valueOf(gmailPortPop3));

        props.setProperty("server.relay.email", serverRelayEmail != null ? serverRelayEmail : "");
        props.setProperty("server.relay.password", serverRelayAppPassword != null ? serverRelayAppPassword : "");

        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Mail Client & Server Configuration");
        } catch (Exception e) {
            System.err.println("Không thể lưu file config: " + e.getMessage());
        }
    }

    public boolean isLoggedIn() {
        return userEmail != null && !userEmail.trim().isEmpty() &&
               userAppPassword != null && !userAppPassword.trim().isEmpty();
    }

    // Getters and Setters
    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }

    public int getServerSmtpPort() { return serverSmtpPort; }
    public void setServerSmtpPort(int serverSmtpPort) { this.serverSmtpPort = serverSmtpPort; }

    public int getServerPop3Port() { return serverPop3Port; }
    public void setServerPop3Port(int serverPop3Port) { this.serverPop3Port = serverPop3Port; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserAppPassword() { return userAppPassword; }
    public void setUserAppPassword(String userAppPassword) { this.userAppPassword = userAppPassword; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getGmailHostSmtp() { return gmailHostSmtp; }
    public int getGmailPortSmtp() { return gmailPortSmtp; }

    public String getGmailHostPop3() { return gmailHostPop3; }
    public int getGmailPortPop3() { return gmailPortPop3; }

    // Backward compatibility helpers
    public int getLocalSmtpPort() { return serverSmtpPort; }
    public void setLocalSmtpPort(int port) { this.serverSmtpPort = port; }
    public int getLocalPop3Port() { return serverPop3Port; }
    public void setLocalPop3Port(int port) { this.serverPop3Port = port; }
    public String getCurrentUserEmail() { return userEmail; }
    public void setCurrentUserEmail(String email) { this.userEmail = email; }
    public String getCurrentDisplayName() { return displayName; }
    public void setCurrentDisplayName(String name) { this.displayName = name; }
    public String getGmailAddress() { return userEmail; }
    public void setGmailAddress(String email) { this.userEmail = email; }
    public String getGmailAppPassword() { return userAppPassword; }
    public void setGmailAppPassword(String pass) { this.userAppPassword = pass; }
    public boolean hasGmailCredentials() { return isLoggedIn(); }
    public String getServerRelayEmail() { return serverRelayEmail; }
    public void setServerRelayEmail(String serverRelayEmail) { this.serverRelayEmail = serverRelayEmail; }

    public String getServerRelayAppPassword() { return serverRelayAppPassword; }
    public void setServerRelayAppPassword(String serverRelayAppPassword) { this.serverRelayAppPassword = serverRelayAppPassword; }

    public boolean hasServerRelayCredentials() {
        return serverRelayEmail != null && !serverRelayEmail.trim().isEmpty() &&
               serverRelayAppPassword != null && !serverRelayAppPassword.trim().isEmpty();
    }

    public String getActiveMode() { return "NETWORK"; }
    public void setActiveMode(String mode) { /* no-op or mode */ }
}

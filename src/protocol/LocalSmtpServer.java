package protocol;

import database.DatabaseManager;
import database.EmailDAO;
import model.EmailMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocalSmtpServer implements Runnable {
    private final int port;
    private final EmailDAO emailDAO;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread serverThread;

    public LocalSmtpServer(int port) {
        this.port = port;
        this.emailDAO = new EmailDAO();
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        serverThread = new Thread(this, "Local-SMTP-Server-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
        System.out.println("[Local SMTP Server] Đang lắng nghe trên cổng " + port);
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
        }
        System.out.println("[Local SMTP Server] Đã dừng máy chủ.");
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            DatabaseManager.getInstance().logServerEvent("SMTP_SERVER", "0.0.0.0:" + port, "START",
                    "Máy chủ Local SMTP Server đã khởi động thành công trên cổng " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new SmtpClientHandler(clientSocket, emailDAO)).start();
                } catch (Exception e) {
                    if (!running) break;
                    System.err.println("[Local SMTP Server] Lỗi accept client: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[Local SMTP Server] Không thể mở cổng " + port + ": " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    private static class SmtpClientHandler implements Runnable {
        private final Socket socket;
        private final EmailDAO emailDAO;
        private final database.UserDAO userDAO;

        public SmtpClientHandler(Socket socket, EmailDAO emailDAO) {
            this.socket = socket;
            this.emailDAO = emailDAO;
            this.userDAO = new database.UserDAO();
        }

        @Override
        public void run() {
            String clientIp = socket.getInetAddress().getHostAddress();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                writer.println("220 Localhost ESMTP Mail Server Ready");

                String sender = "";
                String recipient = "";
                String line;

                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    String upper = trimmed.toUpperCase(Locale.ROOT);

                    if (upper.startsWith("HELO")) {
                        writer.println("250 localhost Hello " + clientIp);
                    } else if (upper.startsWith("EHLO")) {
                        writer.println("250-localhost Hello " + clientIp);
                        writer.println("250-SIZE 10485760");
                        writer.println("250 OK");
                    } else if (upper.startsWith("MAIL FROM:")) {
                        sender = extractAddress(trimmed.substring(10));
                        writer.println("250 2.1.0 Sender <" + sender + "> OK");
                    } else if (upper.startsWith("RCPT TO:")) {
                        recipient = extractAddress(trimmed.substring(8));
                        writer.println("250 2.1.5 Recipient <" + recipient + "> OK");
                    } else if (upper.equals("DATA")) {
                        if (recipient.isEmpty()) {
                            writer.println("503 5.5.1 Error: need RCPT command");
                            continue;
                        }
                        writer.println("354 End data with <CR><LF>.<CR><LF>");

                        StringBuilder dataBuffer = new StringBuilder();
                        String dataLine;
                        while ((dataLine = reader.readLine()) != null) {
                            if (dataLine.equals(".")) {
                                break;
                            }
                            dataBuffer.append(dataLine).append("\n");
                        }

                        // Phân tích RFC Header và Body
                        String fullData = dataBuffer.toString();
                        String subject = "(Không có tiêu đề)";
                        String body = fullData;
                        Date sentDate = new Date();

                        int headerEndIdx = fullData.indexOf("\n\n");
                        if (headerEndIdx != -1) {
                            String headerSection = fullData.substring(0, headerEndIdx);
                            body = fullData.substring(headerEndIdx + 2);

                            String[] headers = headerSection.split("\n");
                            for (String h : headers) {
                                if (h.toLowerCase().startsWith("subject:")) {
                                    subject = h.substring(8).trim();
                                }
                            }
                        }

                        // Lưu vào CSDL Server cho Hộp thư đến (INBOX) của người nhận
                        EmailMessage receivedMsg = new EmailMessage();
                        receivedMsg.setMessageId("smtp-" + System.currentTimeMillis());
                        receivedMsg.setSender(sender.isEmpty() ? "unknown@localhost" : sender);
                        receivedMsg.setRecipient(recipient);
                        receivedMsg.setSubject(subject);
                        receivedMsg.setBody(body);
                        receivedMsg.setSentAt(sentDate);
                        receivedMsg.setRead(false);
                        receivedMsg.setFolder("INBOX");
                        receivedMsg.setProtocol("SMTP_LOCAL");
                        receivedMsg.setOwnerEmail(recipient);

                        // Tự động đăng ký user nếu chưa tồn tại trong Server
                        if (userDAO.getUserByEmail(recipient) == null) {
                            String name = recipient.contains("@") ? recipient.split("@")[0] : recipient;
                            userDAO.createUser(recipient, "123456", name);
                        }

                        emailDAO.insertEmail(receivedMsg);

                        // Tự động phát thông báo tới Gmail thật nếu Server có cấu hình Relay
                        config.AppConfig appCfg = config.AppConfig.getInstance();
                        if (appCfg.hasServerRelayCredentials() && recipient.contains("@")) {
                            final String finalSubj = subject;
                            final String finalBody = body;
                            final String finalRecip = recipient;
                            final String finalSender = sender;
                            new Thread(() -> {
                                try {
                                    new SmtpSender().sendRealGmailNotification(
                                            appCfg.getServerRelayEmail(),
                                            appCfg.getServerRelayAppPassword(),
                                            finalRecip,
                                            finalSubj,
                                            finalBody,
                                            "Máy chủ Mail Server (" + NetworkUtils.getPrimaryIpAddress() + ")"
                                    );
                                } catch (Throwable ex) {
                                    System.err.println("Server Relay Notify error: " + ex.getMessage());
                                }
                            }).start();
                        }

                        DatabaseManager.getInstance().logServerEvent("SMTP_SERVER", clientIp, "DELIVER_OK",
                                "Đã nhận và lưu thư vào CSDL cho " + recipient + " từ " + sender + " | Tiêu đề: " + subject);

                        writer.println("250 2.0.0 OK: queued as " + receivedMsg.getMessageId());
                    } else if (upper.equals("RSET")) {
                        sender = "";
                        recipient = "";
                        writer.println("250 2.0.0 Reset state");
                    } else if (upper.equals("NOOP")) {
                        writer.println("250 2.0.0 OK");
                    } else if (upper.equals("QUIT")) {
                        writer.println("221 2.0.0 Localhost closing connection");
                        break;
                    } else {
                        writer.println("500 5.5.2 Error: command not recognized");
                    }
                }
            } catch (Exception e) {
                // Client disconnected
            } finally {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }

        private String extractAddress(String raw) {
            String trimmed = raw.trim();
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
            return trimmed;
        }
    }
}

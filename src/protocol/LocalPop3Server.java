package protocol;

import database.DatabaseManager;
import database.EmailDAO;
import database.UserDAO;
import model.EmailMessage;
import model.UserAccount;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LocalPop3Server implements Runnable {
    private final int port;
    private final EmailDAO emailDAO;
    private final UserDAO userDAO;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread serverThread;

    public LocalPop3Server(int port) {
        this.port = port;
        this.emailDAO = new EmailDAO();
        this.userDAO = new UserDAO();
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        serverThread = new Thread(this, "Local-POP3-Server-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
        System.out.println("[Local POP3 Server] Đang lắng nghe trên cổng " + port);
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
        }
        System.out.println("[Local POP3 Server] Đã dừng máy chủ.");
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            DatabaseManager.getInstance().logServerEvent("POP3_SERVER", "0.0.0.0:" + port, "START",
                    "Máy chủ Local POP3 Server đã khởi động thành công trên cổng " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new Pop3ClientHandler(clientSocket, emailDAO, userDAO)).start();
                } catch (Exception e) {
                    if (!running) break;
                    System.err.println("[Local POP3 Server] Lỗi accept client: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("[Local POP3 Server] Không thể mở cổng " + port + ": " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    private static class Pop3ClientHandler implements Runnable {
        private final Socket socket;
        private final EmailDAO emailDAO;
        private final UserDAO userDAO;

        private enum State { AUTHORIZATION, TRANSACTION, UPDATE }
        private State state = State.AUTHORIZATION;
        private String currentUser = null;
        private List<EmailMessage> messageList = new ArrayList<>();
        private final Set<Integer> markedForDeletion = new HashSet<>();

        public Pop3ClientHandler(Socket socket, EmailDAO emailDAO, UserDAO userDAO) {
            this.socket = socket;
            this.emailDAO = emailDAO;
            this.userDAO = userDAO;
        }

        @Override
        public void run() {
            String clientIp = socket.getInetAddress().getHostAddress();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                writer.println("+OK Localhost POP3 Server Ready");

                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    String upper = trimmed.toUpperCase(Locale.ROOT);

                    if (state == State.AUTHORIZATION) {
                        if (upper.startsWith("USER ")) {
                            currentUser = trimmed.substring(5).trim();
                            writer.println("+OK User name accepted, please provide password");
                        } else if (upper.startsWith("PASS ")) {
                            String password = trimmed.substring(5).trim();
                            if (currentUser == null) {
                                writer.println("-ERR Send USER first");
                                continue;
                            }

                            // Xác thực tài khoản với CSDL Server
                            boolean authenticated = userDAO.authenticate(currentUser, password);
                            // Nếu user chưa có trong DB Server, tự động tạo tài khoản cho client
                            if (!authenticated) {
                                UserAccount u = userDAO.getUserByEmail(currentUser);
                                if (u == null) {
                                    String displayName = currentUser.contains("@") ? currentUser.split("@")[0] : currentUser;
                                    userDAO.createUser(currentUser, password, displayName);
                                    authenticated = true;
                                } else if (password != null && password.equals(u.getPassword())) {
                                    authenticated = true;
                                }
                            }

                            if (authenticated) {
                                state = State.TRANSACTION;
                                // Tải danh sách thư trong INBOX của user từ SQLite
                                messageList = emailDAO.getEmails(currentUser, "INBOX", null);
                                markedForDeletion.clear();
                                writer.println("+OK Mailbox locked and ready (" + messageList.size() + " messages)");
                                DatabaseManager.getInstance().logServerEvent("POP3_SERVER", clientIp, "AUTH_OK",
                                        "User " + currentUser + " xác thực thành công qua POP3");
                            } else {
                                writer.println("-ERR Authentication failed");
                                DatabaseManager.getInstance().logServerEvent("POP3_SERVER", clientIp, "AUTH_FAIL",
                                        "Đăng nhập thất bại cho user: " + currentUser);
                            }
                        } else if (upper.equals("QUIT")) {
                            writer.println("+OK Bye");
                            break;
                        } else {
                            writer.println("-ERR Command not valid in AUTHORIZATION state");
                        }
                    } else if (state == State.TRANSACTION) {
                        if (upper.equals("STAT")) {
                            int count = 0;
                            long totalSize = 0;
                            for (int i = 0; i < messageList.size(); i++) {
                                if (!markedForDeletion.contains(i)) {
                                    count++;
                                    totalSize += estimateMessageSize(messageList.get(i));
                                }
                            }
                            writer.println("+OK " + count + " " + totalSize);
                        } else if (upper.startsWith("LIST")) {
                            if (upper.equals("LIST")) {
                                int count = 0;
                                long totalSize = 0;
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (!markedForDeletion.contains(i)) {
                                        count++;
                                        totalSize += estimateMessageSize(messageList.get(i));
                                    }
                                }
                                writer.println("+OK " + count + " messages (" + totalSize + " octets)");
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (!markedForDeletion.contains(i)) {
                                        writer.println((i + 1) + " " + estimateMessageSize(messageList.get(i)));
                                    }
                                }
                                writer.println(".");
                            } else {
                                try {
                                    int msgNum = Integer.parseInt(trimmed.substring(4).trim()) - 1;
                                    if (msgNum >= 0 && msgNum < messageList.size() && !markedForDeletion.contains(msgNum)) {
                                        writer.println("+OK " + (msgNum + 1) + " " + estimateMessageSize(messageList.get(msgNum)));
                                    } else {
                                        writer.println("-ERR No such message");
                                    }
                                } catch (NumberFormatException e) {
                                    writer.println("-ERR Invalid message number");
                                }
                            }
                        } else if (upper.startsWith("RETR ")) {
                            try {
                                int msgNum = Integer.parseInt(trimmed.substring(5).trim()) - 1;
                                if (msgNum >= 0 && msgNum < messageList.size() && !markedForDeletion.contains(msgNum)) {
                                    EmailMessage msg = messageList.get(msgNum);
                                    long size = estimateMessageSize(msg);
                                    writer.println("+OK " + size + " octets");

                                    // Render RFC 822 format
                                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
                                    writer.println("From: " + msg.getSender());
                                    writer.println("To: " + msg.getRecipient());
                                    writer.println("Subject: " + msg.getSubject());
                                    writer.println("Date: " + sdf.format(msg.getSentAt() != null ? msg.getSentAt() : new java.util.Date()));
                                    writer.println("Message-ID: <" + msg.getMessageId() + ">");
                                    writer.println("Content-Type: text/plain; charset=UTF-8");
                                    writer.println();
                                    writer.println(msg.getBody());
                                    writer.println(".");
                                } else {
                                    writer.println("-ERR No such message");
                                }
                            } catch (NumberFormatException e) {
                                writer.println("-ERR Invalid message number");
                            }
                        } else if (upper.startsWith("DELE ")) {
                            try {
                                int msgNum = Integer.parseInt(trimmed.substring(5).trim()) - 1;
                                if (msgNum >= 0 && msgNum < messageList.size() && !markedForDeletion.contains(msgNum)) {
                                    markedForDeletion.add(msgNum);
                                    writer.println("+OK Message " + (msgNum + 1) + " marked for deletion");
                                } else {
                                    writer.println("-ERR No such message");
                                }
                            } catch (NumberFormatException e) {
                                writer.println("-ERR Invalid message number");
                            }
                        } else if (upper.equals("NOOP")) {
                            writer.println("+OK");
                        } else if (upper.equals("RSET")) {
                            markedForDeletion.clear();
                            writer.println("+OK Mailbox reset");
                        } else if (upper.equals("QUIT")) {
                            state = State.UPDATE;
                            // Xóa thư đã đánh dấu DELE
                            for (int idx : markedForDeletion) {
                                if (idx >= 0 && idx < messageList.size()) {
                                    emailDAO.moveToFolder(messageList.get(idx).getId(), "TRASH");
                                }
                            }
                            writer.println("+OK POP3 server signing off");
                            break;
                        } else {
                            writer.println("-ERR Unknown command");
                        }
                    }
                }
            } catch (Exception e) {
                // Client closed
            } finally {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }

        private long estimateMessageSize(EmailMessage msg) {
            int bodyLen = msg.getBody() != null ? msg.getBody().getBytes(StandardCharsets.UTF_8).length : 0;
            int subjLen = msg.getSubject() != null ? msg.getSubject().getBytes(StandardCharsets.UTF_8).length : 0;
            return bodyLen + subjLen + 200; // rough RFC header estimate
        }
    }
}


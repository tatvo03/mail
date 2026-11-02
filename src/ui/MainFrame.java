package ui;

import config.AppConfig;
import database.DatabaseManager;
import database.EmailDAO;
import model.EmailMessage;
import protocol.LocalPop3Server;
import protocol.LocalSmtpServer;
import protocol.Pop3Receiver;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MainFrame extends JFrame {
    private final EmailDAO emailDAO;
    private final LocalSmtpServer smtpServer;
    private final LocalPop3Server pop3Server;
    private final Pop3Receiver pop3Receiver;

    // State
    private String currentFolder = "INBOX"; // INBOX, SENT, TRASH
    private List<EmailMessage> currentEmailList;
    private EmailMessage selectedEmail;

    // UI Components
    private JLabel lblUserBadge;
    private JLabel lblServerStatus;
    private JTextField txtSearch;
    private JButton btnInbox;
    private JButton btnSent;
    private JButton btnTrash;
    private JLabel lblInboxBadge;

    private JTable tblEmails;
    private DefaultTableModel tableModel;
    private JLabel lblFolderTitle;
    private JButton btnRefresh;
    private JButton btnMarkRead;
    private JButton btnDelete;

    // Detail Panel Components
    private JPanel detailPanel;
    private JLabel lblDetailSubject;
    private JLabel lblDetailSender;
    private JLabel lblDetailRecipient;
    private JLabel lblDetailDate;
    private JLabel lblDetailProtocol;
    private JTextArea txtDetailBody;
    private JPanel detailContentCard;
    private JLabel lblEmptyPlaceholder;

    public MainFrame(LocalSmtpServer smtpServer, LocalPop3Server pop3Server) {
        super("Gmail - Hệ thống Mail Client & Server (Java SMTP/POP3)");
        this.smtpServer = smtpServer;
        this.pop3Server = pop3Server;
        this.emailDAO = new EmailDAO();
        this.pop3Receiver = new Pop3Receiver();

        initComponents();
        loadEmails();
        updateUnreadBadge();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 1. TOP HEADER (Phong cách Gmail)
        JPanel topHeader = createTopHeader();
        add(topHeader, BorderLayout.NORTH);

        // 2. MAIN SPLIT (Sidebar bên trái, Nội dung bên phải)
        JPanel sidebar = createSidebar();

        // Email List & Detail SplitPane
        JSplitPane contentSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        contentSplit.setDividerLocation(520);
        contentSplit.setContinuousLayout(true);
        contentSplit.setDividerSize(6);

        JPanel listPanel = createEmailListPanel();
        detailPanel = createEmailDetailPanel();

        contentSplit.setLeftComponent(listPanel);
        contentSplit.setRightComponent(detailPanel);

        // Sidebar + Content Panel
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(sidebar, BorderLayout.WEST);
        centerContainer.add(contentSplit, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(225, 227, 230)),
                new EmptyBorder(10, 20, 10, 20)
        ));

        // Brand Logo phong cách Gmail
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel("✉");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblLogo.setForeground(new Color(234, 67, 53)); // Gmail Red

        JLabel lblBrandName = new JLabel("Gmail");
        lblBrandName.setFont(new Font("Product Sans", Font.BOLD, 22));
        lblBrandName.setForeground(new Color(95, 99, 104));

        JLabel lblTag = new JLabel(" Java Client & Server");
        lblTag.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTag.setForeground(new Color(128, 134, 139));

        brandPanel.add(lblLogo);
        brandPanel.add(lblBrandName);
        brandPanel.add(lblTag);
        header.add(brandPanel, BorderLayout.WEST);

        // Search Bar phong cách Google
        JPanel searchContainer = new JPanel(new BorderLayout(8, 0));
        searchContainer.setOpaque(false);
        searchContainer.setPreferredSize(new Dimension(460, 40));

        JPanel searchBox = new JPanel(new BorderLayout(8, 0));
        searchBox.setBackground(new Color(241, 243, 244));
        searchBox.setBorder(new EmptyBorder(4, 15, 4, 15));

        JLabel lblSearchIcon = new JLabel("🔍");
        lblSearchIcon.setForeground(Color.GRAY);
        searchBox.add(lblSearchIcon, BorderLayout.WEST);

        txtSearch = new JTextField();
        txtSearch.setBorder(null);
        txtSearch.setOpaque(false);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setToolTipText("Tìm kiếm theo người gửi, người nhận, tiêu đề hoặc nội dung");
        txtSearch.addActionListener(e -> loadEmails());
        searchBox.add(txtSearch, BorderLayout.CENTER);

        JButton btnClearSearch = new JButton("✕");
        btnClearSearch.setBorder(null);
        btnClearSearch.setContentAreaFilled(false);
        btnClearSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearSearch.addActionListener(e -> {
            txtSearch.setText("");
            loadEmails();
        });
        searchBox.add(btnClearSearch, BorderLayout.EAST);

        searchContainer.add(searchBox, BorderLayout.CENTER);
        header.add(searchContainer, BorderLayout.CENTER);

        // Right Actions & Profile
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        // Server Status Chip
        AppConfig cfg = AppConfig.getInstance();
        lblServerStatus = new JLabel("🌐 Server: " + cfg.getServerHost() + ":" + cfg.getServerSmtpPort());
        lblServerStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblServerStatus.setForeground(new Color(30, 130, 60));
        lblServerStatus.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 230, 200)),
                new EmptyBorder(4, 8, 4, 8)
        ));
        rightActions.add(lblServerStatus);

        // Switch Account / Logout Button
        JButton btnSwitchUser = new JButton("Đổi tài khoản");
        btnSwitchUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSwitchUser.addActionListener(e -> {
            LoginDialog login = new LoginDialog(this);
            login.setVisible(true);
            if (login.isLoginSuccessful()) {
                updateUserBadgeDisplay();
                lblServerStatus.setText("🌐 Server: " + AppConfig.getInstance().getServerHost() + ":" + AppConfig.getInstance().getServerSmtpPort());
                loadEmails();
            }
        });
        rightActions.add(btnSwitchUser);

        // Database Viewer Button
        JButton btnDb = new JButton("🗄️ Database Server");
        btnDb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDb.setBackground(new Color(245, 247, 250));
        btnDb.setToolTipText("Xem trực tiếp Cơ sở dữ liệu SQLite của Server");
        btnDb.addActionListener(e -> new DatabaseViewerDialog(this).setVisible(true));
        rightActions.add(btnDb);

        // Settings Button
        JButton btnSettings = new JButton("⚙️ Cài đặt");
        btnSettings.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSettings.addActionListener(e -> new SettingsDialog(this, this::onConfigUpdated).setVisible(true));
        rightActions.add(btnSettings);

        // User Profile Pill
        lblUserBadge = new JLabel();
        lblUserBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUserBadge.setForeground(new Color(60, 64, 67));
        updateUserBadgeDisplay();
        rightActions.add(lblUserBadge);

        header.add(rightActions, BorderLayout.EAST);

        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 600));
        sidebar.setBackground(new Color(246, 248, 252));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, new Color(225, 227, 230)));

        JPanel topSide = new JPanel();
        topSide.setLayout(new BoxLayout(topSide, BoxLayout.Y_AXIS));
        topSide.setOpaque(false);
        topSide.setBorder(new EmptyBorder(15, 12, 10, 12));

        // Nút "+ Soạn thư" Floating Action Pill phong cách Gmail
        JButton btnCompose = new JButton("+  Soạn thư");
        btnCompose.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCompose.setBackground(new Color(194, 231, 255)); // Gmail Light Blue Compose
        btnCompose.setForeground(new Color(0, 29, 53));
        btnCompose.setFocusPainted(false);
        btnCompose.setBorder(new EmptyBorder(12, 24, 12, 24));
        btnCompose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCompose.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnCompose.setMaximumSize(new Dimension(190, 50));
        btnCompose.addActionListener(e -> openComposeDialog());
        topSide.add(btnCompose);

        topSide.add(Box.createVerticalStrut(20));

        // Folder Menu Buttons
        btnInbox = createSidebarMenuButton("📥  Hộp thư đến", "INBOX");
        lblInboxBadge = new JLabel("0");
        lblInboxBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblInboxBadge.setForeground(new Color(60, 64, 67));

        JPanel inboxRow = new JPanel(new BorderLayout());
        inboxRow.setOpaque(false);
        inboxRow.setMaximumSize(new Dimension(200, 36));
        inboxRow.add(btnInbox, BorderLayout.CENTER);
        inboxRow.add(lblInboxBadge, BorderLayout.EAST);
        topSide.add(inboxRow);

        topSide.add(Box.createVerticalStrut(4));

        btnSent = createSidebarMenuButton("📤  Thư đã gửi", "SENT");
        topSide.add(btnSent);

        topSide.add(Box.createVerticalStrut(4));

        btnTrash = createSidebarMenuButton("🗑️  Thùng rác", "TRASH");
        topSide.add(btnTrash);

        sidebar.add(topSide, BorderLayout.NORTH);

        // Sidebar Bottom: Server Info Box
        JPanel bottomSide = new JPanel();
        bottomSide.setLayout(new BoxLayout(bottomSide, BoxLayout.Y_AXIS));
        bottomSide.setOpaque(false);
        bottomSide.setBorder(new EmptyBorder(10, 12, 15, 12));

        JLabel lblServerBoxTitle = new JLabel("Máy chủ Localhost");
        lblServerBoxTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblServerBoxTitle.setForeground(new Color(90, 95, 100));
        bottomSide.add(lblServerBoxTitle);

        bottomSide.add(Box.createVerticalStrut(6));

        JLabel lblPorts = new JLabel("• SMTP Port: " + AppConfig.getInstance().getLocalSmtpPort() + "\n• POP3 Port: " + AppConfig.getInstance().getLocalPop3Port());
        lblPorts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPorts.setForeground(new Color(110, 115, 120));
        bottomSide.add(lblPorts);

        bottomSide.add(Box.createVerticalStrut(10));

        JButton btnToggleServer = new JButton("Khởi động lại Server");
        btnToggleServer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnToggleServer.setMaximumSize(new Dimension(190, 28));
        btnToggleServer.addActionListener(e -> restartLocalServer());
        bottomSide.add(btnToggleServer);

        sidebar.add(bottomSide, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createSidebarMenuButton(String title, String folderKey) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(200, 36));

        if ("INBOX".equals(folderKey)) {
            btn.setOpaque(true);
            btn.setBackground(new Color(211, 227, 253)); // Active background
            btn.setForeground(new Color(4, 30, 66));
        } else {
            btn.setForeground(new Color(68, 71, 70));
        }

        btn.addActionListener(e -> switchFolder(folderKey));
        return btn;
    }

    private JPanel createEmailListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // List Action Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);

        lblFolderTitle = new JLabel("Hộp thư đến");
        lblFolderTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblFolderTitle.setForeground(new Color(30, 30, 30));
        leftTools.add(lblFolderTitle);

        toolbar.add(leftTools, BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightTools.setOpaque(false);

        btnRefresh = new JButton("🔄 Nhận mail (POP3)");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setToolTipText("Lấy thư mới qua giao thức POP3 (từ Gmail hoặc Localhost)");
        btnRefresh.addActionListener(e -> handleFetchPop3());
        rightTools.add(btnRefresh);

        btnMarkRead = new JButton("✉ Đã đọc");
        btnMarkRead.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnMarkRead.addActionListener(e -> handleMarkRead());
        rightTools.add(btnMarkRead);

        btnDelete = new JButton("🗑 Xóa");
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDelete.addActionListener(e -> handleDeleteEmail());
        rightTools.add(btnDelete);

        toolbar.add(rightTools, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        // Table List
        String[] cols = {"⭐", "Đối tác", "Tiêu đề & Tóm tắt", "Giao thức", "Thời gian"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblEmails = new JTable(tableModel);
        tblEmails.setRowHeight(38);
        tblEmails.setShowGrid(false);
        tblEmails.setIntercellSpacing(new Dimension(0, 0));
        tblEmails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEmails.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblEmails.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblEmails.getTableHeader().setBackground(new Color(248, 249, 250));

        // Column widths
        tblEmails.getColumnModel().getColumn(0).setPreferredWidth(35);
        tblEmails.getColumnModel().getColumn(0).setMaxWidth(40);
        tblEmails.getColumnModel().getColumn(1).setPreferredWidth(130);
        tblEmails.getColumnModel().getColumn(2).setPreferredWidth(220);
        tblEmails.getColumnModel().getColumn(3).setPreferredWidth(75);
        tblEmails.getColumnModel().getColumn(4).setPreferredWidth(60);

        // Custom Renderer to bold unread emails like Gmail
        tblEmails.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (currentEmailList != null && row >= 0 && row < currentEmailList.size()) {
                    EmailMessage msg = currentEmailList.get(row);
                    if (!msg.isRead() && "INBOX".equals(currentFolder)) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        if (!isSelected) c.setBackground(new Color(242, 246, 252));
                    } else {
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                        if (!isSelected) c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        // Click row event
        tblEmails.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = tblEmails.getSelectedRow();
                if (selectedRow >= 0 && currentEmailList != null && selectedRow < currentEmailList.size()) {
                    showEmailDetail(currentEmailList.get(selectedRow));
                }
            }
        });

        // Star toggle on column 0 click
        tblEmails.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblEmails.rowAtPoint(e.getPoint());
                int col = tblEmails.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 0 && currentEmailList != null && row < currentEmailList.size()) {
                    EmailMessage msg = currentEmailList.get(row);
                    emailDAO.toggleStar(msg.getId(), msg.isStarred());
                    msg.setStarred(!msg.isStarred());
                    tableModel.setValueAt(msg.isStarred() ? "★" : "☆", row, 0);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblEmails);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEmailDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new MatteBorder(0, 1, 0, 0, new Color(225, 227, 230)));

        // Empty placeholder
        lblEmptyPlaceholder = new JLabel("Chọn một thư bên trái để đọc nội dung", SwingConstants.CENTER);
        lblEmptyPlaceholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblEmptyPlaceholder.setForeground(new Color(150, 150, 150));
        panel.add(lblEmptyPlaceholder, BorderLayout.CENTER);

        // Detail Content Card (Initially hidden)
        detailContentCard = new JPanel(new BorderLayout(0, 10));
        detailContentCard.setBackground(Color.WHITE);
        detailContentCard.setBorder(new EmptyBorder(15, 20, 15, 20));
        detailContentCard.setVisible(false);

        // Header Section
        JPanel headerSec = new JPanel();
        headerSec.setLayout(new BoxLayout(headerSec, BoxLayout.Y_AXIS));
        headerSec.setOpaque(false);

        lblDetailSubject = new JLabel("Tiêu đề thư");
        lblDetailSubject.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblDetailSubject.setForeground(new Color(32, 33, 36));
        lblDetailSubject.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerSec.add(lblDetailSubject);

        headerSec.add(Box.createVerticalStrut(10));

        // Meta Info Panel
        JPanel metaPanel = new JPanel(new BorderLayout(10, 0));
        metaPanel.setOpaque(false);
        metaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel senderBox = new JPanel();
        senderBox.setLayout(new BoxLayout(senderBox, BoxLayout.Y_AXIS));
        senderBox.setOpaque(false);

        lblDetailSender = new JLabel("Từ: Sender");
        lblDetailSender.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblDetailSender.setForeground(new Color(32, 33, 36));

        lblDetailRecipient = new JLabel("Đến: Recipient");
        lblDetailRecipient.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDetailRecipient.setForeground(new Color(95, 99, 104));

        senderBox.add(lblDetailSender);
        senderBox.add(lblDetailRecipient);
        metaPanel.add(senderBox, BorderLayout.WEST);

        JPanel rightMeta = new JPanel();
        rightMeta.setLayout(new BoxLayout(rightMeta, BoxLayout.Y_AXIS));
        rightMeta.setOpaque(false);

        lblDetailDate = new JLabel("12:00 01/01/2026");
        lblDetailDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDetailDate.setForeground(new Color(95, 99, 104));
        lblDetailDate.setAlignmentX(Component.RIGHT_ALIGNMENT);

        lblDetailProtocol = new JLabel("[SMTP_GMAIL]");
        lblDetailProtocol.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDetailProtocol.setForeground(new Color(24, 90, 188));
        lblDetailProtocol.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightMeta.add(lblDetailDate);
        rightMeta.add(lblDetailProtocol);
        metaPanel.add(rightMeta, BorderLayout.EAST);

        headerSec.add(metaPanel);
        headerSec.add(Box.createVerticalStrut(12));
        headerSec.add(new JSeparator(SwingConstants.HORIZONTAL));

        detailContentCard.add(headerSec, BorderLayout.NORTH);

        // Body Text
        txtDetailBody = new JTextArea();
        txtDetailBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDetailBody.setLineWrap(true);
        txtDetailBody.setWrapStyleWord(true);
        txtDetailBody.setEditable(false);
        txtDetailBody.setBackground(Color.WHITE);
        txtDetailBody.setBorder(new EmptyBorder(10, 5, 10, 5));

        JScrollPane bodyScroll = new JScrollPane(txtDetailBody);
        bodyScroll.setBorder(null);
        detailContentCard.add(bodyScroll, BorderLayout.CENTER);

        // Bottom Action Bar (Reply, Forward)
        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        detailActions.setOpaque(false);
        detailActions.setBorder(new MatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnReply = new JButton("↩ Trả lời");
        btnReply.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReply.addActionListener(e -> handleReply());
        detailActions.add(btnReply);

        JButton btnForward = new JButton("↪ Chuyển tiếp");
        btnForward.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnForward.addActionListener(e -> handleForward());
        detailActions.add(btnForward);

        JButton btnTrashThis = new JButton("🗑 Xóa thư này");
        btnTrashThis.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnTrashThis.addActionListener(e -> handleDeleteEmail());
        detailActions.add(btnTrashThis);

        detailContentCard.add(detailActions, BorderLayout.SOUTH);

        panel.add(detailContentCard, BorderLayout.CENTER);

        return panel;
    }

    private void showEmailDetail(EmailMessage msg) {
        this.selectedEmail = msg;
        lblEmptyPlaceholder.setVisible(false);
        detailContentCard.setVisible(true);

        lblDetailSubject.setText(msg.getSubject());
        lblDetailSender.setText("Từ: " + msg.getSender());
        lblDetailRecipient.setText("Đến: " + msg.getRecipient());
        lblDetailDate.setText(msg.getFormattedDate());
        lblDetailProtocol.setText("[" + msg.getProtocol() + "]");
        txtDetailBody.setText(msg.getBody());
        txtDetailBody.setCaretPosition(0);

        // If unread, mark as read
        if (!msg.isRead()) {
            emailDAO.markAsRead(msg.getId(), true);
            msg.setRead(true);
            updateUnreadBadge();
            tblEmails.repaint();
        }
    }

    private void switchFolder(String folder) {
        this.currentFolder = folder;
        selectedEmail = null;
        detailContentCard.setVisible(false);
        lblEmptyPlaceholder.setVisible(true);

        // Update button visual styles
        resetSidebarButtonStyles();
        if ("INBOX".equals(folder)) {
            btnInbox.setOpaque(true);
            btnInbox.setBackground(new Color(211, 227, 253));
            lblFolderTitle.setText("Hộp thư đến");
        } else if ("SENT".equals(folder)) {
            btnSent.setOpaque(true);
            btnSent.setBackground(new Color(211, 227, 253));
            lblFolderTitle.setText("Thư đã gửi");
        } else if ("TRASH".equals(folder)) {
            btnTrash.setOpaque(true);
            btnTrash.setBackground(new Color(211, 227, 253));
            lblFolderTitle.setText("Thùng rác");
        }

        loadEmails();
    }

    private void resetSidebarButtonStyles() {
        btnInbox.setOpaque(false);
        btnInbox.setBackground(null);
        btnSent.setOpaque(false);
        btnSent.setBackground(null);
        btnTrash.setOpaque(false);
        btnTrash.setBackground(null);
    }

    public void loadEmails() {
        String owner = AppConfig.getInstance().getCurrentUserEmail();
        String search = txtSearch.getText().trim();

        currentEmailList = emailDAO.getEmails(owner, currentFolder, search);
        tableModel.setRowCount(0);

        for (EmailMessage m : currentEmailList) {
            String partner = "SENT".equals(currentFolder) ? "Đến: " + m.getRecipient() : m.getSender();
            String star = m.isStarred() ? "★" : "☆";
            String preview = m.getSubject() + " - " + m.getSnippet(40);
            String protoShort = m.getProtocol().replace("SMTP_", "").replace("POP3_", "");

            tableModel.addRow(new Object[]{
                    star,
                    partner,
                    preview,
                    protoShort,
                    m.getShortDate()
            });
        }
        updateUnreadBadge();
    }

    private void updateUnreadBadge() {
        String owner = AppConfig.getInstance().getCurrentUserEmail();
        int unread = emailDAO.getUnreadCount(owner);
        lblInboxBadge.setText(unread > 0 ? String.valueOf(unread) : "");
        lblInboxBadge.setVisible(unread > 0);
    }

    private void updateUserBadgeDisplay() {
        AppConfig config = AppConfig.getInstance();
        lblUserBadge.setText("👤 " + config.getCurrentDisplayName() + " <" + config.getCurrentUserEmail() + ">");
    }

    private void openComposeDialog() {
        ComposeDialog compose = new ComposeDialog(this, this::loadEmails);
        compose.setVisible(true);
    }

    private void handleFetchPop3() {
        AppConfig config = AppConfig.getInstance();

        btnRefresh.setEnabled(false);
        btnRefresh.setText("Đang tải thư từ Server...");

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return pop3Receiver.fetchFromServer(
                        config.getServerHost(),
                        config.getServerPop3Port(),
                        config.getUserEmail(),
                        config.getUserAppPassword()
                );
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                btnRefresh.setText("🔄 Nhận mail (POP3)");
                try {
                    int count = get();
                    loadEmails();
                    String msg = "✓ Đã kết nối POP3 tới Máy chủ (" + config.getServerHost() + ":" + config.getServerPop3Port() + ") thành công!\n" +
                                 "Tải về: " + count + " tin nhắn mới vào Hộp thư đến.";
                    JOptionPane.showMessageDialog(MainFrame.this, msg, "Kết quả tải POP3", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Lỗi tải thư qua POP3 từ Server " + config.getServerHost() + ":\n" + cause.getMessage(),
                            "Lỗi POP3", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void handleMarkRead() {
        int row = tblEmails.getSelectedRow();
        if (row >= 0 && currentEmailList != null && row < currentEmailList.size()) {
            EmailMessage msg = currentEmailList.get(row);
            emailDAO.markAsRead(msg.getId(), true);
            msg.setRead(true);
            updateUnreadBadge();
            tblEmails.repaint();
        }
    }

    private void handleDeleteEmail() {
        int row = tblEmails.getSelectedRow();
        if (row >= 0 && currentEmailList != null && row < currentEmailList.size()) {
            EmailMessage msg = currentEmailList.get(row);
            if ("TRASH".equals(currentFolder)) {
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa vĩnh viễn thư này?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    emailDAO.deletePermanently(msg.getId());
                    loadEmails();
                    detailContentCard.setVisible(false);
                    lblEmptyPlaceholder.setVisible(true);
                }
            } else {
                emailDAO.moveToFolder(msg.getId(), "TRASH");
                loadEmails();
                detailContentCard.setVisible(false);
                lblEmptyPlaceholder.setVisible(true);
            }
        }
    }

    private void handleReply() {
        if (selectedEmail == null) return;
        String to = selectedEmail.getSender();
        String subj = selectedEmail.getSubject();
        if (!subj.toLowerCase().startsWith("re:")) {
            subj = "Re: " + subj;
        }
        String quote = "\n\n--- Thư gốc từ " + selectedEmail.getSender() + " (" + selectedEmail.getFormattedDate() + ") ---\n" + selectedEmail.getBody();
        ComposeDialog compose = new ComposeDialog(this, this::loadEmails, to, subj, quote);
        compose.setVisible(true);
    }

    private void handleForward() {
        if (selectedEmail == null) return;
        String subj = selectedEmail.getSubject();
        if (!subj.toLowerCase().startsWith("fwd:")) {
            subj = "Fwd: " + subj;
        }
        String quote = "\n\n--- Thư chuyển tiếp từ " + selectedEmail.getSender() + " (" + selectedEmail.getFormattedDate() + ") ---\n" + selectedEmail.getBody();
        ComposeDialog compose = new ComposeDialog(this, this::loadEmails, "", subj, quote);
        compose.setVisible(true);
    }

    private void restartLocalServer() {
        if (smtpServer != null && pop3Server != null) {
            smtpServer.stop();
            pop3Server.stop();
            smtpServer.start();
            pop3Server.start();
            JOptionPane.showMessageDialog(this, "Đã khởi động lại máy chủ Server cục bộ!", "Server Status", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Ứng dụng này đang hoạt động như một Client độc lập kết nối tới Máy chủ tại: " + AppConfig.getInstance().getServerHost(), "Thông tin Server", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onConfigUpdated() {
        updateUserBadgeDisplay();
        loadEmails();
    }
}

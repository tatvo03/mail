# HỆ THỐNG MAIL SERVER & MULTI-CLIENT (GMAIL THẬT - ĐA MÁY)

Dự án Java hoàn chỉnh cho phép **1 Máy chủ (Server)** và **Nhiều Máy khách (Clients)** chạy trên các máy tính khác nhau trong mạng (Wi-Fi / LAN), sử dụng **tài khoản Gmail thật** để đăng nhập, gửi tin nhắn cho nhau và **người nhận sẽ nhận được thông báo ở hộp thư Gmail thật trên điện thoại / máy tính!**

---

## 🌟 TÍNH NĂNG NỔI BẬT ĐÁP ỨNG ĐÚNG YÊU CẦU

1. **Mô hình 1 Server - Nhiều Client chạy trên các máy khác nhau**:
   - **Máy 1 (Server)**: Chạy `run_server.bat` - Khởi động máy chủ Socket SMTP (cổng `2525`) và POP3 (cổng `1110`). Server tự động phát hiện và hiển thị địa chỉ IP LAN (ví dụ: `192.168.1.15`).
   - **Máy 2, 3... (Client)**: Chạy `run_client.bat` - Kết nối tới IP của Server.
2. **Đăng nhập bằng Gmail thật**:
   - Mỗi Client nhập địa chỉ **Gmail thật** (ví dụ: `nguyenvana@gmail.com`) và **Mật khẩu ứng dụng Google (16 ký tự)**.
   - Tài khoản được tự động đồng bộ và quản lý trong CSDL Server (`mail_server.db`).
3. **Gửi tin nhắn cho nhau qua Server**:
   - Client A (`userA@gmail.com`) gửi tin nhắn cho Client B (`userB@gmail.com`).
   - Tin nhắn đi qua giao thức SMTP Socket tới Server và lưu vào CSDL SQLite của Server.
4. **🔔 Người nhận nhận được thông báo ở Gmail thật (Ting Ting trên điện thoại)**:
   - Khi gửi tin nhắn, hệ thống **đồng thời phát một email thông báo thật sự qua Google SMTP (`smtp.gmail.com:587`) tới hòm thư Gmail thật của người nhận**.
   - Người nhận ngay lập tức thấy thông báo thư mới từ ứng dụng Gmail thật trên điện thoại hoặc trình duyệt!
   - Người nhận mở ứng dụng Client trên máy của mình, nhấn "Nhận mail (POP3)" -> tải toàn bộ nội dung tin nhắn về Hộp thư đến (Inbox) từ Server.

---

## 📁 CẤU TRÚC THƯ MỤC DỰ ÁN

```
java-mail-system/
├── lib/                             # Thư viện ngoài (đã có sẵn)
│   ├── javax.mail-1.6.2.jar         # JavaMail API (Google SMTP & POP3)
│   ├── sqlite-jdbc-3.36.0.3.jar     # SQLite Driver (Lưu trữ CSDL Server)
│   └── flatlaf-3.4.1.jar            # Giao diện hiện đại phong cách Gmail
├── src/
│   ├── ServerMain.java              # Điểm chạy riêng cho Máy chủ Server
│   ├── ClientMain.java              # Điểm chạy riêng cho Máy khách Client
│   ├── Main.java                    # Chạy cả Server và Client trên 1 máy
│   ├── protocol/
│   │   ├── LocalSmtpServer.java     # Socket Server SMTP (:2525, RFC 5321)
│   │   ├── LocalPop3Server.java     # Socket Server POP3 (:1110, RFC 1939)
│   │   ├── SmtpSender.java          # Gửi mail qua Server & phát thông báo Gmail
│   │   ├── Pop3Receiver.java        # Tải mail từ Server qua POP3
│   │   └── NetworkUtils.java        # Tự động quét và lấy địa chỉ IP LAN
│   ├── database/
│   │   ├── DatabaseManager.java     # SQLite mail_server.db & ghi logs
│   │   ├── EmailDAO.java            # Quản lý bảng emails
│   │   └── UserDAO.java             # Quản lý bảng users
│   ├── ui/
│   │   ├── ServerFrame.java         # Giao diện Quản trị Server & Xem Logs
│   │   ├── LoginDialog.java         # Màn hình Đăng nhập bằng Gmail thật
│   │   ├── MainFrame.java           # Giao diện Client tương tự Gmail
│   │   ├── ComposeDialog.java       # Hộp thoại Soạn thư
│   │   └── DatabaseViewerDialog.java # Trình quản lý CSDL của Server
├── run_server.bat                   # File khởi chạy Server (Máy 1)
├── run_client.bat                   # File khởi chạy Client (Máy 2, 3...)
├── run.bat                          # File menu khởi chạy linh hoạt
└── build.bat                        # File biên dịch nhanh toàn bộ mã nguồn
```

---

## 🔑 CÁCH LẤY MẬT KHẨU ỨNG DỤNG GOOGLE (APP PASSWORD 16 KÝ TỰ)

Để gửi và nhận thông báo email thật, Google yêu cầu tạo Mật khẩu ứng dụng 16 ký tự:

1. Đăng nhập tài khoản Gmail trên trình duyệt và vào:  
   👉 [https://myaccount.google.com/security](https://myaccount.google.com/security)
2. Bật **"Xác minh 2 bước"** (2-Step Verification) nếu tài khoản chưa bật.
3. Truy cập link tạo Mật khẩu ứng dụng:  
   👉 [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
4. Nhập tên ứng dụng (ví dụ: `MailApp`), bấm **Tạo** (Create).
5. Google sẽ cấp cho bạn một chuỗi **16 chữ cái** (ví dụ: `abcd efgh ijkl mnop`). Hãy sao chép 16 chữ này để dán vào ô Mật khẩu ứng dụng khi đăng nhập Client.

---

## 🚀 HƯỚNG DẪN CHẠY TRÊN NHIỀU MÁY (MULTI-COMPUTER)

### Bước 1: Khởi động Server trên Máy 1
1. Trên Máy 1, nhấp đúp vào [`run_server.bat`](file:///C:/Users/LENOVO/.gemini/antigravity/scratch/java-mail-system/run_server.bat).
2. Cửa sổ **Mail Server Manager** xuất hiện:
   - Dòng trên cùng hiển thị địa chỉ IP của Server, ví dụ:  
     `🌐 ĐỊA CHỈ IP MÁY CHỦ: 192.168.1.15`
   - Bảng **Nhật ký Socket & Giao dịch** sẵn sàng ghi lại mọi tương tác.

### Bước 2: Khởi động Client trên Máy 2 (hoặc Máy 3)
1. Sao chép thư mục `java-mail-system` sang Máy 2 (hoặc Máy 2 kết nối chung mạng Wi-Fi với Máy 1).
2. Nhấp đúp vào [`run_client.bat`](file:///C:/Users/LENOVO/.gemini/antigravity/scratch/java-mail-system/run_client.bat).
3. Màn hình Đăng nhập hiện ra:
   - **Gmail thật**: Nhập email của bạn (ví dụ: `userA@gmail.com`).
   - **Mật khẩu ứng dụng**: Dán 16 ký tự Google cấp.
   - **Tên hiển thị**: Nhập tên của bạn (ví dụ: `Nguyễn Văn A`).
   - **Địa chỉ IP của Mail Server**: Nhập đúng địa chỉ IP của Máy 1 (ví dụ: `192.168.1.15`). *(Nếu chạy cùng trên 1 máy thì để `localhost`)*.
   - Bấm **"Đăng nhập & Vào ứng dụng ➔"**.

---

## 🎯 KỊCH BẢN DEMO THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN

### Kịch bản: Máy 2 gửi tin nhắn cho Máy 3 -> Máy 3 nhận thông báo trên điện thoại thật
1. Trên Máy 2 (đang đăng nhập `userA@gmail.com`):
   - Nhấn nút **"+ Soạn thư"**.
   - **Người nhận (To)**: Nhập Gmail thật của Máy 3 (ví dụ: `userB@gmail.com`).
   - **Tiêu đề**: *"Họp đồ án Mạng máy tính"*.
   - **Nội dung**: *"Chào bạn, tin nhắn này gửi qua Server và báo về Gmail thật!"*.
   - Đảm bảo ô **"🔔 Gửi kèm thông báo đến hộp thư Gmail thật của người nhận"** đang được tích chọn.
   - Bấm **"Gửi tin nhắn ✈"**.

2. **Kiểm tra kết quả**:
   - **Tại hộp thư Gmail thật của `userB@gmail.com` (trên điện thoại hoặc trình duyệt)**: Điện thoại lập tức rung/reo chuông thông báo thư mới từ Google với tiêu đề `[Thông báo Mail Server] Họp đồ án Mạng máy tính`!
   - **Tại màn hình Server (Máy 1)**: Bảng nhật ký Logs ghi lại ngay lập tức:
     `[SMTP_SERVER] Received mail from userA@gmail.com to userB@gmail.com`
     và thư được lưu vào CSDL SQLite của Server.
   - **Tại Máy 3 (đang mở Client của `userB@gmail.com`)**: Bấm nút **"🔄 Nhận mail (POP3)"**, tin nhắn sẽ hiện ra ngay trong Hộp thư đến (Inbox)!

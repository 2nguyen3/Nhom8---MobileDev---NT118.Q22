# Nhom8---MobileDev---NT118.Q22
Ứng dụng chăm sóc sức khỏe tinh thần và kết nối cộng đồng ẩn danh

# Nhóm 8 - NT118.Q22 | Hệ thống hỗ trợ cân bằng cảm xúc
Ứng dụng chăm sóc sức khỏe tinh thần và kết nối cộng đồng ẩn danh trên Android, tập trung vào các bài toán kỹ thuật:
- **AI On-device (TensorFlow Lite)**: nhận diện cảm xúc từ khuôn mặt (offline, bảo vệ quyền riêng tư)
- **Multimedia + Foreground Service**: phát nhạc trị liệu/podcast/white noise chạy nền, tắt màn hình vẫn hoạt động
- **Real-time Networking (Socket.IO)**: chat ẩn danh theo tâm trạng, độ trễ thấp
- **Scheduler (AlarmManager + Deep Link)**: nhắc nhở/báo thức trị liệu chính xác kể cả khi app bị kill
- **Analytics**: nhật ký tâm trạng + biểu đồ theo tuần/tháng
- (Optional) **Relax Map (Google Maps + Location)**: gợi ý “góc yên tĩnh” gần người dùng

> Lưu ý: Ứng dụng **không phải công cụ chẩn đoán y khoa**. Mục tiêu là hỗ trợ người dùng check-in cảm xúc, thư giãn và kết nối an toàn.

---

## Môn học
**Phát triển ứng dụng trên thiết bị di động - NT118.Q22**

## Thành viên nhóm 8
| STT | Họ và tên | MSSV | Vai trò |
|---:|---|---:|---|
| 1 | Nguyễn Trần Thảo Nguyên | 23521052 | Nhóm trưởng |
| 2 | Nguyễn Thúy Ngân | 23520996 | Thành viên |
| 3 | Lê Đào Anh Thư | 23521537 | Thành viên |

---

## Tổng quan chức năng hệ thống

### 1) Thấu hiểu & Phân tích (Input)
- **Check-in cảm xúc (AI Camera)**: quét khuôn mặt để nhận diện cảm xúc (Vui/Buồn/Căng thẳng/...)
- **Mood Diary**: ghi chú nhanh + chọn icon cảm xúc + nguyên nhân (tạo dữ liệu lịch sử)

### 2) Chữa lành & Cân bằng (Action)
- **Gợi ý trị liệu thông minh**: đề xuất nhạc/bài tập/podcast theo kết quả AI hoặc mood diary
- **Healing Audio Hub + Player**: binaural beats / white noise / podcast (hỗ trợ chạy nền)
- **Breathing Exercise**: bài tập hít thở theo nhịp (hình ảnh/âm thanh)
- **Therapy Scheduler**: nhắc nhở & báo thức (uống nước, đi ngủ, thư giãn)

### 3) Kết nối & Chia sẻ (Community)
- **Anonymous Chat (1-1 ngẫu nhiên)**: kết nối theo tâm trạng, nhắn tin thời gian thực, ẩn danh

### 4) Tiện ích mở rộng (Utility)
- **Relax Map**: tìm công viên/quán cafe/quán sách yên tĩnh gần bạn (optional)
- **Analytics Dashboard**: biểu đồ biến động tâm lý theo tuần/tháng
- **Cài đặt**: Dark Mode, tùy chỉnh thông báo

---

## Danh sách chức năng (tường minh)
- Đăng ký / Đăng nhập / Quên mật khẩu (Email)
- Hồ sơ cá nhân: Avatar, Biệt danh (Nickname), Câu châm ngôn yêu thích
- Check-in cảm xúc (AI Camera): nhận diện Vui/Buồn/Căng thẳng
- Gợi ý trị liệu thông minh (Smart Recommendation)
- Trình phát nhạc trị liệu (Player): binaural/podcast/white noise (chạy nền)
- Nhật ký tâm trạng (Mood Diary)
- Biểu đồ thống kê theo tuần/tháng
- Kết nối ẩn danh (Anonymous Chat): 1-1 ngẫu nhiên theo tâm trạng
- Bài tập hít thở (Breathing)
- Nhắc nhở & Báo thức (Alarm/Scheduler)
- Bản đồ thư giãn (Relax Map) (optional)
- Cài đặt hệ thống: Dark Mode, tùy chỉnh thông báo

---

## Core Technical Highlights (điểm nhấn kỹ thuật)

### AI On-device: Emotion Recognition
- Dataset: **FER-2013**
- Training: CNN với TensorFlow (Python)
- Deploy: convert sang **TensorFlow Lite (.tflite)** để chạy offline
- Flow: Camera → Face Detect → Crop → TFLite Inference → Emotion Result → Recommendation

### Multimedia (Foreground Service)
- Audio player chạy nền ổn định (tắt màn hình vẫn nghe)
- Media controls (Notification / tai nghe)
- Buffering tối ưu khi mạng yếu

### Real-time Chat (Socket.IO)
- NodeJS + Socket.IO server
- Android client nhận/gửi tin nhắn realtime
- Tối ưu tránh ANR khi nhiều message (queue/batch UI updates)

### Scheduler (AlarmManager + Deep Link)
- Alarm hoạt động chính xác theo giờ hệ thống, kể cả khi app bị kill
- Deep link mở đúng màn hình bài tập/bản nhạc trị liệu

---

## Tech Stack
- Android: **Java**, Android Studio
- Architecture: MVVM (ViewModel + LiveData) hoặc MVP (tùy triển khai)
- Local DB: Room (SQLite)
- AI: TensorFlow / TensorFlow Lite
- Audio: ExoPlayer + Foreground Service
- Realtime: NodeJS + Socket.IO
- Map (optional): Google Maps SDK + Location Service

---

## Project Structure

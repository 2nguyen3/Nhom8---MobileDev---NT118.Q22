# BT02 — Debugging Demo (MoodMusic)

Môn: **Phát triển ứng dụng trên thiết bị di động — NT118.Q22**  
Nhóm: **Nhóm 8**  
Ngôn ngữ: **Java (Android Studio)**  
Nhánh: **BT02**

## 1. Mục tiêu bài tập
- Tạo một project Android nhỏ để **demo quy trình debug khi có lỗi**.
- Có giao diện đơn giản, dễ trình bày, đủ “đất” để tạo bug và sửa bug.
- Chuẩn bị slide thuyết trình (có nói sơ về **cấu trúc thư mục**).

## 2. Giới thiệu project: MoodMusic
**MoodMusic** là app phát nhạc theo cảm xúc:
- Người dùng chọn 1 trong 3 cảm xúc (emoji): **Vui / Buồn / Căng thẳng**
- App đổi mood (animation) và **phát nhạc** tương ứng
- Có nút **Dừng nhạc**

### Tính năng hiện có (bản chạy đúng)
- UI theo phong cách pastel + wave
- Chọn mood bằng emoji: `emoSad`, `emoHappy`, `emoStress`
- Nhãn hiển thị mood: `tvMoodLabel`
- Phát nhạc theo mood: `happy.mp3`, `sad.mp3`, `stress.mp3` (trong `res/raw`)
- Dừng nhạc: `btnStop`

## 3. Quy ước ID quan trọng (không đổi)
Để tránh lỗi khi ghép và đúng yêu cầu bài:
- Emoji: `emoSad`, `emoHappy`, `emoStress`
- Label: `tvMoodLabel`
- Nút dừng: `btnStop`

Ngoài ra có các ID tương thích (placeholder) để không vỡ merge:
- `btnHappy`, `btnSad`, `btnStress` (ẩn)

## 4. Cấu trúc thư mục chính (tóm tắt)
- `app/src/main/java/com/example/moodmusic/MainActivity.java`  
  Xử lý UI interaction + animation + MediaPlayer
- `app/src/main/res/layout/activity_main.xml`  
  Giao diện chính
- `app/src/main/res/raw/`  
  Chứa file nhạc: `happy.mp3`, `sad.mp3`, `stress.mp3`
- `app/src/main/res/values/strings.xml`  
  Chuỗi text 
- `app/src/main/AndroidManifest.xml`  
  Cấu hình app

## 5. Cách chạy project
1. Mở project bằng Android Studio
2. Sync Gradle (nếu được yêu cầu)
3. Chọn emulator / device
4. Run ▶️

## 6. Kịch bản Debug 
> Nhóm sẽ chuẩn bị **2 phiên bản**:  
> - **Bản lỗi (bug)** để demo crash/logic bug  
> - **Bản fix** để chứng minh cách sửa đúng

### Bug 1 — Crash (NullPointerException)
- Mô tả: cố tình “quên” ánh xạ 1 view nhưng vẫn dùng nó
- Hiện tượng: bấm emoji → app crash
- Debug: đọc Logcat, xác định dòng NPE, thêm lại `findViewById`

### Bug 2 — Logic bug (nhạc chồng + nhạc không tắt)
- Mô tả:
  - Quên `stopMusic()` trước khi phát bài mới → nhạc bị chồng
  - Không xử lý vòng đời (`onStop`) → thoát app nhạc vẫn phát
- Debug: tìm logic sai, thêm `stopMusic()` đúng chỗ và xử lý lifecycle

## 7. Thành viên nhóm
- **Nhóm trưởng:** Nguyễn Trần Thảo Nguyên - 23521052
- **Thành viên:** Nguyễn Thúy Ngân — 23520996  
- **Thành viên:** Lê Đào Anh Thư — 23521537

---
> Branch này phục vụ cho **BT02 (Debugging demo)**.  

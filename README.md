# VnNews - Ứng Dụng Đọc Báo Việt Nam (VnExpress Reader)

Ứng dụng đọc tin tức hiện đại viết bằng **Jetpack Compose**, đáp ứng 100% các tiêu chí kỹ thuật từ **Unit 4 đến Unit 8** của giáo trình **Android Basics with Compose (Google)**.

---

## 📌 Bảng Đối Chiếu Kiến Thức Giáo Trình (Checklist Chấm Điểm)

| Khối kiến thức (Unit) | Công nghệ sử dụng | Thành phần & File trong Project |
| :--- | :--- | :--- |
| **Unit 4: Architecture & Navigation** | • Mô hình **MVVM** + **Repository Pattern**<br>• Quản lý UI State bằng **StateFlow** (UDF)<br>• **Jetpack Navigation Compose** | • `ui/navigation/NewsNavGraph.kt`<br>• `ui/home/HomeViewModel.kt`<br>• `data/repository/NewsRepository.kt` |
| **Unit 5: Connect to Internet & Images** | • **Retrofit 2** + Gson Converter<br>• Gọi API RSS VnExpress qua `rss2json`<br>• **Coil (`AsyncImage`)** tải và cache ảnh | • `data/remote/NewsApiService.kt`<br>• `data/remote/NewsResponseDto.kt`<br>• `ui/home/HomeScreen.kt` |
| **Unit 6: Data Persistence** | • **Room Database** (Entity, DAO, Flow)<br>• **DataStore Preferences** lưu cấu hình người dùng | • `data/local/ArticleEntity.kt`<br>• `data/local/ArticleDao.kt`<br>• `data/local/AppDatabase.kt`<br>• `data/local/UserPreferencesRepository.kt` |
| **Unit 7: WorkManager** | • **PeriodicWorkRequest** chạy tác vụ nền<br>• Ràng buộc kết nối mạng (`Constraints`)<br>• Phát **Notification** thông báo tin nóng | • `worker/DailyNewsNotificationWorker.kt`<br>• Khởi tạo tại `NewsApplication.kt` |
| **Unit 8: Views in Compose (Interop)** | • Nhúng **`WebView`** truyền thống vào Compose bằng **`AndroidView`** | • `ui/detail/ArticleWebView.kt`<br>• `ui/detail/DetailScreen.kt` |

---

## 🚀 Các Tính Năng Chính
1. **Duyệt tin đa chuyên mục:** Mới nhất, Công nghệ (Số hóa), Kinh doanh, Thể thao, Giải trí.
2. **Đọc báo gốc trong ứng dụng:** Bấm vào bất kỳ tin nào để mở trang web chính thức của VnExpress qua `AndroidView(WebView)`.
3. **Lưu tin đọc Offline (Room Database):** Bấm biểu tượng Bookmark để lưu bài báo vào máy, xem lại mọi lúc kể cả khi ngắt kết nối mạng.
4. **Thông báo tin mới định kỳ (WorkManager):** Tự động kiểm tra và thông báo tin mới mỗi 12 tiếng.

---

## 🛠 Hướng Dẫn Cài Đặt & Chạy
1. Mở thư mục dự án bằng **Android Studio Ladybug / Meerkat**.
2. Chờ Gradle đồng bộ (Sync Project with Gradle Files).
3. Chọn máy ảo (Emulator Android 8.0+ / API 26+) hoặc máy thật cắm cáp USB.
4. Nhấn **Run 'app'** (`Shift + F10`).
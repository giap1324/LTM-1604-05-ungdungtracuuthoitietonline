# Ứng dụng Tra cứu Thời tiết

<div align="center">
    <h2>
        <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
            🎓 Khoa Công nghệ Thông tin - Đại học Đại Nam
        </a>
    </h2>
    
    <p>
        <img src="docs/aiotlab_logo.png" alt="Logo AIoTLab" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="Logo FIT DNU" width="180"/>
        <img src="docs/dnu_logo.png" alt="Logo Đại học Đại Nam" width="200"/>
    </p>
    
    [![AIoTLab](https://img.shields.io/badge/AIoTLab-28a745?style=for-the-badge&logo=gitlab&logoColor=white)](https://www.facebook.com/DNUAIoTLab)
    [![Khoa Công nghệ Thông tin](https://img.shields.io/badge/Khoa%20CNTT-0066cc?style=for-the-badge&logo=university&logoColor=white)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
    [![Đại học Đại Nam](https://img.shields.io/badge/Đại%20học%20Đại%20Nam-ff6600?style=for-the-badge&logo=university&logoColor=white)](https://dainam.edu.vn)
    
    <p><strong>Hệ thống Thông tin Thời tiết Trực tuyến</strong></p>
    
    ![Phiên bản](https://img.shields.io/badge/phiên%20bản-1.0.0-blue.svg)
    ![Java](https://img.shields.io/badge/Java-17+-orange.svg)
    ![Giấy phép](https://img.shields.io/badge/giấy%20phép-MIT-green.svg)
    
</div>

---

## 📋 Mục lục

- [Tổng quan](#-tổng-quan)
- [Tính năng nổi bật](#-tính-năng-nổi-bật)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
- [Hướng dẫn sử dụng](#-hướng-dẫn-sử-dụng)
- [Tài liệu API](#-tài-liệu-api)
- [Hình ảnh minh họa](#-hình-ảnh-minh-họa)
- [Liên hệ](#-liên-hệ)

---

## 🌟 Tổng quan

**Ứng dụng Tra cứu Thời tiết** là một hệ thống client-server toàn diện được thiết kế để cung cấp thông tin thời tiết theo thời gian thực với khả năng lưu trữ dữ liệu bền vững. Dự án giáo dục này minh họa các khái niệm cơ bản trong lập trình mạng, tích hợp API và quản lý cơ sở dữ liệu.

### Mục tiêu dự án

- **Truy cập dữ liệu thời gian thực**: Tích hợp với WeatherAPI để lấy thông tin thời tiết hiện tại
- **Giao diện thân thiện**: Cung cấp chức năng tra cứu thời tiết trực quan
- **Lưu trữ dữ liệu**: Duy trì lịch sử tìm kiếm bằng cơ sở dữ liệu SQLite
- **Giá trị giáo dục**: Phục vụ như một ví dụ thực tế cho các khái niệm lập trình mạng
- **Khả năng mở rộng**: Hỗ trợ nhiều người dùng và thành phố đồng thời

---

## ✨ Tính năng nổi bật

### 🔍 Chức năng cốt lõi
- **Tra cứu thời tiết theo thành phố**: Tìm kiếm thông tin thời tiết bằng tên thành phố
- **Dữ liệu thời gian thực**: Nhiệt độ, độ ẩm và điều kiện thời tiết hiện tại
- **Hỗ trợ đa ngôn ngữ**: Giao diện tiếng Việt và tiếng Anh
- **Thiết kế responsive**: Tối ưu hóa cho nhiều kích thước màn hình

### 📊 Quản lý dữ liệu
- **Lịch sử tìm kiếm**: Lưu trữ bền vững tất cả truy vấn thời tiết
- **Tích hợp SQLite**: Giải pháp cơ sở dữ liệu nhúng nhẹ
- **Phân tích dữ liệu**: Xem mẫu tìm kiếm và thành phố phổ biến
- **Chức năng xuất**: Xuất lịch sử tìm kiếm sang định dạng CSV

### 🛡️ Tính năng hệ thống
- **Xử lý lỗi**: Quản lý lỗi toàn diện và phản hồi người dùng
- **Kiểm tra đầu vào**: Làm sạch đầu vào người dùng và phản hồi API
- **Quản lý kết nối**: Giao tiếp client-server mạnh mẽ
- **Tối ưu hóa hiệu suất**: Bộ nhớ đệm và truy xuất dữ liệu hiệu quả

---

## 🔧 Công nghệ sử dụng

<div align="center">

| Công nghệ | Mục đích | Phiên bản |
|-----------|----------|-----------|
| [![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/) | Phát triển Backend | 17+ |
| [![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/) | Quản lý Cơ sở dữ liệu | 3.40+ |
| [![WeatherAPI](https://img.shields.io/badge/WeatherAPI-00A1F1?style=for-the-badge&logo=cloud&logoColor=white)](https://www.weatherapi.com/) | Nguồn dữ liệu thời tiết | v1 |
| [![Swing](https://img.shields.io/badge/Java%20Swing-ED8B00?style=for-the-badge&logo=java&logoColor=white)]() | Framework GUI | Tích hợp sẵn |
| [![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/) | Công cụ Build | 3.8+ |

</div>

### Thư viện bổ sung
- **Xử lý JSON**: org.json để xử lý phản hồi API
- **HTTP Client**: Java 11+ HTTP Client cho yêu cầu API
- **Logging**: java.util.logging để giám sát hệ thống
- **Testing**: JUnit 5 cho unit testing

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────┐    HTTP/HTTPS    ┌─────────────────┐    API Calls    ┌─────────────────┐
│                 │   ◄──────────►   │                 │   ◄──────────►  │                 │
│  Client GUI     │                  │  Ứng dụng       │                 │  Dịch vụ        │
│  (Java Swing)   │                  │  Server         │                 │  WeatherAPI     │
│                 │                  │                 │                 │                 │
└─────────────────┘                  └─────────────────┘                 └─────────────────┘
                                              │
                                              │ JDBC
                                              ▼
                                     ┌─────────────────┐
                                     │                 │
                                     │  Cơ sở dữ liệu  │
                                     │  SQLite         │
                                     │                 │
                                     └─────────────────┘
```

### Mô tả thành phần

- **Ứng dụng Client**: GUI dựa trên Java Swing cho tương tác người dùng
- **Ứng dụng Server**: Server đa luồng xử lý các yêu cầu của client
- **Lớp cơ sở dữ liệu**: SQLite để lưu trữ dữ liệu bền vững
- **API bên ngoài**: Tích hợp WeatherAPI cho dữ liệu thời gian thực

---

## 📦 Hướng dẫn cài đặt

### Yêu cầu hệ thống

```bash
# Kiểm tra cài đặt Java
java --version  # Phải là 17 trở lên
javac --version

# Kiểm tra cài đặt Maven (tùy chọn)
mvn --version
```

### Hướng dẫn từng bước

1. **Clone Repository**
   ```bash
   git clone https://github.com/your-username/weather-lookup-app.git
   cd weather-lookup-app
   ```

2. **Cấu hình API Key**
   ```bash
   # Tạo file config
   cp config.example.properties config.properties
   
   # Chỉnh sửa config.properties và thêm WeatherAPI key của bạn
   WEATHER_API_KEY=your_api_key_here
   WEATHER_API_URL=http://api.weatherapi.com/v1
   ```

3. **Biên dịch ứng dụng**
   ```bash
   # Sử dụng Maven
   mvn clean compile
   
   # Hoặc sử dụng javac trực tiếp
   javac -cp "lib/*" src/**/*.java -d build/
   ```

4. **Khởi tạo cơ sở dữ liệu**
   ```bash
   # Chạy script thiết lập cơ sở dữ liệu
   java -cp "build:lib/*" com.weatherapp.DatabaseSetup
   ```

### Tùy chọn cấu hình

Tạo file `application.properties`:

```properties
# Cấu hình Server
server.port=8080
server.max.connections=100

# Cấu hình Database
database.path=data/weather.db
database.connection.timeout=30

# Cấu hình API
weather.api.key=YOUR_API_KEY
weather.api.timeout=5000
weather.cache.duration=300
```

---

## 🚀 Hướng dẫn sử dụng

### Khởi động Server

```bash
# Phương pháp 1: Sử dụng Maven
mvn exec:java -Dexec.mainClass="com.weatherapp.server.WeatherServer"

# Phương pháp 2: Sử dụng Java trực tiếp
java -cp "build:lib/*" com.weatherapp.server.WeatherServer

# Server sẽ khởi động trên cổng 8080 theo mặc định
```

### Chạy ứng dụng Client

```bash
# Phương pháp 1: Sử dụng Maven
mvn exec:java -Dexec.mainClass="com.weatherapp.client.WeatherClient"

# Phương pháp 2: Sử dụng Java trực tiếp
java -cp "build:lib/*" com.weatherapp.client.WeatherClient
```

### Các thao tác cơ bản

1. **Tìm kiếm thông tin thời tiết**
   - Nhập tên thành phố vào ô tìm kiếm
   - Nhấn nút "Xem thời tiết"
   - Xem điều kiện thời tiết hiện tại

2. **Xem lịch sử tìm kiếm**
   - Chuyển đến tab "Lịch sử"
   - Duyệt các tìm kiếm trước đó
   - Xuất dữ liệu nếu cần

3. **Chức năng quản trị**
   - Truy cập bảng quản trị với thông tin đăng nhập
   - Xem thống kê hệ thống
   - Quản lý dữ liệu người dùng

---

## 📚 Tài liệu API

### Các endpoint API nội bộ

| Endpoint | Phương thức | Mô tả | Tham số |
|----------|-------------|-------|---------|
| `/weather` | GET | Lấy thời tiết theo thành phố | `city` (chuỗi) |
| `/history` | GET | Truy xuất lịch sử tìm kiếm | `limit` (int), `offset` (int) |
| `/stats` | GET | Thống kê hệ thống | Không |

### Ví dụ Request/Response

**Lấy thông tin thời tiết:**
```json
// Request
GET /weather?city=Hanoi

// Response
{
    "status": "success",
    "data": {
        "city": "Hà Nội",
        "country": "Việt Nam",
        "temperature": 28.5,
        "humidity": 75,
        "condition": "Có mây rải rác",
        "timestamp": "2024-03-15T10:30:00Z"
    }
}
```

---

## 📸 Hình ảnh minh họa

### Giao diện chính của ứng dụng
![Giao diện chính](docs/screenshots/main-interface.png)

### Hiển thị thời tiết
![Hiển thị thời tiết](docs/screenshots/weather-display.png)

### Lịch sử tìm kiếm
![Lịch sử tìm kiếm](docs/screenshots/search-history.png)

### Bảng quản trị
![Bảng quản trị](docs/screenshots/admin-panel.png)

---

## 🤝 Đóng góp

Chúng tôi hoan nghênh sự đóng góp từ cộng đồng! Vui lòng tuân theo các hướng dẫn sau:

### Quy trình phát triển

1. **Fork repository**
2. **Tạo nhánh tính năng**
   ```bash
   git checkout -b feature/tinh-nang-tuyet-voi
   ```

3. **Thực hiện thay đổi**
4. **Chạy tests**
   ```bash
   mvn test
   ```

5. **Commit thay đổi**
   ```bash
   git commit -m "Thêm tính năng tuyệt vời"
   ```

6. **Push lên nhánh của bạn**
   ```bash
   git push origin feature/tinh-nang-tuyet-voi
   ```

7. **Mở Pull Request**

### Tiêu chuẩn code

- Tuân theo quy ước đặt tên Java
- Viết unit tests toàn diện
- Tài liệu hóa các phương thức public bằng JavaDoc
- Duy trì code coverage trên 80%

---

## 📄 Giấy phép

Dự án này được cấp phép theo MIT License - xem file [LICENSE.md](LICENSE.md) để biết thêm chi tiết.

```
MIT License

Copyright (c) 2024 Đại học Đại Nam - Khoa Công nghệ Thông tin

Quyền được cấp miễn phí cho bất kỳ ai có được bản sao
của phần mềm này và các file tài liệu liên quan (gọi là "Phần mềm"), được phép
xử lý Phần mềm mà không bị hạn chế...
```

---

## 📞 Liên hệ

<div align="center">

### Người bảo trì dự án

**Nguyễn Nguyễn**  
📧 **Email**: [nguyennguyenvh09@gmail.com](mailto:nguyennguyenvh09@gmail.com)  
🌐 **Trường**: [Khoa Công nghệ Thông tin](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)  
🏫 **Cơ sở**: [Đại học Đại Nam](https://dainam.edu.vn)

### Liên kết học thuật

[![AIoTLab](https://img.shields.io/badge/AIoTLab-Facebook-1877f2?style=flat-square&logo=facebook)](https://www.facebook.com/DNUAIoTLab)
[![Đại học](https://img.shields.io/badge/Đại%20học-Đại%20Nam-orange?style=flat-square&logo=university)](https://dainam.edu.vn)

</div>

---

<div align="center">
    
**Được tạo với ❤️ bởi Khoa Công nghệ Thông tin**  
*Đại học Đại Nam - AIoTLab*

![Footer](https://img.shields.io/badge/Được%20xây%20dựng%20với-Java%20%26%20Đam%20mê-red?style=for-the-badge)

</div>
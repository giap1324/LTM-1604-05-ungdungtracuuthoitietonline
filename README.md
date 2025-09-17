<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
        🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>

<h2 align="center">
    Ứng dụng tra cứu thời tiết online
</h2>

<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="FIT Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

## 1. Giới thiệu hệ thống

Hệ thống **Ứng dụng tra cứu thời tiết trực tuyến** được xây dựng theo mô hình **Client-Server** sử dụng Java nhằm:

- Hỗ trợ người dùng tra cứu thông tin thời tiết (nhiệt độ, độ ẩm, tốc độ gió, mô tả thời tiết) theo thành phố
- Cung cấp dữ liệu thời tiết theo thời gian thực từ API công khai (WeatherAPI.com)
- Giao diện đồ họa thân thiện với người dùng sử dụng Java Swing
- Hỗ trợ kết nối đồng thời nhiều client thông qua Thread Pool
- Xử lý lỗi và phản hồi một cách chuyên nghiệp

### 👉 **Điểm nổi bật**:
- Người dùng có thể nhập tên thành phố và nhận thông tin thời tiết ngay lập tức
- Hỗ trợ nhiều thành phố trên toàn thế giới, dữ liệu cập nhật theo thời gian thực
- Giao diện client hiện đại với các thông báo trạng thái rõ ràng
- Mock data tích hợp sẵn cho việc test khi chưa có API key
- Log hoạt động chi tiết giúp theo dõi quá trình giao tiếp

## 🔧 2. Công nghệ & Ngôn ngữ sử dụng

[![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![Swing GUI](https://img.shields.io/badge/Swing_GUI-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![WeatherAPI](https://img.shields.io/badge/WeatherAPI-00A1F1?style=for-the-badge&logo=cloud&logoColor=white)](https://www.weatherapi.com/)
[![Socket Programming](https://img.shields.io/badge/Socket_Programming-FF6B35?style=for-the-badge&logo=network&logoColor=white)]()

### Chi tiết công nghệ:
- **Java SE 11+**: Ngôn ngữ lập trình chính
- **Java Swing**: Xây dựng giao diện người dùng
- **Socket Programming**: Giao tiếp Client-Server qua TCP
- **HTTP Client**: Kết nối với WeatherAPI.com
- **JSON Parsing**: Xử lý dữ liệu JSON từ API (custom parser)
- **Multithreading**: ExecutorService cho xử lý đồng thời
- **WeatherAPI.com**: Nguồn dữ liệu thời tiết

## 📡 3. Giao thức giao tiếp

Hệ thống sử dụng giao thức tùy chỉnh qua TCP socket:

### Connection Handshake:
```
Client -> Server: TCP Connection
Server -> Client: "WEATHER_SERVER_CONNECTED"
Server -> Client: "Chào mừng đến Weather Server (WeatherAPI.com)!"
```

### Weather Request:
```
Client -> Server: "WEATHER:<city_name>"
Server -> Client: "SUCCESS:<weather_data>" hoặc "ERROR:<error_message>"
```

### Ping Test:
```
Client -> Server: "PING"
Server -> Client: "PONG"
```

### Weather Data Format:
```
"CITY:<city>,<country>|TEMP:<temperature>|DESC:<description>|HUMIDITY:<humidity>|WIND:<wind_speed>"
```

## 🚀 4. Các bước cài đặt

### Yêu cầu hệ thống:
- Java Development Kit (JDK) 11 trở lên
- IDE: Eclipse, IntelliJ IDEA, hoặc VS Code
- Kết nối internet (để truy cập WeatherAPI)

### Bước 1: Clone dự án
```bash
git clone [repository-url]
cd WeatherApp
```

### Bước 2: Cấu hình API Key
1. Đăng ký tài khoản tại [WeatherAPI.com](https://www.weatherapi.com/)
2. Lấy API key miễn phí
3. Mở file `Server.java` và thay thế:
```java
private static final String API_KEY = "YOUR_API_KEY_HERE";
```

### Bước 3: Biên dịch dự án
```bash
# Biên dịch tất cả các file Java
javac -d bin src/WeatherApp/*.java

# Hoặc sử dụng IDE để build project
```

### Bước 4: Chạy ứng dụng

#### Chạy Server trước:
```bash
java -cp bin WeatherApp.Server
```

#### Sau đó chạy Client:
```bash
java -cp bin WeatherApp.Client
```

### Bước 5: Sử dụng ứng dụng
1. Nhấn nút "Kết nối" trên client
2. Nhập tên thành phố vào ô text
3. Nhấn "Tra cứu thời tiết" hoặc Enter
4. Xem kết quả hiển thị

## 📁 5. Cấu trúc dự án

```
WeatherApp/
├── src/
│   └── WeatherApp/
│       ├── Client.java           # GUI Client application
│       ├── Server.java           # Multi-threaded server
│       ├── WeatherProtocol.java  # Protocol definitions
│       └── module-info.java      # Java module configuration
├── docs/                         # Documentation and images
├── bin/                          # Compiled classes
└── README.md
```

### Chi tiết các thành phần:

#### Client.java
- Giao diện đồ họa sử dụng Swing
- Quản lý kết nối TCP tới server
- Hiển thị thông tin thời tiết một cách trực quan
- Log hoạt động chi tiết

#### Server.java
- Multi-threaded server sử dụng ExecutorService
- Tích hợp với WeatherAPI.com
- Hỗ trợ mock data cho testing
- Xử lý JSON parsing tùy chỉnh

#### WeatherProtocol.java
- Định nghĩa các message format
- Utility methods cho parsing data
- WeatherData class cho lưu trữ thông tin

## 🎯 6. Tính năng chính

### Client Features:
- ✅ Giao diện đồ họa hiện đại với tiếng Việt
- ✅ Kết nối/ngắt kết nối server dễ dàng
- ✅ Test ping để kiểm tra kết nối
- ✅ Hiển thị đầy đủ thông tin thời tiết
- ✅ Log hoạt động theo thời gian thực
- ✅ Xử lý lỗi và thông báo người dùng

### Server Features:
- ✅ Xử lý đồng thời nhiều client
- ✅ Tích hợp WeatherAPI.com
- ✅ Mock data cho testing
- ✅ Logging chi tiết server-side
- ✅ Xử lý lỗi robust

## 🚀 7. Một số hình ảnh

### Giao diện chính của Client
![Client Interface](docs/client-interface.png)
*Giao diện chính với các chức năng kết nối và tra cứu thời tiết*

### Kết quả tra cứu thời tiết
![Weather Result](docs/weather-result.png)
*Hiển thị thông tin thời tiết chi tiết của thành phố*

### Server Console Log
![Server Console](docs/server-console.png)
*Log hoạt động của server khi xử lý các request*

## 🔧 8. Hướng dẫn phát triển

### Thêm tính năng mới:
1. Mở rộng `WeatherProtocol.java` với command mới
2. Cập nhật logic xử lý trong `Server.java`
3. Thêm UI controls tương ứng trong `Client.java`

### Debug và Testing:
- Sử dụng mock data khi `API_KEY = "YOUR_WEATHERAPI_KEY_HERE"`
- Kiểm tra log trong console để trace lỗi
- Test với nhiều client đồng thời

## ⚠️ 9. Lưu ý quan trọng

- **API Limitations**: WeatherAPI.com có giới hạn 1,000,000 calls/month cho tài khoản miễn phí
- **Network**: Đảm bảo port 2000 không bị firewall chặn
- **Character Encoding**: Sử dụng UTF-8 để hiển thị đúng tiếng Việt
- **Testing**: Mock data available cho Hanoi, Ho Chi Minh, London, Tokyo, New York

## 📚 10. Tài liệu tham khảo

- [Java Socket Programming Guide](https://docs.oracle.com/javase/tutorial/networking/)
- [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)
- [WeatherAPI.com Documentation](https://www.weatherapi.com/docs/)
- [JSON Processing in Java](https://www.oracle.com/technical-resources/articles/java/json.html)

## 🤝 11. Đóng góp

Chúng tôi hoan nghênh mọi đóng góp để cải thiện dự án:

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📄 12. License

Distributed under the MIT License. See `LICENSE` for more information.

## ✉️ 13. Liên hệ

**Tác giả**: Nguyễn Văn Nguyên  
📧 **Email**: nguyennguyenvh09@gmail.com  
🏫 **Trường**: Đại học Đại Nam - Khoa Công nghệ Thông tin  
🔬 **Lab**: AIoTLab - [Facebook](https://www.facebook.com/DNUAIoTLab)

---

<div align="center">
    <p>Made with ❤️ by AIoTLab - Faculty of Information Technology - DaiNam University</p>
    <p>© 2024 DaiNam University. All rights reserved.</p>
</div>
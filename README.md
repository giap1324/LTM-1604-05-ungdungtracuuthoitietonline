
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

---




## 1. Giới thiệu hệ thống

Hệ thống **Ứng dụng tra cứu thời tiết trực tuyến** được xây dựng theo mô hình **Client-Server** sử dụng **JavaFX** nhằm:

- Hỗ trợ người dùng tra cứu thông tin thời tiết theo thời gian thực (nhiệt độ, độ ẩm, tốc độ gió, mô tả thời tiết chi tiết)
- Cung cấp dự báo thời tiết **5 ngày** với thông tin chi tiết theo từng 3 giờ
- Hiển thị **bản đồ thời tiết tương tác** với OpenStreetMap và OpenWeatherMap layers
- Quản lý **danh sách thành phố yêu thích** với cơ sở dữ liệu MySQL
- Giao diện đồ họa hiện đại với **hiệu ứng động** (animated backgrounds, đồng hồ vector, biểu đồ thời tiết)

👉 **Điểm nổi bật**:
- **Giao diện 3 cột**: Sidebar tìm kiếm + thông tin thời tiết + favorites/clock
- **Hiệu ứng thời tiết động**: Rain, snow, clear sky, clouds, thunderstorm với animation
- **Bản đồ tương tác**: Hiển thị vị trí thành phố, nhiệt độ overlay trên bản đồ thế giới
- **Chuyển đổi đơn vị**: Celsius ↔ Fahrenheit theo nhu cầu người dùng
- **Tự động phát hiện vị trí**: Sử dụng IP geolocation để tìm thành phố hiện tại
- **Dữ liệu theo thời gian thực**: Kết nối với OpenWeatherMap API (5 Day / 3 Hour Forecast)

---

## 🔧 2. Công nghệ & Ngôn ngữ sử dụng

[![Java](https://img.shields.io/badge/Java_23-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![OpenWeatherMap](https://img.shields.io/badge/OpenWeatherMap-EB6E4B?style=for-the-badge&logo=openstreetmap&logoColor=white)](https://openweathermap.org/)

### 🚀 Chi tiết công nghệ
- **Java SE 23**: Ngôn ngữ lập trình chính với các tính năng hiện đại
- **JavaFX**: Framework xây dựng giao diện đồ họa phong phú với animation
- **TCP Socket Programming**: Giao tiếp Client-Server qua cổng 2000
- **HTTP Client**: Kết nối với OpenWeatherMap API (5 Day Forecast)
- **Custom JSON Parser**: Xử lý dữ liệu JSON không cần thư viện bên ngoài
- **MySQL**: Lưu trữ danh sách thành phố yêu thích và lịch sử tìm kiếm
- **OpenStreetMap**: Hiển thị bản đồ tương tác với Leaflet.js integration
- **IP Geolocation**: Tự động phát hiện vị trí người dùng





## 🚀 3. Một số hình ảnh

### Giao diện chính của Client  
![Client Interface](docs/Screenshot%2025-11-24%081849.png)  
Kết nối **Weather Server** qua TCP, nhập tên thành phố để tra cứu thời tiết.  

### Chi tiết thời tiết  
![Weather Result](docs/Screenshot%2025-11-24%082542.png)  
Hiển thị thông tin thời tiết chi tiết: nhiệt độ, độ ẩm, gió, trạng thái bầu trời.  
 
## Bản đồ thời tiết 
![Weather Map](docs/Screenshot%2025-11-24%081304.png")
Cho phép xem lớp mưa (precipitation), mây (clouds), nhiệt độ (temp), gió (wind) với độ phân giải cao theo thời gian thực.

## 📝 4. Các bước cài đặt

### Yêu cầu hệ thống:
- Java Development Kit (JDK) 11 trở lên
- IDE: Eclipse, IntelliJ IDEA, hoặc VS Code
- Kết nối internet (để truy cập WeatherAPI)

### Cài đặt và chạy:

**Cấu hình API Key** (tùy chọn):
   - Đăng ký tài khoản tại [WeatherAPI.com](https://www.weatherapi.com/)
   - Thay thế API key trong `Server.java`:
   ```java
   private static final String API_KEY = "YOUR_API_KEY_HERE";
   ```

**Biên dịch dự án**:
   ```bash
   javac -d bin src/WeatherApp/*.java
   ```

**Chạy ứng dụng**:
   ```bash
   # Chạy Server trước
   java -cp bin WeatherApp.Server
   
   # Sau đó chạy Client
   java -cp bin WeatherApp.Client
   ```

**Sử dụng**:
   - Nhấn "Kết nối" → Nhập tên thành phố → "Tra cứu thời tiết"


## ✉️ 5. Liên hệ

**Tác giả**: Nguyễn Đào Nguyên Giáp 

📧 **Email**: nguyennguyenvh09@gmail.com  
🏫 **Trường**: Đại học Đại Nam - Khoa Công nghệ Thông tin  


---


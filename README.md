
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

👉 **Điểm nổi bật**:
- Người dùng có thể nhập tên thành phố và nhận thông tin thời tiết ngay lập tức
- Hỗ trợ nhiều thành phố trên toàn thế giới, dữ liệu cập nhật theo thời gian thực
- Giao diện client hiện đại với các thông báo trạng thái rõ ràng

## 🔧 2. Công nghệ & Ngôn ngữ sử dụng

[![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![Swing GUI](https://img.shields.io/badge/Swing_GUI-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![WeatherAPI](https://img.shields.io/badge/WeatherAPI-00A1F1?style=for-the-badge&logo=cloud&logoColor=white)](https://www.weatherapi.com/)


**Chi tiết công nghệ:**
- **Java SE 23+**: Ngôn ngữ lập trình chính
- **Java Swing**: Xây dựng giao diện người dùng
- **Socket Programming**: Giao tiếp Client-Server qua TCP
- **HTTP Client**: Kết nối với WeatherAPI.com
- **JSON Parsing**: Xử lý dữ liệu JSON từ API (custom parser)


## 🚀 3. Một số hình ảnh

### Giao diện chính của Client
![Client Interface](docs/Screenshot%202025-09-25%20074913.png)
- Dự án Weather Client cho phép người dùng **kết nối tới Weather Server** và tra cứu thông tin thời tiết theo thành phố.  
- Ứng dụng sử dụng **giao thức TCP** để kết nối Client–Server đảm bảo dữ liệu truyền tin cậy, ổn định.



### Kết quả tra cứu thời tiết
![Weather Result](docs/Screenshot%202025-09-25%20074925.png)  

- Người dùng nhập tên thành phố (**hanoi**) và nhấn nút **Tra cứu thời tiết**.  
- Ứng dụng kết nối với **Weather Server** qua giao thức **TCP** và hiển thị thông tin:  


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

>>>>>>> 0fcc8ff20011e12b650ff450dc189750b11e766e

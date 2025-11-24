// ====================================
// INTEGRATION GUIDE - Thêm Temperature Map vào Weather App
// ====================================

// ============================================
// PHẦN 1: Thêm vào WeatherUI.java
// ============================================

// 1. Thêm import ở đầu file WeatherUI.java:
import application.JxMapsTemperatureView;

// 2. Thêm field vào class WeatherUI:
public class WeatherUI {
    // ... existing fields ...
    
    private JxMapsTemperatureView temperatureMap;
    
    // ... rest of class ...
}

// 3. Thêm method createTemperatureMapSection() vào WeatherUI.java:
/**
 * Create temperature map section with JxMaps
 */
public VBox createTemperatureMapSection() {
    try {
        if (temperatureMap == null) {
            temperatureMap = new JxMapsTemperatureView();
        }
        return temperatureMap.getView();
    } catch (Exception e) {
        System.err.println("[WeatherUI] Error creating temperature map: " + e.getMessage());
        e.printStackTrace();
        
        // Return error placeholder if JxMaps fails
        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setPadding(new Insets(40));
        errorBox.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.12);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;"
        );
        
        Label errorLabel = new Label("❌ Bản đồ nhiệt độ không khả dụng");
        errorLabel.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;"
        );
        
        Label infoLabel = new Label(
            "Vui lòng cài đặt JxMaps để sử dụng tính năng này.\n" +
            "Xem hướng dẫn trong file JXMAPS_SETUP.md"
        );
        infoLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: rgba(255,255,255,0.8);" +
            "-fx-text-alignment: center;"
        );
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(400);
        
        errorBox.getChildren().addAll(errorLabel, infoLabel);
        return errorBox;
    }
}

// 4. Thêm cleanup method vào WeatherUI.java:
/**
 * Cleanup resources (call on application exit)
 */
public void dispose() {
    if (temperatureMap != null) {
        temperatureMap.dispose();
    }
}


// ============================================
// PHẦN 2: Thêm vào client.java
// ============================================

// Tìm method createThreeColumnLayout() trong client.java
// Thêm temperature map section vào middle column

private StackPane createThreeColumnLayout() {
    // ... existing code for root, topContainer, etc. ...
    
    // ========== MIDDLE COLUMN (50%): Main Weather Card + Hourly Forecast + TEMPERATURE MAP ==========
    VBox middleCol = new VBox(20);
    middleCol.setAlignment(Pos.TOP_CENTER);
    
    // Existing sections
    VBox hero = weatherUI.createHeroSection();
    VBox basicsWrap = new VBox(weatherUI.createMainDetailsGrid());
    basicsWrap.setAlignment(Pos.CENTER);
    basicsWrap.setPadding(new Insets(8));
    VBox hourly = weatherUI.createHourlyForecastSection();
    
    // ⭐ THÊM MỚI: Temperature Map Section
    VBox temperatureMapSection = weatherUI.createTemperatureMapSection();
    temperatureMapSection.getStyleClass().add("card");
    temperatureMapSection.setPrefHeight(650);  // Chiều cao phù hợp cho map
    
    // Apply CSS classes
    hero.getStyleClass().add("card");
    basicsWrap.getStyleClass().add("card");
    hourly.getStyleClass().add("card");
    
    // Add all sections to middle column (THÊM temperatureMapSection)
    middleCol.getChildren().addAll(
        hero, 
        basicsWrap, 
        hourly,
        temperatureMapSection  // ⭐ THÊM DÒNG NÀY
    );
    
    // ... rest of the method (right column, scroll pane, etc.) ...
}

// Thêm cleanup vào stop() method của client.java:
@Override
public void stop() {
    if (clockTimeline != null) {
        clockTimeline.stop();
    }
    if (tcpClient != null) {
        try {
            tcpClient.close();
        } catch (Exception e) {
            System.err.println("Error closing TCP client: " + e.getMessage());
        }
    }
    // ⭐ THÊM MỚI: Cleanup map resources
    if (weatherUI != null) {
        weatherUI.dispose();
    }
}


// ============================================
// PHẦN 3: Cấu hình Eclipse Run Configuration
// ============================================

/*
Bước 1: Chuột phải client.java → Run As → Run Configurations...

Bước 2: Tab "Arguments" → VM arguments:

--module-path "C:\javafx-sdk-25\lib" 
--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web 
--add-opens javafx.web/com.sun.webkit=ALL-UNNAMED

(Thay C:\javafx-sdk-25 bằng đường dẫn JavaFX SDK của bạn)

Bước 3: Apply → Run
*/


// ============================================
// PHẦN 4: Cấu hình .env
// ============================================

/*
File: src/application/.env

# OpenWeather API Key (đã có)
OPENWEATHER_API_KEY=your_actual_key_here

# JxMaps License Key (THÊM MỚI - lấy từ email đăng ký)
JXMAPS_LICENSE_KEY=eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqeG1hcHMi...

*/


// ============================================
// PHẦN 5: Thêm JxMaps initialization trong JxMapsTemperatureView.java
// ============================================

// Đảm bảo license được load trong constructor của JxMapsTemperatureView:

public JxMapsTemperatureView() {
    // Load license key from .env
    String licenseKey = EnvLoader.getenv("JXMAPS_LICENSE_KEY");
    if (licenseKey != null && !licenseKey.isEmpty()) {
        System.setProperty("jxmaps.key", licenseKey);
        System.out.println("[JxMapsTemperatureView] License key configured");
    } else {
        System.err.println("[JxMapsTemperatureView] Warning: No license key found in .env");
    }
    
    temperatureService = new TemperatureMapService();
    initializeUI();
}


// ============================================
// PHẦN 6: Alternative Layout - Thêm vào Right Column (nếu muốn)
// ============================================

// Nếu muốn map nhỏ hơn ở cột phải thay vì cột giữa:

// Trong createThreeColumnLayout(), thêm vào rightCol thay vì middleCol:

VBox rightCol = new VBox(18);
// ... existing code ...

// Thêm temperature map (compact version)
VBox temperatureMapSection = weatherUI.createTemperatureMapSection();
temperatureMapSection.getStyleClass().add("card");
temperatureMapSection.setPrefHeight(400);  // Nhỏ hơn cho sidebar
temperatureMapSection.setMaxWidth(380);

rightCol.getChildren().addAll(
    daily,
    temperatureMapSection  // ⭐ Map ở cột phải
);


// ============================================
// PHẦN 7: Testing & Verification
// ============================================

/*
Sau khi tích hợp, kiểm tra Console output:

✅ Expected output:
   [JxMapsTemperatureView] License key configured
   [JxMapsTemperatureView] Map configured successfully
   [TemperatureMapService] Loaded: Tokyo: 23.5°C - clear sky
   [TemperatureMapService] Loaded: Delhi: 31.2°C - haze
   ...
   [TemperatureMapService] Total points loaded: 40

❌ Common errors:
   - "JxMaps không khả dụng" → JARs chưa được thêm vào Build Path
   - "License key is invalid" → License key sai hoặc hết hạn
   - Map trắng → Internet connection hoặc Google Maps API issue
   - JavaFX errors → Xem CONFIGURE_JAVAFX.md
*/


// ============================================
// PHẦN 8: Customization Options
// ============================================

// 1. Thay đổi danh sách thành phố mặc định:
//    Edit: TemperatureMapService.java → DEFAULT_CITIES array

// 2. Thay đổi màu sắc nhiệt độ:
//    Edit: TemperatureDataPoint.java → getTemperatureColor() method

// 3. Thay đổi kích thước markers:
//    Edit: TemperatureDataPoint.java → getMarkerSize() method

// 4. Thay đổi interval auto-refresh:
//    Edit: JxMapsTemperatureView.java → toggleAutoRefresh() method

// 5. Thêm filter theo khu vực:
//    Implement region filter trong TemperatureMapService


// ============================================
// END OF INTEGRATION GUIDE
// ============================================

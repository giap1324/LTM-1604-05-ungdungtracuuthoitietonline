package application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class xử lý parsing JSON data từ Weather API
 * (Phiên bản nâng cấp: Đọc dự báo 5 ngày/3 giờ VÀ tổng hợp 5 ngày)
 */
public class WeatherDataParser {
    
    /**
     * Parse THỜI TIẾT HIỆN TẠI từ JSON dự báo
     * (Lấy từ "city" và "list[0]")
     */
    public WeatherData parseWeatherData(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            throw new Exception("Không có dữ liệu trả về từ server!");
        }

        String cod = extractJsonValue(json, "cod");
        if (cod != null && !"200".equals(cod)) {
            String message = extractJsonString(json, "message");
            throw new Exception(message != null ? capitalizeFirst(message) : "Không tìm thấy thành phố");
        }

        WeatherData data = new WeatherData();
        
        // Trích xuất từ "city" object
        String citySection = extractJsonObject(json, "city");
        data.cityName = extractJsonString(citySection, "name");
        data.country = extractJsonString(citySection, "country");
        data.timezone = parseIntDefault(extractJsonValue(citySection, "timezone"), 0);
        data.sunrise = parseLongDefault(extractJsonValue(citySection, "sunrise"), 0L);
        data.sunset = parseLongDefault(extractJsonValue(citySection, "sunset"), 0L);
        
        // Extract coordinates from city.coord
        String coordSection = extractJsonObject(citySection, "coord");
        data.lat = parseDoubleDefault(extractJsonValue(coordSection, "lat"), 0.0);
        data.lon = parseDoubleDefault(extractJsonValue(coordSection, "lon"), 0.0);
        
        // Trích xuất từ "list" array (lấy phần tử đầu tiên list[0])
        String listArray = extractJsonArray(json, "list");
        String firstElement = extractFirstArrayElement(listArray);
        
        if (firstElement == null) {
            throw new Exception("Không tìm thấy dữ liệu thời tiết trong danh sách");
        }

        String mainSection = extractJsonObject(firstElement, "main");
        data.temperature = parseDoubleDefault(extractJsonValue(mainSection, "temp"), 0.0);
        data.feelsLike = parseDoubleDefault(extractJsonValue(mainSection, "feels_like"), 0.0);
        data.humidity = parseIntDefault(extractJsonValue(mainSection, "humidity"), 0);
        data.pressure = parseIntDefault(extractJsonValue(mainSection, "pressure"), 0);
        data.temp_min = parseDoubleDefault(extractJsonValue(mainSection, "temp_min"), 0.0);
        data.temp_max = parseDoubleDefault(extractJsonValue(mainSection, "temp_max"), 0.0);

        String weatherArray = extractJsonArray(firstElement, "weather");
        String weatherObj = extractFirstArrayElement(weatherArray);
        data.description = extractJsonString(weatherObj, "description");
        data.mainWeather = extractJsonString(weatherObj, "main");
        data.iconCode = extractJsonString(weatherObj, "icon");

        String windSection = extractJsonObject(firstElement, "wind");
        data.windSpeed = parseDoubleDefault(extractJsonValue(windSection, "speed"), 0.0);
        
        data.visibility = parseIntDefault(extractJsonValue(firstElement, "visibility"), 0);
        
        return data;
    }
    
    /**
     * Parse DỰ BÁO HÀNG GIỜ (TOÀN BỘ 5 NGÀY / 3 GIỜ)
     * (Lấy toàn bộ "list")
     */
    public List<ForecastItem> parseForecastData(String json) {
        List<ForecastItem> forecastList = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return forecastList;
        }

        String listArray = extractJsonArray(json, "list");
        if (listArray == null) {
            return forecastList;
        }

        // Vòng lặp qua các phần tử của mảng
        for (String element : extractArrayElements(listArray)) {
            try {
                ForecastItem item = new ForecastItem();
                
                item.dt = parseLongDefault(extractJsonValue(element, "dt"), 0L);

                String mainSection = extractJsonObject(element, "main");
                item.temp = parseDoubleDefault(extractJsonValue(mainSection, "temp"), 0.0);
                item.feelsLike = parseDoubleDefault(extractJsonValue(mainSection, "feels_like"), 0.0);
                item.humidity = parseIntDefault(extractJsonValue(mainSection, "humidity"), 0);

                String weatherArray = extractJsonArray(element, "weather");
                String weatherObj = extractFirstArrayElement(weatherArray);
                item.iconCode = extractJsonString(weatherObj, "icon");
                item.description = extractJsonString(weatherObj, "description");

                // wind
                String windSection = extractJsonObject(element, "wind");
                item.windSpeed = parseDoubleDefault(extractJsonValue(windSection, "speed"), 0.0);

                // clouds
                String cloudsSection = extractJsonObject(element, "clouds");
                if (cloudsSection != null) {
                    item.clouds = parseIntDefault(extractJsonValue(cloudsSection, "all"), 0);
                }

                // rain (3h) if present
                String rainSection = extractJsonObject(element, "rain");
                if (rainSection != null) {
                    String rain3 = extractJsonValue(rainSection, "3h");
                    item.rain3h = parseDoubleDefault(rain3, 0.0);
                }
                
                // pop (probability of precipitation) if present
                item.pop = parseDoubleDefault(extractJsonValue(element, "pop"), 0.0);
                
                forecastList.add(item);
                
                // BỎ GIỚI HẠN 8 MỤC ĐỂ ĐỌC HẾT 40 MỤC (5 NGÀY)
                // if (forecastList.size() >= 8) {
                //     break;
                // }
            } catch (Exception e) {
                // Bỏ qua nếu một mục bị lỗi
                System.err.println("Lỗi parse một mục dự báo: " + e.getMessage());
            }
        }
        return forecastList;
    }

    /**
     * HÀM MỚI: Tổng hợp danh sách dự báo (3 giờ) thành dự báo (hàng ngày)
     */
    public List<DailyForecastItem> aggregateDailyForecast(List<ForecastItem> hourlyItems, int timezoneShift) {
        // Sử dụng LinkedHashMap để giữ thứ tự các ngày
        Map<String, DailyForecastItem> dailyMap = new LinkedHashMap<>();

        for (ForecastItem item : hourlyItems) {
            String date = WeatherHelper.formatDate(item.dt, timezoneShift);
            String hour = WeatherHelper.getHourOfDay(item.dt, timezoneShift);

            if (!dailyMap.containsKey(date)) {
                DailyForecastItem dailyItem = new DailyForecastItem();
                dailyItem.date = date;
                // store a localized weekday name (e.g., "Hôm nay", "Thứ 2", "Chủ nhật")
                dailyItem.weekday = WeatherHelper.getWeekday(item.dt, timezoneShift);
                dailyItem.minTemp = item.temp;
                dailyItem.maxTemp = item.temp;
                dailyItem.iconCode = item.iconCode;
                dailyItem.description = item.description;  // 👈 thêm dòng này
                dailyItem.timezone = timezoneShift;
                dailyItem.windSpeed = item.windSpeed;
                dailyItem.sunrise = 0;
                dailyItem.sunset = 0;
                dailyMap.put(date, dailyItem);
            } else {
                DailyForecastItem dailyItem = dailyMap.get(date);
                dailyItem.minTemp = Math.min(dailyItem.minTemp, item.temp);
                dailyItem.maxTemp = Math.max(dailyItem.maxTemp, item.temp);
                // prefer midday icons/descriptions for representative symbol
                if ("12".equals(hour) || "15".equals(hour)) {
                    dailyItem.iconCode = item.iconCode;
                    dailyItem.description = item.description; // 👈 cập nhật mô tả giữa ngày
                }
                // accumulate/average wind speed
                dailyItem.windSpeed = (dailyItem.windSpeed + item.windSpeed) / 2.0;
            }

        }

        return new ArrayList<>(dailyMap.values());
    }
    /**
     * Lọc danh sách dự báo 3 giờ theo ngày cụ thể
     * Dùng cho màn hình chi tiết (DailyDetailView)
     */
    public List<ForecastItem> filterForecastForDate(List<ForecastItem> all, String targetDate, int timezone) {
        List<ForecastItem> result = new ArrayList<>();
        if (all == null || targetDate == null) return result;

        for (ForecastItem f : all) {
            String date = WeatherHelper.formatDate(f.dt, timezone);
            if (targetDate.equals(date)) {
                result.add(f);
            }
        }
        return result;
    }

    
    // ========== JSON Parsing Helper Methods ==========
    
    // (Các hàm extractJsonString, extractJsonValue, extractJsonObject, extractJsonArray, extractFirstArrayElement giữ nguyên)
    
    private String extractJsonString(String json, String key) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern); if (idx == -1) return null;
            int startQuote = json.indexOf("\"", idx + pattern.length() + 1);
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (startQuote == -1 || endQuote == -1) return null;
            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) { return null; }
    }
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern); if (idx == -1) return null;
            int colon = json.indexOf(":", idx); if (colon == -1) return null;
            int start = colon + 1;
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
            int end = start;
            while (end < json.length()) { char c = json.charAt(end); if (c == ',' || c == '}' || c == ']') break; end++; }
            String value = json.substring(start, end).trim();
            if (value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
            return value;
        } catch (Exception e) { return null; }
    }
    private String extractJsonObject(String json, String key) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern); if (idx == -1) return null;
            int start = json.indexOf("{", idx); if (start == -1) return null;
            int braceCount = 1, end = start + 1;
            while (end < json.length() && braceCount > 0) { if (json.charAt(end) == '{') braceCount++; else if (json.charAt(end) == '}') braceCount--; end++; }
            return json.substring(start, end);
        } catch (Exception e) { return null; }
    }
    private String extractJsonArray(String json, String key) {
        try {
            String pattern = "\"" + key + "\"";
            int idx = json.indexOf(pattern); if (idx == -1) return null;
            int start = json.indexOf("[", idx); if (start == -1) return null;
            int bracketCount = 1, end = start + 1;
            while (end < json.length() && bracketCount > 0) { if (json.charAt(end) == '[') bracketCount++; else if (json.charAt(end) == ']') bracketCount--; end++; }
            return json.substring(start, end);
        } catch (Exception e) { return null; }
    }
    private String extractFirstArrayElement(String array) {
        if (array == null) return null;
        try {
            int start = array.indexOf("{"); if (start == -1) return null;
            int braceCount = 1, end = start + 1;
            while (end < array.length() && braceCount > 0) { if (array.charAt(end) == '{') braceCount++; else if (array.charAt(end) == '}') braceCount--; end++; }
            return array.substring(start, end);
        } catch (Exception e) { return null; }
    }

    /**
     * HÀM MỚI: Tách tất cả các object từ một array
     */
    private List<String> extractArrayElements(String array) {
        List<String> elements = new ArrayList<>();
        if (array == null || !array.startsWith("[")) return elements;
        
        int braceCount = 0;
        int start = -1;
        
        for (int i = 0; i < array.length(); i++) {
            char c = array.charAt(i);
            if (c == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && start != -1) {
                    elements.add(array.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return elements;
    }
    
    // (Các hàm parseDoubleDefault, parseIntDefault, parseLongDefault, capitalizeFirst giữ nguyên)
    private double parseDoubleDefault(String val, double defaultVal) { try { return Double.parseDouble(val); } catch (Exception e) { return defaultVal; } }
    private int parseIntDefault(String val, int defaultVal) { try { return Integer.parseInt(val); } catch (Exception e) { return defaultVal; } }
    private long parseLongDefault(String val, long defaultVal) { try { return Long.parseLong(val); } catch (Exception e) { return defaultVal; } }
    private String capitalizeFirst(String text) { if (text == null || text.isEmpty()) return text; return text.substring(0, 1).toUpperCase() + text.substring(1); }
    
    /**
     * Lớp con cho DỰ BÁO HÀNG GIỜ
     */
    public static class ForecastItem {
        public long dt; // timestamp
        public double temp;
        public String iconCode;
        public String description;
        // Added fields
        public int humidity;
        public double windSpeed;
        public double rain3h;
        public double feelsLike;
        public int clouds;
        public double pop; // probability of precipitation (0.0 - 1.0)
	
    }

    /**
     * LỚP CON MỚI: Cho DỰ BÁO HÀNG NGÀY
     */
    public static class DailyForecastItem {
        public String date;       // "dd/MM"
        public String weekday;    // e.g. "Thứ 2" or "Hôm nay"
        public double minTemp;
        public double maxTemp;
        public String iconCode;
        public String description; // 🌤 mô tả tổng quát (ví dụ: "clear sky")
        public int timezone;       // ⏰ múi giờ (dùng cho hiển thị giờ địa phương)
		public double windSpeed;
		public int sunset;
		public int sunrise;
    }

    /**
     * Lớp con cho THỜI TIẾT HIỆN TẠI
     */
    public static class WeatherData {
        public String cityName, country, description, mainWeather, iconCode;
        public int timezone, visibility, humidity, pressure;
        public double temperature, feelsLike, temp_min, temp_max, windSpeed;
        public long sunrise, sunset;
        public double lat, lon; // Coordinates for map view
    }
    
}
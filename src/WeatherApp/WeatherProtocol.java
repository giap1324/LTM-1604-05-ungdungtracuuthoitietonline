package WeatherApp;

/**
 * WeatherProtocol - Định nghĩa giao thức giao tiếp giữa Client và Server
 * 
 * GIAO THỨC GIAO TIẾP:
 * ===================
 * 
 * 1. CONNECTION HANDSHAKE:
 *    Client -> Server: Kết nối TCP
 *    Server -> Client: "WEATHER_SERVER_CONNECTED"
 *    Server -> Client: "Chào mừng đến với Weather Server!"
 * 
 * 2. WEATHER REQUEST:
 *    Client -> Server: "WEATHER:<city_name>"
 *    Server -> Client: "SUCCESS:<weather_data>" hoặc "ERROR:<error_message>"
 * 
 * 3. PING REQUEST:
 *    Client -> Server: "PING"
 *    Server -> Client: "PONG"
 * 
 * 4. QUIT REQUEST:
 *    Client -> Server: "QUIT"
 *    Server: Đóng kết nối
 * 
 * 5. WEATHER DATA FORMAT:
 *    "CITY:<city>,<country>|TEMP:<temperature>|DESC:<description>|HUMIDITY:<humidity>|WIND:<wind_speed>"
 */
public class WeatherProtocol {
    
    // Protocol Commands
    public static final String CMD_WEATHER = "WEATHER:";
    public static final String CMD_PING = "PING";
    public static final String CMD_QUIT = "QUIT";
    
    // Protocol Responses
    public static final String RESP_CONNECTED = "WEATHER_SERVER_CONNECTED";
    public static final String RESP_SUCCESS = "SUCCESS:";
    public static final String RESP_ERROR = "ERROR:";
    public static final String RESP_PONG = "PONG";
    
    // Data Field Separators
    public static final String FIELD_SEPARATOR = "|";
    public static final String VALUE_SEPARATOR = ":";
    
    // Data Field Names
    public static final String FIELD_CITY = "CITY";
    public static final String FIELD_TEMP = "TEMP";
    public static final String FIELD_DESC = "DESC";
    public static final String FIELD_HUMIDITY = "HUMIDITY";
    public static final String FIELD_WIND = "WIND";
    
    /**
     * Tạo request message cho việc tra cứu thời tiết
     */
    public static String createWeatherRequest(String cityName) {
        return CMD_WEATHER + cityName;
    }
    
    /**
     * Tạo success response với dữ liệu thời tiết
     */
    public static String createSuccessResponse(String city, String country, 
                                             double temperature, String description, 
                                             int humidity, double windSpeed) {
        StringBuilder response = new StringBuilder(RESP_SUCCESS);
        
        response.append(FIELD_CITY).append(VALUE_SEPARATOR).append(city).append(",").append(country);
        response.append(FIELD_SEPARATOR);
        response.append(FIELD_TEMP).append(VALUE_SEPARATOR).append(temperature);
        response.append(FIELD_SEPARATOR);
        response.append(FIELD_DESC).append(VALUE_SEPARATOR).append(description);
        response.append(FIELD_SEPARATOR);
        response.append(FIELD_HUMIDITY).append(VALUE_SEPARATOR).append(humidity);
        response.append(FIELD_SEPARATOR);
        response.append(FIELD_WIND).append(VALUE_SEPARATOR).append(windSpeed);
        
        return response.toString();
    }
    
    /**
     * Tạo error response
     */
    public static String createErrorResponse(String errorMessage) {
        return RESP_ERROR + errorMessage;
    }
    
    /**
     * Parse weather data từ response
     */
    public static WeatherData parseWeatherResponse(String response) {
        if (!response.startsWith(RESP_SUCCESS)) {
            return null;
        }
        
        String data = response.substring(RESP_SUCCESS.length());
        String[] fields = data.split("\\" + FIELD_SEPARATOR);
        
        WeatherData weatherData = new WeatherData();
        
        for (String field : fields) {
            String[] parts = field.split(VALUE_SEPARATOR, 2);
            if (parts.length != 2) continue;
            
            String fieldName = parts[0];
            String fieldValue = parts[1];
            
            switch (fieldName) {
                case FIELD_CITY:
                    weatherData.setCityInfo(fieldValue);
                    break;
                case FIELD_TEMP:
                    weatherData.setTemperature(Double.parseDouble(fieldValue));
                    break;
                case FIELD_DESC:
                    weatherData.setDescription(fieldValue);
                    break;
                case FIELD_HUMIDITY:
                    weatherData.setHumidity(Integer.parseInt(fieldValue));
                    break;
                case FIELD_WIND:
                    weatherData.setWindSpeed(Double.parseDouble(fieldValue));
                    break;
            }
        }
        
        return weatherData;
    }
    
    /**
     * Kiểm tra xem message có phải là weather request không
     */
    public static boolean isWeatherRequest(String message) {
        return message != null && message.startsWith(CMD_WEATHER);
    }
    
    /**
     * Lấy city name từ weather request
     */
    public static String getCityFromRequest(String request) {
        if (isWeatherRequest(request)) {
            return request.substring(CMD_WEATHER.length()).trim();
        }
        return null;
    }
    
    /**
     * Kiểm tra xem response có phải là success không
     */
    public static boolean isSuccessResponse(String response) {
        return response != null && response.startsWith(RESP_SUCCESS);
    }
    
    /**
     * Kiểm tra xem response có phải là error không
     */
    public static boolean isErrorResponse(String response) {
        return response != null && response.startsWith(RESP_ERROR);
    }
    
    /**
     * Lấy error message từ error response
     */
    public static String getErrorMessage(String errorResponse) {
        if (isErrorResponse(errorResponse)) {
            return errorResponse.substring(RESP_ERROR.length());
        }
        return null;
    }
    
    /**
     * Inner class để lưu trữ dữ liệu thời tiết đã parse
     */
    public static class WeatherData {
        private String cityInfo;
        private double temperature;
        private String description;
        private int humidity;
        private double windSpeed;
        
        // Getters and setters
        public String getCityInfo() { return cityInfo; }
        public void setCityInfo(String cityInfo) { this.cityInfo = cityInfo; }
        
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getHumidity() { return humidity; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
        
        public double getWindSpeed() { return windSpeed; }
        public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
        
        @Override
        public String toString() {
            return String.format("Weather{city='%s', temp=%.1f°C, desc='%s', humidity=%d%%, wind=%.1fm/s}",
                    cityInfo, temperature, description, humidity, windSpeed);
        }
    }
}
package WeatherApp;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 2000;
    private static final String API_KEY = "51d06d36e74049658b942307251709"; // API key từ WeatherAPI.com
    private static final String WEATHER_API_URL = "http://api.weatherapi.com/v1/current.json";
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        System.out.println("=== WEATHER SERVER WITH REAL API ===");
        System.out.println("Server khởi động trên port: " + PORT);
        System.out.println("Sử dụng WeatherAPI.com service");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server sẵn sàng...");
            
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("🔗 Client kết nối: " + clientSocket.getInetAddress());
                    
                    threadPool.submit(new SimpleClientHandler(clientSocket));
                    
                } catch (IOException e) {
                    System.err.println("❌ Lỗi chấp nhận kết nối: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Không thể khởi tạo server: " + e.getMessage());
        }
    }

    static class SimpleClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;

        public SimpleClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                setupStreams();
                handleClient();
            } catch (IOException e) {
                System.err.println("❌ Lỗi xử lý client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void setupStreams() throws IOException {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        private void handleClient() throws IOException {
            String clientAddr = clientSocket.getInetAddress().getHostAddress();
            System.out.println("📡 Xử lý client: " + clientAddr);

            // Gửi welcome message
            output.println("WEATHER_SERVER_CONNECTED");
            output.println("Chào mừng đến Weather Server (WeatherAPI.com)!");

            String request;
            while ((request = input.readLine()) != null) {
                System.out.println("📨 Request: " + request);
                
                if (request.equals("QUIT")) {
                    System.out.println("👋 Client ngắt kết nối");
                    break;
                }
                
                if (request.startsWith("WEATHER:")) {
                    String city = request.substring(8).trim();
                    handleWeatherRequest(city);
                } else if (request.equals("PING")) {
                    output.println("PONG");
                } else {
                    output.println("ERROR:Lệnh không hợp lệ");
                }
            }
        }

        private void handleWeatherRequest(String cityName) {
            if (cityName.isEmpty()) {
                output.println("ERROR:Tên thành phố trống");
                return;
            }

            System.out.println("🌤️ Tra cứu thời tiết cho: " + cityName);
            
            try {
                String weatherData = fetchWeatherApiData(cityName);
                if (weatherData != null) {
                    String formatted = parseWeatherApiData(weatherData);
                    if (formatted != null) {
                        output.println("SUCCESS:" + formatted);
                        System.out.println("✅ Đã gửi dữ liệu thời tiết cho: " + cityName);
                    } else {
                        output.println("ERROR:Lỗi xử lý dữ liệu từ API");
                    }
                } else {
                    output.println("ERROR:Không tìm thấy dữ liệu cho " + cityName);
                }
                
            } catch (Exception e) {
                output.println("ERROR:Lỗi server - " + e.getMessage());
                System.err.println("❌ Lỗi: " + e.getMessage());
            }
        }

        private String fetchWeatherApiData(String cityName) {
            try {
                String encodedCity = URLEncoder.encode(cityName, "UTF-8");
                String urlString = WEATHER_API_URL + "?key=" + API_KEY + "&q=" + encodedCity + "&aqi=no";
                
                System.out.println("📡 Calling WeatherAPI: " + urlString.replace(API_KEY, "***"));
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "WeatherApp/1.0");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                
                int responseCode = connection.getResponseCode();
                System.out.println("📊 API Response Code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    System.out.println("✅ API trả về dữ liệu thành công");
                    return response.toString();
                    
                } else {
                    // Đọc error response
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream())
                    );
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                    
                    System.err.println("❌ API Error " + responseCode + ": " + errorResponse.toString());
                    return null;
                }
                
            } catch (Exception e) {
                System.err.println("❌ Lỗi kết nối API: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        private String parseWeatherApiData(String jsonData) {
            try {
                System.out.println("📝 Đang phân tích dữ liệu JSON...");
                
                // Extract location information
                String cityName = extractJsonValue(jsonData, "\"name\":");
                String country = extractJsonValue(jsonData, "\"country\":");
                
                // Extract current weather data
                String tempStr = extractJsonValue(jsonData, "\"temp_c\":");
                String humidityStr = extractJsonValue(jsonData, "\"humidity\":");
                String windStr = extractJsonValue(jsonData, "\"wind_kph\":");
                String condition = extractJsonValue(jsonData, "\"text\":");
                
                if (cityName.isEmpty() || tempStr.isEmpty()) {
                    System.err.println("❌ Không thể parse được dữ liệu cần thiết từ API");
                    return null;
                }
                
                // Convert wind speed from km/h to m/s
                double windKph = Double.parseDouble(windStr.isEmpty() ? "0" : windStr);
                double windMs = Math.round((windKph / 3.6) * 100.0) / 100.0;
                
                // Round temperature
                double temp = Math.round(Double.parseDouble(tempStr) * 10.0) / 10.0;
                
                // Format response
                StringBuilder result = new StringBuilder();
                result.append("CITY:").append(cityName).append(",").append(country).append("|");
                result.append("TEMP:").append(temp).append("|");
                result.append("DESC:").append(condition).append("|");
                result.append("HUMIDITY:").append(humidityStr.isEmpty() ? "0" : humidityStr).append("|");
                result.append("WIND:").append(windMs);
                
                System.out.println("✅ Dữ liệu đã được xử lý: " + result.toString());
                return result.toString();
                
            } catch (Exception e) {
                System.err.println("❌ Lỗi phân tích JSON: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        private String extractJsonValue(String json, String key) {
            try {
                int keyIndex = json.indexOf(key);
                if (keyIndex == -1) {
                    System.out.println("⚠️ Không tìm thấy key: " + key);
                    return "";
                }
                
                int valueStart = keyIndex + key.length();
                
                // Skip whitespace and colon
                while (valueStart < json.length() && 
                       (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == ':')) {
                    valueStart++;
                }
                
                // Check if value is string (starts with quote)
                if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                    valueStart++; // Skip opening quote
                    int valueEnd = json.indexOf('"', valueStart);
                    if (valueEnd == -1) return "";
                    return json.substring(valueStart, valueEnd);
                } else {
                    // Numeric value
                    int valueEnd = valueStart;
                    while (valueEnd < json.length() && 
                           (Character.isDigit(json.charAt(valueEnd)) || 
                            json.charAt(valueEnd) == '.' || 
                            json.charAt(valueEnd) == '-')) {
                        valueEnd++;
                    }
                    if (valueEnd > valueStart) {
                        return json.substring(valueStart, valueEnd);
                    }
                    return "";
                }
                
            } catch (Exception e) {
                System.err.println("❌ Lỗi trích xuất giá trị cho key: " + key + " - " + e.getMessage());
                return "";
            }
        }

        private void cleanup() {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (clientSocket != null) clientSocket.close();
                
                System.out.println("🔌 Đã đóng kết nối client");
            } catch (IOException e) {
                System.err.println("❌ Lỗi đóng kết nối: " + e.getMessage());
            }
        }
    }
}
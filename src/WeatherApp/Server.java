package WeatherApp;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 2000;
    private static final String API_KEY = "51d06d36e74049658b942307251709";
    private static final String WEATHER_API_URL = "http://api.weatherapi.com/v1/current.json";
    private static final String FORECAST_API_URL = "http://api.weatherapi.com/v1/forecast.json";
    private static final String IP_GEO_API_URL = "https://ipapi.co/json";
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Weather Server running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new SimpleClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
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
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);

                output.println("WEATHER_SERVER_CONNECTED");
                output.println("Welcome to Weather Server");

                String request;
                while ((request = input.readLine()) != null) {
                    if (request.equals("QUIT")) break;

                    if (request.startsWith("WEATHER:")) {
                        String city = request.substring(8).trim();
                        handleWeatherRequest(city);
                    } else if (request.equals("WEATHER_CURRENT")) {
                        String city = fetchCurrentLocation();
                        handleWeatherRequest(city);
                    } else if (request.startsWith("WEATHER_FORECAST:")) {
                        String city = request.substring(17).trim();
                        handleForecastRequest(city, 5); // dự báo 5 ngày
                    } else if (request.equals("PING")) {
                        output.println("PONG");
                    } else {
                        output.println("ERROR:Invalid command");
                    }
                }

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleWeatherRequest(String cityName) {
            if (cityName.isEmpty()) {
                output.println("ERROR:Empty city name");
                return;
            }
            try {
                String weatherData = fetchWeatherApiData(cityName);
                if (weatherData != null) {
                    String formatted = parseWeatherApiData(weatherData);
                    if (formatted != null) {
                        output.println("SUCCESS:" + formatted);
                    } else {
                        output.println("ERROR:Failed to process API data");
                    }
                } else {
                    output.println("ERROR:No data found for " + cityName);
                }
            } catch (Exception e) {
                output.println("ERROR:Server error - " + e.getMessage());
            }
        }

        private String fetchWeatherApiData(String cityName) {
            try {
                String encodedCity = URLEncoder.encode(cityName, "UTF-8");
                // Thêm &lang=vi để nhận dữ liệu tiếng Việt
                String urlString = WEATHER_API_URL + "?key=" + API_KEY + "&q=" + encodedCity + "&aqi=no&lang=vi";
                return fetchHttp(urlString);
            } catch (Exception e) {
                return null;
            }
        }

        private String parseWeatherApiData(String jsonData) {
            try {
                String cityName = extractJsonValue(jsonData, "\"name\":");
                String country = extractJsonValue(jsonData, "\"country\":");
                String tempStr = extractJsonValue(jsonData, "\"temp_c\":");
                String humidityStr = extractJsonValue(jsonData, "\"humidity\":");
                String windStr = extractJsonValue(jsonData, "\"wind_kph\":");
                String condition = extractJsonValue(jsonData, "\"text\":"); // giờ là tiếng Việt

                if (cityName.isEmpty() || tempStr.isEmpty()) return null;

                double windKph = Double.parseDouble(windStr.isEmpty() ? "0" : windStr);
                double windMs = Math.round((windKph / 3.6) * 100.0) / 100.0;
                double temp = Math.round(Double.parseDouble(tempStr) * 10.0) / 10.0;

                return "CITY:" + cityName + "," + country +
                        "|TEMP:" + temp +
                        "|DESC:" + condition +
                        "|HUMIDITY:" + (humidityStr.isEmpty() ? "0" : humidityStr) +
                        "|WIND:" + windMs;

            } catch (Exception e) {
                return null;
            }
        }

        private void handleForecastRequest(String cityName, int days) {
            if (cityName.isEmpty()) {
                output.println("ERROR:Empty city name");
                return;
            }
            try {
                String forecastData = fetchForecastApiData(cityName, days);
                if (forecastData != null) {
                    String formatted = parseForecastData(forecastData, days);
                    if (formatted != null) {
                        output.println("FORECAST_SUCCESS:" + formatted);
                    } else {
                        output.println("ERROR:Failed to process forecast data");
                    }
                } else {
                    output.println("ERROR:No forecast data found for " + cityName);
                }
            } catch (Exception e) {
                output.println("ERROR:Server error - " + e.getMessage());
            }
        }

        private String fetchForecastApiData(String cityName, int days) {
            try {
                String encodedCity = URLEncoder.encode(cityName, "UTF-8");
                // Thêm &lang=vi để nhận dữ liệu tiếng Việt
                String urlString = FORECAST_API_URL + "?key=" + API_KEY + "&q=" + encodedCity +
                        "&days=" + days + "&aqi=no&alerts=no&lang=vi";
                return fetchHttp(urlString);
            } catch (Exception e) {
                return null;
            }
        }

        private String parseForecastData(String jsonData, int days) {
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < days; i++) {
                    String date = extractJsonValue(jsonData, "\"date\":", i, "\"forecastday\":");
                    String maxTemp = extractJsonValue(jsonData, "\"maxtemp_c\":", i, "\"day\":");
                    String minTemp = extractJsonValue(jsonData, "\"mintemp_c\":", i, "\"day\":");
                    String condition = extractJsonValue(jsonData, "\"text\":", i, "\"condition\":"); // giờ là tiếng Việt

                    sb.append(date)
                      .append("|MAX:").append(maxTemp)
                      .append("|MIN:").append(minTemp)
                      .append("|DESC:").append(condition);

                    if (i < days - 1) sb.append(";");
                }
                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        }

        private String fetchHttp(String urlString) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "WeatherApp/1.0");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    return response.toString();
                } else return null;
            } catch (Exception e) {
                return null;
            }
        }

        private String fetchCurrentLocation() {
            try {
                URL url = new URL(IP_GEO_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String city = extractJsonValue(sb.toString(), "\"city\":");
                return city.isEmpty() ? "Hanoi" : city;
            } catch (Exception e) {
                return "Hanoi";
            }
        }

        private String extractJsonValue(String json, String key) {
            return extractJsonValue(json, key, 0, null);
        }

        private String extractJsonValue(String json, String key, int occurrence, String parentKey) {
            try {
                int start = 0;
                if (parentKey != null) {
                    int parentIndex = json.indexOf(parentKey);
                    if (parentIndex == -1) return "";
                    start = parentIndex;
                }

                for (int i = 0; i <= occurrence; i++) {
                    start = json.indexOf(key, start);
                    if (start == -1) return "";
                    start += key.length();
                }

                if (start < json.length() && json.charAt(start) == '"') {
                    start++;
                    int end = json.indexOf('"', start);
                    return end == -1 ? "" : json.substring(start, end);
                } else {
                    int end = start;
                    while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-'))
                        end++;
                    return end > start ? json.substring(start, end) : "";
                }
            } catch (Exception e) {
                return "";
            }
        }

        private void cleanup() {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}

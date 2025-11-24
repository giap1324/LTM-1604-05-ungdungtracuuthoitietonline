package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Server {
    private static final String API_KEY = "f703b7ff3e074702874145120250110";
    private static final String CURRENT_API = "http://api.weatherapi.com/v1/current.json";
    private static final String FORECAST_API = "http://api.weatherapi.com/v1/forecast.json";
    private static final String SEARCH_API = "http://api.weatherapi.com/v1/search.json";

    public static void main(String[] args) {
        try (ServerSocket ss = new ServerSocket(5000)) {
            System.out.println("Weather Server started on port 5000!");
            System.out.println("Waiting for clients...");
            while (true) {
                Socket connSocket = ss.accept();
                System.out.println("Client connected: " + connSocket.getInetAddress());
                new Thread(new ClientHandler(connSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Cannot start server on port 5000!");
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                String request;
                while ((request = br.readLine()) != null) {
                    System.out.println("Received: " + request);
                    String response = handleRequest(request);
                    bw.write(response);
                    bw.newLine();
                    bw.flush();
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + e.getMessage());
            } finally {
                try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
            }
        }

        private String handleRequest(String request) {
            try {
                if (request == null || request.trim().isEmpty()) return "ERROR|Empty request";
                String[] parts = request.split("\\|");
                if (parts.length == 0) return "ERROR|Bad request format";
                String command = parts[0].trim().toUpperCase();

                switch (command) {
                    case "CURRENT":
                        if (parts.length < 2) return "ERROR|Missing param";
                        if (parts[1].startsWith("LAT:")) {
                            String[] coords = parts[1].substring(4).split(",");
                            if (coords.length < 2) return "ERROR|Bad coords";
                            return fetchCurrentWeather(coords[0].trim() + "," + coords[1].trim());
                        }
                        return fetchCurrentWeather(URLEncoder.encode(parts[1].trim(), "UTF-8"));

                    case "FORECAST":
                        if (parts.length < 3) return "ERROR|Missing params";
                        String query = parts[1].startsWith("LAT:") ? 
                            parts[1].substring(4) : URLEncoder.encode(parts[1].trim(), "UTF-8");
                        return fetchForecast(query, parts[2].trim());

                    case "RESOLVE":
                        if (parts.length < 2) return "ERROR|Missing city";
                        return resolveCoordsCommand(parts[1].trim());

                    case "DETAILED":
                        if (parts.length < 3) return "ERROR|Missing params";
                        String detailQuery = parts[1].startsWith("LAT:") ? 
                            parts[1].substring(4) : URLEncoder.encode(parts[1].trim(), "UTF-8");
                        return fetchDetailedWeather(detailQuery, parts[2].trim());
                    case "SEARCH":
                        if (parts.length < 2) return "ERROR|Missing search query";
                        return searchCities(URLEncoder.encode(parts[1].trim(), "UTF-8"));
                    default:
                        return "ERROR|Unknown command";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR|" + e.getMessage();
            }
        }
        private String searchCities(String query) {
            try {
                String url = SEARCH_API + "?key=" + API_KEY + "&q=" + query;
                String json = makeHttpRequest(url);
                if (json == null || json.startsWith("ERROR")) return json;
                return parseSearchResults(json);
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }
        private String resolveCoordsCommand(String city) {
            try {
                String coords = resolveToCoords(city);
                if (coords.isEmpty()) return "ERROR|No coordinates found";
                String[] ll = coords.split(",");
                return "COORDS|" + ll[0] + "|" + ll[1];
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String resolveToCoords(String city) {
            try {
                String url = SEARCH_API + "?key=" + API_KEY + "&q=" + URLEncoder.encode(city, "UTF-8");
                String json = makeHttpRequest(url);
                if (json == null || json.length() < 10) return "";

                int latIdx = json.indexOf("\"lat\":");
                int lonIdx = json.indexOf("\"lon\":");
                if (latIdx != -1 && lonIdx != -1) {
                    String lat = extractValue(json.substring(latIdx), "\"lat\":", ",");
                    String lon = extractValue(json.substring(lonIdx), "\"lon\":", ",");
                    if (!lat.isEmpty() && !lon.isEmpty()) return lat + "," + lon;
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        private String formatDateForHour(String time) {
            try {
                if (time.length() >= 10) {
                    return time.substring(0, 10);
                }
            } catch (Exception e) {
                // Ignore
            }
            return time;
        }

        private String extractValue(String text, String start, String end) {
            try {
                int idx = text.indexOf(start);
                if (idx == -1) return "";
                idx += start.length();
                int endIdx = "\"".equals(end) ? text.indexOf("\"", idx) : text.indexOf(end, idx);
                if (endIdx == -1) endIdx = text.length();
                String val = text.substring(idx, Math.min(endIdx, text.length())).trim();
                if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
                return val;
            } catch (Exception e) {
                return "";
            }
        }

        private String extractJsonValue(String json, String key) {
            try {
                int idx = json.indexOf(key);
                if (idx == -1) return "";
                int start = idx + key.length();
                while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"' || json.charAt(start) == ':')) start++;
                if (start >= json.length()) return "";
                boolean quoted = json.charAt(start - 1) == '"';
                int end = start;
                if (quoted) {
                    end = json.indexOf("\"", start);
                } else {
                    while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
                }
                return json.substring(start, end).trim();
            } catch (Exception e) {
                return "";
            }
        }

        private String formatTime(String time) {
            try {
                if (time.contains(" ")) {
                    return time;
                } else if (time.length() >= 5) {
                    return time.substring(0, 5);
                }
            } catch (Exception e) {
                // Ignore
            }
            return time;
        }

        private String fetchCurrentWeather(String query) {
            try {
                String url = CURRENT_API + "?key=" + API_KEY + "&q=" + query + "&aqi=no&lang=vi";
                String json = makeHttpRequest(url);
                if (json == null || json.startsWith("ERROR")) return json;
                return parseCurrentWeather(json);
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String fetchForecast(String query, String days) {
            try {
                String url = FORECAST_API + "?key=" + API_KEY + "&q=" + query + "&days=" + days + "&aqi=no&lang=vi";
                String json = makeHttpRequest(url);
                if (json == null || json.startsWith("ERROR")) return json;
                return parseForecast(json);
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String fetchDetailedWeather(String query, String date) {
            try {
                String url = FORECAST_API + "?key=" + API_KEY + "&q=" + query + "&days=1&aqi=no&lang=vi&dt=" + date;
                String json = makeHttpRequest(url);
                if (json == null || json.startsWith("ERROR")) return json;
                return parseDetailedWeather(json);
            } catch (Exception e) {
                return "ERROR|" + e.getMessage();
            }
        }

        private String makeHttpRequest(String urlString) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                if (in == null) return "ERROR|HTTP " + code;

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                return (code >= 200 && code < 300) ? response.toString() : "ERROR|HTTP " + code;
            } catch (IOException e) {
                return "ERROR|" + e.getMessage();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        private String parseCurrentWeather(String json) {
            try {
                return String.format("CURRENT|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                    extractJsonValue(json, "\"name\":"),
                    extractJsonValue(json, "\"country\":"),
                    extractJsonValue(json, "\"temp_c\":"),
                    extractJsonValue(json, "\"humidity\":"),
                    extractJsonValue(json, "\"text\":"),
                    extractJsonValue(json, "\"wind_dir\":"),
                    extractJsonValue(json, "\"wind_kph\":"),
                    extractJsonValue(json, "\"feelslike_c\":"),
                    extractJsonValue(json, "\"cloud\":"),
                    extractJsonValue(json, "\"uv\":"),
                    extractJsonValue(json, "\"icon\":"));
            } catch (Exception e) {
                return "ERROR|Parse error";
            }
        }

        private String parseForecast(String json) {
            try {
                StringBuilder result = new StringBuilder("FORECAST|");
                result.append(extractJsonValue(json, "\"name\":"));

                int fcIdx = json.indexOf("\"forecastday\":");
                if (fcIdx == -1) return "ERROR|No forecast data";

                String fcSection = json.substring(fcIdx);
                int pos = 0, dayCount = 0;

                while (dayCount < 5) {
                    int dateIdx = fcSection.indexOf("\"date\":\"", pos);
                    if (dateIdx == -1) break;
                    
                    String date = fcSection.substring(dateIdx + 8, fcSection.indexOf("\"", dateIdx + 8));
                    String section = fcSection.substring(dateIdx, Math.min(dateIdx + 2000, fcSection.length()));
                    
                    result.append("|").append(date)
                        .append(",").append(extractValue(section, "\"maxtemp_c\":", ","))
                        .append(",").append(extractValue(section, "\"mintemp_c\":", ","))
                        .append(",").append(extractValue(section, "\"text\":\"", "\""))
                        .append(",").append(extractValue(section, "\"icon\":\"", "\""))
                        .append(",").append(extractValue(section, "\"daily_chance_of_rain\":", ","));
                    
                    dayCount++;
                    pos = dateIdx + date.length() + 10;
                }

                result.append("|HOURLY");
                int hourIdx = fcSection.indexOf("\"hour\":[");
                if (hourIdx != -1) {
                    String hourSection = fcSection.substring(hourIdx);
                    pos = 0;
                    int hourCount = 0;
                    
                    while (pos < hourSection.length() && hourCount < 24) {
                        int timeIdx = hourSection.indexOf("\"time\":\"", pos);
                        if (timeIdx == -1) break;
                        
                        int timeEnd = hourSection.indexOf("\"", timeIdx + 8);
                        String time = hourSection.substring(timeIdx + 8, timeEnd);
                        String temp = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 100, hourSection.length())), "\"temp_c\":", ",");
                        
                        if (temp.isEmpty()) { pos = timeEnd; continue; }
                        
                        String cond = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 200, hourSection.length())), "\"text\":\"", "\"");
                        String icon = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 300, hourSection.length())), "\"icon\":\"", "\"");
                        String rain = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 400, hourSection.length())), "\"chance_of_rain\":", ",");
                        
                        result.append("|").append(time).append(",").append(temp)
                            .append(",").append(cond).append(",").append(icon).append(",").append(rain);
                        
                        hourCount++;
                        pos = timeEnd;
                    }
                }

                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR|Parse error";
            }
        }
        private String parseSearchResults(String json) {
            try {
                StringBuilder result = new StringBuilder("SEARCH");
                
                int pos = 0;
                int count = 0;
                
                while (pos < json.length() && count < 10) {
                    int nameIdx = json.indexOf("\"name\":\"", pos);
                    if (nameIdx == -1) break;
                    
                    int nameEnd = json.indexOf("\"", nameIdx + 8);
                    if (nameEnd == -1) break;
                    String name = json.substring(nameIdx + 8, nameEnd);
                    
                    int countryIdx = json.indexOf("\"country\":\"", nameEnd);
                    if (countryIdx == -1) break;
                    
                    int countryEnd = json.indexOf("\"", countryIdx + 11);
                    if (countryEnd == -1) break;
                    String country = json.substring(countryIdx + 11, countryEnd);
                    
                    result.append("|").append(name).append(",,").append(country);
                    
                    count++;
                    pos = countryEnd;
                }
                
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR|Parse error: " + e.getMessage();
            }
        }

        private String parseDetailedWeather(String json) {
            try {
                StringBuilder result = new StringBuilder("DETAILED");
                
                // Lấy thông tin mặt trời mọc/lặn
                String sunrise = extractJsonValue(json, "\"sunrise\":\"");
                String sunset = extractJsonValue(json, "\"sunset\":\"");
                
                // Lấy thông tin current weather cho các chi tiết khác
                String humidity = extractJsonValue(json, "\"humidity\":");
                String windSpeed = extractJsonValue(json, "\"wind_kph\":");
                String windDir = extractJsonValue(json, "\"wind_dir\":");
                String uv = extractJsonValue(json, "\"uv\":");
                
                result.append("|").append(sunrise.isEmpty() ? "06:00 AM" : formatTime(sunrise))
                      .append("|").append(sunset.isEmpty() ? "06:00 PM" : formatTime(sunset))
                      .append("|").append(humidity.isEmpty() ? "75" : humidity)
                      .append("|").append(windSpeed.isEmpty() ? "15" : windSpeed)
                      .append("|").append(windDir.isEmpty() ? "NE" : windDir)
                      .append("|").append(uv.isEmpty() ? "5" : uv);
                
                // Thêm hourly data
                result.append("|HOURLY");
                
                int hourIdx = json.indexOf("\"hour\":[");
                if (hourIdx != -1) {
                    String hourSection = json.substring(hourIdx);
                    int pos = 0;
                    int hourCount = 0;
                    
                    while (pos < hourSection.length() && hourCount < 8) {
                        int timeIdx = hourSection.indexOf("\"time\":\"", pos);
                        if (timeIdx == -1) break;
                        
                        int timeEnd = hourSection.indexOf("\"", timeIdx + 8);
                        String time = hourSection.substring(timeIdx + 8, timeEnd);
                        
                        // Kiểm tra xem có phải là ngày đang xét không
                        if (!time.contains(formatDateForHour(time))) {
                            pos = timeEnd;
                            continue;
                        }
                        
                        String temp = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 100, hourSection.length())), "\"temp_c\":", ",");
                        String cond = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 200, hourSection.length())), "\"text\":\"", "\"");
                        String icon = extractValue(hourSection.substring(timeEnd, Math.min(timeEnd + 300, hourSection.length())), "\"icon\":\"", "\"");
                        
                        result.append("|").append(time).append(",").append(temp)
                              .append(",").append(cond).append(",").append(icon);
                        
                        hourCount++;
                        pos = timeEnd;
                    }
                }
                
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR|Parse error";
            }
        }
    }
}
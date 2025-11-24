package application;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class server {

    private static final String API_KEY = "6b1eed84b2f428b079f017d13cc8a953";

    public static void main(String[] args) {
        // Ensure favorites table exists and has a uniqueness constraint to avoid duplicates
        ensureFavoritesTable();

        try (ServerSocket ss = new ServerSocket(2000)) {
            System.out.println("🌤 Weather Server started on port 5000...");

            while (true) {
                Socket connSocket = ss.accept();
                System.out.println("Client connected: " + connSocket.getInetAddress());

                // Mỗi client chạy trên 1 thread riêng
                new Thread(() -> handleClient(connSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("❌ Cannot start server: " + e.getMessage());
        }
    }

    /**
     * Ensure the favorites table exists and enforce uniqueness (username, city, country).
     * If duplicates already exist, migrate by copying distinct rows into a new table.
     */
    private static void ensureFavoritesTable() {
        String url = "jdbc:mysql://localhost:3306/weatherdb?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String pass = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement st = conn.createStatement()) {

            // If table does not exist, create it with UNIQUE constraint
            String create = "CREATE TABLE IF NOT EXISTS favorites (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(64) DEFAULT 'guest', " +
                    "city VARCHAR(200) NOT NULL, " +
                    "country CHAR(2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY ux_fav (username, city, country)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            st.execute(create);

            // If table existed previously without unique index, attempt to dedupe by creating a temp table
            // and copying distinct rows (group by username,city,country keeping earliest created_at)
            // This is safe to run even if no duplicates exist.
            String createTmp = "CREATE TABLE IF NOT EXISTS favorites_tmp (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(64) DEFAULT 'guest', " +
                    "city VARCHAR(200) NOT NULL, " +
                    "country CHAR(2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY ux_fav (username, city, country)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            st.execute(createTmp);

            // Copy distinct rows into tmp (keep the earliest created_at for each trio)
            String copyDistinct = "INSERT IGNORE INTO favorites_tmp (username, city, country, created_at) " +
                    "SELECT username, city, country, MIN(created_at) FROM favorites GROUP BY username, city, country";
            try {
                st.executeUpdate(copyDistinct);
                // Swap tables only if copy succeeded (and only if favorites_tmp contains rows)
                String countTmp = "SELECT COUNT(*) AS c FROM favorites_tmp";
                try (ResultSet rs = st.executeQuery(countTmp)) {
                    if (rs.next() && rs.getInt("c") > 0) {
                        // Rename original to backup and tmp to favorites
                        st.execute("RENAME TABLE favorites TO favorites_old, favorites_tmp TO favorites");
                        // Drop backup
                        st.execute("DROP TABLE IF EXISTS favorites_old");
                    }
                }
            } catch (SQLException ex) {
                // If something went wrong during copy (e.g., favorites table empty), ignore and continue
                System.err.println("Favorites dedupe/migrate warning: " + ex.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("Could not ensure favorites table: " + e.getMessage());
        }
    }

    private static void handleClient(Socket connSocket) {
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(connSocket.getInputStream(), StandardCharsets.UTF_8)
            );
            BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(connSocket.getOutputStream(), StandardCharsets.UTF_8)
            )
        ) {
            String request;
            while ((request = br.readLine()) != null) {
                System.out.println("🔵 Received: " + request);

                if (request.startsWith("ADD_FAV")) {
                    String[] parts = request.split(":");
                    System.out.println("🔵 ADD_FAV parts: " + java.util.Arrays.toString(parts));
                    if (parts.length >= 3) {
                        String city = parts[1].trim();
                        String country = parts[2].trim();
                        System.out.println("🔵 Adding: city='" + city + "', country='" + country + "'");
                        addFavoriteToDB(city, country);
                        bw.write("OK: Added to favorites");
                        bw.newLine(); bw.flush();
                    }
                    continue;
                } else if (request.startsWith("DEL_FAV")) {
                    String[] parts = request.split(":");
                    System.out.println("🔵 DEL_FAV parts: " + java.util.Arrays.toString(parts));
                    if (parts.length >= 3) {
                        String city = parts[1].trim();
                        String country = parts[2].trim();
                        System.out.println("🔵 Removing: city='" + city + "', country='" + country + "'");
                        removeFavoriteFromDB(city, country);
                        bw.write("OK: Removed from favorites");
                        bw.newLine(); bw.flush();
                    }
                    continue;
                } else if (request.equals("GET_FAV")) {
                    System.out.println("🔵 Processing GET_FAV...");
                    String favList = getFavoritesFromDB();
                    System.out.println("🔵 GET_FAV result: '" + favList + "'");
                    bw.write(favList);
                    bw.newLine(); bw.flush();
                    System.out.println("🔵 GET_FAV sent to client");
                    continue;
                }

                // các xử lý khác (Hanoi, coord:lat,lon, v.v)
                String jsonResponse = getWeather(request.trim());
                bw.write(jsonResponse);
                bw.newLine(); bw.flush();
            }


        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        }
    }

    private static String getWeather(String city) {
        try {
            String apiUrl;
            // Nếu client gửi theo dạng coord:lat,lon -> dùng lat/lon API
            if (city != null && city.toLowerCase().startsWith("coord:")) {
                String coordPart = city.substring(6).trim(); // lat,lon
                String[] parts = coordPart.split(",");
                if (parts.length >= 2) {
                    String lat = URLEncoder.encode(parts[0].trim(), StandardCharsets.UTF_8);
                    String lon = URLEncoder.encode(parts[1].trim(), StandardCharsets.UTF_8);
                    apiUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=vi";
                } else {
                    // fallback to city query if parsing fails
                    String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
                    apiUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + encodedCity + "&appid=" + API_KEY + "&units=metric&lang=vi";
                }
            } else {
                String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
                apiUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" + encodedCity + "&appid=" + API_KEY + "&units=metric&lang=vi";
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            InputStream is = (code == 200)
                ? conn.getInputStream()
                : conn.getErrorStream();

            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();
            return json;

        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
    private static void addFavoriteToDB(String city, String country) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/weatherdb?useSSL=false&allowPublicKeyRetrieval=true",
                "root", "123456")) {

            String sql = "INSERT IGNORE INTO favorites(username, city, country) VALUES ('default', ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, city);
            ps.setString(2, country);
            int updated = ps.executeUpdate();
            System.out.println("✅ DB insert, affected=" + updated + ", city='" + city + "', country='" + country + "'");

        } catch (SQLException e) {
            System.err.println("❌ DB insert error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void removeFavoriteFromDB(String city, String country) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/weatherdb?useSSL=false&allowPublicKeyRetrieval=true",
                "root", "123456")) {

            String sql = "DELETE FROM favorites WHERE username='default' AND city = ? AND country = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, city);
            ps.setString(2, country);
            int deleted = ps.executeUpdate();
            System.out.println("✅ DB delete, affected=" + deleted + ", city='" + city + "', country='" + country + "'");

        } catch (SQLException e) {
            System.err.println("❌ DB delete error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static String getFavoritesFromDB() {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/weatherdb?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456")) {
            Statement st = conn.createStatement();
            System.out.println("📊 Querying favorites from DB...");
            ResultSet rs = st.executeQuery("SELECT city, country FROM favorites WHERE username='default' ORDER BY created_at DESC");
            boolean first = true;
            int count = 0;
            while (rs.next()) {
                String city = rs.getString("city");
                String country = rs.getString("country");
                System.out.println("📊 Row " + (++count) + ": city='" + city + "', country='" + country + "'");
                if (!first) {
                    sb.append("|");
                }
                sb.append(city).append(",").append(country);
                first = false;
            }
            System.out.println("📊 Total rows: " + count);
        } catch (SQLException e) {
            System.err.println("❌ DB query error: " + e.getMessage());
            e.printStackTrace();
        }
        String result = sb.toString();
        System.out.println("📊 GET_FAV returning: '" + result + "'");
        return result;
    }

}

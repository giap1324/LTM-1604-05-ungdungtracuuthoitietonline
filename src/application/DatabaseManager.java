package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/weather_app?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root"; // Thay đổi theo username của bạn
    private static final String DB_PASSWORD = "123456"; // Thay đổi theo password của bạn
    
    private Connection connection;
    
    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean addFavorite(String cityName, String country, double lat, double lon) {
        String sql = "INSERT INTO favorite_cities (city_name, country, latitude, longitude) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE added_date = CURRENT_TIMESTAMP";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, cityName);
            pstmt.setString(2, country);
            pstmt.setDouble(3, lat);
            pstmt.setDouble(4, lon);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding favorite: " + e.getMessage());
            return false;
        }
    }
    
    public boolean removeFavorite(String cityName, String country) {
        String sql = "DELETE FROM favorite_cities WHERE city_name = ? AND country = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, cityName);
            pstmt.setString(2, country);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error removing favorite: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isFavorite(String cityName, String country) {
        String sql = "SELECT COUNT(*) FROM favorite_cities WHERE city_name = ? AND country = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, cityName);
            pstmt.setString(2, country);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking favorite: " + e.getMessage());
        }
        return false;
    }
    
    public List<FavoriteCity> getAllFavorites() {
        List<FavoriteCity> favorites = new ArrayList<>();
        String sql = "SELECT * FROM favorite_cities ORDER BY added_date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                favorites.add(new FavoriteCity(
                    rs.getString("city_name"),
                    rs.getString("country"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getTimestamp("added_date")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting favorites: " + e.getMessage());
        }
        return favorites;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    public static class FavoriteCity {
        public String cityName;
        public String country;
        public double latitude;
        public double longitude;
        public Timestamp addedDate;
        
        public FavoriteCity(String cityName, String country, double lat, double lon, Timestamp addedDate) {
            this.cityName = cityName;
            this.country = country;
            this.latitude = lat;
            this.longitude = lon;
            this.addedDate = addedDate;
        }
        
        public String getDisplayName() {
            return cityName + ", " + country;
        }
        
        public String getCoordinates() {
            return "LAT:" + latitude + "," + longitude;
        }
    }
}
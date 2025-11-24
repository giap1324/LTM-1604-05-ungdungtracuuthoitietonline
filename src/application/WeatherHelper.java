package application;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javafx.scene.paint.LinearGradient;

/**
 * Helper class
 * (Phiên bản nâng cấp: Thêm formatDate và getHourOfDay)
 */
public class WeatherHelper {
    
    // (Các hàm getWeatherIconUrl, getWeatherBackground, isDayTime, capitalizeFirst, 
    // formatTemperature, formatTemperatureDecimal, formatWindSpeed, 
    // formatPressure, formatHumidity, formatVisibility giữ nguyên)
    
    public static String getWeatherIconUrl(String iconCode) {
        if (iconCode == null || iconCode.isEmpty()) {
            return "https://openweathermap.org/img/wn/02d@2x.png";
        }
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }
    public static String getWeatherBackground(String mainWeather, boolean isDay) {
        if (mainWeather == null) { return isDay ? "-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 50%, #ed4264 100%);" : "-fx-background-color: linear-gradient(to bottom right, #2c3e50 0%, #3498db 50%, #8e44ad 100%);"; }
        switch (mainWeather.toLowerCase()) {
            case "clear": return isDay ? "-fx-background-color: linear-gradient(to bottom right, #36d1dc 0%, #5b86e5 100%);" : "-fx-background-color: linear-gradient(to bottom right, #0f2027 0%, #203a43 50%, #2c5364 100%);";
            case "clouds": return isDay ? "-fx-background-color: linear-gradient(to bottom right, #bdc3c7 0%, #2c3e50 100%);" : "-fx-background-color: linear-gradient(to bottom right, #485563 0%, #29323c 100%);";
            case "rain": return "-fx-background-color: linear-gradient(to bottom right, #636363 0%, #a2ab58 100%);";
            case "drizzle": return "-fx-background-color: linear-gradient(to bottom right, #89f7fe 0%, #66a6ff 100%);";
            case "thunderstorm": return "-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 100%);";
            case "snow": return "-fx-background-color: linear-gradient(to bottom right, #e6dada 0%, #274046 100%);";
            case "mist": case "fog": case "haze": return "-fx-background-color: linear-gradient(to bottom right, #bdc3c7 0%, #2c3e50 100%);";
            default: return isDay ? "-fx-background-color: linear-gradient(to bottom right, #667eea 0%, #764ba2 50%, #ed4264 100%);" : "-fx-background-color: linear-gradient(to bottom right, #2c3e50 0%, #3498db 50%, #8e44ad 100%);";
        }
    }
    public static boolean isDayTime(long sunrise, long sunset) {
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime > sunrise && currentTime < sunset;
    }
    public static String capitalizeFirst(String text) { if (text == null || text.isEmpty()) return text; return text.substring(0, 1).toUpperCase() + text.substring(1); }
    public static String formatTemperature(double temp) { return String.format("%.0f°C", temp); }
    public static String formatTemperatureDecimal(double temp) { return String.format("%.1f°C", temp); }
    public static String formatTemperatureF(double celsius) { double f = celsius * 9.0/5.0 + 32.0; return String.format("%.0f°F", f); }
    public static String formatTemperatureDecimalF(double celsius) { double f = celsius * 9.0/5.0 + 32.0; return String.format("%.1f°F", f); }
    public static String formatWindSpeed(double speed) { return String.format("%.1f m/s", speed); }
    public static String formatPressure(int pressure) { return pressure + " hPa"; }
    public static String formatHumidity(int humidity) { return humidity + "%"; }
    public static String formatVisibility(int visibilityInMeters) { return String.format("%.1f km", visibilityInMeters / 1000.0); }
    
    /**
     * HÀM SỬA LẠI: Format thời gian (sunrise/sunset) với múi giờ
     */
    public static String formatTime(long unixTimestamp, int timezoneShift) {
        return formatTimestamp(unixTimestamp, timezoneShift, "HH:mm");
    }

    /**
     * HÀM MỚI: Format giờ (cho dự báo)
     */
    public static String formatHour(long unixTimestamp, int timezoneShift) {
        return formatTimestamp(unixTimestamp, timezoneShift, "HH:00");
    }
    
    /**
     * HÀM MỚI: Format ngày (cho dự báo 5 ngày)
     */
    public static String formatDate(long unixTimestamp, int timezoneShift) {
        return formatTimestamp(unixTimestamp, timezoneShift, "dd/MM");
    }

    /**
     * Return a localized weekday label for a unix timestamp in the given timezone.
     * Returns "Hôm nay" if the date equals today's date in that timezone, otherwise
     * returns Vietnamese weekday names (e.g. "Thứ 2", "Thứ 3", ..., "Chủ nhật").
     */
    public static String getWeekday(long unixTimestamp, int timezoneShift) {
        Date date = new Date(unixTimestamp * 1000L);
        int hours = timezoneShift / 3600;
        int minutes = Math.abs((timezoneShift % 3600) / 60);
        String gmtOffset = String.format("GMT%+d:%02d", hours, minutes);

        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(gmtOffset));
        cal.setTime(date);

        java.util.Calendar now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(gmtOffset));

        boolean sameDay = cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
                && cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR);
        if (sameDay) return "Hôm nay";

        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK); // 1=Sunday
        switch (dow) {
            case java.util.Calendar.SUNDAY: return "Chủ nhật";
            case java.util.Calendar.MONDAY: return "Thứ 2";
            case java.util.Calendar.TUESDAY: return "Thứ 3";
            case java.util.Calendar.WEDNESDAY: return "Thứ 4";
            case java.util.Calendar.THURSDAY: return "Thứ 5";
            case java.util.Calendar.FRIDAY: return "Thứ 6";
            case java.util.Calendar.SATURDAY: return "Thứ 7";
            default: return "";
        }
    }
    
    /**
     * HÀM MỚI: Lấy giờ trong ngày (để kiểm tra)
     */
    public static String getHourOfDay(long unixTimestamp, int timezoneShift) {
        return formatTimestamp(unixTimestamp, timezoneShift, "HH");
    }
    
    
    /**
     * HÀM TRỢ GIÚP CHUNG: Format một timestamp
     */
    private static String formatTimestamp(long unixTimestamp, int timezoneShift, String pattern) {
        Date date = new Date(unixTimestamp * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        
        String gmtOffset;
        if (timezoneShift == 0) {
            gmtOffset = "GMT";
        } else {
            int hours = timezoneShift / 3600;
            int minutes = Math.abs((timezoneShift % 3600) / 60);
            gmtOffset = String.format("GMT%+d:%02d", hours, minutes);
        }
        
        sdf.setTimeZone(TimeZone.getTimeZone(gmtOffset));
        return sdf.format(date);
    }
	public static LinearGradient getWeatherGradient(String mainWeather, boolean isDay) {
		// TODO Auto-generated method stub
		return null;
	}
}
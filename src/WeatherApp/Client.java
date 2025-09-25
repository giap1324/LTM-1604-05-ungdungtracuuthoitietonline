package WeatherApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Random;

import java.awt.image.BufferedImage;
import java.awt.BasicStroke;
import java.awt.GradientPaint;

public class Client extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2000;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private JTextField cityTextField;
    private JButton searchButton, pingButton, connectButton, disconnectButton;

    private JLabel cityLabel, temperatureLabel, descriptionLabel, humidityLabel, windSpeedLabel, timestampLabel;
    private JLabel connectionStatusLabel;

    private JLabel[] forecastLabels = new JLabel[5];
    private WeatherIconPanel weatherIconPanel;

    private boolean isConnected = false;

    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(64, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);
    private static final Color BACKGROUND_COLOR = new Color(250, 250, 252);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(33, 37, 41);
    private static final Color TEXT_SECONDARY = new Color(108, 117, 125);
    
    public Client() {
        setTitle("🌤️ Weather Station Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 850);
        setLocationRelativeTo(null);
        setBackground(BACKGROUND_COLOR);

        // Modern Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }

        createComponents();
        layoutComponents();
        setupEventHandlers();
        updateConnectionStatus();
    }

    private void createComponents() {
        cityTextField = new JTextField(20);
        cityTextField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cityTextField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        searchButton = createModernButton("🔍 Tìm kiếm", PRIMARY_COLOR, false);
        pingButton = createModernButton("📡 Kiểm tra", new Color(108, 117, 125), false);
        connectButton = createModernButton("🔗 Kết nối", SUCCESS_COLOR, false);
        disconnectButton = createModernButton("🔌 Ngắt kết nối", ERROR_COLOR, true);

        cityLabel = createInfoLabel("---");
        temperatureLabel = createTemperatureLabel("---");
        descriptionLabel = createInfoLabel("---");
        humidityLabel = createInfoLabel("---");
        windSpeedLabel = createInfoLabel("---");
        timestampLabel = createTimestampLabel("---");

        for (int i = 0; i < 5; i++) {
            forecastLabels[i] = createForecastLabel("---");
        }

        connectionStatusLabel = new JLabel("⌛ Chưa kết nối");
        connectionStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        connectionStatusLabel.setForeground(ERROR_COLOR);

        weatherIconPanel = new WeatherIconPanel();
    }

    private JButton createModernButton(String text, Color color, boolean disabled) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setEnabled(!disabled);
        
        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            Color original = btn.getBackground();
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(original.brighter());
                }
            }
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(original);
                }
            }
        });
        
        return btn;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private JLabel createTemperatureLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 28));
        label.setForeground(PRIMARY_COLOR);
        return label;
    }

    private JLabel createTimestampLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    private JLabel createForecastLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        label.setOpaque(true);
        label.setBackground(CARD_COLOR);
        return label;
    }

    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        
        // Content Panel
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(BACKGROUND_COLOR);
        
        JPanel weatherPanel = createWeatherDisplayPanel();
        JPanel forecastPanel = createForecastDisplayPanel();

        contentPanel.add(weatherPanel, BorderLayout.CENTER);
        contentPanel.add(forecastPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 15));
        headerPanel.setBackground(BACKGROUND_COLOR);

        // Connection Panel
        JPanel connectionPanel = createCardPanel();
        connectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        connectionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                "Trạng thái kết nối",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_SECONDARY
            ),
            new EmptyBorder(10, 15, 15, 15)
        ));

        connectionPanel.add(connectionStatusLabel);
        connectionPanel.add(Box.createHorizontalStrut(10));
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(pingButton);

        // Search Panel
        JPanel searchPanel = createCardPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                "Tìm kiếm thời tiết",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_SECONDARY
            ),
            new EmptyBorder(10, 15, 15, 15)
        ));

        JLabel searchLabel = new JLabel("Tên thành phố:");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchLabel.setForeground(TEXT_PRIMARY);
        
        searchPanel.add(searchLabel);
        searchPanel.add(cityTextField);
        searchPanel.add(searchButton);

        headerPanel.add(connectionPanel, BorderLayout.NORTH);
        headerPanel.add(searchPanel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            new EmptyBorder(0, 0, 0, 0)
        ));
        return panel;
    }

    private JPanel createWeatherDisplayPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                "🌤️ Thông tin thời tiết hiện tại",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                TEXT_SECONDARY
            ),
            new EmptyBorder(20, 25, 25, 25)
        ));

        // Left side - Weather info
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(CARD_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;

        String[] labels = {"🏙️ Thành phố:", "🌡️ Nhiệt độ:", "☁️ Mô tả:", "💧 Độ ẩm:", "🌬️ Tốc độ gió:", "⏰ Cập nhật:"};
        JLabel[] valueLabels = {cityLabel, temperatureLabel, descriptionLabel, humidityLabel, windSpeedLabel, timestampLabel};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            infoPanel.add(createBoldLabel(labels[i]), gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            infoPanel.add(valueLabels[i], gbc);
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
        }

        // Right side - Weather icon
        weatherIconPanel.setPreferredSize(new Dimension(200, 200));
        weatherIconPanel.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(weatherIconPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createForecastDisplayPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new GridLayout(1, 5, 10, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                "📅 Dự báo 5 ngày",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                TEXT_SECONDARY
            ),
            new EmptyBorder(15, 20, 20, 20)
        ));

        for (int i = 0; i < 5; i++) {
            JPanel dayPanel = new JPanel(new BorderLayout());
            dayPanel.setBackground(BACKGROUND_COLOR);
            dayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(10, 8, 10, 8)
            ));
            
            forecastLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
            dayPanel.add(forecastLabels[i], BorderLayout.CENTER);
            panel.add(dayPanel);
        }
        
        return panel;
    }

    private void setupEventHandlers() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { disconnect(); }
        });

        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnect());
        searchButton.addActionListener(e -> searchWeather());
        pingButton.addActionListener(e -> pingServer());
        cityTextField.addActionListener(e -> { if (isConnected) searchWeather(); });
    }

    private void connectToServer() {
        connectButton.setEnabled(false);
        connectButton.setText("🔄 Đang kết nối...");
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                String welcome1 = input.readLine();
                input.readLine(); // skip line
                if ("WEATHER_SERVER_CONNECTED".equals(welcome1)) isConnected = true;

                SwingUtilities.invokeLater(() -> {
                    updateConnectionStatus();
                    cityTextField.requestFocus();
                    fetchWeatherCurrentLocation(); // gọi weather hiện tại
                });
            } catch (Exception e) {
                isConnected = false;
                SwingUtilities.invokeLater(this::updateConnectionStatus);
            } finally {
                SwingUtilities.invokeLater(() -> connectButton.setText("🔗 Kết nối"));
            }
        }).start();
    }

    private void disconnect() {
        try {
            if (isConnected && output != null) output.println("QUIT");
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        isConnected = false;
        updateConnectionStatus();
        clearWeatherDisplay();
    }

    private void searchWeather() {
        String city = cityTextField.getText().trim();
        if (city.isEmpty() || !isConnected) return;

        new Thread(() -> {
            try {
                output.println("WEATHER:" + city);
                String response = input.readLine();
                if (response != null && response.startsWith("SUCCESS:")) {
                    displayWeatherInfo(response.substring(8));
                    output.println("WEATHER_FORECAST:" + city);
                    String forecastResponse = input.readLine();
                    if (forecastResponse != null && forecastResponse.startsWith("FORECAST_SUCCESS:")) {
                        displayForecastInfo(forecastResponse.substring(17));
                    }
                } else if (response != null && response.startsWith("ERROR:")) {
                    JOptionPane.showMessageDialog(this, response.substring(6), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void fetchWeatherCurrentLocation() {
        if (!isConnected) return;
        new Thread(() -> {
            try {
                output.println("WEATHER_CURRENT");
                String response = input.readLine();
                if (response != null && response.startsWith("SUCCESS:")) {
                    SwingUtilities.invokeLater(() -> {
                        displayWeatherInfo(response.substring(8));
                        
                        // Bây giờ cityLabel đã có city hợp lệ
                        String city = cityLabel.getText().split(",")[0];
                        requestForecast(city);
                    });
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void requestForecast(String city) {
        new Thread(() -> {
            try {
                output.println("WEATHER_FORECAST:" + city);
                String forecastResponse = input.readLine();
                if (forecastResponse != null && forecastResponse.startsWith("FORECAST_SUCCESS:")) {
                    displayForecastInfo(forecastResponse.substring(17));
                }
            } catch (IOException ignored) {}
        }).start();
    }


    private void pingServer() {
        if (!isConnected) return;
        new Thread(() -> {
            try {
                output.println("PING");
                input.readLine(); // PONG
            } catch (IOException ignored) {}
        }).start();
    }

    private void displayWeatherInfo(String data) {
        String city = "", temp = "", desc = "", humidity = "", wind = "";
        String[] parts = data.split("\\|");

        for (String p : parts) {
            if (p.startsWith("CITY:")) city = p.substring(5);
            else if (p.startsWith("TEMP:")) temp = p.substring(5);
            else if (p.startsWith("DESC:")) desc = p.substring(5);
            else if (p.startsWith("HUMIDITY:")) humidity = p.substring(9);
            else if (p.startsWith("WIND:")) wind = p.substring(5);
        }

        final String fCity = city;
        final String fTemp = temp;
        final String fDesc = desc;
        final String fHumidity = humidity;
        final String fWind = wind;

        SwingUtilities.invokeLater(() -> {
            cityLabel.setText(fCity);
            temperatureLabel.setText(fTemp + "°C");
            descriptionLabel.setText(fDesc.isEmpty() ? "---" : Character.toUpperCase(fDesc.charAt(0)) + fDesc.substring(1));
            humidityLabel.setText(fHumidity + "%");
            windSpeedLabel.setText(fWind + " m/s");
            timestampLabel.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
            weatherIconPanel.setWeatherDescription(fDesc);
        });
    }

    private void displayForecastInfo(String data) {
        // data: "dd/MM/yyyy|tempMin|tempMax|description;..."
        String[] days = data.split(";");
        for (int i = 0; i < days.length && i < 5; i++) {
            String[] parts = days[i].split("\\|");
            if (parts.length >= 4) {
                String date = parts[0];
                String tempMin = parts[1] + "°C";
                String tempMax = parts[2] + "°C";
                String desc = parts[3];
                
                final String text = "<html><center>" + date + "<br>"
                                    + tempMin + "<br>"
                                    + tempMax + "<br>"
                                    + desc + "</center></html>";
                final int idx = i;
                SwingUtilities.invokeLater(() -> forecastLabels[idx].setText(text));
            }
        }
    }


    private void updateConnectionStatus() {
        if (isConnected) {
            connectionStatusLabel.setText("✅ Đã kết nối");
            connectionStatusLabel.setForeground(SUCCESS_COLOR);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            searchButton.setEnabled(true);
            pingButton.setEnabled(true);
        } else {
            connectionStatusLabel.setText("⌛ Chưa kết nối");
            connectionStatusLabel.setForeground(ERROR_COLOR);
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            searchButton.setEnabled(false);
            pingButton.setEnabled(false);
        }
    }

    private void clearWeatherDisplay() {
        cityLabel.setText("---");
        temperatureLabel.setText("---");
        descriptionLabel.setText("---");
        humidityLabel.setText("---");
        windSpeedLabel.setText("---");
        timestampLabel.setText("---");
        for (JLabel lbl : forecastLabels) lbl.setText("---");
        weatherIconPanel.setWeatherDescription("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }

    class WeatherIconPanel extends JPanel {
        private String weatherDesc = "";
        private String weatherType = null;

        private Image sunImage;
        private Image cloudImage;
        private Image rainImage;
        private Image snowImage;

        public WeatherIconPanel() {
            setPreferredSize(new Dimension(200, 200));
            setBackground(new Color(240, 248, 255));
            loadIcons();
        }

        private void loadIcons() {
            try {
                // Thử load từ resources folder
                ClassLoader classLoader = getClass().getClassLoader();
                
                // Cách 1: Thử load từ resources
                try {
                    sunImage = new ImageIcon(classLoader.getResource("icons/sun.png")).getImage();
                    cloudImage = new ImageIcon(classLoader.getResource("icons/cloud.png")).getImage();
                    rainImage = new ImageIcon(classLoader.getResource("icons/rain.png")).getImage();
                    snowImage = new ImageIcon(classLoader.getResource("icons/snow.png")).getImage();
                    System.out.println("✅ Đã load thành công các icon từ resources");
                } catch (Exception e1) {
                    System.out.println("❌ Không thể load từ resources: " + e1.getMessage());
                    
                    // Cách 2: Thử load từ thư mục hiện tại
                    try {
                        sunImage = new ImageIcon("icons/sun.png").getImage();
                        cloudImage = new ImageIcon("icons/cloud.png").getImage();
                        rainImage = new ImageIcon("icons/rain.png").getImage();
                        snowImage = new ImageIcon("icons/snow.png").getImage();
                        System.out.println("✅ Đã load thành công các icon từ thư mục hiện tại");
                    } catch (Exception e2) {
                        System.out.println("❌ Không thể load từ thư mục hiện tại: " + e2.getMessage());
                        
                        // Cách 3: Tạo icon mặc định nếu không load được
                        createDefaultIcons();
                        System.out.println("✅ Đã tạo các icon mặc định");
                    }
                }
            } catch (Exception e) {
                createDefaultIcons();
                System.out.println("⚠️ Sử dụng icon mặc định do lỗi: " + e.getMessage());
            }
        }

        private void createDefaultIcons() {
            // Tạo các icon đơn giản bằng code khi không load được file
            int size = 150;
            
            // Icon mặt trời
            sunImage = createDefaultSunIcon(size);
            
            // Icon mây
            cloudImage = createDefaultCloudIcon(size);
            
            // Icon mưa
            rainImage = createDefaultRainIcon(size);
            
            // Icon tuyết
            snowImage = createDefaultSnowIcon(size);
        }

        private Image createDefaultSunIcon(int size) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ mặt trời
            g2.setColor(new Color(255, 215, 0)); // Màu vàng
            int centerX = size / 2;
            int centerY = size / 2;
            int radius = size / 3;
            
            // Vẽ tia nắng
            g2.setStroke(new BasicStroke(3));
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int x1 = centerX + (int)(radius * 1.2 * Math.cos(angle));
                int y1 = centerY + (int)(radius * 1.2 * Math.sin(angle));
                int x2 = centerX + (int)(radius * 1.6 * Math.cos(angle));
                int y2 = centerY + (int)(radius * 1.6 * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
            }
            
            // Vẽ mặt trời
            g2.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            
            // Vẽ mặt cười
            g2.setColor(Color.ORANGE);
            g2.setStroke(new BasicStroke(2));
            // Mắt
            g2.fillOval(centerX - radius/3, centerY - radius/4, 8, 8);
            g2.fillOval(centerX + radius/3 - 8, centerY - radius/4, 8, 8);
            // Miệng
            g2.drawArc(centerX - radius/3, centerY, radius*2/3, radius/2, 0, -180);
            
            g2.dispose();
            return img;
        }

        private Image createDefaultCloudIcon(int size) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ mây
            g2.setColor(new Color(220, 220, 220));
            int centerX = size / 2;
            int centerY = size / 2;
            
            // Vẽ các hình tròn tạo thành mây
            g2.fillOval(centerX - 40, centerY - 10, 50, 40);
            g2.fillOval(centerX - 20, centerY - 25, 60, 50);
            g2.fillOval(centerX + 10, centerY - 15, 45, 35);
            g2.fillOval(centerX - 10, centerY - 5, 40, 30);
            
            g2.dispose();
            return img;
        }

        private Image createDefaultRainIcon(int size) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ mây
            g2.setColor(new Color(150, 150, 150));
            int centerX = size / 2;
            int centerY = size / 3;
            
            g2.fillOval(centerX - 40, centerY - 10, 50, 40);
            g2.fillOval(centerX - 20, centerY - 25, 60, 50);
            g2.fillOval(centerX + 10, centerY - 15, 45, 35);
            
            // Vẽ giọt mưa
            g2.setColor(new Color(100, 150, 255));
            g2.setStroke(new BasicStroke(2));
            for (int i = 0; i < 6; i++) {
                int x = centerX - 30 + i * 12;
                int y1 = centerY + 20;
                int y2 = centerY + 50;
                g2.drawLine(x, y1, x, y2);
                g2.fillOval(x - 2, y2 - 4, 4, 8);
            }
            
            g2.dispose();
            return img;
        }

        private Image createDefaultSnowIcon(int size) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ mây
            g2.setColor(new Color(200, 200, 200));
            int centerX = size / 2;
            int centerY = size / 3;
            
            g2.fillOval(centerX - 40, centerY - 10, 50, 40);
            g2.fillOval(centerX - 20, centerY - 25, 60, 50);
            g2.fillOval(centerX + 10, centerY - 15, 45, 35);
            
            // Vẽ bông tuyết
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1));
            for (int i = 0; i < 8; i++) {
                int x = centerX - 35 + (i % 4) * 20;
                int y = centerY + 25 + (i / 4) * 20;
                drawSnowflake(g2, x, y, 8);
            }
            
            g2.dispose();
            return img;
        }

        private void drawSnowflake(Graphics2D g2, int centerX, int centerY, int size) {
            // Vẽ bông tuyết 6 cánh
            for (int i = 0; i < 6; i++) {
                double angle = i * Math.PI / 3;
                int x1 = centerX + (int)(size * Math.cos(angle));
                int y1 = centerY + (int)(size * Math.sin(angle));
                g2.drawLine(centerX, centerY, x1, y1);
                
                // Vẽ cành nhỏ
                int x2 = centerX + (int)(size * 0.6 * Math.cos(angle));
                int y2 = centerY + (int)(size * 0.6 * Math.sin(angle));
                int x3 = x2 + (int)(size * 0.3 * Math.cos(angle + Math.PI/4));
                int y3 = y2 + (int)(size * 0.3 * Math.sin(angle + Math.PI/4));
                int x4 = x2 + (int)(size * 0.3 * Math.cos(angle - Math.PI/4));
                int y4 = y2 + (int)(size * 0.3 * Math.sin(angle - Math.PI/4));
                g2.drawLine(x2, y2, x3, y3);
                g2.drawLine(x2, y2, x4, y4);
            }
            
            // Vẽ tâm
            g2.fillOval(centerX - 2, centerY - 2, 4, 4);
        }

        public void setWeatherDescription(String desc) {
            this.weatherDesc = desc == null ? "" : desc.toLowerCase();
            this.weatherType = mapWeatherDescToType(this.weatherDesc);
            repaint();
        }

        private String mapWeatherDescToType(String desc) {
            if (desc == null || desc.isEmpty()) return null;
            
            desc = desc.toLowerCase();
            
            // Kiểm tra các từ khóa tiếng Việt
            if (desc.contains("nắng") || desc.contains("quang") || desc.contains("trong") || 
                desc.contains("sun") || desc.contains("clear") || desc.contains("sunny")) {
                return "sun";
            }
            
            if (desc.contains("mây") || desc.contains("âm u") || desc.contains("cloud") || 
                desc.contains("cloudy") || desc.contains("overcast")) {
                return "cloud";
            }
            
            if (desc.contains("mưa") || desc.contains("rain") || desc.contains("drizzle") || 
                desc.contains("shower") || desc.contains("phùn") || desc.contains("rào")) {
                return "rain";
            }
            
            if (desc.contains("tuyết") || desc.contains("snow") || desc.contains("sleet") || 
                desc.contains("băng")) {
                return "snow";
            }
            
            return "cloud"; // Mặc định hiển thị mây nếu không khớp
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isConnected) {
                drawNoConnection(g2);
                return;
            }

            if (weatherType == null || weatherDesc.isEmpty()) {
                drawNoWeatherData(g2);
                return;
            }

            // Vẽ background gradient
            GradientPaint gradient = new GradientPaint(0, 0, new Color(240, 248, 255), 
                                                      0, getHeight(), new Color(220, 235, 250));
            g2.setPaint(gradient);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Chọn và vẽ icon thời tiết
            Image icon = null;
            switch (weatherType) {
                case "sun":
                    icon = sunImage;
                    break;
                case "cloud":
                    icon = cloudImage;
                    break;
                case "rain":
                    icon = rainImage;
                    break;
                case "snow":
                    icon = snowImage;
                    break;
            }

            if (icon != null) {
                // Vẽ icon ở giữa panel với kích thước phù hợp
                int iconSize = Math.min(getWidth(), getHeight()) - 20;
                int x = (getWidth() - iconSize) / 2;
                int y = (getHeight() - iconSize) / 2;
                
                g2.drawImage(icon, x, y, iconSize, iconSize, this);
            } else {
                drawNoIcon(g2);
            }

            // Vẽ text mô tả thời tiết
            g2.setColor(TEXT_SECONDARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            String displayText = weatherDesc.length() > 15 ? 
                                weatherDesc.substring(0, 15) + "..." : weatherDesc;
            FontMetrics fm = g2.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(displayText)) / 2;
            int textY = getHeight() - 10;
            g2.drawString(displayText, textX, textY);
        }

        private void drawNoConnection(Graphics2D g2) {
            g2.setColor(new Color(200, 200, 200));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            String text = "Chưa kết nối";
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g2.drawString(text, x, y);
        }

        private void drawNoWeatherData(Graphics2D g2) {
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            String text = "Không có dữ liệu";
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g2.drawString(text, x, y);
        }

        private void drawNoIcon(Graphics2D g2) {
            g2.setColor(new Color(180, 180, 180));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            String text = "🌤️";
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g2.drawString(text, x, y);
        }
    }}
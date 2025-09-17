package WeatherApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2000;
    
    // Network components
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    
    // GUI components
    private JTextField cityTextField;
    private JButton searchButton;
    private JButton pingButton;
    private JButton connectButton;
    private JButton disconnectButton;
    
    // Weather display components
    private JLabel cityLabel;
    private JLabel temperatureLabel;
    private JLabel descriptionLabel;
    private JLabel humidityLabel;
    private JLabel windSpeedLabel;
    private JLabel timestampLabel;
    private JLabel connectionStatusLabel;
    
    // Log area
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    
    private boolean isConnected = false;

    public Client() {
        initializeGUI();
        setupEventHandlers();
    }

    private void initializeGUI() {
        setTitle("🌤️ Weather Client - GUI Version");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        createComponents();
        layoutComponents();
        updateConnectionStatus();
    }
    
    private void createComponents() {
        // Input components
        cityTextField = new JTextField(20);
        cityTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        searchButton = new JButton("🔍 Tra cứu thời tiết");
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        searchButton.setEnabled(false);
        
        pingButton = new JButton("📡 Test kết nối");
        pingButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pingButton.setEnabled(false);
        
        connectButton = new JButton("🔗 Kết nối");
        connectButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        connectButton.setBackground(new Color(76, 175, 80));
        connectButton.setForeground(Color.WHITE);
        
        disconnectButton = new JButton("🔌 Ngắt kết nối");
        disconnectButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        disconnectButton.setBackground(new Color(244, 67, 54));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setEnabled(false);
        
        // Weather display components
        cityLabel = createInfoLabel("---");
        temperatureLabel = createInfoLabel("---");
        descriptionLabel = createInfoLabel("---");
        humidityLabel = createInfoLabel("---");
        windSpeedLabel = createInfoLabel("---");
        timestampLabel = createInfoLabel("---");
        
        connectionStatusLabel = new JLabel("❌ Chưa kết nối");
        connectionStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        connectionStatusLabel.setForeground(Color.RED);
        
        // Log area
        logArea = new JTextArea(10, 50);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        appendLog("🌤️ Weather Client GUI khởi động");
        appendLog("Nhấn 'Kết nối' để bắt đầu...");
    }
    
    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Main container with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Top panel - Connection and input
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        
        // Connection panel
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.setBorder(new TitledBorder("Kết nối Server"));
        connectionPanel.add(connectionStatusLabel);
        connectionPanel.add(Box.createHorizontalStrut(20));
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(pingButton);
        
        // Input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.setBorder(new TitledBorder("Tra cứu thời tiết"));
        inputPanel.add(new JLabel("Tên thành phố:"));
        inputPanel.add(cityTextField);
        inputPanel.add(searchButton);
        
        topPanel.add(connectionPanel, BorderLayout.NORTH);
        topPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // Center panel - Weather display
        JPanel weatherPanel = createWeatherDisplayPanel();
        
        // Bottom panel - Log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Log hoạt động"));
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Add to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(weatherPanel, BorderLayout.CENTER);
        mainPanel.add(logPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createWeatherDisplayPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder("🌤️ Thông tin thời tiết"));
        panel.setBackground(new Color(245, 245, 245));
        
        // Weather info grid
        JPanel gridPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 1: City
        gbc.gridx = 0; gbc.gridy = 0;
        gridPanel.add(createBoldLabel("🏙️ Thành phố:"), gbc);
        gbc.gridx = 1;
        gridPanel.add(cityLabel, gbc);
        
        // Row 2: Temperature
        gbc.gridx = 0; gbc.gridy = 1;
        gridPanel.add(createBoldLabel("🌡️ Nhiệt độ:"), gbc);
        gbc.gridx = 1;
        gridPanel.add(temperatureLabel, gbc);
        
        // Row 3: Description
        gbc.gridx = 0; gbc.gridy = 2;
        gridPanel.add(createBoldLabel("☁️ Mô tả:"), gbc);
        gbc.gridx = 1;
        gridPanel.add(descriptionLabel, gbc);
        
        // Row 4: Humidity
        gbc.gridx = 0; gbc.gridy = 3;
        gridPanel.add(createBoldLabel("💧 Độ ẩm:"), gbc);
        gbc.gridx = 1;
        gridPanel.add(humidityLabel, gbc);
        
        // Row 5: Wind Speed
        gbc.gridx = 0; gbc.gridy = 4;
        gridPanel.add(createBoldLabel("🌬️ Tốc độ gió:"), gbc);
        gbc.gridx = 1;
        gridPanel.add(windSpeedLabel, gbc);
        
        // Row 6: Timestamp
        gbc.gridx = 0; gbc.gridy = 5;
        gridPanel.add(createBoldLabel("⏰ Thời gian:"), gbc);
        gbc.gridx = 1;
        gridPanel.add(timestampLabel, gbc);
        
        panel.add(Box.createVerticalStrut(10));
        panel.add(gridPanel);
        panel.add(Box.createVerticalStrut(10));
        
        return panel;
    }
    
    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        return label;
    }
    
    private void setupEventHandlers() {
        // Window close event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                System.exit(0);
            }
        });
        
        // Connect button
        connectButton.addActionListener(e -> connectToServer());
        
        // Disconnect button
        disconnectButton.addActionListener(e -> disconnect());
        
        // Search button
        searchButton.addActionListener(e -> searchWeather());
        
        // Ping button
        pingButton.addActionListener(e -> pingServer());
        
        // Enter key in text field
        cityTextField.addActionListener(e -> {
            if (isConnected) {
                searchWeather();
            }
        });
    }
    
    private void connectToServer() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(false);
            connectButton.setText("🔄 Đang kết nối...");
        });
        
        // Connect in background thread
        new Thread(() -> {
            try {
                appendLog("🔗 Đang kết nối tới Weather Server...");
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                
                // Read welcome messages
                String welcome1 = input.readLine();
                String welcome2 = input.readLine();
                
                if ("WEATHER_SERVER_CONNECTED".equals(welcome1)) {
                    isConnected = true;
                    appendLog("✅ " + welcome2);
                    
                    SwingUtilities.invokeLater(() -> {
                        updateConnectionStatus();
                        cityTextField.requestFocus();
                    });
                } else {
                    appendLog("❌ Không thể kết nối tới Weather Server");
                }
                
            } catch (ConnectException e) {
                appendLog("❌ Không thể kết nối tới server. Kiểm tra server đã chạy chưa?");
            } catch (IOException e) {
                appendLog("❌ Lỗi kết nối: " + e.getMessage());
            }
            
            SwingUtilities.invokeLater(() -> {
                connectButton.setText("🔗 Kết nối");
                if (!isConnected) {
                    connectButton.setEnabled(true);
                }
            });
        }).start();
    }
    
    private void disconnect() {
        if (isConnected) {
            try {
                if (output != null) {
                    output.println("QUIT");
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
            
            isConnected = false;
            appendLog("🔌 Đã ngắt kết nối");
            
        } catch (IOException e) {
            appendLog("❌ Lỗi đóng kết nối: " + e.getMessage());
        }
        
        updateConnectionStatus();
        clearWeatherDisplay();
    }
    
    private void searchWeather() {
        String cityName = cityTextField.getText().trim();
        if (cityName.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Vui lòng nhập tên thành phố!", 
                "Thông báo", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!isConnected) {
            JOptionPane.showMessageDialog(this, 
                "Chưa kết nối tới server!", 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Search in background thread
        new Thread(() -> {
            try {
                appendLog("🌤️ Tra cứu: " + cityName);
                output.println("WEATHER:" + cityName);
                handleServerResponse();
                
            } catch (Exception e) {
                appendLog("❌ Lỗi tra cứu: " + e.getMessage());
            }
        }).start();
    }
    
    private void pingServer() {
        if (!isConnected) {
            appendLog("❌ Chưa kết nối tới server!");
            return;
        }
        
        new Thread(() -> {
            try {
                appendLog("📡 Ping server...");
                output.println("PING");
                handleServerResponse();
                
            } catch (Exception e) {
                appendLog("❌ Lỗi ping: " + e.getMessage());
            }
        }).start();
    }
    
    private void handleServerResponse() {
        try {
            String response = input.readLine();
            
            if (response == null) {
                appendLog("❌ Mất kết nối với server");
                isConnected = false;
                SwingUtilities.invokeLater(this::updateConnectionStatus);
                return;
            }
            
            if (response.equals("PONG")) {
                appendLog("✅ Server phản hồi: PONG");
                
            } else if (response.startsWith("SUCCESS:")) {
                String weatherData = response.substring(8);
                displayWeatherInfo(weatherData);
                
            } else if (response.startsWith("ERROR:")) {
                String errorMsg = response.substring(6);
                appendLog("❌ " + errorMsg);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, errorMsg, "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
                
            } else {
                appendLog("Server: " + response);
            }
            
        } catch (IOException e) {
            appendLog("❌ Lỗi đọc phản hồi từ server: " + e.getMessage());
            isConnected = false;
            SwingUtilities.invokeLater(this::updateConnectionStatus);
        }
    }
    
    private void displayWeatherInfo(String weatherData) {
        try {
            String[] parts = weatherData.split("\\|");
            
            // Declare as final variables
            final String[] weatherInfo = {"", "", "", "", ""}; // city, temp, desc, humidity, wind
            
            for (String part : parts) {
                if (part.startsWith("CITY:")) {
                    weatherInfo[0] = part.substring(5);
                } else if (part.startsWith("TEMP:")) {
                    weatherInfo[1] = part.substring(5);
                } else if (part.startsWith("DESC:")) {
                    weatherInfo[2] = part.substring(5);
                } else if (part.startsWith("HUMIDITY:")) {
                    weatherInfo[3] = part.substring(9);
                } else if (part.startsWith("WIND:")) {
                    weatherInfo[4] = part.substring(5);
                }
            }
            
            // Update GUI in EDT
            SwingUtilities.invokeLater(() -> {
                cityLabel.setText(weatherInfo[0]);
                temperatureLabel.setText(weatherInfo[1] + "°C");
                descriptionLabel.setText(capitalizeFirst(weatherInfo[2]));
                humidityLabel.setText(weatherInfo[3] + "%");
                windSpeedLabel.setText(weatherInfo[4] + " m/s");
                timestampLabel.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
            });
            
            appendLog("✅ Đã cập nhật thông tin thời tiết cho: " + weatherInfo[0]);
            
        } catch (Exception e) {
            appendLog("❌ Lỗi hiển thị dữ liệu thời tiết: " + e.getMessage());
        }
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private void updateConnectionStatus() {
        if (isConnected) {
            connectionStatusLabel.setText("✅ Đã kết nối");
            connectionStatusLabel.setForeground(new Color(76, 175, 80));
            
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            searchButton.setEnabled(true);
            pingButton.setEnabled(true);
            
        } else {
            connectionStatusLabel.setText("❌ Chưa kết nối");
            connectionStatusLabel.setForeground(Color.RED);
            
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
    }
    
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client().setVisible(true);
        });
    }
}
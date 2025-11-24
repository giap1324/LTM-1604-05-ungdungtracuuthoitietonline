package application;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class Client extends Application {
    private Socket socket;
    private BufferedReader br;
    private BufferedWriter bw;

    private TextField cityInput;
    private Label statusLabel;
    private String currentLocation = "";
    private boolean isVietnamese = true;
    private boolean isCelsius = true;

    // Current weather
    private VBox currentWeatherBox;
    private Label cityNameLabel, temperatureLabel, feelsLikeLabel;
    private Label conditionLabel, humidityLabel, windLabel, cloudLabel, uvLabel;
    private ImageView weatherIcon;

    // Forecast
    private HBox forecastDaysBox;

    // Hourly
    private HBox hourlyBox;

    // Language texts
    private Label titleLabel, currentWeatherTitle, forecastTitle, hourlyTitle;
    
    private DatabaseManager dbManager;
    private Button favoriteBtn;
    private String currentCity = "";
    private String currentCountry = "";
    private double currentLat = 0;
    private double currentLon = 0;

    private Popup suggestionsPopup;
    private VBox suggestionsBox;
    private List<String[]> searchResults = new ArrayList<>();
    private boolean suggestionsVisible = false;

    // Synchronization object for socket operations
    private final Object socketLock = new Object();
    
    private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private StackPane videoContainer;

    private StackPane createVideoBackground() {
        videoContainer = new StackPane();
        videoContainer.setStyle("-fx-background-color: #1a1a2e;");
        
        mediaView = new MediaView();
        mediaView.setPreserveRatio(false);
        
        // Bind kích thước video với container
        mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
        mediaView.fitHeightProperty().bind(videoContainer.heightProperty());
        
        // Tăng độ mờ của video (0.5 = 50% mờ, thấy rõ hơn)
        mediaView.setOpacity(0.5);
        
        videoContainer.getChildren().add(mediaView);
        
        return videoContainer;
    }

    // Method để load video theo điều kiện thời tiết
    private void loadWeatherVideo(String condition) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        
        String videoPath = getVideoPathForCondition(condition);
        
        try {
            // Load video từ resources
            Media media = new Media(getClass().getResource(videoPath).toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            
            // Cấu hình MediaPlayer
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0);
            mediaPlayer.setMute(true);
            
            // Smooth transition khi video restart
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(Duration.ZERO);
            });
            
            mediaView.setMediaPlayer(mediaPlayer);
            
            // Fade in effect với opacity cao hơn
            FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), mediaView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(0.5); // Tăng từ 0.3 lên 0.5
            fadeIn.play();
            
        } catch (Exception e) {
            System.out.println("Không thể load video: " + e.getMessage());
        }
    }

    // Method để chọn video phù hợp với điều kiện thời tiết
    private String getVideoPathForCondition(String condition) {
        condition = condition.toLowerCase();
        
        if (condition.contains("rain") || condition.contains("mưa")) {
            return "/videos/rain.mp4";
        } else if (condition.contains("snow") || condition.contains("tuyết")) {
            return "/videos/snow.mp4";
        } else if (condition.contains("cloud") || condition.contains("mây")) {
            return "/videos/Sunny.mp4";
        } else if (condition.contains("storm") || condition.contains("bão")) {
            return "/videos/storm.mp4";
        } else if (condition.contains("clear") || condition.contains("nắng")) {
            return "/videos/Sunny.mp4";
        } else if (condition.contains("fog") || condition.contains("sương")) {
            return "/videos/fog.mp4";
        } else {
            return "/videos/Sunny.mp4"; // Default video
        }
    }

    @Override
    public void start(Stage primaryStage) {
        dbManager = new DatabaseManager();
        
        primaryStage.setTitle("Weather Application");
        
        // TẠO VIDEO BACKGROUND TRƯỚC
        StackPane videoBackground = createVideoBackground();
        
        // Tạo ScrollPane cho main content
        ScrollPane mainScroll = new ScrollPane();
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background: transparent;");
        mainScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: transparent;");
        
        // Header
        VBox header = createHeader();
        root.getChildren().add(header);
        
        // Main content container
        VBox contentBox = new VBox(25);
        contentBox.setPadding(new Insets(25, 35, 35, 35));
        contentBox.setAlignment(Pos.TOP_CENTER);
        
        // Combined Current Weather + Hourly Section
        VBox combinedSection = createCombinedWeatherSection();
        // 5-Day Forecast Section
        VBox forecastSection = createForecastSection();
        contentBox.getChildren().addAll(combinedSection, forecastSection);
        
        root.getChildren().add(contentBox);
        
        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(15));
        statusLabel = new Label(isVietnamese ? "Đang kết nối..." : "Connecting...");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setTextFill(Color.web("#8892b0"));
        footer.getChildren().add(statusLabel);
        root.getChildren().add(footer);
        
        mainScroll.setContent(root);
        
        // STACK VIDEO BACKGROUND VÀ CONTENT
        StackPane mainContainer = new StackPane();
        mainContainer.getChildren().addAll(videoBackground, mainScroll);
        
        // TẠO SCENE VỚI MAIN CONTAINER
        Scene scene = new Scene(mainContainer, 1100, 850);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Kết nối server
        connectToServer();
        
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                getCurrentLocationWeather();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        primaryStage.setOnCloseRequest(e -> closeConnection());
        
        // Sự kiện click outside để ẩn suggestions
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (suggestionsVisible && suggestionsPopup != null && suggestionsPopup.isShowing()) {
                if (cityInput != null && !cityInput.getBoundsInParent().contains(
                    cityInput.sceneToLocal(e.getSceneX(), e.getSceneY()))) {
                    
                    PauseTransition delay = new PauseTransition(Duration.millis(100));
                    delay.setOnFinished(evt -> {
                        if (suggestionsPopup.isShowing()) {
                            hideSuggestionsPopup();
                        }
                    });
                    delay.play();
                }
            }
        });
    }

    private VBox createHeader() {
        VBox header = new VBox(18);
        header.setPadding(new Insets(30, 35, 25, 35));
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #667eea 0%, #764ba2 100%);");

        HBox titleBox = new HBox(20);
        titleBox.setAlignment(Pos.CENTER);

        titleLabel = new Label("🌤 DỰ BÁO THỜI TIẾT");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        Button langBtn = new Button("EN");
        langBtn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        langBtn.getStyleClass().add("lang-button");
        langBtn.setOnAction(e -> {
            isVietnamese = !isVietnamese;
            langBtn.setText(isVietnamese ? "EN" : "VI");
            updateLanguage();
            hideSuggestionsPopup();
        });

        Button tempUnitBtn = new Button("°F");
        tempUnitBtn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        tempUnitBtn.getStyleClass().add("lang-button");
        tempUnitBtn.setOnAction(e -> {
            isCelsius = !isCelsius;
            tempUnitBtn.setText(isCelsius ? "°F" : "°C");
            loadAllWeatherData();
            hideSuggestionsPopup();
        });

        Button favoritesListBtn = new Button("⭐ " + (isVietnamese ? "Yêu thích" : "Favorites"));
        favoritesListBtn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        favoritesListBtn.getStyleClass().add("lang-button");
        favoritesListBtn.setOnAction(e -> {
            showFavoritesList();
            hideSuggestionsPopup();
        });

        titleBox.getChildren().addAll(titleLabel, langBtn, tempUnitBtn, favoritesListBtn);

        VBox searchContainer = new VBox(0);
        searchContainer.setAlignment(Pos.CENTER);
        searchContainer.setMaxWidth(500);

        HBox searchWrapper = new HBox();
        searchWrapper.setAlignment(Pos.CENTER);
        searchWrapper.setMaxWidth(500);

        HBox searchFieldContainer = new HBox();
        searchFieldContainer.setAlignment(Pos.CENTER_LEFT);
        searchFieldContainer.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95);" +
            "-fx-background-radius: 25;" +
            "-fx-padding: 5 20 5 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);"
        );
        searchFieldContainer.setMaxWidth(500);
        searchFieldContainer.setPrefHeight(50);

        Label searchIcon = new Label("🔍");
        searchIcon.setFont(Font.font(16));
        searchIcon.setTextFill(Color.web("#667eea"));
        searchIcon.setPadding(new Insets(0, 10, 0, 0));

        cityInput = new TextField();
        cityInput.setPromptText(isVietnamese ? "Tìm kiếm thành phố..." : "Search for cities...");
        cityInput.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-width: 0;" +
            "-fx-font-size: 15;" +
            "-fx-text-fill: #2c3e50;" +
            "-fx-prompt-text-fill: #95a5a6;" +
            "-fx-padding: 0;"
        );
        cityInput.setPrefHeight(40);
        cityInput.setPrefWidth(400);
        HBox.setHgrow(cityInput, Priority.ALWAYS);

        Button clearBtn = new Button("✕");
        clearBtn.setFont(Font.font(12));
        clearBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #95a5a6;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 5;"
        );
        clearBtn.setVisible(false);

        clearBtn.setOnAction(e -> {
            cityInput.clear();
            hideSuggestionsPopup();
            cityInput.requestFocus();
        });

        cityInput.textProperty().addListener((observable, oldValue, newValue) -> {
            clearBtn.setVisible(!newValue.isEmpty());
            
            if (newValue.length() >= 2) {
                searchCitiesInHeader(newValue, searchFieldContainer);
            } else {
                hideSuggestionsPopup();
            }
        });

        cityInput.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                PauseTransition pause = new PauseTransition(Duration.millis(200));
                pause.setOnFinished(e -> hideSuggestionsPopup());
                pause.play();
            } else if (cityInput.getText().length() >= 2) {
                PauseTransition pause = new PauseTransition(Duration.millis(100));
                pause.setOnFinished(e -> {
                    if (cityInput.isFocused() && cityInput.getText().length() >= 2) {
                        searchCitiesInHeader(cityInput.getText(), searchFieldContainer);
                    }
                });
                pause.play();
            }
        });

        cityInput.setOnAction(e -> {
            if (!searchResults.isEmpty()) {
                String[] firstResult = searchResults.get(0);
                String displayText = firstResult[0];
                if (firstResult.length > 2 && !firstResult[2].isEmpty()) {
                    displayText += ", " + firstResult[2];
                }
                cityInput.setText(displayText);
                hideSuggestionsPopup();
            }
            searchWeather();
        });

        searchFieldContainer.getChildren().addAll(searchIcon, cityInput, clearBtn);
        searchWrapper.getChildren().add(searchFieldContainer);
        searchContainer.getChildren().add(searchWrapper);

        header.getChildren().addAll(titleBox, searchContainer);
        
        initSuggestionsPopup();
        
        return header;
    }

    private void initSuggestionsPopup() {
        suggestionsPopup = new Popup();
        suggestionsPopup.setAutoHide(true);
        
        suggestionsBox = new VBox();
        suggestionsBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 5);" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 15;" +
            "-fx-padding: 10 0;"
        );
        suggestionsBox.setMaxWidth(500);
        suggestionsBox.setPrefWidth(500);
        
        ScrollPane scrollPane = new ScrollPane(suggestionsBox);
        scrollPane.setStyle(
            "-fx-background: white;" +
            "-fx-background-color: white;" +
            "-fx-border-width: 0;" +
            "-fx-background-radius: 15;"
        );
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(300);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        suggestionsPopup.getContent().add(scrollPane);
    }

    private void searchCitiesInHeader(String query, HBox searchFieldContainer) {
        new Thread(() -> {
            synchronized (socketLock) {
                try {
                    if (bw == null) return;
                    
                    String request = "SEARCH|" + query;
                    bw.write(request);
                    bw.newLine();
                    bw.flush();

                    String response = br.readLine();
                    Platform.runLater(() -> handleSearchResponse(response, searchFieldContainer));
                } catch (IOException e) {
                    Platform.runLater(() -> hideSuggestionsPopup());
                }
            }
        }).start();
    }

    private void handleSearchResponse(String response, HBox searchFieldContainer) {
        searchResults.clear();
        
        if (response == null || response.startsWith("ERROR")) {
            hideSuggestionsPopup();
            return;
        }

        String[] parts = response.split("\\|");
        if (parts.length < 2 || !parts[0].equals("SEARCH")) {
            hideSuggestionsPopup();
            return;
        }

        for (int i = 1; i < parts.length; i++) {
            String[] cityData = parts[i].split(",");
            if (cityData.length >= 3) {
                searchResults.add(cityData);
            }
        }

        if (searchResults.isEmpty()) {
            hideSuggestionsPopup();
        } else {
            showSuggestionsPopup(searchFieldContainer);
        }
    }

    private void showSuggestionsPopup(HBox searchFieldContainer) {
        if (searchResults.isEmpty()) return;

        suggestionsBox.getChildren().clear();
        
        for (String[] cityData : searchResults) {
            String name = cityData[0];
            String region = cityData.length > 1 ? cityData[1] : "";
            String country = cityData.length > 2 ? cityData[2] : "";
            
            HBox item = createSuggestionItem(name, region, country);
            suggestionsBox.getChildren().add(item);
        }

        try {
            Bounds bounds = searchFieldContainer.localToScreen(searchFieldContainer.getBoundsInLocal());
            
            if (bounds != null) {
                suggestionsPopup.setX(bounds.getMinX());
                suggestionsPopup.setY(bounds.getMaxY() + 5);

                if (!suggestionsPopup.isShowing()) {
                    suggestionsPopup.show(searchFieldContainer.getScene().getWindow());
                }
                
                suggestionsVisible = true;
            }
        } catch (Exception e) {
            System.out.println("Cannot show suggestions popup: " + e.getMessage());
        }
    }

    private void hideSuggestionsPopup() {
        if (suggestionsPopup != null && suggestionsPopup.isShowing()) {
            suggestionsPopup.hide();
        }
        suggestionsVisible = false;
        searchResults.clear();
    }

    private HBox createSuggestionItem(String name, String region, String country) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 20, 12, 20));
        item.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: transparent transparent #f0f0f0 transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        Label locationIcon = new Label("📍");
        locationIcon.setFont(Font.font(14));
        locationIcon.setTextFill(Color.web("#667eea"));

        VBox textBox = new VBox(3);
        
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nameLabel.setTextFill(Color.web("#2c3e50"));

        String subtitle = "";
        if (!region.isEmpty()) subtitle += region;
        if (!country.isEmpty()) {
            if (!subtitle.isEmpty()) subtitle += ", ";
            subtitle += country;
        }
        
        if (!subtitle.isEmpty()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setFont(Font.font("Arial", 12));
            subtitleLabel.setTextFill(Color.web("#7f8c8d"));
            textBox.getChildren().addAll(nameLabel, subtitleLabel);
        } else {
            textBox.getChildren().add(nameLabel);
        }

        HBox.setHgrow(textBox, Priority.ALWAYS);

        item.getChildren().addAll(locationIcon, textBox);

        item.setOnMouseEntered(e -> {
            item.setStyle(
                "-fx-background-color: #f8f9fa;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: transparent transparent #e0e0e0 transparent;"
            );
        });

        item.setOnMouseExited(e -> {
            item.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: transparent transparent #f0f0f0 transparent;"
            );
        });

        item.setOnMouseClicked(e -> {
            String displayText = name;
            if (!country.isEmpty()) {
                displayText += ", " + country;
            }
            cityInput.setText(displayText);
            hideSuggestionsPopup();
            searchWeather();
        });

        return item;
    }

    private VBox createCombinedWeatherSection() {
        VBox section = new VBox(25);
        section.setAlignment(Pos.TOP_CENTER);
        section.setPadding(new Insets(30));
        section.getStyleClass().add("weather-card");
        section.setMaxWidth(950);
        // ĐÃ GIẢM OPACITY - MỜ HƠN
        section.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.65);" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);"
        );

        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER);

        currentWeatherTitle = new Label(isVietnamese ? "THỜI TIẾT HIỆN TẠI" : "CURRENT WEATHER");
        currentWeatherTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        currentWeatherTitle.setTextFill(Color.web("#0f3460"));

        HBox.setHgrow(currentWeatherTitle, Priority.ALWAYS);

        favoriteBtn = new Button("☆");
        favoriteBtn.setFont(Font.font("Arial", 24));
        favoriteBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #ffd700;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 5 15;"
        );
        favoriteBtn.setOnAction(e -> toggleFavorite());

        headerBox.getChildren().addAll(currentWeatherTitle, favoriteBtn);

        currentWeatherBox = new VBox(15);
        currentWeatherBox.setAlignment(Pos.CENTER);
        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(110);
        weatherIcon.setFitHeight(110);
        cityNameLabel = new Label();
        cityNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        cityNameLabel.setTextFill(Color.web("#0f3460"));
        temperatureLabel = new Label();
        temperatureLabel.setFont(Font.font("Arial", FontWeight.BOLD, 62));
        temperatureLabel.setTextFill(Color.web("#e94560"));
        feelsLikeLabel = new Label();
        feelsLikeLabel.setFont(Font.font("Arial", 14));
        feelsLikeLabel.setTextFill(Color.web("#6c757d"));
        conditionLabel = new Label();
        conditionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 19));
        conditionLabel.setTextFill(Color.web("#16213e"));
        
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(40);
        detailsGrid.setVgap(15);
        detailsGrid.setAlignment(Pos.CENTER);
        detailsGrid.setPadding(new Insets(18, 0, 0, 0));
        humidityLabel = new Label();
        humidityLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        humidityLabel.setTextFill(Color.web("#495057"));
        windLabel = new Label();
        windLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        windLabel.setTextFill(Color.web("#495057"));
        cloudLabel = new Label();
        cloudLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        cloudLabel.setTextFill(Color.web("#495057"));
        uvLabel = new Label();
        uvLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 16));
        uvLabel.setTextFill(Color.web("#495057"));
        detailsGrid.add(humidityLabel, 0, 0);
        detailsGrid.add(windLabel, 1, 0);
        detailsGrid.add(cloudLabel, 0, 1);
        detailsGrid.add(uvLabel, 1, 1);
        
        currentWeatherBox.getChildren().addAll(weatherIcon, cityNameLabel, temperatureLabel, 
                                               feelsLikeLabel, conditionLabel, detailsGrid);
        
        Separator separator = new Separator();
        separator.setMaxWidth(800);
        separator.getStyleClass().add("separator");
        
        hourlyTitle = new Label(isVietnamese ? "DỰ BÁO 24 GIỜ" : "24-HOUR FORECAST");
        hourlyTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        hourlyTitle.setTextFill(Color.web("#0f3460"));
        hourlyBox = new HBox(12);
        hourlyBox.setAlignment(Pos.CENTER_LEFT);
        hourlyBox.setPadding(new Insets(5));
        
        ScrollPane scrollPane = new ScrollPane(hourlyBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(200);
        
        section.getChildren().addAll(headerBox, currentWeatherBox, separator, hourlyTitle, scrollPane);
        return section;
    }

    private VBox createForecastSection() {
        VBox section = new VBox(18);
        section.setAlignment(Pos.TOP_CENTER);
        section.setPadding(new Insets(30));
        section.getStyleClass().add("weather-card");
        section.setMaxWidth(950);
        // ĐÃ GIẢM OPACITY - MỜ HƠN
        section.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.65);" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);"
        );

        forecastTitle = new Label(isVietnamese ? "DỰ BÁO 5 NGÀY" : "5-DAY FORECAST");
        forecastTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        forecastTitle.setTextFill(Color.web("#0f3460"));

        forecastDaysBox = new HBox(15);
        forecastDaysBox.setAlignment(Pos.CENTER);

        section.getChildren().addAll(forecastTitle, forecastDaysBox);
        return section;
    }

    private VBox createHourlyBox(String time, String temp, String condition, String icon) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));
        box.getStyleClass().add("hourly-card");
        box.setPrefWidth(110);
        box.setMinWidth(110);
        // ĐÃ GIẢM OPACITY
        box.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.7);" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 5, 0, 0, 2);"
        );
        
        String hourStr = "N/A";
        try {
            if (time != null && time.length() >= 16) {
                hourStr = time.substring(11, 16);
            } else if (time != null && time.length() >= 5) {
                hourStr = time.substring(0, 5);
            }
        } catch (Exception e) {
            hourStr = time;
        }
        
        Label timeLabel = new Label(hourStr);
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timeLabel.setTextFill(Color.web("#0f3460"));
        box.getChildren().add(timeLabel);
        
        ImageView iconView = new ImageView();
        try {
            iconView.setImage(new Image("https:" + icon, true));
            iconView.setFitWidth(45);
            iconView.setFitHeight(45);
            box.getChildren().add(iconView);
        } catch (Exception e) {}
        
        Label tempLabel = new Label(convertTemp(temp) + getTempUnit());
        tempLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        tempLabel.setTextFill(getTemperatureColor(temp));
        box.getChildren().add(tempLabel);
        
        Label condLabel = new Label(condition);
        condLabel.setFont(Font.font("Arial", 11));
        condLabel.setWrapText(true);
        condLabel.setMaxWidth(100);
        condLabel.setAlignment(Pos.CENTER);
        condLabel.setTextFill(Color.web("#6c757d"));
        box.getChildren().add(condLabel);
        
        return box;
    }

    private VBox createForecastDayBox(String date, String maxTemp, String minTemp, String condition, String icon, String rain) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("forecast-day-card");
        box.setPrefWidth(170);
        // ĐÃ GIẢM OPACITY - MỜ HƠN
        box.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.75);" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);" +
            "-fx-cursor: hand;"
        );

        String displayDate = date;
        final String[] dayOfWeekHolder = new String[1];
        dayOfWeekHolder[0] = "";

        try {
            String[] dateParts = date.split("-");
            if (dateParts.length == 3) {
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]);
                int day = Integer.parseInt(dateParts[2]);
                
                java.time.LocalDate localDate = java.time.LocalDate.of(year, month, day);
                if (isVietnamese) {
                    dayOfWeekHolder[0] = switch(localDate.getDayOfWeek()) {
                        case MONDAY -> "Thứ 2";
                        case TUESDAY -> "Thứ 3";
                        case WEDNESDAY -> "Thứ 4";
                        case THURSDAY -> "Thứ 5";
                        case FRIDAY -> "Thứ 6";
                        case SATURDAY -> "Thứ 7";
                        case SUNDAY -> "CN";
                    };
                    displayDate = String.format("%02d/%02d", day, month);
                } else {
                    dayOfWeekHolder[0] = localDate.getDayOfWeek().toString().substring(0, 3);
                    displayDate = String.format("%02d/%02d", month, day);
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error parsing date: " + date);
        }

        Label dayLabel = new Label(dayOfWeekHolder[0]);
        dayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        dayLabel.setTextFill(Color.web("#667eea"));
            
        Label dateLabel = new Label(displayDate);
        dateLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 15));
        dateLabel.setTextFill(Color.web("#16213e"));

        ImageView iconView = new ImageView();
        try {
            iconView.setImage(new Image("https:" + icon, true));
            iconView.setFitWidth(60);
            iconView.setFitHeight(60);
        } catch (Exception e) {
            System.out.println("DEBUG: Error loading icon: " + icon);
        }

        HBox tempBox = new HBox(8);
        tempBox.setAlignment(Pos.CENTER);

        Label maxTempLabel = new Label(convertTemp(maxTemp) + getTempUnit());
        maxTempLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        maxTempLabel.setTextFill(getTemperatureColor(maxTemp));

        Label slashLabel = new Label("/");
        slashLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        slashLabel.setTextFill(Color.web("#95a5a6"));

        Label minTempLabel = new Label(convertTemp(minTemp) + getTempUnit());
        minTempLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        minTempLabel.setTextFill(getTemperatureColor(minTemp));

        tempBox.getChildren().addAll(maxTempLabel, slashLabel, minTempLabel);

        Label condLabel = new Label(condition);
        condLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        condLabel.setTextFill(Color.web("#495057"));
        condLabel.setWrapText(true);
        condLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        condLabel.setMaxWidth(150);

        Label rainLabel = new Label("💧 " + rain + "%");
        rainLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        rainLabel.setTextFill(Color.web("#3498db"));

        box.getChildren().addAll(dayLabel, dateLabel, iconView, tempBox, condLabel, rainLabel);

        box.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                DayWeatherData data = new DayWeatherData(date, maxTemp, minTemp, condition, icon, rain, dayOfWeekHolder[0]);
                showDayDetailDialog(data);
            }
        });

        box.setOnMouseEntered(e -> {
            box.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.92);" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5);" +
                "-fx-cursor: hand;"
            );
        });
        
        box.setOnMouseExited(e -> {
            box.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.75);" +
                "-fx-background-radius: 15;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);" +
                "-fx-cursor: hand;"
            );
        });

        return box;
    }

    private static class DayWeatherData {
        String date, maxTemp, minTemp, condition, icon, rain, dayOfWeek;
        
        public DayWeatherData(String date, String maxTemp, String minTemp, String condition, 
                             String icon, String rain, String dayOfWeek) {
            this.date = date;
            this.maxTemp = maxTemp;
            this.minTemp = minTemp;
            this.condition = condition;
            this.icon = icon;
            this.rain = rain;
            this.dayOfWeek = dayOfWeek;
        }
    }

    private void showDayDetailDialog(DayWeatherData data) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialogStage.setTitle(isVietnamese ? "Chi tiết thời tiết" : "Weather Details");
        
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 0% 100%, #667eea 0%, #764ba2 100%);");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.setAlignment(Pos.TOP_CENTER);

        VBox headerCard = new VBox(20);
        headerCard.setAlignment(Pos.CENTER);
        headerCard.setPadding(new Insets(30));
        headerCard.setMaxWidth(650);
        headerCard.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.95);" +
            "-fx-background-radius: 25;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 20, 0, 0, 10);"
        );

        String fullDate = "";
        try {
            String[] dateParts = data.date.split("-");
            if (dateParts.length >= 3) {
                int year = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]);
                int day = Integer.parseInt(dateParts[2]);
                fullDate = isVietnamese ? "Ngày " + day + " tháng " + month + ", " + year : month + "/" + day + "/" + year;
            }
        } catch (Exception e) {
            fullDate = data.date;
        }

        Label dowLabel = new Label(data.dayOfWeek);
        dowLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        dowLabel.setTextFill(Color.web("#667eea"));
        dowLabel.setStyle("-fx-background-color: rgba(102, 126, 234, 0.1); -fx-padding: 5 15; -fx-background-radius: 15;");

        Label dateLabel = new Label(fullDate);
        dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        dateLabel.setTextFill(Color.web("#16213e"));

        ImageView iconView = new ImageView();
        try {
            iconView.setImage(new Image("https:" + data.icon, true));
            iconView.setFitWidth(100);
            iconView.setFitHeight(100);
        } catch (Exception e) {}

        Label conditionLabel = new Label(data.condition);
        conditionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        conditionLabel.setTextFill(Color.web("#0f3460"));

        HBox tempCards = new HBox(20);
        tempCards.setAlignment(Pos.CENTER);
        
        VBox maxCard = createDetailStatCard(isVietnamese ? "Cao nhất" : "High",
            convertTemp(data.maxTemp) + getTempUnit(), getTemperatureColor(data.maxTemp), "rgba(233, 69, 96, 0.1)");
        VBox minCard = createDetailStatCard(isVietnamese ? "Thấp nhất" : "Low",
            convertTemp(data.minTemp) + getTempUnit(), getTemperatureColor(data.minTemp), "rgba(52, 152, 219, 0.1)");
        VBox rainCard = createDetailStatCard(isVietnamese ? "Khả năng mưa" : "Rain Chance",
            data.rain + "%", Color.web("#3498db"), "rgba(52, 152, 219, 0.1)");

        tempCards.getChildren().addAll(maxCard, minCard, rainCard);
        headerCard.getChildren().addAll(dowLabel, dateLabel, iconView, conditionLabel, tempCards);
        content.getChildren().add(headerCard);
        scrollPane.setContent(content);
        mainContainer.getChildren().add(scrollPane);
        
        Scene scene = new Scene(mainContainer, 800, 600);
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    private VBox createDetailStatCard(String title, String value, Color valueColor, String bgColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15; -fx-border-color: rgba(0, 0, 0, 0.05); -fx-border-width: 1; -fx-border-radius: 15;");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));
        titleLabel.setTextFill(Color.web("#6c757d"));
        
        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        valueLabel.setTextFill(valueColor);
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void toggleFavorite() {
        if (currentCity.isEmpty() || currentCountry.isEmpty()) {
            showAlert(isVietnamese ? "Thông báo" : "Notice",
                isVietnamese ? "Vui lòng chọn một thành phố trước!" : "Please select a city first!");
            return;
        }
        
        boolean isFav = dbManager.isFavorite(currentCity, currentCountry);
        
        if (isFav) {
            boolean success = dbManager.removeFavorite(currentCity, currentCountry);
            if (success) {
                showAlert(isVietnamese ? "Thành công" : "Success",
                    isVietnamese ? "Đã xóa khỏi danh sách yêu thích" : "Removed from favorites");
                updateFavoriteButton();
            }
        } else {
            boolean success = dbManager.addFavorite(currentCity, currentCountry, currentLat, currentLon);
            if (success) {
                showAlert(isVietnamese ? "Thành công" : "Success",
                    isVietnamese ? "Đã thêm vào danh sách yêu thích" : "Added to favorites");
                updateFavoriteButton();
            }
        }
    }

    private void showFavoritesList() {
        Stage favStage = new Stage();
        favStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        favStage.setTitle(isVietnamese ? "Danh sách yêu thích" : "Favorites List");
        
        VBox mainLayout = new VBox(0);
        mainLayout.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #667eea 0%, #764ba2 100%);");
        
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(30, 20, 20, 20));
        
        Label titleLabel = new Label(isVietnamese ? "🌟 THÀNH PHỐ YÊU THÍCH" : "🌟 FAVORITE CITIES");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");
        
        Label subtitleLabel = new Label(isVietnamese ? "Quản lý các địa điểm yêu thích của bạn" : "Manage your favorite locations");
        subtitleLabel.setFont(Font.font("Arial", 14));
        subtitleLabel.setTextFill(Color.web("rgba(255,255,255,0.8)"));
        header.getChildren().addAll(titleLabel, subtitleLabel);
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 25 25 0 0;");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox citiesBox = new VBox(15);
        citiesBox.setPadding(new Insets(10));
        citiesBox.setAlignment(Pos.TOP_CENTER);
        
        List<DatabaseManager.FavoriteCity> favorites = dbManager.getAllFavorites();
        
        if (favorites.isEmpty()) {
            VBox emptyBox = new VBox(20);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50, 20, 50, 20));
            
            Label iconLabel = new Label("🏙️");
            iconLabel.setFont(Font.font(48));
            
            Label emptyTitle = new Label(isVietnamese ? "Chưa có thành phố yêu thích" : "No favorite cities yet");
            emptyTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            emptyTitle.setTextFill(Color.web("#6c757d"));
            
            Label emptyDesc = new Label(isVietnamese ? "Thêm thành phố vào danh sách yêu thích để truy cập nhanh" : "Add cities to your favorites for quick access");
            emptyDesc.setFont(Font.font("Arial", 14));
            emptyDesc.setTextFill(Color.web("#95a5a6"));
            emptyDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            emptyDesc.setWrapText(true);
            emptyDesc.setMaxWidth(300);
            
            emptyBox.getChildren().addAll(iconLabel, emptyTitle, emptyDesc);
            citiesBox.getChildren().add(emptyBox);
        } else {
            for (DatabaseManager.FavoriteCity city : favorites) {
                VBox cityCard = createFavoriteCityCard(city, favStage);
                citiesBox.getChildren().add(cityCard);
            }
        }
        
        scrollPane.setContent(citiesBox);
        
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20, 0, 10, 0));
        
        Button closeBtn = new Button(isVietnamese ? "ĐÓNG" : "CLOSE");
        closeBtn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        closeBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        closeBtn.setOnAction(e -> favStage.close());
        
        footer.getChildren().add(closeBtn);
        content.getChildren().addAll(scrollPane, footer);
        mainLayout.getChildren().addAll(header, content);
        
        Scene scene = new Scene(mainLayout, 500, 600);
        favStage.setScene(scene);
        favStage.show();
    }

    private VBox createFavoriteCityCard(DatabaseManager.FavoriteCity city, Stage parentStage) {
        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); -fx-cursor: hand;");
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-background-radius: 15 15 0 0;");
        
        Label locationIcon = new Label("📍");
        locationIcon.setFont(Font.font(16));
        
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(city.getDisplayName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);");
        
        Label countryLabel = new Label(city.country);
        countryLabel.setFont(Font.font("Arial", 12));
        countryLabel.setTextFill(Color.web("rgba(255,255,255,0.8)"));
        
        nameBox.getChildren().addAll(nameLabel, countryLabel);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        header.getChildren().addAll(locationIcon, nameBox);
        
        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(20));
        
        VBox infoBox = new VBox(8);
        
        HBox coordBox = new HBox(8);
        coordBox.setAlignment(Pos.CENTER_LEFT);
        Label coordIcon = new Label("🌐");
        coordIcon.setFont(Font.font(12));
        Label coordLabel = new Label(String.format("%.4f, %.4f", city.latitude, city.longitude));
        coordLabel.setFont(Font.font("Arial", 12));
        coordLabel.setTextFill(Color.web("#6c757d"));
        coordBox.getChildren().addAll(coordIcon, coordLabel);
        
        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        dateIcon.setFont(Font.font(12));
        String formattedDate = formatDate(city.addedDate);
        Label dateLabel = new Label((isVietnamese ? "Đã thêm: " : "Added: ") + formattedDate);
        dateLabel.setFont(Font.font("Arial", 11));
        dateLabel.setTextFill(Color.web("#95a5a6"));
        dateBox.getChildren().addAll(dateIcon, dateLabel);
        infoBox.getChildren().addAll(coordBox, dateBox);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button viewBtn = createActionButton("👁", isVietnamese ? "Xem thời tiết" : "View weather", Color.web("#2ecc71"));
        Button removeBtn = createActionButton("X", isVietnamese ? "Xóa" : "Remove", Color.web("#e74c3c"));
        
        viewBtn.setOnAction(e -> {
            currentLocation = city.getCoordinates();
            cityInput.setText(city.cityName);
            loadAllWeatherData();
            parentStage.close();
        });
        
        removeBtn.setOnAction(e -> {
            dbManager.removeFavorite(city.cityName, city.country);
            parentStage.close();
            showFavoritesList();
        });
        
        buttonBox.getChildren().addAll(viewBtn, removeBtn);
        content.getChildren().addAll(infoBox, buttonBox);
        card.getChildren().addAll(header, content);
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); -fx-cursor: hand;"));
        
        return card;
    }

    private Button createActionButton(String icon, String tooltip, Color color) {
        Button button = new Button(icon);
        button.setFont(Font.font(14));
        button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 0, 1);", color.toString().replace("0x", "#")));
        
        Tooltip tip = new Tooltip(tooltip);
        tip.setStyle("-fx-font-size: 11;");
        Tooltip.install(button, tip);
        
        button.setOnMouseEntered(e -> button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);", color.darker().toString().replace("0x", "#"))));
        button.setOnMouseExited(e -> button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 0, 1);", color.toString().replace("0x", "#"))));
        
        return button;
    }

    private String formatDate(java.sql.Timestamp timestamp) {
        try {
            if (timestamp == null) return "N/A";
            java.time.LocalDateTime dateTime = timestamp.toLocalDateTime();
            return isVietnamese ? 
                dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (Exception e) {
            return "N/A";
        }
    }

    private void updateLanguage() {
        titleLabel.setText(isVietnamese ? "🌤 DỰ BÁO THỜI TIẾT" : "🌤 WEATHER FORECAST");
        cityInput.setPromptText(isVietnamese ? "Nhập tên thành phố..." : "Enter city name...");
        currentWeatherTitle.setText(isVietnamese ? "THỜI TIẾT HIỆN TẠI" : "CURRENT WEATHER");
        forecastTitle.setText(isVietnamese ? "DỰ BÁO 5 NGÀY" : "5-DAY FORECAST");
        hourlyTitle.setText(isVietnamese ? "DỰ BÁO 24 GIỜ" : "24-HOUR FORECAST");
        statusLabel.setText(isVietnamese ? "✓ Đã kết nối đến server" : "✓ Connected to server");
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                Platform.runLater(() -> {
                    statusLabel.setText(isVietnamese ? "✓ Đã kết nối đến server" : "✓ Connected to server");
                    statusLabel.setTextFill(Color.web("#2ecc71"));
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText(isVietnamese ? "✗ Không thể kết nối" : "✗ Connection failed");
                    statusLabel.setTextFill(Color.web("#e94560"));
                    showAlert(isVietnamese ? "Lỗi" : "Error",
                            isVietnamese ? "Không thể kết nối đến server!" : "Cannot connect to server!");
                });
            }
        }).start();
    }

    private void getCurrentLocationWeather() {
        if (socket == null || socket.isClosed()) {
            showAlert(isVietnamese ? "Lỗi" : "Error", isVietnamese ? "Chưa kết nối đến server!" : "Not connected to server!");
            return;
        }

        statusLabel.setText(isVietnamese ? "Đang lấy vị trí..." : "Getting location...");

        new Thread(() -> {
            try {
                String ipApiUrl = "http://ip-api.com/json/?lang=vi";
                URL url = new URL(ipApiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                double lat = Double.parseDouble(extractJsonValue(json, "\"lat\":"));
                double lon = Double.parseDouble(extractJsonValue(json, "\"lon\":"));
                String city = extractJsonValue(json, "\"city\":");

                currentLocation = "LAT:" + lat + "," + lon;

                Platform.runLater(() -> {
                    cityInput.setText(city);
                    statusLabel.setText(isVietnamese ? "✓ Đã kết nối đến server" : "✓ Connected to server");
                    loadAllWeatherData();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert(isVietnamese ? "Lỗi" : "Error", isVietnamese ? "Không thể lấy vị trí: " + e.getMessage()
                            : "Cannot get location: " + e.getMessage());
                    statusLabel.setText(isVietnamese ? "✓ Đã kết nối đến server" : "✓ Connected to server");
                });
            }
        }).start();
    }

    private void searchWeather() {
        String city = cityInput.getText().trim();
        if (city.isEmpty()) {
            showAlert(isVietnamese ? "Thông báo" : "Notice",
                    isVietnamese ? "Vui lòng nhập tên thành phố!" : "Please enter city name!");
            return;
        }

        if (socket == null || socket.isClosed()) {
            showAlert(isVietnamese ? "Lỗi" : "Error",
                    isVietnamese ? "Chưa kết nối đến server!" : "Not connected to server!");
            return;
        }

        currentLocation = city;
        loadAllWeatherData();
    }

    private void loadAllWeatherData() {
        loadCurrentWeather();
        loadForecast();
    }

    private void loadCurrentWeather() {
        new Thread(() -> {
            synchronized (socketLock) {
                try {
                    bw.write("CURRENT|" + currentLocation);
                    bw.newLine();
                    bw.flush();

                    String response = br.readLine();
                    Platform.runLater(() -> displayCurrentWeather(response));
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(isVietnamese ? "Lỗi" : "Error",
                            isVietnamese ? "Lỗi kết nối: " + e.getMessage() : "Connection error: " + e.getMessage()));
                }
            }
        }).start();
    }

    private void loadForecast() {
        new Thread(() -> {
            synchronized (socketLock) {
                try {
                    bw.write("FORECAST|" + currentLocation + "|5");
                    bw.newLine();
                    bw.flush();

                    String response = br.readLine();
                    Platform.runLater(() -> displayForecast(response));
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(isVietnamese ? "Lỗi" : "Error",
                            isVietnamese ? "Lỗi kết nối: " + e.getMessage() : "Connection error: " + e.getMessage()));
                }
            }
        }).start();
    }

    private Color getTemperatureColor(String tempStr) {
        try {
            double temp = Double.parseDouble(tempStr);
            if (temp <= 10) return Color.web("#0d47a1");
            else if (temp <= 15) return Color.web("#1976d2");
            else if (temp <= 20) return Color.web("#42a5f5");
            else if (temp <= 25) return Color.web("#66bb6a");
            else if (temp <= 30) return Color.web("#ffa726");
            else if (temp <= 35) return Color.web("#ff7043");
            else return Color.web("#e53935");
        } catch (NumberFormatException e) {
            return Color.web("#495057");
        }
    }

    private void displayCurrentWeather(String response) {
        String[] parts = response.split("\\|");
        if (parts[0].equals("ERROR")) {
            showAlert(isVietnamese ? "Lỗi" : "Error", 
                     parts.length > 1 ? parts[1] : (isVietnamese ? "Lỗi không xác định" : "Unknown error"));
            return;
        }
        if (parts.length < 12) {
            showAlert(isVietnamese ? "Lỗi" : "Error", 
                     isVietnamese ? "Dữ liệu không hợp lệ" : "Invalid data");
            return;
        }
        
        currentCity = parts[1];
        currentCountry = parts[2];
        if (currentLocation.startsWith("LAT:")) {
            String[] coords = currentLocation.substring(4).split(",");
            if (coords.length >= 2) {
                try {
                    currentLat = Double.parseDouble(coords[0].trim());
                    currentLon = Double.parseDouble(coords[1].trim());
                } catch (NumberFormatException e) {
                    currentLat = 0;
                    currentLon = 0;
                }
            }
        }
        
        String weatherCondition = parts[5];
        loadWeatherVideo(weatherCondition);
        
        String inputCity = cityInput.getText() != null ? cityInput.getText().trim() : "";
        String displayCity = inputCity.isEmpty() ? parts[1] : inputCity;
        cityNameLabel.setText(displayCity + ", " + parts[2]);
        temperatureLabel.setText(convertTemp(parts[3]) + getTempUnit());
        temperatureLabel.setTextFill(getTemperatureColor(parts[3]));
        feelsLikeLabel.setText((isVietnamese ? "Cảm giác như " : "Feels like ") + convertTemp(parts[8]) + getTempUnit());
        conditionLabel.setText(parts[5]);
        humidityLabel.setText("💧 " + (isVietnamese ? "Độ ẩm: " : "Humidity: ") + parts[4] + "%");
        windLabel.setText("🌬 " + (isVietnamese ? "Gió " : "Wind ") + parts[6] + ": " + parts[7] + " km/h");
        cloudLabel.setText("☁ " + (isVietnamese ? "Mây: " : "Cloud: ") + parts[9] + "%");
        uvLabel.setText("☀ UV: " + parts[10]);
        try {
            String iconUrl = "https:" + parts[11];
            weatherIcon.setImage(new Image(iconUrl, true));
        } catch (Exception e) {
            weatherIcon.setImage(null);
        }
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        if (favoriteBtn == null || currentCity.isEmpty() || currentCountry.isEmpty()) {
            return;
        }
        
        boolean isFav = dbManager.isFavorite(currentCity, currentCountry);
        
        if (isFav) {
            favoriteBtn.setText("★");
            favoriteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffd700; -fx-cursor: hand; -fx-padding: 5 15; -fx-font-size: 24px;");
        } else {
            favoriteBtn.setText("☆");
            favoriteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-cursor: hand; -fx-padding: 5 15; -fx-font-size: 24px;");
        }
    }

    private void displayForecast(String response) {
        String[] parts = response.split("\\|");
        if (parts[0].equals("ERROR")) {
            showAlert(isVietnamese ? "Lỗi" : "Error",
                    parts.length > 1 ? parts[1] : (isVietnamese ? "Lỗi không xác định" : "Unknown error"));
            return;
        }

        forecastDaysBox.getChildren().clear();
        hourlyBox.getChildren().clear();

        int hourlyIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("HOURLY")) {
                hourlyIndex = i;
                break;
            }
        }

        int endIndex = hourlyIndex != -1 ? hourlyIndex : Math.min(parts.length, 7);
        for (int i = 2; i < endIndex; i++) {
            String[] dayData = parts[i].split(",");
            if (dayData.length >= 6) {
                VBox dayBox = createForecastDayBox(dayData[0], dayData[1], dayData[2], dayData[3], dayData[4], dayData[5]);
                forecastDaysBox.getChildren().add(dayBox);
            }
        }

        if (hourlyIndex != -1 && hourlyIndex + 1 < parts.length) {
            for (int i = hourlyIndex + 1; i < parts.length; i++) {
                String[] hourData = parts[i].split(",");
                if (hourData.length >= 4) {
                    VBox hourBox = createHourlyBox(hourData[0], hourData[1], hourData[2], hourData[3]);
                    hourlyBox.getChildren().add(hourBox);
                }
            }
        }
    }

    private String extractJsonValue(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return "";

        int valueStart = keyIndex + key.length();
        while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '"')) {
            valueStart++;
        }

        int valueEnd = valueStart;
        boolean inQuotes = json.charAt(keyIndex + key.length()) == '"' || (valueStart > 0 && json.charAt(valueStart - 1) == '"');

        if (inQuotes) {
            valueEnd = json.indexOf("\"", valueStart);
        } else {
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == ',' || c == '}') break;
                valueEnd++;
            }
        }

        if (valueEnd == -1) valueEnd = json.length();
        return json.substring(valueStart, valueEnd).trim();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeConnection() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            if (dbManager != null) {
                dbManager.close();
            }
            if (bw != null) bw.close();
            if (br != null) br.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String convertTemp(String tempC) {
        try {
            double temp = Double.parseDouble(tempC);
            if (isCelsius) {
                return String.format("%.1f", temp);
            } else {
                double tempF = (temp * 9.0 / 5.0) + 32;
                return String.format("%.1f", tempF);
            }
        } catch (NumberFormatException e) {
            return tempC;
        }
    }

    private String getTempUnit() {
        return isCelsius ? "°C" : "°F";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
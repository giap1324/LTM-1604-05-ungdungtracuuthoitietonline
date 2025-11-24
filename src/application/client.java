package application;

import application.EnvLoader;
import application.map.WeatherMapWindow;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.Node;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import application.DailyDetailView; 

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main Application - Weather App với layout 2 cột
 */
public class client extends Application {

    private tcpclient tcpClient;
    private WeatherUI weatherUI;
    private WeatherDataParser dataParser;
    private TextField txtCity;
    private Button btnSearch;
    private Button btnMyLocation;
    // Location card labels (show detected city/country and hint)
    private Label locationLine1;
    private Label locationLine2;
    private Label currentTimeLabel;
    private Timeline clockTimeline;
    // Vector clock hand rotations and graphic
    private javafx.scene.transform.Rotate hourHandRotate;
    private javafx.scene.transform.Rotate minuteHandRotate;
    private javafx.scene.transform.Rotate secondHandRotate;
    private StackPane clockGraphicPane;
    private Label statusLabel;
    private WeatherMapWindow mapWindow;
    private String currentCityKey = null;


    @Override
    public void start(Stage stage) {
        tcpClient = new tcpclient("localhost", 2000);
        weatherUI = new WeatherUI();
        dataParser = new WeatherDataParser();
      
        
        // Sử dụng layout 3 cột với animated background
        StackPane mainContainer = createThreeColumnLayout();

        Scene scene = new Scene(mainContainer, 1600, 1000); // Rộng hơn để chứa 3 cột
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        
        stage.setTitle("🌤️ Weather App - Tra cứu thời tiết");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        
        // Bắt đầu đồng hồ thời gian thực
        startClock();
        // Load favorites list
        Platform.runLater(() -> {
            refreshFavoritesList();
            performGeoSearch();
        });
    }

    // --- Favorites persistence helpers (store as pipe-separated list in Preferences) ---
 // --- Favorites handled via server (MySQL) ---
    private void addFavorite(String cityKey) {
        // Use async send and refresh after server confirms to avoid race conditions
        System.out.println("Adding favorite: " + cityKey);
        String request = "ADD_FAV:" + cityKey.replace(",", ":");
        System.out.println("Sending to server: " + request);
        
        tcpClient.sendAsync(request, resp -> {
            Platform.runLater(() -> {
                System.out.println("Server response for ADD_FAV: " + resp);
                if (statusLabel != null) statusLabel.setText("❤ Đã thêm yêu thích: " + cityKey);
                refreshFavoritesList();
            });
        });
    }

    private void removeFavorite(String cityKey) {
        System.out.println("Removing favorite: " + cityKey);
        String request = "DEL_FAV:" + cityKey.replace(",", ":");
        System.out.println("Sending to server: " + request);
        
        tcpClient.sendAsync(request, resp -> {
            Platform.runLater(() -> {
                System.out.println("Server response for DEL_FAV: " + resp);
                if (statusLabel != null) statusLabel.setText("♡ Đã xóa yêu thích: " + cityKey);
                refreshFavoritesList();
            });
        });
    }

    private void refreshFavoritesList() {
        tcpClient.sendAsync("GET_FAV", resp -> {
            Platform.runLater(() -> {
                System.out.println("GET_FAV response: '" + resp + "'");
                
                if (resp != null && !resp.trim().isEmpty()) {
                    List<String> favorites = Arrays.stream(resp.split("\\|"))
                                                   .map(String::trim)
                                                   .filter(s -> !s.isEmpty() && s.contains(","))
                                                   .distinct()
                                                   .toList();
                    
                    System.out.println("Parsed favorites: " + favorites);
                    
                    weatherUI.updateFavoritesList(favorites, e -> {
                        Button btn = (Button) e.getSource();
                        String cityKey = (String) btn.getUserData();
                        String[] parts = cityKey.split(",");
                        if (parts.length >= 2) {
                            txtCity.setText(parts[0].trim());
                            performSearch();
                        }
                    }, e2 -> {
                        // delete handler: send DEL_FAV for the clicked favorite
                        Button delBtn = (Button) e2.getSource();
                        String cityKey = (String) delBtn.getUserData();
                        if (cityKey != null && !cityKey.trim().isEmpty()) {
                            System.out.println("Removing favorite: " + cityKey);
                            removeFavorite(cityKey);
                        }
                    });
                    
                    // Update favorite toggle state for current city if present
                    updateFavoriteButtonState(favorites);
                } else {
                    System.out.println("Empty favorites response, clearing list");
                    weatherUI.updateFavoritesList(new ArrayList<>(), null, null);
                    // ensure toggle is cleared
                    try { 
                        ToggleButton fav = weatherUI.getFavoriteButton(); 
                        if (fav != null) {
                            fav.setSelected(false);
                            fav.getStyleClass().remove("favorite-active");
                        }
                    } catch (Exception ignore) {}
                }
            });
        });
    }

    /**
     * Update favorite button state based on current city
     */
    private void updateFavoriteButtonState(List<String> favorites) {
        try {
            ToggleButton fav = weatherUI.getFavoriteButton();
            if (fav != null && currentCityKey != null) {
                boolean isFav = favorites.contains(currentCityKey);
                System.out.println("Current city: " + currentCityKey + ", is favorite: " + isFav);
                fav.setSelected(isFav);
                if (isFav) {
                    if (!fav.getStyleClass().contains("favorite-active")) {
                        fav.getStyleClass().add("favorite-active");
                    }
                } else {
                    fav.getStyleClass().remove("favorite-active");
                }
            }
        } catch (Exception ex) {
            System.err.println("Error updating favorite button state: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Setup favorite button handler (safe to call multiple times)
     */
    private void setupFavoriteButton() {
        ToggleButton fav = weatherUI.getFavoriteButton();
        if (fav != null) {
            // Remove old handler if exists and add new one
            fav.setOnAction(null);
            fav.setOnAction(e -> {
                if (currentCityKey == null) {
                    setStatus("❌ Không có thành phố nào được chọn");
                    fav.setSelected(false);
                    return;
                }
                
                if (fav.isSelected()) {
                    addFavorite(currentCityKey);
                    if (!fav.getStyleClass().contains("favorite-active")) {
                        fav.getStyleClass().add("favorite-active");
                    }
                    setStatus("❤ Đang thêm vào yêu thích...");
                } else {
                    removeFavorite(currentCityKey);
                    fav.getStyleClass().remove("favorite-active");
                    setStatus("♡ Đang xóa khỏi yêu thích...");
                }
            });
        }
    }

    
    /**
     * Tạo layout 3 cột - returns StackPane with animated background
     */
    private StackPane createThreeColumnLayout() {
        // Initialize WeatherUI's animated background layer
        weatherUI.ensureInit(); // Ensure labels exist before composing sections
        
        // Create our main BorderPane
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12, 16, 12, 16));
        
        // Get WeatherUI's animated background (this will create rootPane + animationLayer if needed)
        StackPane animatedBg = weatherUI.getRootPane();
        animatedBg.setMouseTransparent(true); // Don't block mouse events
        
        // Use a StackPane to layer: animated background at bottom, content on top
        StackPane layeredRoot = new StackPane();
        layeredRoot.getChildren().addAll(animatedBg, root);

        // Top: header (title + clock)
        VBox topContainer = new VBox(8);
        topContainer.setAlignment(Pos.TOP_CENTER);
        topContainer.getChildren().addAll(createHeaderWithClock());

        // Center: HBox with 3 columns (25% - 50% - 25%)
        HBox mainContent = new HBox(12);
        mainContent.setPadding(new Insets(10, 0, 10, 0));
        mainContent.setAlignment(Pos.TOP_CENTER);

        // ========== LEFT COLUMN (25%): Navigation & Search ==========
        VBox leftCol = new VBox(16);
        leftCol.setPadding(new Insets(8));
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setMinWidth(280);
        leftCol.setMaxWidth(350);
        HBox.setHgrow(leftCol, Priority.NEVER);

        // Ensure WeatherUI core fields exist (labels/boxes) before composing sections
        weatherUI.ensureInit();

        // Search box from client (it wires buttons and textfield)
        HBox searchBox = createSearchBox();
        // Favorites section (lịch sử tìm kiếm)
        VBox favoritesSection = weatherUI.createFavoritesSection();
        
    // Apply CSS classes for nicer card visuals
    searchBox.getStyleClass().add("card");
    favoritesSection.getStyleClass().add("card");

    VBox mapCard = createMapCard();
    mapCard.getStyleClass().add("card");

    // show search, location card, map entry, then favorites
    leftCol.getChildren().addAll(searchBox, createLocationCard(), mapCard, favoritesSection);

    // ========== MIDDLE COLUMN (50%): Main Weather Card + Hourly Forecast ==========
    VBox middleCol = new VBox(20);
    middleCol.setAlignment(Pos.TOP_CENTER);
    // Hero (main weather summary)
    VBox hero = weatherUI.createHeroSection();
    // Main details grid (humidity, wind, pressure, visibility) - wrap so we can apply card styling
    javafx.scene.Node basicsNode = weatherUI.createMainDetailsGrid();
    VBox basicsWrap = new VBox(basicsNode);
    basicsWrap.setAlignment(Pos.CENTER);
    basicsWrap.setPadding(new Insets(8));

    // Hourly forecast section (horizontal scroll)
    VBox hourly = weatherUI.createHourlyForecastSection();

    // Apply CSS classes
    hero.getStyleClass().add("card");
    basicsWrap.getStyleClass().add("card");
    hourly.getStyleClass().add("card");
    // Add hero, basics and hourly sections (map removed)
    middleCol.getChildren().addAll(hero, basicsWrap, hourly);

        // ========== RIGHT COLUMN (25%): 5-Day Forecast + Extended Info ==========
        VBox rightCol = new VBox(18);
        rightCol.setPadding(new Insets(8));
        rightCol.setAlignment(Pos.TOP_CENTER);
        rightCol.setMinWidth(280);
        rightCol.setMaxWidth(350);
        HBox.setHgrow(rightCol, Priority.NEVER);
    // Gemini alert removed
        // Daily 5-day forecast section
        VBox daily = weatherUI.createDailyForecastSection();
        // Detailed weather grid (extended information)
        GridPane detailsGrid = weatherUI.createWeatherDetailsGrid();

    // Apply CSS classes
    daily.getStyleClass().add("card");
    detailsGrid.getStyleClass().add("card");
    // Put the daily forecast in the right column, but move the details grid to the middle column
    rightCol.getChildren().addAll(daily);
    // Place detailsGrid into the middle column so it's centered with main weather card
    detailsGrid.setMaxWidth(760);
    middleCol.getChildren().add(detailsGrid);

        // Add all 3 columns to main content
        mainContent.getChildren().addAll(leftCol, middleCol, rightCol);

        // Single ScrollPane for entire content
        ScrollPane mainScroll = new ScrollPane(mainContent);
        mainScroll.setFitToWidth(true);
        mainScroll.setFitToHeight(true);
        mainScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        mainScroll.getStyleClass().add("main-scroll");

        // Bottom: status bar
        HBox status = createStatusBar();
        status.getStyleClass().add("status-bar");

        root.setTop(topContainer);
        root.setCenter(mainScroll);
        root.setBottom(status);
        
        // Return the layered root with animated background
        return layeredRoot;
    }

    private HBox createStatusBar() {
        if (statusLabel == null) {
            statusLabel = new Label("Ready");
            statusLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");
        }

        Label connection = new Label("●");
        connection.setStyle("-fx-text-fill: #4cd137; -fx-font-size: 14px;");

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPadding(new Insets(8, 12, 8, 12));
        statusBox.getChildren().addAll(connection, statusLabel);
        statusBox.setStyle("-fx-background-color: rgba(0,0,0,0.15); -fx-background-radius: 6;");
        return statusBox;
    }

    // Old duplicate methods - remove these
    /*
    private VBox createLeftColumn() {
        VBox leftColumn = new VBox(20);
        leftColumn.setAlignment(Pos.TOP_CENTER);
        leftColumn.setPadding(new Insets(20, 25, 20, 25));
        leftColumn.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
        
        // Search Box
        VBox searchSection = createSearchSection();
        
        // Current Weather Card
        VBox currentWeatherCard = weatherUI.createCurrentWeatherCard();
        
        leftColumn.getChildren().addAll(searchSection, currentWeatherCard);
        return leftColumn;
    }
    
    private ScrollPane createRightColumn() {
        VBox rightColumn = new VBox(25);
        rightColumn.setAlignment(Pos.TOP_CENTER);
        rightColumn.setPadding(new Insets(20, 25, 20, 25));
        rightColumn.setStyle("-fx-background-color: transparent;");
        
        // Hourly Forecast
        VBox hourlySection = weatherUI.createHourlyForecastSection();
        
        // Daily Forecast
        VBox dailySection = weatherUI.createDailyForecastSection();
        
        // Weather Details Grid
        GridPane detailsGrid = weatherUI.createWeatherDetailsGrid();
        
        rightColumn.getChildren().addAll(hourlySection, dailySection, detailsGrid);
        
        ScrollPane scroller = new ScrollPane(rightColumn);
        scroller.setFitToWidth(true);
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        return scroller;
    }
    
    private VBox createSearchSection() {
        VBox searchSection = new VBox(15);
        searchSection.setAlignment(Pos.CENTER);
        searchSection.setPadding(new Insets(10, 0, 10, 0));
        
        HBox searchBox = createSearchBox();
        searchSection.getChildren().add(searchBox);
        return searchSection;
    }
    */
    
    private VBox createHeaderWithClock() {
        // Reuse the header constructed by WeatherUI (which has a proper vector icon)
        VBox header = weatherUI.createHeader();
        header.setPadding(new Insets(15, 0, 15, 0));
        header.setStyle("-fx-background-color: rgba(0,0,0,0.2);");

        // Build small vector clock graphic and label, then append to the header
        clockGraphicPane = buildClockGraphic();
        currentTimeLabel = new Label("");
        currentTimeLabel.setStyle(
            "-fx-font-family: 'Segoe UI', 'Arial';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: 600;" +
            "-fx-text-fill: rgba(255,255,255,0.95);" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 8, 0, 0, 2);"
        );

        HBox clockBox = new HBox(10);
        clockBox.setAlignment(Pos.CENTER);
        clockBox.getChildren().addAll(clockGraphicPane, currentTimeLabel);

        header.getChildren().add(clockBox);
        return header;
    }

    // Các phương thức còn lại giữ nguyên (buildClockGraphic, startClock, updateClock, createSearchBox, performGeoSearch, etc.)
    // ... [giữ nguyên tất cả các phương thức khác từ code gốc] ...

    private HBox createSearchBox() {
        HBox searchBox = new HBox(12);
        searchBox.setAlignment(Pos.CENTER);
        searchBox.setPadding(new Insets(15, 20, 15, 20));
        searchBox.getStyleClass().add("search-box");

        // ----- TextField -----
        txtCity = new TextField();
        txtCity.setPromptText("Nhập tên thành phố... (VD: Hanoi, Tokyo, Paris)");
        txtCity.setPrefWidth(280);
        txtCity.setPrefHeight(42);
        txtCity.getStyleClass().add("search-textfield");

        // Khi focus thay đổi: thêm/xóa class "focused"
        txtCity.focusedProperty().addListener((obs, was, now) -> {
            if (now) txtCity.getStyleClass().add("focused");
            else txtCity.getStyleClass().remove("focused");
        });

    // ----- Nút tìm kiếm -----
    btnSearch = new Button();
    // compact yellow circular button with magnifier icon
    btnSearch.setGraphic(createMagnifierIcon(Color.WHITE, 18, 2.2));
    btnSearch.setPrefSize(48, 48);
    btnSearch.setMinSize(48, 48);
    btnSearch.setStyle("-fx-background-radius: 28; -fx-cursor: hand; -fx-border-color: transparent;");
    btnSearch.setBackground(new Background(new BackgroundFill(Color.web("#FFCC33"), new CornerRadii(28), null)));
    btnSearch.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.18)));
    btnSearch.setOnAction(e -> performSearch());
        txtCity.setOnAction(e -> performSearch());

        // location button will be part of the dedicated location card
        searchBox.getChildren().addAll(txtCity, btnSearch);
        java.net.URL sbCss = getClass().getResource("styles/searchbox.css");
        if (sbCss != null) {
            searchBox.getStylesheets().add(sbCss.toExternalForm());
        }
        return searchBox;
    }

    /**
     * Create a small card that shows current location and a small location button.
     */
    private VBox createLocationCard() {
        // ensure btnMyLocation exists and is wired
        if (btnMyLocation == null) {
            btnMyLocation = new Button();
        }
    // small square icon button with location-pin icon on the right of the location card
    btnMyLocation.setGraphic(createLocationPinIcon(Color.web("#111827"), 14));
    btnMyLocation.setPrefSize(40, 40);
    btnMyLocation.setMinSize(40,40);
    btnMyLocation.setTooltip(new Tooltip("Vị trí hiện tại"));
    btnMyLocation.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: rgba(0,0,0,0.06); -fx-border-radius: 10; -fx-cursor: hand;");
    btnMyLocation.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.10)));
    btnMyLocation.setOnAction(e -> performGeoSearch());

        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setMinWidth(220);
        card.setMaxWidth(Double.MAX_VALUE);
        // translucent card style to match other UI cards
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.12);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255,255,255,0.22);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 4);"
        );

        HBox header = new HBox();
        header.setAlignment(Pos.TOP_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

    Label title = new Label("Vị trí hiện tại");
    title.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: rgba(255,255,255,0.95);");

        header.getChildren().addAll(title, spacer, btnMyLocation);

        if (locationLine1 == null) {
            locationLine1 = new Label("--");
            locationLine1.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: rgba(255,255,255,0.95);");
        }
        if (locationLine2 == null) {
            locationLine2 = new Label("Tự động phát hiện");
            locationLine2.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.72);");
        }

        card.getChildren().addAll(header, locationLine1, locationLine2);
        return card;
    }
    private Node createMapSvgIcon(double size, Color strokeColor, Color fillColor) {
        Group g = new Group();

        double s = size;

        // === Map shape (folded map) ===
        Path mapShape = new Path();
        mapShape.setStroke(strokeColor);
        mapShape.setFill(fillColor);
        mapShape.setStrokeWidth(1.8);

        mapShape.getElements().addAll(
            new MoveTo(0, s * 0.6),
            new LineTo(s * 0.28, s * 0.10),
            new LineTo(s * 0.62, s * 0.30),
            new LineTo(s * 1.0, s * 0.05),
            new LineTo(s * 1.0, s * 0.75),
            new LineTo(s * 0.62, s * 0.95),
            new LineTo(s * 0.28, s * 0.70),
            new LineTo(0, s * 0.90),
            new ClosePath()
        );

        // === Location Pin (circle + triangle tail) ===
        double r = size * 0.18;
        Circle pinHead = new Circle(s * 0.62, s * 0.50, r);
        pinHead.setFill(strokeColor);

        Polygon pinTail = new Polygon(
            s * 0.62, s * 0.50 + r * 1.6,
            s * 0.62 - r * 0.6, s * 0.50 + r * 0.2,
            s * 0.62 + r * 0.6, s * 0.50 + r * 0.2
        );
        pinTail.setFill(strokeColor);

        g.getChildren().addAll(mapShape, pinHead, pinTail);

        StackPane wrap = new StackPane(g);
        wrap.setPrefSize(size + 6, size + 6);

        return wrap;
    }


    private VBox createMapCard() {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 14, 16, 14));
        card.setMinWidth(220);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: rgba(14,165,233,0.18);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(14,165,233,0.4);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1.5;"
        );

        // --- SVG icon map ---
        Node svgIcon = createMapSvgIcon(32, Color.WHITE, Color.TRANSPARENT);

        Label title = new Label("Weather Map");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label desc = new Label("Khám phá bản đồ radar thời tiết");
        desc.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-text-alignment: center;");
        desc.setWrapText(true);

        Button openBtn = new Button("Mở Bản Đồ");
        openBtn.setPrefWidth(180);
        openBtn.setGraphic(svgIcon);
        openBtn.setOnAction(e -> openWeatherMap());

        // 👉 Only 1 button now (web button removed)
        card.getChildren().addAll(title, desc, openBtn);
        return card;
    }


    private void openWeatherMap() {
        try {
            if (mapWindow == null) {
                mapWindow = new WeatherMapWindow();
            }
            mapWindow.show();
            setStatus("🗺️ Đã mở Weather Map");
        } catch (Exception ex) {
            showMapErrorDialog("Không thể mở Weather Map: " + ex.getMessage());
        }
    }
    
    private void openWeatherMapWeb() {
        try {
            String htmlPath = new java.io.File("src/application/map/standalone.html").getAbsolutePath();
            java.awt.Desktop.getDesktop().browse(new java.net.URI("file:///" + htmlPath.replace("\\", "/")));
            setStatus("🌐 Đã mở Weather Map trong browser");
        } catch (Exception ex) {
            showMapErrorDialog("Không thể mở browser: " + ex.getMessage());
        }
    }

    private void showMapErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi Weather Map");
        alert.setHeaderText("Không thể mở bản đồ");
        alert.setContentText(message);
        alert.showAndWait();
    }

    

        // Build small vector clock graphic used in header
        private StackPane buildClockGraphic() {
            double size = 48;
            double center = size / 2.0;
            StackPane pane = new StackPane();
            pane.setPrefSize(size, size);

            Circle face = new Circle(center, center, center - 1);
            face.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255,255,255,0.06)), new Stop(1, Color.rgb(255,255,255,0.02))));
            face.setStroke(Color.rgb(255,255,255,0.18));
            face.setStrokeWidth(1.2);
            face.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.25)));

            Group hands = new Group();

            Line hourHand = new Line(0, -8, 0, 2);
            hourHand.setStroke(Color.WHITE);
            hourHand.setStrokeWidth(3);
            hourHand.setStrokeLineCap(StrokeLineCap.ROUND);
            hourHandRotate = new javafx.scene.transform.Rotate(0, 0, 0);
            hourHand.getTransforms().add(hourHandRotate);

            Line minuteHand = new Line(0, -12, 0, 2);
            minuteHand.setStroke(Color.WHITE);
            minuteHand.setStrokeWidth(2);
            minuteHand.setStrokeLineCap(StrokeLineCap.ROUND);
            minuteHandRotate = new javafx.scene.transform.Rotate(0, 0, 0);
            minuteHand.getTransforms().add(minuteHandRotate);

            Line secondHand = new Line(0, -14, 0, 4);
            secondHand.setStroke(Color.web("#FFD700"));
            secondHand.setStrokeWidth(1);
            secondHand.setStrokeLineCap(StrokeLineCap.ROUND);
            secondHandRotate = new javafx.scene.transform.Rotate(0, 0, 0);
            secondHand.getTransforms().add(secondHandRotate);

            Circle pin = new Circle(0, 0, 2, Color.web("#FFD700"));

            hands.getChildren().addAll(hourHand, minuteHand, secondHand, pin);
            hands.setLayoutX(center);
            hands.setLayoutY(center);

            pane.getChildren().addAll(face, hands);
            return pane;
        }

        // Helper: create a simple magnifier icon using vector shapes so it scales nicely
        private Node createMagnifierIcon(Color color, double size, double strokeWidth) {
            Group g = new Group();
            double r = size * 0.35;
            Circle ring = new Circle(0, 0, r);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(color);
            ring.setStrokeWidth(strokeWidth);

            Line handle = new Line(r * 0.6, r * 0.6, r * 1.4, r * 1.4);
            handle.setStroke(color);
            handle.setStrokeWidth(strokeWidth);
            handle.setStrokeLineCap(StrokeLineCap.ROUND);

            g.getChildren().addAll(ring, handle);
            StackPane wrap = new StackPane(g);
            wrap.setPrefSize(size + 8, size + 8);
            return wrap;
        }

        // Helper: create a simple location-pin icon (circle + triangular tail)
        private Node createLocationPinIcon(Color color, double size) {
            Group g = new Group();
            double headR = size * 0.45;
            Circle head = new Circle(0, -size * 0.12, headR);
            head.setFill(color);

            Polygon tail = new Polygon();
            // triangle pointing down under the head
            tail.getPoints().addAll(
                0.0, headR * 1.6,
                -headR * 0.7, headR * 0.0,
                headR * 0.7, headR * 0.0
            );
            tail.setFill(color);

            g.getChildren().addAll(head, tail);
            StackPane wrap = new StackPane(g);
            wrap.setPrefSize(size + 12, size + 12);
            return wrap;
        }

        /**
         * Bắt đầu đồng hồ thời gian thực - cập nhật mỗi giây
         */
        private void startClock() {
            // Cập nhật ngay lập tức
            updateClock();
            // Cập nhật mỗi giây
            clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
            clockTimeline.setCycleCount(Timeline.INDEFINITE);
            clockTimeline.play();
        }

        private void updateClock() {
            LocalDateTime now = LocalDateTime.now();

            int h = now.getHour() % 12;
            int m = now.getMinute();
            int s = now.getSecond();

            double secondAngle = s * 6.0; // 360 / 60
            double minuteAngle = m * 6.0 + s * 0.1; // plus seconds fraction
            double hourAngle = h * 30.0 + m * 0.5; // 360 / 12 + minutes fraction

            if (secondHandRotate != null) secondHandRotate.setAngle(secondAngle);
            if (minuteHandRotate != null) minuteHandRotate.setAngle(minuteAngle);
            if (hourHandRotate != null) hourHandRotate.setAngle(hourAngle);

            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            if (currentTimeLabel != null) currentTimeLabel.setText(now.format(timeFmt) + " - " + now.format(dateFmt));
        }

        /**
         * Get approximate location via IP geolocation and query server for weather.
         * Uses http://ip-api.com/json which returns {lat, lon, city, country, ...}
         */
        private void performGeoSearch() {
            weatherUI.showLoading(true);
            btnMyLocation.setDisable(true);
            btnSearch.setDisable(true);
            txtCity.setDisable(true);

            new Thread(() -> {
                String geoJson = null;
                try {
                    java.net.URL url = new java.net.URL("http://ip-api.com/json");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    int code = conn.getResponseCode();
                    java.io.InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                    geoJson = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    conn.disconnect();
                } catch (Exception ex) {
                    geoJson = null;
                }

                final String response = geoJson;
                Platform.runLater(() -> {
                    try {
                        setStatus("Resolving current location...");
                        if (response == null || response.isEmpty()) {
                            weatherUI.showError("⚠️ Không thể lấy vị trí hiện tại");
                            setStatus("Could not resolve location");
                            return;
                        }

                        // Robust parsing: extract numeric lat/lon and string city
                        String lat = extractJsonValue(response, "lat");
                        String lon = extractJsonValue(response, "lon");
                        String city = extractJsonString(response, "city");
                        String country = extractJsonString(response, "country");

                        // Update location card labels if available
                        try {
                            if (locationLine1 != null) {
                                if (city != null && !city.isEmpty()) {
                                    locationLine1.setText(city + (country != null ? ", " + country : ""));
                                } else if (lat != null && lon != null) {
                                    locationLine1.setText(String.format("%s, %s", lat, lon));
                                } else {
                                    locationLine1.setText("--");
                                }
                            }
                            if (locationLine2 != null) {
                                locationLine2.setText("Tự động phát hiện");
                            }
                        } catch (Exception ex) {
                            // ignore UI update errors
                        }

                        String request = null;
                        if (lat != null && lon != null) {
                            request = "coord:" + lat + "," + lon;
                        } else if (city != null && !city.isEmpty()) {
                            request = city;
                        } else {
                            weatherUI.showError("⚠️ Không tìm thấy thông tin vị trí trong phản hồi");
                            setStatus("No usable location in geolocation response");
                            return;
                        }

                        final String reqToSend = request;
                        setStatus("Querying weather for: " + reqToSend);
                        tcpClient.sendAsync(reqToSend, resp -> {
                            Platform.runLater(() -> {
                                weatherUI.showLoading(false);
                                btnMyLocation.setDisable(false);
                                btnSearch.setDisable(false);
                                txtCity.setDisable(false);

                                if (resp == null || resp.isEmpty()) {
                                    weatherUI.showError("❌ No response from server!");
                                    setStatus("Server returned empty response");
                                    return;
                                }
                                displayWeather(resp);
                                setStatus("Showing weather for: " + reqToSend);
                            });
                        });

                    } finally {
                        weatherUI.showLoading(false);
                        btnMyLocation.setDisable(false);
                        btnSearch.setDisable(false);
                        txtCity.setDisable(false);
                    }
                });
            }).start();
        }

        // Very-small helper: extract numeric value (or negative) from JSON by key
        private String extractJsonValue(String json, String key) {
            if (json == null || key == null) return null;
            try {
                String look = '"' + key + '"';
                int idx = json.indexOf(look);
                if (idx == -1) return null;
                int colon = json.indexOf(':', idx + look.length());
                if (colon == -1) return null;
                int i = colon + 1;
                // skip whitespace
                while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
                if (i >= json.length()) return null;
                // if value is a string, not numeric
                if (json.charAt(i) == '"') return null;
                int start = i;
                int end = start;
                while (end < json.length()) {
                    char c = json.charAt(end);
                    if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                        end++; continue;
                    }
                    break;
                }
                if (end == start) return null;
                return json.substring(start, end).trim();
            } catch (Exception e) {
                return null;
            }
        }

        // Very-small helper: extract string value from JSON by key
        private String extractJsonString(String json, String key) {
            if (json == null || key == null) return null;
            try {
                String look = '"' + key + '"';
                int idx = json.indexOf(look);
                if (idx == -1) return null;
                int colon = json.indexOf(':', idx + look.length());
                if (colon == -1) return null;
                int i = colon + 1;
                // skip whitespace
                while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
                if (i >= json.length() || json.charAt(i) != '"') return null;
                i++; // move past opening quote
                StringBuilder sb = new StringBuilder();
                while (i < json.length()) {
                    char c = json.charAt(i);
                    if (c == '\\') {
                        // escaped char
                        if (i + 1 < json.length()) {
                            char next = json.charAt(i + 1);
                            // handle basic escapes
                            if (next == '"' || next == '\\' || next == '/') sb.append(next);
                            else if (next == 'b') sb.append('\b');
                            else if (next == 'f') sb.append('\f');
                            else if (next == 'n') sb.append('\n');
                            else if (next == 'r') sb.append('\r');
                            else if (next == 't') sb.append('\t');
                            // skip unicode and others for brevity
                            i += 2; continue;
                        } else break;
                    }
                    if (c == '"') break;
                    sb.append(c);
                    i++;
                }
                return sb.toString();
            } catch (Exception e) { return null; }
        }

        private void setStatus(String txt) {
            try {
                if (statusLabel != null) statusLabel.setText(txt);
            } catch (Exception ignored) {}
        }

        private void performSearch() {
            String city = txtCity.getText().trim();

            if (city.isEmpty()) {
                weatherUI.showError("⚠️ Vui lòng nhập tên thành phố!");
                return;
            }

            weatherUI.showLoading(true);
            btnSearch.setDisable(true);
            txtCity.setDisable(true);

            setStatus("🔍 Đang truy vấn thông tin thời tiết cho: " + city);

            // 🔹 Thread riêng để tránh đứng giao diện
            new Thread(() -> {
                try {
                    // Send city name directly to server; map/geocoding removed.
                    String req = city;
                    tcpClient.sendAsync(req, weatherResp -> {
                        Platform.runLater(() -> {
                            if (weatherResp == null || weatherResp.isEmpty()) {
                                weatherUI.showError("❌ No response from server for weather query");
                            } else {
                                displayWeather(weatherResp);
                            }
                            // Restore UI
                            weatherUI.showLoading(false);
                            btnSearch.setDisable(false);
                            txtCity.setDisable(false);
                        });
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        weatherUI.showError("⚠️ Lỗi khi truy vấn: " + e.getMessage());
                        weatherUI.showLoading(false);
                        btnSearch.setDisable(false);
                        txtCity.setDisable(false);
                    });
                }
            }).start();
        }



        private void displayWeather(String jsonResponse) {
            try {
                // 1. Parse dữ liệu hiện tại
                WeatherDataParser.WeatherData data = dataParser.parseWeatherData(jsonResponse);
                // 2. Parse toàn bộ 40 mục dự báo (5 ngày / 3 giờ)
                List<WeatherDataParser.ForecastItem> hourlyForecast = dataParser.parseForecastData(jsonResponse);
                // 3. ➕ Tổng hợp 40 mục đó thành 5-6 mục theo ngày
                List<WeatherDataParser.DailyForecastItem> dailyForecast = dataParser.aggregateDailyForecast(hourlyForecast, data.timezone);
                
                // 4. ➕ Truyền cả 3 vào hàm hiển thị
                updateWeatherDisplay(data, hourlyForecast, dailyForecast);
                
            } catch (Exception e) {
                weatherUI.showError("❌ " + e.getMessage());
                System.err.println("Error parsing: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void updateWeatherDisplay(WeatherDataParser.WeatherData data, 
                                          List<WeatherDataParser.ForecastItem> hourly, 
                                          List<WeatherDataParser.DailyForecastItem> daily) {
            try {
                // Update weather icon
                if (data.iconCode != null && !data.iconCode.isEmpty()) {
                    weatherUI.updateWeatherIcon(WeatherHelper.getWeatherIconUrl(data.iconCode));
                }
                
                // Update basic info
                weatherUI.setTemperatures(data.temperature, data.feelsLike, data.temp_min, data.temp_max);
                weatherUI.getDescriptionLabel().setText(WeatherHelper.capitalizeFirst(data.description));
                
                if (data.cityName != null && data.country != null) {
                    weatherUI.getCityLabel().setText("📍 " + data.cityName + ", " + data.country);
                    // wire favorite button state and action
                    String cityKey = data.cityName + "," + data.country;
                    // remember current city for later UI sync
                    currentCityKey = cityKey;
                    
                    // Setup favorite button handler (only once, safe to call multiple times)
                    setupFavoriteButton();

                    // Update favorites list display (this will also set fav selection when GET_FAV returns)
                    refreshFavoritesList();
                }
                
                // "Feels like" is handled by setTemperatures (updates feels-like according to unit)
                
                // Update detailed info (4 ô chính)
                weatherUI.getHumidityLabel().setText(data.humidity + "%");
                weatherUI.getWindLabel().setText(WeatherHelper.formatWindSpeed(data.windSpeed));
                weatherUI.getPressureLabel().setText(WeatherHelper.formatPressure(data.pressure));
                weatherUI.getVisibilityLabel().setText(WeatherHelper.formatVisibility(data.visibility));
                
                // Min/Max handled by setTemperatures
                
                // Format sunrise/sunset
                weatherUI.getSunriseLabel().setText(WeatherHelper.formatTime(data.sunrise, data.timezone));
                weatherUI.getSunsetLabel().setText(WeatherHelper.formatTime(data.sunset, data.timezone));
                
                // Update background và weather animation
                boolean isDay = WeatherHelper.isDayTime(data.sunrise, data.sunset);
                weatherUI.updateWeatherBackground(data.mainWeather, isDay);
                
                // Forecast data handled locally; AI/Gemini integration removed
                
                // Update hourly forecast (vẫn lấy 8 mục đầu)
                updateHourlyForecast(hourly, data.timezone);
                
                // ➕ THÊM: Update daily forecast (pass hourly forecast for details)
                updateDailyForecast(daily, hourly);
                
            } catch (Exception e) {
                weatherUI.showError("❌ Lỗi hiển thị dữ liệu");
            }
        }

        private void updateHourlyForecast(List<WeatherDataParser.ForecastItem> forecast, int timezone) {
            try {
                HBox hourlyBox = weatherUI.getHourlyDetailsBox();
                if (hourlyBox == null) return;
                
                hourlyBox.getChildren().clear();
                
                // Hiển thị 8 giờ tiếp theo (mỗi 3 giờ = tổng 24h)
                int count = Math.min(8, forecast.size());
                for (int i = 0; i < count; i++) {
                    WeatherDataParser.ForecastItem item = forecast.get(i);
                    
                    String time = WeatherHelper.formatHour(item.dt, timezone);
                    String iconUrl = WeatherHelper.getWeatherIconUrl(item.iconCode);
                    double temp = item.temp;

                    VBox hourlyItem = weatherUI.createHourlyItem(time, iconUrl, temp);
                    hourlyBox.getChildren().add(hourlyItem);
                }
                
            } catch (Exception e) {
                System.err.println("Error updating hourly forecast: " + e.getMessage());
            }
        }

        // nhớ import ở đầu file

        private void updateDailyForecast(List<WeatherDataParser.DailyForecastItem> daily, List<WeatherDataParser.ForecastItem> hourlyForecast) {
            try {
                VBox dailyBox = weatherUI.getDailyForecastBox();
                if (dailyBox == null) return;

                dailyBox.getChildren().clear();

                int count = Math.min(5, daily.size()); // hiển thị 7 ngày nếu có
                for (int i = 0; i < count; i++) {
                    WeatherDataParser.DailyForecastItem item = daily.get(i);

                    String day;
                    if (item.weekday != null && !item.weekday.isEmpty()) {
                        day = item.weekday + " • " + item.date;
                    } else {
                        day = item.date;
                    }
                    String iconUrl = WeatherHelper.getWeatherIconUrl(item.iconCode);
                    double maxTemp = item.maxTemp;
                    double minTemp = item.minTemp;
                    String description = item.description != null ? item.description : "";

                    VBox dailyItem = weatherUI.createDailyItem(day, iconUrl, maxTemp, minTemp, description);

                    // ⚡ Thêm sự kiện click để mở chi tiết
                    dailyItem.setOnMouseClicked(e -> {
                        // Lấy dự báo theo giờ tương ứng trong ngày đó (lọc từ hourlyForecast được truyền vào)
                        List<WeatherDataParser.ForecastItem> hourlyForDay = dataParser.filterForecastForDate(hourlyForecast, item.date, item.timezone);
                        DailyDetailView.show(item, hourlyForDay);
                    });

                    dailyBox.getChildren().add(dailyItem);
                }

            } catch (Exception e) {
                System.err.println("Error updating daily forecast: " + e.getMessage());
                e.printStackTrace();
            }
        }



        @Override
        public void stop() {
            if (clockTimeline != null) {
                clockTimeline.stop();
            }
            if (tcpClient != null) {
                try {
                    tcpClient.close();
                } catch (Exception e) {
                    System.err.println("Error closing TCP client: " + e.getMessage());
                }
            }
            if (mapWindow != null && mapWindow.isShowing()) {
                mapWindow.close();
            }
        }

     // Add this to the beginning of main() method in client.java

        public static void main(String[] args) {
            // ===== FIX DPI SCALING =====
            // Must be set BEFORE launching JavaFX application
            
            // Enable HiDPI support
            System.setProperty("prism.allowhidpi", "true");
            System.setProperty("glass.win.uiScale", "100%");
            System.setProperty("glass.win.renderScale", "1.0");
            
            // Improve font rendering
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            
            // WebView optimizations
            System.setProperty("javafx.webview.uiScale", "1.0");
            
            // Detect screen scale factor
            try {
                java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                java.awt.GraphicsDevice gd = ge.getDefaultScreenDevice();
                java.awt.GraphicsConfiguration gc = gd.getDefaultConfiguration();
                java.awt.geom.AffineTransform transform = gc.getDefaultTransform();
                double scaleX = transform.getScaleX();
                double scaleY = transform.getScaleY();
                
                System.out.println("=== DPI Detection ===");
                System.out.println("Scale X: " + scaleX);
                System.out.println("Scale Y: " + scaleY);
                System.out.println("Recommended: " + (scaleX > 1.0 ? "High DPI detected" : "Normal DPI"));
                System.out.println("====================");
                
                // Auto-adjust if high DPI detected
                if (scaleX > 1.0 || scaleY > 1.0) {
                    System.setProperty("glass.win.uiScale", "100%");
                    System.setProperty("prism.order", "sw"); // Use software rendering for better quality
                }
            } catch (Exception e) {
                System.err.println("Could not detect DPI settings: " + e.getMessage());
            }
            
            // Load .env file
            EnvLoader.load();
            
            // Launch JavaFX application
            launch(args);
        }
}
package application.map;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Advanced Weather Map Window - Radar style with precipitation overlay
 */
public class WeatherMapWindow {
    
    private Stage stage;
    private WebView webView;
    private ComboBox<LayerOption> layerSelector;
    private Slider opacitySlider;
    private Label statusLabel;
    private CheckBox autoRefreshCheck;
    private Timer autoRefreshTimer;
    private boolean isShowing = false;
    private boolean mapInitialized = false;

    // SVG icon paths as constants
    private static final String SVG_PRECIPITATION = "M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z";
    private static final String SVG_CLOUDS = "M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z";
    private static final String SVG_TEMPERATURE = "M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z";
    private static final String SVG_WIND = "M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2";
    private static final String SVG_PRESSURE = "M12 2v20M17 7l-5-5-5 5M7 17l5 5 5-5";
    private static final String SVG_VIETNAM = "M12 2L2 7v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-10-5z";
    private static final String SVG_WORLD = "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 2a8 8 0 0 1 8 8 8 8 0 0 1-8 8 8 8 0 0 1-8-8 8 8 0 0 1 8-8zm0 2a6 6 0 0 0-6 6 6 6 0 0 0 6 6 6 6 0 0 0 6-6 6 6 0 0 0-6-6z";
    private static final String SVG_ZOOM_IN = "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10zm0 2v2H9v2h2v2h2v-2h2v-2h-2V8h-2zm9.71 9.29l-4-4 1.42-1.42 4 4-1.42 1.42z";
    private static final String SVG_ZOOM_OUT = "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zm0 2a5 5 0 1 1 0 10 5 5 0 0 1 0-10zM9 10v2h6v-2H9zm11.71 5.29l-4-4 1.42-1.42 4 4-1.42 1.42z";
    private static final String SVG_REFRESH = "M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    private static final String SVG_FORCE_INIT = "M13 3v18M6 8l7-7 7 7M6 16l7 7 7-7";

    // Layer options with Vietnamese names and SVG paths
    private static class LayerOption {
        String code;
        String name;
        String svgPath;
        
        LayerOption(String code, String name, String svgPath) {
            this.code = code;
            this.name = name;
            this.svgPath = svgPath;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    private static final LayerOption[] LAYERS = {
        new LayerOption("precipitation_new", "Mưa / Precipitation", SVG_PRECIPITATION),
        new LayerOption("clouds_new", "Mây / Clouds", SVG_CLOUDS),
        new LayerOption("temp_new", "Nhiệt độ / Temperature", SVG_TEMPERATURE),
        new LayerOption("wind_new", "Gió / Wind Speed", SVG_WIND),
        new LayerOption("pressure_new", "Áp suất / Pressure", SVG_PRESSURE)
    };

    public WeatherMapWindow() {
        initializeWindow();
    }
   
    private void initializeWindow() {
        stage = new Stage();
        stage.setTitle("Weather Map - Bản đồ thời tiết ");
        stage.initStyle(StageStyle.DECORATED);
        
        // Fix DPI scaling for high-resolution displays
        System.setProperty("prism.allowhidpi", "true");
        System.setProperty("glass.win.uiScale", "100%");
        System.setProperty("glass.win.renderScale", "1");
        
        BorderPane root = new BorderPane();
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);" +
            "-fx-padding: 0;"
        );
        
        // Top panel with controls
        VBox topPanel = createControlPanel();
        root.setTop(topPanel);
        
        // Center: WebView with map
        webView = new WebView();
        webView.getEngine().setJavaScriptEnabled(true);
        webView.setContextMenuEnabled(false);
        
        // Fix WebView DPI scaling
        webView.setZoom(1.0);
        webView.getEngine().setUserStyleSheetLocation(
            "data:text/css," + java.net.URLEncoder.encode(
                "body { zoom: 1.0 !important; }", 
                java.nio.charset.StandardCharsets.UTF_8
            )
        );
        
        StackPane mapContainer = new StackPane(webView);
        mapContainer.setPadding(new Insets(0));
        root.setCenter(mapContainer);
        
        // Bottom status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        // Load map
        loadAdvancedMapHTML();
        
        Scene scene = new Scene(root, 1400, 900);
        stage.setScene(scene);
        stage.setMaximized(true);
        
        stage.setOnCloseRequest(e -> {
            stopAutoRefresh();
            isShowing = false;
            mapInitialized = false;
        });
    }

    /**
     * Create SVG icon from path string
     */
    private Node createSvgIcon(String svgPath, double size, String color) {
        SVGPath svg = new SVGPath();
        svg.setContent(svgPath);
        svg.setFill(Color.web(color));
        
        // Scale to desired size (SVG viewBox is typically 24x24)
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        
        // Center the icon in a container
        StackPane container = new StackPane(svg);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);
        container.setMinSize(size, size);
        
        return container;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15, 25, 15, 25));
        panel.setStyle(
            "-fx-background-color: rgba(20, 30, 48, 0.95);" +
            "-fx-border-color: rgba(52, 152, 219, 0.3);" +
            "-fx-border-width: 0 0 2 0;"
        );
        
        // Title with animated gradient effect
        Label title = new Label("WEATHER RADAR MAP - VIỆT NAM");
        title.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: linear-gradient(to right, #3498db, #2ecc71);" +
            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.6), 8, 0, 0, 2);"
        );
        
        HBox controlsRow = new HBox(25);
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        
        // Layer selector
        VBox layerBox = createLayerSelector();
        
        // Opacity control
        VBox opacityBox = createOpacityControl();
        
        // Map controls
        VBox mapControlsBox = createMapControls();
        
        // Auto-refresh
        autoRefreshCheck = new CheckBox("Auto Refresh (30s)");
        autoRefreshCheck.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: 600;" +
            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.4), 3, 0, 0, 1);"
        );
        autoRefreshCheck.selectedProperty().addListener((obs, old, val) -> {
            if (val) startAutoRefresh();
            else stopAutoRefresh();
        });
        
        controlsRow.getChildren().addAll(layerBox, opacityBox, mapControlsBox, autoRefreshCheck);
        
        panel.getChildren().addAll(title, controlsRow);
        return panel;
    }

    private VBox createLayerSelector() {
        VBox box = new VBox(6);
        Label label = new Label("Map Layer:");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.6), 4, 0, 0, 1);");
        
        layerSelector = new ComboBox<>();
        layerSelector.getItems().addAll(LAYERS);
        layerSelector.setValue(LAYERS[0]); // Default: Precipitation
        layerSelector.setPrefWidth(240);
        layerSelector.setStyle(
            "-fx-background-color: rgba(52, 152, 219, 0.2);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(52, 152, 219, 0.5);" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 2;"
        );
        
        // Custom cell factory for dropdown items
        layerSelector.setCellFactory(listView -> new ListCell<LayerOption>() {
            @Override
            protected void updateItem(LayerOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Node icon = createSvgIcon(item.svgPath, 20, "#3498db");
                    setGraphic(icon);
                    setText(item.name);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        // Custom button cell for selected item
        layerSelector.setButtonCell(new ListCell<LayerOption>() {
            @Override
            protected void updateItem(LayerOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Node icon = createSvgIcon(item.svgPath, 20, "#3498db");
                    setGraphic(icon);
                    setText(item.name);
                    setStyle("-fx-text-fill: white;");
                }
            }
        });

        layerSelector.setOnAction(e -> changeMapLayer());
        box.getChildren().addAll(label, layerSelector);
        return box;
    }

    private VBox createOpacityControl() {
        VBox box = new VBox(6);
        
        HBox labelBox = new HBox(10);
        labelBox.setAlignment(Pos.CENTER_LEFT);
        Label label = new Label("Layer Opacity:");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.6), 4, 0, 0, 1);");
        
        Label valueLabel = new Label("70%");
        valueLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.6), 4, 0, 0, 1);");
        labelBox.getChildren().addAll(label, valueLabel);
        
        opacitySlider = new Slider(0, 100, 70);
        opacitySlider.setPrefWidth(200);
        opacitySlider.setShowTickMarks(true);
        opacitySlider.setMajorTickUnit(25);
        opacitySlider.setStyle(
            "-fx-control-inner-background: rgba(52, 152, 219, 0.3);"
        );
        
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percent = newVal.intValue();
            valueLabel.setText(percent + "%");
            updateLayerOpacity(percent / 100.0);
        });
        
        box.getChildren().addAll(labelBox, opacitySlider);
        return box;
    }

    private VBox createMapControls() {
        VBox box = new VBox(6);
        Label label = new Label("Map Controls:");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 600; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.6), 4, 0, 0, 1);");
        
        HBox buttons = new HBox(8);
        
        // Create buttons with SVG icons
        Button vietnam = createStyledButton(SVG_VIETNAM, "Vietnam View");
        Button world = createStyledButton(SVG_WORLD, "World View");
        Button zoomIn = createStyledButton(SVG_ZOOM_IN, "Zoom In");
        Button zoomOut = createStyledButton(SVG_ZOOM_OUT, "Zoom Out");
        Button refresh = createStyledButton(SVG_REFRESH, "Refresh");
        Button forceInit = createStyledButton(SVG_FORCE_INIT, "Force Initialize");
        
        // Set button actions
        vietnam.setOnAction(e -> executeJS("map.setView([16.0, 108.0], 6);"));
        world.setOnAction(e -> executeJS("map.setView([20.0, 0.0], 2);"));
        zoomIn.setOnAction(e -> executeJS("map.zoomIn();"));
        zoomOut.setOnAction(e -> executeJS("map.zoomOut();"));
        refresh.setOnAction(e -> refreshWeatherLayer());
        forceInit.setOnAction(e -> {
            System.out.println("Force initializing map...");
            mapInitialized = true;
            changeMapLayer();
            updateStatus("Map force-initialized");
        });
        
        buttons.getChildren().addAll(vietnam, world, zoomIn, zoomOut, refresh, forceInit);
        box.getChildren().addAll(label, buttons);
        return box;
    }

    /**
     * Create a styled button with SVG icon
     */
    private Button createStyledButton(String svgPath, String tooltipText) {
        Button button = new Button();
        Node icon = createSvgIcon(svgPath, 20, "white");
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(tooltipText));
        button.setPrefSize(45, 40);
        
        String baseStyle = 
            "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.3);" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1.5;";
        
        String hoverStyle = 
            "-fx-background-color: linear-gradient(to bottom, #5dade2, #3498db);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.6);" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.6), 10, 0, 0, 0);";
        
        button.setStyle(baseStyle);
        
        button.setOnMouseEntered(e -> {
            button.setStyle(hoverStyle);
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        
        return button;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(12);
        statusBar.setPadding(new Insets(10, 20, 10, 20));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle(
            "-fx-background-color: rgba(20, 30, 48, 0.95);" +
            "-fx-border-color: rgba(52, 152, 219, 0.3);" +
            "-fx-border-width: 2 0 0 0;"
        );
        
        Label indicator = new Label("●");
        indicator.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-effect: dropshadow(gaussian, rgba(46, 204, 113, 0.6), 6, 0, 0, 0);");
        
        statusLabel = new Label("Map initializing...");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 500; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.5), 5, 0, 0, 1);");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timestamp = new Label("Last update: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        timestamp.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 12px; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.4), 4, 0, 0, 1);");
        
        Label credit = new Label("Data: OpenWeatherMap | Map: Leaflet.js");
        credit.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px; -fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.4), 4, 0, 0, 1);");
        
        statusBar.getChildren().addAll(indicator, statusLabel, spacer, timestamp, credit);
        return statusBar;
    }

    private void loadAdvancedMapHTML() {
        webView.getEngine().load(getClass().getResource("map.html").toExternalForm());

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                Timer initTimer = new Timer(true);
                final int[] retryCount = {0};
                final int maxRetries = 15;
                
                TimerTask checkTask = new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            try {
                                Object leafletExists = webView.getEngine().executeScript("typeof L !== 'undefined'");
                                Object mapExists = webView.getEngine().executeScript("typeof window.map !== 'undefined'");
                                Object functionExists = webView.getEngine().executeScript("typeof window.setWeatherLayer === 'function'");
                                Object weatherLayerExists = webView.getEngine().executeScript("typeof window.weatherLayer !== 'undefined'");
                                Object ready = webView.getEngine().executeScript("typeof window.mapReady !== 'undefined' && window.mapReady");
                                
                                System.out.println("Init check #" + retryCount[0] + 
                                    " - Leaflet: " + leafletExists + 
                                    ", Map: " + mapExists + 
                                    ", Function: " + functionExists + 
                                    ", WeatherLayer: " + weatherLayerExists +
                                    ", Ready: " + ready);
                                
                                if (Boolean.TRUE.equals(ready) && Boolean.TRUE.equals(weatherLayerExists)) {
                                    mapInitialized = true;
                                    updateStatus("Map loaded successfully - Precipitation radar active");
                                    this.cancel();
                                } else {
                                    retryCount[0]++;
                                    if (retryCount[0] < maxRetries) {
                                        updateStatus("Loading weather map... (" + retryCount[0] + "/" + maxRetries + ")");
                                    } else {
                                        updateStatus("Map ready - Default layer loaded");
                                        mapInitialized = true;
                                        this.cancel();
                                    }
                                }
                            } catch (Exception e) {
                                retryCount[0]++;
                                System.err.println("Map init check error (attempt " + retryCount[0] + "): " + e.getMessage());
                                if (retryCount[0] >= maxRetries) {
                                    updateStatus("Map initialized");
                                    mapInitialized = true;
                                    this.cancel();
                                }
                            }
                        });
                    }
                };
                
                initTimer.scheduleAtFixedRate(checkTask, 500, 500);
            } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                updateStatus("Map failed to load");
            }
        });
    }

    private void changeMapLayer() {
        if (!mapInitialized) {
            System.out.println("Map not initialized yet, queuing layer change...");
            return;
        }
        
        LayerOption selected = layerSelector.getValue();
        if (selected != null) {
            double opacity = opacitySlider.getValue() / 100.0;
            String js = String.format("setWeatherLayer('%s', %.2f);", selected.code, opacity);
            executeJS(js);
            updateStatus("Layer: " + selected.name);
        }
    }

    private void updateLayerOpacity(double opacity) {
        if (!mapInitialized) return;
        executeJS(String.format("setLayerOpacity(%.2f);", opacity));
    }

    private void refreshWeatherLayer() {
        if (!mapInitialized) {
            updateStatus("Map not ready for refresh");
            return;
        }
        changeMapLayer();
        updateStatus("Weather data refreshed");
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimer = new Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    refreshWeatherLayer();
                    updateStatus("Auto-refreshed at " + 
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                });
            }
        }, 30000, 30000);
        updateStatus("Auto-refresh enabled (30s interval)");
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.cancel();
            autoRefreshTimer = null;
        }
    }

    private void executeJS(String script) {
        if (!mapInitialized) {
            System.out.println("Skipping JS execution - map not ready: " + script);
            return;
        }
        
        Platform.runLater(() -> {
            try {
                System.out.println("Executing JS: " + script);
                Object result = webView.getEngine().executeScript(script);
                System.out.println("JS result: " + result);
            } catch (Exception e) {
                System.err.println("JS execution error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    public void show() {
        if (!isShowing) {
            stage.show();
            isShowing = true;
        } else {
            stage.toFront();
        }
    }

    public void close() {
        stopAutoRefresh();
        if (stage != null) {
            stage.close();
            isShowing = false;
            mapInitialized = false;
        }
    }

    public boolean isShowing() {
        return isShowing;
    }
}
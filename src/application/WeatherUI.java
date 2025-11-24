package application;

import application.EnvLoader;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * Weather UI with FIXED animated backgrounds
 */
public class WeatherUI {
    
    private StackPane rootPane;
    private Pane animationLayer;
    private ProgressIndicator spinner;
    private ImageView weatherIcon;
    private Label temperatureLabel, descriptionLabel, cityLabel;
    private Label feelsLikeLabel, humidityLabel, windLabel, sunriseLabel;
    private Label sunsetLabel, pressureLabel, visibilityLabel, minTempLabel, maxTempLabel;
    private VBox weatherContentBox;
    private HBox hourlyDetailsBox;
    private VBox dailyForecastBox;
    private Timeline currentAnimation;
    private VBox favoritesBox;
    // Temperature unit state (true = Celsius, false = Fahrenheit)
    private boolean useCelsius = true;
    private ToggleButton tempUnitToggle;
    private ToggleButton favoriteButton;
    // Last known temperature numbers (in Celsius)
    private double lastTemp = Double.NaN, lastFeelsLike = Double.NaN, lastMin = Double.NaN, lastMax = Double.NaN;
    
    // Base colors for animated gradient
    private Color baseC1 = Color.rgb(102, 126, 234);
    private Color baseC2 = Color.rgb(118, 75, 162);
    private Color baseC3 = Color.rgb(237, 66, 100);
    private Color baseC4 = Color.rgb(118, 75, 162);

    // Getters
    public StackPane getRootPane() { 
        if (rootPane == null) {
            rootPane = new StackPane();
            rootPane.getChildren().add(getAnimationLayer()); // This will create animationLayer + backgroundRect
        }
        return rootPane; 
    }

    /**
     * Shorten alert text: prefer the first sentence; otherwise truncate to maxChars and add ellipsis.
     */
    public ProgressIndicator getSpinner() { return spinner; }
    public ImageView getWeatherIcon() { return weatherIcon; }
    public Label getTemperatureLabel() { return temperatureLabel; }
    public Label getDescriptionLabel() { return descriptionLabel; }
    public Label getCityLabel() { return cityLabel; }
    public Label getFeelsLikeLabel() { return feelsLikeLabel; }
    public Label getHumidityLabel() { return humidityLabel; }
    public Label getWindLabel() { return windLabel; }
    public Label getSunriseLabel() { return sunriseLabel; }
    public Label getSunsetLabel() { return sunsetLabel; }
    public Label getPressureLabel() { return pressureLabel; }
    public Label getVisibilityLabel() { return visibilityLabel; }
    public Label getMinTempLabel() { return minTempLabel; }
    public Label getMaxTempLabel() { return maxTempLabel; }
    public HBox getHourlyDetailsBox() { return hourlyDetailsBox; }
    public VBox getDailyForecastBox() { return dailyForecastBox; }
    public ToggleButton getTempUnitToggle() { return tempUnitToggle; }
    public ToggleButton getFavoriteButton() { return favoriteButton; }
    public VBox getFavoritesBox() { return favoritesBox; }

    /**
     * Set the main temperatures (values in Celsius). UI will display according to current unit.
     */
    public void setTemperatures(double tempC, double feelsLikeC, double minC, double maxC) {
        this.lastTemp = tempC;
        this.lastFeelsLike = feelsLikeC;
        this.lastMin = minC;
        this.lastMax = maxC;
        updateTemperatureLabels();
    }

    /**
     * Format a temperature value according to current selected unit.
     */
    public String formatTempForUnit(double celsius) {
        if (useCelsius) return WeatherHelper.formatTemperature(celsius);
        return WeatherHelper.formatTemperatureF(celsius);
    }

    private void updateTemperatureLabels() {
        if (!Double.isNaN(lastTemp) && temperatureLabel != null) {
            temperatureLabel.setText(formatTempForUnit(lastTemp));
            // color the main temperature number according to Celsius value
            try {
                temperatureLabel.setTextFill(temperatureColorForC(lastTemp));
            } catch (Exception ignore) {}
        }
        if (!Double.isNaN(lastFeelsLike) && feelsLikeLabel != null) {
            feelsLikeLabel.setText("Cảm nhận " + formatTempForUnit(lastFeelsLike));
        }
        if (!Double.isNaN(lastMin) && minTempLabel != null) {
            minTempLabel.setText(formatTempForUnit(lastMin));
        }
        if (!Double.isNaN(lastMax) && maxTempLabel != null) {
            maxTempLabel.setText(formatTempForUnit(lastMax));
        }
    }

    /**
     * Return a Color appropriate for a Celsius temperature. Cooler temps -> blue-ish, hot -> red/orange.
     */
    private Color temperatureColorForC(double celsius) {
        try {
            if (Double.isNaN(celsius)) return Color.WHITE;
            if (celsius <= 0) return Color.web("#9BE7FF");
            if (celsius <= 10) return Color.web("#7FD3FF");
            if (celsius <= 20) return Color.web("#A8E6FF");
            if (celsius <= 25) return Color.web("#FFD27D");
            if (celsius <= 30) return Color.web("#FFAA33");
            if (celsius <= 35) return Color.web("#FF6B6B");
            return Color.web("#B22222");
        } catch (Exception ex) {
            return Color.WHITE;
        }
    }

    /**
     * Refresh all forecast temperature labels (hourly and daily) using stored raw Celsius values.
     */
    public void refreshForecastTemperatures() {
        try {
            if (hourlyDetailsBox != null) {
                for (javafx.scene.Node node : hourlyDetailsBox.getChildren()) {
                    if (node instanceof VBox) {
                        VBox vb = (VBox) node;
                        if (vb.getChildren().size() >= 3) {
                            javafx.scene.Node tempNode = vb.getChildren().get(2);
                            if (tempNode instanceof Label) {
                                Object ud = tempNode.getUserData();
                                if (ud instanceof Double) {
                                    double c = (Double) ud;
                                    ((Label) tempNode).setText(formatTempForUnit(c));
                                    try { ((Label) tempNode).setTextFill(temperatureColorForC(c)); } catch (Exception ignore) {}
                                }
                            }
                        }
                    }
                }
            }

            if (dailyForecastBox != null) {
                for (javafx.scene.Node node : dailyForecastBox.getChildren()) {
                    if (node instanceof VBox) {
                        VBox vb = (VBox) node;
                        if (vb.getChildren().size() >= 4) {
                            javafx.scene.Node maxNode = vb.getChildren().get(2);
                            javafx.scene.Node minNode = vb.getChildren().get(3);
                            if (maxNode instanceof Label) {
                                Object ud = maxNode.getUserData();
                                if (ud instanceof Double) {
                                    double c = (Double) ud;
                                    ((Label) maxNode).setText(formatTempForUnit(c));
                                    try { ((Label) maxNode).setTextFill(temperatureColorForC(c)); } catch (Exception ignore) {}
                                }
                            }
                            if (minNode instanceof Label) {
                                Object ud = minNode.getUserData();
                                if (ud instanceof Double) {
                                    double c = (Double) ud;
                                    ((Label) minNode).setText(formatTempForUnit(c));
                                    try { ((Label) minNode).setTextFill(temperatureColorForC(c)); } catch (Exception ignore) {}
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error refreshing forecast temps: " + ex.getMessage());
        }
    }

    /**
     * CRITICAL: This method must be called to get the animation layer
     * The animation layer should be added to the root BorderPane as the BOTTOM layer
     */
    public Pane getAnimationLayer() {
        if (animationLayer == null) {
            animationLayer = new Pane();
            animationLayer.setMouseTransparent(true);
            animationLayer.setPickOnBounds(false);
            // Start with default animated background
            createAnimatedBackground();
        }
        return animationLayer;
    }

    /**
     * Ensure core label fields are initialized
     */
    public void ensureInit() {
        if (temperatureLabel == null) temperatureLabel = new Label("--°C");
        if (descriptionLabel == null) descriptionLabel = new Label("--");
        if (cityLabel == null) cityLabel = new Label("Vị trí: --, --");
        if (feelsLikeLabel == null) feelsLikeLabel = new Label("Feels like --°C");
        if (humidityLabel == null) humidityLabel = new Label("--");
        if (windLabel == null) windLabel = new Label("--");
        if (sunriseLabel == null) sunriseLabel = new Label("--");
        if (sunsetLabel == null) sunsetLabel = new Label("--");
        if (pressureLabel == null) pressureLabel = new Label("--");
        if (visibilityLabel == null) visibilityLabel = new Label("--");
        if (minTempLabel == null) minTempLabel = new Label("--");
        if (maxTempLabel == null) maxTempLabel = new Label("--");
        
    if (hourlyDetailsBox == null) hourlyDetailsBox = new HBox(10);
    if (dailyForecastBox == null) dailyForecastBox = new VBox(8);
        
        if (spinner == null) {
            spinner = new ProgressIndicator();
            spinner.setPrefSize(60, 60);
            spinner.setStyle(
                "-fx-progress-color: white;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 10, 0, 0, 0);"
            );
            spinner.setVisible(false);
        }
        
        if (weatherIcon == null) {
            weatherIcon = new ImageView();
            weatherIcon.setFitWidth(90);
            weatherIcon.setFitHeight(90);
            weatherIcon.setPreserveRatio(true);
        }
        
        if (animationLayer == null) {
            animationLayer = new Pane();
            animationLayer.setMouseTransparent(true);
            animationLayer.setPickOnBounds(false);
        }
        
        // Initialize favorite button if not exists
        if (favoriteButton == null) {
            favoriteButton = new ToggleButton();
            favoriteButton.setTooltip(new Tooltip("Thêm vào yêu thích"));
            favoriteButton.setPrefWidth(44);
            favoriteButton.setPrefHeight(44);
            favoriteButton.setFocusTraversable(false);
            
            SVGPath heart = new SVGPath();
            heart.setContent("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
            heart.setScaleX(0.9);
            heart.setScaleY(0.9);
            heart.setStroke(Color.WHITE);
            heart.setStrokeWidth(0.5);
            heart.setFill(Color.color(1,1,1,0.0));
            heart.setEffect(new DropShadow(6, 1, 1, Color.rgb(0,0,0,0.35)));
            favoriteButton.setGraphic(heart);
            
            favoriteButton.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 22;" +
                "-fx-border-color: rgba(255,255,255,0.14);" +
                "-fx-border-radius: 22;" +
                "-fx-border-width: 1.2;" +
                "-fx-cursor: hand;"
            );
            
            favoriteButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    heart.setFill(Color.web("#FF4D6D"));
                    heart.setStroke(Color.web("#FF2D55"));
                    favoriteButton.setScaleX(1.08);
                    favoriteButton.setScaleY(1.08);
                } else {
                    heart.setFill(Color.color(1,1,1,0.0));
                    heart.setStroke(Color.WHITE);
                    favoriteButton.setScaleX(1.0);
                    favoriteButton.setScaleY(1.0);
                }
            });
        }
    }
    
    /**
     * Create animated gradient background that cycles between base colors
     */
    private void createAnimatedBackground() {
        if (animationLayer == null) return;
        
        // Clear previous content
        animationLayer.getChildren().clear();
        
        // Create a rectangle that fills the entire animation layer
        Rectangle bgRect = new Rectangle();
        bgRect.widthProperty().bind(animationLayer.widthProperty());
        bgRect.heightProperty().bind(animationLayer.heightProperty());
        
        // Animate gradient colors
        DoubleProperty blendProp = new SimpleDoubleProperty(0);
        blendProp.addListener((obs, oldVal, newVal) -> {
            double blend = newVal.doubleValue();
            Stop[] stops = new Stop[]{
                new Stop(0, baseC1.interpolate(baseC3, blend)),
                new Stop(1, baseC2.interpolate(baseC4, blend))
            };
            bgRect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops));
        });
        
        animationLayer.getChildren().add(bgRect);
        
        // Animate the blend
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(blendProp, 0)),
            new KeyFrame(Duration.seconds(15), new KeyValue(blendProp, 1))
        );
        timeline.setAutoReverse(true);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        currentAnimation = timeline;
    }

    /**
     * Set base gradient colors according to weather condition
     */
    public void setThemeForCondition(String mainWeather, boolean isDay) {
        if (mainWeather == null) mainWeather = "clear";
        String w = mainWeather.toLowerCase();
        
        switch (w) {
            case "rain":
            case "drizzle":
                baseC1 = Color.web("#4b79a1");
                baseC2 = Color.web("#283e51");
                baseC3 = Color.web("#659999");
                baseC4 = Color.web("#2b5876");
                break;
            case "thunderstorm":
                baseC1 = Color.web("#232526");
                baseC2 = Color.web("#414345");
                baseC3 = Color.web("#1f1c2c");
                baseC4 = Color.web("#283048");
                break;
            case "clouds":
                baseC1 = Color.web("#bdc3c7");
                baseC2 = Color.web("#2c3e50");
                baseC3 = Color.web("#95a5a6");
                baseC4 = Color.web("#6d7b8a");
                break;
            case "snow":
                baseC1 = Color.web("#e6f0ff");
                baseC2 = Color.web("#b3d4ff");
                baseC3 = Color.web("#dfefff");
                baseC4 = Color.web("#9fc5ff");
                break;
            case "mist":
            case "fog":
            case "haze":
                baseC1 = Color.web("#d7d2cc");
                baseC2 = Color.web("#304352");
                baseC3 = Color.web("#c7c5c5");
                baseC4 = Color.web("#95a3a4");
                break;
            case "clear":
            default:
                if (isDay) {
                    baseC1 = Color.web("#FFD27D");
                    baseC2 = Color.web("#FF9A76");
                    baseC3 = Color.web("#FF6B6B");
                    baseC4 = Color.web("#FFD27D");
                } else {
                    baseC1 = Color.web("#0f2027");
                    baseC2 = Color.web("#203a43");
                    baseC3 = Color.web("#2c5364");
                    baseC4 = Color.web("#0f2027");
                }
                break;
        }
    }
    
    /**
     * Update weather background with animations
     */
    public void updateWeatherBackground(String mainWeather, boolean isDay) {
        setThemeForCondition(mainWeather, isDay);
        startWeatherAnimation(mainWeather, isDay);
    }
    
    /**
     * Start weather-specific animation effects
     */
    private void startWeatherAnimation(String mainWeather, boolean isDay) {
        stopCurrentAnimation();
        
        if (mainWeather == null) {
            createAnimatedBackground();
            return;
        }
        
        switch (mainWeather.toLowerCase()) {
            case "rain": 
                createRainEffect(); 
                break;
            case "drizzle": 
                createDrizzleEffect(); 
                break;
            case "thunderstorm": 
                createThunderstormEffect(); 
                break;
            case "snow": 
                createSnowEffect(); 
                break;
            case "clouds": 
                createCloudsEffect(); 
                break;
            case "clear":
                if (isDay) createSunnyEffect();
                else createStarsEffect();
                break;
            case "mist":
            case "fog":
            case "haze":
                createFogEffect();
                break;
            default:
                createAnimatedBackground();
                break;
        }
    }
    
    // ===== WEATHER ANIMATION EFFECTS =====
    
    private void createRainEffect() {
        animationLayer.getChildren().clear();
        
        // Animated gradient background
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        DoubleProperty blend = new SimpleDoubleProperty(0);
        blend.addListener((obs, o, n) -> {
            LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, baseC1.deriveColor(0, 1.0, 1.0, 0.85).interpolate(baseC3.deriveColor(0, 1.0, 0.9, 0.90), n.doubleValue())),
                new Stop(1, baseC2.deriveColor(0, 1.0, 0.9, 0.88).interpolate(baseC4.deriveColor(0, 1.0, 0.85, 0.92), n.doubleValue())));
            bg.setFill(grad);
        });
        animationLayer.getChildren().add(bg);
        
        Timeline bgAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(blend, 0)),
            new KeyFrame(Duration.seconds(12), new KeyValue(blend, 1))
        );
        bgAnim.setAutoReverse(true);
        bgAnim.setCycleCount(Animation.INDEFINITE);
        bgAnim.play();
        currentAnimation = bgAnim;
        
        // Rain drops
        for (int i = 0; i < 150; i++) {
            Line raindrop = new Line();
            double opacity = 0.3 + Math.random() * 0.5;
            double width = 0.8 + Math.random() * 1.2;
            double length = 15 + Math.random() * 10;
            
            LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(200, 220, 255, opacity)),
                new Stop(1, Color.rgb(150, 180, 255, opacity * 0.3)));
            raindrop.setStroke(gradient);
            raindrop.setStrokeWidth(width);
            
            double startX = Math.random() * 2000;
            double startY = Math.random() * -1000;
            raindrop.setStartX(startX);
            raindrop.setStartY(startY);
            raindrop.setEndX(startX - 10);
            raindrop.setEndY(startY + length);
            
            animationLayer.getChildren().add(raindrop);

            TranslateTransition fall = new TranslateTransition(Duration.millis(700 + Math.random() * 500), raindrop);
            fall.setToY(1200);
            fall.setCycleCount(Animation.INDEFINITE);
            fall.setDelay(Duration.millis(Math.random() * 2000));
            fall.play();
        }
        
        // Rain splashes
        for (int i = 0; i < 20; i++) {
            Circle splash = new Circle(0);
            splash.setFill(Color.TRANSPARENT);
            splash.setStroke(Color.rgb(200, 220, 255, 0.4));
            splash.setStrokeWidth(1);
            
            double x = Math.random() * 1500;
            splash.setLayoutX(x);
            splash.layoutYProperty().bind(animationLayer.heightProperty().subtract(50));
            animationLayer.getChildren().add(splash);
            
            Timeline splashAnim = new Timeline(
                new KeyFrame(Duration.ZERO, 
                    new KeyValue(splash.radiusProperty(), 0),
                    new KeyValue(splash.opacityProperty(), 0.6)),
                new KeyFrame(Duration.millis(400), 
                    new KeyValue(splash.radiusProperty(), 15),
                    new KeyValue(splash.opacityProperty(), 0))
            );
            splashAnim.setCycleCount(Animation.INDEFINITE);
            splashAnim.setDelay(Duration.millis(Math.random() * 3000));
            splashAnim.play();
        }
    }

    private void createDrizzleEffect() {
        animationLayer.getChildren().clear();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, baseC1.deriveColor(0, 1.0, 1.1, 0.80)),
            new Stop(1, baseC2.deriveColor(0, 1.0, 1.0, 0.85)));
        bg.setFill(grad);
        animationLayer.getChildren().add(bg);
        
        for (int i = 0; i < 100; i++) {
            Circle drop = new Circle(0.8 + Math.random() * 1.2);
            RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(200, 220, 255, 0.7)),
                new Stop(1, Color.rgb(150, 180, 255, 0.3)));
            drop.setFill(gradient);
            
            double startX = Math.random() * 1500;
            double startY = Math.random() * -1000;
            drop.setLayoutX(startX);
            drop.setLayoutY(startY);
            animationLayer.getChildren().add(drop);

            TranslateTransition fall = new TranslateTransition(Duration.millis(2500 + Math.random() * 1500), drop);
            fall.setToY(1200);
            fall.setCycleCount(Animation.INDEFINITE);
            fall.setDelay(Duration.millis(Math.random() * 3000));
            fall.play();
        }
    }

    private void createThunderstormEffect() {
        animationLayer.getChildren().clear();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, baseC1.deriveColor(0, 1.0, 0.7, 0.90)),
            new Stop(1, baseC3.deriveColor(0, 1.0, 0.6, 0.95)));
        bg.setFill(grad);
        animationLayer.getChildren().add(bg);
        
        // Heavy rain
        for (int i = 0; i < 200; i++) {
            Line raindrop = new Line();
            LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(200, 220, 255, 0.8)),
                new Stop(1, Color.rgb(100, 150, 255, 0.4)));
            raindrop.setStroke(gradient);
            raindrop.setStrokeWidth(2 + Math.random() * 2);
            
            double startX = Math.random() * 1500;
            double startY = Math.random() * -1000;
            raindrop.setStartX(startX);
            raindrop.setStartY(startY);
            raindrop.setEndX(startX - 15);
            raindrop.setEndY(startY + 40);
            animationLayer.getChildren().add(raindrop);

            TranslateTransition fall = new TranslateTransition(Duration.millis(400 + Math.random() * 300), raindrop);
            fall.setToY(1200);
            fall.setCycleCount(Animation.INDEFINITE);
            fall.setDelay(Duration.millis(Math.random() * 1500));
            fall.play();
        }

        // Lightning flash
        Rectangle flash = new Rectangle();
        flash.widthProperty().bind(animationLayer.widthProperty());
        flash.heightProperty().bind(animationLayer.heightProperty());
        flash.setFill(Color.rgb(255, 255, 255, 0));
        animationLayer.getChildren().add(flash);
        
        Timeline lightning = new Timeline(
            new KeyFrame(Duration.ZERO, 
                new KeyValue(flash.fillProperty(), Color.rgb(255, 255, 255, 0))),
            new KeyFrame(Duration.millis(50), 
                new KeyValue(flash.fillProperty(), Color.rgb(220, 235, 255, 0.6))),
            new KeyFrame(Duration.millis(100), 
                new KeyValue(flash.fillProperty(), Color.rgb(255, 255, 255, 0))),
            new KeyFrame(Duration.millis(150), 
                new KeyValue(flash.fillProperty(), Color.rgb(230, 240, 255, 0.4))),
            new KeyFrame(Duration.millis(200), 
                new KeyValue(flash.fillProperty(), Color.rgb(255, 255, 255, 0)))
        );
        lightning.setCycleCount(Animation.INDEFINITE);
        lightning.setDelay(Duration.seconds(3 + Math.random() * 5));
        lightning.play();
        currentAnimation = lightning;
    }

    private void createSnowEffect() {
        animationLayer.getChildren().clear();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, baseC1.deriveColor(0, 0.9, 1.2, 0.88)),
            new Stop(1, baseC2.deriveColor(0, 0.9, 1.15, 0.90)));
        bg.setFill(grad);
        animationLayer.getChildren().add(bg);
        
        for (int i = 0; i < 120; i++) {
            Circle snowflake = new Circle(2 + Math.random() * 4);
            RadialGradient gradient = new RadialGradient(0, 0, 0.3, 0.3, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255, 0.95)),
                new Stop(0.7, Color.rgb(230, 240, 255, 0.7)),
                new Stop(1, Color.rgb(200, 220, 255, 0.3)));
            snowflake.setFill(gradient);
            snowflake.setEffect(new Bloom(0.3));
            
            double startX = Math.random() * 1500;
            double startY = Math.random() * -1000;
            snowflake.setLayoutX(startX);
            snowflake.setLayoutY(startY);
            animationLayer.getChildren().add(snowflake);

            double fallDuration = 6000 + Math.random() * 5000;
            TranslateTransition fall = new TranslateTransition(Duration.millis(fallDuration), snowflake);
            fall.setToY(1200);
            fall.setCycleCount(Animation.INDEFINITE);
            fall.setDelay(Duration.millis(Math.random() * 6000));
            fall.play();

            TranslateTransition sway = new TranslateTransition(Duration.millis(2000 + Math.random() * 1500), snowflake);
            sway.setByX(-40 + Math.random() * 80);
            sway.setCycleCount(Animation.INDEFINITE);
            sway.setAutoReverse(true);
            sway.play();

            RotateTransition rotate = new RotateTransition(Duration.millis(3000 + Math.random() * 3000), snowflake);
            rotate.setByAngle(360);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.play();
        }
    }

    private void createCloudsEffect() {
        animationLayer.getChildren().clear();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, baseC1.deriveColor(0, 1.0, 1.0, 0.82)),
            new Stop(1, baseC2.deriveColor(0, 1.0, 0.95, 0.85)));
        bg.setFill(grad);
        animationLayer.getChildren().add(bg);
        
        for (int i = 0; i < 10; i++) {
            Pane cloudGroup = new Pane();
            
            for (int j = 0; j < 5; j++) {
                Circle cloudPart = new Circle(30 + Math.random() * 40);
                RadialGradient gradient = new RadialGradient(0, 0, 0.4, 0.4, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 255, 255, 0.6)),
                    new Stop(0.7, Color.rgb(230, 235, 245, 0.4)),
                    new Stop(1, Color.rgb(200, 210, 230, 0.2)));
                cloudPart.setFill(gradient);
                cloudPart.setEffect(new GaussianBlur(30));
                cloudPart.setLayoutX(j * 40 - 80);
                cloudPart.setLayoutY(Math.random() * 30 - 15);
                cloudGroup.getChildren().add(cloudPart);
            }
            
            double startY = 50 + Math.random() * 400;
            cloudGroup.setLayoutX(-250);
            cloudGroup.setLayoutY(startY);
            animationLayer.getChildren().add(cloudGroup);

            TranslateTransition move = new TranslateTransition(Duration.seconds(18 + Math.random() * 12), cloudGroup);
            move.setToX(1800);
            move.setCycleCount(Animation.INDEFINITE);
            move.setDelay(Duration.seconds(Math.random() * 10));
            move.play();
        }
    }

    private void createSunnyEffect() {
        animationLayer.getChildren().clear();
        
        // Blue sky gradient background
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        DoubleProperty bgBlend = new SimpleDoubleProperty(0);
        bgBlend.addListener((obs, o, n) -> {
            double blend = n.doubleValue();
            LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(135, 206, 250).interpolate(Color.rgb(100, 180, 255), blend * 0.3)),
                new Stop(0.5, Color.rgb(135, 206, 235).interpolate(Color.rgb(120, 190, 240), blend * 0.2)),
                new Stop(1, Color.rgb(176, 224, 230).interpolate(Color.rgb(150, 210, 255), blend * 0.15)));
            bg.setFill(grad);
        });
        animationLayer.getChildren().add(bg);
        
        Timeline bgAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bgBlend, 0)),
            new KeyFrame(Duration.seconds(10), new KeyValue(bgBlend, 1))
        );
        bgAnim.setAutoReverse(true);
        bgAnim.setCycleCount(Animation.INDEFINITE);
        bgAnim.play();
        
        // Main Sun (3D-like with multiple layers) - moved to top-right corner
        double sunCenterX = 1100;
        double sunCenterY = 150;
        
        // Outer glow (largest, most transparent)
        Circle sunGlow1 = new Circle(sunCenterX, sunCenterY, 100);
        RadialGradient glowGrad1 = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 240, 150, 0.25)),
            new Stop(0.6, Color.rgb(255, 220, 100, 0.15)),
            new Stop(1, Color.rgb(255, 200, 80, 0)));
        sunGlow1.setFill(glowGrad1);
        sunGlow1.setEffect(new GaussianBlur(40));
        animationLayer.getChildren().add(sunGlow1);
        
        // Middle glow
        Circle sunGlow2 = new Circle(sunCenterX, sunCenterY, 70);
        RadialGradient glowGrad2 = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 250, 180, 0.45)),
            new Stop(0.7, Color.rgb(255, 230, 120, 0.25)),
            new Stop(1, Color.rgb(255, 210, 100, 0)));
        sunGlow2.setFill(glowGrad2);
        sunGlow2.setEffect(new GaussianBlur(25));
        animationLayer.getChildren().add(sunGlow2);
        
        // Core sun body (solid, bright)
        Circle sunCore = new Circle(sunCenterX, sunCenterY, 48);
        RadialGradient coreGrad = new RadialGradient(0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 240, 0.98)),
            new Stop(0.5, Color.rgb(255, 245, 200, 0.95)),
            new Stop(0.85, Color.rgb(255, 220, 130, 0.92)),
            new Stop(1, Color.rgb(255, 200, 100, 0.88)));
        sunCore.setFill(coreGrad);
        sunCore.setEffect(new Bloom(0.5));
        animationLayer.getChildren().add(sunCore);
        
        // Pulsing animation for sun core
        ScaleTransition sunPulse = new ScaleTransition(Duration.seconds(3), sunCore);
        sunPulse.setFromX(1.0);
        sunPulse.setFromY(1.0);
        sunPulse.setToX(1.08);
        sunPulse.setToY(1.08);
        sunPulse.setCycleCount(Animation.INDEFINITE);
        sunPulse.setAutoReverse(true);
        sunPulse.play();
        
        // Rotating sun rays (sharp, long rays)
        Group rayGroup1 = new Group();
        for (int i = 0; i < 16; i++) {
            // Main ray
            Polygon ray = new Polygon();
            ray.getPoints().addAll(
                0.0, -130.0,    // tip
                -6.0, -45.0,    // left base
                0.0, -50.0,     // center base indent
                6.0, -45.0      // right base
            );
            LinearGradient rayGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 220, 0.5)),
                new Stop(0.4, Color.rgb(255, 245, 180, 0.65)),
                new Stop(0.8, Color.rgb(255, 230, 140, 0.45)),
                new Stop(1, Color.rgb(255, 220, 120, 0)));
            ray.setFill(rayGrad);
            ray.setRotate(i * 22.5);
            ray.setEffect(new GaussianBlur(3));
            rayGroup1.getChildren().add(ray);
        }
        rayGroup1.setLayoutX(sunCenterX);
        rayGroup1.setLayoutY(sunCenterY);
        animationLayer.getChildren().add(rayGroup1);
        
        RotateTransition rotateRays1 = new RotateTransition(Duration.seconds(45), rayGroup1);
        rotateRays1.setByAngle(360);
        rotateRays1.setCycleCount(Animation.INDEFINITE);
        rotateRays1.play();
        
        // Secondary shorter rays (rotating opposite direction)
        Group rayGroup2 = new Group();
        for (int i = 0; i < 16; i++) {
            Polygon ray = new Polygon();
            ray.getPoints().addAll(
                0.0, -100.0,
                -4.0, -48.0,
                0.0, -52.0,
                4.0, -48.0
            );
            LinearGradient rayGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 250, 200, 0.35)),
                new Stop(0.5, Color.rgb(255, 240, 160, 0.45)),
                new Stop(1, Color.rgb(255, 230, 130, 0)));
            ray.setFill(rayGrad);
            ray.setRotate(i * 22.5 + 11.25); // offset by half
            ray.setEffect(new GaussianBlur(4));
            rayGroup2.getChildren().add(ray);
        }
        rayGroup2.setLayoutX(sunCenterX);
        rayGroup2.setLayoutY(sunCenterY);
        animationLayer.getChildren().add(rayGroup2);
        
        RotateTransition rotateRays2 = new RotateTransition(Duration.seconds(60), rayGroup2);
        rotateRays2.setByAngle(-360);
        rotateRays2.setCycleCount(Animation.INDEFINITE);
        rotateRays2.play();
        
        // Soft wavy heat distortion rays
        for (int i = 0; i < 8; i++) {
            Rectangle heatRay = new Rectangle(8, 140);
            LinearGradient heatGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 220, 0)),
                new Stop(0.3, Color.rgb(255, 245, 180, 0.08)),
                new Stop(0.7, Color.rgb(255, 235, 150, 0.12)),
                new Stop(1, Color.rgb(255, 225, 130, 0)));
            heatRay.setFill(heatGrad);
            heatRay.setArcWidth(8);
            heatRay.setArcHeight(8);
            heatRay.setLayoutX(sunCenterX - 4);
            heatRay.setLayoutY(sunCenterY - 70);
            heatRay.setRotate(i * 45);
            heatRay.setEffect(new GaussianBlur(20));
            animationLayer.getChildren().add(heatRay);
            
            // Wave animation
            Timeline wave = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(heatRay.scaleXProperty(), 1.0)),
                new KeyFrame(Duration.seconds(2), new KeyValue(heatRay.scaleXProperty(), 1.3)),
                new KeyFrame(Duration.seconds(4), new KeyValue(heatRay.scaleXProperty(), 1.0))
            );
            wave.setDelay(Duration.millis(i * 500));
            wave.setCycleCount(Animation.INDEFINITE);
            wave.play();
        }
        
        // Floating light particles (more realistic)
        for (int i = 0; i < 80; i++) {
            Circle particle = new Circle(1.2 + Math.random() * 3.5);
            double opacity = 0.3 + Math.random() * 0.5;
            RadialGradient particleGrad = new RadialGradient(0, 0, 0.3, 0.3, 0.7, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 240, opacity)),
                new Stop(0.6, Color.rgb(255, 245, 200, opacity * 0.7)),
                new Stop(1, Color.rgb(255, 230, 160, opacity * 0.3)));
            particle.setFill(particleGrad);
            particle.setEffect(new Bloom(0.6));
            
            double x = Math.random() * 1500;
            double y = Math.random() * 1000;
            particle.setLayoutX(x);
            particle.setLayoutY(y);
            animationLayer.getChildren().add(particle);

            // Gentle floating motion
            TranslateTransition float1 = new TranslateTransition(Duration.millis(3000 + Math.random() * 3000), particle);
            float1.setByY(-30 - Math.random() * 40);
            float1.setByX(-20 + Math.random() * 40);
            float1.setCycleCount(Animation.INDEFINITE);
            float1.setAutoReverse(true);
            float1.setDelay(Duration.millis(Math.random() * 3000));
            float1.play();
            
            // Twinkling effect
            FadeTransition fade = new FadeTransition(Duration.millis(1200 + Math.random() * 2500), particle);
            fade.setFromValue(0.2);
            fade.setToValue(1.0);
            fade.setCycleCount(Animation.INDEFINITE);
            fade.setAutoReverse(true);
            fade.setDelay(Duration.millis(Math.random() * 2000));
            fade.play();
            
            // Subtle scale pulsing
            ScaleTransition scale = new ScaleTransition(Duration.millis(2000 + Math.random() * 2000), particle);
            scale.setFromX(0.8);
            scale.setFromY(0.8);
            scale.setToX(1.3);
            scale.setToY(1.3);
            scale.setCycleCount(Animation.INDEFINITE);
            scale.setAutoReverse(true);
            scale.setDelay(Duration.millis(Math.random() * 2000));
            scale.play();
        }
        
        // Lens flare effect
        Circle flare1 = new Circle(sunCenterX + 150, sunCenterY + 80, 25);
        RadialGradient flareGrad1 = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 255, 0.4)),
            new Stop(0.5, Color.rgb(255, 240, 180, 0.25)),
            new Stop(1, Color.rgb(255, 230, 150, 0)));
        flare1.setFill(flareGrad1);
        flare1.setEffect(new GaussianBlur(15));
        animationLayer.getChildren().add(flare1);
        
        Circle flare2 = new Circle(sunCenterX + 280, sunCenterY + 140, 18);
        RadialGradient flareGrad2 = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 200, 150, 0.35)),
            new Stop(0.6, Color.rgb(255, 180, 120, 0.2)),
            new Stop(1, Color.rgb(255, 160, 100, 0)));
        flare2.setFill(flareGrad2);
        flare2.setEffect(new GaussianBlur(12));
        animationLayer.getChildren().add(flare2);
        
        // Flare fade animation
        FadeTransition flareFade1 = new FadeTransition(Duration.seconds(3), flare1);
        flareFade1.setFromValue(0.6);
        flareFade1.setToValue(1.0);
        flareFade1.setCycleCount(Animation.INDEFINITE);
        flareFade1.setAutoReverse(true);
        flareFade1.play();
        
        FadeTransition flareFade2 = new FadeTransition(Duration.seconds(4), flare2);
        flareFade2.setFromValue(0.5);
        flareFade2.setToValue(0.9);
        flareFade2.setCycleCount(Animation.INDEFINITE);
        flareFade2.setAutoReverse(true);
        flareFade2.setDelay(Duration.seconds(1));
        flareFade2.play();
        
        currentAnimation = bgAnim; // Track main animation for cleanup
    }

    private void createStarsEffect() {
        animationLayer.getChildren().clear();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, baseC1.deriveColor(0, 1.0, 0.5, 0.95)),
            new Stop(0.5, baseC2.deriveColor(0, 1.0, 0.6, 0.97)),
            new Stop(1, baseC3.deriveColor(0, 1.0, 0.4, 0.98)));
        bg.setFill(grad);
        animationLayer.getChildren().add(bg);
        
        // Moon
        Circle moon = new Circle(50);
        RadialGradient moonGradient = new RadialGradient(0, 0, 0.4, 0.4, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 240, 0.95)),
            new Stop(0.8, Color.rgb(240, 240, 220, 0.85)),
            new Stop(1, Color.rgb(220, 220, 200, 0.7)));
        moon.setFill(moonGradient);
        moon.setEffect(new Bloom(0.3));
        moon.setLayoutX(600);
        moon.setLayoutY(120);
        animationLayer.getChildren().add(moon);
        
        // Stars
        for (int i = 0; i < 100; i++) {
            Circle star = new Circle(0.5 + Math.random() * 2.5);
            double brightness = Math.random();
            Color starColor;
            if (brightness > 0.8) {
                starColor = Color.rgb(255, 255, 255, 0.95);
            } else if (brightness > 0.5) {
                starColor = Color.rgb(240, 245, 255, 0.8);
            } else {
                starColor = Color.rgb(220, 230, 255, 0.6);
            }
            star.setFill(starColor);
            if (brightness > 0.7) {
                star.setEffect(new Bloom(0.5));
            }
            
            double x = Math.random() * 1500;
            double y = Math.random() * 1000;
            star.setLayoutX(x);
            star.setLayoutY(y);
            animationLayer.getChildren().add(star);

            FadeTransition twinkle = new FadeTransition(Duration.millis(1000 + Math.random() * 2500), star);
            twinkle.setFromValue(0.3);
            twinkle.setToValue(1.0);
            twinkle.setCycleCount(Animation.INDEFINITE);
            twinkle.setAutoReverse(true);
            twinkle.setDelay(Duration.millis(Math.random() * 3000));
            twinkle.play();
        }
        
        // Shooting stars
        for (int i = 0; i < 3; i++) {
            Line shootingStar = new Line();
            LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255, 0)),
                new Stop(0.3, Color.rgb(240, 245, 255, 0.8)),
                new Stop(1, Color.rgb(200, 220, 255, 0.4)));
            shootingStar.setStroke(gradient);
            shootingStar.setStrokeWidth(2);
            shootingStar.setEffect(new Bloom(0.6));
            
            double startX = 100 + Math.random() * 600;
            double startY = 50 + Math.random() * 300;
            shootingStar.setStartX(startX);
            shootingStar.setStartY(startY);
            shootingStar.setEndX(startX + 80);
            shootingStar.setEndY(startY + 80);
            shootingStar.setOpacity(0);
            animationLayer.getChildren().add(shootingStar);
            
            Timeline shoot = new Timeline(
                new KeyFrame(Duration.ZERO, 
                    new KeyValue(shootingStar.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(100), 
                    new KeyValue(shootingStar.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(800), 
                    new KeyValue(shootingStar.opacityProperty(), 0),
                    new KeyValue(shootingStar.translateXProperty(), 150),
                    new KeyValue(shootingStar.translateYProperty(), 150))
            );
            shoot.setCycleCount(Animation.INDEFINITE);
            shoot.setDelay(Duration.seconds(5 + i * 4 + Math.random() * 5));
            shoot.play();
        }
    }

    private void createFogEffect() {
        animationLayer.getChildren().clear();
        
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(animationLayer.widthProperty());
        bg.heightProperty().bind(animationLayer.heightProperty());
        
        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, baseC1.deriveColor(0, 1.0, 1.05, 0.83)),
            new Stop(0.5, baseC2.deriveColor(0, 1.0, 1.0, 0.87)),
            new Stop(1, baseC3.deriveColor(0, 1.0, 0.95, 0.85)));
        bg.setFill(grad);
        animationLayer.getChildren().add(bg);
        
        // Fog layers
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 15; i++) {
                Rectangle fog = new Rectangle(200 + Math.random() * 180, 100 + Math.random() * 100);
                double opacity = (0.15 + layer * 0.05) * (0.8 + Math.random() * 0.4);
                RadialGradient gradient = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(220, 225, 235, opacity)),
                    new Stop(0.6, Color.rgb(200, 205, 220, opacity * 0.7)),
                    new Stop(1, Color.rgb(180, 185, 200, opacity * 0.3)));
                fog.setFill(gradient);
                fog.setArcWidth(50);
                fog.setArcHeight(50);
                fog.setEffect(new GaussianBlur(35 + layer * 10));
                
                double startY = (layer * 250) + Math.random() * 200;
                fog.setLayoutX(-300);
                fog.setLayoutY(startY);
                animationLayer.getChildren().add(fog);

                double speed = 20 + layer * 8 + Math.random() * 12;
                TranslateTransition drift = new TranslateTransition(Duration.seconds(speed), fog);
                drift.setToX(1800);
                drift.setCycleCount(Animation.INDEFINITE);
                drift.setDelay(Duration.seconds(Math.random() * 15));
                drift.play();
            }
        }
    }
    
    private void stopCurrentAnimation() {
        if (animationLayer != null) {
            animationLayer.getChildren().clear();
        }
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }
    }
    
    // ===== ICON SVG CREATORS =====
    private StackPane createColorfulIcon(String type) {
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(32, 32);
        
        SVGPath iconPath = new SVGPath();
        
        switch (type) {
            case "location":
                iconPath.setContent("M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z");
                RadialGradient locationGradient = new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 100, 100)),
                    new Stop(1, Color.rgb(220, 50, 80)));
                iconPath.setFill(locationGradient);
                break;
                
            case "calendar":
                iconPath.setContent("M19 4h-1V2h-2v2H8V2H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11zM7 11h5v5H7z");
                LinearGradient calendarGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(100, 180, 255)),
                    new Stop(1, Color.rgb(50, 120, 220)));
                iconPath.setFill(calendarGradient);
                break;
            
            case "droplet":
                iconPath.setContent("M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z");
                LinearGradient dropletGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(100, 200, 255)),
                    new Stop(1, Color.rgb(30, 144, 255)));
                iconPath.setFill(dropletGradient);
                break;
                
            case "wind":
                iconPath.setContent("M9.59 4.59A2 2 0 1 1 11 8H2m10.59 11.41A2 2 0 1 0 14 16H2m15.73-8.27A2.5 2.5 0 1 1 19.5 12H2");
                LinearGradient windGradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(135, 206, 235)),
                    new Stop(1, Color.rgb(70, 130, 180)));
                iconPath.setStroke(windGradient);
                iconPath.setStrokeWidth(2);
                iconPath.setFill(Color.TRANSPARENT);
                break;
                
            case "gauge":
                iconPath.setContent("M12 2a10 10 0 1 0 10 10H12V2z");
                RadialGradient gaugeGradient = new RadialGradient(0, 0, 0.5, 0.5, 0.8, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 215, 0)),
                    new Stop(1, Color.rgb(255, 140, 0)));
                iconPath.setFill(gaugeGradient);
                break;
                
            case "eye":
                iconPath.setContent("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z");
                LinearGradient eyeGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(138, 43, 226)),
                    new Stop(1, Color.rgb(75, 0, 130)));
                iconPath.setFill(eyeGradient);
                
                Circle pupil = new Circle(6);
                pupil.setFill(Color.rgb(50, 50, 80));
                iconPane.getChildren().add(pupil);
                break;
                
            case "sunrise":
                iconPath.setContent("M17 18a5 5 0 0 0-10 0M12 2v7M4.22 10.22l1.42 1.42M1 18h2M21 18h2M18.36 11.64l1.42-1.42");
                LinearGradient sunriseGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 215, 0)),
                    new Stop(1, Color.rgb(255, 140, 0)));
                iconPath.setStroke(sunriseGradient);
                iconPath.setStrokeWidth(2);
                iconPath.setFill(Color.TRANSPARENT);
                break;
                
            case "sunset":
                iconPath.setContent("M17 18a5 5 0 0 0-10 0M12 9V2M4.22 10.22l1.42 1.42M1 18h2M21 18h2M18.36 11.64l1.42-1.42");
                LinearGradient sunsetGradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 99, 71)),
                    new Stop(1, Color.rgb(220, 20, 60)));
                iconPath.setStroke(sunsetGradient);
                iconPath.setStrokeWidth(2);
                iconPath.setFill(Color.TRANSPARENT);
                break;
                
            case "thermometer":
                iconPath.setContent("M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z");
                LinearGradient thermGradient = new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 69, 0)),
                    new Stop(0.5, Color.rgb(255, 165, 0)),
                    new Stop(1, Color.rgb(135, 206, 250)));
                iconPath.setFill(thermGradient);
                break;
        }
        
        iconPath.setScaleX(1.3);
        iconPath.setScaleY(1.3);
        iconPath.setEffect(new DropShadow(5, 2, 2, Color.rgb(0, 0, 0, 0.3)));
        
        iconPane.getChildren().add(iconPath);
        return iconPane;
    }

    /**
     * Create a StackPane that contains a fallback colorful icon underneath and an ImageView
     * above it which will attempt to load the remote iconUrl. If loading fails the ImageView
     * is hidden so the fallback remains visible.
     */
    private StackPane makeIconWithFallback(String iconUrl, String fallbackType, double w, double h) {
        StackPane wrap = new StackPane();
        wrap.setPrefSize(w, h);
        Node fallback = createColorfulIcon(fallbackType);
        fallback.setManaged(true);
        fallback.setMouseTransparent(true);
        // ensure fallback sizes reasonably
        fallback.setStyle("-fx-pref-width: " + w + "; -fx-pref-height: " + h + ";");

        ImageView iv = new ImageView();
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        if (iconUrl != null && !iconUrl.trim().isEmpty()) {
            try {
                Image img = new Image(iconUrl, true);
                iv.setImage(img);
                // hide image view if loading fails
                img.errorProperty().addListener((obs, oldV, newV) -> {
                    if (newV) iv.setVisible(false);
                });
                // when image has width (loaded) ensure visible
                img.widthProperty().addListener((obs, oldW, newW) -> {
                    if (newW != null && newW.doubleValue() > 0 && !img.isError()) iv.setVisible(true);
                });
            } catch (Exception ex) {
                iv.setVisible(false);
            }
        } else {
            iv.setVisible(false);
        }

        wrap.getChildren().addAll(fallback, iv);
        return wrap;
    }
    
    // ===== UI COMPONENTS =====
    
    public VBox createHeader() {
        VBox header = new VBox(8); 
        header.setAlignment(Pos.CENTER); 
        header.setPadding(new Insets(0, 0, 8, 0));
        
     // Tạo icon thời tiết (mặt trời và mây)
        Group weatherIcon = new Group();

        // Vẽ mặt trời
        Circle sun = new Circle(12, Color.rgb(255, 200, 50));
        sun.setTranslateX(-8);
        sun.setTranslateY(-5);

        // Tia sáng mặt trời
        for (int i = 0; i < 8; i++) {
            Line ray = new Line(-8, -20, -8, -26);
            ray.setStroke(Color.rgb(255, 200, 50));
            ray.setStrokeWidth(2.5);
            ray.setStrokeLineCap(StrokeLineCap.ROUND);
            ray.getTransforms().add(new Rotate(i * 45, -8, -5));
            weatherIcon.getChildren().add(ray);
        }
        weatherIcon.getChildren().add(sun);

        // Vẽ mây
        Ellipse cloud1 = new Ellipse(8, 6);
        cloud1.setFill(Color.WHITE);
        cloud1.setTranslateX(5);
        cloud1.setTranslateY(5);

        Ellipse cloud2 = new Ellipse(7, 5);
        cloud2.setFill(Color.WHITE);
        cloud2.setTranslateX(-2);
        cloud2.setTranslateY(7);

        Ellipse cloud3 = new Ellipse(6, 5);
        cloud3.setFill(Color.WHITE);
        cloud3.setTranslateX(12);
        cloud3.setTranslateY(7);

        weatherIcon.getChildren().addAll(cloud1, cloud2, cloud3);

        // Đổ bóng cho icon
        DropShadow iconShadow = new DropShadow(8, 2, 2, Color.rgb(0, 0, 0, 0.3));
        weatherIcon.setEffect(iconShadow);

        // Tạo title với icon
        Label title = new Label(" WEATHER FINDER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);
        title.setGraphic(weatherIcon);
        title.setGraphicTextGap(10);
        title.setEffect(new DropShadow(15, 3, 3, Color.rgb(0, 0, 0, 0.4)));
        Label subtitle = new Label("Khám phá thời tiết toàn cầu trong tích tắc"); 
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14)); 
        subtitle.setTextFill(Color.rgb(255, 255, 255, 0.95));
        subtitle.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        
        header.getChildren().addAll(title, subtitle); 
        return header;
    }
    
    public HBox createSearchBox(TextField txtCity, Button btnSearch) {
        HBox searchBox = new HBox(12); 
        searchBox.setAlignment(Pos.CENTER); 
        searchBox.setPadding(new Insets(12, 18, 12, 18));
        searchBox.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.12);" +
            "-fx-background-radius: 28;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 28;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
        );
        
        txtCity.setPromptText("Nhập thành phố... (VD: Hanoi, Tokyo, London)"); 
        txtCity.setPrefWidth(360); 
        txtCity.setPrefHeight(44);
        txtCity.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.25);" +
            "-fx-background-radius: 22;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255, 255, 255, 0.65);" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 0 18 0 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.35);" +
            "-fx-border-radius: 22;" +
            "-fx-border-width: 1.5;"
        );
        
      
        btnSearch.setPrefHeight(44); 
        btnSearch.setPrefWidth(100);
        
        Stop[] stops = new Stop[]{
            new Stop(0, Color.rgb(255, 200, 55)), 
            new Stop(1, Color.rgb(255, 150, 50))
        };
        
        btnSearch.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 22;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: rgba(255, 255, 255, 0.4);" +
            "-fx-border-radius: 22;" +
            "-fx-border-width: 1.5;"
        );
        
        btnSearch.setBackground(new Background(
            new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops), 
                new CornerRadii(22), 
                null
            )
        ));
        btnSearch.setEffect(new DropShadow(10, 3, 3, Color.rgb(0, 0, 0, 0.3)));
        
        setupButtonHoverEffects(btnSearch); 
        // Simple search action (map/geocoding removed). Trigger weather lookup elsewhere.
        btnSearch.setOnAction(evt -> {
            String city = txtCity.getText() == null ? "" : txtCity.getText().trim();
            if (city.isEmpty()) {
                descriptionLabel.setText("Vui lòng nhập tên thành phố.");
                return;
            }
            descriptionLabel.setText("⏳ Đang tải dữ liệu cho " + city + "...");
            // TODO: integrate with existing weather lookup / TCP client. Map/geocoding removed.
        });
        searchBox.getChildren().addAll(txtCity, btnSearch);
        return searchBox;
    }
    
    private void setupButtonHoverEffects(Button btn) {
        btn.setOnMouseEntered(e -> { 
            Stop[] s = new Stop[]{
                new Stop(0, Color.rgb(255, 180, 45)), 
                new Stop(1, Color.rgb(255, 130, 40))
            }; 
            btn.setBackground(new Background(
                new BackgroundFill(
                    new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, s), 
                    new CornerRadii(22), 
                    null
                )
            )); 
            btn.setScaleX(1.05); 
            btn.setScaleY(1.05); 
            btn.setEffect(new DropShadow(15, 5, 5, Color.rgb(255, 180, 0, 0.5))); 
        });
        
        btn.setOnMouseExited(e -> { 
            Stop[] s = new Stop[]{
                new Stop(0, Color.rgb(255, 200, 55)), 
                new Stop(1, Color.rgb(255, 150, 50))
            }; 
            btn.setBackground(new Background(
                new BackgroundFill(
                    new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, s), 
                    new CornerRadii(22), 
                    null
                )
            )); 
            btn.setScaleX(1.0); 
            btn.setScaleY(1.0); 
            btn.setEffect(new DropShadow(10, 3, 3, Color.rgb(0, 0, 0, 0.3))); 
        });
    }

    public VBox createWeatherCard() {
        VBox card = new VBox(18);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(25, 25, 25, 25));
        card.setMinHeight(520);
        card.setMaxHeight(600);
        card.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.18);" +
            "-fx-background-radius: 28;" +
            "-fx-border-color: rgba(255, 255, 255, 0.35);" +
            "-fx-border-radius: 28;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 18, 0, 0, 6);"
        );

        feelsLikeLabel = new Label("--"); 
        humidityLabel = new Label("--"); 
        windLabel = new Label("--");
        sunriseLabel = new Label("--"); 
        sunsetLabel = new Label("--"); 
        pressureLabel = new Label("--");
        visibilityLabel = new Label("--"); 
        minTempLabel = new Label("--"); 
        maxTempLabel = new Label("--");

        weatherContentBox = new VBox(22);
        weatherContentBox.setAlignment(Pos.TOP_CENTER);
        
    weatherContentBox.getChildren().add(createHeroSection());
    // Map and AI sections removed
    weatherContentBox.getChildren().add(createMainDetailsGrid());
    weatherContentBox.getChildren().add(createHourlyForecastSection());
    weatherContentBox.getChildren().add(createDailyForecastSection());
        weatherContentBox.getChildren().add(createSecondaryDetailsGrid());

        spinner = new ProgressIndicator();
        spinner.setPrefSize(60, 60);
        spinner.setStyle(
            "-fx-progress-color: white;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.5), 10, 0, 0, 0);"
        );
        spinner.setVisible(false);

        ScrollPane scrollPane = new ScrollPane(weatherContentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

    StackPane stackPane = new StackPane();
    stackPane.getChildren().addAll(scrollPane, spinner);
    
    card.getChildren().add(stackPane);
        return card;
    }
    
    public VBox createHeroSection() {
        VBox heroBox = new VBox(6);
        heroBox.setAlignment(Pos.CENTER);
        heroBox.setPadding(new Insets(10, 0, 10, 0));
        
    cityLabel = new Label("Vị trí: --, --");
    cityLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));
    cityLabel.setTextFill(Color.rgb(255, 255, 255, 0.92));
        cityLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0, 0, 2);");

        // Favorite toggle (heart) - use SVG graphic for consistent icon rendering
        favoriteButton = new ToggleButton();
        favoriteButton.setTooltip(new Tooltip("Thêm vào yêu thích"));
        favoriteButton.setPrefWidth(44);
        favoriteButton.setPrefHeight(44);
        favoriteButton.setFocusTraversable(false);

        // Create SVG heart graphic
        SVGPath heart = new SVGPath();
        heart.setContent("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
        heart.setScaleX(0.9);
        heart.setScaleY(0.9);
        heart.setStroke(Color.WHITE);
        heart.setStrokeWidth(0.5);
        heart.setFill(Color.color(1,1,1,0.0));
        heart.setEffect(new DropShadow(6, 1, 1, Color.rgb(0,0,0,0.35)));
        favoriteButton.setGraphic(heart);

        // Base style (transparent background so gradient from parent shows through lightly)
        favoriteButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.08);" +
            "-fx-background-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-radius: 22;" +
            "-fx-border-width: 1.2;" +
            "-fx-cursor: hand;"
        );

        // Update graphic fill and subtle scale on selection
        favoriteButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                heart.setFill(Color.web("#FF4D6D"));
                heart.setStroke(Color.web("#FF2D55"));
                favoriteButton.setScaleX(1.08);
                favoriteButton.setScaleY(1.08);
            } else {
                heart.setFill(Color.color(1,1,1,0.0));
                heart.setStroke(Color.WHITE);
                favoriteButton.setScaleX(1.0);
                favoriteButton.setScaleY(1.0);
            }
        });

        favoriteButton.setOnMouseEntered(e -> {
            favoriteButton.setScaleX(1.05);
            favoriteButton.setScaleY(1.05);
        });
        favoriteButton.setOnMouseExited(e -> {
            if (!favoriteButton.isSelected()) {
                favoriteButton.setScaleX(1.0);
                favoriteButton.setScaleY(1.0);
            } else {
                favoriteButton.setScaleX(1.08);
                favoriteButton.setScaleY(1.08);
            }
        });

        // Insert compact temp toggle into the cityRow to keep header compact
        // (cityRow was created earlier and contains cityLabel, spacer, favoriteButton)
        // add tempUnitToggle right before the favorite button
        // We'll insert after tempUnitToggle is initialized below.

    HBox cityRow = new HBox(8);
    cityRow.setAlignment(Pos.CENTER_LEFT);
    // spacer to push controls to the right of the city label
    Region citySpacer = new Region();
    HBox.setHgrow(citySpacer, Priority.ALWAYS);
    cityRow.getChildren().addAll(cityLabel, citySpacer, favoriteButton);

    temperatureLabel = new Label("--°C");
        temperatureLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 72));
        temperatureLabel.setTextFill(Color.WHITE);
        temperatureLabel.setEffect(new DropShadow(12, 0, 4, Color.rgb(0, 0, 0, 0.5)));

        // Temperature unit toggle - styled beautifully
        // Compact temp unit toggle (moved to city row)
        tempUnitToggle = new ToggleButton("°C");
        tempUnitToggle.setSelected(true);
        tempUnitToggle.setTooltip(new Tooltip("Chuyển đơn vị: °C / °F"));
        tempUnitToggle.setPrefWidth(40);
        tempUnitToggle.setPrefHeight(36);
        tempUnitToggle.setStyle(
            "-fx-background-color: rgba(255,255,255,0.12);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255,255,255,0.18);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;"
        );
        tempUnitToggle.setOnAction(e -> {
            useCelsius = tempUnitToggle.isSelected();
            tempUnitToggle.setText(useCelsius ? "°C" : "°F");
            // Update style based on selection
            if (useCelsius) {
                tempUnitToggle.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, rgba(100,200,255,0.5), rgba(50,150,255,0.35));" +
                    "-fx-background-radius: 25;" +
                    "-fx-border-color: rgba(150,220,255,0.8);" +
                    "-fx-border-radius: 25;" +
                    "-fx-border-width: 2;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(100,200,255,0.5), 10, 0, 0, 2);"
                );
            } else {
                tempUnitToggle.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, rgba(255,150,100,0.5), rgba(255,100,50,0.35));" +
                    "-fx-background-radius: 25;" +
                    "-fx-border-color: rgba(255,180,120,0.8);" +
                    "-fx-border-radius: 25;" +
                    "-fx-border-width: 2;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(255,150,100,0.5), 10, 0, 0, 2);"
                );
            }
            updateTemperatureLabels();
            refreshForecastTemperatures();
        });
        
        // Hover effects for temp toggle
        tempUnitToggle.setOnMouseEntered(e -> {
            tempUnitToggle.setScaleX(1.1);
            tempUnitToggle.setScaleY(1.1);
        });
        tempUnitToggle.setOnMouseExited(e -> {
            tempUnitToggle.setScaleX(1.0);
            tempUnitToggle.setScaleY(1.0);
        });

        // Insert the compact temp toggle into the cityRow before the favorite button
        try {
            int favIndex = cityRow.getChildren().indexOf(favoriteButton);
            if (favIndex >= 0) {
                cityRow.getChildren().add(favIndex, tempUnitToggle);
            } else {
                cityRow.getChildren().add(tempUnitToggle);
            }
        } catch (Exception ex) {
            // if something unexpected happens, ignore - hero layout will still render
        }

    HBox tempRow = new HBox(12);
    tempRow.setAlignment(Pos.CENTER);
    // temperatureLabel remains centered; unit toggle moved to cityRow
    tempRow.getChildren().addAll(temperatureLabel);
        
        descriptionLabel = new Label("Chọn thành phố để xem thời tiết");
        descriptionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 17));
        descriptionLabel.setTextFill(Color.rgb(255, 255, 255, 0.95));
        descriptionLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 5, 0, 0, 2);");
        
        feelsLikeLabel = new Label("Feels like --°C");
        feelsLikeLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        feelsLikeLabel.setTextFill(Color.rgb(255, 255, 255, 0.85));
        
        weatherIcon = new ImageView();
        weatherIcon.setFitWidth(90);
        weatherIcon.setFitHeight(90);
        weatherIcon.setPreserveRatio(true);
        weatherIcon.setEffect(new DropShadow(10, 3, 3, Color.rgb(0, 0, 0, 0.35)));
        
    heroBox.getChildren().addAll(cityRow, tempRow, descriptionLabel, feelsLikeLabel, weatherIcon);
        return heroBox;
    }
    
    public GridPane createMainDetailsGrid() {
        // Initialize labels if not already done
        if (humidityLabel == null) humidityLabel = new Label("--");
        if (windLabel == null) windLabel = new Label("--");
        if (pressureLabel == null) pressureLabel = new Label("--");
        if (visibilityLabel == null) visibilityLabel = new Label("--");
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(0);
        grid.setPadding(new Insets(8, 0, 8, 0));
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(25);
        col1.setHalignment(HPos.CENTER);
        
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        col2.setHalignment(HPos.CENTER);
        
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        col3.setHalignment(HPos.CENTER);
        
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(25);
        col4.setHalignment(HPos.CENTER);
        
        grid.getColumnConstraints().addAll(col1, col2, col3, col4);
        
        grid.add(createDetailCard("droplet", "Humidity", humidityLabel), 0, 0);
        grid.add(createDetailCard("wind", "Wind Speed", windLabel), 1, 0);
        grid.add(createDetailCard("gauge", "Pressure", pressureLabel), 2, 0);
        grid.add(createDetailCard("eye", "Visibility", visibilityLabel), 3, 0);
        
        return grid;
    }
    
    public GridPane createSecondaryDetailsGrid() {
        // Initialize labels if not already done
        if (sunriseLabel == null) sunriseLabel = new Label("--");
        if (sunsetLabel == null) sunsetLabel = new Label("--");
        if (minTempLabel == null) minTempLabel = new Label("--");
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 0, 5, 0));
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33.33);
        col1.setHalignment(HPos.CENTER);
        
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33.33);
        col2.setHalignment(HPos.CENTER);
        
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33.33);
        col3.setHalignment(HPos.CENTER);
        
        grid.getColumnConstraints().addAll(col1, col2, col3);
        
    grid.add(createDetailCard("sunrise", "Sunrise", sunriseLabel), 0, 0);
    grid.add(createDetailCard("sunset", "Sunset", sunsetLabel), 1, 0);
    // Create a combined label for Min/Max (binds to individual labels so it updates)
    if (minTempLabel == null) minTempLabel = new Label("--");
    if (maxTempLabel == null) maxTempLabel = new Label("--");
    Label combinedMinMax = new Label();
    combinedMinMax.textProperty().bind(javafx.beans.binding.Bindings.concat(minTempLabel.textProperty(), " / ", maxTempLabel.textProperty()));
    grid.add(createDetailCard("thermometer", "Min/Max", combinedMinMax), 2, 0);
        
        return grid;
    }
    
    private VBox createDetailCard(String iconType, String title, Label dataLabel) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPrefHeight(95);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(12, 8, 12, 8));
        card.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
        );
        
        StackPane iconPane = createColorfulIcon(iconType);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.rgb(255, 255, 255, 0.88));
        
        if (dataLabel == null) {
            dataLabel = new Label("--");
        }
        dataLabel.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        dataLabel.setTextFill(Color.WHITE);
        dataLabel.setEffect(new DropShadow(3, 1, 1, Color.rgb(0, 0, 0, 0.25)));
        
        card.getChildren().addAll(iconPane, titleLabel, dataLabel);
        
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.22);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255, 255, 255, 0.35);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);"
            );
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255, 255, 255, 0.25);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
            );
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });
        
        return card;
    }
    
    public VBox createHourlyForecastSection() {
        VBox forecastBox = new VBox(10);
        forecastBox.setAlignment(Pos.CENTER_LEFT);
    // Increase outer box padding so the rounded border and shadows have more room
    forecastBox.setPadding(new Insets(22, 22, 22, 22));
    // Make the outer box taller so hourly items + hover effects are fully visible
    forecastBox.setPrefHeight(220);
    forecastBox.setMinHeight(190);
    forecastBox.setMaxWidth(Double.MAX_VALUE);
        forecastBox.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.12);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;"
        );
        
        Label title = new Label("DỰ BÁO HÀNG GIỜ");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        title.setTextFill(Color.WHITE);
        title.setPadding(new Insets(0, 0, 8, 3));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    // Allow content to size itself vertically so hover/scale won't be clipped
    scrollPane.setFitToHeight(false);
    // Increase inner scroll area height to give items more vertical room
    scrollPane.setPrefHeight(180);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-insets: 0;" +
            "-fx-padding: 0;"
        );
        
        if (hourlyDetailsBox == null) {
            // Slightly larger spacing to avoid items touching each other
            hourlyDetailsBox = new HBox(12);
        }
        hourlyDetailsBox.setAlignment(Pos.CENTER_LEFT);
        // Add more top/bottom padding so rounded corners and shadows aren't clipped
        hourlyDetailsBox.setPadding(new Insets(12, 8, 12, 8));
        
        scrollPane.setContent(hourlyDetailsBox);
        forecastBox.getChildren().addAll(title, scrollPane);
        return forecastBox;
    }
    
    public VBox createDailyForecastSection() {
        VBox forecastBox = new VBox(10);
        forecastBox.setAlignment(Pos.CENTER_LEFT);
        forecastBox.setPadding(new Insets(15, 15, 15, 15));
        forecastBox.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.12);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;"
        );
        
        Label title = new Label("DỰ BÁO 5 NGÀY");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);
        title.setPadding(new Insets(0, 0, 8, 3));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        
        ScrollPane scrollPane = new ScrollPane();
        // For vertical list: enable vertical scrolling and fit content width
        scrollPane.getStyleClass().add("custom-scroll");
        scrollPane.setPrefHeight(400);

        // Load CSS (chỉ cần load 1 lần cho toàn Scene hoặc container chính)
        java.net.URL cssUrl = getClass().getResource("styles/scrollbar.css");
        if (cssUrl != null) {
            scrollPane.getStylesheets().add(cssUrl.toExternalForm());
        }

        
        if (dailyForecastBox == null) {
            dailyForecastBox = new VBox(10); // spacing between items
        }
        // Center the column in the card and make children centered
        dailyForecastBox.setAlignment(Pos.TOP_CENTER);
        dailyForecastBox.setPadding(new Insets(5, 0, 5, 0));
        dailyForecastBox.setFillWidth(true);
        
        scrollPane.setContent(dailyForecastBox);
        forecastBox.getChildren().addAll(title, scrollPane);
        return forecastBox;
    }
    // Map and AI features fully removed.
    public GridPane createWeatherDetailsGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 15, 20, 15));
        grid.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.12);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;"
        );
        
        // 2 rows x 3 columns
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33.33);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33.33);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33.33);
        grid.getColumnConstraints().addAll(col1, col2, col3);
        
        // Row 1: sunrise, sunset, min/max temp
        grid.add(createDetailCard("sunrise", "Sunrise", sunriseLabel), 0, 0);
        grid.add(createDetailCard("sunset", "Sunset", sunsetLabel), 1, 0);

        // Build a dedicated Min/Max card that displays both min and max values
        VBox minMaxCard = new VBox(6);
        minMaxCard.setAlignment(Pos.CENTER);
        minMaxCard.setPrefHeight(95);
        minMaxCard.setMaxWidth(Double.MAX_VALUE);
        minMaxCard.setPadding(new Insets(12, 8, 12, 8));
        minMaxCard.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
        );

        StackPane iconPane = createColorfulIcon("thermometer");
        Label titleLabel = new Label("Min/Max");
        titleLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        titleLabel.setTextFill(Color.rgb(255, 255, 255, 0.88));

        // Ensure labels exist
        if (minTempLabel == null) minTempLabel = new Label("--");
        if (maxTempLabel == null) maxTempLabel = new Label("--");

        HBox values = new HBox(8);
        values.setAlignment(Pos.CENTER);

    // Create labels and bind them to the underlying min/max label text so they update automatically
    Label minLbl = new Label();
    minLbl.textProperty().bind(javafx.beans.binding.Bindings.concat("↓ ", minTempLabel.textProperty()));
    minLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
    minLbl.setTextFill(Color.web("#A8E6FF"));

    Label maxLbl = new Label();
    maxLbl.textProperty().bind(javafx.beans.binding.Bindings.concat("↑ ", maxTempLabel.textProperty()));
    maxLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
    maxLbl.setTextFill(Color.web("#FFD4A3"));

        values.getChildren().addAll(minLbl, maxLbl);
        minMaxCard.getChildren().addAll(iconPane, titleLabel, values);

        grid.add(minMaxCard, 2, 0);
        
        return grid;
    }


    public VBox createHourlyItem(String time, String iconUrl, double tempC) {
        VBox itemBox = new VBox(8);
        itemBox.setAlignment(Pos.CENTER);
        itemBox.setPrefWidth(75);
        itemBox.setPrefHeight(115);
        itemBox.setPadding(new Insets(10, 6, 10, 6));
        itemBox.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
        );
        
        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        timeLabel.setTextFill(Color.WHITE);
        
        ImageView iconView = new ImageView();
        try {
            iconView.setImage(new Image(iconUrl, true));
        } catch (Exception e) {
            System.err.println("Error loading hourly icon: " + e.getMessage());
        }
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        iconView.setPreserveRatio(true);
        
    Label tempLabel = new Label(formatTempForUnit(tempC));
        tempLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
    tempLabel.setTextFill(temperatureColorForC(tempC));
        tempLabel.setEffect(new DropShadow(3, 1, 1, Color.rgb(0, 0, 0, 0.25)));
    // store raw Celsius value so we can refresh format when unit toggles
    tempLabel.setUserData(Double.valueOf(tempC));
        
        itemBox.getChildren().addAll(timeLabel, iconView, tempLabel);
        
        itemBox.setOnMouseEntered(e -> {
            itemBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.25);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255, 255, 255, 0.35);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);"
            );
            itemBox.setScaleX(1.05);
            itemBox.setScaleY(1.05);
        });
        
        itemBox.setOnMouseExited(e -> {
            itemBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255, 255, 255, 0.25);" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);"
            );
            itemBox.setScaleX(1.0);
            itemBox.setScaleY(1.0);
        });
        
        return itemBox;
    }

    public VBox createDailyItem(String day, String iconUrl, double maxC, double minC) {
        return createDailyItem(day, iconUrl, maxC, minC, "");
    }
    
    public VBox createDailyItem(String day, String iconUrl, double maxC, double minC, String description) {
        // Create HBox for horizontal layout: [Icon | Day/Description | High/Low Temps]
        HBox itemBox = new HBox(12);
        itemBox.setAlignment(Pos.CENTER_LEFT);
        itemBox.setMaxWidth(Double.MAX_VALUE);
        itemBox.setPrefHeight(70);
        itemBox.setPadding(new Insets(10, 14, 10, 14));
        itemBox.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.08);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255, 255, 255, 0.15);" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 1;"
        );
        
        // Left: Weather Icon
        ImageView iconView = new ImageView();
        try {
            iconView.setImage(new Image(iconUrl, true));
        } catch (Exception e) {
            System.err.println("Error loading daily icon: " + e.getMessage());
        }
        iconView.setFitWidth(40);
        iconView.setFitHeight(40);
        iconView.setPreserveRatio(true);
        
        // Middle: Day name and description in a VBox
        VBox middleBox = new VBox(2);
        middleBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(middleBox, Priority.ALWAYS);
        
        Label dayLabel = new Label(day);
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        dayLabel.setTextFill(Color.WHITE);
        
        // Weather description
        Label descLabel = new Label(description != null && !description.isEmpty() ? WeatherHelper.capitalizeFirst(description) : "");
        descLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        descLabel.setTextFill(Color.rgb(255, 255, 255, 0.75));
        
        middleBox.getChildren().addAll(dayLabel, descLabel);
        
        // Right: Temperature labels (High / Low) in a VBox
        VBox tempBox = new VBox(2);
        tempBox.setAlignment(Pos.CENTER_RIGHT);
        tempBox.setMinWidth(60);
        
        Label maxTempLabel = new Label(formatTempForUnit(maxC));
        maxTempLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
    maxTempLabel.setTextFill(temperatureColorForC(maxC));
        
        Label minTempLabel = new Label(formatTempForUnit(minC));
        minTempLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
    minTempLabel.setTextFill(temperatureColorForC(minC));
        
        // Store raw Celsius values for unit toggle
        maxTempLabel.setUserData(Double.valueOf(maxC));
        minTempLabel.setUserData(Double.valueOf(minC));
        
        tempBox.getChildren().addAll(maxTempLabel, minTempLabel);
        
        // Assemble: Icon | Middle | Temps
        itemBox.getChildren().addAll(iconView, middleBox, tempBox);
        
    // Wrap HBox in a VBox so caller expects a VBox; constrain width so cards are centered
    VBox wrapper = new VBox(itemBox);
    wrapper.setAlignment(Pos.CENTER);
    // Narrower card width to match desired compact look
    final double CARD_WIDTH = 300;
    wrapper.setMaxWidth(CARD_WIDTH);
    wrapper.setPrefWidth(CARD_WIDTH);
    // Prevent inner HBox from stretching beyond the card width
    itemBox.setMaxWidth(CARD_WIDTH);
        
        // Hover effects on the HBox inside
        itemBox.setOnMouseEntered(e -> {
            itemBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255, 255, 255, 0.28);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1.2;"
            );
            itemBox.setScaleX(1.02);
            itemBox.setScaleY(1.02);
        });
        
        itemBox.setOnMouseExited(e -> {
            itemBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.08);" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;"
            );
            itemBox.setScaleX(1.0);
            itemBox.setScaleY(1.0);
        });
        
        return wrapper;
    }
    

    public void updateWeatherIcon(String iconUrl) { 
        if (weatherIcon == null) {
            weatherIcon = new ImageView();
            weatherIcon.setFitWidth(90);
            weatherIcon.setFitHeight(90);
            weatherIcon.setPreserveRatio(true);
        }
        try { 
            Image image = new Image(iconUrl, true); 
            weatherIcon.setImage(image); 
        } catch (Exception e) { 
            System.err.println("Error loading weather icon: " + e.getMessage()); 
        } 
    }
    
    public void showLoading(boolean show) {
        if (spinner != null) {
            spinner.setVisible(show);
        }
        // weatherContentBox is only used in old single-card layout, not in split layout
        if (weatherContentBox != null) {
            weatherContentBox.setVisible(!show);
        }
    }
    
    public void resetDisplay() {
        if (weatherIcon != null) {
            weatherIcon.setImage(null);
        }
        if (temperatureLabel != null) {
            temperatureLabel.setText("--°C");
        }
        if (descriptionLabel != null) {
            descriptionLabel.setText("Chọn thành phố để xem thời tiết");
        }
        if (cityLabel != null) {
            cityLabel.setText("Vị trí: --, --");
        }
        
        if (feelsLikeLabel != null) feelsLikeLabel.setText("--"); 
        if (humidityLabel != null) humidityLabel.setText("--"); 
        if (windLabel != null) windLabel.setText("--");
        if (sunriseLabel != null) sunriseLabel.setText("--"); 
        if (sunsetLabel != null) sunsetLabel.setText("--"); 
        if (pressureLabel != null) pressureLabel.setText("--");
        if (visibilityLabel != null) visibilityLabel.setText("--"); 
        if (minTempLabel != null) minTempLabel.setText("--"); 
        if (maxTempLabel != null) maxTempLabel.setText("--");
        
        if (hourlyDetailsBox != null) {
            hourlyDetailsBox.getChildren().clear();
        }
        
        if (dailyForecastBox != null) {
            dailyForecastBox.getChildren().clear();
        }
        
        stopCurrentAnimation();

        if (rootPane != null) {
            rootPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " +
                "#667eea 0%, #764ba2 50%, #ed4264 100%);"
            );
        }
        if (animationLayer != null && rootPane != null) {
            createAnimatedBackground();
        }
    }
    
    /**
     * Create favorites section with list of favorite cities
     */
    public VBox createFavoritesSection() {
        VBox container = new VBox(10);
        container.setAlignment(Pos.TOP_CENTER);
        // Slightly larger padding and fixed height to create a consistent card
        container.setPadding(new Insets(18, 14, 18, 14));
        container.setPrefHeight(300);
        container.setMinHeight(220);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.12);" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.25);" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"
        );
        
        // Title with heart icon
        Label title = new Label("❤ YÊU THÍCH");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);");
        
        // Favorites list container (use fixed-size rows for consistent layout)
        if (favoritesBox == null) {
            favoritesBox = new VBox(10);
        }
        favoritesBox.setAlignment(Pos.TOP_CENTER);
        favoritesBox.setFillWidth(true);

        // Empty state message
        Label emptyLabel = new Label("Chưa có thành phố yêu thích.\nBấm ❤ để thêm!");
        emptyLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        emptyLabel.setTextFill(Color.rgb(255, 255, 255, 0.7));
        emptyLabel.setStyle("-fx-text-alignment: center;");
        emptyLabel.setWrapText(true);
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setPrefHeight(80);
        emptyLabel.setAlignment(Pos.CENTER);

        favoritesBox.getChildren().add(emptyLabel);

        // Wrap favoritesBox in a ScrollPane so long lists scroll instead of overflowing
        ScrollPane favScroll = new ScrollPane(favoritesBox);
        favScroll.setFitToWidth(true);
        favScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        favScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        favScroll.setPrefHeight(180);
        favScroll.setMaxHeight(220);
        favScroll.getStyleClass().add("scroll-pane"); // thêm class CSS
        java.net.URL favCss = getClass().getResource("styles/scrollbar.css");
        if (favCss != null) {
            favScroll.getStylesheets().add(favCss.toExternalForm());
        }


        container.getChildren().addAll(title, favScroll);
        return container;
    }
    
    /**
     * Update favorites list display
     */
    public void updateFavoritesList(java.util.List<String> favorites, javafx.event.EventHandler<javafx.event.ActionEvent> onCityClick,
                                    javafx.event.EventHandler<javafx.event.ActionEvent> onDeleteClick) {
        if (favoritesBox == null) return;
        
        favoritesBox.getChildren().clear();
        
        if (favorites == null || favorites.isEmpty()) {
            Label emptyLabel = new Label("Chưa có thành phố yêu thích.\nBấm ❤ để thêm!");
            emptyLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            emptyLabel.setTextFill(Color.rgb(255, 255, 255, 0.7));
            emptyLabel.setStyle("-fx-text-alignment: center;");
            emptyLabel.setWrapText(true);
            emptyLabel.setMaxWidth(200);
            favoritesBox.getChildren().add(emptyLabel);
            return;
        }
        
        for (String cityKey : favorites) {
            // Fixed-height row for consistent list appearance
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setPrefHeight(48);
            row.setMinHeight(48);

            // City button fills remaining width, left-aligned text
            Button cityBtn = new Button(cityKey.replace(",", ", "));
            cityBtn.setMaxWidth(Double.MAX_VALUE);
            cityBtn.setPrefHeight(40);
            HBox.setHgrow(cityBtn, Priority.ALWAYS);
            cityBtn.setAlignment(Pos.CENTER_LEFT);
            cityBtn.setGraphic(new Label("📍"));
            cityBtn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 600;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 12 8 12;"
            );
            cityBtn.setUserData(cityKey);
            cityBtn.setOnAction(onCityClick);

            // Small circular delete button
            Button del = new Button("✖");
            del.setPrefSize(30, 30);
            del.setMinSize(30, 30);
            del.setMaxSize(30, 30);
            del.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                "-fx-background-radius: 16;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-cursor: hand;"
            );
            del.setTooltip(new Tooltip("Xóa yêu thích"));
            del.setUserData(cityKey);
            if (onDeleteClick != null) {
                del.setOnAction(onDeleteClick);
            } else {
                del.setDisable(true);
            }

            // Subtle hover effect for the city button
            cityBtn.setOnMouseEntered(e -> cityBtn.setStyle("-fx-background-color: rgba(100, 200, 255, 0.06); -fx-text-fill: white; -fx-font-size:13px; -fx-font-weight:600; -fx-padding:8 12 8 12;"));
            cityBtn.setOnMouseExited(e -> cityBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size:13px; -fx-font-weight:600; -fx-padding:8 12 8 12;"));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(cityBtn, spacer, del);
            favoritesBox.getChildren().add(row);
        }
    }
    
    // MapView removed
    public void showError(String message) {
        resetDisplay();
        if (descriptionLabel != null) {
            descriptionLabel.setText(message);
        }
    }
}
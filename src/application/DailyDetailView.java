package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;

import java.util.List;



public class DailyDetailView {

    public static void show(WeatherDataParser.DailyForecastItem item,
                            List<WeatherDataParser.ForecastItem> hourlyForDay) {

        Stage stage = new Stage();
        stage.setTitle("Chi tiết dự báo ngày " + item.date);

        // Create animated gradient background layer (like WeatherUI)
        Pane animationLayer = createAnimatedBackground();
        
        // Main content container
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: transparent;");

        // Hero header section with translucent card
        VBox heroSection = createHeroSection(item);
        
        // Stats cards grid with colorful gradients
        HBox statsGrid = createStatsGrid(item);
        
        // Chart section with glass-morphism design
        VBox chartSection = createEnhancedChartSection(hourlyForDay, item.timezone);
        
        // Precipitation probability chart
        VBox precipSection = createPrecipitationChart(hourlyForDay, item.timezone);
        
        // Action buttons
        HBox actionButtons = createActionButtons(stage);

        root.getChildren().addAll(heroSection, statsGrid, chartSection, precipSection, actionButtons);

        // Wrap content in ScrollPane with transparency
        ScrollPane mainScroll = new ScrollPane(root);
        mainScroll.setFitToWidth(true);
        mainScroll.setFitToHeight(true);
        mainScroll.setPannable(true);
        mainScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        // Load custom scrollbar styles
        try {
            java.net.URL css = DailyDetailView.class.getResource("styles/scrollbar.css");
            if (css != null) mainScroll.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignore) {}

        // Layer animation background and content
        StackPane layeredRoot = new StackPane();
        layeredRoot.getChildren().addAll(animationLayer, mainScroll);
        
        Scene scene = new Scene(layeredRoot, 1100, 620);
        stage.setScene(scene);
        stage.show();
        
        // Entrance animations
        playEntranceAnimations(heroSection, statsGrid, chartSection, precipSection, actionButtons);
    }

    private static VBox createHeroSection(WeatherDataParser.DailyForecastItem item) {
        VBox hero = new VBox(15);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(40, 50, 40, 50));
        hero.setMaxWidth(1000);
        // Translucent glass-morphism card (like WeatherUI weather card)
        hero.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.18);" +
            "-fx-background-radius: 28;" +
            "-fx-border-color: rgba(255, 255, 255, 0.35);" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 28;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 18, 0, 0, 6);"
        );

        // Date badge with vibrant gradient
        HBox dateBadge = new HBox(10);
        dateBadge.setAlignment(Pos.CENTER);
        dateBadge.setPadding(new Insets(12, 30, 12, 30));
        dateBadge.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(255,200,55,0.9), rgba(255,150,50,0.9));" +
            "-fx-background-radius: 25;" +
            "-fx-border-color: rgba(255, 255, 255, 0.4);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 25;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,180,0,0.4), 10, 0, 0, 3);"
        );
        
    Node dateIcon = createCalendarIcon(26);
        
        Label dateLabel = new Label(item.date);
        dateLabel.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-letter-spacing: 1px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 0, 2);"
        );
        dateBadge.getChildren().addAll(dateIcon, dateLabel);

        // Large temperature display (white text like WeatherUI)
        VBox tempDisplay = new VBox(5);
        tempDisplay.setAlignment(Pos.CENTER);
        
        HBox mainTempBox = new HBox(15);
        mainTempBox.setAlignment(Pos.CENTER);
        
        double avgTemp = (item.maxTemp + item.minTemp) / 2;
        Label mainTemp = new Label(String.format("%.0f°", avgTemp));
        mainTemp.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 96px;" +
            "-fx-font-weight: 800;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 5);"
        );
        
    VBox rangeBox = new VBox(6);
    rangeBox.setAlignment(Pos.CENTER_RIGHT);
        
    HBox maxBox = new HBox(6);
    maxBox.setAlignment(Pos.CENTER_RIGHT);
    Label maxIcon = new Label("↑");
    maxIcon.setStyle("-fx-text-fill: rgba(255,200,55,0.95); -fx-font-size: 20px; -fx-font-weight: 700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
    Label maxLabel = new Label(String.format("%.0f°", item.maxTemp));
    maxLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
    maxBox.getChildren().addAll(maxIcon, maxLabel);

    HBox minBox = new HBox(6);
    minBox.setAlignment(Pos.CENTER_RIGHT);
    Label minIcon = new Label("↓");
    minIcon.setStyle("-fx-text-fill: rgba(100,200,255,0.95); -fx-font-size: 20px; -fx-font-weight: 700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
    Label minLabel = new Label(String.format("%.0f°", item.minTemp));
    minLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 3, 0, 0, 1);");
    minBox.getChildren().addAll(minIcon, minLabel);
        
        rangeBox.getChildren().addAll(maxBox, minBox);
        mainTempBox.getChildren().addAll(mainTemp, rangeBox);
        
        // Weather description with bright styling
    HBox descBox = new HBox(10);
    descBox.setAlignment(Pos.CENTER);
    descBox.setPadding(new Insets(15, 0, 0, 0));

    // Use vector SVG icons for the hero weather symbol (avoids emoji glyph issues)
    Node weatherIcon = createWeatherIcon(item.description, 36);

    Label desc = new Label(WeatherHelper.capitalizeFirst(item.description));
        desc.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: 600;" +
            "-fx-letter-spacing: 0.5px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 5, 0, 0, 2);"
        );
        descBox.getChildren().addAll(weatherIcon, desc);

        tempDisplay.getChildren().addAll(mainTempBox, descBox);
        hero.getChildren().addAll(dateBadge, tempDisplay);
        
        return hero;
    }

    private static HBox createStatsGrid(WeatherDataParser.DailyForecastItem item) {
        HBox grid = new HBox(15);
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(1000);
        
        // Create stat cards with vibrant gradients and SVG icons
        VBox card1 = createStatCard(createFireIcon(48), "Cao nhất", String.format("%.0f°C", item.maxTemp), "linear-gradient(135deg, #FF6B6B, #FF8E53)");
        VBox card2 = createStatCard(createSnowflakeIcon(48), "Thấp nhất", String.format("%.0f°C", item.minTemp), "linear-gradient(135deg, #4FACFE, #00F2FE)");
        VBox card3 = createStatCard(createCloudIcon(48), "Thời tiết", WeatherHelper.capitalizeFirst(item.description), "linear-gradient(135deg, #A29BFE, #6C5CE7)");
        
        grid.getChildren().addAll(card1, card2, card3);
        return grid;
    }

    private static VBox createStatCard(Node iconNode, String label, String value, String gradientColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25, 35, 25, 35));
        // Glass-morphism card with gradient background (matching WeatherUI)
        card.setStyle(
            "-fx-background-color: " + gradientColor + ";" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
        );
        card.setPrefWidth(220);
        
        // Use the provided icon node instead of emoji label
        
        Label labelText = new Label(label);
        labelText.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.9);" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: 600;" +
            "-fx-letter-spacing: 1px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 3, 0, 0, 1);"
        );
        
        Label valueText = new Label(value);
        valueText.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 5, 0, 0, 2);"
        );
        valueText.setWrapText(true);
        valueText.setTextAlignment(TextAlignment.CENTER);
        valueText.setMaxWidth(180);
        
        card.getChildren().addAll(iconNode, labelText, valueText);
        
        // Smooth hover effect
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
            card.setStyle(
                    "-fx-background-color: " + gradientColor + ";" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(255, 255, 255, 0.5);" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 18;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 18, 0, 0, 8);"
                );
        });
        
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            card.setStyle(
                "-fx-background-color: " + gradientColor + ";" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 4);"
            );
        });
        
        return card;
    }

    private static VBox createEnhancedChartSection(List<WeatherDataParser.ForecastItem> hourlyForDay, int timezone) {
        VBox section = new VBox(20);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(35));
        section.setMaxWidth(1000);
        section.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-background-radius: 30;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 30;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 30, 0, 0, 10);"
        );

        // Header with gradient
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER);
        
        // Use SVG icon for chart header (sharp on all platforms)
        Node chartIcon = createChartIcon(32);
        
        VBox titleBox = new VBox(2);
        Label title = new Label("Biểu đồ nhiệt độ theo giờ");
        title.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;" +
            "-fx-letter-spacing: 0.5px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);"
        );
        
        Label subtitle = new Label("Dự báo chi tiết mỗi 3 giờ");
        subtitle.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.7);" +
            "-fx-font-size: 14px;" +
            "-fx-font-style: italic;"
        );
        
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(chartIcon, titleBox);

        // Enhanced chart
        Pane chartPane = createModernChart(hourlyForDay, timezone);
        
        section.getChildren().addAll(header, chartPane);
        return section;
    }

    private static Pane createModernChart(List<WeatherDataParser.ForecastItem> hourlyForDay, int timezone) {
        Pane chartPane = new Pane();
        chartPane.setPrefHeight(450);
        chartPane.setMinHeight(450);
        chartPane.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.2);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255, 255, 255, 0.2);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;"
        );

        if (hourlyForDay.isEmpty()) {
            Label noData = new Label("Không có dữ liệu");
            noData.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
            noData.setLayoutX(400);
            noData.setLayoutY(200);
            chartPane.getChildren().add(noData);
            return chartPane;
        }

        // Calculate ranges
        double minTemp = hourlyForDay.stream().mapToDouble(i -> i.temp).min().orElse(0);
        double maxTemp = hourlyForDay.stream().mapToDouble(i -> i.temp).max().orElse(30);
        double tempRange = maxTemp - minTemp;
        double padding = Math.max(tempRange * 0.2, 2);
        minTemp -= padding;
        maxTemp += padding;

        double chartWidth = 850;
        double chartHeight = 280;
        double leftMargin = 60;
        double topMargin = 50;
        double bottomMargin = 90;

        // Grid with glow effect
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            double y = topMargin + (chartHeight * i / gridLines);
            
            Line gridLine = new Line(leftMargin, y, leftMargin + chartWidth, y);
            gridLine.setStroke(Color.rgb(255, 255, 255, 0.12));
            gridLine.setStrokeWidth(1);
            gridLine.getStrokeDashArray().addAll(8d, 8d);
            chartPane.getChildren().add(gridLine);

            double tempValue = maxTemp - ((maxTemp - minTemp) * i / gridLines);
            Text label = new Text(String.format("%.0f°", tempValue));
            label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            label.setFill(Color.rgb(255, 255, 255, 0.9));
            
            DropShadow textGlow = new DropShadow();
            textGlow.setColor(Color.rgb(100, 200, 255, 0.6));
            textGlow.setRadius(6);
            label.setEffect(textGlow);
            
            label.setX(10);
            label.setY(y + 5);
            chartPane.getChildren().add(label);
        }

        int dataPoints = hourlyForDay.size();
        double xStep = chartWidth / Math.max(1, dataPoints - 1);

        // Gradient fill area
        javafx.scene.shape.Path areaPath = new javafx.scene.shape.Path();
        areaPath.getElements().add(new javafx.scene.shape.MoveTo(leftMargin, topMargin + chartHeight));
        
        for (int i = 0; i < dataPoints; i++) {
            WeatherDataParser.ForecastItem item = hourlyForDay.get(i);
            double x = leftMargin + (i * xStep);
            double y = topMargin + chartHeight - ((item.temp - minTemp) / (maxTemp - minTemp)) * chartHeight;
            areaPath.getElements().add(new javafx.scene.shape.LineTo(x, y));
        }
        
        areaPath.getElements().add(new javafx.scene.shape.LineTo(
            leftMargin + ((dataPoints - 1) * xStep), topMargin + chartHeight
        ));
        areaPath.getElements().add(new javafx.scene.shape.ClosePath());
        
        LinearGradient gradient = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 107, 107, 0.5)),
            new Stop(0.5, Color.rgb(238, 90, 111, 0.3)),
            new Stop(1, Color.rgb(79, 172, 254, 0.1))
        );
        areaPath.setFill(gradient);
        chartPane.getChildren().add(areaPath);

        // Draw glowing line segments
        for (int i = 0; i < dataPoints - 1; i++) {
            WeatherDataParser.ForecastItem current = hourlyForDay.get(i);
            WeatherDataParser.ForecastItem next = hourlyForDay.get(i + 1);

            double x1 = leftMargin + (i * xStep);
            double y1 = topMargin + chartHeight - ((current.temp - minTemp) / (maxTemp - minTemp)) * chartHeight;
            double x2 = leftMargin + ((i + 1) * xStep);
            double y2 = topMargin + chartHeight - ((next.temp - minTemp) / (maxTemp - minTemp)) * chartHeight;

            Line line = new Line(x1, y1, x2, y2);
            line.setStroke(Color.WHITE);
            line.setStrokeWidth(4);
            line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            
            DropShadow lineGlow = new DropShadow();
            lineGlow.setColor(Color.rgb(100, 200, 255, 0.8));
            lineGlow.setRadius(12);
            lineGlow.setSpread(0.4);
            line.setEffect(lineGlow);
            
            chartPane.getChildren().add(line);
        }

        // Data points with advanced interactions
        for (int i = 0; i < dataPoints; i++) {
            WeatherDataParser.ForecastItem item = hourlyForDay.get(i);
            double x = leftMargin + (i * xStep);
            double y = topMargin + chartHeight - ((item.temp - minTemp) / (maxTemp - minTemp)) * chartHeight;

            // Pulsing ring (visual) and a larger invisible hit area for reliable hover
            Circle pulseRing = new Circle(x, y, 20);
            pulseRing.setFill(Color.TRANSPARENT);
            pulseRing.setStroke(Color.rgb(100, 200, 255, 0.6));
            pulseRing.setStrokeWidth(3);
            pulseRing.setVisible(false);

            // Glow circle
            Circle glowCircle = new Circle(x, y, 12);
            glowCircle.setFill(Color.rgb(255, 255, 255, 0.3));
            Glow glow = new Glow(0.8);
            glowCircle.setEffect(glow);

            // Main point (smaller visual)
            Circle point = new Circle(x, y, 6);
            point.setFill(Color.WHITE);
            point.setStroke(Color.rgb(100, 200, 255));
            point.setStrokeWidth(3);
            DropShadow pointShadow = new DropShadow();
            pointShadow.setColor(Color.rgb(0, 0, 0, 0.5));
            pointShadow.setRadius(8);
            point.setEffect(pointShadow);

            // Larger transparent hit area so hover triggers when mouse is near the point
            Circle hitArea = new Circle(x, y, 20);
            hitArea.setFill(Color.TRANSPARENT);
            hitArea.setStrokeWidth(0);
            hitArea.setCursor(javafx.scene.Cursor.HAND);

            // Temperature label
            Text tempLabel = new Text(String.format("%.0f°", item.temp));
            tempLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            tempLabel.setFill(Color.WHITE);
            tempLabel.setX(x - 15);
            tempLabel.setY(y - 20);
            DropShadow tempGlow = new DropShadow();
            tempGlow.setColor(Color.rgb(100, 200, 255, 0.8));
            tempGlow.setRadius(6);
            tempLabel.setEffect(tempGlow);

            // Time label
            String timeStr = WeatherHelper.formatHour(item.dt, timezone);
            Text timeLabel = new Text(timeStr);
            timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            timeLabel.setFill(Color.rgb(255, 255, 255, 0.95));
            timeLabel.setTextAlignment(TextAlignment.CENTER);
            timeLabel.setX(x - 25);
            timeLabel.setY(topMargin + chartHeight + 35);

            // Weather emoji
            String weatherEmoji = getWeatherEmoji(item.description);
            Node weatherIconNode = makeIconWithFallback(item.iconCode, item.description, 24);
            weatherIconNode.setLayoutX(x - 12);
            weatherIconNode.setLayoutY(topMargin + chartHeight + 45);

            // Tooltip (install on hitArea for reliable activation)
         // Create tooltip
            Tooltip tooltip = new Tooltip();
            String tooltipText = String.format(
                "🕐 %s\n\n🌡 Nhiệt độ: %.1f°C\n💭 Cảm giác: %.1f°C\n💧 Độ ẩm: %d%%\n💨 Gió: %.1f m/s\n☁ Mây: %d%%\n\n%s %s",
                timeStr, item.temp, item.feelsLike, item.humidity,
                item.windSpeed, item.clouds, weatherEmoji,
                WeatherHelper.capitalizeFirst(item.description)
            );
            tooltip.setText(tooltipText);
            tooltip.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #fafbfc);" +
                "-fx-text-fill: #1e293b;" +
                "-fx-font-size: 13.5px;" +
                "-fx-font-weight: 600;" +
                "-fx-font-family: 'Segoe UI', 'SF Pro Display', system-ui, sans-serif;" +
                "-fx-padding: 16 20 16 20;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #c7d2fe;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(99, 102, 241, 0.25), 20, 0, 0, 4);"
            );
            tooltip.setShowDelay(javafx.util.Duration.millis(150));
            Tooltip.install(hitArea, tooltip);

            // CRITICAL: Set hitArea to be mouse transparent for tooltip to prevent flickering
            hitArea.setMouseTransparent(false); // Keep it interactive
            hitArea.setPickOnBounds(true); // Use bounds for hit testing

            // Use flags to prevent animation conflicts
            final boolean[] isHovering = {false};
            Duration animDuration = Duration.millis(250);

            // Store current animations to stop them if needed
            final ParallelTransition[] currentAnimation = {null};

            hitArea.setOnMouseEntered(e -> {
                if (isHovering[0]) return; // Already hovering, skip
                isHovering[0] = true;
                
                // Stop any running animation
                if (currentAnimation[0] != null) {
                    currentAnimation[0].stop();
                }
                
                pulseRing.setVisible(true);
                pulseRing.setOpacity(0);
                pulseRing.setScaleX(0.5);
                pulseRing.setScaleY(0.5);
                
                // Point animation
                Timeline pointAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(point.radiusProperty(), 6),
                        new KeyValue(point.fillProperty(), Color.WHITE)
                    ),
                    new KeyFrame(animDuration,
                        new KeyValue(point.radiusProperty(), 10, Interpolator.EASE_OUT),
                        new KeyValue(point.fillProperty(), Color.rgb(139, 92, 246))
                    )
                );
                
                // Glow animation
                Timeline glowAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(glowCircle.radiusProperty(), 12),
                        new KeyValue(glowCircle.fillProperty(), Color.rgb(255, 255, 255, 0.3))
                    ),
                    new KeyFrame(animDuration,
                        new KeyValue(glowCircle.radiusProperty(), 18, Interpolator.EASE_OUT),
                        new KeyValue(glowCircle.fillProperty(), Color.rgb(139, 92, 246, 0.6))
                    )
                );
                
                // Pulse ring scale
                ScaleTransition pulseScale = new ScaleTransition(animDuration, pulseRing);
                pulseScale.setToX(1.2);
                pulseScale.setToY(1.2);
                pulseScale.setInterpolator(Interpolator.EASE_OUT);
                
                // Pulse ring fade
                FadeTransition pulseFade = new FadeTransition(animDuration, pulseRing);
                pulseFade.setToValue(0.7);
                pulseFade.setInterpolator(Interpolator.EASE_OUT);
                
                // Label scale
                ScaleTransition labelScale = new ScaleTransition(animDuration, tempLabel);
                labelScale.setToX(1.15);
                labelScale.setToY(1.15);
                labelScale.setInterpolator(Interpolator.EASE_OUT);
                
                // Label color
                Timeline labelColor = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(tempLabel.fillProperty(), Color.WHITE),
                        new KeyValue(tempLabel.fontProperty(), Font.font("Arial", FontWeight.BOLD, 16))
                    ),
                    new KeyFrame(animDuration,
                        new KeyValue(tempLabel.fillProperty(), Color.rgb(139, 92, 246)),
                        new KeyValue(tempLabel.fontProperty(), Font.font("Arial", FontWeight.BOLD, 18))
                    )
                );
                
                // Play all animations
                currentAnimation[0] = new ParallelTransition(
                    pointAnim, glowAnim, pulseScale, pulseFade, labelScale, labelColor
                );
                currentAnimation[0].play();
            });

            hitArea.setOnMouseExited(e -> {
                if (!isHovering[0]) return; // Not hovering, skip
                isHovering[0] = false;
                
                // Stop any running animation
                if (currentAnimation[0] != null) {
                    currentAnimation[0].stop();
                }
                
                // Point animation
                Timeline pointAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(point.radiusProperty(), point.getRadius()),
                        new KeyValue(point.fillProperty(), point.getFill())
                    ),
                    new KeyFrame(animDuration,
                        new KeyValue(point.radiusProperty(), 6, Interpolator.EASE_IN),
                        new KeyValue(point.fillProperty(), Color.WHITE)
                    )
                );
                
                // Glow animation
                Timeline glowAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(glowCircle.radiusProperty(), glowCircle.getRadius()),
                        new KeyValue(glowCircle.fillProperty(), glowCircle.getFill())
                    ),
                    new KeyFrame(animDuration,
                        new KeyValue(glowCircle.radiusProperty(), 12, Interpolator.EASE_IN),
                        new KeyValue(glowCircle.fillProperty(), Color.rgb(255, 255, 255, 0.3))
                    )
                );
                
                // Pulse ring fade out
                FadeTransition pulseFade = new FadeTransition(Duration.millis(200), pulseRing);
                pulseFade.setToValue(0);
                pulseFade.setInterpolator(Interpolator.EASE_IN);
                pulseFade.setOnFinished(ev -> pulseRing.setVisible(false));
                
                // Label scale
                ScaleTransition labelScale = new ScaleTransition(animDuration, tempLabel);
                labelScale.setToX(1.0);
                labelScale.setToY(1.0);
                labelScale.setInterpolator(Interpolator.EASE_IN);
                
                // Label color
                Timeline labelColor = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(tempLabel.fillProperty(), tempLabel.getFill()),
                        new KeyValue(tempLabel.fontProperty(), Font.font("Arial", FontWeight.BOLD, 18))
                    ),
                    new KeyFrame(animDuration,
                        new KeyValue(tempLabel.fillProperty(), Color.WHITE),
                        new KeyValue(tempLabel.fontProperty(), Font.font("Arial", FontWeight.BOLD, 16))
                    )
                );
                
                // Play all animations
                currentAnimation[0] = new ParallelTransition(
                    pointAnim, glowAnim, pulseFade, labelScale, labelColor
                );
                currentAnimation[0].play();
            });
            // Order children so visual elements render correctly (pulse under point)
            chartPane.getChildren().addAll(glowCircle, pulseRing, point, tempLabel, timeLabel, weatherIconNode, hitArea);
        }

        return chartPane;
    }

    /**
     * Create precipitation probability chart (bar chart style)
     */
    private static VBox createPrecipitationChart(List<WeatherDataParser.ForecastItem> hourlyForDay, int timezone) {
        VBox section = new VBox(20);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(35));
        section.setMaxWidth(1000);
        section.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15);" +
            "-fx-background-radius: 30;" +
            "-fx-border-color: rgba(255, 255, 255, 0.3);" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 30;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 30, 0, 0, 10);"
        );

        // Header
    HBox header = new HBox(15);
    header.setAlignment(Pos.CENTER);
        
    // Use rain SVG icon instead of emoji (avoids missing-glyph box)
    Node chartIcon = createRainIcon(32);
        
        VBox titleBox = new VBox(2);
        Label title = new Label("Khả năng có mưa");
        title.setStyle(
            "-fx-text-fill: white;" +
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;" +
            "-fx-letter-spacing: 0.5px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);"
        );
        
        Label subtitle = new Label("Xác suất mưa theo giờ (%)");
        subtitle.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.7);" +
            "-fx-font-size: 14px;" +
            "-fx-font-style: italic;"
        );
        
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(chartIcon, titleBox);

        // Bar chart
        Pane chartPane = createPrecipitationBarChart(hourlyForDay, timezone);
        
        section.getChildren().addAll(header, chartPane);
        return section;
    }

    /**
     * Create bar chart for precipitation probability
     */
    private static Pane createPrecipitationBarChart(List<WeatherDataParser.ForecastItem> hourlyForDay, int timezone) {
        Pane chartPane = new Pane();
        chartPane.setPrefHeight(320);
        chartPane.setMinHeight(320);
        chartPane.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.2);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255, 255, 255, 0.2);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;"
        );

        if (hourlyForDay.isEmpty()) {
            Label noData = new Label("Không có dữ liệu");
            noData.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
            noData.setLayoutX(400);
            noData.setLayoutY(150);
            chartPane.getChildren().add(noData);
            return chartPane;
        }

        double chartWidth = 850;
        double chartHeight = 200;
        double leftMargin = 60;
        double topMargin = 40;
        double bottomMargin = 60;

        int dataPoints = hourlyForDay.size();
        double barWidth = (chartWidth / dataPoints) * 0.7; // 70% width for bars
        double barSpacing = (chartWidth / dataPoints);

        // Draw horizontal grid lines for percentage reference
        for (int i = 0; i <= 4; i++) {
            double y = topMargin + (chartHeight * i / 4);
            
            Line gridLine = new Line(leftMargin, y, leftMargin + chartWidth, y);
            gridLine.setStroke(Color.rgb(255, 255, 255, 0.12));
            gridLine.setStrokeWidth(1);
            gridLine.getStrokeDashArray().addAll(8d, 8d);
            chartPane.getChildren().add(gridLine);

            int percentage = 100 - (i * 25);
            Text label = new Text(percentage + "%");
            label.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            label.setFill(Color.rgb(255, 255, 255, 0.9));
            
            DropShadow textGlow = new DropShadow();
            textGlow.setColor(Color.rgb(100, 200, 255, 0.5));
            textGlow.setRadius(4);
            label.setEffect(textGlow);
            
            label.setX(10);
            label.setY(y + 5);
            chartPane.getChildren().add(label);
        }

        // Draw bars for each time point
        for (int i = 0; i < dataPoints; i++) {
            WeatherDataParser.ForecastItem item = hourlyForDay.get(i);
            double x = leftMargin + (i * barSpacing) + (barSpacing - barWidth) / 2;
            double popPercent = item.pop * 100; // Convert 0.0-1.0 to 0-100
            double barHeight = (popPercent / 100.0) * chartHeight;
            double y = topMargin + chartHeight - barHeight;

            // Create bar with gradient based on precipitation level
            Rectangle bar = new Rectangle(x, y, barWidth, barHeight);
            
            // Color gradient: blue for low, cyan for medium, intense blue for high
            Color barColor;
            if (popPercent < 30) {
                barColor = Color.rgb(79, 172, 254, 0.6); // light blue
            } else if (popPercent < 70) {
                barColor = Color.rgb(0, 200, 255, 0.75); // cyan
            } else {
                barColor = Color.rgb(0, 100, 255, 0.9); // intense blue
            }
            
            LinearGradient barGradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, barColor),
                new Stop(1, barColor.deriveColor(0, 1.0, 0.7, 1.0))
            );
            bar.setFill(barGradient);
            bar.setArcWidth(8);
            bar.setArcHeight(8);
            
            // Glow effect
            DropShadow barGlow = new DropShadow();
            barGlow.setColor(Color.rgb(0, 150, 255, 0.6));
            barGlow.setRadius(10);
            barGlow.setSpread(0.3);
            bar.setEffect(barGlow);

            // Percentage label on top of bar
            Text percentLabel = new Text(String.format("%.0f%%", popPercent));
            percentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            percentLabel.setFill(Color.WHITE);
            percentLabel.setX(x + barWidth / 2 - 12);
            percentLabel.setY(y - 8);
            
            DropShadow percentGlow = new DropShadow();
            percentGlow.setColor(Color.rgb(0, 0, 0, 0.5));
            percentGlow.setRadius(3);
            percentLabel.setEffect(percentGlow);

            // Time label below chart
            String timeStr = WeatherHelper.formatHour(item.dt, timezone);
            Text timeLabel = new Text(timeStr);
            timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            timeLabel.setFill(Color.rgb(255, 255, 255, 0.95));
            timeLabel.setTextAlignment(TextAlignment.CENTER);
            timeLabel.setX(x + barWidth / 2 - 15);
            timeLabel.setY(topMargin + chartHeight + 25);

            // Tooltip for detailed info
            Tooltip tooltip = new Tooltip();
            String tooltipText = String.format(
                "\u23F0  %s\n\n\uD83C\uDF27\uFE0F  Khả năng mưa: %.0f%%\n\uD83D\uDCA7  Lượng mưa: %.1f mm",
                timeStr, popPercent, item.rain3h
            );
            tooltip.setText(tooltipText);
            tooltip.setStyle(
                "-fx-background-color: linear-gradient(135deg, rgba(255,255,255,0.98), rgba(250,250,250,0.98));" +
                "-fx-text-fill: #111827;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 500;" +
                "-fx-padding: 12;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(0,0,0,0.06);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;"
            );
            Tooltip.install(bar, tooltip);

            // Hover animation for bar
            bar.setOnMouseEntered(e -> {
                bar.setScaleY(1.05);
                bar.setFill(barGradient.getStops().get(0).getColor().brighter());
                percentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            });

            bar.setOnMouseExited(e -> {
                bar.setScaleY(1.0);
                bar.setFill(barGradient);
                percentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            });

            chartPane.getChildren().addAll(bar, percentLabel, timeLabel);
        }

        return chartPane;
    }

    private static HBox createActionButtons(Stage stage) {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        
    Button closeBtn = createGlowButton("\u2715  Đóng", "#ff6b6b");
        closeBtn.setOnAction(e -> stage.close());
        
        buttonBox.getChildren().add(closeBtn);
        return buttonBox;
    }

    private static Button createGlowButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: linear-gradient(to right, " + color + ", " + adjustColor(color) + ");" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 18px;" +
            "-fx-padding: 16 50 16 50;" +
            "-fx-background-radius: 30;" +
            "-fx-border-color: white;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 30;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + hexToRgba(color, 0.6) + ", 20, 0, 0, 8);"
        );
        
        btn.setOnMouseEntered(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to right, " + adjustColor(color) + ", " + color + ");" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 18px;" +
                "-fx-padding: 16 50 16 50;" +
                "-fx-background-radius: 30;" +
                "-fx-border-color: white;" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 30;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + hexToRgba(color, 0.9) + ", 30, 0, 0, 12);"
            );
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });
        
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                "-fx-background-color: linear-gradient(to right, " + color + ", " + adjustColor(color) + ");" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 18px;" +
                "-fx-padding: 16 50 16 50;" +
                "-fx-background-radius: 30;" +
                "-fx-border-color: white;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 30;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + hexToRgba(color, 0.6) + ", 20, 0, 0, 8);"
            );
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        
        return btn;
    }

    private static void playEntranceAnimations(VBox hero, HBox stats, VBox chart, VBox precip, HBox buttons) {
        // Hero fade in from top
        TranslateTransition heroTrans = new TranslateTransition(Duration.millis(600), hero);
        heroTrans.setFromY(-50);
        heroTrans.setToY(0);
        FadeTransition heroFade = new FadeTransition(Duration.millis(600), hero);
        heroFade.setFromValue(0);
        heroFade.setToValue(1);
        
        // Stats slide in from left
        TranslateTransition statsTrans = new TranslateTransition(Duration.millis(700), stats);
        statsTrans.setFromX(-80);
        statsTrans.setToX(0);
        FadeTransition statsFade = new FadeTransition(Duration.millis(700), stats);
        statsFade.setFromValue(0);
        statsFade.setToValue(1);
        statsTrans.setDelay(Duration.millis(200));
        statsFade.setDelay(Duration.millis(200));
        
        // Chart slide in from right
        TranslateTransition chartTrans = new TranslateTransition(Duration.millis(800), chart);
        chartTrans.setFromX(80);
        chartTrans.setToX(0);
        FadeTransition chartFade = new FadeTransition(Duration.millis(800), chart);
        chartFade.setFromValue(0);
        chartFade.setToValue(1);
        chartTrans.setDelay(Duration.millis(400));
        chartFade.setDelay(Duration.millis(400));
        
        // Precipitation chart slide in from left
        TranslateTransition precipTrans = new TranslateTransition(Duration.millis(800), precip);
        precipTrans.setFromX(-80);
        precipTrans.setToX(0);
        FadeTransition precipFade = new FadeTransition(Duration.millis(800), precip);
        precipFade.setFromValue(0);
        precipFade.setToValue(1);
        precipTrans.setDelay(Duration.millis(600));
        precipFade.setDelay(Duration.millis(600));
        
        // Buttons fade in from bottom
        TranslateTransition btnTrans = new TranslateTransition(Duration.millis(600), buttons);
        btnTrans.setFromY(30);
        btnTrans.setToY(0);
        FadeTransition btnFade = new FadeTransition(Duration.millis(600), buttons);
        btnFade.setFromValue(0);
        btnFade.setToValue(1);
        btnTrans.setDelay(Duration.millis(800));
        btnFade.setDelay(Duration.millis(800));
        
        // Play all animations
        new ParallelTransition(heroTrans, heroFade).play();
        new ParallelTransition(statsTrans, statsFade).play();
        new ParallelTransition(chartTrans, chartFade).play();
        new ParallelTransition(precipTrans, precipFade).play();
        new ParallelTransition(btnTrans, btnFade).play();
    }

    private static String adjustColor(String hexColor) {
        // Lighten color for gradient
        try {
            int r = Integer.parseInt(hexColor.substring(1, 3), 16);
            int g = Integer.parseInt(hexColor.substring(3, 5), 16);
            int b = Integer.parseInt(hexColor.substring(5, 7), 16);
            
            r = Math.min(255, r + 30);
            g = Math.min(255, g + 30);
            b = Math.min(255, b + 30);
            
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
    }

    /**
     * Convert a hex color like "#ff6b6b" to an rgba(...) CSS string with the given alpha.
     */
    private static String hexToRgba(String hexColor, double alpha) {
        try {
            String h = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
            if (h.length() == 3) {
                // expand short form e.g. f60 -> ff6600
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, Math.max(0, Math.min(1.0, alpha)));
        } catch (Exception ex) {
            // fallback: return white with provided alpha
            return String.format("rgba(255, 255, 255, %.2f)", Math.max(0, Math.min(1.0, alpha)));
        }
    }

    private static String getWeatherEmoji(String description) {
        String desc = description.toLowerCase();
    if (desc.contains("clear")) return "\u2600\uFE0F";
    if (desc.contains("cloud")) return "\u2601\uFE0F";
    if (desc.contains("rain") || desc.contains("drizzle")) return "\uD83C\uDF27\uFE0F";
    if (desc.contains("thunder") || desc.contains("storm")) return "\u26C8\uFE0F";
    if (desc.contains("snow")) return "\u2744\uFE0F";
    if (desc.contains("mist") || desc.contains("fog")) return "\uD83C\uDF01"; // alternative fog emoji
    return "\uD83C\uDF24\uFE0F";
    }

    /**
     * Try to load the remote OpenWeather icon by code and overlay it on top of a simple emoji fallback.
     * If the remote icon fails to load the emoji remains visible.
     */
    private static Node makeIconWithFallback(String iconCode, String description, double size) {
        // fallback emoji label
        Label fallback = new Label(getWeatherEmoji(description == null ? "" : description));
        fallback.setStyle("-fx-font-size: " + (int)size + "px;");

        if (iconCode == null || iconCode.isEmpty()) {
            return fallback;
        }

        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        container.getChildren().add(fallback);

        try {
            String url = WeatherHelper.getWeatherIconUrl(iconCode);
            if (url != null && !url.isEmpty()) {
                Image img = new Image(url, size, size, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(size);
                iv.setFitHeight(size);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);

                // hide the ImageView if loading fails
                img.errorProperty().addListener((obs, oldVal, newVal) -> {
                    iv.setVisible(!newVal);
                });
                if (img.isError()) iv.setVisible(false);

                container.getChildren().add(iv);
            }
        } catch (Exception ex) {
            // ignore - fallback emoji remains visible
        }

        return container;
    }

    /**
     * Create animated gradient background (matching WeatherUI style)
     */
    private static Pane createAnimatedBackground() {
        Pane animLayer = new Pane();
        animLayer.setMouseTransparent(true);
        animLayer.setPickOnBounds(false);
        
        // Background rectangle fills entire pane
        Rectangle bgRect = new Rectangle();
        bgRect.widthProperty().bind(animLayer.widthProperty());
        bgRect.heightProperty().bind(animLayer.heightProperty());
        
        // Animated gradient colors (matching WeatherUI default)
        Color baseC1 = Color.rgb(102, 126, 234);
        Color baseC2 = Color.rgb(118, 75, 162);
        Color baseC3 = Color.rgb(237, 66, 100);
        Color baseC4 = Color.rgb(118, 75, 162);
        
        DoubleProperty blendProp = new SimpleDoubleProperty(0);
        blendProp.addListener((obs, oldVal, newVal) -> {
            double blend = newVal.doubleValue();
            Stop[] stops = new Stop[]{
                new Stop(0, baseC1.interpolate(baseC3, blend)),
                new Stop(1, baseC2.interpolate(baseC4, blend))
            };
            bgRect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops));
        });
        
        animLayer.getChildren().add(bgRect);
        
        // Animate the gradient blend
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(blendProp, 0)),
            new KeyFrame(Duration.seconds(15), new KeyValue(blendProp, 1))
        );
        timeline.setAutoReverse(true);
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        
        return animLayer;
    }
    
    /**
     * Create SVG-based fire/flame icon
     */
    private static StackPane createFireIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        
        SVGPath flame = new SVGPath();
        flame.setContent("M12 2c1.5 4 4 6 7 7-3-1-5-2-7-2-2 0-4 1-7 2 3-1 5.5-3 7-7zm0 5c1 3 3 4.5 5 5.5-2.5-.5-4-1.5-5-1.5s-2.5 1-5 1.5c2-1 4-2.5 5-5.5zm0 5c.5 2 2 3 3.5 3.5-2-.5-3-1-3.5-1s-1.5.5-3.5 1c1.5-.5 3-1.5 3.5-3.5z");
        
        double scale = size / 24.0;
        flame.setScaleX(scale);
        flame.setScaleY(scale);
        
        LinearGradient flameGradient = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 200, 50)),
            new Stop(1, Color.rgb(255, 80, 50))
        );
        flame.setFill(flameGradient);
        
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(255, 150, 0, 0.8));
        glow.setRadius(15);
        glow.setSpread(0.5);
        flame.setEffect(glow);
        
        container.getChildren().add(flame);
        return container;
    }

    /**
     * Create SVG-based snowflake icon
     */
    private static StackPane createSnowflakeIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        
        SVGPath snowflake = new SVGPath();
        snowflake.setContent("M12 2L12 22M12 2L9 5M12 2L15 5M12 22L9 19M12 22L15 19M2 12L22 12M2 12L5 9M2 12L5 15M22 12L19 9M22 12L19 15M5.64 5.64L18.36 18.36M5.64 5.64L8.05 8.05M18.36 18.36L15.95 15.95M5.64 18.36L18.36 5.64M5.64 18.36L8.05 15.95M18.36 5.64L15.95 8.05");
        
        double scale = size / 24.0;
        snowflake.setScaleX(scale);
        snowflake.setScaleY(scale);
        snowflake.setStroke(Color.rgb(100, 200, 255));
        snowflake.setStrokeWidth(2);
        snowflake.setFill(Color.TRANSPARENT);
        
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(100, 200, 255, 0.8));
        glow.setRadius(12);
        glow.setSpread(0.4);
        snowflake.setEffect(glow);
        
        container.getChildren().add(snowflake);
        return container;
    }

    /**
     * Create SVG-based cloud icon
     */
    private static StackPane createCloudIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);
        
        SVGPath cloud = new SVGPath();
        cloud.setContent("M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z");
        
        double scale = size / 24.0;
        cloud.setScaleX(scale);
        cloud.setScaleY(scale);
        
        LinearGradient cloudGradient = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 255)),
            new Stop(1, Color.rgb(230, 230, 250))
        );
        cloud.setFill(cloudGradient);
        
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(200, 200, 255, 0.6));
        glow.setRadius(10);
        glow.setSpread(0.3);
        cloud.setEffect(glow);
        
        container.getChildren().add(cloud);
        return container;
    }

    /**
     * Create simple SVG-based sun icon
     */
    private static StackPane createCalendarIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);

        SVGPath box = new SVGPath();
        // simple calendar outline
        box.setContent("M3 6h18v13H3z M16 3v4 M8 3v4");
        double scale = size / 24.0;
        box.setScaleX(scale);
        box.setScaleY(scale);
        box.setFill(Color.rgb(255,255,255));
        box.setOpacity(0.95);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(255,255,255,0.6));
        glow.setRadius(6);
        box.setEffect(glow);

        container.getChildren().add(box);
        return container;
    }

    private static StackPane createSunIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);

        Circle core = new Circle(size * 0.32);
        RadialGradient rg = new RadialGradient(0, 0.1, 0.3, 0.3, 0.7, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 220, 80)),
            new Stop(1, Color.rgb(255, 160, 50))
        );
        core.setFill(rg);
        core.setStroke(Color.rgb(255, 200, 80));
        core.setStrokeWidth(1.2);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(255, 200, 80, 0.6));
        glow.setRadius(12);
        core.setEffect(glow);

        container.getChildren().add(core);
        return container;
    }

    /**
     * Choose an appropriate SVG icon for a weather description
     */
    private static Node createWeatherIcon(String description, double size) {
        String desc = description == null ? "" : description.toLowerCase();
        if (desc.contains("clear") || desc.contains("sun")) {
            return createSunIcon(size);
        }
        if (desc.contains("snow")) {
            return createSnowflakeIcon(size);
        }
        if (desc.contains("rain") || desc.contains("drizzle") || desc.contains("shower")) {
            // No dedicated raindrop icon yet; reuse cloud which works well for rain scenarios
            return createCloudIcon(size);
        }
        if (desc.contains("cloud") || desc.contains("overcast") || desc.contains("mist") || desc.contains("fog")) {
            return createCloudIcon(size);
        }
        if (desc.contains("thunder") || desc.contains("storm")) {
            return createCloudIcon(size);
        }
        // Fallback
        return createCloudIcon(size);
    }

    /**
     * Create SVG-based rain icon (cloud + drops)
     */
    private static StackPane createRainIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);

        // Cloud base
        SVGPath cloud = new SVGPath();
        cloud.setContent("M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z");
        double scale = size / 24.0;
        cloud.setScaleX(scale);
        cloud.setScaleY(scale);
        cloud.setFill(Color.rgb(240, 245, 255));
        cloud.setOpacity(0.95);

        DropShadow cloudGlow = new DropShadow();
        cloudGlow.setColor(Color.rgb(150, 180, 255, 0.35));
        cloudGlow.setRadius(8);
        cloud.setEffect(cloudGlow);

        // Raindrops (three drops)
        javafx.scene.shape.SVGPath drop1 = new javafx.scene.shape.SVGPath();
        drop1.setContent("M2 6c0 1 1 2 1 2s1-1 1-2a1 1 0 0 0-2 0z");
        drop1.setScaleX(scale * 0.9);
        drop1.setScaleY(scale * 0.9);
        drop1.setFill(Color.rgb(100, 180, 255));
        drop1.setLayoutX(-6);
        drop1.setLayoutY(size * 0.28);

        javafx.scene.shape.SVGPath drop2 = new javafx.scene.shape.SVGPath();
        drop2.setContent("M2 6c0 1 1 2 1 2s1-1 1-2a1 1 0 0 0-2 0z");
        drop2.setScaleX(scale * 0.9);
        drop2.setScaleY(scale * 0.9);
        drop2.setFill(Color.rgb(80, 160, 255));
        drop2.setLayoutX(0);
        drop2.setLayoutY(size * 0.36);

        javafx.scene.shape.SVGPath drop3 = new javafx.scene.shape.SVGPath();
        drop3.setContent("M2 6c0 1 1 2 1 2s1-1 1-2a1 1 0 0 0-2 0z");
        drop3.setScaleX(scale * 0.9);
        drop3.setScaleY(scale * 0.9);
        drop3.setFill(Color.rgb(60, 140, 255));
        drop3.setLayoutX(6);
        drop3.setLayoutY(size * 0.28);

        container.getChildren().addAll(cloud, drop1, drop2, drop3);
        return container;
    }

    /**
     * Create a small bar-chart SVG icon for headers
     */
    private static StackPane createChartIcon(double size) {
        StackPane container = new StackPane();
        container.setPrefSize(size, size);

        double barW = size * 0.14;
        Rectangle b1 = new Rectangle(barW, size * 0.4, Color.rgb(255, 255, 255, 0.9));
        Rectangle b2 = new Rectangle(barW, size * 0.6, Color.rgb(255, 255, 255, 0.95));
        Rectangle b3 = new Rectangle(barW, size * 0.3, Color.rgb(255, 255, 255, 0.85));
        b1.setArcWidth(3); b1.setArcHeight(3);
        b2.setArcWidth(3); b2.setArcHeight(3);
        b3.setArcWidth(3); b3.setArcHeight(3);

        HBox bars = new HBox(barW * 0.5, b1, b2, b3);
        bars.setAlignment(Pos.CENTER);
        container.getChildren().add(bars);
        return container;
    }

}

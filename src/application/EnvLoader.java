package application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple .env file loader for loading environment variables from .env file
 */
public class EnvLoader {
    private static final Map<String, String> envVars = new HashMap<>();
    private static boolean loaded = false;
    
    /**
     * Load .env file from src/application directory
     */
    public static void load() {
        if (loaded) return;
        
        try {
            // Try multiple possible locations
            String[] possiblePaths = {
                "src/application/.env",
                ".env",
                "app123/src/application/.env"
            };
            
            Path envPath = null;
            for (String pathStr : possiblePaths) {
                Path p = Paths.get(pathStr);
                if (Files.exists(p)) {
                    envPath = p;
                    break;
                }
            }
            
            if (envPath == null) {
                System.err.println("⚠️ Warning: .env file not found. Trying system environment variables.");
                loaded = true;
                return;
            }
            
            System.out.println("✅ Loading .env from: " + envPath.toAbsolutePath());
            
            try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    // Parse KEY=VALUE format
                    int equalIndex = line.indexOf('=');
                    if (equalIndex > 0) {
                        String key = line.substring(0, equalIndex).trim();
                        String value = line.substring(equalIndex + 1).trim();
                        
                        // Remove surrounding quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        envVars.put(key, value);
                        System.out.println("  Loaded: " + key);
                    }
                }
            }
            
            loaded = true;
            System.out.println("✅ .env file loaded successfully with " + envVars.size() + " variables");
            
        } catch (IOException e) {
            System.err.println("❌ Error loading .env file: " + e.getMessage());
            e.printStackTrace();
            loaded = true; // Mark as loaded to avoid retrying
        }
    }
    
    /**
     * Get environment variable, first from .env file, then from system env
     */
    public static String getenv(String key) {
        if (!loaded) {
            load();
        }
        
        // First try .env file
        String value = envVars.get(key);
        
        // Fallback to system environment
        if (value == null) {
            value = System.getenv(key);
        }
        
        return value;
    }
    
    /**
     * Get environment variable with default value
     */
    public static String getenv(String key, String defaultValue) {
        String value = getenv(key);
        return value != null ? value : defaultValue;
    }
}

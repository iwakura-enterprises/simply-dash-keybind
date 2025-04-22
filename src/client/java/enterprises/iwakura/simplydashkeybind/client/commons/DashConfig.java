package enterprises.iwakura.simplydashkeybind.client.commons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

@Data
public class DashConfig {

    private static final Logger logger = LoggerFactory.getLogger(DashConfig.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private double dashStrength = 0.6;
    private double dashStrengthMultiplier = 0.3;
    private long dashCooldownMillis = 4000;

    /**
     * Loads the config from the file.
     *
     * @return The loaded config
     */
    public static DashConfig load() {
        File configFile = new File("config/simplydashkeybind.json");
        if (!configFile.exists()) {
            var config = new DashConfig();
            config.save();
            return config;
        }

        // Load the config from the file
        try {
            String json = new String(Files.readAllBytes(configFile.toPath()));
            var config = gson.fromJson(json, DashConfig.class);
            config.save();
            return config;
        } catch (Exception exception) {
            logger.error("Failed to load dash config! Using default values.", exception);
            return new DashConfig();
        }
    }

    /**
     * Saves the config to the file.
     */
    public void save() {
        File configFile = new File("config/simplydashkeybind.json");
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            String json = gson.toJson(this);
            Files.write(configFile.toPath(), json.getBytes());
        } catch (Exception exception) {
            logger.error("Failed to save dash config!", exception);
        }
    }
}

package de.fanta.stats;

import de.fanta.stats.client.StatsClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.TextColor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static String statsurl = "Cubes";
    public static int places = 3;
    public static Boolean headline = true;

    static final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("cubeside-stats.properties");

    static void serialize() {
        Properties prop = new Properties();
        prop.setProperty("statsurl", statsurl);
        prop.setProperty("places", String.valueOf(places));
        prop.setProperty("headline", String.valueOf(headline));
        try {
            OutputStream s = Files.newOutputStream(configPath);
            prop.store(s, "Cubeside Config");
            s.close();
        } catch (IOException e) {
            StatsClient.LOGGER.warn("Failed to write config!");
        }
    }

    public static void deserialize() {
        Properties prop = new Properties();
        try {
            InputStream s = Files.newInputStream(configPath);
            prop.load(s);
            statsurl = prop.getProperty("statsurl", statsurl);
            places = Integer.parseInt(prop.getProperty("places", String.valueOf(places)));
            headline = Boolean.parseBoolean(prop.getProperty("headline", "true"));
        } catch (IOException e) {
            StatsClient.LOGGER.warn("Failed to read config!");
        }
        Config.serialize();
    }
}

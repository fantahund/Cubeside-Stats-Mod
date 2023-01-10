package de.fanta.stats;

import de.fanta.stats.client.StatsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Config {
    public static Boolean showstats = true;
    public static String statsKeyID = "freebuildCubes";
    public static int places = 3;
    public static Boolean headline = true;
    public static String otherPlayers = "";
    static final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("cubeside-stats.properties");
    public static int wight = 0;
    public static int high = 0;

    static void serialize() {
        Properties prop = new Properties();
        prop.setProperty("showstats", String.valueOf(showstats));
        prop.setProperty("statsKeyID", statsKeyID);
        prop.setProperty("places", String.valueOf(places));
        prop.setProperty("headline", String.valueOf(headline));
        prop.setProperty("otherPlayer", String.valueOf(otherPlayers));
        prop.setProperty("wight", String.valueOf(wight));
        prop.setProperty("high", String.valueOf(high));
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
            showstats = Boolean.parseBoolean(prop.getProperty("showstats", "true"));
            statsKeyID = prop.getProperty("statsKeyID", statsKeyID);
            places = Integer.parseInt(prop.getProperty("places", String.valueOf(places)));
            headline = Boolean.parseBoolean(prop.getProperty("headline", "true"));
            otherPlayers = String.valueOf(prop.getProperty("otherPlayer", ""));
            high = Integer.parseInt(prop.getProperty("high", String.valueOf(high)));
            wight = Integer.parseInt(prop.getProperty("wight", String.valueOf(wight)));
        } catch (IOException e) {
            StatsClient.LOGGER.warn("Failed to read config!");
        }
        Config.serialize();
    }
}

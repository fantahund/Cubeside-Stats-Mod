package de.fanta.stats.client;

import de.fanta.stats.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class StatsClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Cubeside-Stats");

    public static final HashMap<String, String> StatsURLs = new HashMap<>();

    @Override
    public void onInitializeClient() {
        Config.deserialize();
        try {
            getStatsURLs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(StatsURLs);
        LOGGER.info("[Cubeside-Stats] Mod Loaded");
    }

    private void getStatsURLs() throws IOException {
        Document doc = Jsoup.connect("https://stats.cubeside.de").get();
        Element body = doc.body();
        Elements elements = body.getElementsByTag("h3");
        if (elements.isEmpty()) {
            return;
        }
        for (Element element : elements) {
            String url = element.getElementsByTag("a").attr("href");
            String title = element.text();
            synchronized (StatsURLs) {
                StatsURLs.put(title, "https://stats.cubeside.de" + url);
            }
        }
    }
}

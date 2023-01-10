package de.fanta.stats.client;

import de.cubeside.cubesidestatswebapi.CubesideStats;
import de.cubeside.cubesidestatswebapi.model.PlayerStatsProvider;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class StatsClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Cubeside-Stats");

    private static CubesideStats cubesideStats;

    public static Collection<PlayerStatsProvider> statsKeys = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        Config.deserialize();

        cubesideStats = new CubesideStats();
        getStatsKeys();

        LOGGER.info("[Cubeside-Stats] Mod Loaded");
    }

    private void getStatsKeys() {
        statsKeys = cubesideStats.getAllStatsKeys();
    }

    public static CubesideStats getCubesideStats() {
        return cubesideStats;
    }
}

package de.fanta.stats;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.cubeside.cubesidestatswebapi.model.PlayerStatsEntry;
import de.cubeside.cubesidestatswebapi.model.PlayerStatsProvider;
import de.fanta.stats.client.StatsClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ConfigMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            StatsClient.LOGGER.warn("Couldn't find Cloth Config, config menu disabled!");
            return parent -> null;
        }
        return new Builder()::build;
    }

    static class Builder {
        Screen build(Screen parent) {
            ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Text.of("Cubeside-Stats Config"));
            builder.setSavingRunnable(Config::serialize);
            builder.setTransparentBackground(true);
            ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();
            general.addEntry(entryBuilder.startBooleanToggle(Text.of("Stats anzeigen"), Config.showstats).setDefaultValue(true).setSaveConsumer(val -> Config.showstats = val).build());
            general.addEntry(entryBuilder.startStringDropdownMenu(Text.of("Statistiken"), Config.statsKeyID).setDefaultValue("Cubes").setSelections(StatsClient.statsKeys.stream().map(PlayerStatsProvider::getId).toList()).setSaveConsumer(statsKeyTitle -> Config.statsKeyID = statsKeyTitle).build()); //TODO SAVE STATS ID FROM STATS TITLE
            general.addEntry(entryBuilder.startIntField(Text.of("Plätze"), Config.places).setDefaultValue(3).setSaveConsumer(integer -> Config.places = integer).setMin(1).setMax(30).setTooltip(Text.of("1-30")).build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.of("Überschrift"), Config.headline).setDefaultValue(true).setSaveConsumer(val -> Config.headline = val).build());
            general.addEntry(entryBuilder.startStrField(Text.of("Andere Spieler (durch Komma trennen)"), Config.otherPlayers).setDefaultValue("").setSaveConsumer(val -> Config.otherPlayers = val).build());
            general.addEntry(entryBuilder.startIntField(Text.of("GUI Höhe"), Config.high).setDefaultValue(0).setSaveConsumer(integer -> Config.high = integer).build());
            general.addEntry(entryBuilder.startIntField(Text.of("GUI Breite"), Config.wight).setDefaultValue(0).setSaveConsumer(integer -> Config.wight = integer).build());
            return builder.build();
        }
    }
}

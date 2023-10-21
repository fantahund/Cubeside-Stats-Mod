package de.fanta.stats.client;

import com.mojang.blaze3d.platform.GlStateManager;
import de.cubeside.cubesidestatswebapi.model.PlayerStatsEntry;
import de.cubeside.cubesidestatswebapi.model.PlayerStatsProvider;
import de.fanta.stats.Config;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import org.apache.logging.log4j.Level;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GUI {
    private static MinecraftClient minecraft;
    private static boolean visible;
    private final TextRenderer fontRenderer;
    private static PlayerStatsEntry ownStatsEntry;
    private static String ownPlayerName;
    private static final HashMap<String, PlayerStatsEntry> otherStatsEntries = new HashMap<>();
    private static final HashMap<String, PlayerStatsEntry> positionStatsEntries = new HashMap<>();
    private static final HashMap<String, ItemStack> skullList = new HashMap<>();
    public static Thread updater;

    public GUI() {
        minecraft = MinecraftClient.getInstance();
        this.fontRenderer = minecraft.textRenderer;
        visible = true;
        updater = new Thread(() -> {
            while (true) {
                updateStats();
                try {
                    Thread.sleep(1000 * 30);
                } catch (InterruptedException e) {
                    StatsClient.LOGGER.log(Level.ERROR, "Sleep Error", e);
                }
            }

        });
        updater.start();
    }

    private static class RenderSize {
        int width;
        int height;

        RenderSize(int w, int h) {
            this.width = w;
            this.height = h;
        }
    }

    public void onRenderGameOverlayPost(DrawContext drawContext) {
        if (!visible || minecraft.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        if (Config.showstats && updater != null) {
            GlStateManager._clearColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderStats(drawContext);
        }
    }

    private void renderStats(DrawContext drawContext) {
        RenderSize result = new RenderSize(Config.wight, Config.high);

        int distance = 14;
        if (Config.headline) {
            PlayerStatsProvider provider = StatsClient.getStatsKey(Config.statsKeyID);
            drawContext.drawText(this.fontRenderer, "§l" + "----- " + (provider != null ? provider.getTitle() : Config.statsKeyID) + " -----", 5, (30 + result.height + 9 / 2), Color.white.getRGB(), true);
            result.height += distance;
        }

        Color[] colors = {new Color(226, 176, 7), new Color(138, 149, 151), new Color(167, 104, 75)};
        HashMap<PlayerStatsEntry, Integer> scorePosList = new HashMap<>();
        synchronized (positionStatsEntries) {
            for (PlayerStatsEntry statsEntry : positionStatsEntries.values()) {
                scorePosList.put(statsEntry, statsEntry.getPosition());
            }

            Set<Map.Entry<PlayerStatsEntry, Integer>> sorted = scorePosList.entrySet().stream().sorted((o2, o1) -> o1.getValue().compareTo(o2.getValue()) * -1).collect(Collectors.toCollection(LinkedHashSet::new));
            for (Map.Entry<PlayerStatsEntry, Integer> list : sorted) {
                PlayerStatsEntry statsEntry = list.getKey();
                String playerName = statsEntry.getName();
                String score = statsEntry.getValue();
                int position = statsEntry.getPosition();
                result.width = getWith(result.width, position + ". " + playerName + ": " + score);
                ItemStack itemStack = skullList.get(playerName);
                if (itemStack != null) {
                    drawContext.drawItem(skullList.get(playerName), 5, 30 + result.height);
                }
                Objects.requireNonNull(this.fontRenderer);
                if (position - 1 < 3) {
                    drawContext.drawText(this.fontRenderer, "§l" + position + ". " + playerName + ": " + score, (5 + 16 + 2), (30 + result.height + 9 / 2), colors[position - 1].getRGB(), true);
                } else {
                    drawContext.drawText(this.fontRenderer, "§l" + position + ". " + playerName + ": " + score, (5 + 16 + 2), (30 + result.height + 9 / 2), new Color(255, 255, 255).getRGB(), true);
                }
                result.height += distance;
            }
        }

        if (ownStatsEntry != null && !positionStatsEntries.containsKey(ownStatsEntry.getName())) {
            if (Config.headline) {
                drawContext.drawText(this.fontRenderer, "§l" + "----- " + "Deine Platzierung" + " -----", 5, (30 + result.height + 9 / 2), Color.white.getRGB(), true);
                result.height += distance;
            }

            ItemStack stack = skullList.get(ownPlayerName);
            if (stack != null) {
                drawContext.drawItem(stack, 5, 30 + result.height);
            }
            drawContext.drawText(this.fontRenderer, "§l" + ownStatsEntry.getDisplayPosition() + ". " + ownPlayerName + ": " + ownStatsEntry.getValue(), (5 + 16 + 2), (30 + result.height + 9 / 2), new Color(255, 255, 255).getRGB(), true);
            result.height += distance;
        }

        HashMap<PlayerStatsEntry, Integer> scoreList = new HashMap<>();
        synchronized (otherStatsEntries) {
            for (PlayerStatsEntry statsEntry : otherStatsEntries.values()) {
                if (!statsEntry.getName().equals(ownStatsEntry.getName()) && !positionStatsEntries.containsKey(statsEntry.getName())) {
                    scoreList.put(statsEntry, statsEntry.getPosition());
                }
            }

            Set<Map.Entry<PlayerStatsEntry, Integer>> sorted = scoreList.entrySet().stream().sorted((o2, o1) -> o1.getValue().compareTo(o2.getValue()) * -1).collect(Collectors.toCollection(LinkedHashSet::new));
            if (Config.headline) {
                drawContext.drawText(this.fontRenderer, "§l" + "----- " + "Andere Platzierung" + " -----", 5, (30 + result.height + 9 / 2), Color.white.getRGB(), true);
                result.height += distance;
            }
            boolean first = true;
            for (Map.Entry<PlayerStatsEntry, Integer> list : sorted) {
                PlayerStatsEntry statsEntry = list.getKey();
                if (statsEntry != null) {
                    if (!otherStatsEntries.isEmpty()) {
                        if (!first) {
                            result.height += distance;
                        }
                        first = false;
                        ItemStack stack = skullList.get(statsEntry.getName());
                        if (stack != null) {
                            drawContext.drawItem(stack, 5, 30 + result.height);
                        }
                        drawContext.drawText(this.fontRenderer, "§l" + statsEntry.getDisplayPosition() + ". " + statsEntry.getName() + ": " + statsEntry.getValue(), (5 + 16 + 2), (30 + result.height + 9 / 2), new Color(255, 255, 255).getRGB(), true);
                    }
                }
            }
        }

        if (result.width != 0) {
            result.width += 20;
        }
    }

    private int getWith(int resultWidth, String text) {
        int width = this.fontRenderer.getWidth(text);
        return Math.max(width, resultWidth);
    }

    public static void updateStats() {
        try {
            if (minecraft == null) {
                return;
            }
            // - Get OwnScoreplaces
            PlayerEntity player = minecraft.player;
            if (player == null) {
                return;
            }

            // - Get PositionScores
            Collection<PlayerStatsEntry> newPositionStatsEntries = new ArrayList<>(StatsClient.getCubesideStats().getStatsFromPositionRange(Config.statsKeyID, 0, Config.places));
            synchronized (skullList) {
                for (PlayerStatsEntry statsPlayer : newPositionStatsEntries) {
                    if (!skullList.containsKey(statsPlayer.getName())) {
                        skullList.put(statsPlayer.getName(), getCustomHead(statsPlayer.getName()));
                    }
                }
            }

            synchronized (positionStatsEntries) {
                positionStatsEntries.clear();
                for (PlayerStatsEntry entry : newPositionStatsEntries) {
                    System.out.println(entry.getPosition() + "(" + entry.getDisplayPosition() + ") ." + entry.getName());
                    positionStatsEntries.put(entry.getName(), entry);
                }
            }

            //Get OwnScore
            ownPlayerName = player.getName().getString();
            ownStatsEntry = StatsClient.getCubesideStats().getStatsFromPlayerOrUUID(Config.statsKeyID, ownPlayerName);

            synchronized (skullList) {
                if (!skullList.containsKey(ownPlayerName)) {
                    skullList.put(ownPlayerName, getCustomHead(ownPlayerName));
                }
            }

            // - Get OtherScores
            Collection<String> otherPlayers = new ArrayList<>();
            for (String otherPlayer : Config.otherPlayers.split(",")) {
                otherPlayers.add(otherPlayer.replace(" ", ""));
            }

            Collection<PlayerStatsEntry> newOtherPositionEntries = new ArrayList<>(StatsClient.getCubesideStats().getStatsFromPlayersOrUUIDs(Config.statsKeyID, otherPlayers));
            synchronized (skullList) {
                for (PlayerStatsEntry statsPlayer : newOtherPositionEntries) {
                    if (!skullList.containsKey(statsPlayer.getName())) {
                        skullList.put(statsPlayer.getName(), getCustomHead(statsPlayer.getName()));
                    }
                }
            }

            synchronized (otherStatsEntries) {
                otherStatsEntries.clear();
                for (PlayerStatsEntry entry : newOtherPositionEntries) {
                    otherStatsEntries.put(entry.getName(), entry);
                }
            }
        } catch (Exception e) {
            StatsClient.LOGGER.log(Level.ERROR, "Error while updating the stats", e);
        }
    }

    private static ItemStack getCustomHead(String playerName) {
        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
        NbtCompound compound = playerHead.getOrCreateNbt();
        compound.putString(SkullBlockEntity.SKULL_OWNER_KEY, playerName);
        SkullBlockEntity.fillSkullOwner(compound);
        return playerHead;
    }
}


package de.fanta.stats.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import de.cubeside.cubesidestatswebapi.model.PlayerStatsEntry;
import de.fanta.stats.Config;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
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
    private final MinecraftClient minecraft;
    private static boolean visible;
    private final TextRenderer fontRenderer;
    private final ItemRenderer itemRenderer;
    private static PlayerStatsEntry ownStatsEntry;
    private static String ownPlayerName;
    private static final Collection<PlayerStatsEntry> otherStatsEntries = new ArrayList<>();
    private static final Collection<PlayerStatsEntry> positionStatsEntries = new ArrayList<>();
    private final HashMap<String, ItemStack> skullList = new HashMap<>();
    public static Thread updater;

    public GUI() {
        this.minecraft = MinecraftClient.getInstance();
        this.fontRenderer = minecraft.textRenderer;
        this.itemRenderer = minecraft.getItemRenderer();
        visible = true;
        updater = new Thread(() -> {
            while (true) {
                try {
                    // - Get OwnScore
                    PlayerEntity player = minecraft.player;
                    if (player == null) {
                        return;
                    }

                    ownPlayerName = player.getName().getString();
                    ownStatsEntry = StatsClient.getCubesideStats().getStatsFromPlayerOrUUID(Config.statsKeyID, ownPlayerName);

                    synchronized (skullList) {
                        if (!skullList.containsKey(ownPlayerName)) {
                            ItemStack ownDisplayHead = new ItemStack(Items.PLAYER_HEAD);
                            GameProfile ownGameProfile = new GameProfile(null, ownPlayerName);
                            SkullBlockEntity.loadProperties(ownGameProfile, (profile) -> ownDisplayHead.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile)));
                            skullList.put(ownPlayerName, ownDisplayHead);
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
                                ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
                                GameProfile ownGameProfile = new GameProfile(null, statsPlayer.getName());
                                SkullBlockEntity.loadProperties(ownGameProfile, (profile) -> playerHead.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile)));
                                skullList.put(statsPlayer.getName(), playerHead);
                            }
                        }
                    }

                    synchronized (otherStatsEntries) {
                        otherStatsEntries.clear();
                        otherStatsEntries.addAll(newOtherPositionEntries);
                    }

                    // - Get PositionScores
                    Collection<PlayerStatsEntry> newPositionStatsEntries = new ArrayList<>(StatsClient.getCubesideStats().getStatsFromPositionRange(Config.statsKeyID, 0, Config.places - 1));

                    synchronized (skullList) {
                        for (PlayerStatsEntry statsPlayer : newPositionStatsEntries) {
                            if (!skullList.containsKey(statsPlayer.getName())) {
                                ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
                                GameProfile ownGameProfile = new GameProfile(null, statsPlayer.getName());
                                SkullBlockEntity.loadProperties(ownGameProfile, (profile) -> playerHead.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile)));
                                skullList.put(statsPlayer.getName(), playerHead);
                            }
                        }
                    }

                    synchronized (positionStatsEntries) {
                        positionStatsEntries.clear();
                        positionStatsEntries.addAll(newPositionStatsEntries);
                    }
                } catch (Exception e) {
                    StatsClient.LOGGER.log(Level.ERROR, "Error while updating the stats", e);
                }
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

    public void onRenderGameOverlayPost(MatrixStack stack) {
        if (!visible || minecraft.options.debugEnabled) {
            return;
        }
        if (Config.showstats) {
            GlStateManager._clearColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderStats(stack);
        }
    }

    private void renderStats(MatrixStack stack) {
        RenderSize result = new RenderSize(Config.wight, Config.high);

        int distance = 14;
        if (Config.headline) {
            this.fontRenderer.drawWithShadow(stack, "§l" + "----- " + Config.statsKeyID + " -----", 5, (30 + result.height + 9 / 2f), Color.white.getRGB());
            result.height += distance;
        }

        Color[] colors = {new Color(226, 176, 7), new Color(138, 149, 151), new Color(167, 104, 75)};
        synchronized (positionStatsEntries) {
            int i = 0;
            for (PlayerStatsEntry statsEntry : positionStatsEntries) {
                i++;
                String playerName = statsEntry.getName();
                String score = statsEntry.getValue();
                int position = statsEntry.getPosition();
                result.width = getWith(result.width, position + ". " + playerName + ": " + score);
                ItemStack itemStack = skullList.get(playerName);
                if (itemStack != null) {
                    this.itemRenderer.renderGuiItemIcon(skullList.get(playerName), 5, 30 + result.height);
                }
                Objects.requireNonNull(this.fontRenderer);
                if (i < 3) {
                    this.fontRenderer.drawWithShadow(stack, "§l" + position + ". " + playerName + ": " + score, (5 + 16 + 2), (30 + result.height + 9 / 2f), colors[i - 1].getRGB());
                } else {
                    this.fontRenderer.drawWithShadow(stack, "§l" + position + ". " + playerName + ": " + score, (5 + 16 + 2), (30 + result.height + 9 / 2f), new Color(255, 255, 255).getRGB());
                }
                result.height += distance;
            }
        }

        if (ownStatsEntry != null) {
            if (Config.headline) {
                this.fontRenderer.drawWithShadow(stack, "§l" + "----- " + "Deine Platzierung" + " -----", 5, (30 + result.height + 9 / 2f), Color.white.getRGB());
                result.height += distance;
            }

            this.itemRenderer.renderGuiItemIcon(skullList.get(ownPlayerName), 5, 30 + result.height);
            this.fontRenderer.drawWithShadow(stack, "§l" + ownStatsEntry.getDisplayPosition() + " " + ownPlayerName + ": " + ownStatsEntry.getValue(), (5 + 16 + 2), (30 + result.height + 9 / 2f), new Color(255, 255, 255).getRGB());
            result.height += distance;
        }


        HashMap<PlayerStatsEntry, Integer> scoreList = new HashMap<>();
        synchronized (otherStatsEntries) {
            for (PlayerStatsEntry statsEntry : otherStatsEntries) {
                scoreList.put(statsEntry, statsEntry.getDisplayPosition());
            }

            Set<Map.Entry<PlayerStatsEntry, Integer>> sorted = scoreList.entrySet().stream().sorted((o2, o1) -> o1.getValue().compareTo(o2.getValue()) * -1).collect(Collectors.toCollection(LinkedHashSet::new));

            if (Config.headline) {
                this.fontRenderer.drawWithShadow(stack, "§l" + "----- " + "Andere Platzierung" + " -----", 5, (30 + result.height + 9 / 2f), Color.white.getRGB());
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
                        this.itemRenderer.renderGuiItemIcon(skullList.get(statsEntry.getName()), 5, 30 + result.height);
                        this.fontRenderer.drawWithShadow(stack, "§l" + statsEntry.getDisplayPosition() + " " + statsEntry.getName() + ": " + statsEntry.getValue(), (5 + 16 + 2), (30 + result.height + 9 / 2f), new Color(255, 255, 255).getRGB());
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
}


package de.fanta.stats.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GUI {
    private final MinecraftClient minecraft;

    private static boolean visible;

    private final TextRenderer fontRenderer;

    private final ItemRenderer itemRenderer;

    public final List<Object> scores = new ArrayList<>();

    public static String ownPos;
    public static String ownPlayerName;
    public static String ownScore;
    public static ItemStack ownStack;

    public final HashMap<String, ItemStack> skull = new HashMap<>();

    public GUI() {
        this.minecraft = MinecraftClient.getInstance();
        this.fontRenderer = minecraft.textRenderer;
        this.itemRenderer = minecraft.getItemRenderer();
        visible = true;
        Thread updater = new Thread(() -> {
            try {
                while (true) {
                    // - Get OwnScore
                    PlayerEntity player = minecraft.player;
                    if (player == null) {
                        return;
                    }

                    ownPlayerName = player.getName().asString();

                    String connString = StatsClient.StatsURLs.get(Config.statsurl) + "/desc/1?player=" + ownPlayerName;
                    Document own = Jsoup.connect(connString).get();
                    String classname = "searched-row";
                    Element sel = own.getElementsByClass(classname).first();
                    if (sel != null) {
                        ownPos = sel.getElementsByTag("td").first().text();

                        Elements tmp = sel.getElementsByClass("align-right");
                        for (Element el : tmp) {
                            if (!el.hasClass("hideable")) {
                                ownScore = el.text();
                            }
                        }

                        ownStack = new ItemStack(Items.PLAYER_HEAD);
                        GameProfile ownGameProfile = new GameProfile(null, ownPlayerName);
                        SkullBlockEntity.loadProperties(ownGameProfile, (profile) -> ownStack.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile)));
                    }



                    // Dep
                    Document doc = Jsoup.connect(StatsClient.StatsURLs.get(Config.statsurl)).get();
                    Element body = doc.body();
                    Elements elements = body.getElementsByTag("table");
                    if (!elements.isEmpty()) {
                        Element table = elements.first();
                        Elements tableRows = table.getElementsByTag("tbody").first().getElementsByTag("tr");
                        if (tableRows.size() > 1) {
                            List<Object> scores = new ArrayList<>();
                            int i = 1;
                            while (i < Config.places + 1 && i < tableRows.size()) {
                                Element row = tableRows.get(i);
                                Element href = row.getElementsByTag("a").first();
                                Elements ss = row.getElementsByTag("td");
                                int ii = 0;
                                while (ii < ss.size()) {
                                    Element element = ss.get(ii);
                                    if (element.hasClass("align-right") & !element.hasClass("hideable")) {
                                        String[] values = new String[2];
                                        values[0] = href.text();
                                        values[1] = element.text();
                                        scores.add(values);
                                        break;
                                    }
                                    ii++;
                                }
                                synchronized (this.skull) {
                                    if (!skull.containsKey(href.toString())) {
                                        ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
                                        GameProfile gameProfile = new GameProfile(null, href.text());
                                        SkullBlockEntity.loadProperties(gameProfile, (profile) -> {
                                            itemStack.getOrCreateNbt().put("SkullOwner", NbtHelper.writeGameProfile(new NbtCompound(), profile));
                                            skull.put(href.text(), itemStack);
                                        });
                                    }
                                }
                                i++;
                            }

                            synchronized (this.scores) {
                                this.scores.clear();
                                this.scores.addAll(scores);
                            }
                        }
                    }
                    Thread.sleep(1000 * 30);
                }
            } catch (InterruptedException | java.io.IOException e) {
                e.printStackTrace();
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
        RenderSize result = new RenderSize(0, 0);
        Color[] colors = {new Color(226, 176, 7), new Color(138, 149, 151), new Color(167, 104, 75)};
        synchronized (this.scores) {
            if (Config.headline) {
                this.fontRenderer.drawWithShadow(stack, "§l" + "----- " + Config.statsurl + " -----", 5, (30 + result.height + 9 / 2f), Color.white.getRGB());
                result.height += 15;
            }
            for (int i = 0; i < this.scores.size(); i++) {
                String playerName = ((String[]) this.scores.get(i))[0];
                String score = ((String[]) this.scores.get(i))[1];
                result.width = getWith(result.width, (i + 1) + ". " + playerName + ": " + score);
                ItemStack itemStack = skull.get(playerName);
                if (itemStack != null) {
                    this.itemRenderer.renderGuiItemIcon(skull.get(playerName), 5, 30 + result.height);
                }
                Objects.requireNonNull(this.fontRenderer);
                if (i < 3) {
                    this.fontRenderer.drawWithShadow(stack, "§l" + (i + 1) + ". " + playerName + ": " + score, (5 + 16 + 2), (30 + result.height + 9 / 2f), colors[i].getRGB());
                } else {
                    this.fontRenderer.drawWithShadow(stack, "§l" + (i + 1) + ". " + playerName + ": " + score, (5 + 16 + 2), (30 + result.height + 9 / 2f), new Color(255, 255, 255).getRGB());
                }
                result.height += 15;
            }
            if (ownScore != null && ownPos != null && ownStack != null && ownPlayerName != null) {
                boolean showOwnCore = true;
                for (Object score : this.scores) {
                    String playerName = ((String[]) score)[0];
                    if (playerName.contains(ownPlayerName)) {
                        showOwnCore = false;
                        break;
                    }
                }

                if (showOwnCore) {
                    this.fontRenderer.drawWithShadow(stack, "§l" + "----- " + "Deine Platzierung" + " -----", 5, (30 + result.height + 9 / 2f), Color.white.getRGB());
                    result.height += 15;
                    this.itemRenderer.renderGuiItemIcon(ownStack, 5, 30 + result.height);
                    this.fontRenderer.drawWithShadow(stack, "§l" + ownPos + " " + ownPlayerName + ": " + ownScore, (5 + 16 + 2), (30 + result.height + 9 / 2f), new Color(255, 255, 255).getRGB());
                }

            }

        }
        if (result.width != 0)
            result.width += 20;
    }

    private int getWith(int resultWidth, String text) {
        int width = this.fontRenderer.getWidth(text);
        return Math.max(width, resultWidth);
    }
}


package melhorias.client;

import melhorias.MelhoriasConfig;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Interface de pesca desenhada no HUD (não é uma tela): não captura mouse/teclado
 * e não bloqueia a movimentação. O item já foi sorteado pelo jogo; aqui só se apresenta o resultado real.
 */
public final class FishingOverlay {
    private static final Deque<ItemStack> QUEUE = new ArrayDeque<>();
    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final Random RNG = new Random();
    private static final Set<Item> TREASURES = Set.of(Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL,
            Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT, Items.ENCHANTED_GOLDEN_APPLE);

    private static ItemStack current;
    private static long startMs, lastMs;
    private static boolean revealStarted;

    private static final class Spark {
        float x, y, vx, vy; int life, age, color, size;
    }

    public static void enqueue(ItemStack stack) {
        if (!MelhoriasConfig.get.fishingUiEnabled || stack.isEmpty()) return;
        if (QUEUE.size() < 8) QUEUE.add(stack.copy());
    }

    /** 0 = comum (discreto), 1 = incomum, 2 = raro/tesouro (efeitos especiais). */
    static int tier(ItemStack s) {
        Rarity r = s.getRarity();
        if (r == Rarity.RARE || r == Rarity.EPIC || s.isEnchanted() || TREASURES.contains(s.getItem())) return 2;
        return r == Rarity.UNCOMMON ? 1 : 0;
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        long now = Util.getMillis();
        if (current == null) {
            current = QUEUE.poll();
            if (current == null) return;
            startMs = now; lastMs = now; revealStarted = false; SPARKS.clear();
        }
        MelhoriasConfig.Data cfg = MelhoriasConfig.get;
        float dt = Math.min(0.1F, (now - lastMs) / 1000F);
        lastMs = now;

        long fill = (long) (cfg.fishingFillSeconds * 1000);
        long flash = 400, show = (long) (cfg.fishingShowSeconds * 1000), fade = 400;
        long t = now - startMs;
        if (t > fill + flash + show + fade) { current = null; return; }

        Font font = mc.font;
        int cx = g.guiWidth() / 2;
        int top = (int) (g.guiHeight() * 0.16F);   // parte alta da tela: não cobre a mira nem a hotbar

        if (t < fill) {
            drawFilling(g, font, cx, top, t / (float) fill, t);
        } else {
            drawReveal(g, font, cx, top, t - fill, flash, show, fade, dt);
        }
    }

    // ---------- fase 1: barra com suspense ----------
    private static float progress(float x) {
        if (x < 0.60F) { float u = x / 0.60F; return 0.70F * (1 - (1 - u) * (1 - u)); }   // sobe rápido
        if (x < 0.80F) return 0.70F + 0.03F * ((x - 0.60F) / 0.20F);                      // quase para (suspense)
        float u = (x - 0.80F) / 0.20F; return 0.73F + 0.27F * u * u;                       // arranque final
    }

    private static void drawFilling(GuiGraphics g, Font font, int cx, int top, float x, long t) {
        float p = progress(Mth.clamp(x, 0, 1));
        int dots = 1 + (int) ((t / 350) % 3);
        Component title = Component.translatable("gui.melhorias.fishing").append(".".repeat(dots));
        g.drawCenteredString(font, title, cx, top, 0xFFFFFFFF);

        int boxX = cx - 95, boxY = top + 14, boxW = 22;
        int barX = cx - 65, barY = top + 21, barW = 160, barH = 8;

        // caixa do "?" (treme mais perto do fim)
        g.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxW + 1, 0xFF111111);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxW, 0xFF2B3A4A);
        float shake = p > 0.7F ? 1.5F : 0.5F;
        int ox = (int) (Math.sin(t / 55.0) * shake), oy = (int) (Math.cos(t / 70.0) * shake);
        g.pose().pushPose();
        g.pose().translate(boxX + boxW / 2F + ox, boxY + 4 + oy, 0);
        g.pose().scale(1.6F, 1.6F, 1);
        g.drawCenteredString(font, "?", 0, 0, 0xFFFFE680);
        g.pose().popPose();

        // barra
        boolean tense = x > 0.75F;
        int border = tense && ((t / 120) % 2 == 0) ? 0xFFFFD24A : 0xFF111111;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, border);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1C1C1C);
        int fillW = (int) (barW * p);
        int col = lerpColor(0xFF3FA7FF, 0xFFFFC83D, p);
        g.fill(barX, barY, barX + fillW, barY + barH, col);
        g.fill(barX, barY, barX + fillW, barY + 2, 0x55FFFFFF);          // brilho
    }

    // ---------- fase 2: revelação ----------
    private static void drawReveal(GuiGraphics g, Font font, int cx, int top, long r, long flash, long show,
                                   long fade, float dt) {
        MelhoriasConfig.Data cfg = MelhoriasConfig.get;
        int tier = tier(current);
        int rgb = tier == 0 ? 0xFFFFFF : nameColor(current);
        int iconY = top + 26;

        if (!revealStarted) {
            revealStarted = true;
            Minecraft mc = Minecraft.getInstance();
            if (cfg.fishingSounds) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(
                        tier == 2 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.EXPERIENCE_ORB_PICKUP,
                        tier == 2 ? 1.0F : 1.2F));
            }
            if (cfg.fishingParticles) spawnSparks(cx, iconY, tier, rgb);
        }

        float fadeK = r > flash + show ? Mth.clamp((r - flash - show) / (float) fade, 0, 1) : 0;
        int textA = Math.max(8, (int) (255 * (1 - fadeK)));

        g.drawCenteredString(font, Component.translatable("gui.melhorias.fished"), cx, top,
                (textA << 24) | 0xCCCCCC);

        // flash de revelação (apenas em volta do ícone, nunca na tela toda)
        if (r < flash) {
            int a = (int) (170 * (1 - r / (float) flash)) * (tier == 0 ? 1 : 2) / (tier == 0 ? 2 : 1);
            a = Mth.clamp(a, 0, 200);
            int rad = tier == 0 ? 22 : 34;
            g.fill(cx - rad, iconY + 16 - rad, cx + rad, iconY + 16 + rad, (a << 24) | 0xFFFFFF);
        }

        // ícone: aparece com "pop" (overshoot) e some encolhendo
        float pop = backOut(Mth.clamp(r / 450F, 0, 1));
        float scale = 2.0F * pop * (1 - 0.35F * fadeK);
        if (scale > 0.05F) {
            g.pose().pushPose();
            g.pose().translate(cx, iconY + 16, 100);
            g.pose().scale(scale, scale, 1);
            g.renderItem(current, -8, -8);
            g.pose().popPose();
        }

        // nome do item (aparece depois do ícone)
        if (r > 250) {
            int a = Math.max(8, (int) (textA * Mth.clamp((r - 250) / 250F, 0, 1)));
            Component name = current.getHoverName();
            g.pose().pushPose();
            g.pose().translate(cx, iconY + 40, 0);
            g.pose().scale(1.3F, 1.3F, 1);
            g.drawCenteredString(font, name.getString().toUpperCase(), 0, 0, (a << 24) | rgb);
            g.pose().popPose();
        }

        updateAndDrawSparks(g, dt);
    }

    // ---------- partículas ----------
    private static void spawnSparks(int cx, int cy, int tier, int rgb) {
        int n = tier == 0 ? 6 : tier == 1 ? 20 : 56;
        for (int i = 0; i < n; i++) {
            Spark s = new Spark();
            double ang = RNG.nextDouble() * Math.PI * 2;
            float sp = (tier == 0 ? 25 : 60) + RNG.nextFloat() * (tier == 2 ? 130 : 70);
            s.x = cx; s.y = cy + 16;
            s.vx = (float) Math.cos(ang) * sp; s.vy = (float) Math.sin(ang) * sp - 20;
            s.life = 500 + RNG.nextInt(500);
            s.size = tier == 0 ? 1 : 1 + RNG.nextInt(2);
            s.color = tier == 0 ? 0xFFFFFF : (RNG.nextInt(3) == 0 ? 0xFFFFFF : (tier == 2 && RNG.nextBoolean() ? 0xFFD24A : rgb));
            SPARKS.add(s);
        }
    }

    private static void updateAndDrawSparks(GuiGraphics g, float dt) {
        for (int i = SPARKS.size() - 1; i >= 0; i--) {
            Spark s = SPARKS.get(i);
            s.age += (int) (dt * 1000);
            if (s.age >= s.life) { SPARKS.remove(i); continue; }
            s.vy += 120 * dt;                // gravidade leve
            s.x += s.vx * dt; s.y += s.vy * dt;
            int a = Math.max(8, (int) (255 * (1 - s.age / (float) s.life)));
            g.fill((int) s.x, (int) s.y, (int) s.x + s.size, (int) s.y + s.size, (a << 24) | s.color);
        }
    }

    // ---------- utilidades ----------
    private static int nameColor(ItemStack s) {
        Integer c = s.getRarity().color().getColor();
        return c == null ? 0xFFFFFF : c;
    }

    private static float backOut(float x) {
        float c1 = 1.70158F, c3 = c1 + 1;
        float u = x - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    private static int lerpColor(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int gr = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    private FishingOverlay() {}
}

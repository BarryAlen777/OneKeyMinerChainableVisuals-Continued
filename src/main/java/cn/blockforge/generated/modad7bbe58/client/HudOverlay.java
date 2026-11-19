package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.Config;
import cn.blockforge.generated.modad7bbe58.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class HudOverlay {
    /** Last rendered label, cached per (count, shape, mode) - rebuilding a translatable
     *  Component 60 times a second only feeds the young-gen GC for zero visible change. */
    private static String cachedKey;
    private static Component cachedText;

    private HudOverlay() { }

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        // Compare overlays by id like the main mod does; object identity against
        // VanillaGuiOverlay.CROSSHAIR.type() only works as long as Forge happens to cache it.
        if (!Config.SHOW_HUD.get()
                || !event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.type().id())) return;
        // One box per preview block; the number quoted excludes the block under the crosshair,
        // matching the main mod's own panel so both read the same value.
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || ChainScanner.positions(mc).size() < 2) return;
        int count = ChainScanner.chainedBlocks();
        if (count < 1) return;
        int x = event.getWindow().getGuiScaledWidth() / 2 + 10;
        int y = event.getWindow().getGuiScaledHeight() / 2 + 8;
        boolean interaction = ChainScanner.interactionMode();
        // Anything not computed by the main mod itself is an estimate (local same-block
        // flood fill / the block-name heuristic for interaction targets) - the number must
        // say so instead of looking as exact as the real chain list (audit P2).
        boolean estimated = !ChainScanner.mainModDriven();
        String shapeKey = ChainScanner.mainModDriven() ? ChainScanner.shapeKey() : null;
        // The language code is part of the key so switching languages in the options
        // invalidates the cached string immediately (audit P3: stale text until the count
        // happened to change).
        String key = count + "|" + shapeKey + "|" + interaction + "|" + estimated
                + "|" + mc.options.languageCode;
        Component text;
        if (!key.equals(cachedKey)) {
            if (shapeKey != null) {
                text = Component.translatable("hud.onekeyminer_chainable_visuals.chain_count_shape",
                        count, Component.translatable(shapeKey));
            } else if (estimated) {
                text = Component.translatable("hud.onekeyminer_chainable_visuals.chain_count_estimate", count);
            } else {
                text = Component.translatable("hud.onekeyminer_chainable_visuals.chain_count", count);
            }
            cachedKey = key;
            cachedText = text;
        } else {
            text = cachedText;
        }
        int width = mc.font.width(text);
        int bgAlpha = 0x99000000;
        event.getGuiGraphics().fill(x - 3, y - 2, x + width + 3, y + 11, bgAlpha);
        event.getGuiGraphics().drawString(mc.font, text, x, y, interaction ? 0xFF80D8FF : 0xFFB5FFC8, true);
    }
}

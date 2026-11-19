package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private static boolean startupNoticeShown;

    private ClientSetup() { }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ConfigScreen(parent))));
        boolean bridge = ChainPositionBridge.previewApiAttached();
        GeneratedMod.LOGGER.info("OneKeyMiner integration: {}", bridge
                ? "main mod preview bridge attached" : "main mod absent, using local scanner");
        // The mods.toml dependency stays [0,) so an old main mod never blocks loading - but the
        // preview API only exists since 1.6.8, so say plainly that the highlight we show is
        // the local estimate, not the real chain range. Silent "looks fine but wrong" was the
        // failure mode the review called out (audit report issue 8); the log line alone was
        // invisible in-game, so notifyOnce() repeats it in chat when the world loads.
        if (!bridge && ModList.get().isLoaded("onekeyminer")) {
            GeneratedMod.LOGGER.warn("[连锁可视化续作] 检测到 OneKeyMiner，但未能接入其连锁预览 API"
                    + "（该 API 需要主模组 1.6.8 或更新版本）。当前高亮为本地预判，范围可能与真实连锁不一致，建议升级主模组。");
        }
    }

    /**
     * Once per session, as soon as the player exists, show the "main mod too old" notice in
     * chat. Called from the client tick; the boolean short-circuit makes it free afterwards
     * (audit report issue 8: the warning used to live only in the log).
     */
    public static void notifyOnce(Minecraft mc) {
        if (startupNoticeShown || mc.player == null) return;
        startupNoticeShown = true;
        if (!ChainPositionBridge.previewApiAttached() && ModList.get().isLoaded("onekeyminer")) {
            mc.player.sendSystemMessage(
                    Component.translatable("msg.onekeyminer_chainable_visuals.main_mod_outdated"));
        }
    }

    /** Keep a handle on the loaded client config so the one-off default migration can persist. */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            GeneratedMod.CLIENT_CONFIG = event.getConfig();
        }
    }
}

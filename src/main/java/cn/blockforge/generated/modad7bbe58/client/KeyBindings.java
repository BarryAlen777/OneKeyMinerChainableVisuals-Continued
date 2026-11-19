package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.GeneratedMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {
    // r11: this used to default to LEFT_SHIFT - the vanilla sneak key. Sneak-mining (the most
    // common mining posture!) triggered the interaction preview and hid the real mining outline.
    // The mapping id also changed ("..._v2"): Minecraft pins saved keybinds by id, so an old
    // options.txt that stored Shift for the previous id cannot pin the new default.
    public static final KeyMapping SHOW_INTERACTION_KEY = new KeyMapping(
            "key.onekeyminer_chainable_visuals.show_interaction_v2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            "key.categories.onekeyminer_chainable_visuals");
    // The mapping id is "open_settings", not "open_config": Minecraft persists keybinds by
    // their id, so an old options.txt that stored O for the previous id would pin the new
    // default key and never pick it up. A fresh id starts clean with the N default.
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.onekeyminer_chainable_visuals.open_settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,
            "key.categories.onekeyminer_chainable_visuals");

    private static KeyMapping[] inspectedMappings;
    private static KeyMapping chainKey;

    private KeyBindings() { }

    static boolean chainHeld(Minecraft mc) {
        if (mc.player == null || mc.screen != null || mc.isPaused()) return false;
        // Use the registered mapping, including any rebind, without loading OneKeyMiner classes.
        KeyMapping[] mappings = mc.options.keyMappings;
        if (inspectedMappings != mappings) {
            inspectedMappings = mappings;
            chainKey = null;
            for (KeyMapping mapping : mappings) {
                if ("key.onekeyminer.hold".equals(mapping.getName())) {
                    chainKey = mapping;
                    break;
                }
            }
        }
        return chainKey != null && chainKey.isDown();
    }

    static boolean interactionHeld(Minecraft mc) {
        return mc.screen == null && !mc.isPaused() && SHOW_INTERACTION_KEY.isDown();
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SHOW_INTERACTION_KEY);
        event.register(OPEN_CONFIG_KEY);
    }
}

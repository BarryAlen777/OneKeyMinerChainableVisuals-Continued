package cn.blockforge.generated.modad7bbe58;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "onekeyminer_chainable_visuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("chain_visualizer");
    /** Client config handle, kept so the one-off default migration can persist changes. */
    public static volatile ModConfig CLIENT_CONFIG;

    public GeneratedMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}

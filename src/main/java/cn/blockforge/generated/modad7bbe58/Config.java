package cn.blockforge.generated.modad7bbe58;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only visualization settings. */
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /** ARGB defaults, kept as constants so the settings screen can offer "reset to default". */
    public static final int DEFAULT_COLOR_MINING = 0xFFFFFFFF;
    public static final int DEFAULT_COLOR_INTERACTION = 0xD8D8F4FF;
    public static final int DEFAULT_COLOR_SHEEP = 0xF0F0F0FF;

    public static final ForgeConfigSpec.BooleanValue SHOW_MINING;
    public static final ForgeConfigSpec.BooleanValue FOLLOW_MAIN_MOD;
    public static final ForgeConfigSpec.BooleanValue SHOW_INTERACTION;
    public static final ForgeConfigSpec.BooleanValue MERGED_OUTLINE;
    public static final ForgeConfigSpec.BooleanValue OUTLINE_MODE;
    public static final ForgeConfigSpec.BooleanValue SHOW_THROUGH_WALLS;
    public static final ForgeConfigSpec.DoubleValue LINE_ALPHA;
    public static final ForgeConfigSpec.IntValue MAX_CHAIN_BLOCKS;
    public static final ForgeConfigSpec.IntValue MAX_CHAIN_DEPTH;
    public static final ForgeConfigSpec.BooleanValue DIAGONAL_CHAINING;
    public static final ForgeConfigSpec.BooleanValue SHOW_PARTICLES;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_ALPHA;
    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    public static final ForgeConfigSpec.BooleanValue PLAY_CHAIN_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec.BooleanValue SHOW_SHEEP;
    public static final ForgeConfigSpec.IntValue SHEEP_RANGE;
    public static final ForgeConfigSpec.IntValue COLOR_MINING;
    public static final ForgeConfigSpec.IntValue COLOR_INTERACTION;
    public static final ForgeConfigSpec.IntValue COLOR_SHEEP;
    public static final ForgeConfigSpec.BooleanValue CUSTOM_RENDER_PIPELINE;
    public static final ForgeConfigSpec.IntValue REFRESH_TICKS;
    public static final ForgeConfigSpec.IntValue DEFAULTS_REVISION;
    public static final ForgeConfigSpec SPEC;

    private Config() { }

    static {
        BUILDER.push("highlight");
        SHOW_MINING = BUILDER.comment("Show connected matching blocks when the crosshair targets a block.")
                .define("showMining", true);
        FOLLOW_MAIN_MOD = BUILDER.comment("Follow OneKeyMiner's own chain preview while the main mod is installed.")
                .define("followMainMod", true);
        SHOW_INTERACTION = BUILDER.comment("Show interaction targets while holding the interaction preview key.")
                .define("showInteraction", true);
        MERGED_OUTLINE = BUILDER.comment("Draw one clean outline around the whole chain group instead of a wireframe per block.")
                .define("mergedOutline", true);
        OUTLINE_MODE = BUILDER.comment("Draw an extra bright halo around the preview edges (off by default: FTB-Ultimine-style clean white outline).").define("outlineMode", false);
        SHOW_THROUGH_WALLS = BUILDER.comment("Draw outlines through solid blocks.").define("showThroughWalls", false);
        LINE_ALPHA = BUILDER.comment("Outline opacity multiplier (0.0 fully transparent, 1.0 fully opaque).").defineInRange("lineAlpha", 1.0D, 0.0D, 1.0D);
        MAX_CHAIN_BLOCKS = BUILDER.comment("Maximum number of blocks included in the local preview."
                        + " Ignored while following OneKeyMiner's own preview - that list already obeys the main mod's limit.")
                .defineInRange("maxChainBlocks", 64, 2, 4096);
        MAX_CHAIN_DEPTH = BUILDER.comment("Maximum distance from the targeted block.").defineInRange("maxChainDepth", 16, 1, 64);
        DIAGONAL_CHAINING = BUILDER.comment("Include diagonal neighbors in the preview search.").define("diagonalChaining", false);
        BUILDER.pop();

        BUILDER.push("particles");
        SHOW_PARTICLES = BUILDER.comment("Show soft white particle traces from the target to preview blocks (off by default).").define("showParticles", false);
        PARTICLE_ALPHA = BUILDER.comment("Particle trace density multiplier.").defineInRange("particleDensity", 0.65D, 0.1D, 1.0D);
        BUILDER.pop();

        BUILDER.push("hud");
        SHOW_HUD = BUILDER.comment("Show the preview block count beside the crosshair.").define("showHud", true);
        BUILDER.pop();

        BUILDER.push("sound");
        PLAY_CHAIN_SOUNDS = BUILDER.comment("Play a short rising note sequence when the targeted block is broken (off by default).").define("playChainSounds", false);
        SOUND_VOLUME = BUILDER.comment("Volume multiplier for the preview sound sequence.").defineInRange("soundVolume", 0.55D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("sheep");
        SHOW_SHEEP = BUILDER.comment("Highlight nearby shearable sheep while holding the OneKeyMiner key and aiming at a shearable sheep with shears.").define("showSheep", true);
        SHEEP_RANGE = BUILDER.comment("Maximum distance used for sheep preview.").defineInRange("sheepRange", 24, 4, 64);
        BUILDER.pop();

        BUILDER.push("colors");
        COLOR_MINING = BUILDER.comment("ARGB tint of the mining chain highlight (default: white, like FTB Ultimine).")
                .defineInRange("colorMining", DEFAULT_COLOR_MINING, Integer.MIN_VALUE, Integer.MAX_VALUE);
        COLOR_INTERACTION = BUILDER.comment("ARGB tint of the interaction chain highlight (default: near-white cyan).")
                .defineInRange("colorInteraction", DEFAULT_COLOR_INTERACTION, Integer.MIN_VALUE, Integer.MAX_VALUE);
        COLOR_SHEEP = BUILDER.comment("ARGB tint of the shearable-sheep highlight (default: soft white lilac).")
                .defineInRange("colorSheep", DEFAULT_COLOR_SHEEP, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("advanced");
        CUSTOM_RENDER_PIPELINE = BUILDER.comment("Use this mod's own line render layers. Turn off if lines conflict with another mod's renderer.")
                .define("customRenderPipeline", true);
        REFRESH_TICKS = BUILDER.comment("Client ticks between preview recalculations.").defineInRange("refreshTicks", 4, 1, 20);
        // Internal marker, do not edit. Old installs (which still carry the r6-era defaults
        // saved into their TOML) are migrated once to the FTB-style defaults on first tick;
        // the default value 1 means "not yet migrated", the mod writes 2 after migrating.
        DEFAULTS_REVISION = BUILDER.comment("Internal migration marker, do not edit.").defineInRange("defaultsRevision", 1, 0, 99);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

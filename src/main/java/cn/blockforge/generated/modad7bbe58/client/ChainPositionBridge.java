package cn.blockforge.generated.modad7bbe58.client;

import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Compile-time-independent bridge to the OneKeyMiner (org.xiyu:onekeyminer) client API.
 *
 * <p>OneKeyMiner 1.6.8+ exposes {@code OneKeyMinerAPI.getPreviewBlocks()} which returns the
 * positions its own {@code ChainPreviewManager} computed from the player's selected chain
 * shape, throttled client ticks and server-synced preview policy. Using it directly means
 * our highlight matches exactly what the main mod would actually mine (including tunnel
 * shapes and diagonal rules), so we no longer need to guess anything.</p>
 *
 * <p>The live list is handed out as-is. Legacy snapshots can omit the origin.
 * {@link ChainScanner} associates fresh publications with their client tick's target
 * before adding the origin for display.</p>
 *
 * <p>All lookups are reflective so this addon stays loadable without the main mod. Three
 * states are distinguished:</p>
 * <ul>
 *   <li>main mod absent - {@link #isMainModPresent()} is false, callers use the local scan;</li>
 *   <li>main mod present, chain key held - {@link #readMiningPreview()} returns the real
 *       target list;</li>
 *   <li>main mod present, nothing previewed - an empty list, meaning "show nothing",
 *       NOT a fallback to the local scanner.</li>
 * </ul>
 *
 * <p>{@link #policy()} additionally mirrors the main mod's effective chain limits (max block
 * count, max distance, diagonal flag - server-synced when the server provides them), so the
 * addon automatically matches whatever the player configures in the main mod.</p>
 */
final class ChainPositionBridge {
    private static final String API_CLASS = "org.xiyu.onekeyminer.api.OneKeyMinerAPI";
    private static final String PREVIEW_CLASS = "org.xiyu.onekeyminer.preview.ChainPreviewManager";
    private static final String CONFIG_MANAGER_CLASS = "org.xiyu.onekeyminer.config.ConfigManager";
    private static final String MINER_CONFIG_CLASS = "org.xiyu.onekeyminer.config.MinerConfig";
    private static final String PREF_SESSION_CLASS = "org.xiyu.onekeyminer.network.ClientPreferenceSession";

    private static final boolean PRESENT;
    private static final Method GET_PREVIEW_BLOCKS;
    private static final Method PREVIEW_INSTANCE;
    private static final Method GET_SHAPE_KEY;

    private static final Method GET_CONFIG;
    private static final Method RESOLVE_POLICY;
    private static final Method POLICY_MAX_BLOCKS;
    private static final Method POLICY_MAX_DISTANCE;
    private static final Method POLICY_ALLOW_DIAGONAL;
    private static final Method POLICY_SERVER_ACK;
    private static final Field FIELD_MAX_BLOCKS;
    private static final Field FIELD_MAX_DISTANCE;
    private static final Field FIELD_ALLOW_DIAGONAL;

    /** Policy cache: the main mod's numbers change at most when its config syncs, 1 s is plenty. */
    private static final long POLICY_CACHE_MS = 1000L;
    private static volatile MainPolicy cachedPolicy;
    private static volatile long cachedPolicyAt;

    /**
     * Transient degradation state. A single failed reflective call (mid world unload, main mod
     * reloading its config) must not black out the bridge for the whole session, so only
     * <em>sustained</em> failure demotes us to the local scanner: at least
     * {@link #FAILURE_LIMIT} consecutive reads AND the streak must span at least
     * {@link #FAILURE_WINDOW_MS} (audit report issue 5 - r12 demoted after 3 failures ≈
     * 150 ms, which a config reload or dimension switch can hit in passing; at ~20 tps the
     * new rule needs the API to stay broken for 2 whole seconds). Demotion is re-tried when
     * the client level changes. A {@link LinkageError} is permanent by nature.
     */
    private static final int FAILURE_LIMIT = 20;
    private static final long FAILURE_WINDOW_MS = 2000L;
    private static int consecutiveFailures;
    private static long firstFailureAt;
    private static boolean broken;
    private static boolean brokenPermanent;
    private static Object brokenLevel;

    static {
        Method previewBlocks = null;
        Method instance = null;
        Method shapeKey = null;
        boolean found = false;
        ClassLoader cl = ChainPositionBridge.class.getClassLoader();
        try {
            Class<?> api = Class.forName(API_CLASS, false, cl);
            previewBlocks = api.getMethod("getPreviewBlocks");
            found = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Main mod not installed: stay standalone.
        }
        if (found) {
            try {
                Class<?> manager = Class.forName(PREVIEW_CLASS, false, cl);
                instance = manager.getMethod("getInstance");
                shapeKey = manager.getMethod("getCurrentShapeTranslationKey");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Shape name display is optional decoration.
            }
        }
        PRESENT = found;
        GET_PREVIEW_BLOCKS = previewBlocks;
        PREVIEW_INSTANCE = instance;
        GET_SHAPE_KEY = shapeKey;

        Method getConfig = null;
        Method resolve = null;
        Method pMaxBlocks = null;
        Method pMaxDistance = null;
        Method pAllowDiagonal = null;
        Method pServerAck = null;
        Field fMaxBlocks = null;
        Field fMaxDistance = null;
        Field fAllowDiagonal = null;
        try {
            Class<?> managerClass = Class.forName(CONFIG_MANAGER_CLASS, false, cl);
            getConfig = managerClass.getMethod("getConfig");
            Class<?> minerConfigClass = Class.forName(MINER_CONFIG_CLASS, false, cl);
            try {
                // Preferred route: the server-aware effective policy (what mining will honour).
                Class<?> session = Class.forName(PREF_SESSION_CLASS, false, cl);
                resolve = session.getMethod("resolvePreviewPolicy", minerConfigClass);
                Class<?> policyClass = resolve.getReturnType();
                pMaxBlocks = policyClass.getMethod("maxBlocks");
                pMaxDistance = policyClass.getMethod("maxDistance");
                pAllowDiagonal = policyClass.getMethod("allowDiagonal");
                try {
                    pServerAck = policyClass.getMethod("serverAcknowledged");
                } catch (NoSuchMethodException ignored2) {
                    // Older policy record without the server flag; treat as local.
                }
            } catch (ReflectiveOperationException ignored2) {
                // Fallback: raw local config fields.
                try {
                    fMaxBlocks = minerConfigClass.getField("maxBlocks");
                    fMaxDistance = minerConfigClass.getField("maxDistance");
                    fAllowDiagonal = minerConfigClass.getField("allowDiagonal");
                } catch (NoSuchFieldException ignored3) {
                    getConfig = null;
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // No config bridge; the addon keeps using its own client settings.
            getConfig = null;
        }
        GET_CONFIG = getConfig;
        RESOLVE_POLICY = resolve;
        POLICY_MAX_BLOCKS = pMaxBlocks;
        POLICY_MAX_DISTANCE = pMaxDistance;
        POLICY_ALLOW_DIAGONAL = pAllowDiagonal;
        POLICY_SERVER_ACK = pServerAck;
        FIELD_MAX_BLOCKS = fMaxBlocks;
        FIELD_MAX_DISTANCE = fMaxDistance;
        FIELD_ALLOW_DIAGONAL = fAllowDiagonal;
    }

    private ChainPositionBridge() { }

    /** True when OneKeyMiner's API class was found on the classpath and still answers. */
    static boolean isMainModPresent() {
        if (!PRESENT) return false;
        if (broken && !brokenPermanent && net.minecraft.client.Minecraft.getInstance().level != brokenLevel) {
            // New world/dimension: give the main mod another chance before demoting again.
            broken = false;
            consecutiveFailures = 0;
            brokenLevel = null;
        }
        return !broken;
    }

    /**
     * True when the OneKeyMiner preview API class (1.6.8+) was located at load time - unlike
     * {@link #isMainModPresent()} this ignores the transient demotion state, so the version
     * hint can tell "old main mod, no API" apart from "new main mod, temporary read failure".
     */
    static boolean previewApiAttached() {
        return PRESENT;
    }

    /**
     * Reads the live chain preview list computed by the main mod, exactly as published.
     *
     * @return immutable position list; empty when the main mod previews nothing right now;
     *         null when the bridge broke (callers then fall back to the local scanner)
     */
    static List<BlockPos> readMiningPreview() {
        if (!isMainModPresent()) return null;
        try {
            Object result = GET_PREVIEW_BLOCKS.invoke(null);
            if (result instanceof List<?> list) {
                @SuppressWarnings("unchecked") // OneKeyMinerAPI declares List<BlockPos>
                List<BlockPos> typed = (List<BlockPos>) list;
                consecutiveFailures = 0;
                return typed;
            }
            if (result instanceof Iterable<?> iterable) {
                List<BlockPos> positions = new ArrayList<>();
                for (Object value : iterable) {
                    if (value instanceof BlockPos pos) positions.add(pos.immutable());
                }
                consecutiveFailures = 0;
                return List.copyOf(positions);
            }
            consecutiveFailures = 0;
            return List.of();
        } catch (LinkageError fatal) {
            // Class-loading level damage: retrying can only throw again.
            broken = true;
            brokenPermanent = true;
            return null;
        } catch (ReflectiveOperationException | RuntimeException t) {
            // A one-off hiccup (unloading world, config reload) must not demote the bridge.
            // Only a sustained streak - FAILURE_LIMIT consecutive reads spanning at least
            // FAILURE_WINDOW_MS - flips us to the local scanner, and the player is told so
            // in chat once instead of watching a log line they cannot see.
            long now = System.currentTimeMillis();
            if (consecutiveFailures == 0) firstFailureAt = now;
            if (++consecutiveFailures >= FAILURE_LIMIT && now - firstFailureAt >= FAILURE_WINDOW_MS) {
                broken = true;
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                brokenLevel = mc.level;
                cn.blockforge.generated.modad7bbe58.GeneratedMod.LOGGER.warn(
                        "[连锁可视化续作] 连续读取主模组连锁预览失败超过 2 秒，本次进入的世界改用本地预判（换世界后自动重试）。", t);
                if (mc.player != null) {
                    // Chat line, not action bar: the demotion happens once per world and the
                    // player may be mid-mining when it fires (r15 audit report issue 2).
                    mc.player.sendSystemMessage(net.minecraft.network.chat.Component
                            .translatable("msg.onekeyminer_chainable_visuals.bridge_degraded"));
                }
            }
            return null;
        }
    }

    /** The main mod's effective chain limits, mirrored from its own config/session state. */
    record MainPolicy(int maxBlocks, int maxDistance, boolean allowDiagonal, boolean fromServer) { }

    /** True when the main mod's config classes were located (independent of the preview API). */
    static boolean configBridgeAvailable() {
        return GET_CONFIG != null;
    }

    /**
     * Main mod's live chain limits, or null when no bridge exists. Server-synced values win
     * over local ones automatically because they are read through the main mod's own
     * {@code resolvePreviewPolicy} - the same call its preview manager uses.
     */
    static MainPolicy policy() {
        if (GET_CONFIG == null) return null;
        MainPolicy cached = cachedPolicy;
        long now = System.currentTimeMillis();
        if (cached != null && now - cachedPolicyAt < POLICY_CACHE_MS) return cached;
        MainPolicy read = readPolicy();
        if (read != null) {
            cachedPolicy = read;
            cachedPolicyAt = now;
            return read;
        }
        return cached; // keep last known value on transient failures
    }

    private static MainPolicy readPolicy() {
        try {
            Object config = GET_CONFIG.invoke(null);
            if (config == null) return null;
            if (RESOLVE_POLICY != null) {
                Object policy = RESOLVE_POLICY.invoke(null, config);
                if (policy != null) {
                    int blocks = ((Number) POLICY_MAX_BLOCKS.invoke(policy)).intValue();
                    int distance = ((Number) POLICY_MAX_DISTANCE.invoke(policy)).intValue();
                    boolean diagonal = Boolean.TRUE.equals(POLICY_ALLOW_DIAGONAL.invoke(policy));
                    boolean ack = POLICY_SERVER_ACK != null && Boolean.TRUE.equals(POLICY_SERVER_ACK.invoke(policy));
                    return sanitize(blocks, distance, diagonal, ack);
                }
            }
            if (FIELD_MAX_BLOCKS != null) {
                int blocks = FIELD_MAX_BLOCKS.getInt(config);
                int distance = FIELD_MAX_DISTANCE.getInt(config);
                boolean diagonal = FIELD_ALLOW_DIAGONAL.getBoolean(config);
                return sanitize(blocks, distance, diagonal, false);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Caller keeps the previous values; nothing here may disturb the game.
        }
        return null;
    }

    private static MainPolicy sanitize(int blocks, int distance, boolean diagonal, boolean fromServer) {
        int safeBlocks = Math.max(1, Math.min(8192, blocks));
        int safeDistance = Math.max(1, Math.min(256, distance));
        return new MainPolicy(safeBlocks, safeDistance, diagonal, fromServer);
    }

    /** Translation key of the shape the main mod is previewing with, or null if unavailable. */
    static String currentShapeTranslationKey() {
        if (!isMainModPresent() || PREVIEW_INSTANCE == null || GET_SHAPE_KEY == null) return null;
        try {
            Object manager = PREVIEW_INSTANCE.invoke(null);
            Object key = GET_SHAPE_KEY.invoke(manager);
            if (key instanceof String s && !s.isEmpty()) return s;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Decoration only; ignore.
        }
        return null;
    }
}

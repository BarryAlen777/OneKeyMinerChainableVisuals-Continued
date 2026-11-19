package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Publishes immutable preview groups; the renderer independently uses the live crosshair. */
public final class ChainScanner {
    private static final int HARD_CAP = 8192;
    private static final int MAX_SCAN_DEPTH = 256;
    private static final String AMORPHOUS = "onekeyminer.shape.amorphous";
    private static final AnchorGate GATE = new AnchorGate();
    private static final long[] DISPLAY_KEYS = new long[HARD_CAP + 1];

    private static ClientLevel previewLevel;
    private static BlockPos sampledTarget;
    private static int sampledOrientation;
    private static List<BlockPos> positions = List.of();
    private static BlockPos center;
    private static long lastRefreshTick;
    private static boolean interactionMode;
    private static boolean mainModDriven;
    private static String shapeKey;
    private static int mainModRawCount;
    private static BlockState originState;

    private static final long[] SCAN_QUEUE = new long[HARD_CAP + 32];
    private static final long[] SCAN_KEYS = new long[HARD_CAP];
    private static final LongSet SCAN_VISITED = new LongSet();
    private static final BlockPos.MutableBlockPos SCAN_POS = new BlockPos.MutableBlockPos();

    private ChainScanner() { }

    /** Called early in client tick END, before the main mod's normal-priority handler. */
    static void beforeMainTick(Minecraft mc) {
        if (previewLevel != mc.level) {
            clear();
            MergedOutline.reset();
            previewLevel = mc.level;
        }
        sampledTarget = null;
        GATE.begin(null);
        if (!canFollow(mc)) return;
        sampledTarget = target(mc);
        if (sampledTarget == null) return;
        sampledOrientation = orientation(mc);
        GATE.begin(ChainPositionBridge.readMiningPreview());
    }

    /** Called late in client tick END, after OneKeyMiner publishes its new snapshot. */
    static void tick(Minecraft mc) {
        BlockPos aim = target(mc);
        if (aim == null || mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()
                || mc.player.isSpectator()) {
            clear();
            return;
        }
        boolean interact = wantsInteraction(mc);
        if (!interact && Config.FOLLOW_MAIN_MOD.get() && ChainPositionBridge.isMainModPresent()
                && !KeyBindings.chainHeld(mc)) {
            clear();
            return;
        }
        List<BlockPos> live = canFollow(mc) ? ChainPositionBridge.readMiningPreview() : null;
        if (live != null) {
            if (!mainModDriven) clearGroup();
            mainModDriven = true;
            interactionMode = false;
            if (center != null && mc.level.getBlockState(center) != originState) {
                GATE.clear();
                clearGroup();
            }
            switch (GATE.finish(live, live.isEmpty())) {
                case CLEAR -> clearGroup();
                case ANCHOR -> {
                    if (aim.equals(sampledTarget) && orientation(mc) == sampledOrientation) {
                        adopt(mc.level, live, aim);
                    }
                }
                case SAME -> { }
            }
            return;
        }

        boolean sourceChanged = mainModDriven || interact != interactionMode;
        GATE.reset();
        mainModDriven = false;
        interactionMode = interact;
        long now = mc.level.getGameTime();
        if (sourceChanged || !aim.equals(center) || mc.level.getBlockState(aim) != originState
                || now < lastRefreshTick || now - lastRefreshTick >= Config.REFRESH_TICKS.get()) {
            lastRefreshTick = now;
            center = aim;
            originState = mc.level.getBlockState(aim);
            positions = scan(mc, aim, interact);
        }
    }

    private static boolean canFollow(Minecraft mc) {
        return mc.player != null && !mc.player.isSpectator() && mc.level != null && !wantsInteraction(mc)
                && Config.FOLLOW_MAIN_MOD.get() && KeyBindings.chainHeld(mc) && ChainPositionBridge.isMainModPresent();
    }

    private static void adopt(Level level, List<BlockPos> live, BlockPos aim) {
        BlockState state = level.getBlockState(aim);
        if (state.isAir()) {
            GATE.clear();
            clearGroup();
            return;
        }
        String shape = ChainPositionBridge.currentShapeTranslationKey();
        int capped = Math.min(HARD_CAP, live.size());
        List<BlockPos> display = new ArrayList<>(capped + 1);
        // Legacy OneKeyMiner lists omit the origin; newer snapshots may already include it.
        display.add(aim);
        int covered = 0;
        for (int i = 0; i < capped; i++) {
            BlockPos pos = live.get(i);
            if (pos == null || pos.equals(aim)) continue;
            display.add(pos.immutable());
            DISPLAY_KEYS[covered++] = packed(pos);
        }
        GATE.anchor(packed(aim), AMORPHOUS.equals(shape) ? AnchorGate.ANY_ORIENTATION : sampledOrientation,
                DISPLAY_KEYS, covered);
        center = aim;
        originState = state;
        mainModRawCount = capped;
        shapeKey = shape;
        List<BlockPos> frozen = Collections.unmodifiableList(display);
        if (!positions.equals(frozen)) positions = frozen;
    }

    static BlockPos target(Minecraft mc) {
        return mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getBlockPos() : null;
    }

    static boolean wantsInteraction(Minecraft mc) {
        return Config.SHOW_INTERACTION.get() && KeyBindings.interactionHeld(mc);
    }

    private static long packed(BlockPos pos) {
        return PackedPos.pack(pos.getX(), pos.getY(), pos.getZ());
    }

    private static int orientation(Minecraft mc) {
        float pitch = mc.player.getXRot();
        int vertical = pitch < -45 ? 1 : pitch > 45 ? 2 : 0;
        return mc.player.getDirection().ordinal() * 3 + vertical;
    }

    static List<BlockPos> positions(Minecraft mc) {
        BlockPos aim = target(mc);
        if (aim == null || mc.player == null || mc.player.isSpectator() || mc.level == null
                || mc.level != previewLevel || mc.screen != null || mc.isPaused() || center == null
                || wantsInteraction(mc) != interactionMode || mc.level.getBlockState(center) != originState) {
            return List.of();
        }
        if (!mainModDriven) return aim.equals(center) ? positions : List.of();
        boolean covered = GATE.covers(packed(aim), AMORPHOUS.equals(shapeKey) ? AnchorGate.ANY_ORIENTATION : orientation(mc));
        return Config.FOLLOW_MAIN_MOD.get() && KeyBindings.chainHeld(mc) && covered
                && mc.level.getBlockState(aim).getBlock() == originState.getBlock() ? positions : List.of();
    }

    private static List<BlockPos> scan(Minecraft mc, BlockPos root, boolean interact) {
        Level level = mc.level;
        BlockState origin = level.getBlockState(root);
        if (origin.isAir() || (interact && !isInteractable(origin))) return List.of();
        Block expected = origin.getBlock();
        ChainPositionBridge.MainPolicy policy = Config.FOLLOW_MAIN_MOD.get() ? ChainPositionBridge.policy() : null;
        int max = Math.min(HARD_CAP, policy == null ? Config.MAX_CHAIN_BLOCKS.get() : policy.maxBlocks());
        int depth = Math.min(MAX_SCAN_DEPTH, policy == null ? Config.MAX_CHAIN_DEPTH.get() : policy.maxDistance());
        boolean diagonal = policy == null ? Config.DIAGONAL_CHAINING.get() : policy.allowDiagonal();
        final int rootX = root.getX(), rootY = root.getY(), rootZ = root.getZ();
        final int neighbourCount = diagonal ? FloodFillCore.NX.length : 6;
        int found = FloodFillCore.floodFill(rootX, rootY, rootZ, max, depth, neighbourCount,
                (x, y, z) -> {
                    SCAN_POS.set(x, y, z);
                    if (!level.hasChunkAt(SCAN_POS)) return false;
                    BlockState state = level.getBlockState(SCAN_POS);
                    if (state.isAir() || state.getBlock() != expected) return false;
                    return !interact || isInteractable(state);
                },
                SCAN_QUEUE, SCAN_VISITED, SCAN_KEYS);
        if (found <= 0) return List.of();
        List<BlockPos> out = new ArrayList<>(found);
        for (int i = 0; i < found; i++) {
            long key = SCAN_KEYS[i];
            out.add(new BlockPos(PackedPos.unpackX(key), PackedPos.unpackY(key), PackedPos.unpackZ(key)));
        }
        return Collections.unmodifiableList(out);
    }

    private static boolean isInteractable(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id == null ? "" : id.getPath();
        return state.hasBlockEntity() || path.endsWith("door") || path.endsWith("trapdoor")
                || path.endsWith("gate") || path.endsWith("button") || path.endsWith("lever")
                || path.endsWith("chest") || path.endsWith("barrel") || path.endsWith("furnace");
    }

    private static void clearGroup() {
        positions = List.of();
        center = null;
        originState = null;
        mainModRawCount = 0;
        shapeKey = null;
    }

    private static void clear() {
        GATE.reset();
        clearGroup();
        mainModDriven = false;
        interactionMode = false;
    }

    static int displaySize() { return positions.size(); }
    static BlockPos center() { return center; }
    static boolean interactionMode() { return interactionMode; }
    static boolean mainModDriven() { return mainModDriven; }
    static String shapeKey() { return shapeKey; }
    static int chainedBlocks() { return mainModDriven ? mainModRawCount : Math.max(0, positions.size() - 1); }
}

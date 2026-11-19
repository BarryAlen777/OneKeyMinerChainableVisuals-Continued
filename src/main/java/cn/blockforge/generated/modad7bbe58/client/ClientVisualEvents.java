package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.Config;
import cn.blockforge.generated.modad7bbe58.GeneratedMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class ClientVisualEvents {
    private static int particleTick;

    private ClientVisualEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sampleBeforeMainMod(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ChainScanner.beforeMainTick(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        migrateDefaultsOnce();
        ClientSetup.notifyOnce(mc);
        drainAutoMergeNotice(mc);
        ChainScanner.tick(mc);
        SoundFeedback.tick(mc);
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        // consumeClick() deliberately runs first so a press inside any GUI is swallowed here
        // rather than queued and popping the settings screen open the moment the GUI closes.
        if (KeyBindings.OPEN_CONFIG_KEY.consumeClick() && mc.screen == null) {
            mc.setScreen(new ConfigScreen(null));
        }
        if (++particleTick >= 3) {
            particleTick = 0;
            spawnParticleTraces(mc);
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (renderFaulty) {
            // Recover automatically when the player (re)enters a world; a cosmetic overlay
            // must never stay dead for the whole game session after one unlucky frame.
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != faultLevel) {
                renderFaulty = false;
                faultLevel = null; // drop the strong reference, or an old ClientLevel
                                   // (chunks, entities) stays pinned until the next fault
            } else return;
        }
        try {
            renderLevelSafe(event);
        } catch (Throwable t) {
            // A cosmetic preview overlay must never take the whole game down.
            renderFaulty = true;
            faultLevel = Minecraft.getInstance().level;
            GeneratedMod.LOGGER.error("[连锁可视化续作] 高亮渲染出错，已暂停高亮绘制"
                    + "（重进世界或改设置后自动恢复；不影响连锁功能本身）。", t);
        }
    }

    private static boolean renderFaulty;
    private static Object faultLevel;
    private static boolean defaultsMigrated;
    /** Set from the render thread when the per-block mode auto-falls back to merged outline. */
    private static volatile boolean autoMergeNoticePending;
    /** One notice per world (see {@link #drainAutoMergeNotice}); r15 audit report issue 8:
     *  it used to reset on every settings save, so tuning options while a huge preview was
     *  up re-pinged the chat each time "Done" was clicked. */
    private static boolean autoMergeNoticeShown;
    /** The level the notice was last armed for; a new world/dimension re-arms it once. */
    private static Object autoMergeNoticeLevel;

    /** Forget a paused render pipeline (called when the user changes settings). */
    static void resetRenderFault() {
        renderFaulty = false;
        faultLevel = null;
        autoMergeNoticePending = false;
    }

    /**
     * Re-arm the auto-merge notice. Called only when the user re-enables per-block mode -
     * that is the one setting change that makes a fresh "switched to merged outline for
     * performance" notice meaningful again (audit issue 19 kept, issue 8 cured).
     */
    static void rearmAutoMergeNotice() {
        autoMergeNoticeShown = false;
        autoMergeNoticePending = false;
    }

    /**
     * Send the "switched to merged outline for performance" notice once, on the client tick
     * (never from {@code RenderLevelStageEvent}: pushing chat straight off the render thread
     * is not what {@code sendSystemMessage} expects while the world is being drawn).
     */
    private static void drainAutoMergeNotice(Minecraft mc) {
        if (mc.level != autoMergeNoticeLevel) {
            // New world or dimension: the player has a fresh context, let them be told once
            // more. (Held weakly-in-spirit: only ever the *current* level object.)
            autoMergeNoticeLevel = mc.level;
            autoMergeNoticeShown = false;
        }
        if (!autoMergeNoticePending || autoMergeNoticeShown) return;
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        autoMergeNoticeShown = true;
        mc.player.sendSystemMessage(
                Component.translatable("msg.onekeyminer_chainable_visuals.auto_merged", PER_BLOCK_MERGE_LIMIT));
        GeneratedMod.LOGGER.info("[连锁可视化续作] 预览方块超过 {} 个，已临时改用统一外轮廓显示（性能保护）。",
                PER_BLOCK_MERGE_LIMIT);
    }

    /**
     * r7 switched the out-of-the-box look to FTB Ultimine's style (opaque white outline, no
     * halo) and turned particles/sounds off by default. Players upgrading from r6 still have
     * the old values written into their TOML, so the new defaults alone would never reach
     * them. Once per session, if the config predates the switch, push the new values in and
     * bump the marker; users who never touched these options end up exactly at the defaults.
     *
     * <p>Two outcomes are distinguished deliberately (audit report issue 18). "Config not
     * loaded yet" is transient - the client config handle can lag the first tick by a frame -
     * so it must NOT latch the flag or those players keep their stale r6 defaults forever.
     * "Migration attempted but failed" (e.g. a read-only TOML) does latch, so we never re-run
     * and re-stomp a value the player is actively changing.</p>
     */
    private static void migrateDefaultsOnce() {
        if (defaultsMigrated) return;
        // Transient: the config has not finished loading this session. Retry next tick; do
        // not set the flag, or the migration would be skipped for the whole game forever.
        if (GeneratedMod.CLIENT_CONFIG == null) return;
        try {
            if (Config.DEFAULTS_REVISION.get() >= 2) {
                defaultsMigrated = true; // already migrated in a past launch; nothing to do
                return;
            }
            Config.SHOW_PARTICLES.set(false);
            Config.PLAY_CHAIN_SOUNDS.set(false);
            Config.OUTLINE_MODE.set(false);
            Config.LINE_ALPHA.set(1.0D);
            Config.COLOR_MINING.set(0xFFFFFFFF);
            Config.DEFAULTS_REVISION.set(2);
            GeneratedMod.CLIENT_CONFIG.save();
            defaultsMigrated = true;
            GeneratedMod.LOGGER.info("[连锁可视化续作] 已把升级玩家旧的显示默认值迁移为 FTB 风格（白色实轮廓、默认关闭粒子与音效）。"
                    + "想改回去随时按 N 进设置调整。");
        } catch (Throwable t) {
            // A real attempt failed (config present but read/save errored). Latch so we do not
            // loop every tick; the in-session FTB-style display still holds.
            defaultsMigrated = true;
            GeneratedMod.LOGGER.warn("[连锁可视化续作] 默认值迁移未完成（可能配置文件只读），"
                    + "本局游戏内已按 FTB 风格显示，不影响连锁功能本身。", t);
        }
    }

    private static void renderLevelSafe(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator()
                || mc.screen != null || mc.isPaused()) return;
        if (mc.hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof Sheep target && target.readyForShearing()
                    && Config.SHOW_SHEEP.get() && KeyBindings.chainHeld(mc)
                    && (mc.player.getMainHandItem().is(Items.SHEARS) || mc.player.getOffhandItem().is(Items.SHEARS))) {
                renderSheep(event, mc, target);
            }
            return;
        }
        BlockPos aim = ChainScanner.target(mc);
        if (aim == null) return;
        boolean interaction = ChainScanner.wantsInteraction(mc);
        if (!interaction && !Config.SHOW_MINING.get()) return;
        List<BlockPos> positions = ChainScanner.positions(mc);
        AABB aimBox = showAimMarker(mc) ? aimBounds(mc, event.getCamera(), aim) : null;
        if (positions.size() < 2 && aimBox == null) return;

        // Per-block wireframes cost 24 vertices each (48 with the halo): past about 1500
        // blocks that is several MB of line data per frame and the client visibly chokes.
        // Fall back to the merged silhouette automatically, whatever the toggle says.
        // Audit issue 19: the automatic switch used to be invisible - players with the
        // unified outline OFF swore the mod "silently stopped drawing boxes". Flag it once
        // (chat notice is drained on the client tick, not from this render thread).
        boolean drawGroup = positions.size() >= 2;
        boolean autoMerged = drawGroup && !Config.MERGED_OUTLINE.get()
                && positions.size() > PER_BLOCK_MERGE_LIMIT;
        if (autoMerged) autoMergeNoticePending = true;
        boolean merged = Config.MERGED_OUTLINE.get() || autoMerged;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean xray = Config.SHOW_THROUGH_WALLS.get();
        int argb = (interaction ? Config.COLOR_INTERACTION : Config.COLOR_MINING).get();
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float alpha = Config.LINE_ALPHA.get().floatValue() * ((argb >>> 24) & 0xFF) / 255.0F;
        if (drawGroup) {
            if (merged) {
                renderMerged(poseStack, buffers, camera, positions, r, g, b, alpha, xray);
            } else {
                renderPerBlock(poseStack, buffers, aimBox == null ? null : aim, camera, positions, r, g, b, alpha, xray);
            }
        }
        if (aimBox != null) {
            renderAimBox(poseStack, buffers, camera, aim, aimBox, alpha, xray);
        }
    }

    /** Above this many preview blocks the per-block mode auto-switches to the merged outline. */
    private static final int PER_BLOCK_MERGE_LIMIT = 1500;

    /** Above this many preview blocks the faint depth-free echo pass is skipped (cost cap). */
    private static final int ECHO_MAX_BLOCKS = 2048;

    /**
     * The merged silhouette pass: a single bright outline tracing the union of all preview
     * blocks, optionally softened by an outward halo, and echoed once more without depth
     * testing so partially hidden groups still read as a whole shape. Geometry is emitted in
     * group-local space (anchored at the outline's minimum corner), so vertices stay small
     * floats no matter how far from the world origin the player digs - no coordinate jitter.
     */
    private static void renderMerged(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                     Camera camera, List<BlockPos> positions,
                                     float r, float g, float b, float alpha, boolean xray) {
        MergedOutline.Data outline = MergedOutline.get(positions);
        if (outline.core().length == 0) return;
        poseStack.pushPose();
        try {
            poseStack.translate(outline.ox() - camera.getPosition().x,
                    outline.oy() - camera.getPosition().y,
                    outline.oz() - camera.getPosition().z);
            PoseStack.Pose matrix = poseStack.last();
            boolean custom = Config.CUSTOM_RENDER_PIPELINE.get();
            RenderType main = lineLayer(xray);
            try {
                VertexConsumer lines = buffers.getBuffer(main);
                emitEdges(matrix, lines, outline.core(), 0.0F, outline, r, g, b, alpha);
                if (Config.OUTLINE_MODE.get()) {
                    // Soft bright halo just outside the core line for a glowing-edge read.
                    emitEdges(matrix, lines, outline.core(), 0.04F, outline, 1.0F, 1.0F, 1.0F, alpha * 0.5F);
                }
            } finally {
                // Flush our vertex data even when emitting threw: leaving a half-built
                // batch in the shared buffer source would crash the game at frame end,
                // outside any of our try/catch blocks.
                buffers.endBatch(main);
            }
            if (custom && !xray && positions.size() <= ECHO_MAX_BLOCKS) {
                // Faint depth-free echo so the outline stays readable behind occluders
                // (FTB Ultimine does the same with a fixed ~30/255-alpha white ghost pass).
                // The echo doubles the line-emission cost, which the r19 spark profile shows
                // is this overlay's entire render budget (emitEdges ~2% of render-thread
                // time). Past a few thousand blocks the second pass is wasted overdraw on a
                // silhouette far too dense to read through walls anyway, so it is skipped.
                try {
                    VertexConsumer ghost = buffers.getBuffer(PreviewRenderTypes.XRAY_LINES);
                    emitEdges(matrix, ghost, outline.core(), 0.0F, outline, r, g, b, 0.12F);
                } finally {
                    buffers.endBatch(PreviewRenderTypes.XRAY_LINES);
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Fallback look: one wireframe per block, anchored at the first preview position.
     * Written against our own vertex format on purpose - {@code LevelRenderer.renderLineBox}
     * skips the NORMAL element POSITION_COLOR_NORMAL demands, and the unfilled vertex it
     * leaves behind threw every frame: turning "unified outline" off used to kill ALL
     * highlighting for the rest of the session.
     */
    private static void renderPerBlock(PoseStack poseStack, MultiBufferSource.BufferSource buffers, BlockPos marked,
                                       Camera camera, List<BlockPos> positions,
                                       float r, float g, float b, float alpha, boolean xray) {
        BlockPos anchor = positions.get(0);
        poseStack.pushPose();
        try {
            poseStack.translate(anchor.getX() - camera.getPosition().x,
                    anchor.getY() - camera.getPosition().y,
                    anchor.getZ() - camera.getPosition().z);
            PoseStack.Pose matrix = poseStack.last();
            RenderType layer = lineLayer(xray);
            try {
                VertexConsumer lines = buffers.getBuffer(layer);
                boolean halo = Config.OUTLINE_MODE.get();
                for (BlockPos pos : positions) {
                    if (pos.equals(marked)) continue;
                    float x = pos.getX() - anchor.getX(), y = pos.getY() - anchor.getY(), z = pos.getZ() - anchor.getZ();
                    emitBox(matrix, lines, x - 0.003F, y - 0.003F, z - 0.003F, x + 1.003F, y + 1.003F, z + 1.003F, r, g, b, alpha);
                    if (halo) {
                        emitBox(matrix, lines, x - 0.015F, y - 0.015F, z - 0.015F, x + 1.015F, y + 1.015F, z + 1.015F,
                                1.0F, 1.0F, 1.0F, alpha * 0.55F);
                    }
                }
            } finally {
                buffers.endBatch(layer);
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * The crosshair marker follows the live hit result each frame, independently of the
     * chain group. Its separate color distinguishes it from the group at shared edges.
     */
    private static boolean showAimMarker(Minecraft mc) {
        return Config.SHOW_MINING.get() && !ChainScanner.wantsInteraction(mc) && KeyBindings.chainHeld(mc)
                && !mc.player.isSpectator() && Config.LINE_ALPHA.get() > 0
                && (Config.COLOR_MINING.get() >>> 24) != 0;
    }

    private static AABB aimBounds(Minecraft mc, Camera camera, BlockPos pos) {
        var state = mc.level.getBlockState(pos);
        if (state.isAir()) return null;
        var shape = state.getShape(mc.level, pos, CollisionContext.of(camera.getEntity()));
        return shape.isEmpty() ? null : shape.bounds().inflate(0.012D);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceVanillaAimBox(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (renderFaulty || mc.player == null || mc.level == null || !showAimMarker(mc)) return;
        if (aimBounds(mc, event.getCamera(), event.getTarget().getBlockPos()) != null) event.setCanceled(true);
    }

    private static void renderAimBox(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                     Camera camera, BlockPos pos, AABB box,
                                     float alpha, boolean xray) {
        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - camera.getPosition().x,
                    pos.getY() - camera.getPosition().y,
                    pos.getZ() - camera.getPosition().z);
            PoseStack.Pose matrix = poseStack.last();
            RenderType layer = lineLayer(xray);
            try {
                VertexConsumer lines = buffers.getBuffer(layer);
                emitBox(matrix, lines, (float) box.minX, (float) box.minY, (float) box.minZ,
                        (float) box.maxX, (float) box.maxY, (float) box.maxZ, 0.25F, 1.0F, 0.35F, alpha);
            } finally {
                buffers.endBatch(layer);
            }
        } finally {
            poseStack.popPose();
        }
    }

    /** Picks the line layer: our own render types, or vanilla lines when the compat switch is off. */    private static RenderType lineLayer(boolean xray) {
        if (!Config.CUSTOM_RENDER_PIPELINE.get()) return RenderType.lines();
        return xray ? PreviewRenderTypes.XRAY_LINES : PreviewRenderTypes.DEPTH_LINES;
    }

    private static void emitEdges(PoseStack.Pose matrix, VertexConsumer buffer, float[] core,
                                  float push, MergedOutline.Data outline,
                                  float r, float g, float b, float a) {
        for (int i = 0; i < core.length; i += 6) {
            float x1 = core[i], y1 = core[i + 1], z1 = core[i + 2];
            float x2 = core[i + 3], y2 = core[i + 4], z2 = core[i + 5];
            if (push > 0.0F) {
                // Radial nudge away from the group centroid (allocation-free halo).
                float dx1 = x1 - outline.cx(), dy1 = y1 - outline.cy(), dz1 = z1 - outline.cz();
                float l1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1 + dz1 * dz1);
                if (l1 >= 1.0E-4F) {
                    float k1 = push / l1;
                    x1 += dx1 * k1; y1 += dy1 * k1; z1 += dz1 * k1;
                }
                float dx2 = x2 - outline.cx(), dy2 = y2 - outline.cy(), dz2 = z2 - outline.cz();
                float l2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2 + dz2 * dz2);
                if (l2 >= 1.0E-4F) {
                    float k2 = push / l2;
                    x2 += dx2 * k2; y2 += dy2 * k2; z2 += dz2 * k2;
                }
            }
            emitLine(matrix, buffer, x1, y1, z1, x2, y2, z2, r, g, b, a);
        }
    }

    private static void emitBox(PoseStack.Pose pose, VertexConsumer buffer,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        for (int[] e : BOX_EDGES) {
            emitLine(pose, buffer, e[0] == 0 ? x1 : x2, e[1] == 0 ? y1 : y2, e[2] == 0 ? z1 : z2,
                    e[3] == 0 ? x1 : x2, e[4] == 0 ? y1 : y2, e[5] == 0 ? z1 : z2, r, g, b, a);
        }
    }

    private static void emitLine(PoseStack.Pose pose, VertexConsumer buffer,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        // The vanilla line shader uses Normal as a direction to project line width, not as a face normal.
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float lengthSq = dx * dx + dy * dy + dz * dz;
        if (lengthSq == 0.0F) return;
        float inv = (float) (1.0 / Math.sqrt(lengthSq));
        Matrix4f matrix = pose.pose();
        Matrix3f directions = pose.normal();
        dx *= inv; dy *= inv; dz *= inv;
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(directions, dx, dy, dz).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(directions, dx, dy, dz).endVertex();
    }

    /** 12 unit-cube edges as corner picks per axis (0 = min coordinate, 1 = max coordinate). */
    private static final int[][] BOX_EDGES = {
            {0, 0, 0, 1, 0, 0}, {0, 1, 0, 1, 1, 0}, {0, 0, 1, 1, 0, 1}, {0, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {0, 0, 1, 0, 1, 1}, {1, 0, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 1}, {1, 0, 0, 1, 0, 1}, {0, 1, 0, 0, 1, 1}, {1, 1, 0, 1, 1, 1},
    };

    private static void renderSheep(RenderLevelStageEvent event, Minecraft mc, Sheep target) {
        // Collect and filter around the same reference point: the aimed sheep. Inflating the
        // *player* box while culling by distance-to-sheep used to highlight sheep up to twice
        // the configured range away.
        AABB area = target.getBoundingBox().inflate(Config.SHEEP_RANGE.get());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        try {
            Camera camera = event.getCamera();
            // Anchor at the aimed sheep's block: like the chain paths, keep matrix translations
            // camera-relative-small and vertex magnitudes tiny, so boxes never jitter far out.
            BlockPos anchor = target.blockPosition();
            pose.translate(anchor.getX() - camera.getPosition().x,
                    anchor.getY() - camera.getPosition().y,
                    anchor.getZ() - camera.getPosition().z);
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            PoseStack.Pose matrix = pose.last();
            boolean xray = Config.SHOW_THROUGH_WALLS.get();
            RenderType layer = lineLayer(xray);
            try {
                VertexConsumer lines = buffers.getBuffer(layer);
                int argb = Config.COLOR_SHEEP.get();
                float r = ((argb >> 16) & 0xFF) / 255.0F;
                float g = ((argb >> 8) & 0xFF) / 255.0F;
                float b = (argb & 0xFF) / 255.0F;
                float alpha = Config.LINE_ALPHA.get().floatValue() * ((argb >>> 24) & 0xFF) / 255.0F;
                double rangeSq = (double) Config.SHEEP_RANGE.get() * Config.SHEEP_RANGE.get();
                for (Sheep sheep : mc.level.getEntitiesOfClass(Sheep.class, area)) {
                    if (!sheep.isAlive() || !sheep.readyForShearing() || sheep.distanceToSqr(target) > rangeSq) continue;
                    AABB box = sheep.getBoundingBox().inflate(0.06D);
                    emitBox(matrix, lines, (float) (box.minX - anchor.getX()), (float) (box.minY - anchor.getY()),
                            (float) (box.minZ - anchor.getZ()), (float) (box.maxX - anchor.getX()),
                            (float) (box.maxY - anchor.getY()), (float) (box.maxZ - anchor.getZ()),
                            r, g, b, alpha);
                    if (Config.OUTLINE_MODE.get()) {
                        // A wider, fainter second pass reads as a soft glow around each sheep.
                        AABB glow = box.inflate(0.09D);
                        emitBox(matrix, lines, (float) (glow.minX - anchor.getX()), (float) (glow.minY - anchor.getY()),
                                (float) (glow.minZ - anchor.getZ()), (float) (glow.maxX - anchor.getX()),
                                (float) (glow.maxY - anchor.getY()), (float) (glow.maxZ - anchor.getZ()),
                                1.0F, 1.0F, 1.0F, alpha * 0.4F);
                    }
                }
            } finally {
                buffers.endBatch(layer);
            }
        } finally {
            pose.popPose();
        }
    }

    /**
     * Particle cap per batch (batches run every 3 ticks). The old 48 was ~320 END_ROD
     * particles/second while a chain is previewed - plenty on a desktop, a visible hit on
     * weak GPUs; 24 keeps the effect readable at half the cost.
     */
    private static final int PARTICLES_PER_BATCH = 24;

    private static void spawnParticleTraces(Minecraft mc) {
        if (!Config.SHOW_PARTICLES.get() || mc.level == null || ChainScanner.center() == null) return;
        List<BlockPos> positions = ChainScanner.positions(mc);
        if (positions.size() < 2) return;
        BlockPos origin = ChainScanner.center();
        int emitted = 0;
        double density = Config.PARTICLE_ALPHA.get();
        for (BlockPos pos : positions) {
            if (pos.equals(origin) || emitted >= PARTICLES_PER_BATCH) continue;
            double dx = pos.getX() - origin.getX();
            double dy = pos.getY() - origin.getY();
            double dz = pos.getZ() - origin.getZ();
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 40.0D || mc.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 48.0D * 48.0D) continue;
            int points = Math.max(1, Math.min(5, (int) (length * density / 2.0D)));
            for (int i = 1; i <= points && emitted < PARTICLES_PER_BATCH; i++) {
                double t = (double) i / (points + 1);
                double x = origin.getX() + 0.5D + dx * t;
                double y = origin.getY() + 0.5D + dy * t;
                double z = origin.getZ() + 0.5D + dz * t;
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        x, y, z, 0.0D, 0.012D, 0.0D);
                emitted++;
            }
        }
    }
}

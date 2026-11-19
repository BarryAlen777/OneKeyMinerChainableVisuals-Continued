package cn.blockforge.generated.modad7bbe58.client;

import cn.blockforge.generated.modad7bbe58.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Compact four-tab client settings screen. */
public final class ConfigScreen extends Screen {
    private final Screen parent;
    private int category;
    private int followingInfoY = -1;
    private boolean showMining, showInteraction, merged, outline, xray, diagonal, followMain;
    private boolean particles, hud, sounds, sheep, customPipeline;
    private double opacity, particleDensity, soundVolume;
    private int maxBlocks, maxDepth, sheepRange, refreshTicks;
    private int mineR, mineG, mineB, mineA, interR, interG, interB, interA, sheepR0, sheepG0, sheepB0, sheepA0;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("screen.onekeyminer_chainable_visuals.title"));
        this.parent = parent;
        showMining = Config.SHOW_MINING.get();
        showInteraction = Config.SHOW_INTERACTION.get();
        merged = Config.MERGED_OUTLINE.get();
        outline = Config.OUTLINE_MODE.get();
        xray = Config.SHOW_THROUGH_WALLS.get();
        diagonal = Config.DIAGONAL_CHAINING.get();
        followMain = Config.FOLLOW_MAIN_MOD.get();
        particles = Config.SHOW_PARTICLES.get();
        hud = Config.SHOW_HUD.get();
        sounds = Config.PLAY_CHAIN_SOUNDS.get();
        sheep = Config.SHOW_SHEEP.get();
        opacity = Config.LINE_ALPHA.get();
        particleDensity = Config.PARTICLE_ALPHA.get();
        soundVolume = Config.SOUND_VOLUME.get();
        maxBlocks = Config.MAX_CHAIN_BLOCKS.get();
        maxDepth = Config.MAX_CHAIN_DEPTH.get();
        sheepRange = Config.SHEEP_RANGE.get();
        refreshTicks = Config.REFRESH_TICKS.get();
        customPipeline = Config.CUSTOM_RENDER_PIPELINE.get();
        int mc0 = Config.COLOR_MINING.get();
        mineA = (mc0 >>> 24) & 0xFF; mineR = (mc0 >> 16) & 0xFF; mineG = (mc0 >> 8) & 0xFF; mineB = mc0 & 0xFF;
        int ic = Config.COLOR_INTERACTION.get();
        interA = (ic >>> 24) & 0xFF; interR = (ic >> 16) & 0xFF; interG = (ic >> 8) & 0xFF; interB = ic & 0xFF;
        int sc = Config.COLOR_SHEEP.get();
        sheepA0 = (sc >>> 24) & 0xFF; sheepR0 = (sc >> 16) & 0xFF; sheepG0 = (sc >> 8) & 0xFF; sheepB0 = sc & 0xFF;
    }

    @Override
    protected void init() {
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.onekeyminer_chainable_visuals.tab.highlight"), b -> select(0))
                .bounds(center - 155, 30, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.onekeyminer_chainable_visuals.tab.colors"), b -> select(1))
                .bounds(center - 77, 30, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.onekeyminer_chainable_visuals.tab.lines"), b -> select(2))
                .bounds(center + 1, 30, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.onekeyminer_chainable_visuals.tab.hud"), b -> select(3))
                .bounds(center + 79, 30, 76, 20).build());

        // r18: the Highlight page needs title + tabs + six control rows + up to two live-hint
        // lines above the Done row. At the common 240-px GUI height the fixed 24-px rows
        // pushed the last hint line under the buttons (audit P3), so the column packs
        // tighter on short screens and keeps room for the hints.
        int startY = 56;
        int rowStep = 24;
        if (height < 268) {
            startY = 52;
            rowStep = 21;
        }
        int y = startY;
        switch (category) {
            case 0 -> {
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.mining", () -> showMining, v -> showMining = v);
                toggle(center + 5, y, "screen.onekeyminer_chainable_visuals.option.interaction", () -> showInteraction, v -> showInteraction = v);
                y += rowStep;
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.merged", () -> merged, v -> merged = v);
                toggle(center + 5, y, "screen.onekeyminer_chainable_visuals.option.xray", () -> xray, v -> xray = v);
                y += rowStep;
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.outline", () -> outline, v -> outline = v);
                toggle(center + 5, y, "screen.onekeyminer_chainable_visuals.option.diagonal", () -> diagonal, v -> diagonal = v);
                y += rowStep;
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.follow_main", () -> followMain, v -> followMain = v);
                slider(center + 5, y, 145, "screen.onekeyminer_chainable_visuals.option.opacity", opacity, 0.0, 1.0, v -> opacity = v, 2);
                y += rowStep;
                steppedSlider(center - 150, y, 145, "screen.onekeyminer_chainable_visuals.option.max_blocks",
                        BLOCK_LIMIT_STEPS, maxBlocks, v -> maxBlocks = v);
                slider(center + 5, y, 145, "screen.onekeyminer_chainable_visuals.option.max_depth", maxDepth, 1, 64, v -> maxDepth = (int) Math.round(v), 0);
                y += rowStep;
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.custom_pipeline", () -> customPipeline, v -> customPipeline = v);
                // Local-scan cadence had no UI entry at all (audit P3: only editable in the
                // TOML). This right-hand slot is the last free row on the page.
                slider(center + 5, y, 145, "screen.onekeyminer_chainable_visuals.option.refresh_ticks",
                        refreshTicks, 1, 20, v -> refreshTicks = (int) Math.round(v), 0);
                followingInfoY = y + 26;
            }
            case 1 -> {
                channelRow(center - 155, 64, mineR, mineG, mineB, mineA,
                        v -> mineR = v, v -> mineG = v, v -> mineB = v, v -> mineA = v,
                        () -> applyColor(Config.DEFAULT_COLOR_MINING,
                                v -> mineR = v, v -> mineG = v, v -> mineB = v, v -> mineA = v));
                channelRow(center - 155, 112, interR, interG, interB, interA,
                        v -> interR = v, v -> interG = v, v -> interB = v, v -> interA = v,
                        () -> applyColor(Config.DEFAULT_COLOR_INTERACTION,
                                v -> interR = v, v -> interG = v, v -> interB = v, v -> interA = v));
                channelRow(center - 155, 160, sheepR0, sheepG0, sheepB0, sheepA0,
                        v -> sheepR0 = v, v -> sheepG0 = v, v -> sheepB0 = v, v -> sheepA0 = v,
                        () -> applyColor(Config.DEFAULT_COLOR_SHEEP,
                                v -> sheepR0 = v, v -> sheepG0 = v, v -> sheepB0 = v, v -> sheepA0 = v));
            }
            case 2 -> {
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.particles", () -> particles, v -> particles = v);
                y += 34;
                slider(center - 150, y, 300, "screen.onekeyminer_chainable_visuals.option.particle_density", particleDensity, 0.1, 1.0, v -> particleDensity = v, 2);
            }
            default -> {
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.hud", () -> hud, v -> hud = v);
                toggle(center + 5, y, "screen.onekeyminer_chainable_visuals.option.sounds", () -> sounds, v -> sounds = v);
                y += 31;
                slider(center - 150, y, 300, "screen.onekeyminer_chainable_visuals.option.sound_volume", soundVolume, 0.0, 1.0, v -> soundVolume = v, 2);
                y += 31;
                toggle(center - 155, y, "screen.onekeyminer_chainable_visuals.option.sheep", () -> sheep, v -> sheep = v);
                y += 27;
                slider(center - 150, y, 300, "screen.onekeyminer_chainable_visuals.option.sheep_range", sheepRange, 4, 64, v -> sheepRange = (int) Math.round(v), 0);
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(center - 105, height - 34, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(center + 5, height - 34, 100, 20).build());
    }

    private void select(int next) {
        if (category == next) return;
        category = next;
        rebuildWidgets();
    }

    private void toggle(int x, int y, String key, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        Button button = Button.builder(toggleMessage(key, getter.get()), b -> {
            setter.accept(!getter.get());
            b.setMessage(toggleMessage(key, getter.get()));
        }).bounds(x, y, 150, 20).build();
        addRenderableWidget(button);
    }

    private Component toggleMessage(String key, boolean enabled) {
        return Component.translatable(key).append(enabled ? Component.literal("  ✓") : Component.literal("  —"));
    }

    private void slider(int x, int y, int w, String key, double initial, double min, double max,
                        Consumer<Double> changed, int decimals) {
        addRenderableWidget(new ValueSlider(x, y, w, 20, Component.translatable(key), initial, min, max, changed, decimals));
    }

    /**
     * Slider over a fixed set of steps (for the block cap a linear 2..4096 slider would put
     * "64" half a pixel away from "8" - unusable). The thumb snaps between steps.
     */
    private void steppedSlider(int x, int y, int w, String key, int[] steps, int current,
                               Consumer<Integer> changed) {
        int idx = 0;
        int bestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < steps.length; i++) {
            int diff = Math.abs(steps[i] - current);
            if (diff < bestDiff) {
                bestDiff = diff;
                idx = i;
            }
        }
        addRenderableWidget(new StepSlider(x, y, w, 20, Component.translatable(key), steps, current, idx, changed));
    }

    private static final int[] BLOCK_LIMIT_STEPS = {2, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096};

    /** One RGBA row: four 0-255 channel sliders side by side, then a per-group reset-to-default. */
    private void channelRow(int x, int y, int r, int g, int b, int a,
                            Consumer<Integer> setR, Consumer<Integer> setG,
                            Consumer<Integer> setB, Consumer<Integer> setA,
                            Runnable resetToDefault) {
        int w = 68;
        int step = 70;
        byteChannel(x, y, "R", r, setR);
        byteChannel(x + step, y, "G", g, setG);
        byteChannel(x + step * 2, y, "B", b, setB);
        byteChannel(x + step * 3, y, "A", a, setA);
        addRenderableWidget(Button.builder(Component.literal("\u21BA"), btn -> {
            resetToDefault.run();
            rebuildWidgets(); // sliders must re-read the restored channels
        }).bounds(x + step * 4, y, 24, 20).build());
    }

    private void byteChannel(int x, int y, String label, int current, Consumer<Integer> changed) {
        addRenderableWidget(new ValueSlider(x, y, 68, 20, Component.literal(label), current, 0, 255,
                v -> changed.accept((int) Math.round(v)), 0));
    }

    /** Splits an ARGB default back into the four 0-255 fields of one colour group. */
    private static void applyColor(int argb, Consumer<Integer> setR, Consumer<Integer> setG,
                                   Consumer<Integer> setB, Consumer<Integer> setA) {
        setA.accept((argb >>> 24) & 0xFF);
        setR.accept((argb >> 16) & 0xFF);
        setG.accept((argb >> 8) & 0xFF);
        setB.accept(argb & 0xFF);
    }

    private void saveAndClose() {
        boolean mergedBefore = Config.MERGED_OUTLINE.get();
        Config.SHOW_MINING.set(showMining);
        Config.SHOW_INTERACTION.set(showInteraction);
        Config.MERGED_OUTLINE.set(merged);
        Config.OUTLINE_MODE.set(outline);
        Config.SHOW_THROUGH_WALLS.set(xray);
        Config.DIAGONAL_CHAINING.set(diagonal);
        Config.FOLLOW_MAIN_MOD.set(followMain);
        Config.LINE_ALPHA.set(opacity);
        Config.MAX_CHAIN_BLOCKS.set(maxBlocks);
        Config.MAX_CHAIN_DEPTH.set(maxDepth);
        Config.SHOW_PARTICLES.set(particles);
        Config.PARTICLE_ALPHA.set(particleDensity);
        Config.SHOW_HUD.set(hud);
        Config.PLAY_CHAIN_SOUNDS.set(sounds);
        Config.SOUND_VOLUME.set(soundVolume);
        Config.SHOW_SHEEP.set(sheep);
        Config.SHEEP_RANGE.set(sheepRange);
        Config.REFRESH_TICKS.set(refreshTicks);
        Config.CUSTOM_RENDER_PIPELINE.set(customPipeline);
        ClientVisualEvents.resetRenderFault();
        // Only re-enabling per-block mode makes a fresh auto-merge notice meaningful; a plain
        // settings save must not re-ping the chat (r15 audit report issue 8).
        if (mergedBefore && !merged) ClientVisualEvents.rearmAutoMergeNotice();
        Config.COLOR_MINING.set((mineA << 24) | (mineR << 16) | (mineG << 8) | mineB);
        Config.COLOR_INTERACTION.set((interA << 24) | (interR << 16) | (interG << 8) | interB);
        Config.COLOR_SHEEP.set((sheepA0 << 24) | (sheepR0 << 16) | (sheepG0 << 8) | sheepB0);
        Config.SPEC.save();
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        Component subtitle;
        if (category == 0) {
            subtitle = Component.translatable(ChainPositionBridge.isMainModPresent()
                    ? "screen.onekeyminer_chainable_visuals.subtitle.main_found"
                    : "screen.onekeyminer_chainable_visuals.subtitle.standalone");
        } else if (category == 1) {
            subtitle = Component.translatable("screen.onekeyminer_chainable_visuals.subtitle.colors");
        } else {
            subtitle = Component.translatable("screen.onekeyminer_chainable_visuals.subtitle");
        }
        if (category == 1) {
            int center = width / 2;
            String[] groups = {"screen.onekeyminer_chainable_visuals.color_group.mining",
                    "screen.onekeyminer_chainable_visuals.color_group.interaction", "screen.onekeyminer_chainable_visuals.color_group.sheep"};
            int[] swatches = {(mineA << 24) | (mineR << 16) | (mineG << 8) | mineB,
                    (interA << 24) | (interR << 16) | (interG << 8) | interB,
                    (sheepA0 << 24) | (sheepR0 << 16) | (sheepG0 << 8) | sheepB0};
            int y = 64;
            for (int i = 0; i < 3; i++, y += 48) {
                graphics.drawString(font, Component.translatable(groups[i]), center - 155, y - 12, 0xB0B0B0);
                graphics.fill(center + 2, y - 13, center + 70, y - 2, swatches[i]);
                graphics.renderOutline(center + 2, y - 13, 68, 12, 0x80000000);
                // #AARRGGBB next to the swatch: the TOML stores the same value as a signed
                // int (white shows as -1), so without this the file and the screen never
                // looked like the same colour (audit P2).
                graphics.drawString(font, String.format(java.util.Locale.ROOT, "#%08X", swatches[i]),
                        center + 74, y - 12, 0xB0B0B0);
            }
        }
        // Live-hint block under the last control row. It has priority over the generic
        // subtitle and must never reach into the Done/Cancel row (audit P3: at GUI height
        // 240 the warning line used to sit on top of the buttons).
        int hintBottom = 0;
        if (category == 0 && followingInfoY > 0) {
            int center = width / 2;
            int yCur = followingInfoY;
            boolean valuesDrawn = false;
            if (followMain) {
                ChainPositionBridge.MainPolicy policy = ChainPositionBridge.policy();
                if (policy != null) {
                    Component values = Component.translatable("screen.onekeyminer_chainable_visuals.option.following_values",
                            policy.maxBlocks(), policy.maxDistance(),
                            Component.translatable(policy.allowDiagonal()
                                    ? "screen.onekeyminer_chainable_visuals.on" : "screen.onekeyminer_chainable_visuals.off"))
                            .append(Component.literal(" · ").append(Component.translatable(policy.fromServer()
                                    ? "screen.onekeyminer_chainable_visuals.source_server"
                                    : "screen.onekeyminer_chainable_visuals.source_local")));
                    graphics.drawString(font, values, center - 155, yCur, 0xB7E4A0);
                    valuesDrawn = true;
                    hintBottom = yCur + 10;
                    yCur += 12;
                }
            }
            if (xray && !customPipeline) {
                // Compat mode falls back to vanilla RenderType.lines(), which always depth-tests -
                // "through walls" genuinely cannot work there, so stop it failing silently.
                if (yCur + 10 > height - 34) yCur = height - 44;
                graphics.drawString(font, Component.translatable("screen.onekeyminer_chainable_visuals.xray_needs_pipeline"),
                        center - 155, yCur, 0xFFB86E);
                hintBottom = yCur + 10;
            }
        }
        if (category == 2) {
            graphics.drawCenteredString(font, Component.translatable("screen.onekeyminer_chainable_visuals.lines_hint"),
                    width / 2, Math.min(128, height - 64), 0xA0A0A0);
        }
        if (hintBottom + 3 <= height - 55) {
            graphics.drawCenteredString(font, subtitle, width / 2, height - 55,
                    ChainPositionBridge.isMainModPresent() && category == 0 ? 0x7FE380 : 0xA0A0A0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final Component label;
        private final int[] steps;
        private final Consumer<Integer> changed;
        /** Stored value shown verbatim until the first drag: with a hand-edited 100 in the
         *  TOML, displaying the nearest step (128) while the real value stays 100 read as a
         *  lying label (audit P3). Once touched the slider only ever sits on real steps. */
        private Integer offStepValue;

        private StepSlider(int x, int y, int width, int height, Component label,
                           int[] steps, int current, int index, Consumer<Integer> changed) {
            super(x, y, width, height, Component.empty(), index / (double) (steps.length - 1));
            this.label = label;
            this.steps = steps;
            this.changed = changed;
            boolean onStep = false;
            for (int step : steps) {
                if (step == current) {
                    onStep = true;
                    break;
                }
            }
            offStepValue = onStep ? null : current;
            updateMessage();
        }

        private int stepIndex() {
            return (int) Math.round(value * (steps.length - 1));
        }

        @Override
        protected void updateMessage() {
            int shown = offStepValue != null ? offStepValue : steps[stepIndex()];
            setMessage(label.copy().append(": " + shown));
        }

        @Override
        protected void applyValue() {
            offStepValue = null;
            changed.accept(steps[stepIndex()]);
        }
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final Component label;
        private final double min;
        private final double max;
        private final Consumer<Double> changed;
        private final int decimals;

        private ValueSlider(int x, int y, int width, int height, Component label, double current,
                            double min, double max, Consumer<Double> changed, int decimals) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.changed = changed;
            this.decimals = decimals;
            updateMessage();
        }

        private double actual() { return min + value * (max - min); }

        @Override
        protected void updateMessage() {
            String number = decimals == 0 ? String.format(Locale.ROOT, "%.0f", actual())
                    : String.format(Locale.ROOT, "%1$." + decimals + "f", actual());
            setMessage(label.copy().append(": " + number));
        }

        @Override
        protected void applyValue() {
            changed.accept(actual());
        }
    }
}

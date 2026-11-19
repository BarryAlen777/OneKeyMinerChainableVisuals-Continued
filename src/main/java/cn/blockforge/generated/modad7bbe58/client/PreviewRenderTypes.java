package cn.blockforge.generated.modad7bbe58.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/** Builds Forge 1.20.1 line layers; shards are exposed only inside this subclass. */
final class PreviewRenderTypes extends RenderStateShard {
    static final RenderType DEPTH_LINES = createLines(false);
    static final RenderType XRAY_LINES = createLines(true);

    private PreviewRenderTypes() {
        super("chain_preview", () -> { }, () -> { });
    }

    private static RenderType createLines(boolean xray) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_LINES_SHADER)
                .setTextureState(NO_TEXTURE)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(xray ? NO_DEPTH_TEST : LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                // LINES expands each segment in the vanilla shader, using this screen-space width.
                .setLineState(new LineStateShard(OptionalDouble.of(2.0D)))
                .setOutputState(MAIN_TARGET)
                .createCompositeState(false);
        // bufferSize semantics, verified against the 1.20.1 official-mapping bytecode
        // (audit P3 said our old "4096 vertices" comment had the wrong unit - correct):
        // BufferBuilder(n) pre-allocates n * 6 BYTES (MemoryTracker.create(n * 6)), so this
        // 4096 is a 24 KB hint (~870 POSITION_COLOR_NORMAL vertices), not a vertex count -
        // vanilla lines() passes 256, i.e. 1.5 KB. Also note the number only sizes builders
        // created from this RenderType directly; the immediate bufferSource we draw through
        // shares one transient builder that grows in whole 2 MB steps (BufferBuilder's
        // GROWTH_SIZE) and keeps the grown capacity, so big previews never saw a dozen
        // regrow passes to begin with. 4096 is kept as a sane per-type hint for pools that
        // do honour it (chunk-style buffer packs, some Forge pipelines).
        return RenderType.create(xray ? "chain_preview_xray_lines" : "chain_preview_depth_lines",
                DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES,
                4096, false, false, state);
    }
}

package com.hughbone.eldenhorses.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

// Extends RenderStateShard for access to protected shard constants.
public final class EldenRenderTypes extends RenderStateShard {
    private EldenRenderTypes() {
        super("elden_horses_marker", () -> {}, () -> {});
    }

    private static final Function<ResourceLocation, RenderType> NO_DEPTH =
            Util.memoize(EldenRenderTypes::build);

    public static RenderType translucentNoDepth(ResourceLocation tex) {
        return NO_DEPTH.apply(tex);
    }

    private static RenderType build(ResourceLocation tex) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return RenderType.create("elden_horses_translucent_no_depth",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, true, true, state);
    }
}

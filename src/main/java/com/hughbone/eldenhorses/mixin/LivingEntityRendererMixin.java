package com.hughbone.eldenhorses.mixin;

import com.hughbone.eldenhorses.EldenHorsesConfig;
import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import com.hughbone.eldenhorses.client.EldenRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    private boolean elden_lowerOpacity = false;
    private LivingEntity elden_currentEntity = null;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void elden_renderHead(LivingEntity entity, float yaw, float pt, PoseStack pose,
                                  MultiBufferSource src, int packed, CallbackInfo ci) {
        if (!(entity instanceof Horse horse)) return;
        Minecraft mc = Minecraft.getInstance();
        // Skip GUI contexts (horse inventory preview, JEI, etc.). With
        // no-depth-write enabled and full-screen viewport, the translucent
        // horse would tint the whole screen with its texture color.
        if (mc.screen != null) return;
        if (!horse.hasPassenger(mc.player)) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (!HorseEldenArmorCap.of(horse).hasSpectralSteed()) return;
        elden_lowerOpacity = true;
        elden_currentEntity = entity;
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void elden_renderTail(LivingEntity entity, float yaw, float pt, PoseStack pose,
                                  MultiBufferSource src, int packed, CallbackInfo ci) {
        elden_currentEntity = null;
    }

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("STORE"), ordinal = 2)
    private boolean elden_modifyOpacityLocal(boolean original) {
        if (elden_lowerOpacity) {
            elden_lowerOpacity = false;
            return true;
        }
        return original;
    }

    // Swap the body's translucent buffer to a no-depth-write variant. The
    // vanilla translucent type writes depth, which causes block-break crack
    // overlays under the horse to fail the depth test (cracks invisible).
    // Skipping depth write fixes that without dropping the horse from the
    // frame.
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer elden_swapBodyBuffer(MultiBufferSource src, RenderType rt) {
        if (elden_currentEntity == null) return src.getBuffer(rt);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ResourceLocation tex = ((LivingEntityRenderer) (Object) this).getTextureLocation(elden_currentEntity);
        return src.getBuffer(EldenRenderTypes.translucentNoDepth(tex));
    }

    // Vanilla hardcodes body alpha at 0.15 for the translucent path. Expose
    // it as a config knob so users can tune ghost-density independently of
    // the armor alpha.
    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7)
    private float elden_modifyBodyAlpha(float original) {
        if (elden_currentEntity == null) return original;
        return (float) EldenHorsesConfig.bodyTransparencyAlpha;
    }
}

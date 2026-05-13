package com.hughbone.eldenhorses.mixin;

import com.hughbone.eldenhorses.EldenHorsesConfig;
import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import com.hughbone.eldenhorses.client.ClientAnimationHooks;
import com.hughbone.eldenhorses.client.EldenRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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
    private float elden_fadeAlpha = -1f;
    private boolean elden_pushedMountPose = false;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void elden_renderHead(LivingEntity entity, float yaw, float pt, PoseStack pose,
                                  MultiBufferSource src, int packed, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer) {
            float offset = ClientAnimationHooks.getMountAnimOffset(entity.getId(), pt);
            if (offset != 0f) {
                pose.pushPose();
                pose.translate(0.0, -offset, 0.0);
                elden_pushedMountPose = true;
            }
            return;
        }

        if (!(entity instanceof Horse horse)) return;
        Minecraft mc = Minecraft.getInstance();

        if (ClientAnimationHooks.isFading(horse.getId())) {
            elden_fadeAlpha = ClientAnimationHooks.getFadeAlpha(horse.getId());
            elden_lowerOpacity = true;
            elden_currentEntity = entity;
            return;
        }

        // Skip GUI contexts (inventory preview would tint the screen)
        if (mc.screen != null) return;
        if (!horse.hasPassenger(mc.player)) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (!HorseEldenArmorCap.of(horse).hasSpectralSteed()) return;
        elden_lowerOpacity = true;
        elden_currentEntity = entity;
        elden_fadeAlpha = -1f;
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void elden_renderTail(LivingEntity entity, float yaw, float pt, PoseStack pose,
                                  MultiBufferSource src, int packed, CallbackInfo ci) {
        elden_currentEntity = null;
        elden_fadeAlpha = -1f;
        if (elden_pushedMountPose) {
            pose.popPose();
            elden_pushedMountPose = false;
        }
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

    // No-depth-write so block-break cracks under the horse stay visible
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

    @ModifyArg(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7)
    private float elden_modifyBodyAlpha(float original) {
        if (elden_currentEntity == null) return original;
        if (elden_fadeAlpha >= 0f) return elden_fadeAlpha;
        return (float) EldenHorsesConfig.bodyTransparencyAlpha;
    }
}

package com.hughbone.eldenhorses.mixin;

import com.hughbone.eldenhorses.EldenHorsesConfig;
import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import com.hughbone.eldenhorses.client.ClientAnimationHooks;
import com.hughbone.eldenhorses.client.EldenRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Priority 1500: applied after Shiny Horses (1000) so our @ModifyVariable runs last.
@Mixin(value = HorseArmorLayer.class, priority = 1500)
public abstract class HorseArmorLayerTransparencyMixin {

    @Unique private boolean elden_shouldLower = false;
    @Unique private float elden_fadeAlpha = -1f;

    @Inject(method = "render", at = @At("HEAD"))
    private void elden_renderHead(PoseStack pose, MultiBufferSource src,
                                  int packedLight, Horse horse,
                                  float limbSwing, float limbSwingAmount,
                                  float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch,
                                  CallbackInfo ci) {
        elden_shouldLower = false;
        elden_fadeAlpha = -1f;

        if (ClientAnimationHooks.isFading(horse.getId())) {
            elden_shouldLower = true;
            elden_fadeAlpha = ClientAnimationHooks.getFadeAlpha(horse.getId());
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Skip GUI contexts (horse inventory preview); see body mixin.
        if (mc.screen != null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (!horse.hasPassenger(mc.player)) return;
        if (!HorseEldenArmorCap.of(horse).hasSpectralSteed()) return;
        elden_shouldLower = true;
    }

    @ModifyVariable(
            method = "render",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            argsOnly = false)
    private VertexConsumer elden_replaceBuffer(VertexConsumer afterShiny,
                                               PoseStack pose,
                                               MultiBufferSource src,
                                               int packedLight,
                                               Horse horse,
                                               float limbSwing, float limbSwingAmount,
                                               float partialTicks, float ageInTicks,
                                               float netHeadYaw, float headPitch) {
        if (!elden_shouldLower) return afterShiny;
        ItemStack armorStack = horse.getArmor();
        if (!(armorStack.getItem() instanceof HorseArmorItem armorItem)) return afterShiny;
        ResourceLocation tex = armorItem.getTexture();
        if (tex == null) return afterShiny;

        VertexConsumer translucent = src.getBuffer(EldenRenderTypes.translucentNoDepth(tex));
        // Preserve glint if Shiny Horses (or vanilla) would have shown it.
        if (armorItem.isFoil(armorStack)) {
            return VertexMultiConsumer.create(src.getBuffer(RenderType.entityGlint()), translucent);
        }
        return translucent;
    }

    @ModifyArg(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HorseModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7)
    private float elden_modifyAlpha(float original) {
        if (!elden_shouldLower) return original;
        if (elden_fadeAlpha >= 0f) return elden_fadeAlpha;
        return (float) EldenHorsesConfig.transparencyAlpha;
    }
}

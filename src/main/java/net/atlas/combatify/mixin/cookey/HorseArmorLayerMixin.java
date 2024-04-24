package net.atlas.combatify.mixin.cookey;

import com.mojang.blaze3d.vertex.PoseStack;
import net.atlas.combatify.CombatifyClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HorseArmorLayer;
import net.minecraft.world.entity.animal.horse.Horse;
import net.atlas.combatify.config.cookey.option.BooleanOption;
import net.atlas.combatify.extensions.OverlayRendered;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(HorseArmorLayer.class)
public abstract class HorseArmorLayerMixin implements OverlayRendered<Horse> {
    @Shadow
    public abstract void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Horse horse, float f, float g, float h, float j, float k, float l);


    int overlayCoords;

    BooleanOption showDamageTintOnArmor = CombatifyClient.getInstance().getConfig().hudRendering().showDamageTintOnArmor();

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HorseModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"), index = 3)
    public int modifyOverlayCoords(int previousCoords) {
        boolean show = this.showDamageTintOnArmor.get();
        return show ? this.overlayCoords : previousCoords;
    }

    @Override
    public void renderWithOverlay(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Horse entity, float f, float g, float h, float j, float k, float l, int overlayCoords) {
        this.overlayCoords = overlayCoords;
        this.render(poseStack, multiBufferSource, i, entity, f, g, h, j, k, l);
    }
}
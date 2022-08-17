package net.alexandra.atlas.atlas_combat.mixin;

import io.netty.buffer.Unpooled;
import net.alexandra.atlas.atlas_combat.extensions.IMinecraft;
import net.alexandra.atlas.atlas_combat.extensions.IOptions;
import net.alexandra.atlas.atlas_combat.extensions.PlayerExtensions;
import net.fabricmc.fabric.impl.screenhandler.client.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements IMinecraft {
	@Shadow
	@Final
	public Options options;

	@Shadow
	protected abstract boolean startAttack();

	@Shadow
	@Nullable
	public LocalPlayer player;

	@Shadow
	protected abstract void continueAttack(boolean b);

	@Shadow
	@Nullable
	public HitResult hitResult;
	@Shadow
	private int rightClickDelay;
	@Shadow
	@Final
	private static Logger LOGGER;

	@Shadow
	@Nullable
	public MultiPlayerGameMode gameMode;

	@Shadow
	@Nullable
	public ClientLevel level;

	@Shadow
	public abstract @org.jetbrains.annotations.Nullable Entity getCameraEntity();

	@Shadow
	@org.jetbrains.annotations.Nullable
	public Entity crosshairPickEntity;

	@Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"))
	public void redirectContinueAttack(Minecraft instance, boolean b) {
		if(hitResult.getType() == HitResult.Type.BLOCK) {
			continueAttack(b);
		}else {
			if (b && ((IOptions) options).autoAttack().get()) {
				if((((PlayerExtensions)player).getMissedAttackRecovery())){
					if (((PlayerExtensions) player).isAttackAvailable(1.0F, 1.2F, true)) {
						startAttack();
					}
				}else{
					if (((PlayerExtensions) player).isAttackAvailable(1.0F, 1.0F, true)) {
						startAttack();
					}
				}
			} else {
				continueAttack(b);
			}
		}
	}
	@ModifyConstant(method = "startAttack", constant = @Constant(intValue = 10))
	public int redirectMissPenalty(int constant) {
		return 4;
	}
	@Inject(method = "startAttack", at = @At(value = "HEAD"), cancellable = true)
	private void injectDelay(CallbackInfoReturnable<Boolean> cir){
		assert player != null;
		for(InteractionHand hand : InteractionHand.values()) {
			if (player.isUsingItem() || (((IOptions)options).shieldCrouch().get() && player.isCrouching()) && player.getItemInHand(hand).getItem() instanceof ShieldItem) {
				((PlayerExtensions) player).customShieldInteractions(1.0F);
			}
		}
		if((((PlayerExtensions)player).getMissedAttackRecovery())) {
			if(!(((PlayerExtensions) player).isAttackAvailable(1.0F, 1.2F))){
				cir.setReturnValue(false);
				cir.cancel();
			}
		}else if (!(((PlayerExtensions) player).isAttackAvailable(1.0F))) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}
	@Redirect(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"))
	public final HitResult.Type redirectResult(HitResult instance) {
		HitResult.Type type = instance.getType();
		if(type == HitResult.Type.BLOCK) {
			BlockHitResult blockHitResult = (BlockHitResult)instance;
			BlockPos blockPos = blockHitResult.getBlockPos();
			boolean bl = !level.getBlockState(blockPos).canOcclude() && !level.getBlockState(blockPos).getBlock().hasCollision;
			Entity entity = crosshairPickEntity;
			assert entity != null;
			if (bl && entity.distanceToSqr(player.getEyePosition()) < ((PlayerExtensions)player).getSquaredAttackRange(player, Mth.square(6.0))) {
				hitResult = new EntityHitResult(entity);
				return hitResult.getType();
			}else {
				return type;
			}

		}else {
			return type;
		}
	}
	@Unique
	@Override
	public final void startUseItem(InteractionHand interactionHand) {
		if (!gameMode.isDestroying()) {
			this.rightClickDelay = 4;
			if (!this.player.isHandsBusy()) {
				if (this.hitResult == null) {
					LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
				}
					ItemStack itemStack = this.player.getItemInHand(interactionHand);
					if (!itemStack.isEmpty()) {
						InteractionResult interactionResult3 = this.gameMode.useItem(this.player, interactionHand);
						if (interactionResult3.consumesAction()) {
							if (interactionResult3.shouldSwing()) {
								this.player.swing(interactionHand);
							}
							return;
						}
					}
				}
		}
	}
}

package net.atlas.combatify.networking;

import net.atlas.combatify.Combatify;
import net.atlas.combatify.extensions.ServerPlayerExtensions;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;

import java.util.Timer;
import java.util.TimerTask;

import static net.atlas.combatify.Combatify.*;

public class NetworkingHandler {

	public NetworkingHandler() {

		ServerPlayNetworking.registerGlobalReceiver(modDetectionNetworkChannel,(server, player, handler, buf, responseSender) -> {
		});
		ServerPlayConnectionEvents.DISCONNECT.register(modDetectionNetworkChannel, (handler, server) -> {
			Timer timer = scheduleHitResult.get(handler.getPlayer().getUUID());
			timer.cancel();
			timer.purge();
		});
		ServerPlayConnectionEvents.JOIN.register(modDetectionNetworkChannel,(handler, sender, server) -> {
			boolean bl = CONFIG.configOnlyWeapons() || CONFIG.defender() || CONFIG.piercer() || !CONFIG.letVanillaConnect();
			if(!ServerPlayNetworking.canSend(handler.player, modDetectionNetworkChannel)) {
				if(bl) {
					handler.player.connection.disconnect(Component.literal("Combatify needs to be installed on the client to join this server!"));
					return;
				}
				Combatify.unmoddedPlayers.add(handler.player.getUUID());
				Combatify.isPlayerAttacking.put(handler.player.getUUID(), true);
				Combatify.finalizingAttack.put(handler.player.getUUID(), true);
				scheduleHitResult.put(handler.player.getUUID(), new Timer());
				scheduleHitResult.get(handler.player.getUUID()).schedule(new TimerTask() {
					@Override
					public void run() {
						ServerPlayer player = handler.player;
						if (player != null) {
							Entity camera = player.getCamera();
							ServerPlayerExtensions serverPlayer = ((ServerPlayerExtensions) player);
							serverPlayer.adjustHitResults(serverPlayer.pickResult(camera));
						}
					}
				}, 0, 10);
				return;
			}
			if (unmoddedPlayers.contains(handler.player.getUUID())) {
				unmoddedPlayers.remove(handler.player.getUUID());
				isPlayerAttacking.remove(handler.player.getUUID());
				finalizingAttack.remove(handler.player.getUUID());
			}
		});
		AttackBlockCallback.EVENT.register(modDetectionNetworkChannel, (player, world, hand, pos, direction) -> {
			if (Combatify.unmoddedPlayers.contains(player.getUUID()) && finalizingAttack.get(player.getUUID()))
				return InteractionResult.FAIL;
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register(modDetectionNetworkChannel, (player, world, hand, hitResult) -> {
			if(Combatify.unmoddedPlayers.contains(player.getUUID()))
				Combatify.isPlayerAttacking.put(player.getUUID(), false);
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register(modDetectionNetworkChannel, (player, world, hand, entity, hitResult) -> {
			if(Combatify.unmoddedPlayers.contains(player.getUUID()))
				Combatify.isPlayerAttacking.put(player.getUUID(), false);
			return InteractionResult.PASS;
		});
		UseItemCallback.EVENT.register(modDetectionNetworkChannel, (player, world, hand) -> {
			if(Combatify.unmoddedPlayers.contains(player.getUUID()))
				Combatify.isPlayerAttacking.put(player.getUUID(), false);
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		});
	}
}

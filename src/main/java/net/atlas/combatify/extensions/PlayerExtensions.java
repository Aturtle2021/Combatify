package net.atlas.combatify.extensions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;

public interface PlayerExtensions extends ClientInformationHolder {
	boolean isAttackAvailable(float baseTime);

	void customSwing(InteractionHand interactionHand);

	void resetAttackStrengthTicker(boolean var1);

	default boolean ctsShieldDisable(float damage, Item item) {
		return false;
	}
	boolean getMissedAttackRecovery();

    void attackAir();

	int getAttackStrengthStartValue();
}

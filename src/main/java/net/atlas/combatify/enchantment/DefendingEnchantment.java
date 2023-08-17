package net.atlas.combatify.enchantment;

import net.atlas.combatify.extensions.CustomEnchantment;
import net.atlas.combatify.extensions.IShieldItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import static net.atlas.combatify.Combatify.id;

public class DefendingEnchantment extends Enchantment implements CustomEnchantment {
	public static final Enchantment DEFENDER = EnchantmentRegistry.registerEnchant(id("defender"), new DefendingEnchantment());

	public DefendingEnchantment() {
		super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
	}

	@Override
	public int getMinCost(int level) {
		return level * 25;
	}

	@Override
	public int getMaxCost(int level) {
		return this.getMinCost(level) + 50;
	}

	@Override
	public boolean canEnchant(ItemStack stack) {
		return stack.getItem() instanceof IShieldItem;
	}

	@Override
	public boolean isAcceptibleConditions(ItemStack stack) {
		return this.canEnchant(stack);
	}

	@Override
	public boolean isAcceptibleAnvil(ItemStack stack) {
		return this.canEnchant(stack);
	}
	public static void registerEnchants() {

	}
}
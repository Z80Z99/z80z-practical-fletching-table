package cn.blockforge.generated.practicalfletching.item;

import cn.blockforge.generated.practicalfletching.entity.CraftedArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class CraftedArrowItem extends ArrowItem {
    private static final String[] ENCHANT_KEYS = {
            "Piercing", "Acceleration", "Stability", "Shatter",
            "Critical", "Venom", "Incineration", "Lifesteal"
    };

    public CraftedArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        CraftedArrowEntity arrow = new CraftedArrowEntity(level, shooter);
        arrow.configure(stack, shooter);
        return arrow;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (stack.hasTag()) for (String key : ENCHANT_KEYS) if (stack.getTag().getInt(key) > 0) return true;
        return super.isFoil(stack);
    }

    /** 追加一条道具效果说明（只在该效果存在时显示）。 */
    private static void appendEffectLine(List<Component> tooltip, ItemStack stack, String key, String langKey, ChatFormatting color) {
        double power = stack.getTag().getDouble(key);
        if (power > 0) tooltip.add(Component.translatable(langKey).withStyle(color));
    }

    /** 把箭头上存的道具 ID 翻译成显示名；none/wood/potion 为特殊标记。 */
    private static Component tipName(String tipId) {
        if ("none".equals(tipId)) return Component.translatable("tooltip.z80z_practical_fletching.tip.none");
        if ("wood".equals(tipId)) return Component.translatable("tooltip.z80z_practical_fletching.tip.wood");
        if ("potion".equals(tipId)) return Component.translatable("tooltip.z80z_practical_fletching.tip.potion");
        ResourceLocation id = ResourceLocation.tryParse(tipId);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null ? Component.literal(tipId) : new ItemStack(item).getHoverName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!stack.hasTag()) return;
        tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.tip", tipName(stack.getTag().getString("Tip"))).withStyle(ChatFormatting.GRAY));
        double bonus = stack.getTag().getDouble("BonusDamage");
        if (bonus > 0) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.damage", bonus).withStyle(ChatFormatting.RED));
        int drop = stack.getTag().getInt("Drop");
        if (drop != 0) {
            tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.drop",
                    (drop > 0 ? "+" : "") + drop + "%").withStyle(drop > 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }
        if (stack.getTag().contains("ItemBoost") && stack.getTag().getDouble("ItemBoost") > 1.0D) {
            tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.itemboost",
                    (int) Math.round((stack.getTag().getDouble("ItemBoost") - 1.0D) * 100) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stack.getTag().contains("EnchantBoost") && stack.getTag().getDouble("EnchantBoost") > 1.0D) {
            tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.enchantboost",
                    (int) Math.round((stack.getTag().getDouble("EnchantBoost") - 1.0D) * 100) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stack.getTag().getBoolean("PotionOnly")) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.potion_only").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (stack.getTag().getInt("Rocket") > 0) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.rocket").withStyle(ChatFormatting.AQUA));
        if (stack.getTag().getInt("Flame") > 0) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.flame").withStyle(ChatFormatting.GOLD));
        if (stack.getTag().getInt("Explosion") > 0) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.explosion").withStyle(ChatFormatting.DARK_RED));
        if (stack.getTag().getInt("Glow") > 0) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.glow").withStyle(ChatFormatting.YELLOW));
        if (stack.getTag().getInt("Knockback") > 0) tooltip.add(Component.translatable("tooltip.z80z_practical_fletching.knockback").withStyle(ChatFormatting.BLUE));
        appendEffectLine(tooltip, stack, "RocketPower", "tooltip.z80z_practical_fletching.fx.rocket", ChatFormatting.AQUA);
        appendEffectLine(tooltip, stack, "BouncePower", "tooltip.z80z_practical_fletching.fx.bounce", ChatFormatting.GREEN);
        appendEffectLine(tooltip, stack, "EchoPower", "tooltip.z80z_practical_fletching.fx.echo", ChatFormatting.AQUA);
        appendEffectLine(tooltip, stack, "PearlPower", "tooltip.z80z_practical_fletching.fx.pearl", ChatFormatting.DARK_PURPLE);
        appendEffectLine(tooltip, stack, "ChorusPower", "tooltip.z80z_practical_fletching.fx.chorus", ChatFormatting.LIGHT_PURPLE);
        appendEffectLine(tooltip, stack, "YankPower", "tooltip.z80z_practical_fletching.fx.yank", ChatFormatting.GREEN);
        appendEffectLine(tooltip, stack, "BellPower", "tooltip.z80z_practical_fletching.fx.bell", ChatFormatting.GOLD);
        appendEffectLine(tooltip, stack, "HornPower", "tooltip.z80z_practical_fletching.fx.horn", ChatFormatting.YELLOW);
        appendEffectLine(tooltip, stack, "StickPower", "tooltip.z80z_practical_fletching.fx.stick", ChatFormatting.GOLD);
        appendEffectLine(tooltip, stack, "FirePower", "tooltip.z80z_practical_fletching.fx.fire", ChatFormatting.GOLD);
        appendEffectLine(tooltip, stack, "WebPower", "tooltip.z80z_practical_fletching.fx.web", ChatFormatting.WHITE);
        appendEffectLine(tooltip, stack, "BonePower", "tooltip.z80z_practical_fletching.fx.bone", ChatFormatting.GREEN);
        appendEffectLine(tooltip, stack, "SnowPower", "tooltip.z80z_practical_fletching.fx.snow", ChatFormatting.AQUA);
        appendEffectLine(tooltip, stack, "LightPower", "tooltip.z80z_practical_fletching.fx.light", ChatFormatting.YELLOW);
        appendEffectLine(tooltip, stack, "ShockPower", "tooltip.z80z_practical_fletching.fx.shock", ChatFormatting.DARK_RED);
        appendEffectLine(tooltip, stack, "CloudPower", "tooltip.z80z_practical_fletching.fx.cloud", ChatFormatting.DARK_PURPLE);
        appendEffectLine(tooltip, stack, "PiercePower", "tooltip.z80z_practical_fletching.fx.pierce", ChatFormatting.RED);
        appendEffectLine(tooltip, stack, "ResonancePower", "tooltip.z80z_practical_fletching.fx.resonance", ChatFormatting.LIGHT_PURPLE);
        for (String key : ENCHANT_KEYS) {
            int enchantLevel = stack.getTag().getInt(key);
            if (enchantLevel > 0) tooltip.add(Component.translatable("enchantment.z80z_practical_fletching." + key.toLowerCase(), enchantLevel).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}

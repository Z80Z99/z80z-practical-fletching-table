package cn.blockforge.generated.mod70022c7a.item;

import cn.blockforge.generated.mod70022c7a.entity.CraftedArrowEntity;
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
        if ("none".equals(tipId)) return Component.translatable("tooltip.mod_70022c7a.tip.none");
        if ("wood".equals(tipId)) return Component.translatable("tooltip.mod_70022c7a.tip.wood");
        if ("potion".equals(tipId)) return Component.translatable("tooltip.mod_70022c7a.tip.potion");
        ResourceLocation id = ResourceLocation.tryParse(tipId);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null ? Component.literal(tipId) : new ItemStack(item).getHoverName();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!stack.hasTag()) return;
        tooltip.add(Component.translatable("tooltip.mod_70022c7a.tip", tipName(stack.getTag().getString("Tip"))).withStyle(ChatFormatting.GRAY));
        double bonus = stack.getTag().getDouble("BonusDamage");
        if (bonus > 0) tooltip.add(Component.translatable("tooltip.mod_70022c7a.damage", bonus).withStyle(ChatFormatting.RED));
        int drop = stack.getTag().getInt("Drop");
        if (drop != 0) {
            tooltip.add(Component.translatable("tooltip.mod_70022c7a.drop",
                    (drop > 0 ? "+" : "") + drop + "%").withStyle(drop > 0 ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }
        if (stack.getTag().contains("ItemBoost") && stack.getTag().getDouble("ItemBoost") > 1.0D) {
            tooltip.add(Component.translatable("tooltip.mod_70022c7a.itemboost",
                    (int) Math.round((stack.getTag().getDouble("ItemBoost") - 1.0D) * 100) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stack.getTag().contains("EnchantBoost") && stack.getTag().getDouble("EnchantBoost") > 1.0D) {
            tooltip.add(Component.translatable("tooltip.mod_70022c7a.enchantboost",
                    (int) Math.round((stack.getTag().getDouble("EnchantBoost") - 1.0D) * 100) + "%").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stack.getTag().getBoolean("PotionOnly")) tooltip.add(Component.translatable("tooltip.mod_70022c7a.potion_only").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (stack.getTag().getInt("Rocket") > 0) tooltip.add(Component.translatable("tooltip.mod_70022c7a.rocket").withStyle(ChatFormatting.AQUA));
        if (stack.getTag().getInt("Flame") > 0) tooltip.add(Component.translatable("tooltip.mod_70022c7a.flame").withStyle(ChatFormatting.GOLD));
        if (stack.getTag().getInt("Explosion") > 0) tooltip.add(Component.translatable("tooltip.mod_70022c7a.explosion").withStyle(ChatFormatting.DARK_RED));
        if (stack.getTag().getInt("Glow") > 0) tooltip.add(Component.translatable("tooltip.mod_70022c7a.glow").withStyle(ChatFormatting.YELLOW));
        if (stack.getTag().getInt("Knockback") > 0) tooltip.add(Component.translatable("tooltip.mod_70022c7a.knockback").withStyle(ChatFormatting.BLUE));
        appendEffectLine(tooltip, stack, "RocketPower", "tooltip.mod_70022c7a.fx.rocket", ChatFormatting.AQUA);
        appendEffectLine(tooltip, stack, "BouncePower", "tooltip.mod_70022c7a.fx.bounce", ChatFormatting.GREEN);
        appendEffectLine(tooltip, stack, "EchoPower", "tooltip.mod_70022c7a.fx.echo", ChatFormatting.AQUA);
        appendEffectLine(tooltip, stack, "PearlPower", "tooltip.mod_70022c7a.fx.pearl", ChatFormatting.DARK_PURPLE);
        appendEffectLine(tooltip, stack, "ChorusPower", "tooltip.mod_70022c7a.fx.chorus", ChatFormatting.LIGHT_PURPLE);
        appendEffectLine(tooltip, stack, "YankPower", "tooltip.mod_70022c7a.fx.yank", ChatFormatting.GREEN);
        appendEffectLine(tooltip, stack, "BellPower", "tooltip.mod_70022c7a.fx.bell", ChatFormatting.GOLD);
        appendEffectLine(tooltip, stack, "HornPower", "tooltip.mod_70022c7a.fx.horn", ChatFormatting.YELLOW);
        appendEffectLine(tooltip, stack, "StickPower", "tooltip.mod_70022c7a.fx.stick", ChatFormatting.GOLD);
        appendEffectLine(tooltip, stack, "FirePower", "tooltip.mod_70022c7a.fx.fire", ChatFormatting.GOLD);
        appendEffectLine(tooltip, stack, "WebPower", "tooltip.mod_70022c7a.fx.web", ChatFormatting.WHITE);
        appendEffectLine(tooltip, stack, "BonePower", "tooltip.mod_70022c7a.fx.bone", ChatFormatting.GREEN);
        appendEffectLine(tooltip, stack, "SnowPower", "tooltip.mod_70022c7a.fx.snow", ChatFormatting.AQUA);
        appendEffectLine(tooltip, stack, "LightPower", "tooltip.mod_70022c7a.fx.light", ChatFormatting.YELLOW);
        appendEffectLine(tooltip, stack, "ShockPower", "tooltip.mod_70022c7a.fx.shock", ChatFormatting.DARK_RED);
        appendEffectLine(tooltip, stack, "CloudPower", "tooltip.mod_70022c7a.fx.cloud", ChatFormatting.DARK_PURPLE);
        appendEffectLine(tooltip, stack, "PiercePower", "tooltip.mod_70022c7a.fx.pierce", ChatFormatting.RED);
        appendEffectLine(tooltip, stack, "ResonancePower", "tooltip.mod_70022c7a.fx.resonance", ChatFormatting.LIGHT_PURPLE);
        for (String key : ENCHANT_KEYS) {
            int enchantLevel = stack.getTag().getInt(key);
            if (enchantLevel > 0) tooltip.add(Component.translatable("enchantment.mod_70022c7a." + key.toLowerCase(), enchantLevel).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}

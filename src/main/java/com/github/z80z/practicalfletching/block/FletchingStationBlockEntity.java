package com.github.z80z.practicalfletching.block;

import com.github.z80z.practicalfletching.GeneratedMod;
import com.github.z80z.practicalfletching.menu.FletchingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.HashMap;
import java.util.Map;

public class FletchingStationBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_COUNT = 19;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int tier;
    private int enchantSeed;
    /** 批量材料的"已用支数"余量（物品 ID → 支数），零头累计，做满比例才消耗一份。 */
    private final Map<String, Integer> remainders = new HashMap<>();
    /** 已实际使用过的效果道具（物品 ID），用于图鉴逐步解锁。 */
    private final java.util.Set<String> usedEffects = new java.util.HashSet<>();
    /** 已使用道具的位掩码缓存（与 FletchingMenu.GUIDE_EFFECT_IDS 顺序对应）。 */
    private int usedMask;

    public FletchingStationBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratedMod.FLETCHING_STATION_ENTITY.get(), pos, state);
        enchantSeed = (int) (pos.asLong() ^ System.nanoTime());
    }

    public int getTier() { return tier; }

    public int getEnchantSeed() { return enchantSeed; }

    public int getRemainder(String itemId) {
        return remainders.getOrDefault(itemId, 0);
    }

    public void setRemainder(String itemId, int arrowsUsed) {
        if (arrowsUsed <= 0) remainders.remove(itemId);
        else remainders.put(itemId, arrowsUsed);
        setChanged();
    }

    public boolean isEffectUsed(String itemId) {
        return usedEffects.contains(itemId);
    }

    /** 记录某道具被实际用于制作，图鉴据此解锁该行。 */
    public void markEffectUsed(String itemId) {
        if (usedEffects.add(itemId)) {
            int idx = java.util.Arrays.asList(FletchingMenu.GUIDE_EFFECT_IDS).indexOf(itemId);
            if (idx >= 0) usedMask |= 1 << idx;
            setChanged();
        }
    }

    public int getUsedMask() {
        return usedMask;
    }

    /** 每次附魔后重抽种子，使下一次的三选一结果刷新。 */
    public void rerollEnchantSeed() {
        enchantSeed = (int) (worldPosition.asLong() ^ System.nanoTime());
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void upgrade() {
        if (tier < 5) {
            tier++;
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public Component getTierName() {
        return Component.translatable("tier.z80z_practical_fletching." + tier);
    }

    @Override protected Component getDefaultName() {
        return Component.translatable("container.z80z_practical_fletching.fletching_station");
    }

    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FletchingMenu(id, inventory, this);
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() {
        for (int i = 0; i < items.size(); i++) {
            if (isResultSlot(i)) continue;
            if (!items.get(i).isEmpty()) return false;
        }
        return true;
    }

    /** 结果/预览槽是计算产物，不参与存档、掉落与自动化抽取。 */
    private static boolean isResultSlot(int slot) {
        return slot == FletchingMenu.SLOT_CRAFT_OUTPUT || slot == FletchingMenu.SLOT_ENCHANT_OUTPUT;
    }

    /** 漏斗/管道：只允许向合法输入槽插入，禁止抽取结果槽与"最后一份被扣住"的材料。 */
    @Override
    protected IItemHandler createUnSidedHandler() {
        return new InvWrapper(this) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return mayInsertViaAutomation(slot) ? super.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return mayExtractViaAutomation(slot) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            private boolean mayInsertViaAutomation(int slot) {
                if (slot < 0 || slot >= SLOT_COUNT || isResultSlot(slot)) return false;
                if (slot == FletchingMenu.SLOT_BOW || slot == FletchingMenu.SLOT_UPGRADE) return false;
                int need = switch (slot) {
                    case FletchingMenu.SLOT_EFFECT_1 -> 1;
                    case FletchingMenu.SLOT_EFFECT_1 + 1 -> 2;
                    case FletchingMenu.SLOT_EFFECT_1 + 2 -> 3;
                    case FletchingMenu.SLOT_EFFECT_1 + 3 -> 4;
                    case FletchingMenu.SLOT_EFFECT_1 + 4 -> 5;
                    case FletchingMenu.SLOT_ENCHANT_ARROW, FletchingMenu.SLOT_ENCHANT_LAPIS -> 3;
                    default -> {
                        if (slot >= FletchingMenu.SLOT_BOW_EFFECT_1
                                && slot < FletchingMenu.SLOT_BOW_EFFECT_1 + FletchingMenu.BOW_EFFECT_COUNT) yield 5;
                        yield 0;
                    }
                };
                return tier >= need;
            }

            private boolean mayExtractViaAutomation(int slot) {
                if (slot < 0 || slot >= SLOT_COUNT || isResultSlot(slot)) return false;
                ItemStack stack = getItem(slot);
                return !(stack.getCount() == 1 && getRemainder(itemId(stack)) > 0);
            }
        };
    }

    private static String itemId(ItemStack stack) {
        var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }
    @Override public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < items.size(); i++) if (isResultSlot(i)) items.set(i, ItemStack.EMPTY);
        tier = Math.max(0, Math.min(5, tag.getInt("Tier")));
        enchantSeed = tag.getInt("EnchantSeed");
        remainders.clear();
        CompoundTag rem = tag.getCompound("Remainders");
        for (String key : rem.getAllKeys()) remainders.put(key, rem.getInt(key));
        usedEffects.clear();
        usedMask = 0;
        CompoundTag used = tag.getCompound("UsedEffects");
        for (String key : used.getAllKeys()) {
            usedEffects.add(key);
            int idx = java.util.Arrays.asList(FletchingMenu.GUIDE_EFFECT_IDS).indexOf(key);
            if (idx >= 0) usedMask |= 1 << idx;
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        NonNullList<ItemStack> saved = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) if (!isResultSlot(i)) saved.set(i, items.get(i));
        ContainerHelper.saveAllItems(tag, saved);
        writeProgress(tag);
    }

    /** 把等级、种子、余量与图鉴进度写入 tag（不含槽内物品）。 */
    public void writeProgress(CompoundTag tag) {
        tag.putInt("Tier", tier);
        tag.putInt("EnchantSeed", enchantSeed);
        CompoundTag rem = new CompoundTag();
        for (Map.Entry<String, Integer> entry : remainders.entrySet()) rem.putInt(entry.getKey(), entry.getValue());
        tag.put("Remainders", rem);
        CompoundTag used = new CompoundTag();
        for (String id : usedEffects) used.putBoolean(id, true);
        tag.put("UsedEffects", used);
    }

    /** 供破坏掉落携带的进度数据（等级 + 余量 + 图鉴）。 */
    public CompoundTag saveProgress() {
        CompoundTag tag = new CompoundTag();
        writeProgress(tag);
        return tag;
    }
}

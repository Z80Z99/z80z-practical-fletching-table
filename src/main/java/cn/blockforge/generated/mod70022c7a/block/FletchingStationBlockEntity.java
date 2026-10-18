package cn.blockforge.generated.mod70022c7a.block;

import cn.blockforge.generated.mod70022c7a.GeneratedMod;
import cn.blockforge.generated.mod70022c7a.menu.FletchingMenu;
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

import java.util.HashMap;
import java.util.Map;

public class FletchingStationBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_COUNT = 19;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int tier;
    private int enchantSeed;
    /** 批量材料的"已用支数"余量（物品 ID → 支数），零头累计，做满比例才消耗一份。 */
    private final Map<String, Integer> remainders = new HashMap<>();

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
        return Component.translatable("tier.mod_70022c7a." + tier);
    }

    @Override protected Component getDefaultName() {
        return Component.translatable("container.mod_70022c7a.fletching_station");
    }

    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FletchingMenu(id, inventory, this);
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty()) setChanged();
        return result;
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
        tier = Math.max(0, Math.min(5, tag.getInt("Tier")));
        enchantSeed = tag.getInt("EnchantSeed");
        remainders.clear();
        CompoundTag rem = tag.getCompound("Remainders");
        for (String key : rem.getAllKeys()) remainders.put(key, rem.getInt(key));
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Tier", tier);
        tag.putInt("EnchantSeed", enchantSeed);
        CompoundTag rem = new CompoundTag();
        for (Map.Entry<String, Integer> entry : remainders.entrySet()) rem.putInt(entry.getKey(), entry.getValue());
        tag.put("Remainders", rem);
    }
}

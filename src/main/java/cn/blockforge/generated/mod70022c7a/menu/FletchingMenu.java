package cn.blockforge.generated.mod70022c7a.menu;

import cn.blockforge.generated.mod70022c7a.GeneratedMod;
import cn.blockforge.generated.mod70022c7a.block.FletchingStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EnchantmentTableBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class FletchingMenu extends AbstractContainerMenu {
    public static final int SLOT_COUNT = 19;

    // 制作页
    public static final int SLOT_FEATHER = 0;
    public static final int SLOT_SHAFT = 1;
    public static final int SLOT_TIP = 2;
    public static final int SLOT_EFFECT_1 = 3;
    public static final int EFFECT_SLOT_COUNT = 5;
    public static final int SLOT_CRAFT_OUTPUT = 8;
    // 附魔页
    public static final int SLOT_ENCHANT_ARROW = 9;
    public static final int SLOT_ENCHANT_LAPIS = 10;
    public static final int SLOT_ENCHANT_OUTPUT = 11;
    // 弓改装页
    public static final int SLOT_BOW = 12;
    public static final int SLOT_BOW_EFFECT_1 = 13;
    public static final int BOW_EFFECT_COUNT = 5;
    // 升级
    public static final int SLOT_UPGRADE = 18;
    public static final int INV_START = SLOT_COUNT;

    public static final int TAB_CRAFT = 0;
    public static final int TAB_ENCHANT = 1;
    public static final int TAB_BOW = 2;

    private static final int DATA_TIER = 0;
    private static final int DATA_OFFER = 1;
    private static final int DATA_COST = 4;
    private static final int DATA_REM_SHAFT = 7;
    private static final int DATA_REM_TIP = 8;
    private static final int DATA_REM_EFFECT = 9;
    private static final int DATA_COUNT = 14;

    private static final String[] ENCHANT_KEYS = {
            "Piercing", "Acceleration", "Stability", "Shatter",
            "Critical", "Venom", "Incineration", "Lifesteal"
    };
    private static final Item[] UPGRADE_MATERIALS = {
            Items.COPPER_BLOCK, Items.IRON_BLOCK, Items.GOLD_BLOCK, Items.DIAMOND_BLOCK, Items.NETHERITE_BLOCK
    };

    private final Container container;
    private final ContainerData data;
    private final BlockPos stationPos;
    private final FletchingStationBlockEntity station;
    private final int shelves;
    private final int[] offers = new int[3];
    private final int[] costs = new int[3];
    private int tab = TAB_CRAFT;
    private boolean updatingOutput;

    public FletchingMenu(int id, Inventory inventory, net.minecraft.network.FriendlyByteBuf buffer) {
        this(id, inventory, null, buffer.readBlockPos());
    }

    public FletchingMenu(int id, Inventory inventory, FletchingStationBlockEntity station) {
        this(id, inventory, station, station.getBlockPos());
    }

    private FletchingMenu(int id, Inventory inventory, FletchingStationBlockEntity station, BlockPos pos) {
        super(GeneratedMod.FLETCHING_MENU.get(), id);
        this.station = station;
        this.stationPos = pos;
        this.container = station != null ? station : new SimpleContainer(SLOT_COUNT);
        this.shelves = station != null ? bookshelfCount(inventory.player) : 0;
        if (station != null) refreshOffers();
        this.data = station != null ? new ContainerData() {
            @Override public int get(int index) {
                if (index == DATA_TIER) return station.getTier();
                if (index >= DATA_OFFER && index < DATA_OFFER + 3) {
                    return station.getTier() >= 3 ? offers[index - DATA_OFFER] : 0;
                }
                if (index >= DATA_COST && index < DATA_COST + 3) return costs[index - DATA_COST];
                if (index == DATA_REM_SHAFT) return bulkRemainder(container.getItem(SLOT_SHAFT));
                if (index == DATA_REM_TIP) return bulkRemainder(container.getItem(SLOT_TIP));
                if (index >= DATA_REM_EFFECT && index < DATA_REM_EFFECT + EFFECT_SLOT_COUNT) {
                    return bulkRemainder(container.getItem(SLOT_EFFECT_1 + index - DATA_REM_EFFECT));
                }
                return 0;
            }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        } : new SimpleContainerData(DATA_COUNT);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        container.startOpen(inventory.player);

        // 制作页（槽位群在背包与右侧箭头之间水平居中：跨 124..312，中心 218）
        addSlot(new TabSlot(container, SLOT_FEATHER, 124, 92, TAB_CRAFT, 0, stack -> stack.is(Items.FEATHER)));
        addSlot(new TabSlot(container, SLOT_SHAFT, 148, 92, TAB_CRAFT, 0, stack -> stack.is(Items.STICK) || stack.is(ItemTags.PLANKS)));
        addSlot(new TabSlot(container, SLOT_TIP, 172, 92, TAB_CRAFT, 0, FletchingMenu::isTip));
        for (int i = 0; i < EFFECT_SLOT_COUNT; i++) {
            addSlot(new TabSlot(container, SLOT_EFFECT_1 + i, 196 + i * 18, 92, TAB_CRAFT, i + 1, FletchingMenu::isEffect));
        }
        addSlot(new CraftOutputSlot(container, SLOT_CRAFT_OUTPUT, 296, 92));
        // 附魔页
        addSlot(new TabSlot(container, SLOT_ENCHANT_ARROW, 130, 96, TAB_ENCHANT, 3,
                stack -> stack.is(GeneratedMod.CRAFTED_ARROW.get())));
        addSlot(new TabSlot(container, SLOT_ENCHANT_LAPIS, 154, 96, TAB_ENCHANT, 3, stack -> stack.is(Items.LAPIS_LAZULI)));
        addSlot(new EnchantOutputSlot(container, SLOT_ENCHANT_OUTPUT, 296, 96, TAB_ENCHANT, 3));
        // 弓改装页
        addSlot(new BowSlot(container, SLOT_BOW, 210, 84));
        for (int i = 0; i < BOW_EFFECT_COUNT; i++) {
            addSlot(new TabSlot(container, SLOT_BOW_EFFECT_1 + i, 173 + i * 18, 118, TAB_BOW, 5, FletchingMenu::isBowEffect));
        }
        // 升级槽（所有页共享，放入金属块即自动升级；满级时隐藏）
        addSlot(new UpgradeSlot(container, SLOT_UPGRADE, 296, 180));

        // 玩家背包：最左列为快捷栏，右侧三列为主背包（在面板内居中）
        for (int row = 0; row < 9; row++) addSlot(new Slot(inventory, row, 20, 36 + row * 18));
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 9; row++) addSlot(new Slot(inventory, 9 + col * 9 + row, 38 + col * 18, 36 + row * 18));
        }
        addDataSlots(data);
    }

    public int getTier() { return data.get(DATA_TIER); }
    public int getTab() { return tab; }
    public void setTab(int tab) { this.tab = tab; }
    public BlockPos getStationPos() { return stationPos; }
    public int getOffer(int index) { return data.get(DATA_OFFER + index); }
    public int getCost(int index) { return data.get(DATA_COST + index); }
    /** 箭杆槽（木板）/箭头槽（药水）/效果槽当前物品的已累计支数，供客户端悬停显示。 */
    public int getRemainderFor(int slotIndex) {
        if (slotIndex == SLOT_SHAFT) return data.get(DATA_REM_SHAFT);
        if (slotIndex == SLOT_TIP) return data.get(DATA_REM_TIP);
        if (slotIndex >= SLOT_EFFECT_1 && slotIndex < SLOT_EFFECT_1 + EFFECT_SLOT_COUNT) {
            return data.get(DATA_REM_EFFECT + slotIndex - SLOT_EFFECT_1);
        }
        return 0;
    }
    public Item nextUpgradeMaterial() {
        int next = getTier();
        return next < UPGRADE_MATERIALS.length ? UPGRADE_MATERIALS[next] : null;
    }

    private void refreshOffers() {
        RandomSource random = RandomSource.create(station != null ? station.getEnchantSeed() : 0);
        // 洗牌后取前三项，保证三条选项互不重复
        int[] keys = new int[ENCHANT_KEYS.length];
        for (int i = 0; i < keys.length; i++) keys[i] = i;
        for (int i = keys.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = keys[i];
            keys[i] = keys[j];
            keys[j] = tmp;
        }
        for (int i = 0; i < 3; i++) {
            int cost = i + 1 + random.nextInt(8) + (shelves >> 1) + random.nextInt(shelves + 1);
            cost = Math.max(cost, i + 1);
            int level = cost >= 20 ? 3 : cost >= 10 ? 2 : 1;
            offers[i] = (keys[i] + 1) * 10 + level;
            costs[i] = cost;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 50 && id <= 52) return applyOffer(player, id - 50);
        return false;
    }

    // ---------------------------------------------------------------- 制作页

    /** 每支箭的箭杆消耗：木棍 1 根；木板 1 块 = 8 支用量。 */
    private static final int PLANK_SHAFTS = 8;
    /** 效果材料基础比例：1 份 = 4 支。 */
    private static final int EFFECT_ARROW_RATIO = 4;
    /** 药水基础比例：1 瓶 = 8 支。 */
    private static final int POTION_ARROW_RATIO = 8;

    /** 本次可制作的箭数：每支消耗 1 羽毛 + 1 箭杆 + 1 箭头（留空不耗），效果材料按比例封顶。单次最多一组（64 支）。 */
    private int computeBatchSize(ItemStack feather, ItemStack shaft, ItemStack tip) {
        if (feather.getCount() < 1) return 0;
        int shafts = shaft.is(ItemTags.PLANKS)
                ? shaft.getCount() * PLANK_SHAFTS - bulkRemainder(shaft) : shaft.getCount();
        if (shafts < 1) return 0;
        int tips;
        if (tip.isEmpty()) tips = Integer.MAX_VALUE;
        else if (isPotion(tip)) tips = bulkSupply(tip, POTION_ARROW_RATIO);
        else {
            TipStats stats = tipStats(tip);
            tips = stats == null ? 0 : bulkSupply(tip, stats.supplies());
        }
        int cap = new ItemStack(GeneratedMod.CRAFTED_ARROW.get()).getMaxStackSize();
        int tier = getTier();
        int n = Math.min(Math.min(feather.getCount(), shafts), tips);
        for (int i = 0; i < EFFECT_SLOT_COUNT; i++) {
            ItemStack effect = container.getItem(SLOT_EFFECT_1 + i);
            if (effect.isEmpty()) continue;
            if (tier < i + 1) return 0; // 未解锁的效果槽有材料时不允许制作
            int ratio = isPotion(effect) ? POTION_ARROW_RATIO : EFFECT_ARROW_RATIO;
            n = Math.min(n, bulkSupply(effect, ratio));
        }
        return Math.max(0, Math.min(n, cap));
    }

    /** 批量材料的剩余可用支数 = 数量×比例 − 已用支数（余量已从配额中花掉，不能重复计入）。 */
    private int bulkSupply(ItemStack stack, int ratio) {
        return Math.max(0, stack.getCount() * ratio - bulkRemainder(stack));
    }

    /** 批量材料的已累计支数（服务端从台子余量表读；客户端恒 0，仅服务端用于预览与扣料）。 */
    private int bulkRemainder(ItemStack stack) {
        if (station == null || stack.isEmpty()) return 0;
        return station.getRemainder(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
    }

    /** 按累计支数扣料：做满比例才消耗一份，零头记入台子余量表。 */
    private void consumeBulk(ItemStack stack, int arrows, int ratio) {
        if (station == null) {
            stack.shrink((arrows + ratio - 1) / ratio);
            return;
        }
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        int used = station.getRemainder(id) + arrows;
        stack.shrink(used / ratio);
        station.setRemainder(id, used % ratio);
    }

    private ItemStack previewCraftResult() {
        ItemStack feather = container.getItem(SLOT_FEATHER);
        ItemStack shaft = container.getItem(SLOT_SHAFT);
        ItemStack tip = container.getItem(SLOT_TIP);
        int outputCount = computeBatchSize(feather, shaft, tip);
        if (outputCount < 1) return ItemStack.EMPTY;
        return createArrowBatch(tip, outputCount, Math.min(getTier(), EFFECT_SLOT_COUNT));
    }

    private ItemStack createArrowBatch(ItemStack tip, int outputCount, int maxEffects) {
        ItemStack result = new ItemStack(GeneratedMod.CRAFTED_ARROW.get(), outputCount);
        CompoundTag tag = result.getOrCreateTag();
        if (isPotion(tip)) {
            CompoundTag potionTag = tip.getTag();
            if (potionTag != null) tag.put("PotionData", potionTag.copy());
            tag.putBoolean("PotionOnly", true);
            tag.putString("Tip", "potion");
            tag.putDouble("BonusDamage", 0.0D);
        } else {
            TipStats stats = tipStats(tip);
            if (stats == null) {
                tag.putString("Tip", "none");
                tag.putDouble("BonusDamage", 0.0D);
            } else {
                tag.putString("Tip", tip.getItem().getDescriptionId());
                tag.putDouble("BonusDamage", stats.damage());
                boolean netherite = tip.is(Items.NETHERITE_INGOT);
                tag.putInt("Drop", netherite
                        ? RandomSource.create().nextInt(51) - 25
                        : stats.dropPct());
                if (stats.itemBoost() > 1.0D) tag.putDouble("ItemBoost", stats.itemBoost());
                if (stats.enchantBoost() > 1.0D) tag.putDouble("EnchantBoost", stats.enchantBoost());
                if (netherite) rollNetheriteBonuses(tag);
            }
        }
        tag.putInt("EffectCount", 0);
        for (int i = 0; i < maxEffects; i++) {
            ItemStack effect = container.getItem(SLOT_EFFECT_1 + i);
            if (!effect.isEmpty()) applyEffect(tag, effect);
        }
        return result;
    }

    /** 下界合金箭头：附带随机一种物品效果与随机一种制箭附魔（Ⅰ级）。 */
    private static void rollNetheriteBonuses(CompoundTag tag) {
        RandomSource random = RandomSource.create();
        switch (random.nextInt(5)) {
            case 0 -> tag.putInt("Rocket", tag.getInt("Rocket") + 2);
            case 1 -> tag.putInt("Flame", tag.getInt("Flame") + 1);
            case 2 -> tag.putInt("Explosion", tag.getInt("Explosion") + 1);
            case 3 -> tag.putInt("Glow", tag.getInt("Glow") + 1);
            default -> tag.putInt("Knockback", tag.getInt("Knockback") + 1);
        }
        String key = ENCHANT_KEYS[random.nextInt(ENCHANT_KEYS.length)];
        tag.putInt(key, Math.max(1, tag.getInt(key)));
        tag.putInt("EffectCount", tag.getInt("EffectCount") + 1);
    }

    private void consumeCraftInputs() {
        ItemStack shaft = container.getItem(SLOT_SHAFT);
        ItemStack tip = container.getItem(SLOT_TIP);
        int n = computeBatchSize(container.getItem(SLOT_FEATHER), shaft, tip);
        if (n < 1) return;
        container.getItem(SLOT_FEATHER).shrink(n);
        if (shaft.is(ItemTags.PLANKS)) consumeBulk(shaft, n, PLANK_SHAFTS);
        else shaft.shrink(n);
        if (!tip.isEmpty()) {
            if (isPotion(tip)) consumeBulk(tip, n, POTION_ARROW_RATIO);
            else {
                TipStats stats = tipStats(tip);
                if (stats != null) consumeBulk(tip, n, stats.supplies());
            }
        }
        for (int i = 0; i < EFFECT_SLOT_COUNT; i++) {
            ItemStack effect = container.getItem(SLOT_EFFECT_1 + i);
            if (!effect.isEmpty()) {
                consumeBulk(effect, n, isPotion(effect) ? POTION_ARROW_RATIO : EFFECT_ARROW_RATIO);
            }
        }
        container.setChanged();
        if (stationPos != null && station != null && station.getLevel() instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.OAK_PLANKS)),
                    stationPos.getX() + 0.5, stationPos.getY() + 1.1, stationPos.getZ() + 0.5, Math.min(16, n * 2), 0.35, 0.15, 0.35, 0.08);
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.FEATHER)),
                    stationPos.getX() + 0.5, stationPos.getY() + 1.15, stationPos.getZ() + 0.5, Math.min(8, n), 0.3, 0.15, 0.3, 0.06);
        }
    }

    /** 根据当前材料刷新成品槽预览。 */
    private void updateCraftOutput() {
        if (updatingOutput) return;
        updatingOutput = true;
        ItemStack preview = previewCraftResult();
        ItemStack current = container.getItem(SLOT_CRAFT_OUTPUT);
        boolean same = preview.isEmpty() ? current.isEmpty()
                : !current.isEmpty() && ItemStack.isSameItemSameTags(current, preview) && current.getCount() == preview.getCount();
        if (!same) container.setItem(SLOT_CRAFT_OUTPUT, preview);
        updatingOutput = false;
    }

    // ---------------------------------------------------------------- 附魔页

    /** 原版附魔台机制：三选一，消耗经验等级与青金石。 */
    private boolean applyOffer(Player player, int index) {
        if (getTier() < 3) return false;
        int encoded = station != null ? offers[index] : data.get(DATA_OFFER + index);
        int cost = station != null ? costs[index] : data.get(DATA_COST + index);
        if (encoded == 0) return false;
        ItemStack arrows = container.getItem(SLOT_ENCHANT_ARROW);
        ItemStack lapis = container.getItem(SLOT_ENCHANT_LAPIS);
        if (!arrows.is(GeneratedMod.CRAFTED_ARROW.get()) || !lapis.is(Items.LAPIS_LAZULI)) return false;
        if (lapis.getCount() < index + 1) return false;
        if (!player.getAbilities().instabuild && player.experienceLevel < cost) return false;
        int keyIndex = encoded / 10 - 1;
        int level = encoded % 10;
        if (keyIndex < 0 || keyIndex >= ENCHANT_KEYS.length) return false;
        ItemStack result = arrows.copy();
        result.getOrCreateTag().putInt(ENCHANT_KEYS[keyIndex],
                Math.max(level, result.getOrCreateTag().getInt(ENCHANT_KEYS[keyIndex])));
        if (!canOutput(SLOT_ENCHANT_OUTPUT, result)) return false;
        container.setItem(SLOT_ENCHANT_ARROW, ItemStack.EMPTY);
        lapis.shrink(index + 1);
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(-cost);
        putOutput(SLOT_ENCHANT_OUTPUT, result);
        if (station != null) {
            station.rerollEnchantSeed();
            refreshOffers();
        }
        enchantParticles(player);
        return true;
    }

    // ---------------------------------------------------------------- 弓改装页

    /** 取出弓时自动把材料效果附加到弓上，并各消耗 1 个材料。 */
    private void applyBowEffects(ItemStack bow) {
        if (getTier() < 5) return;
        CompoundTag tag = bow.getOrCreateTag();
        boolean applied = false;
        for (int i = 0; i < BOW_EFFECT_COUNT; i++) {
            ItemStack effect = container.getItem(SLOT_BOW_EFFECT_1 + i);
            if (effect.isEmpty()) continue;
            if (effect.is(Items.FIREWORK_ROCKET)) tag.putInt("FletchingVelocity", tag.getInt("FletchingVelocity") + 1);
            else if (effect.is(Items.BLAZE_POWDER)) tag.putInt("FletchingFlame", tag.getInt("FletchingFlame") + 1);
            else if (effect.is(Items.GLOWSTONE_DUST)) tag.putInt("FletchingFocus", tag.getInt("FletchingFocus") + 1);
        }
        for (int i = 0; i < BOW_EFFECT_COUNT; i++) {
            ItemStack effect = container.getItem(SLOT_BOW_EFFECT_1 + i);
            if (!effect.isEmpty()) {
                effect.shrink(1);
                applied = true;
            }
        }
        if (applied) container.setChanged();
    }

    // ---------------------------------------------------------------- 通用

    private int bookshelfCount(Player player) {
        int count = 0;
        for (BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantmentTableBlock.isValidBookShelf(player.level(), stationPos, stationPos.offset(offset))) count++;
        }
        return count;
    }

    private void enchantParticles(Player player) {
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT, stationPos.getX() + 0.5, stationPos.getY() + 1.2, stationPos.getZ() + 0.5, 40, 0.5, 0.25, 0.5, 0.5);
        }
    }

    /** 效果材料消耗：非药水 ⌈N/4⌉，药水 ⌈N/8⌉。 */
    private int effectCost(ItemStack effect, int outputCount) {
        int ratio = isPotion(effect) ? POTION_ARROW_RATIO : EFFECT_ARROW_RATIO;
        return (outputCount + ratio - 1) / ratio;
    }

    private static void applyEffect(CompoundTag tag, ItemStack effect) {
        int count = tag.getInt("EffectCount");
        tag.putInt("EffectCount", count + 1);
        if (effect.is(Items.FIREWORK_ROCKET)) tag.putInt("Rocket", tag.getInt("Rocket") + 2);
        else if (effect.is(Items.BLAZE_POWDER)) tag.putInt("Flame", tag.getInt("Flame") + 1);
        else if (effect.is(Items.GUNPOWDER)) tag.putInt("Explosion", tag.getInt("Explosion") + 1);
        else if (effect.is(Items.GLOWSTONE_DUST)) tag.putInt("Glow", tag.getInt("Glow") + 1);
        else if (effect.is(Items.SLIME_BALL)) tag.putInt("Knockback", tag.getInt("Knockback") + 1);
        else if (isPotion(effect)) {
            CompoundTag potionTag = effect.getTag();
            if (potionTag != null) tag.put("PotionData", potionTag.copy());
            tag.putBoolean("PotionOnly", true);
        }
    }

    private static boolean isPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    // ---------------------------------------------------------------- 箭头属性

    /** 箭头属性：供应支数、额外伤害、下坠增减（百分比，正=更重下坠更快）、物品效果增幅、附魔效果增幅。 */
    public record TipStats(int supplies, double damage, int dropPct, double itemBoost, double enchantBoost) { }

    private static final TipStats PLANK_TIP = new TipStats(16, 0.5D, 0, 1.0D, 1.0D);
    private static final Map<Item, TipStats> TIP_STATS = new LinkedHashMap<>();

    static {
        TIP_STATS.put(Items.FLINT, new TipStats(4, 1.0D, 0, 1.0D, 1.0D));
        TIP_STATS.put(Items.COBBLESTONE, new TipStats(8, 2.0D, 25, 1.0D, 1.0D));
        TIP_STATS.put(Items.COPPER_INGOT, new TipStats(8, 1.5D, 10, 1.0D, 1.0D));
        TIP_STATS.put(Items.IRON_NUGGET, new TipStats(1, 2.5D, 15, 1.0D, 1.0D));
        TIP_STATS.put(Items.IRON_INGOT, new TipStats(9, 2.5D, 15, 1.0D, 1.0D));
        TIP_STATS.put(Items.GOLD_NUGGET, new TipStats(1, 1.0D, 5, 1.5D, 1.0D));
        TIP_STATS.put(Items.GOLD_INGOT, new TipStats(9, 1.0D, 5, 1.5D, 1.0D));
        TIP_STATS.put(Items.AMETHYST_SHARD, new TipStats(4, 2.0D, -10, 1.0D, 1.5D));
        TIP_STATS.put(Items.DIAMOND, new TipStats(4, 5.0D, 0, 1.5D, 1.5D));
        TIP_STATS.put(Items.NETHERITE_INGOT, new TipStats(8, 5.0D, 0, 1.0D, 1.0D));
    }

    /** 箭头槽可用材料的属性；木板按物品标签特判；非箭头材料返回 null。 */
    public static TipStats tipStats(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ItemTags.PLANKS)) return PLANK_TIP;
        return TIP_STATS.get(stack.getItem());
    }

    /** 箭头材料的供应支数（药水 1 瓶 = 8 支药水箭）。供界面余量显示复用。 */
    public static int tipSupplies(ItemStack stack) {
        if (isPotion(stack)) return POTION_ARROW_RATIO;
        TipStats stats = tipStats(stack);
        return stats == null ? 0 : stats.supplies();
    }

    private static boolean isTip(ItemStack stack) {
        if (isPotion(stack)) return true;
        return tipStats(stack) != null;
    }

    private static boolean isEffect(ItemStack stack) {
        return isPotion(stack) || stack.is(Items.FIREWORK_ROCKET) || stack.is(Items.BLAZE_POWDER)
                || stack.is(Items.GUNPOWDER) || stack.is(Items.GLOWSTONE_DUST) || stack.is(Items.SLIME_BALL);
    }

    private static boolean isBowEffect(ItemStack stack) {
        return stack.is(Items.FIREWORK_ROCKET) || stack.is(Items.BLAZE_POWDER) || stack.is(Items.GLOWSTONE_DUST);
    }

    private boolean canOutput(int outputSlot, ItemStack stack) {
        ItemStack output = container.getItem(outputSlot);
        return output.isEmpty() || (ItemStack.isSameItemSameTags(output, stack) && output.getCount() + stack.getCount() <= output.getMaxStackSize());
    }

    private void putOutput(int outputSlot, ItemStack stack) {
        ItemStack output = container.getItem(outputSlot);
        if (output.isEmpty()) container.setItem(outputSlot, stack);
        else output.grow(stack.getCount());
        container.setChanged();
    }

    @Override
    public void broadcastChanges() {
        updateCraftOutput();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (station != null) container.setItem(SLOT_CRAFT_OUTPUT, ItemStack.EMPTY);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index == SLOT_CRAFT_OUTPUT) {
            if (!moveItemStackTo(stack, INV_START, slots.size(), true)) return ItemStack.EMPTY;
            slot.onTake(player, result);
            return result;
        }
        if (index == SLOT_BOW) {
            if (!moveItemStackTo(stack, INV_START, slots.size(), true)) return ItemStack.EMPTY;
            slot.onTake(player, result);
            return result;
        }
        if (index < SLOT_COUNT) {
            if (!moveItemStackTo(stack, INV_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /** 带页面归属与等级门槛的输入槽。 */
    private class TabSlot extends Slot {
        private final int tab;
        private final int minTier;
        private final Predicate<ItemStack> filter;

        TabSlot(Container container, int index, int x, int y, int tab, int minTier, Predicate<ItemStack> filter) {
            super(container, index, x, y);
            this.tab = tab;
            this.minTier = minTier;
            this.filter = filter;
        }

        private boolean tierOk() { return getTier() >= minTier; }

        @Override public boolean mayPlace(ItemStack stack) {
            return tierOk() && filter.test(stack);
        }

        /** 有已用零头的批量材料，最后 1 份被台子扣住不可取出。 */
        @Override public boolean mayPickup(Player player) {
            ItemStack stack = getItem();
            if (stack.getCount() == 1 && bulkRemainder(stack) > 0) return false;
            return super.mayPickup(player);
        }

        @Override public boolean isActive() {
            return (tab < 0 || getTab() == tab) && tierOk();
        }
    }

    /** 成品预览槽：点击取出即完成制作并消耗材料。 */
    private class CraftOutputSlot extends Slot {
        CraftOutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override public boolean isActive() { return getTab() == TAB_CRAFT; }

        @Override public void onTake(Player player, ItemStack stack) {
            consumeCraftInputs();
            updateCraftOutput();
            super.onTake(player, stack);
        }
    }

    private class EnchantOutputSlot extends TabSlot {
        EnchantOutputSlot(Container container, int index, int x, int y, int tab, int minTier) {
            super(container, index, x, y, tab, minTier, stack -> false);
        }

        @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); }
    }

    /** 弓槽：取出弓时把材料效果附加到弓上。 */
    private class BowSlot extends Slot {
        BowSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return getTier() >= 5 && stack.getItem() instanceof BowItem;
        }

        @Override public boolean isActive() { return getTab() == TAB_BOW && getTier() >= 5; }

        @Override public void onTake(Player player, ItemStack stack) {
            applyBowEffects(stack);
            super.onTake(player, stack);
        }
    }

    /** 升级槽：放入下一级金属块立即升级并消耗 1 个。 */
    private class UpgradeSlot extends Slot {
        UpgradeSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        private boolean isUpgradeMaterial(ItemStack stack) {
            Item required = nextUpgradeMaterial();
            return required != null && stack.is(required);
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return isUpgradeMaterial(stack);
        }

        @Override public boolean isActive() { return getTier() < 5; }

        @Override public void setByPlayer(ItemStack stack) {
            super.setByPlayer(stack);
            if (station == null) return;
            ItemStack current = getItem();
            if (!current.isEmpty() && isUpgradeMaterial(current)) {
                current.shrink(1);
                setChanged();
                station.upgrade();
                refreshOffers();
            }
        }
    }
}

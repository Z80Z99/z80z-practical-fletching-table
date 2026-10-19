package cn.blockforge.generated.practicalfletching;

import cn.blockforge.generated.practicalfletching.block.FletchingStationBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class CommonEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 放置原版制箭台时直接替换为实用制箭台（基础级，从铜锭开始升级）。 */
    @SubscribeEvent
    public static void replacePlacedTable(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        // 用世界实际方块判定，避免非玩家实体放置时 getPlacedBlock 语义不同导致误判
        if (!level.getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)) return;
        level.setBlockAndUpdate(event.getPos(), GeneratedMod.FLETCHING_STATION.get().defaultBlockState());
    }

    /** 区块加载时把其中的原版制箭台替换为实用制箭台（覆盖村庄结构与旧存档）。 */
    @SubscribeEvent
    public static void replaceWorldgenTables(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel)) return;
        net.minecraft.world.level.chunk.ChunkAccess chunk = event.getChunk();
        Block station = GeneratedMod.FLETCHING_STATION.get();
        net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            net.minecraft.world.level.chunk.LevelChunkSection section = sections[sectionIndex];
            if (!section.maybeHas(state -> state.is(Blocks.FLETCHING_TABLE))) continue;
            int minY = chunk.getMinBuildHeight() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!state.is(Blocks.FLETCHING_TABLE)) continue;
                        BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, minY + y, chunk.getPos().getMinBlockZ() + z);
                        // 直接写入区块数据，避免加载期触发方块更新级联；setBlockState 会自动创建方块实体
                        chunk.setBlockState(pos, station.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    /** 粘着箭：目标身上的数据每 20 tick 结算一次物理伤害，到期自动清理。 */
    @SubscribeEvent
    public static void tickSticking(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        LivingEntity living = event.getEntity();
        if (living.tickCount % 20 != 0) return;
        CompoundTag data = living.getPersistentData();
        if (!data.contains("FletchingStickPower")) return;
        long until = data.getLong("FletchingStickUntil");
        if (server.getGameTime() >= until) {
            data.remove("FletchingStickPower");
            data.remove("FletchingStickUntil");
            return;
        }
        double power = data.getDouble("FletchingStickPower");
        living.hurt(server.damageSources().generic(), (float) (0.5D * power));
    }

    @SubscribeEvent
    public static void convertVanillaTable(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)) return;
        ItemStack held = event.getItemStack();
        if (!held.is(Items.COPPER_INGOT) || !event.getEntity().isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (!event.getLevel().isClientSide) {
            event.getLevel().setBlockAndUpdate(event.getPos(), GeneratedMod.FLETCHING_STATION.get().defaultBlockState());
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof FletchingStationBlockEntity station) station.upgrade();
            if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
            event.getLevel().levelEvent(2005, event.getPos().above(), 0);
            event.getEntity().displayClientMessage(Component.translatable("message.z80z_practical_fletching.copper_created"), true);
        }
    }

    /** 把实用制箭台的方块状态登记为原版制箭师 POI，失业村民即可在台旁转职。 */
    @SubscribeEvent
    public static void registerFletcherPoi(ServerAboutToStartEvent event) {
        try {
            Map<Object, Object> byState = findPoiStateMap();
            if (byState == null) {
                LOGGER.warn("[制箭台] 未能定位 PoiTypes 的方块状态映射表，村民将无法在实用制箭台转职");
                return;
            }
            Object fletcher = byState.get(Blocks.FLETCHING_TABLE.defaultBlockState());
            if (fletcher == null) {
                LOGGER.warn("[制箭台] 未找到原版制箭台对应的制箭师 POI");
                return;
            }
            Block station = GeneratedMod.FLETCHING_STATION.get();
            int registered = 0;
            for (BlockState state : station.getStateDefinition().getPossibleStates()) {
                byState.put(state, fletcher);
                registered++;
            }
            LOGGER.info("[制箭台] 已登记 {} 个实用制箭台方块状态为制箭师 POI", registered);
        } catch (Throwable t) {
            LOGGER.error("[制箭台] 注册制箭师 POI 失败，村民将无法在实用制箭台转职", t);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> findPoiStateMap() throws IllegalAccessException {
        Field fallback = null;
        for (Field field : PoiTypes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Map.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            String name = field.getName();
            if ("TYPE_BY_STATE".equals(name) || "f_218070_".equals(name)) {
                return (Map<Object, Object>) field.get(null);
            }
            if (field.getGenericType() instanceof ParameterizedType type) {
                Type[] args = type.getActualTypeArguments();
                if (args.length == 2 && args[0] == BlockState.class) {
                    fallback = field;
                }
            }
        }
        if (fallback != null) {
            Object value = fallback.get(null);
            if (value instanceof Map<?, ?> map && map.keySet().stream().findFirst().orElse(null) instanceof BlockState) {
                return (Map<Object, Object>) value;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void bowTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.hasTag()) return;
        if (stack.getTag().getInt("FletchingVelocity") > 0)
            event.getToolTip().add(Component.translatable("tooltip.z80z_practical_fletching.bow_velocity").withStyle(ChatFormatting.AQUA));
        if (stack.getTag().getInt("FletchingFlame") > 0)
            event.getToolTip().add(Component.translatable("tooltip.z80z_practical_fletching.bow_flame").withStyle(ChatFormatting.GOLD));
        if (stack.getTag().getInt("FletchingFocus") > 0)
            event.getToolTip().add(Component.translatable("tooltip.z80z_practical_fletching.bow_focus").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

package cn.blockforge.generated.practicalfletching;

import cn.blockforge.generated.practicalfletching.block.FletchingStationBlock;
import cn.blockforge.generated.practicalfletching.block.FletchingStationBlockEntity;
import cn.blockforge.generated.practicalfletching.entity.CraftedArrowEntity;
import cn.blockforge.generated.practicalfletching.item.CraftedArrowItem;
import cn.blockforge.generated.practicalfletching.menu.FletchingMenu;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "z80z_practical_fletching";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Block> FLETCHING_STATION = BLOCKS.register("fletching_station", FletchingStationBlock::new);
    public static final RegistryObject<Item> FLETCHING_STATION_ITEM = ITEMS.register("fletching_station",
            () -> new BlockItem(FLETCHING_STATION.get(), new Item.Properties()));
    public static final RegistryObject<Item> CRAFTED_ARROW = ITEMS.register("crafted_arrow",
            () -> new CraftedArrowItem(new Item.Properties()));

    public static final RegistryObject<BlockEntityType<FletchingStationBlockEntity>> FLETCHING_STATION_ENTITY =
            BLOCK_ENTITIES.register("fletching_station", () -> BlockEntityType.Builder.of(
                    FletchingStationBlockEntity::new, FLETCHING_STATION.get()).build(null));

    public static final RegistryObject<MenuType<FletchingMenu>> FLETCHING_MENU = MENUS.register("fletching_station",
            () -> IForgeMenuType.create(FletchingMenu::new));

    public static final RegistryObject<EntityType<CraftedArrowEntity>> CRAFTED_ARROW_ENTITY = ENTITIES.register("crafted_arrow",
            () -> EntityType.Builder.<CraftedArrowEntity>of(CraftedArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                    .build(MOD_ID + ":crafted_arrow"));

    public GeneratedMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(FLETCHING_STATION_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.COMBAT) event.accept(CRAFTED_ARROW.get());
    }
}

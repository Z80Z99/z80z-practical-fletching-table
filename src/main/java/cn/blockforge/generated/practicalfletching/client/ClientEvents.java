package cn.blockforge.generated.practicalfletching.client;

import cn.blockforge.generated.practicalfletching.GeneratedMod;
import cn.blockforge.generated.practicalfletching.entity.CraftedArrowEntity;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation("minecraft", "textures/entity/projectiles/arrow.png");

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(GeneratedMod.FLETCHING_MENU.get(), FletchingScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GeneratedMod.CRAFTED_ARROW_ENTITY.get(), CraftedArrowRenderer::new);
    }

    private static final class CraftedArrowRenderer extends ArrowRenderer<CraftedArrowEntity> {
        private CraftedArrowRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(CraftedArrowEntity arrow) { return ARROW_TEXTURE; }
    }
}

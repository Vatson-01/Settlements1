package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.client.screen.PlotScreen;
import com.settlements.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PlotMenuClientEvents {
    private PlotMenuClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(new Runnable() {
            @Override
            public void run() {
                MenuScreens.register(ModMenuTypes.PLOT_MENU.get(), PlotScreen::new);
            }
        });
    }
}

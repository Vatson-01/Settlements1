package com.settlements.client;

import com.settlements.SettlementsMod;
import com.settlements.client.screen.SettlementScreen;
import com.settlements.client.screen.ShopManagementScreen;
import com.settlements.client.screen.ShopScreen;
import com.settlements.client.screen.SettlementResidentManageScreen;
import com.settlements.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
            MenuScreens.register(ModMenuTypes.SHOP_MANAGEMENT_MENU.get(), ShopManagementScreen::new);
            MenuScreens.register(ModMenuTypes.SETTLEMENT_RESIDENT_MANAGE_MENU.get(), SettlementResidentManageScreen::new);
            MenuScreens.register(ModMenuTypes.SETTLEMENT_MENU.get(), SettlementScreen::new);
        });
    }
}
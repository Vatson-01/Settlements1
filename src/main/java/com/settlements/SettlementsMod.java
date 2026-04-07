package com.settlements;

import com.settlements.registry.ModBlockEntities;
import com.settlements.registry.ModBlocks;
import com.settlements.registry.ModItems;
import com.settlements.registry.ModMenuTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import com.settlements.network.ShopManagementPackets;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SettlementsMod.MOD_ID)
public class SettlementsMod {
    public static final String MOD_ID = "settlements";

    public SettlementsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ShopManagementPackets.init();
    }
}
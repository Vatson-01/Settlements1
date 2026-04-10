package com.settlements.registry;

import com.settlements.SettlementsMod;
import com.settlements.world.menu.SettlementMenu;
import com.settlements.world.menu.ShopManagementMenu;
import com.settlements.world.menu.ShopMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import com.settlements.world.menu.SettlementResidentManageMenu;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import com.settlements.world.menu.SettlementResidentsMenu;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SettlementsMod.MOD_ID);

    public static final RegistryObject<MenuType<ShopMenu>> SHOP_MENU =
            MENU_TYPES.register("shop_menu", () -> IForgeMenuType.create(ShopMenu::new));

    public static final RegistryObject<MenuType<ShopManagementMenu>> SHOP_MANAGEMENT_MENU =
            MENU_TYPES.register("shop_management_menu", () -> IForgeMenuType.create(ShopManagementMenu::new));
    public static final RegistryObject<MenuType<SettlementResidentsMenu>> SETTLEMENT_RESIDENTS_MENU =
            MENU_TYPES.register("settlement_residents_menu", () -> IForgeMenuType.create(SettlementResidentsMenu::new));
    public static final RegistryObject<MenuType<SettlementMenu>> SETTLEMENT_MENU =
            MENU_TYPES.register("settlement_menu", () -> IForgeMenuType.create(SettlementMenu::new));
    public static final RegistryObject<MenuType<SettlementResidentManageMenu>> SETTLEMENT_RESIDENT_MANAGE_MENU =
            MENU_TYPES.register("settlement_resident_manage_menu", () -> IForgeMenuType.create(SettlementResidentManageMenu::new));
    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
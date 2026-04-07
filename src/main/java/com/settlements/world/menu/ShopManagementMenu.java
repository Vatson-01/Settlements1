package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PriceMode;
import com.settlements.data.model.ShopRecord;
import com.settlements.data.model.ShopTradeEntry;
import com.settlements.network.ShopManagementPackets;
import com.settlements.registry.ModBlocks;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.ShopService;
import com.settlements.world.blockentity.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ShopManagementMenu extends AbstractContainerMenu {
    private static final int SLOT_SELECTED_TRADE = 0;

    public static final int BUTTON_TOGGLE_ENABLED = 0;
    public static final int BUTTON_OPEN_STORAGE = 1;
    public static final int BUTTON_DEPOSIT_ALL = 2;
    public static final int BUTTON_WITHDRAW_ALL = 3;
    public static final int BUTTON_DEPOSIT_10 = 4;
    public static final int BUTTON_WITHDRAW_10 = 5;
    public static final int BUTTON_DEPOSIT_100 = 6;
    public static final int BUTTON_WITHDRAW_100 = 7;
    public static final int BUTTON_DEPOSIT_1000 = 8;
    public static final int BUTTON_WITHDRAW_1000 = 9;
    public static final int BUTTON_TOGGLE_INFINITE_STOCK = 10;
    public static final int BUTTON_TOGGLE_INFINITE_BALANCE = 11;
    public static final int BUTTON_TOGGLE_INDESTRUCTIBLE = 12;

    public static final int BUTTON_PREV_TRADE = 13;
    public static final int BUTTON_NEXT_TRADE = 14;
    public static final int BUTTON_ADD_SELL_TRADE = 15;
    public static final int BUTTON_ADD_BUY_TRADE = 16;
    public static final int BUTTON_ADD_DUAL_TRADE = 17;
    public static final int BUTTON_REMOVE_TRADE = 18;
    public static final int BUTTON_TOGGLE_TRADE_ENABLED = 19;

    public static final int BUTTON_SELL_PRICE_MINUS_1 = 20;
    public static final int BUTTON_SELL_PRICE_PLUS_1 = 21;
    public static final int BUTTON_SELL_PRICE_MINUS_10 = 22;
    public static final int BUTTON_SELL_PRICE_PLUS_10 = 23;

    public static final int BUTTON_BUY_PRICE_MINUS_1 = 24;
    public static final int BUTTON_BUY_PRICE_PLUS_1 = 25;
    public static final int BUTTON_BUY_PRICE_MINUS_10 = 26;
    public static final int BUTTON_BUY_PRICE_PLUS_10 = 27;

    public static final int BUTTON_SELL_BATCH_MINUS_1 = 28;
    public static final int BUTTON_SELL_BATCH_PLUS_1 = 29;
    public static final int BUTTON_BUY_BATCH_MINUS_1 = 30;
    public static final int BUTTON_BUY_BATCH_PLUS_1 = 31;

    public static final int BUTTON_TOGGLE_TRADE_MODE = 32;

    public static final int BUTTON_DYNAMIC_MIN_SELL_MINUS_1 = 33;
    public static final int BUTTON_DYNAMIC_MIN_SELL_PLUS_1 = 34;
    public static final int BUTTON_DYNAMIC_MAX_SELL_MINUS_1 = 35;
    public static final int BUTTON_DYNAMIC_MAX_SELL_PLUS_1 = 36;
    public static final int BUTTON_DYNAMIC_MIN_BUY_MINUS_1 = 37;
    public static final int BUTTON_DYNAMIC_MIN_BUY_PLUS_1 = 38;
    public static final int BUTTON_DYNAMIC_MAX_BUY_MINUS_1 = 39;
    public static final int BUTTON_DYNAMIC_MAX_BUY_PLUS_1 = 40;
    public static final int BUTTON_DYNAMIC_ELASTICITY_MINUS = 41;
    public static final int BUTTON_DYNAMIC_ELASTICITY_PLUS = 42;
    public static final int BUTTON_DYNAMIC_DECAY_MINUS = 43;
    public static final int BUTTON_DYNAMIC_DECAY_PLUS = 44;
    public static final int BUTTON_DYNAMIC_INACTIVITY_SELL_MINUS = 45;
    public static final int BUTTON_DYNAMIC_INACTIVITY_SELL_PLUS = 46;
    public static final int BUTTON_DYNAMIC_INACTIVITY_BUY_MINUS = 47;
    public static final int BUTTON_DYNAMIC_INACTIVITY_BUY_PLUS = 48;

    private static final int DATA_BALANCE_LOW = 0;
    private static final int DATA_BALANCE_HIGH = 1;
    private static final int DATA_ENABLED = 2;
    private static final int DATA_IS_ADMIN = 3;
    private static final int DATA_INFINITE_STOCK = 4;
    private static final int DATA_INFINITE_BALANCE = 5;
    private static final int DATA_INDESTRUCTIBLE = 6;
    private static final int DATA_SELECTED_TRADE_INDEX = 7;
    private static final int DATA_TRADE_COUNT = 8;
    private static final int DATA_SELECTED_TRADE_ENABLED = 9;
    private static final int DATA_SELECTED_CAN_SELL = 10;
    private static final int DATA_SELECTED_CAN_BUY = 11;
    private static final int DATA_SELECTED_SELL_BATCH = 12;
    private static final int DATA_SELECTED_BUY_BATCH = 13;
    private static final int DATA_SELECTED_SELL_PRICE_LOW = 14;
    private static final int DATA_SELECTED_SELL_PRICE_HIGH = 15;
    private static final int DATA_SELECTED_BUY_PRICE_LOW = 16;
    private static final int DATA_SELECTED_BUY_PRICE_HIGH = 17;
    private static final int DATA_SELECTED_DYNAMIC = 18;
    private static final int DATA_SELECTED_MIN_SELL_LOW = 19;
    private static final int DATA_SELECTED_MIN_SELL_HIGH = 20;
    private static final int DATA_SELECTED_MAX_SELL_LOW = 21;
    private static final int DATA_SELECTED_MAX_SELL_HIGH = 22;
    private static final int DATA_SELECTED_MIN_BUY_LOW = 23;
    private static final int DATA_SELECTED_MIN_BUY_HIGH = 24;
    private static final int DATA_SELECTED_MAX_BUY_LOW = 25;
    private static final int DATA_SELECTED_MAX_BUY_HIGH = 26;
    private static final int DATA_SELECTED_ELASTICITY_SCALED = 27;
    private static final int DATA_SELECTED_DECAY_SCALED = 28;
    private static final int DATA_SELECTED_INACTIVITY_SELL_SCALED = 29;
    private static final int DATA_SELECTED_INACTIVITY_BUY_SCALED = 30;
    private static final int DATA_COUNT = 31;

    private final BlockPos shopPos;
    private final String shopName;
    private final ContainerData menuData;
    private final SimpleContainer selectedTradeDisplay;
    private int selectedTradeIndex;

    public ShopManagementMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                buf.readBlockPos(),
                buf.readUtf(),
                createClientData()
        );
    }

    public ShopManagementMenu(int containerId, Inventory playerInventory, BlockPos shopPos) {
        this(
                containerId,
                playerInventory,
                shopPos,
                getServerShopName(playerInventory, shopPos),
                createServerData(playerInventory, shopPos)
        );
    }

    private ShopManagementMenu(int containerId,
                               Inventory playerInventory,
                               BlockPos shopPos,
                               String shopName,
                               ContainerData menuData) {
        super(ModMenuTypes.SHOP_MANAGEMENT_MENU.get(), containerId);
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.menuData = menuData;
        this.selectedTradeIndex = 1;
        this.selectedTradeDisplay = new SimpleContainer(1);


        this.addDataSlots(menuData);

        this.addSlot(new Slot(selectedTradeDisplay, SLOT_SELECTED_TRADE, 188, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });

        addPlayerInventorySlots(playerInventory);
        refreshTradeDisplay(playerInventory.player);
    }

    public static void writeOpenData(FriendlyByteBuf buf, BlockPos pos, ShopRecord shop) {
        buf.writeBlockPos(pos);
        buf.writeUtf(shop.getName());
    }

    private static String getServerShopName(Inventory playerInventory, BlockPos pos) {
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
            return "Управление магазином";
        }

        ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), pos);
        return shop == null ? "Управление магазином" : shop.getName();
    }

    private static ContainerData createClientData() {
        return new net.minecraft.world.inventory.SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(final Inventory playerInventory, final BlockPos shopPos) {
        return new ContainerData() {
            private ShopRecord getShop() {
                if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
                    return null;
                }

                return SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
            }

            private int getSelectedIndex() {
                if (playerInventory.player.containerMenu instanceof ShopManagementMenu menu) {
                    return menu.getServerSelectedTradeIndex();
                }

                return 1;
            }

            @Override
            public int get(int index) {
                ShopRecord shop = getShop();
                long balance = shop == null ? 0L : shop.getBalance();
                int selectedIndex = getSelectedIndex();
                ShopTradeEntry trade = shop == null ? null : shop.getTradeByHumanIndex(selectedIndex);

                if (index == DATA_BALANCE_LOW) {
                    return (int) (balance & 0xFFFFFFFFL);
                }
                if (index == DATA_BALANCE_HIGH) {
                    return (int) ((balance >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_ENABLED) {
                    return shop != null && shop.isEnabled() ? 1 : 0;
                }
                if (index == DATA_IS_ADMIN) {
                    return shop != null && shop.isAdminShop() ? 1 : 0;
                }
                if (index == DATA_INFINITE_STOCK) {
                    return shop != null && shop.isInfiniteStock() ? 1 : 0;
                }
                if (index == DATA_INFINITE_BALANCE) {
                    return shop != null && shop.isInfiniteBalance() ? 1 : 0;
                }
                if (index == DATA_INDESTRUCTIBLE) {
                    return shop != null && shop.isIndestructible() ? 1 : 0;
                }
                if (index == DATA_SELECTED_TRADE_INDEX) {
                    return shop == null || shop.getTrades().isEmpty() ? 0 : selectedIndex;
                }
                if (index == DATA_TRADE_COUNT) {
                    return shop == null ? 0 : shop.getTrades().size();
                }
                if (index == DATA_SELECTED_TRADE_ENABLED) {
                    return trade != null && trade.isEnabled() ? 1 : 0;
                }
                if (index == DATA_SELECTED_CAN_SELL) {
                    return trade != null && trade.canSellToPlayer() ? 1 : 0;
                }
                if (index == DATA_SELECTED_CAN_BUY) {
                    return trade != null && trade.canBuyFromPlayer() ? 1 : 0;
                }
                if (index == DATA_SELECTED_SELL_BATCH) {
                    return trade == null ? 0 : trade.getSellBatchSize();
                }
                if (index == DATA_SELECTED_BUY_BATCH) {
                    return trade == null ? 0 : trade.getBuyBatchSize();
                }
                if (index == DATA_SELECTED_SELL_PRICE_LOW) {
                    long value = trade == null ? 0L : trade.getSellPrice();
                    return (int) (value & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_SELL_PRICE_HIGH) {
                    long value = trade == null ? 0L : trade.getSellPrice();
                    return (int) ((value >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_BUY_PRICE_LOW) {
                    long value = trade == null ? 0L : trade.getBuyPrice();
                    return (int) (value & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_BUY_PRICE_HIGH) {
                    long value = trade == null ? 0L : trade.getBuyPrice();
                    return (int) ((value >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_DYNAMIC) {
                    return trade != null && trade.getPriceMode() == PriceMode.DYNAMIC ? 1 : 0;
                }
                if (index == DATA_SELECTED_MIN_SELL_LOW) {
                    long value = trade == null ? 0L : trade.getMinSellPrice();
                    return (int) (value & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MIN_SELL_HIGH) {
                    long value = trade == null ? 0L : trade.getMinSellPrice();
                    return (int) ((value >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MAX_SELL_LOW) {
                    long value = trade == null ? 0L : trade.getMaxSellPrice();
                    return (int) (value & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MAX_SELL_HIGH) {
                    long value = trade == null ? 0L : trade.getMaxSellPrice();
                    return (int) ((value >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MIN_BUY_LOW) {
                    long value = trade == null ? 0L : trade.getMinBuyPrice();
                    return (int) (value & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MIN_BUY_HIGH) {
                    long value = trade == null ? 0L : trade.getMinBuyPrice();
                    return (int) ((value >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MAX_BUY_LOW) {
                    long value = trade == null ? 0L : trade.getMaxBuyPrice();
                    return (int) (value & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_MAX_BUY_HIGH) {
                    long value = trade == null ? 0L : trade.getMaxBuyPrice();
                    return (int) ((value >>> 32) & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_ELASTICITY_SCALED) {
                    return scaleDouble(trade == null ? 0.0D : trade.getElasticity());
                }
                if (index == DATA_SELECTED_DECAY_SCALED) {
                    return scaleDouble(trade == null ? 0.0D : trade.getDecayPerStep());
                }
                if (index == DATA_SELECTED_INACTIVITY_SELL_SCALED) {
                    return scaleDouble(trade == null ? 0.0D : trade.getInactivitySellDrop());
                }
                if (index == DATA_SELECTED_INACTIVITY_BUY_SCALED) {
                    return scaleDouble(trade == null ? 0.0D : trade.getInactivityBuyRise());
                }

                return 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int scaleDouble(double value) {
        return (int) Math.round(value * 1000.0D);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 228;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 286;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, startX + column * 18, hotbarY));
        }
    }

    public String getShopName() {
        return shopName;
    }

    public BlockPos getShopPos() {
        return shopPos;
    }

    public long getBalance() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_BALANCE_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_BALANCE_HIGH));
        return low | (high << 32);
    }

    public boolean isEnabled() {
        return menuData.get(DATA_ENABLED) != 0;
    }

    public boolean isAdminShop() {
        return menuData.get(DATA_IS_ADMIN) != 0;
    }

    public boolean isInfiniteStock() {
        return menuData.get(DATA_INFINITE_STOCK) != 0;
    }

    public boolean isInfiniteBalance() {
        return menuData.get(DATA_INFINITE_BALANCE) != 0;
    }

    public boolean isIndestructible() {
        return menuData.get(DATA_INDESTRUCTIBLE) != 0;
    }

    public int getTradeCount() {
        return menuData.get(DATA_TRADE_COUNT);
    }

    public int getSelectedTradeIndex() {
        return menuData.get(DATA_SELECTED_TRADE_INDEX);
    }

    public boolean hasSelectedTrade() {
        return getTradeCount() > 0 && getSelectedTradeIndex() > 0;
    }

    public boolean isSelectedTradeEnabled() {
        return menuData.get(DATA_SELECTED_TRADE_ENABLED) != 0;
    }

    public boolean selectedTradeCanSell() {
        return menuData.get(DATA_SELECTED_CAN_SELL) != 0;
    }

    public boolean selectedTradeCanBuy() {
        return menuData.get(DATA_SELECTED_CAN_BUY) != 0;
    }

    public int getSelectedTradeSellBatch() {
        return menuData.get(DATA_SELECTED_SELL_BATCH);
    }

    public int getSelectedTradeBuyBatch() {
        return menuData.get(DATA_SELECTED_BUY_BATCH);
    }

    public long getSelectedTradeSellPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_SELL_PRICE_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_SELL_PRICE_HIGH));
        return low | (high << 32);
    }

    public long getSelectedTradeBuyPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_BUY_PRICE_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_BUY_PRICE_HIGH));
        return low | (high << 32);
    }

    public boolean isSelectedTradeDynamic() {
        return menuData.get(DATA_SELECTED_DYNAMIC) != 0;
    }

    public long getSelectedTradeMinSellPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MIN_SELL_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MIN_SELL_HIGH));
        return low | (high << 32);
    }

    public long getSelectedTradeMaxSellPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MAX_SELL_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MAX_SELL_HIGH));
        return low | (high << 32);
    }

    public long getSelectedTradeMinBuyPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MIN_BUY_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MIN_BUY_HIGH));
        return low | (high << 32);
    }

    public long getSelectedTradeMaxBuyPrice() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MAX_BUY_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_MAX_BUY_HIGH));
        return low | (high << 32);
    }

    public double getSelectedTradeElasticity() {
        return menuData.get(DATA_SELECTED_ELASTICITY_SCALED) / 1000.0D;
    }

    public double getSelectedTradeDecayPerStep() {
        return menuData.get(DATA_SELECTED_DECAY_SCALED) / 1000.0D;
    }

    public double getSelectedTradeInactivitySellDrop() {
        return menuData.get(DATA_SELECTED_INACTIVITY_SELL_SCALED) / 1000.0D;
    }

    public double getSelectedTradeInactivityBuyRise() {
        return menuData.get(DATA_SELECTED_INACTIVITY_BUY_SCALED) / 1000.0D;
    }

    public ItemStack getSelectedTradeDisplayStack() {
        return selectedTradeDisplay.getItem(0);
    }

    private int getServerSelectedTradeIndex() {
        return selectedTradeIndex;
    }

    private void moveSelection(int delta, Player player) {
        if (getTradeCount() <= 0) {
            selectedTradeIndex = 1;
            refreshTradeDisplay(player);
            return;
        }

        selectedTradeIndex += delta;
        if (selectedTradeIndex < 1) {
            selectedTradeIndex = getTradeCount();
        }
        if (selectedTradeIndex > getTradeCount()) {
            selectedTradeIndex = 1;
        }

        refreshTradeDisplay(player);
    }

    private void refreshTradeDisplay(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ShopRecord shop = SettlementSavedData.get(serverPlayer.server).getShopByPos(serverPlayer.level(), shopPos);
        if (shop == null || shop.getTrades().isEmpty()) {
            selectedTradeIndex = 1;
            selectedTradeDisplay.setItem(0, ItemStack.EMPTY);
            return;
        }

        if (selectedTradeIndex < 1) {
            selectedTradeIndex = 1;
        }
        if (selectedTradeIndex > shop.getTrades().size()) {
            selectedTradeIndex = shop.getTrades().size();
        }

        ShopTradeEntry trade = shop.getTradeByHumanIndex(selectedTradeIndex);
        selectedTradeDisplay.setItem(0, buildDisplayStack(trade));
    }

    private ItemStack buildDisplayStack(ShopTradeEntry trade) {
        if (trade == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(trade.getItemId());
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId == SLOT_SELECTED_TRADE
                && clickType == ClickType.PICKUP
                && hasSelectedTrade()
                && !getCarried().isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    ShopService.changeTradeItemAt(serverPlayer, shopPos, selectedTradeIndex, getCarried());
                    refreshTradeDisplay(player);
                    broadcastChanges();
                } catch (Exception e) {
                    serverPlayer.displayClientMessage(Component.literal(e.getMessage()), true);
                }
            }
            return;
        }

        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        try {
            if (buttonId == BUTTON_TOGGLE_ENABLED) {
                ShopService.setShopEnabledAt(serverPlayer, shopPos, !isEnabled());
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_DEPOSIT_ALL) {
                ShopService.depositAllToShopAt(serverPlayer, shopPos);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_WITHDRAW_ALL) {
                ShopService.withdrawAllFromShopAt(serverPlayer, shopPos);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_DEPOSIT_10) {
                ShopService.depositToShopAt(serverPlayer, shopPos, 10L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_WITHDRAW_10) {
                ShopService.withdrawFromShopAt(serverPlayer, shopPos, 10L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_DEPOSIT_100) {
                ShopService.depositToShopAt(serverPlayer, shopPos, 100L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_WITHDRAW_100) {
                ShopService.withdrawFromShopAt(serverPlayer, shopPos, 100L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_DEPOSIT_1000) {
                ShopService.depositToShopAt(serverPlayer, shopPos, 1000L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_WITHDRAW_1000) {
                ShopService.withdrawFromShopAt(serverPlayer, shopPos, 1000L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_OPEN_STORAGE) {
                if (!(serverPlayer.level().getBlockEntity(shopPos) instanceof ShopBlockEntity shopBlockEntity)) {
                    throw new IllegalStateException("Склад магазина не найден.");
                }

                serverPlayer.openMenu(shopBlockEntity);
                return true;
            }
            if (buttonId == BUTTON_TOGGLE_INFINITE_STOCK) {
                ShopService.setAdminInfiniteStockAt(serverPlayer, shopPos, !isInfiniteStock());
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_TOGGLE_INFINITE_BALANCE) {
                ShopService.setAdminInfiniteBalanceAt(serverPlayer, shopPos, !isInfiniteBalance());
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_TOGGLE_INDESTRUCTIBLE) {
                ShopService.setAdminIndestructibleAt(serverPlayer, shopPos, !isIndestructible());
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_PREV_TRADE) {
                moveSelection(-1, player);
                return true;
            }
            if (buttonId == BUTTON_NEXT_TRADE) {
                moveSelection(1, player);
                return true;
            }
            if (buttonId == BUTTON_ADD_SELL_TRADE) {
                ShopService.addSellTradeFromMainHandAt(serverPlayer, shopPos, 1, 1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_ADD_BUY_TRADE) {
                ShopService.addBuyTradeFromMainHandAt(serverPlayer, shopPos, 1, 1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_ADD_DUAL_TRADE) {
                ShopService.addDualTradeFromMainHandAt(serverPlayer, shopPos, 1, 1L, 1, 1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_REMOVE_TRADE) {
                ShopService.removeTradeFromShopAt(serverPlayer, shopPos, selectedTradeIndex);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_TOGGLE_TRADE_ENABLED) {
                ShopService.setTradeEnabledAt(serverPlayer, shopPos, selectedTradeIndex, !isSelectedTradeEnabled());
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_SELL_PRICE_MINUS_1) {
                ShopService.changeTradeSellPriceAt(serverPlayer, shopPos, selectedTradeIndex, -1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_SELL_PRICE_PLUS_1) {
                ShopService.changeTradeSellPriceAt(serverPlayer, shopPos, selectedTradeIndex, 1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_SELL_PRICE_MINUS_10) {
                ShopService.changeTradeSellPriceAt(serverPlayer, shopPos, selectedTradeIndex, -10L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_SELL_PRICE_PLUS_10) {
                ShopService.changeTradeSellPriceAt(serverPlayer, shopPos, selectedTradeIndex, 10L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_BUY_PRICE_MINUS_1) {
                ShopService.changeTradeBuyPriceAt(serverPlayer, shopPos, selectedTradeIndex, -1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_BUY_PRICE_PLUS_1) {
                ShopService.changeTradeBuyPriceAt(serverPlayer, shopPos, selectedTradeIndex, 1L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_BUY_PRICE_MINUS_10) {
                ShopService.changeTradeBuyPriceAt(serverPlayer, shopPos, selectedTradeIndex, -10L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_BUY_PRICE_PLUS_10) {
                ShopService.changeTradeBuyPriceAt(serverPlayer, shopPos, selectedTradeIndex, 10L);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_SELL_BATCH_MINUS_1) {
                ShopService.changeTradeSellBatchAt(serverPlayer, shopPos, selectedTradeIndex, -1);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_SELL_BATCH_PLUS_1) {
                ShopService.changeTradeSellBatchAt(serverPlayer, shopPos, selectedTradeIndex, 1);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_BUY_BATCH_MINUS_1) {
                ShopService.changeTradeBuyBatchAt(serverPlayer, shopPos, selectedTradeIndex, -1);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_BUY_BATCH_PLUS_1) {
                ShopService.changeTradeBuyBatchAt(serverPlayer, shopPos, selectedTradeIndex, 1);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId == BUTTON_TOGGLE_TRADE_MODE) {
                ShopService.toggleTradePriceModeAt(serverPlayer, shopPos, selectedTradeIndex);
                refreshTradeDisplay(player);
                return true;
            }
            if (buttonId >= BUTTON_DYNAMIC_MIN_SELL_MINUS_1 && buttonId <= BUTTON_DYNAMIC_INACTIVITY_BUY_PLUS) {
                adjustDynamicTrade(serverPlayer, buttonId);
                refreshTradeDisplay(player);
                return true;
            }
        } catch (Exception e) {
            serverPlayer.displayClientMessage(Component.literal(e.getMessage()), true);
            refreshTradeDisplay(player);
            return false;
        }

        return false;
    }

    private void adjustDynamicTrade(ServerPlayer serverPlayer, int buttonId) {
        ShopRecord shop = ShopService.getShopAt(serverPlayer, shopPos);
        ShopTradeEntry trade = shop.getTradeByHumanIndex(selectedTradeIndex);

        if (trade == null) {
            throw new IllegalStateException("Сделка не найдена.");
        }
        if (!shop.isAdminShop()) {
            throw new IllegalStateException("Динамический режим доступен только у админ-магазина.");
        }
        if (trade.getPriceMode() != PriceMode.DYNAMIC) {
            throw new IllegalStateException("Сначала включи динамический режим сделки.");
        }

        long baseSellPrice = trade.canSellToPlayer() ? Math.max(1L, trade.getSellPrice()) : 0L;
        long baseBuyPrice = trade.canBuyFromPlayer() ? Math.max(1L, trade.getBuyPrice()) : 0L;

        long minSellPrice = trade.canSellToPlayer() ? Math.max(1L, trade.getMinSellPrice()) : 1L;
        long maxSellPrice = trade.canSellToPlayer() ? Math.max(1L, trade.getMaxSellPrice()) : 1L;
        long minBuyPrice = trade.canBuyFromPlayer() ? Math.max(1L, trade.getMinBuyPrice()) : 1L;
        long maxBuyPrice = trade.canBuyFromPlayer() ? Math.max(1L, trade.getMaxBuyPrice()) : 1L;

        double elasticity = trade.getElasticity();
        double decayPerStep = trade.getDecayPerStep();
        double inactivitySellDrop = trade.getInactivitySellDrop();
        double inactivityBuyRise = trade.getInactivityBuyRise();

        if (buttonId == BUTTON_DYNAMIC_MIN_SELL_MINUS_1) {
            minSellPrice--;
        } else if (buttonId == BUTTON_DYNAMIC_MIN_SELL_PLUS_1) {
            minSellPrice++;
        } else if (buttonId == BUTTON_DYNAMIC_MAX_SELL_MINUS_1) {
            maxSellPrice--;
        } else if (buttonId == BUTTON_DYNAMIC_MAX_SELL_PLUS_1) {
            maxSellPrice++;
        } else if (buttonId == BUTTON_DYNAMIC_MIN_BUY_MINUS_1) {
            minBuyPrice--;
        } else if (buttonId == BUTTON_DYNAMIC_MIN_BUY_PLUS_1) {
            minBuyPrice++;
        } else if (buttonId == BUTTON_DYNAMIC_MAX_BUY_MINUS_1) {
            maxBuyPrice--;
        } else if (buttonId == BUTTON_DYNAMIC_MAX_BUY_PLUS_1) {
            maxBuyPrice++;
        } else if (buttonId == BUTTON_DYNAMIC_ELASTICITY_MINUS) {
            elasticity -= 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_ELASTICITY_PLUS) {
            elasticity += 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_DECAY_MINUS) {
            decayPerStep -= 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_DECAY_PLUS) {
            decayPerStep += 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_INACTIVITY_SELL_MINUS) {
            inactivitySellDrop -= 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_INACTIVITY_SELL_PLUS) {
            inactivitySellDrop += 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_INACTIVITY_BUY_MINUS) {
            inactivityBuyRise -= 0.01D;
        } else if (buttonId == BUTTON_DYNAMIC_INACTIVITY_BUY_PLUS) {
            inactivityBuyRise += 0.01D;
        }

        if (trade.canSellToPlayer()) {
            if (minSellPrice < 1L) {
                minSellPrice = 1L;
            }
            if (maxSellPrice < 1L) {
                maxSellPrice = 1L;
            }
            if (minSellPrice > baseSellPrice) {
                minSellPrice = baseSellPrice;
            }
            if (maxSellPrice < baseSellPrice) {
                maxSellPrice = baseSellPrice;
            }
            if (minSellPrice > maxSellPrice) {
                minSellPrice = maxSellPrice;
            }
        } else {
            minSellPrice = 1L;
            maxSellPrice = 1L;
        }

        if (trade.canBuyFromPlayer()) {
            if (minBuyPrice < 1L) {
                minBuyPrice = 1L;
            }
            if (maxBuyPrice < 1L) {
                maxBuyPrice = 1L;
            }
            if (minBuyPrice > baseBuyPrice) {
                minBuyPrice = baseBuyPrice;
            }
            if (maxBuyPrice < baseBuyPrice) {
                maxBuyPrice = baseBuyPrice;
            }
            if (minBuyPrice > maxBuyPrice) {
                minBuyPrice = maxBuyPrice;
            }
        } else {
            minBuyPrice = 1L;
            maxBuyPrice = 1L;
        }

        elasticity = clamp(elasticity, 0.0D, 10.0D);
        decayPerStep = clamp(decayPerStep, 0.0D, 10.0D);
        inactivitySellDrop = clamp(inactivitySellDrop, 0.0D, 10.0D);
        inactivityBuyRise = clamp(inactivityBuyRise, 0.0D, 10.0D);

        ShopService.configureDynamicTradeAt(
                serverPlayer,
                shopPos,
                selectedTradeIndex,
                baseSellPrice,
                baseBuyPrice,
                minSellPrice,
                maxSellPrice,
                minBuyPrice,
                maxBuyPrice,
                elasticity,
                decayPerStep,
                inactivitySellDrop,
                inactivityBuyRise
        );
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.level().getBlockState(shopPos).is(ModBlocks.SHOP_BLOCK.get())) {
            return false;
        }

        return player.distanceToSqr(
                shopPos.getX() + 0.5D,
                shopPos.getY() + 0.5D,
                shopPos.getZ() + 0.5D
        ) <= 64.0D;
    }
}
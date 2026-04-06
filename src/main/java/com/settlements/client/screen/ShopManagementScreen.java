package com.settlements.client.screen;

import com.settlements.world.menu.ShopManagementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;

public class ShopManagementScreen extends AbstractContainerScreen<ShopManagementMenu> {
    private static final int TAB_MAIN = 0;
    private static final int TAB_DYNAMIC = 1;

    private Button tabMainButton;
    private Button tabDynamicButton;

    private Button toggleEnabledButton;
    private Button openStorageButton;

    private Button depositAllButton;
    private Button withdrawAllButton;
    private Button deposit10Button;
    private Button withdraw10Button;
    private Button deposit100Button;
    private Button withdraw100Button;
    private Button deposit1000Button;
    private Button withdraw1000Button;

    private Button infiniteStockButton;
    private Button infiniteBalanceButton;
    private Button indestructibleButton;

    private Button prevTradeButton;
    private Button nextTradeButton;
    private Button addSellTradeButton;
    private Button addBuyTradeButton;
    private Button addDualTradeButton;
    private Button removeTradeButton;
    private Button toggleTradeEnabledButton;
    private Button toggleTradeModeButton;

    private Button sellPriceMinus1Button;
    private Button sellPricePlus1Button;
    private Button sellPriceMinus10Button;
    private Button sellPricePlus10Button;
    private Button buyPriceMinus1Button;
    private Button buyPricePlus1Button;
    private Button buyPriceMinus10Button;
    private Button buyPricePlus10Button;
    private Button sellBatchMinusButton;
    private Button sellBatchPlusButton;
    private Button buyBatchMinusButton;
    private Button buyBatchPlusButton;

    private Button dynamicMinSellMinusButton;
    private Button dynamicMinSellPlusButton;
    private Button dynamicMaxSellMinusButton;
    private Button dynamicMaxSellPlusButton;
    private Button dynamicMinBuyMinusButton;
    private Button dynamicMinBuyPlusButton;
    private Button dynamicMaxBuyMinusButton;
    private Button dynamicMaxBuyPlusButton;
    private Button dynamicElasticityMinusButton;
    private Button dynamicElasticityPlusButton;
    private Button dynamicDecayMinusButton;
    private Button dynamicDecayPlusButton;
    private Button dynamicInactivitySellMinusButton;
    private Button dynamicInactivitySellPlusButton;
    private Button dynamicInactivityBuyMinusButton;
    private Button dynamicInactivityBuyPlusButton;

    private int activeTab = TAB_MAIN;

    public ShopManagementScreen(ShopManagementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 468;
        this.imageHeight = 310;
        this.inventoryLabelY = 218;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        tabMainButton = addRenderableWidget(Button.builder(Component.literal("Основное"), b -> activeTab = TAB_MAIN)
                .bounds(left + 184, top + 84, 86, 20)
                .build());

        tabDynamicButton = addRenderableWidget(Button.builder(Component.literal("Динамика"), b -> activeTab = TAB_DYNAMIC)
                .bounds(left + 274, top + 84, 86, 20)
                .build());

        toggleEnabledButton = addRenderableWidget(Button.builder(Component.literal("Магазин"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_ENABLED))
                .bounds(left + 10, top + 28, 78, 20)
                .build());

        openStorageButton = addRenderableWidget(Button.builder(Component.literal("Склад"), b -> press(ShopManagementMenu.BUTTON_OPEN_STORAGE))
                .bounds(left + 94, top + 28, 78, 20)
                .build());

        depositAllButton = addRenderableWidget(Button.builder(Component.literal("+Все"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_ALL))
                .bounds(left + 10, top + 76, 78, 20)
                .build());

        withdrawAllButton = addRenderableWidget(Button.builder(Component.literal("-Все"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_ALL))
                .bounds(left + 94, top + 76, 78, 20)
                .build());

        deposit10Button = addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_10))
                .bounds(left + 10, top + 100, 78, 20)
                .build());

        withdraw10Button = addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_10))
                .bounds(left + 94, top + 100, 78, 20)
                .build());

        deposit100Button = addRenderableWidget(Button.builder(Component.literal("+100"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_100))
                .bounds(left + 10, top + 124, 78, 20)
                .build());

        withdraw100Button = addRenderableWidget(Button.builder(Component.literal("-100"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_100))
                .bounds(left + 94, top + 124, 78, 20)
                .build());

        deposit1000Button = addRenderableWidget(Button.builder(Component.literal("+1000"), b -> press(ShopManagementMenu.BUTTON_DEPOSIT_1000))
                .bounds(left + 10, top + 148, 78, 20)
                .build());

        withdraw1000Button = addRenderableWidget(Button.builder(Component.literal("-1000"), b -> press(ShopManagementMenu.BUTTON_WITHDRAW_1000))
                .bounds(left + 94, top + 148, 78, 20)
                .build());

        prevTradeButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> press(ShopManagementMenu.BUTTON_PREV_TRADE))
                .bounds(left + 184, top + 12, 20, 20)
                .build());

        nextTradeButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> press(ShopManagementMenu.BUTTON_NEXT_TRADE))
                .bounds(left + 208, top + 12, 20, 20)
                .build());

        infiniteStockButton = addRenderableWidget(Button.builder(Component.literal("∞ Склад"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_STOCK))
                .bounds(left + 360, top + 12, 100, 20)
                .build());

        infiniteBalanceButton = addRenderableWidget(Button.builder(Component.literal("∞ Баланс"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_BALANCE))
                .bounds(left + 360, top + 36, 100, 20)
                .build());

        indestructibleButton = addRenderableWidget(Button.builder(Component.literal("Неразр."), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INDESTRUCTIBLE))
                .bounds(left + 360, top + 60, 100, 20)
                .build());

        addSellTradeButton = addRenderableWidget(Button.builder(Component.literal("Продать"), b -> press(ShopManagementMenu.BUTTON_ADD_SELL_TRADE))
                .bounds(left + 184, top + 112, 84, 20)
                .build());

        addBuyTradeButton = addRenderableWidget(Button.builder(Component.literal("Скупать"), b -> press(ShopManagementMenu.BUTTON_ADD_BUY_TRADE))
                .bounds(left + 272, top + 112, 84, 20)
                .build());

        addDualTradeButton = addRenderableWidget(Button.builder(Component.literal("Обе"), b -> press(ShopManagementMenu.BUTTON_ADD_DUAL_TRADE))
                .bounds(left + 360, top + 112, 100, 20)
                .build());

        removeTradeButton = addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> press(ShopManagementMenu.BUTTON_REMOVE_TRADE))
                .bounds(left + 184, top + 136, 134, 20)
                .build());

        toggleTradeEnabledButton = addRenderableWidget(Button.builder(Component.literal("Сделка"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_TRADE_ENABLED))
                .bounds(left + 326, top + 136, 134, 20)
                .build());

        toggleTradeModeButton = addRenderableWidget(Button.builder(Component.literal("Режим"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_TRADE_MODE))
                .bounds(left + 326, top + 112, 134, 20)
                .build());

        sellPriceMinus1Button = addRenderableWidget(Button.builder(Component.literal("-1"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_MINUS_1))
                .bounds(left + 346, top + 156, 26, 20)
                .build());

        sellPricePlus1Button = addRenderableWidget(Button.builder(Component.literal("+1"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_PLUS_1))
                .bounds(left + 374, top + 156, 26, 20)
                .build());

        sellPriceMinus10Button = addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_MINUS_10))
                .bounds(left + 402, top + 156, 28, 20)
                .build());

        sellPricePlus10Button = addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_PLUS_10))
                .bounds(left + 432, top + 156, 28, 20)
                .build());

        buyPriceMinus1Button = addRenderableWidget(Button.builder(Component.literal("-1"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_MINUS_1))
                .bounds(left + 346, top + 172, 26, 20)
                .build());

        buyPricePlus1Button = addRenderableWidget(Button.builder(Component.literal("+1"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_PLUS_1))
                .bounds(left + 374, top + 172, 26, 20)
                .build());

        buyPriceMinus10Button = addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_MINUS_10))
                .bounds(left + 402, top + 172, 28, 20)
                .build());

        buyPricePlus10Button = addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_PLUS_10))
                .bounds(left + 432, top + 172, 28, 20)
                .build());

        sellBatchMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_SELL_BATCH_MINUS_1))
                .bounds(left + 404, top + 188, 26, 20)
                .build());

        sellBatchPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_SELL_BATCH_PLUS_1))
                .bounds(left + 432, top + 188, 28, 20)
                .build());

        buyBatchMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_BUY_BATCH_MINUS_1))
                .bounds(left + 404, top + 204, 26, 20)
                .build());

        buyBatchPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_BUY_BATCH_PLUS_1))
                .bounds(left + 432, top + 204, 28, 20)
                .build());

        dynamicMinSellMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MIN_SELL_MINUS_1))
                .bounds(left + 286, top + 124, 18, 18)
                .build());
        dynamicMinSellPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MIN_SELL_PLUS_1))
                .bounds(left + 308, top + 124, 18, 18)
                .build());
        dynamicMaxSellMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MAX_SELL_MINUS_1))
                .bounds(left + 286, top + 140, 18, 18)
                .build());
        dynamicMaxSellPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MAX_SELL_PLUS_1))
                .bounds(left + 308, top + 140, 18, 18)
                .build());
        dynamicMinBuyMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MIN_BUY_MINUS_1))
                .bounds(left + 286, top + 156, 18, 18)
                .build());
        dynamicMinBuyPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MIN_BUY_PLUS_1))
                .bounds(left + 308, top + 156, 18, 18)
                .build());
        dynamicMaxBuyMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MAX_BUY_MINUS_1))
                .bounds(left + 286, top + 172, 18, 18)
                .build());
        dynamicMaxBuyPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_MAX_BUY_PLUS_1))
                .bounds(left + 308, top + 172, 18, 18)
                .build());
        dynamicElasticityMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_ELASTICITY_MINUS))
                .bounds(left + 420, top + 124, 18, 18)
                .build());
        dynamicElasticityPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_ELASTICITY_PLUS))
                .bounds(left + 442, top + 124, 18, 18)
                .build());
        dynamicDecayMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_DECAY_MINUS))
                .bounds(left + 420, top + 140, 18, 18)
                .build());
        dynamicDecayPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_DECAY_PLUS))
                .bounds(left + 442, top + 140, 18, 18)
                .build());
        dynamicInactivitySellMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_INACTIVITY_SELL_MINUS))
                .bounds(left + 420, top + 156, 18, 18)
                .build());
        dynamicInactivitySellPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_INACTIVITY_SELL_PLUS))
                .bounds(left + 442, top + 156, 18, 18)
                .build());
        dynamicInactivityBuyMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_INACTIVITY_BUY_MINUS))
                .bounds(left + 420, top + 172, 18, 18)
                .build());
        dynamicInactivityBuyPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_DYNAMIC_INACTIVITY_BUY_PLUS))
                .bounds(left + 442, top + 172, 18, 18)
                .build());

        updateButtons();
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        boolean admin = menu.isAdminShop();
        boolean hasTrade = menu.hasSelectedTrade();
        boolean canSell = hasTrade && menu.selectedTradeCanSell();
        boolean canBuy = hasTrade && menu.selectedTradeCanBuy();
        boolean dynamic = hasTrade && menu.isSelectedTradeDynamic();
        boolean showMainTab = activeTab == TAB_MAIN;
        boolean showDynamicTab = activeTab == TAB_DYNAMIC;

        if (!admin && activeTab == TAB_DYNAMIC) {
            activeTab = TAB_MAIN;
            showMainTab = true;
            showDynamicTab = false;
        }

        toggleEnabledButton.setMessage(Component.literal(menu.isEnabled() ? "Магазин: Вкл" : "Магазин: Выкл"));
        toggleTradeEnabledButton.setMessage(Component.literal(hasTrade
                ? (menu.isSelectedTradeEnabled() ? "Сделка: Вкл" : "Сделка: Выкл")
                : "Сделка"));
        toggleTradeModeButton.setMessage(Component.literal(hasTrade
                ? (menu.isSelectedTradeDynamic() ? "Режим: Дин." : "Режим: Фикс.")
                : "Режим"));

        infiniteStockButton.setMessage(Component.literal("∞ Склад: " + yesNo(menu.isInfiniteStock())));
        infiniteBalanceButton.setMessage(Component.literal("∞ Баланс: " + yesNo(menu.isInfiniteBalance())));
        indestructibleButton.setMessage(Component.literal("Неразр.: " + yesNo(menu.isIndestructible())));

        tabMainButton.visible = true;
        tabMainButton.active = activeTab != TAB_MAIN;

        tabDynamicButton.visible = admin;
        tabDynamicButton.active = admin && activeTab != TAB_DYNAMIC;

        prevTradeButton.active = menu.getTradeCount() > 1;
        nextTradeButton.active = menu.getTradeCount() > 1;

        infiniteStockButton.visible = admin && showMainTab;
        infiniteBalanceButton.visible = admin && showMainTab;
        indestructibleButton.visible = admin && showMainTab;

        infiniteStockButton.active = admin && showMainTab;
        infiniteBalanceButton.active = admin && showMainTab;
        indestructibleButton.active = admin && showMainTab;

        addSellTradeButton.visible = showMainTab;
        addBuyTradeButton.visible = showMainTab;
        addDualTradeButton.visible = showMainTab;
        removeTradeButton.visible = showMainTab;
        toggleTradeEnabledButton.visible = showMainTab;
        toggleTradeModeButton.visible = showMainTab || showDynamicTab;

        addSellTradeButton.active = showMainTab;
        addBuyTradeButton.active = showMainTab;
        addDualTradeButton.active = showMainTab;
        removeTradeButton.active = showMainTab && hasTrade;
        toggleTradeEnabledButton.active = showMainTab && hasTrade;
        toggleTradeModeButton.active = admin && hasTrade;

        setVisibleAndActive(sellPriceMinus1Button, showMainTab, canSell);
        setVisibleAndActive(sellPricePlus1Button, showMainTab, canSell);
        setVisibleAndActive(sellPriceMinus10Button, showMainTab, canSell);
        setVisibleAndActive(sellPricePlus10Button, showMainTab, canSell);

        setVisibleAndActive(buyPriceMinus1Button, showMainTab, canBuy);
        setVisibleAndActive(buyPricePlus1Button, showMainTab, canBuy);
        setVisibleAndActive(buyPriceMinus10Button, showMainTab, canBuy);
        setVisibleAndActive(buyPricePlus10Button, showMainTab, canBuy);

        setVisibleAndActive(sellBatchMinusButton, showMainTab, canSell);
        setVisibleAndActive(sellBatchPlusButton, showMainTab, canSell);
        setVisibleAndActive(buyBatchMinusButton, showMainTab, canBuy);
        setVisibleAndActive(buyBatchPlusButton, showMainTab, canBuy);

        boolean dynamicEditable = showDynamicTab && admin && hasTrade && dynamic;
        setVisibleAndActive(dynamicMinSellMinusButton, showDynamicTab && canSell, dynamicEditable && canSell);
        setVisibleAndActive(dynamicMinSellPlusButton, showDynamicTab && canSell, dynamicEditable && canSell);
        setVisibleAndActive(dynamicMaxSellMinusButton, showDynamicTab && canSell, dynamicEditable && canSell);
        setVisibleAndActive(dynamicMaxSellPlusButton, showDynamicTab && canSell, dynamicEditable && canSell);
        setVisibleAndActive(dynamicMinBuyMinusButton, showDynamicTab && canBuy, dynamicEditable && canBuy);
        setVisibleAndActive(dynamicMinBuyPlusButton, showDynamicTab && canBuy, dynamicEditable && canBuy);
        setVisibleAndActive(dynamicMaxBuyMinusButton, showDynamicTab && canBuy, dynamicEditable && canBuy);
        setVisibleAndActive(dynamicMaxBuyPlusButton, showDynamicTab && canBuy, dynamicEditable && canBuy);
        setVisibleAndActive(dynamicElasticityMinusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicElasticityPlusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicDecayMinusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicDecayPlusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicInactivitySellMinusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicInactivitySellPlusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicInactivityBuyMinusButton, showDynamicTab, dynamicEditable);
        setVisibleAndActive(dynamicInactivityBuyPlusButton, showDynamicTab, dynamicEditable);
    }

    private void setVisibleAndActive(Button button, boolean visible, boolean active) {
        button.visible = visible;
        button.active = visible && active;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF232323);

        graphics.fill(left + 4, top + 4, left + 176, top + 222, 0xFF343434);
        graphics.fill(left + 180, top + 4, left + imageWidth - 4, top + 222, 0xFF2F2F2F);
        graphics.fill(left + 4, top + 224, left + imageWidth - 4, top + imageHeight - 4, 0xFF303030);

        graphics.fill(left + 186, top + 38, left + 204, top + 56, 0xFF555555);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 7 + column * 18;
                int slotY = top + 228 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 7 + column * 18;
            int slotY = top + 286;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, "Управление: " + trimToWidth(this.menu.getShopName(), 160), 8, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, "Баланс: " + this.menu.getBalance(), 10, 16, 0xE0E0E0, false);

        graphics.drawString(this.font, "Пополнение", 16, 66, 0xFFFFFF, false);
        graphics.drawString(this.font, "Вывод", 112, 66, 0xFFFFFF, false);

        graphics.drawString(this.font, "Сделки", 184, 32, 0xFFFFFF, false);
        graphics.drawString(this.font, tradeCounterText(), 234, 18, 0xE0E0E0, false);

        ItemStack selected = this.menu.getSelectedTradeDisplayStack();
        String tradeName = selected.isEmpty() ? "Нет сделки" : selected.getHoverName().getString();

        graphics.drawString(this.font, trimToWidth(tradeName, 132), 212, 42, 0xFFFFFF, false);
        graphics.drawString(this.font, "Статус: " + selectedTradeStatus(), 212, 54, 0xD8D8D8, false);
        graphics.drawString(this.font, "Тип: " + selectedTradeType(), 212, 66, 0xD8D8D8, false);

        if (activeTab == TAB_MAIN) {
            graphics.drawString(this.font, "ЛКМ по иконке", 362, 42, 0xC8C8C8, false);
            graphics.drawString(this.font, "сменить предмет", 362, 54, 0xC8C8C8, false);

            if (this.menu.isAdminShop()) {
                graphics.drawString(this.font, "Тип магазина: админ", 362, 66, 0xFFD080, false);
            } else {
                graphics.drawString(this.font, "Тип магазина: игрок", 362, 66, 0xA8FFA8, false);
            }

            graphics.drawString(this.font, "Продажа: " + tradeValueText(this.menu.selectedTradeCanSell(), this.menu.getSelectedTradeSellPrice()), 184, 162, 0xFFFFFF, false);
            graphics.drawString(this.font, "Скупка: " + tradeValueText(this.menu.selectedTradeCanBuy(), this.menu.getSelectedTradeBuyPrice()), 184, 178, 0xFFFFFF, false);
            graphics.drawString(this.font, "Пачка прод.: " + batchText(this.menu.selectedTradeCanSell(), this.menu.getSelectedTradeSellBatch()), 184, 194, 0xFFFFFF, false);
            graphics.drawString(this.font, "Пачка скуп.: " + batchText(this.menu.selectedTradeCanBuy(), this.menu.getSelectedTradeBuyBatch()), 184, 210, 0xFFFFFF, false);
        } else {
            drawDynamicTabLabels(graphics);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private void drawDynamicTabLabels(GuiGraphics graphics) {
        if (!this.menu.isAdminShop()) {
            graphics.drawString(this.font, "Динамика доступна только", 184, 112, 0xFFB0B0, false);
            graphics.drawString(this.font, "для админ-магазина.", 184, 124, 0xFFB0B0, false);
            return;
        }

        graphics.drawString(this.font, "База прод.: " + tradeValueText(this.menu.selectedTradeCanSell(), this.menu.getSelectedTradeSellPrice()), 184, 96, 0xFFFFFF, false);
        graphics.drawString(this.font, "База скуп.: " + tradeValueText(this.menu.selectedTradeCanBuy(), this.menu.getSelectedTradeBuyPrice()), 184, 108, 0xFFFFFF, false);

        if (!this.menu.hasSelectedTrade()) {
            graphics.drawString(this.font, "Выбери сделку.", 184, 148, 0xFFB0B0, false);
            return;
        }

        if (!this.menu.isSelectedTradeDynamic()) {
            graphics.drawString(this.font, "Сделка сейчас в фиксированном", 184, 140, 0xFFB0B0, false);
            graphics.drawString(this.font, "режиме. Переключи режим", 184, 152, 0xFFB0B0, false);
            graphics.drawString(this.font, "кнопкой \"Режим\" выше.", 184, 164, 0xFFB0B0, false);
            return;
        }

        graphics.drawString(this.font, dynamicRow("Мин. продажа", this.menu.selectedTradeCanSell(), String.valueOf(this.menu.getSelectedTradeMinSellPrice())), 184, 128, 0xFFFFFF, false);
        graphics.drawString(this.font, dynamicRow("Макс. продажа", this.menu.selectedTradeCanSell(), String.valueOf(this.menu.getSelectedTradeMaxSellPrice())), 184, 144, 0xFFFFFF, false);
        graphics.drawString(this.font, dynamicRow("Мин. скупка", this.menu.selectedTradeCanBuy(), String.valueOf(this.menu.getSelectedTradeMinBuyPrice())), 184, 160, 0xFFFFFF, false);
        graphics.drawString(this.font, dynamicRow("Макс. скупка", this.menu.selectedTradeCanBuy(), String.valueOf(this.menu.getSelectedTradeMaxBuyPrice())), 184, 176, 0xFFFFFF, false);
        graphics.drawString(this.font, "Эластичность: " + formatDouble(this.menu.getSelectedTradeElasticity()), 316, 128, 0xFFFFFF, false);
        graphics.drawString(this.font, "Затухание: " + formatDouble(this.menu.getSelectedTradeDecayPerStep()), 316, 144, 0xFFFFFF, false);
        graphics.drawString(this.font, "Пад. продажи: " + formatDouble(this.menu.getSelectedTradeInactivitySellDrop()), 316, 160, 0xFFFFFF, false);
        graphics.drawString(this.font, "Рост скупки: " + formatDouble(this.menu.getSelectedTradeInactivityBuyRise()), 316, 176, 0xFFFFFF, false);
    }

    private String dynamicRow(String label, boolean supported, String value) {
        return label + ": " + (supported ? value : "—");
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String tradeCounterText() {
        if (this.menu.getTradeCount() <= 0) {
            return "0 / 0";
        }
        return this.menu.getSelectedTradeIndex() + " / " + this.menu.getTradeCount();
    }

    private String selectedTradeStatus() {
        if (!this.menu.hasSelectedTrade()) {
            return "—";
        }
        return this.menu.isSelectedTradeEnabled() ? "вкл" : "выкл";
    }

    private String selectedTradeType() {
        if (!this.menu.hasSelectedTrade()) {
            return "—";
        }
        if (this.menu.selectedTradeCanSell() && this.menu.selectedTradeCanBuy()) {
            return "двусторонняя";
        }
        if (this.menu.selectedTradeCanSell()) {
            return "продажа";
        }
        if (this.menu.selectedTradeCanBuy()) {
            return "скупка";
        }
        return "—";
    }

    private String tradeValueText(boolean supported, long value) {
        return supported ? String.valueOf(value) : "—";
    }

    private String batchText(boolean supported, int value) {
        return supported ? String.valueOf(value) : "—";
    }

    private String yesNo(boolean value) {
        return value ? "Да" : "Нет";
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int allowed = maxWidth - this.font.width(ellipsis);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (this.font.width(builder.toString() + ch) > allowed) {
                break;
            }
            builder.append(ch);
        }

        return builder + ellipsis;
    }

    private boolean isHoveringSelectedTradeIcon(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + 186
                && mouseX < this.leftPos + 204
                && mouseY >= this.topPos + 38
                && mouseY < this.topPos + 56;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (isHoveringSelectedTradeIcon(mouseX, mouseY)) {
            if (this.menu.hasSelectedTrade()) {
                graphics.renderTooltip(
                        this.font,
                        Arrays.asList(
                                Component.literal("Возьми предмет курсором"),
                                Component.literal("и нажми на иконку сделки,"),
                                Component.literal("чтобы заменить товар.")
                        ),
                        ItemStack.EMPTY.getTooltipImage(),
                        mouseX,
                        mouseY
                );
            } else {
                graphics.renderTooltip(
                        this.font,
                        Arrays.asList(
                                Component.literal("Новые сделки создаются"),
                                Component.literal("по предмету из главной руки.")
                        ),
                        ItemStack.EMPTY.getTooltipImage(),
                        mouseX,
                        mouseY
                );
            }
            return;
        }

        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
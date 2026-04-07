package com.settlements.client.screen;

import com.settlements.network.ShopManagementPackets;
import com.settlements.network.packet.C2SShopManagementEditFieldPacket;
import com.settlements.world.menu.ShopManagementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public class ShopManagementScreen extends AbstractContainerScreen<ShopManagementMenu> {
    private static final int TAB_MAIN = 0;
    private static final int TAB_DYNAMIC = 1;

    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d*");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d*([\\.,]\\d*)?");

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

    private EditBox sellPriceBox;
    private EditBox buyPriceBox;
    private EditBox sellBatchBox;
    private EditBox buyBatchBox;

    private EditBox dynamicMinSellBox;
    private EditBox dynamicMaxSellBox;
    private EditBox dynamicMinBuyBox;
    private EditBox dynamicMaxBuyBox;
    private EditBox dynamicElasticityBox;
    private EditBox dynamicDecayBox;
    private EditBox dynamicInactivitySellBox;
    private EditBox dynamicInactivityBuyBox;

    private int activeTab = TAB_MAIN;

    public ShopManagementScreen(ShopManagementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 494;
        this.imageHeight = 310;
        this.inventoryLabelY = 218;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        tabMainButton = addRenderableWidget(Button.builder(Component.literal("Основное"), b -> {
                    commitFocusedBoxes();
                    activeTab = TAB_MAIN;
                    updateButtons();
                })
                .bounds(left + 184, top + 84, 96, 20)
                .build());

        tabDynamicButton = addRenderableWidget(Button.builder(Component.literal("Динамика"), b -> {
                    commitFocusedBoxes();
                    activeTab = TAB_DYNAMIC;
                    updateButtons();
                })
                .bounds(left + 284, top + 84, 96, 20)
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

        prevTradeButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_PREV_TRADE);
                })
                .bounds(left + 184, top + 12, 20, 20)
                .build());

        nextTradeButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_NEXT_TRADE);
                })
                .bounds(left + 208, top + 12, 20, 20)
                .build());

        infiniteStockButton = addRenderableWidget(Button.builder(Component.literal("∞ Склад"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_STOCK))
                .bounds(left + 388, top + 12, 96, 20)
                .build());

        infiniteBalanceButton = addRenderableWidget(Button.builder(Component.literal("∞ Баланс"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_BALANCE))
                .bounds(left + 388, top + 36, 96, 20)
                .build());

        indestructibleButton = addRenderableWidget(Button.builder(Component.literal("Неразр."), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INDESTRUCTIBLE))
                .bounds(left + 388, top + 60, 96, 20)
                .build());

        toggleTradeModeButton = addRenderableWidget(Button.builder(Component.literal("Режим"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_TOGGLE_TRADE_MODE);
                })
                .bounds(left + 388, top + 84, 96, 20)
                .build());

        addSellTradeButton = addRenderableWidget(Button.builder(Component.literal("Продать"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_ADD_SELL_TRADE);
                })
                .bounds(left + 184, top + 112, 92, 20)
                .build());

        addBuyTradeButton = addRenderableWidget(Button.builder(Component.literal("Скупать"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_ADD_BUY_TRADE);
                })
                .bounds(left + 280, top + 112, 92, 20)
                .build());

        addDualTradeButton = addRenderableWidget(Button.builder(Component.literal("Обе"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_ADD_DUAL_TRADE);
                })
                .bounds(left + 376, top + 112, 108, 20)
                .build());

        removeTradeButton = addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_REMOVE_TRADE);
                })
                .bounds(left + 184, top + 136, 146, 20)
                .build());

        toggleTradeEnabledButton = addRenderableWidget(Button.builder(Component.literal("Сделка"), b -> {
                    commitFocusedBoxes();
                    press(ShopManagementMenu.BUTTON_TOGGLE_TRADE_ENABLED);
                })
                .bounds(left + 334, top + 136, 150, 20)
                .build());

        sellPriceBox = createIntegerBox(left + 390, top + 184, 94, 20, 18);
        buyPriceBox = createIntegerBox(left + 390, top + 206, 94, 20, 18);
        sellBatchBox = createIntegerBox(left + 390, top + 228, 94, 20, 12);
        buyBatchBox = createIntegerBox(left + 390, top + 250, 94, 20, 12);

        dynamicMinSellBox = createIntegerBox(left + 276, top + 142, 56, 18, 18);
        dynamicMaxSellBox = createIntegerBox(left + 276, top + 164, 56, 18, 18);
        dynamicMinBuyBox = createIntegerBox(left + 276, top + 186, 56, 18, 18);
        dynamicMaxBuyBox = createIntegerBox(left + 276, top + 208, 56, 18, 18);

        dynamicElasticityBox = createDecimalBox(left + 430, top + 142, 54, 18, 18);
        dynamicDecayBox = createDecimalBox(left + 430, top + 164, 54, 18, 18);
        dynamicInactivitySellBox = createDecimalBox(left + 430, top + 186, 54, 18, 18);
        dynamicInactivityBuyBox = createDecimalBox(left + 430, top + 208, 54, 18, 18);

        syncBoxesFromMenu(true);
        updateButtons();
    }

    private EditBox createIntegerBox(int x, int y, int width, int height, int maxLength) {
        EditBox box = new EditBox(this.font, x, y, width, height, Component.empty());
        box.setMaxLength(maxLength);
        box.setFilter(text -> INTEGER_PATTERN.matcher(text).matches());
        box.setBordered(true);
        addRenderableWidget(box);
        return box;
    }

    private EditBox createDecimalBox(int x, int y, int width, int height, int maxLength) {
        EditBox box = new EditBox(this.font, x, y, width, height, Component.empty());
        box.setMaxLength(maxLength);
        box.setFilter(text -> DECIMAL_PATTERN.matcher(text).matches());
        box.setBordered(true);
        addRenderableWidget(box);
        return box;
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncBoxesFromMenu(false);
        updateButtons();
    }

    private void syncBoxesFromMenu(boolean force) {
        if (force || !sellPriceBox.isFocused()) {
            syncBox(sellPriceBox, menu.selectedTradeCanSell() ? String.valueOf(menu.getSelectedTradeSellPrice()) : "");
        }
        if (force || !buyPriceBox.isFocused()) {
            syncBox(buyPriceBox, menu.selectedTradeCanBuy() ? String.valueOf(menu.getSelectedTradeBuyPrice()) : "");
        }
        if (force || !sellBatchBox.isFocused()) {
            syncBox(sellBatchBox, menu.selectedTradeCanSell() ? String.valueOf(menu.getSelectedTradeSellBatch()) : "");
        }
        if (force || !buyBatchBox.isFocused()) {
            syncBox(buyBatchBox, menu.selectedTradeCanBuy() ? String.valueOf(menu.getSelectedTradeBuyBatch()) : "");
        }

        if (force || !dynamicMinSellBox.isFocused()) {
            syncBox(dynamicMinSellBox, menu.selectedTradeCanSell() ? String.valueOf(menu.getSelectedTradeMinSellPrice()) : "");
        }
        if (force || !dynamicMaxSellBox.isFocused()) {
            syncBox(dynamicMaxSellBox, menu.selectedTradeCanSell() ? String.valueOf(menu.getSelectedTradeMaxSellPrice()) : "");
        }
        if (force || !dynamicMinBuyBox.isFocused()) {
            syncBox(dynamicMinBuyBox, menu.selectedTradeCanBuy() ? String.valueOf(menu.getSelectedTradeMinBuyPrice()) : "");
        }
        if (force || !dynamicMaxBuyBox.isFocused()) {
            syncBox(dynamicMaxBuyBox, menu.selectedTradeCanBuy() ? String.valueOf(menu.getSelectedTradeMaxBuyPrice()) : "");
        }
        if (force || !dynamicElasticityBox.isFocused()) {
            syncBox(dynamicElasticityBox, formatEditableDouble(menu.getSelectedTradeElasticity()));
        }
        if (force || !dynamicDecayBox.isFocused()) {
            syncBox(dynamicDecayBox, formatEditableDouble(menu.getSelectedTradeDecayPerStep()));
        }
        if (force || !dynamicInactivitySellBox.isFocused()) {
            syncBox(dynamicInactivitySellBox, formatEditableDouble(menu.getSelectedTradeInactivitySellDrop()));
        }
        if (force || !dynamicInactivityBuyBox.isFocused()) {
            syncBox(dynamicInactivityBuyBox, formatEditableDouble(menu.getSelectedTradeInactivityBuyRise()));
        }
    }

    private void syncBox(EditBox box, String value) {
        if (!value.equals(box.getValue())) {
            box.setValue(value);
        }
    }

    private void updateButtons() {
        boolean admin = menu.isAdminShop();
        boolean hasTrade = menu.hasSelectedTrade();
        boolean canSell = hasTrade && menu.selectedTradeCanSell();
        boolean canBuy = hasTrade && menu.selectedTradeCanBuy();
        boolean dynamic = hasTrade && menu.isSelectedTradeDynamic();

        if (!admin && activeTab == TAB_DYNAMIC) {
            activeTab = TAB_MAIN;
        }

        boolean showMainTab = activeTab == TAB_MAIN;
        boolean showDynamicTab = activeTab == TAB_DYNAMIC;

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

        infiniteStockButton.visible = admin;
        infiniteBalanceButton.visible = admin;
        indestructibleButton.visible = admin;
        infiniteStockButton.active = admin;
        infiniteBalanceButton.active = admin;
        indestructibleButton.active = admin;

        toggleTradeModeButton.visible = admin;
        toggleTradeModeButton.active = admin && hasTrade;

        setVisibleAndActive(addSellTradeButton, showMainTab, showMainTab);
        setVisibleAndActive(addBuyTradeButton, showMainTab, showMainTab);
        setVisibleAndActive(addDualTradeButton, showMainTab, showMainTab);
        setVisibleAndActive(removeTradeButton, showMainTab, showMainTab && hasTrade);
        setVisibleAndActive(toggleTradeEnabledButton, showMainTab, showMainTab && hasTrade);

        setBoxState(sellPriceBox, showMainTab && canSell, showMainTab && canSell);
        setBoxState(buyPriceBox, showMainTab && canBuy, showMainTab && canBuy);
        setBoxState(sellBatchBox, showMainTab && canSell, showMainTab && canSell);
        setBoxState(buyBatchBox, showMainTab && canBuy, showMainTab && canBuy);

        boolean dynamicEditable = showDynamicTab && admin && hasTrade && dynamic;

        setBoxState(dynamicMinSellBox, showDynamicTab && canSell, dynamicEditable && canSell);
        setBoxState(dynamicMaxSellBox, showDynamicTab && canSell, dynamicEditable && canSell);
        setBoxState(dynamicMinBuyBox, showDynamicTab && canBuy, dynamicEditable && canBuy);
        setBoxState(dynamicMaxBuyBox, showDynamicTab && canBuy, dynamicEditable && canBuy);

        setBoxState(dynamicElasticityBox, showDynamicTab, dynamicEditable);
        setBoxState(dynamicDecayBox, showDynamicTab, dynamicEditable);
        setBoxState(dynamicInactivitySellBox, showDynamicTab, dynamicEditable);
        setBoxState(dynamicInactivityBuyBox, showDynamicTab, dynamicEditable);
    }

    private void setVisibleAndActive(Button button, boolean visible, boolean active) {
        button.visible = visible;
        button.active = visible && active;
    }

    private void setBoxState(EditBox box, boolean visible, boolean editable) {
        box.visible = visible;
        box.active = visible && editable;
        box.setEditable(visible && editable);
        if (!visible) {
            box.setFocused(false);
        }
    }

    private void commitFocusedBoxes() {
        commitBoxIfFocused(sellPriceBox, C2SShopManagementEditFieldPacket.FIELD_SELL_PRICE);
        commitBoxIfFocused(buyPriceBox, C2SShopManagementEditFieldPacket.FIELD_BUY_PRICE);
        commitBoxIfFocused(sellBatchBox, C2SShopManagementEditFieldPacket.FIELD_SELL_BATCH);
        commitBoxIfFocused(buyBatchBox, C2SShopManagementEditFieldPacket.FIELD_BUY_BATCH);

        commitBoxIfFocused(dynamicMinSellBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MIN_SELL);
        commitBoxIfFocused(dynamicMaxSellBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MAX_SELL);
        commitBoxIfFocused(dynamicMinBuyBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MIN_BUY);
        commitBoxIfFocused(dynamicMaxBuyBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MAX_BUY);
        commitBoxIfFocused(dynamicElasticityBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_ELASTICITY);
        commitBoxIfFocused(dynamicDecayBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_DECAY);
        commitBoxIfFocused(dynamicInactivitySellBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_INACTIVITY_SELL);
        commitBoxIfFocused(dynamicInactivityBuyBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_INACTIVITY_BUY);
    }

    private void commitVisibleBoxes() {
        commitBoxIfFocused(sellPriceBox, C2SShopManagementEditFieldPacket.FIELD_SELL_PRICE);
        commitBoxIfFocused(buyPriceBox, C2SShopManagementEditFieldPacket.FIELD_BUY_PRICE);
        commitBoxIfFocused(sellBatchBox, C2SShopManagementEditFieldPacket.FIELD_SELL_BATCH);
        commitBoxIfFocused(buyBatchBox, C2SShopManagementEditFieldPacket.FIELD_BUY_BATCH);

        commitBoxIfFocused(dynamicMinSellBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MIN_SELL);
        commitBoxIfFocused(dynamicMaxSellBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MAX_SELL);
        commitBoxIfFocused(dynamicMinBuyBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MIN_BUY);
        commitBoxIfFocused(dynamicMaxBuyBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MAX_BUY);
        commitBoxIfFocused(dynamicElasticityBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_ELASTICITY);
        commitBoxIfFocused(dynamicDecayBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_DECAY);
        commitBoxIfFocused(dynamicInactivitySellBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_INACTIVITY_SELL);
        commitBoxIfFocused(dynamicInactivityBuyBox, C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_INACTIVITY_BUY);
    }

    private void commitBoxIfFocused(EditBox box, String field) {
        if (!box.isFocused() || !box.visible || !box.active) {
            return;
        }

        String value = box.getValue().trim();
        if (value.isEmpty()) {
            syncBoxesFromMenu(true);
            return;
        }

        try {
            validateField(field, value);
            ShopManagementPackets.sendEditField(field, value);
        } catch (Exception e) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal(e.getMessage()), true);
            }
            syncBoxesFromMenu(true);
        }
    }

    private void validateField(String field, String value) {
        if (C2SShopManagementEditFieldPacket.FIELD_SELL_PRICE.equals(field)
                || C2SShopManagementEditFieldPacket.FIELD_BUY_PRICE.equals(field)
                || C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MIN_SELL.equals(field)
                || C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MAX_SELL.equals(field)
                || C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MIN_BUY.equals(field)
                || C2SShopManagementEditFieldPacket.FIELD_DYNAMIC_MAX_BUY.equals(field)) {
            long parsed = Long.parseLong(value);
            if (parsed <= 0L) {
                throw new IllegalArgumentException("Нужно число больше нуля.");
            }
            return;
        }

        if (C2SShopManagementEditFieldPacket.FIELD_SELL_BATCH.equals(field)
                || C2SShopManagementEditFieldPacket.FIELD_BUY_BATCH.equals(field)) {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("Нужно число больше нуля.");
            }
            return;
        }

        double parsed = Double.parseDouble(value.replace(',', '.'));
        if (parsed < 0.0D) {
            throw new IllegalArgumentException("Нужно число не меньше нуля.");
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        commitFocusedBoxes();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
            commitFocusedBoxes();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        commitVisibleBoxes();
        super.removed();
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
        graphics.drawString(this.font, tradeCounterText(), 236, 18, 0xE0E0E0, false);

        ItemStack selected = this.menu.getSelectedTradeDisplayStack();
        String tradeName = selected.isEmpty() ? "Нет сделки" : selected.getHoverName().getString();

        graphics.drawString(this.font, trimToWidth(tradeName, 160), 212, 42, 0xFFFFFF, false);
        graphics.drawString(this.font, "Статус: " + selectedTradeStatus(), 212, 54, 0xD8D8D8, false);
        graphics.drawString(this.font, "Тип: " + selectedTradeType(), 212, 66, 0xD8D8D8, false);

        if (this.menu.isAdminShop()) {
            graphics.drawString(this.font, "Тип магазина: админ", 212, 74, 0xFFD080, false);
        } else {
            graphics.drawString(this.font, "Тип магазина: игрок", 212, 74, 0xA8FFA8, false);
        }

        if (activeTab == TAB_MAIN) {
            drawMainTabLabels(graphics);
        } else {
            drawDynamicTabLabels(graphics);
        }

        graphics.drawString(this.font, "Enter = применить", 184, 270, 0xB8B8B8, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private void drawMainTabLabels(GuiGraphics graphics) {
        graphics.drawString(this.font, "Продажа:", 184, 190, 0xFFFFFF, false);
        graphics.drawString(this.font, "Скупка:", 184, 212, 0xFFFFFF, false);
        graphics.drawString(this.font, "Пачка прод.:", 184, 234, 0xFFFFFF, false);
        graphics.drawString(this.font, "Пачка скуп.:", 184, 256, 0xFFFFFF, false);
    }

    private void drawDynamicTabLabels(GuiGraphics graphics) {
        graphics.drawString(this.font, "База прод.: " + tradeValueText(this.menu.selectedTradeCanSell(), this.menu.getSelectedTradeSellPrice()), 184, 112, 0xFFFFFF, false);
        graphics.drawString(this.font, "База скуп.: " + tradeValueText(this.menu.selectedTradeCanBuy(), this.menu.getSelectedTradeBuyPrice()), 184, 124, 0xFFFFFF, false);

        if (!this.menu.isAdminShop()) {
            graphics.drawString(this.font, "Динамика доступна только", 184, 148, 0xFFB0B0, false);
            graphics.drawString(this.font, "для админ-магазина.", 184, 160, 0xFFB0B0, false);
            return;
        }

        if (!this.menu.hasSelectedTrade()) {
            graphics.drawString(this.font, "Выбери сделку.", 184, 148, 0xFFB0B0, false);
            return;
        }

        if (!this.menu.isSelectedTradeDynamic()) {
            graphics.drawString(this.font, "Сделка сейчас в фиксированном режиме.", 184, 148, 0xFFB0B0, false);
            graphics.drawString(this.font, "Сначала нажми кнопку \"Режим\" сверху.", 184, 160, 0xFFB0B0, false);
            return;
        }

        graphics.drawString(this.font, "Мин. продажа:", 184, 146, 0xFFFFFF, false);
        graphics.drawString(this.font, "Макс. продажа:", 184, 168, 0xFFFFFF, false);
        graphics.drawString(this.font, "Мин. скупка:", 184, 190, 0xFFFFFF, false);
        graphics.drawString(this.font, "Макс. скупка:", 184, 212, 0xFFFFFF, false);

        graphics.drawString(this.font, "Эластичность:", 340, 146, 0xFFFFFF, false);
        graphics.drawString(this.font, "Затухание:", 340, 168, 0xFFFFFF, false);
        graphics.drawString(this.font, "Пад. продажи:", 340, 190, 0xFFFFFF, false);
        graphics.drawString(this.font, "Рост скупки:", 340, 212, 0xFFFFFF, false);
    }

    private String formatEditableDouble(double value) {
        String text = String.format(Locale.ROOT, "%.6f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text.isEmpty() ? "0" : text;
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
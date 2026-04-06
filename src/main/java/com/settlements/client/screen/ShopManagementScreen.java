package com.settlements.client.screen;

import com.settlements.world.menu.ShopManagementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class ShopManagementScreen extends AbstractContainerScreen<ShopManagementMenu> {
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

    public ShopManagementScreen(ShopManagementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 444;
        this.imageHeight = 310;
        this.inventoryLabelY = 218;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

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
                .bounds(left + 324, top + 12, 110, 20)
                .build());

        infiniteBalanceButton = addRenderableWidget(Button.builder(Component.literal("∞ Баланс"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INFINITE_BALANCE))
                .bounds(left + 324, top + 36, 110, 20)
                .build());

        indestructibleButton = addRenderableWidget(Button.builder(Component.literal("Неразр."), b -> press(ShopManagementMenu.BUTTON_TOGGLE_INDESTRUCTIBLE))
                .bounds(left + 324, top + 60, 110, 20)
                .build());

        addSellTradeButton = addRenderableWidget(Button.builder(Component.literal("Продать"), b -> press(ShopManagementMenu.BUTTON_ADD_SELL_TRADE))
                .bounds(left + 184, top + 96, 76, 20)
                .build());

        addBuyTradeButton = addRenderableWidget(Button.builder(Component.literal("Скупать"), b -> press(ShopManagementMenu.BUTTON_ADD_BUY_TRADE))
                .bounds(left + 264, top + 96, 76, 20)
                .build());

        addDualTradeButton = addRenderableWidget(Button.builder(Component.literal("Обе"), b -> press(ShopManagementMenu.BUTTON_ADD_DUAL_TRADE))
                .bounds(left + 344, top + 96, 90, 20)
                .build());

        removeTradeButton = addRenderableWidget(Button.builder(Component.literal("Удалить"), b -> press(ShopManagementMenu.BUTTON_REMOVE_TRADE))
                .bounds(left + 184, top + 120, 116, 20)
                .build());

        toggleTradeEnabledButton = addRenderableWidget(Button.builder(Component.literal("Сделка"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_TRADE_ENABLED))
                .bounds(left + 306, top + 120, 128, 20)
                .build());

        toggleTradeModeButton = addRenderableWidget(Button.builder(Component.literal("Режим"), b -> press(ShopManagementMenu.BUTTON_TOGGLE_TRADE_MODE))
                .bounds(left + 306, top + 144, 128, 20)
                .build());

        sellPriceMinus1Button = addRenderableWidget(Button.builder(Component.literal("-1"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_MINUS_1))
                .bounds(left + 322, top + 144, 26, 20)
                .build());

        sellPricePlus1Button = addRenderableWidget(Button.builder(Component.literal("+1"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_PLUS_1))
                .bounds(left + 350, top + 144, 26, 20)
                .build());

        sellPriceMinus10Button = addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_MINUS_10))
                .bounds(left + 378, top + 144, 26, 20)
                .build());

        sellPricePlus10Button = addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(ShopManagementMenu.BUTTON_SELL_PRICE_PLUS_10))
                .bounds(left + 406, top + 144, 26, 20)
                .build());

        buyPriceMinus1Button = addRenderableWidget(Button.builder(Component.literal("-1"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_MINUS_1))
                .bounds(left + 322, top + 166, 26, 20)
                .build());

        buyPricePlus1Button = addRenderableWidget(Button.builder(Component.literal("+1"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_PLUS_1))
                .bounds(left + 350, top + 166, 26, 20)
                .build());

        buyPriceMinus10Button = addRenderableWidget(Button.builder(Component.literal("-10"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_MINUS_10))
                .bounds(left + 378, top + 166, 26, 20)
                .build());

        buyPricePlus10Button = addRenderableWidget(Button.builder(Component.literal("+10"), b -> press(ShopManagementMenu.BUTTON_BUY_PRICE_PLUS_10))
                .bounds(left + 406, top + 166, 26, 20)
                .build());

        sellBatchMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_SELL_BATCH_MINUS_1))
                .bounds(left + 378, top + 188, 26, 20)
                .build());

        sellBatchPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_SELL_BATCH_PLUS_1))
                .bounds(left + 406, top + 188, 26, 20)
                .build());

        buyBatchMinusButton = addRenderableWidget(Button.builder(Component.literal("-"), b -> press(ShopManagementMenu.BUTTON_BUY_BATCH_MINUS_1))
                .bounds(left + 378, top + 200, 26, 20)
                .build());

        buyBatchPlusButton = addRenderableWidget(Button.builder(Component.literal("+"), b -> press(ShopManagementMenu.BUTTON_BUY_BATCH_PLUS_1))
                .bounds(left + 406, top + 200, 26, 20)
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

        infiniteStockButton.visible = admin;
        infiniteBalanceButton.visible = admin;
        indestructibleButton.visible = admin;
        toggleTradeModeButton.visible = admin;

        infiniteStockButton.active = admin;
        infiniteBalanceButton.active = admin;
        indestructibleButton.active = admin;
        toggleTradeModeButton.active = admin && hasTrade;

        prevTradeButton.active = menu.getTradeCount() > 1;
        nextTradeButton.active = menu.getTradeCount() > 1;
        removeTradeButton.active = hasTrade;
        toggleTradeEnabledButton.active = hasTrade;

        sellPriceMinus1Button.active = canSell;
        sellPricePlus1Button.active = canSell;
        sellPriceMinus10Button.active = canSell;
        sellPricePlus10Button.active = canSell;

        buyPriceMinus1Button.active = canBuy;
        buyPricePlus1Button.active = canBuy;
        buyPriceMinus10Button.active = canBuy;
        buyPricePlus10Button.active = canBuy;

        sellBatchMinusButton.active = canSell;
        sellBatchPlusButton.active = canSell;
        buyBatchMinusButton.active = canBuy;
        buyBatchPlusButton.active = canBuy;
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

        graphics.drawString(this.font, trimToWidth(tradeName, 102), 212, 42, 0xFFFFFF, false);
        graphics.drawString(this.font, "Статус: " + selectedTradeStatus(), 212, 54, 0xD8D8D8, false);
        graphics.drawString(this.font, "Тип: " + selectedTradeType(), 212, 66, 0xD8D8D8, false);
        graphics.drawString(this.font, "ЛКМ по иконке", 212, 78, 0xC8C8C8, false);
        graphics.drawString(this.font, "сменить предмет", 212, 88, 0xC8C8C8, false);

        graphics.drawString(this.font, "Продажа: " + tradeValueText(this.menu.selectedTradeCanSell(), this.menu.getSelectedTradeSellPrice()), 184, 150, 0xFFFFFF, false);
        graphics.drawString(this.font, "Скупка: " + tradeValueText(this.menu.selectedTradeCanBuy(), this.menu.getSelectedTradeBuyPrice()), 184, 172, 0xFFFFFF, false);
        graphics.drawString(this.font, "Пачка прод.: " + batchText(this.menu.selectedTradeCanSell(), this.menu.getSelectedTradeSellBatch()), 184, 194, 0xFFFFFF, false);
        graphics.drawString(this.font, "Пачка скуп.: " + batchText(this.menu.selectedTradeCanBuy(), this.menu.getSelectedTradeBuyBatch()), 184, 206, 0xFFFFFF, false);

        if (this.menu.isAdminShop()) {
            graphics.drawString(this.font, "Тип магазина: админ", 324, 88, 0xFFD080, false);
        } else {
            graphics.drawString(this.font, "Тип магазина: игрок", 324, 88, 0xA8FFA8, false);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
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
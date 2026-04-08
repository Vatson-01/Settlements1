package com.settlements.client.screen;

import com.settlements.world.menu.SettlementMenu;
import com.settlements.world.menu.SettlementMenuTab;
import com.settlements.world.menu.SettlementReconstructionEntryView;
import com.settlements.world.menu.SettlementResidentView;
import com.settlements.world.menu.SettlementWarView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SettlementScreen extends AbstractContainerScreen<SettlementMenu> {
    private Button overviewTabButton;
    private Button residentsTabButton;
    private Button warTabButton;
    private Button reconstructionTabButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button openStorageButton;
    private Button restoreButton;

    public SettlementScreen(SettlementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 222;
        this.inventoryLabelY = 129;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        overviewTabButton = Button.builder(Component.literal("Обзор"), button -> pressButton(SettlementMenu.BUTTON_TAB_OVERVIEW))
                .bounds(left + 8, top + 6, 70, 20)
                .build();

        residentsTabButton = Button.builder(Component.literal("Жители"), button -> pressButton(SettlementMenu.BUTTON_TAB_RESIDENTS))
                .bounds(left + 82, top + 6, 70, 20)
                .build();

        warTabButton = Button.builder(Component.literal("Война"), button -> pressButton(SettlementMenu.BUTTON_TAB_WAR))
                .bounds(left + 156, top + 6, 70, 20)
                .build();

        reconstructionTabButton = Button.builder(Component.literal("Реконструкция"), button -> pressButton(SettlementMenu.BUTTON_TAB_RECONSTRUCTION))
                .bounds(left + 230, top + 6, 82, 20)
                .build();

        prevPageButton = Button.builder(Component.literal("<"), button -> pressButton(SettlementMenu.BUTTON_PAGE_PREV))
                .bounds(left + 248, top + 32, 24, 20)
                .build();

        nextPageButton = Button.builder(Component.literal(">"), button -> pressButton(SettlementMenu.BUTTON_PAGE_NEXT))
                .bounds(left + 280, top + 32, 24, 20)
                .build();

        openStorageButton = Button.builder(Component.literal("Открыть склад"), button -> pressButton(SettlementMenu.BUTTON_OPEN_RECONSTRUCTION_STORAGE))
                .bounds(left + 182, top + 86, 126, 20)
                .build();

        restoreButton = Button.builder(Component.literal("Восстановить доступное"), button -> pressButton(SettlementMenu.BUTTON_RESTORE_RECONSTRUCTION))
                .bounds(left + 182, top + 110, 126, 20)
                .build();

        addRenderableWidget(overviewTabButton);
        addRenderableWidget(residentsTabButton);
        addRenderableWidget(warTabButton);
        addRenderableWidget(reconstructionTabButton);
        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);
        addRenderableWidget(openStorageButton);
        addRenderableWidget(restoreButton);

        updateButtons();
    }

    private void pressButton(int buttonId) {
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
        SettlementMenuTab selectedTab = menu.getSelectedTab();

        overviewTabButton.active = selectedTab != SettlementMenuTab.OVERVIEW;
        residentsTabButton.active = selectedTab != SettlementMenuTab.RESIDENTS;
        warTabButton.active = selectedTab != SettlementMenuTab.WAR;
        reconstructionTabButton.active = selectedTab != SettlementMenuTab.RECONSTRUCTION;

        boolean pageable = selectedTab != SettlementMenuTab.OVERVIEW;
        int currentPage = getCurrentPage();
        int maxPage = getMaxPageForSelectedTab();

        prevPageButton.visible = pageable;
        nextPageButton.visible = pageable;
        prevPageButton.active = pageable && currentPage > 0;
        nextPageButton.active = pageable && currentPage < maxPage;

        boolean reconstructionTab = selectedTab == SettlementMenuTab.RECONSTRUCTION;
        openStorageButton.visible = reconstructionTab;
        restoreButton.visible = reconstructionTab;

        openStorageButton.active = reconstructionTab
                && menu.hasActiveReconstruction()
                && menu.canOpenReconstructionStorage();

        restoreButton.active = reconstructionTab
                && menu.hasActiveReconstruction()
                && menu.canRestoreReconstruction();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF242424);
        graphics.fill(left + 4, top + 30, left + imageWidth - 4, top + 126, 0xFF343434);
        graphics.fill(left + 4, top + 132, left + 176, top + imageHeight - 4, 0xFF303030);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 7 + column * 18;
                int slotY = top + 139 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 7 + column * 18;
            int slotY = top + 197;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, shorten(menu.getSettlementName(), 28), 8, 34, 0xFFFFFF, false);
        graphics.drawString(this.font, "Глава: " + shorten(menu.getLeaderName(), 22), 8, 46, 0xE0E0E0, false);

        SettlementMenuTab tab = menu.getSelectedTab();
        if (tab == SettlementMenuTab.OVERVIEW) {
            renderOverview(graphics);
        } else if (tab == SettlementMenuTab.RESIDENTS) {
            renderResidents(graphics);
        } else if (tab == SettlementMenuTab.WAR) {
            renderWar(graphics);
        } else if (tab == SettlementMenuTab.RECONSTRUCTION) {
            renderReconstruction(graphics);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private void renderOverview(GuiGraphics graphics) {
        int x = 8;
        int y = 62;

        graphics.drawString(this.font, "Жителей: " + menu.getMemberCount(), x, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, "Клеймов: " + menu.getClaimedChunkCount(), x, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, "Лимит покупки: " + menu.getPurchasedChunkAllowance(), x, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, "Баланс казны: " + menu.getTreasuryBalance(), x, y, 0xA8FFA8, false);
        y += 12;
        graphics.drawString(this.font, "Долг поселения: " + menu.getSettlementDebt(), x, y, 0xFFB0B0, false);
        y += 12;
        graphics.drawString(this.font, "Твой личный долг: " + menu.getPlayerDebt(), x, y, 0xFFD8A8, false);
        y += 12;
        graphics.drawString(this.font, "Активных войн: " + menu.getActiveWarCount(), x, y, 0xD0D0FF, false);
        y += 12;

        String siegeStatus = "Осады нет";
        if (menu.isUnderSiege()) {
            siegeStatus = "Поселение сейчас в осаде";
        } else if (menu.isAttackingSiege()) {
            siegeStatus = "Поселение сейчас ведет осаду";
        }
        graphics.drawString(this.font, shorten(siegeStatus, 34), x, y, 0xFFDDAA, false);
        y += 12;

        graphics.drawString(this.font, "Реконструкция: " + (menu.hasActiveReconstruction() ? "активна" : "нет"), x, y, 0xE0E0E0, false);
        y += 12;
        if (menu.hasActiveReconstruction()) {
            graphics.drawString(this.font, "Ожидает: " + menu.getReconstructionPending(), x, y, 0xFFFFFF, false);
        }
    }

    private void renderResidents(GuiGraphics graphics) {
        List<SettlementResidentView> residents = menu.getResidentViews();
        int page = menu.getResidentPage();
        int start = page * SettlementMenu.PAGE_SIZE;
        int end = Math.min(residents.size(), start + SettlementMenu.PAGE_SIZE);

        graphics.drawString(this.font, buildPageText(page, getMaxPage(residents.size())), 188, 38, 0xFFFFFF, false);

        if (residents.isEmpty()) {
            graphics.drawString(this.font, "Жители отсутствуют.", 8, 64, 0xDDDDDD, false);
            return;
        }

        int y = 62;
        for (int i = start; i < end; i++) {
            SettlementResidentView resident = residents.get(i);
            String line = (resident.isLeader() ? "[ГЛАВА] " : "")
                    + shorten(resident.getDisplayName(), 14)
                    + " | прав: " + resident.getPermissionCount()
                    + " | долг: " + resident.getPersonalDebt()
                    + " | магаз: " + resident.getShopTaxPercent() + "%";

            graphics.drawString(this.font, shorten(line, 46), 8, y, resident.isLeader() ? 0xFFFF88 : 0xFFFFFF, false);
            y += 11;
        }
    }

    private void renderWar(GuiGraphics graphics) {
        List<SettlementWarView> wars = menu.getWarViews();
        int page = menu.getWarPage();
        int start = page * SettlementMenu.PAGE_SIZE;
        int end = Math.min(wars.size(), start + SettlementMenu.PAGE_SIZE);

        graphics.drawString(this.font, buildPageText(page, getMaxPage(wars.size())), 188, 38, 0xFFFFFF, false);

        if (wars.isEmpty()) {
            graphics.drawString(this.font, "Активных войн нет.", 8, 64, 0xDDDDDD, false);
            return;
        }

        int y = 62;
        for (int i = start; i < end; i++) {
            SettlementWarView war = wars.get(i);
            graphics.drawString(this.font, shorten(war.getTitle(), 40), 8, y, 0xFFAAAA, false);
            y += 10;
            graphics.drawString(this.font, shorten(war.getDetail(), 44), 12, y, 0xE0E0E0, false);
            y += 14;
        }
    }

    private void renderReconstruction(GuiGraphics graphics) {
        graphics.drawString(this.font, "Всего: " + menu.getReconstructionTotal(), 8, 62, 0xFFFFFF, false);
        graphics.drawString(this.font, "Ожидает: " + menu.getReconstructionPending(), 8, 74, 0xFFFFFF, false);
        graphics.drawString(this.font, "Восстановлено: " + menu.getReconstructionRestored(), 8, 86, 0xA8FFA8, false);
        graphics.drawString(this.font, "Пропущено: " + menu.getReconstructionSkipped(), 8, 98, 0xFFD8A8, false);

        List<SettlementReconstructionEntryView> entries = menu.getReconstructionViews();
        int page = menu.getReconstructionPage();
        int start = page * SettlementMenu.PAGE_SIZE;
        int end = Math.min(entries.size(), start + SettlementMenu.PAGE_SIZE);

        graphics.drawString(this.font, buildPageText(page, getMaxPage(entries.size())), 188, 38, 0xFFFFFF, false);

        if (!menu.hasActiveReconstruction()) {
            graphics.drawString(this.font, "Активной реконструкции нет.", 8, 112, 0xDDDDDD, false);
            return;
        }

        if (entries.isEmpty()) {
            graphics.drawString(this.font, "Нет ожидающих записей.", 8, 112, 0xDDDDDD, false);
            return;
        }

        int y = 112;
        for (int i = start; i < end; i++) {
            SettlementReconstructionEntryView entry = entries.get(i);
            String line = "#" + entry.getIndex()
                    + " " + shorten(entry.getRequiredItemId(), 16)
                    + " x" + entry.getRequiredCount()
                    + " @" + entry.getPositionText();

            graphics.drawString(this.font, shorten(line, 46), 8, y, 0xFFFFFF, false);
            y += 10;
        }
    }

    private int getCurrentPage() {
        SettlementMenuTab tab = menu.getSelectedTab();
        if (tab == SettlementMenuTab.RESIDENTS) {
            return menu.getResidentPage();
        }
        if (tab == SettlementMenuTab.WAR) {
            return menu.getWarPage();
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            return menu.getReconstructionPage();
        }
        return 0;
    }

    private int getMaxPageForSelectedTab() {
        SettlementMenuTab tab = menu.getSelectedTab();
        if (tab == SettlementMenuTab.RESIDENTS) {
            return getMaxPage(menu.getResidentViews().size());
        }
        if (tab == SettlementMenuTab.WAR) {
            return getMaxPage(menu.getWarViews().size());
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            return getMaxPage(menu.getReconstructionViews().size());
        }
        return 0;
    }

    private int getMaxPage(int size) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / SettlementMenu.PAGE_SIZE;
    }

    private String buildPageText(int page, int maxPage) {
        return "Стр. " + (page + 1) + "/" + (maxPage + 1);
    }

    private String shorten(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
package com.settlements.client.screen;

import com.settlements.world.menu.SettlementMenu;
import com.settlements.world.menu.SettlementMenuTab;
import com.settlements.world.menu.SettlementReconstructionEntryView;
import com.settlements.world.menu.SettlementWarView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettlementScreen extends AbstractContainerScreen<SettlementMenu> {
    private static final int LIST_ROWS = 7;

    private Button overviewTabButton;
    private Button warTabButton;
    private Button reconstructionTabButton;
    private Button prevPageButton;
    private Button nextPageButton;

    private final Button[] listButtons = new Button[LIST_ROWS];

    private Button reconstructionResourcesModeButton;
    private Button reconstructionBlocksModeButton;
    private Button openStorageButton;
    private Button restoreButton;
    private Button stopReconstructionButton;

    private int warPage;
    private int reconstructionPage;
    private ReconstructionPanelMode reconstructionPanelMode = ReconstructionPanelMode.RESOURCES;
    private String selectedResourceKey;

    public SettlementScreen(SettlementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 360;
        this.imageHeight = 262;
        this.inventoryLabelY = 170;
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        this.warPage = menu.getWarPage();
        this.reconstructionPage = menu.getReconstructionPage();

        ensureSelections();
        updateButtons();
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        overviewTabButton = Button.builder(Component.literal("Обзор"), button -> pressButton(SettlementMenu.BUTTON_TAB_OVERVIEW))
                .bounds(left + 8, top + 6, 110, 20)
                .build();

        warTabButton = Button.builder(Component.literal("Война"), button -> pressButton(SettlementMenu.BUTTON_TAB_WAR))
                .bounds(left + 122, top + 6, 110, 20)
                .build();

        reconstructionTabButton = Button.builder(Component.literal("Реконструкция"), button -> pressButton(SettlementMenu.BUTTON_TAB_RECONSTRUCTION))
                .bounds(left + 236, top + 6, 116, 20)
                .build();

        prevPageButton = Button.builder(Component.literal("<"), button -> pressButton(SettlementMenu.BUTTON_PAGE_PREV))
                .bounds(left + 278, top + 34, 24, 18)
                .build();

        nextPageButton = Button.builder(Component.literal(">"), button -> pressButton(SettlementMenu.BUTTON_PAGE_NEXT))
                .bounds(left + 306, top + 34, 24, 18)
                .build();

        addRenderableWidget(overviewTabButton);
        addRenderableWidget(warTabButton);
        addRenderableWidget(reconstructionTabButton);
        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);

        int listX = left + 10;
        int listY = top + 58;
        for (int i = 0; i < LIST_ROWS; i++) {
            final int row = i;
            listButtons[i] = Button.builder(Component.empty(), button -> handleListRowClick(row))
                    .bounds(listX, listY + i * 14, 118, 12)
                    .build();
            addRenderableWidget(listButtons[i]);
        }

        reconstructionResourcesModeButton = Button.builder(Component.literal("Нужные блоки"), button -> {
                    reconstructionPanelMode = ReconstructionPanelMode.RESOURCES;
                    reconstructionPage = 0;
                })
                .bounds(left + 140, top + 56, 96, 18)
                .build();

        reconstructionBlocksModeButton = Button.builder(Component.literal("Координаты"), button -> {
                    reconstructionPanelMode = ReconstructionPanelMode.BLOCKS;
                    reconstructionPage = 0;
                })
                .bounds(left + 240, top + 56, 86, 18)
                .build();

        openStorageButton = Button.builder(Component.literal("Открыть склад"), button -> pressButton(SettlementMenu.BUTTON_OPEN_RECONSTRUCTION_STORAGE))
                .bounds(left + 140, top + 82, 124, 18)
                .build();

        restoreButton = Button.builder(Component.literal("Восстановить"), button -> pressButton(SettlementMenu.BUTTON_RESTORE_RECONSTRUCTION))
                .bounds(left + 268, top + 82, 84, 18)
                .build();

        stopReconstructionButton = Button.builder(Component.literal("Остановить"), button -> {
                    pressButton(SettlementMenu.BUTTON_STOP_RECONSTRUCTION);
                    reconstructionPanelMode = ReconstructionPanelMode.RESOURCES;
                    reconstructionPage = 0;
                })
                .bounds(left + 140, top + 104, 212, 18)
                .build();

        addRenderableWidget(reconstructionResourcesModeButton);
        addRenderableWidget(reconstructionBlocksModeButton);
        addRenderableWidget(openStorageButton);
        addRenderableWidget(restoreButton);
        addRenderableWidget(stopReconstructionButton);

        updateButtons();
    }

    private void pressButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private int clampPage(int value, int maxPage) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, maxPage);
    }

    private void handleListRowClick(int row) {
        SettlementMenuTab tab = menu.getSelectedTab();

        if (tab != SettlementMenuTab.RECONSTRUCTION) {
            return;
        }

        if (reconstructionPanelMode == ReconstructionPanelMode.RESOURCES) {
            List<ReconstructionResourceSummary> resources = getResourceSummaries();
            int index = reconstructionPage * LIST_ROWS + row;
            if (index >= 0 && index < resources.size()) {
                selectedResourceKey = resources.get(index).itemId;
                reconstructionPanelMode = ReconstructionPanelMode.BLOCKS;
                reconstructionPage = 0;
            }
            return;
        }

        if (!menu.canRestoreReconstruction()) {
            return;
        }

        List<SettlementReconstructionEntryView> blocks = getVisibleReconstructionBlocks();
        int index = reconstructionPage * LIST_ROWS + row;
        if (index >= 0 && index < blocks.size()) {
            SettlementReconstructionEntryView entry = blocks.get(index);
            if (!entry.isRestored()) {
                boolean newSkipped = !entry.isSkipped();
                pressButton(SettlementMenu.BUTTON_SKIP_RECON_ENTRY_BASE + entry.getIndex());
                menu.clientSetReconstructionEntrySkipped(entry.getIndex(), newSkipped);
            }
        }
    }

    private void ensureSelections() {
        List<ReconstructionResourceSummary> resources = getResourceSummaries();
        if (!resources.isEmpty()) {
            boolean found = false;
            for (ReconstructionResourceSummary summary : resources) {
                if (summary.itemId.equals(selectedResourceKey)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                selectedResourceKey = resources.get(0).itemId;
            }
        } else {
            selectedResourceKey = null;
            reconstructionPanelMode = ReconstructionPanelMode.RESOURCES;
        }

        warPage = clampPage(warPage, getWarMaxPage());

        int reconstructionMaxPage = reconstructionPanelMode == ReconstructionPanelMode.BLOCKS
                ? getReconstructionBlockMaxPage()
                : getReconstructionResourceMaxPage();
        reconstructionPage = clampPage(reconstructionPage, reconstructionMaxPage);

        if (reconstructionPanelMode == ReconstructionPanelMode.BLOCKS && selectedResourceKey == null) {
            reconstructionPanelMode = ReconstructionPanelMode.RESOURCES;
            reconstructionPage = 0;
        }
    }

    private void updateButtons() {
        SettlementMenuTab selectedTab = menu.getSelectedTab();

        overviewTabButton.active = selectedTab != SettlementMenuTab.OVERVIEW;
        warTabButton.active = selectedTab != SettlementMenuTab.WAR;
        reconstructionTabButton.active = selectedTab != SettlementMenuTab.RECONSTRUCTION;

        int currentPage = 0;
        int maxPage = 0;
        if (selectedTab == SettlementMenuTab.WAR) {
            currentPage = warPage;
            maxPage = getWarMaxPage();
        } else if (selectedTab == SettlementMenuTab.RECONSTRUCTION) {
            currentPage = reconstructionPage;
            maxPage = reconstructionPanelMode == ReconstructionPanelMode.BLOCKS
                    ? getReconstructionBlockMaxPage()
                    : getReconstructionResourceMaxPage();
        }

        prevPageButton.visible = selectedTab != SettlementMenuTab.OVERVIEW;
        nextPageButton.visible = selectedTab != SettlementMenuTab.OVERVIEW;
        prevPageButton.active = selectedTab != SettlementMenuTab.OVERVIEW && currentPage > 0;
        nextPageButton.active = selectedTab != SettlementMenuTab.OVERVIEW && currentPage < maxPage;

        for (int i = 0; i < LIST_ROWS; i++) {
            listButtons[i].visible = false;
            listButtons[i].active = false;
        }

        if (selectedTab == SettlementMenuTab.RECONSTRUCTION) {
            if (reconstructionPanelMode == ReconstructionPanelMode.RESOURCES) {
                List<ReconstructionResourceSummary> resources = getResourceSummaries();
                for (int row = 0; row < LIST_ROWS; row++) {
                    int index = reconstructionPage * LIST_ROWS + row;
                    if (index >= resources.size()) {
                        continue;
                    }
                    ReconstructionResourceSummary summary = resources.get(index);
                    String label = summary.displayName + " x" + summary.pendingCount + "/" + summary.totalCount;
                    listButtons[row].setMessage(Component.literal(shorten(label, 20)));
                    listButtons[row].visible = true;
                    listButtons[row].active = true;
                }
            } else {
                List<SettlementReconstructionEntryView> entries = getVisibleReconstructionBlocks();
                for (int row = 0; row < LIST_ROWS; row++) {
                    int index = reconstructionPage * LIST_ROWS + row;
                    if (index >= entries.size()) {
                        continue;
                    }
                    SettlementReconstructionEntryView entry = entries.get(index);
                    String prefix = entry.isSkipped() ? "[П] " : entry.isRestored() ? "[V] " : "[ ] ";
                    listButtons[row].setMessage(Component.literal(shorten(prefix + entry.getPositionText(), 20)));
                    listButtons[row].visible = true;
                    listButtons[row].active = !entry.isRestored() && menu.canRestoreReconstruction();
                }
            }
        }

        boolean reconstructionTab = selectedTab == SettlementMenuTab.RECONSTRUCTION;
        reconstructionResourcesModeButton.visible = reconstructionTab;
        reconstructionBlocksModeButton.visible = reconstructionTab;
        openStorageButton.visible = reconstructionTab;
        restoreButton.visible = reconstructionTab;
        stopReconstructionButton.visible = reconstructionTab;

        reconstructionResourcesModeButton.active = reconstructionTab && reconstructionPanelMode != ReconstructionPanelMode.RESOURCES;
        reconstructionBlocksModeButton.active = reconstructionTab
                && reconstructionPanelMode != ReconstructionPanelMode.BLOCKS
                && selectedResourceKey != null;

        openStorageButton.active = reconstructionTab && menu.canOpenReconstructionStorage();
        restoreButton.active = reconstructionTab && menu.canRestoreReconstruction();
        stopReconstructionButton.active = reconstructionTab && menu.canStopReconstruction();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1F1F1F);
        graphics.fill(left + 6, top + 32, left + imageWidth - 6, top + 170, 0xFF2F2F2F);
        graphics.fill(left + 8, top + 54, left + 130, top + 162, 0xFF252525);
        graphics.fill(left + 136, top + 54, left + imageWidth - 8, top + 162, 0xFF252525);
        graphics.fill(left + 6, top + 174, left + imageWidth - 6, top + imageHeight - 6, 0xFF2A2A2A);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 8 + column * 18;
                int slotY = top + 180 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 8 + column * 18;
            int slotY = top + 238;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, shorten(menu.getSettlementName(), 32), 10, 36, 0xFFFFFF, false);
        graphics.drawString(this.font, "Глава: " + shorten(menu.getLeaderName(), 24), 10, 46, 0xE0E0E0, false);

        SettlementMenuTab tab = menu.getSelectedTab();
        if (tab == SettlementMenuTab.OVERVIEW) {
            renderOverview(graphics);
        } else if (tab == SettlementMenuTab.WAR) {
            renderWar(graphics);
        } else if (tab == SettlementMenuTab.RECONSTRUCTION) {
            renderReconstruction(graphics);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private void renderOverview(GuiGraphics graphics) {
        int x = 10;
        int y = 60;

        graphics.drawString(this.font, "Жителей: " + menu.getMemberCount(), x, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, "Клеймов: " + menu.getClaimedChunkCount(), x, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(this.font, "Лимит покупки: " + menu.getPurchasedChunkAllowance(), x, y, 0xFFFFFF, false);
        y += 12;

        graphics.drawString(
                this.font,
                menu.canViewTreasuryBalance() ? "Баланс казны: " + menu.getTreasuryBalance() : "Баланс казны: скрыт",
                x,
                y,
                0xA8FFA8,
                false
        );
        y += 12;

        graphics.drawString(
                this.font,
                menu.canViewSettlementDebt() ? "Долг поселения: " + menu.getSettlementDebt() : "Долг поселения: скрыт",
                x,
                y,
                0xFFB0B0,
                false
        );
        y += 12;

        graphics.drawString(this.font, "Твой личный долг: " + menu.getPlayerDebt(), x, y, 0xFFD8A8, false);
        y += 12;
        graphics.drawString(this.font, "Активных войн: " + menu.getActiveWarCount(), x, y, 0xD0D0FF, false);
        y += 12;

        String siegeStatus = menu.isUnderSiege()
                ? "Поселение в осаде"
                : menu.isAttackingSiege() ? "Поселение осаждает врага" : "Осад нет";
        graphics.drawString(this.font, shorten(siegeStatus, 34), x, y, 0xFFDDAA, false);
        y += 12;

        graphics.drawString(this.font, "Реконструкция: " + (menu.hasActiveReconstruction() ? "активна" : "нет"), x, y, 0xE0E0E0, false);
    }

    private void renderWar(GuiGraphics graphics) {
        graphics.drawString(this.font, "Активные войны", 12, 58, 0xFFFFFF, false);
        graphics.drawString(this.font, buildPageText(getCurrentPage(), getCurrentMaxPage()), 278, 58, 0xFFFFFF, false);

        List<SettlementWarView> wars = menu.getWarViews();
        if (wars.isEmpty()) {
            graphics.drawString(this.font, "Активных войн нет.", 12, 74, 0xDDDDDD, false);
            return;
        }

        int start = warPage * 5;
        int end = Math.min(wars.size(), start + 5);
        int y = 74;
        for (int i = start; i < end; i++) {
            SettlementWarView war = wars.get(i);
            graphics.drawString(this.font, shorten(war.getTitle(), 44), 12, y, 0xFFAAAA, false);
            y += 10;
            graphics.drawString(this.font, shorten(war.getDetail(), 48), 18, y, 0xE0E0E0, false);
            y += 16;
        }
    }

    private void renderReconstruction(GuiGraphics graphics) {
        graphics.drawString(this.font, reconstructionPanelMode == ReconstructionPanelMode.RESOURCES ? "Ресурсы" : "Блоки", 12, 58, 0xFFFFFF, false);
        graphics.drawString(this.font, buildPageText(getCurrentPage(), getCurrentMaxPage()), 292, 58, 0xFFFFFF, false);

        if (!menu.hasActiveReconstruction()) {
            graphics.drawString(this.font, "Активной реконструкции нет.", 140, 128, 0xDDDDDD, false);
            return;
        }

        graphics.drawString(this.font, "Всего: " + menu.getReconstructionTotal(), 140, 128, 0xFFFFFF, false);
        graphics.drawString(this.font, "Ожидает: " + menu.getReconstructionPending(), 140, 140, 0xFFFFFF, false);
        graphics.drawString(this.font, "Восстановлено: " + menu.getReconstructionRestored(), 240, 128, 0xA8FFA8, false);
        graphics.drawString(this.font, "Пропущено: " + menu.getReconstructionSkipped(), 240, 140, 0xFFD8A8, false);

        if (reconstructionPanelMode == ReconstructionPanelMode.RESOURCES) {
            ReconstructionResourceSummary selectedSummary = getSelectedResourceSummary();
            if (selectedSummary == null) {
                graphics.drawString(this.font, "Выбери ресурс слева.", 140, 154, 0xDDDDDD, false);
                return;
            }
            graphics.drawString(this.font, shorten(selectedSummary.displayName, 24), 140, 154, 0xFFFFFF, false);
            graphics.drawString(this.font, "Всего нужно: " + selectedSummary.totalCount, 140, 166, 0xFFFFFF, false);
        } else {
            List<SettlementReconstructionEntryView> blocks = getVisibleReconstructionBlocks();
            graphics.drawString(this.font, selectedResourceKey == null ? "Ресурс не выбран" : shorten(selectedResourceKey, 24), 140, 154, 0xFFFFFF, false);
            if (blocks.isEmpty()) {
                graphics.drawString(this.font, "Нет блоков для выбранного ресурса.", 140, 166, 0xDDDDDD, false);
                return;
            }

            int previewStart = reconstructionPage * LIST_ROWS;
            if (previewStart < blocks.size()) {
                SettlementReconstructionEntryView entry = blocks.get(previewStart);
                String status = entry.isRestored() ? "восстановлен" : entry.isSkipped() ? "пропущен" : "ожидает";
                int color = entry.isRestored() ? 0xA8FFA8 : entry.isSkipped() ? 0xFFD8A8 : 0xFFFFFF;
                graphics.drawString(this.font, "Статус: " + status, 140, 166, color, false);
                graphics.drawString(this.font, shorten(entry.getDimensionText(), 24), 140, 178, 0xC8C8C8, false);
            }
        }
    }

    private int getCurrentPage() {
        SettlementMenuTab tab = menu.getSelectedTab();
        if (tab == SettlementMenuTab.WAR) {
            return warPage;
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            return reconstructionPage;
        }
        return 0;
    }

    private int getCurrentMaxPage() {
        SettlementMenuTab tab = menu.getSelectedTab();
        if (tab == SettlementMenuTab.WAR) {
            return getWarMaxPage();
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            return reconstructionPanelMode == ReconstructionPanelMode.BLOCKS
                    ? getReconstructionBlockMaxPage()
                    : getReconstructionResourceMaxPage();
        }
        return 0;
    }

    private int getWarMaxPage() {
        return getMaxPage(menu.getWarViews().size(), 5);
    }

    private int getReconstructionResourceMaxPage() {
        return getMaxPage(getResourceSummaries().size(), LIST_ROWS);
    }

    private int getReconstructionBlockMaxPage() {
        return getMaxPage(getVisibleReconstructionBlocks().size(), LIST_ROWS);
    }

    private int getMaxPage(int size, int pageSize) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / pageSize;
    }

    private List<ReconstructionResourceSummary> getResourceSummaries() {
        Map<String, ReconstructionResourceSummary> grouped = new LinkedHashMap<String, ReconstructionResourceSummary>();
        for (SettlementReconstructionEntryView entry : menu.getReconstructionViews()) {
            ReconstructionResourceSummary summary = grouped.get(entry.getRequiredItemId());
            if (summary == null) {
                summary = new ReconstructionResourceSummary(entry.getRequiredItemId());
                grouped.put(entry.getRequiredItemId(), summary);
            }
            summary.totalCount += entry.getRequiredCount();
            if (entry.isRestored()) {
                summary.restoredCount += entry.getRequiredCount();
            } else if (entry.isSkipped()) {
                summary.skippedCount += entry.getRequiredCount();
            } else {
                summary.pendingCount += entry.getRequiredCount();
            }
        }
        return new ArrayList<ReconstructionResourceSummary>(grouped.values());
    }

    private ReconstructionResourceSummary getSelectedResourceSummary() {
        if (selectedResourceKey == null) {
            return null;
        }
        for (ReconstructionResourceSummary summary : getResourceSummaries()) {
            if (selectedResourceKey.equals(summary.itemId)) {
                return summary;
            }
        }
        return null;
    }

    private List<SettlementReconstructionEntryView> getVisibleReconstructionBlocks() {
        List<SettlementReconstructionEntryView> result = new ArrayList<SettlementReconstructionEntryView>();
        if (selectedResourceKey == null) {
            return result;
        }
        for (SettlementReconstructionEntryView entry : menu.getReconstructionViews()) {
            if (selectedResourceKey.equals(entry.getRequiredItemId())) {
                result.add(entry);
            }
        }
        return result;
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

    private enum ReconstructionPanelMode {
        RESOURCES,
        BLOCKS
    }

    private static final class ReconstructionResourceSummary {
        private final String itemId;
        private final String displayName;
        private int totalCount;
        private int pendingCount;
        private int restoredCount;
        private int skippedCount;

        private ReconstructionResourceSummary(String itemId) {
            this.itemId = itemId == null ? "" : itemId;
            if (this.itemId.isEmpty()) {
                this.displayName = "Без ресурса";
            } else {
                String path = this.itemId;
                int separator = path.indexOf(':');
                if (separator >= 0 && separator + 1 < path.length()) {
                    path = path.substring(separator + 1);
                }
                this.displayName = path.replace('_', ' ');
            }
        }
    }
}
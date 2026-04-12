package com.settlements.client.screen;

import com.settlements.data.model.PlotPermission;
import com.settlements.world.menu.PlotMenu;
import com.settlements.world.menu.PlotPlayerView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class PlotScreen extends AbstractContainerScreen<PlotMenu> {
    private static final int LIST_ROWS = PlotMenu.PAGE_SIZE;

    private Button prevPageButton;
    private Button nextPageButton;
    private Button backButton;
    private final Button[] playerButtons = new Button[LIST_ROWS];

    private Button assignButton;
    private Button unassignButton;

    private Button buildButton;
    private Button breakButton;
    private Button openDoorsButton;
    private Button redstoneButton;
    private Button containersButton;

    public PlotScreen(PlotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 360;
        this.imageHeight = 290;
        this.inventoryLabelY = 186;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        prevPageButton = Button.builder(Component.literal("<"), button -> pressButton(PlotMenu.BUTTON_PAGE_PREV))
                .bounds(left + 104, top + 34, 24, 18)
                .build();

        nextPageButton = Button.builder(Component.literal(">"), button -> pressButton(PlotMenu.BUTTON_PAGE_NEXT))
                .bounds(left + 132, top + 34, 24, 18)
                .build();

        backButton = Button.builder(Component.literal("Назад"), button -> pressButton(PlotMenu.BUTTON_BACK_TO_SETTLEMENT))
                .bounds(left + 282, top + 34, 60, 18)
                .build();

        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);
        addRenderableWidget(backButton);

        int listX = left + 10;
        int listY = top + 58;
        for (int i = 0; i < LIST_ROWS; i++) {
            final int row = i;
            playerButtons[i] = Button.builder(Component.empty(), button -> pressButton(PlotMenu.BUTTON_SELECT_PLAYER_BASE + row))
                    .bounds(listX, listY + i * 14, 146, 12)
                    .build();
            addRenderableWidget(playerButtons[i]);
        }

        assignButton = Button.builder(Component.literal("Назначить"), button -> pressButton(PlotMenu.BUTTON_ASSIGN_SELECTED))
                .bounds(left + 176, top + 124, 80, 18)
                .build();

        unassignButton = Button.builder(Component.literal("Сделать общим"), button -> pressButton(PlotMenu.BUTTON_UNASSIGN))
                .bounds(left + 262, top + 124, 80, 18)
                .build();

        buildButton = Button.builder(Component.empty(), button -> pressButton(PlotMenu.BUTTON_TOGGLE_BUILD))
                .bounds(left + 176, top + 146, 80, 16)
                .build();

        breakButton = Button.builder(Component.empty(), button -> pressButton(PlotMenu.BUTTON_TOGGLE_BREAK))
                .bounds(left + 262, top + 146, 80, 16)
                .build();

        openDoorsButton = Button.builder(Component.empty(), button -> pressButton(PlotMenu.BUTTON_TOGGLE_OPEN_DOORS))
                .bounds(left + 176, top + 166, 80, 16)
                .build();

        redstoneButton = Button.builder(Component.empty(), button -> pressButton(PlotMenu.BUTTON_TOGGLE_USE_REDSTONE))
                .bounds(left + 262, top + 166, 80, 16)
                .build();

        containersButton = Button.builder(Component.empty(), button -> pressButton(PlotMenu.BUTTON_TOGGLE_OPEN_CONTAINERS))
                .bounds(left + 176, top + 186, 166, 16)
                .build();

        addRenderableWidget(assignButton);
        addRenderableWidget(unassignButton);
        addRenderableWidget(buildButton);
        addRenderableWidget(breakButton);
        addRenderableWidget(openDoorsButton);
        addRenderableWidget(redstoneButton);
        addRenderableWidget(containersButton);

        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void pressButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void updateButtons() {
        List<PlotPlayerView> players = menu.getPlayerViews();
        int page = menu.getPage();
        int selectedIndex = menu.getSelectedIndex();

        prevPageButton.active = page > 0;
        nextPageButton.active = page < getMaxPage(players.size());

        for (int row = 0; row < LIST_ROWS; row++) {
            int index = page * LIST_ROWS + row;
            if (index < players.size()) {
                PlotPlayerView view = players.get(index);
                String prefix = index == selectedIndex ? "> " : "";
                String owner = view.isOwner() ? " [вл]" : "";
                playerButtons[row].visible = true;
                playerButtons[row].active = index != selectedIndex;
                playerButtons[row].setMessage(Component.literal(shorten(prefix + view.getPlayerName() + owner, 20)));
            } else {
                playerButtons[row].visible = false;
                playerButtons[row].active = false;
                playerButtons[row].setMessage(Component.empty());
            }
        }

        PlotPlayerView selected = menu.getSelectedPlayerView();
        boolean hasSelected = selected != null;
        boolean selectedOwner = hasSelected && selected.isOwner();

        boolean canAssignSelected = menu.canAssign() && hasSelected && !selectedOwner;
        boolean editable = menu.canEditPermissions() && menu.hasPlotOnChunk() && hasSelected && !selectedOwner;

        assignButton.active = canAssignSelected;
        unassignButton.active = menu.canUnassign() && menu.hasPlotOnChunk();

        buildButton.active = editable;
        breakButton.active = editable;
        openDoorsButton.active = editable;
        redstoneButton.active = editable;
        containersButton.active = editable;

        assignButton.setMessage(Component.literal(menu.hasPlotOnChunk() ? "Передать" : "Назначить"));
        unassignButton.setMessage(Component.literal("Сделать общим"));
        buildButton.setMessage(Component.literal(shorten(permissionLabel("Строить", selected, PlotPermission.BUILD), 14)));
        breakButton.setMessage(Component.literal(shorten(permissionLabel("Ломать", selected, PlotPermission.BREAK), 14)));
        openDoorsButton.setMessage(Component.literal(shorten(permissionLabel("Двери", selected, PlotPermission.OPEN_DOORS), 14)));
        redstoneButton.setMessage(Component.literal(shorten(permissionLabel("Редстоун", selected, PlotPermission.USE_REDSTONE), 14)));
        containersButton.setMessage(Component.literal(shorten(permissionLabel("Контейнеры", selected, PlotPermission.OPEN_CONTAINERS), 30)));
    }

    private String permissionLabel(String label, PlotPlayerView selected, PlotPermission permission) {
        if (selected == null) {
            return label + ": —";
        }
        if (selected.isOwner()) {
            return label + ": владелец";
        }
        return label + ": " + (selected.hasPermission(permission) ? "да" : "нет");
    }

    private int getMaxPage(int size) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / LIST_ROWS;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1F1F1F);
        graphics.fill(left + 6, top + 32, left + imageWidth - 6, top + 194, 0xFF2F2F2F);
        graphics.fill(left + 8, top + 54, left + 160, top + 190, 0xFF252525);
        graphics.fill(left + 170, top + 54, left + imageWidth - 8, top + 190, 0xFF252525);
        graphics.fill(left + 6, top + 200, left + imageWidth - 6, top + imageHeight - 6, 0xFF2A2A2A);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 8 + column * 18;
                int slotY = top + 198 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 8 + column * 18;
            int slotY = top + 256;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, shorten(menu.getSettlementName(), 28), 10, 36, 0xFFFFFF, false);
        graphics.drawString(this.font, "Чанк: " + menu.getChunkX() + ", " + menu.getChunkZ(), 10, 46, 0xE0E0E0, false);

        renderInfo(graphics);

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private void renderInfo(GuiGraphics graphics) {
        int x = 176;
        int y = 60;

        graphics.drawString(
                this.font,
                menu.hasClaimOnChunk() ? "Статус: заклеймлен" : "Статус: не заклеймлен",
                x,
                y,
                menu.hasClaimOnChunk() ? 0xA8FFA8 : 0xFFB0B0,
                false
        );
        y += 12;

        graphics.drawString(
                this.font,
                menu.hasPlotOnChunk() ? "Тип: личный участок" : "Тип: общая территория",
                x,
                y,
                0xFFFFFF,
                false
        );
        y += 12;

        graphics.drawString(this.font, "Владелец: " + shorten(menu.getOwnerName(), 18), x, y, 0xE0E0E0, false);
        y += 12;
        graphics.drawString(this.font, "Чанков в участке: " + menu.getPlotChunkCount(), x, y, 0xE0E0E0, false);
        y += 12;

        PlotPlayerView selected = menu.getSelectedPlayerView();
        if (selected != null) {
            graphics.drawString(this.font, "Выбран: " + shorten(selected.getPlayerName(), 18), x, y, 0xFFDDAA, false);
        } else {
            graphics.drawString(this.font, "Игрок не выбран", x, y, 0xFFDDAA, false);
        }
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}

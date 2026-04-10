package com.settlements.client.screen;

import com.settlements.world.menu.SettlementResidentListView;
import com.settlements.world.menu.SettlementResidentsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class SettlementResidentsScreen extends AbstractContainerScreen<SettlementResidentsMenu> {
    private final Button[] residentButtons = new Button[SettlementResidentsMenu.PAGE_SIZE];
    private Button prevPageButton;
    private Button nextPageButton;
    private int page;

    public SettlementResidentsScreen(SettlementResidentsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 336;
        this.imageHeight = 236;
        this.inventoryLabelY = 142;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        prevPageButton = Button.builder(Component.literal("<"), button -> pressButton(SettlementResidentsMenu.BUTTON_PAGE_PREV))
                .bounds(left + 258, top + 18, 24, 18)
                .build();

        nextPageButton = Button.builder(Component.literal(">"), button -> pressButton(SettlementResidentsMenu.BUTTON_PAGE_NEXT))
                .bounds(left + 286, top + 18, 24, 18)
                .build();

        addRenderableWidget(prevPageButton);
        addRenderableWidget(nextPageButton);

        int listX = left + 10;
        int listY = top + 42;
        for (int i = 0; i < residentButtons.length; i++) {
            final int row = i;
            residentButtons[i] = Button.builder(Component.empty(), button -> pressButton(SettlementResidentsMenu.BUTTON_OPEN_RESIDENT_BASE + row))
                    .bounds(listX, listY + i * 14, 300, 12)
                    .build();
            addRenderableWidget(residentButtons[i]);
        }

        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.page = menu.getPage();
        updateButtons();
    }

    private void pressButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void updateButtons() {
        int maxPage = getMaxPage(menu.getResidentViews().size(), SettlementResidentsMenu.PAGE_SIZE);

        prevPageButton.active = page > 0;
        nextPageButton.active = page < maxPage;

        for (int i = 0; i < residentButtons.length; i++) {
            residentButtons[i].visible = false;
            residentButtons[i].active = false;
        }

        List<SettlementResidentListView> residents = menu.getResidentViews();
        for (int row = 0; row < residentButtons.length; row++) {
            int index = page * SettlementResidentsMenu.PAGE_SIZE + row;
            if (index >= residents.size()) {
                continue;
            }

            SettlementResidentListView view = residents.get(index);
            String label = view.isLeader() ? "[ГЛАВА] " + view.getDisplayName() : view.getDisplayName();

            residentButtons[row].setMessage(Component.literal(shorten(label, 40)));
            residentButtons[row].visible = true;
            residentButtons[row].active = true;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1F1F1F);
        graphics.fill(left + 6, top + 16, left + imageWidth - 6, top + 136, 0xFF2F2F2F);
        graphics.fill(left + 8, top + 18, left + imageWidth - 8, top + 130, 0xFF252525);
        graphics.fill(left + 6, top + 146, left + imageWidth - 6, top + imageHeight - 6, 0xFF2A2A2A);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + 8 + column * 18;
                int slotY = top + 152 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
            }
        }

        for (int column = 0; column < 9; column++) {
            int slotX = left + 8 + column * 18;
            int slotY = top + 210;
            graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF555555);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, shorten("Жители: " + menu.getSettlementName(), 34), 8, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, buildPageText(page, getMaxPage(menu.getResidentViews().size(), SettlementResidentsMenu.PAGE_SIZE)), 208, 22, 0xFFFFFF, false);

        if (menu.getResidentViews().isEmpty()) {
            graphics.drawString(this.font, "Жителей нет.", 10, 22, 0xDDDDDD, false);
        } else {
            graphics.drawString(this.font, "Выбери жителя для управления.", 10, 22, 0xDDDDDD, false);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private int getMaxPage(int size, int pageSize) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / pageSize;
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
package com.settlements.client.screen;

import com.settlements.data.model.SettlementPermission;
import com.settlements.world.menu.SettlementResidentManageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class SettlementResidentManageScreen extends AbstractContainerScreen<SettlementResidentManageMenu> {
    private final Button[] permissionButtons = new Button[SettlementResidentManageMenu.PERMISSION_ROWS];
    private final int[] visiblePermissionOrdinals = new int[SettlementResidentManageMenu.PERMISSION_ROWS];

    private Button permissionPrevButton;
    private Button permissionNextButton;

    private Button personalTaxMinus100Button;
    private Button personalTaxMinus10Button;
    private Button personalTaxPlus10Button;
    private Button personalTaxPlus100Button;

    private Button shopTaxMinus10Button;
    private Button shopTaxMinus1Button;
    private Button shopTaxPlus1Button;
    private Button shopTaxPlus10Button;

    public SettlementResidentManageScreen(SettlementResidentManageMenu menu, Inventory inventory, Component title) {
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

        permissionPrevButton = Button.builder(Component.literal("<"), button -> pressButton(SettlementResidentManageMenu.BUTTON_PERMISSION_PAGE_PREV))
                .bounds(left + 258, top + 18, 24, 18)
                .build();

        permissionNextButton = Button.builder(Component.literal(">"), button -> pressButton(SettlementResidentManageMenu.BUTTON_PERMISSION_PAGE_NEXT))
                .bounds(left + 286, top + 18, 24, 18)
                .build();

        addRenderableWidget(permissionPrevButton);
        addRenderableWidget(permissionNextButton);

        int buttonX = left + 274;
        int buttonY = top + 52;
        for (int i = 0; i < permissionButtons.length; i++) {
            final int row = i;
            visiblePermissionOrdinals[i] = -1;
            permissionButtons[i] = Button.builder(Component.literal("Выкл"), button -> toggleVisiblePermission(row))
                    .bounds(buttonX, buttonY + i * 14, 44, 12)
                    .build();
            addRenderableWidget(permissionButtons[i]);
        }

        personalTaxMinus100Button = smallButton(left + 10, top + 78, "-100", button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_MINUS_100));
        personalTaxMinus10Button = smallButton(left + 59, top + 78, "-10", button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_MINUS_10));
        personalTaxPlus10Button = smallButton(left + 108, top + 78, "+10", button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_PLUS_10));
        personalTaxPlus100Button = smallButton(left + 157, top + 78, "+100", button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_PLUS_100));

        shopTaxMinus10Button = smallButton(left + 10, top + 104, "-10", button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_MINUS_10));
        shopTaxMinus1Button = smallButton(left + 59, top + 104, "-1", button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_MINUS_1));
        shopTaxPlus1Button = smallButton(left + 108, top + 104, "+1", button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_PLUS_1));
        shopTaxPlus10Button = smallButton(left + 157, top + 104, "+10", button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_PLUS_10));

        addRenderableWidget(personalTaxMinus100Button);
        addRenderableWidget(personalTaxMinus10Button);
        addRenderableWidget(personalTaxPlus10Button);
        addRenderableWidget(personalTaxPlus100Button);

        addRenderableWidget(shopTaxMinus10Button);
        addRenderableWidget(shopTaxMinus1Button);
        addRenderableWidget(shopTaxPlus1Button);
        addRenderableWidget(shopTaxPlus10Button);

        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private Button smallButton(int x, int y, String label, Button.OnPress onPress) {
        return Button.builder(Component.literal(label), onPress)
                .bounds(x, y, 45, 14)
                .build();
    }

    private void pressButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private void toggleVisiblePermission(int row) {
        int ordinal = row >= 0 && row < visiblePermissionOrdinals.length ? visiblePermissionOrdinals[row] : -1;
        if (ordinal < 0 || ordinal >= SettlementPermission.values().length) {
            return;
        }
        pressButton(SettlementResidentManageMenu.BUTTON_TOGGLE_PERMISSION_BASE + ordinal);
    }

    private void updateButtons() {
        boolean hasTarget = menu.hasTarget();
        boolean targetLeader = menu.isTargetLeader();

        boolean canViewPermissions = hasTarget && menu.canViewPermissions();
        permissionPrevButton.visible = canViewPermissions;
        permissionNextButton.visible = canViewPermissions;
        permissionPrevButton.active = canViewPermissions && menu.getPermissionPage() > 0;
        permissionNextButton.active = canViewPermissions && menu.getPermissionPage() < getPermissionMaxPage();

        for (int i = 0; i < permissionButtons.length; i++) {
            visiblePermissionOrdinals[i] = -1;
            permissionButtons[i].visible = false;
            permissionButtons[i].active = false;
        }

        if (canViewPermissions) {
            SettlementPermission[] permissions = SettlementPermission.values();
            int start = menu.getPermissionPage() * SettlementResidentManageMenu.PERMISSION_ROWS;

            for (int row = 0; row < permissionButtons.length; row++) {
                int index = start + row;
                if (index >= permissions.length) {
                    continue;
                }

                SettlementPermission permission = permissions[index];
                visiblePermissionOrdinals[row] = permission.ordinal();
                permissionButtons[row].visible = true;
                permissionButtons[row].active = menu.canEditPermissions() && !targetLeader;
                permissionButtons[row].setMessage(Component.literal(menu.targetHasPermission(permission) ? "Вкл" : "Выкл"));
            }
        }

        boolean allowPersonalTax = hasTarget && menu.canEditPersonalTax() && !targetLeader;
        boolean allowShopTax = hasTarget && menu.canEditShopTax() && !targetLeader;

        personalTaxMinus100Button.active = allowPersonalTax;
        personalTaxMinus10Button.active = allowPersonalTax;
        personalTaxPlus10Button.active = allowPersonalTax;
        personalTaxPlus100Button.active = allowPersonalTax;

        shopTaxMinus10Button.active = allowShopTax;
        shopTaxMinus1Button.active = allowShopTax;
        shopTaxPlus1Button.active = allowShopTax;
        shopTaxPlus10Button.active = allowShopTax;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1F1F1F);
        graphics.fill(left + 6, top + 16, left + imageWidth - 6, top + 136, 0xFF2F2F2F);
        graphics.fill(left + 8, top + 18, left + 162, top + 130, 0xFF252525);
        graphics.fill(left + 168, top + 18, left + imageWidth - 8, top + 130, 0xFF252525);
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
        graphics.drawString(this.font, shorten("Поселение: " + menu.getSettlementName(), 34), 8, 6, 0xFFFFFF, false);

        if (!menu.hasTarget()) {
            graphics.drawString(this.font, "Житель не найден.", 8, 24, 0xFFB0B0, false);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
            return;
        }

        graphics.drawString(this.font, shorten("Житель: " + menu.getTargetName(), 22), 10, 22, menu.isTargetLeader() ? 0xFFFF88 : 0xFFFFFF, false);
        graphics.drawString(this.font, shorten("UUID: " + menu.getTargetPlayerUuidString(), 28), 10, 34, 0xD0D0D0, false);

        String debtText = menu.canViewTargetDebt() ? String.valueOf(menu.getTargetPersonalDebt()) : "скрыт";
        graphics.drawString(this.font, "Личный долг: " + debtText, 10, 50, 0xFFD8A8, false);

        graphics.drawString(this.font, "Личный налог: " + menu.getTargetPersonalTaxAmount(), 10, 66, 0xFFFFFF, false);
        graphics.drawString(this.font, "Налог магазина: " + menu.getTargetShopTaxPercent() + "%", 10, 92, 0xFFFFFF, false);

        graphics.drawString(this.font, "Права", 170, 22, 0xFFFFFF, false);
        if (menu.canViewPermissions()) {
            graphics.drawString(this.font, buildPageText(menu.getPermissionPage(), getPermissionMaxPage()), 222, 22, 0xFFFFFF, false);

            SettlementPermission[] permissions = SettlementPermission.values();
            int start = menu.getPermissionPage() * SettlementResidentManageMenu.PERMISSION_ROWS;
            for (int row = 0; row < SettlementResidentManageMenu.PERMISSION_ROWS; row++) {
                int index = start + row;
                if (index >= permissions.length) {
                    continue;
                }

                SettlementPermission permission = permissions[index];
                int y = 54 + row * 14;
                int color = menu.targetHasPermission(permission) ? 0xA8FFA8 : 0xFFB0B0;
                graphics.drawString(this.font, shorten(formatPermissionName(permission), 16), 170, y, color, false);
            }
        } else {
            graphics.drawString(this.font, "Нет права смотреть права.", 170, 54, 0xFFB0B0, false);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
    }

    private int getPermissionMaxPage() {
        int size = SettlementPermission.values().length;
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / SettlementResidentManageMenu.PERMISSION_ROWS;
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

    private String formatPermissionName(SettlementPermission permission) {
        String raw = permission.name().toLowerCase().replace('_', ' ');
        StringBuilder builder = new StringBuilder(raw.length());
        boolean capitalize = true;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (capitalize && c >= 'a' && c <= 'z') {
                builder.append((char) (c - 32));
                capitalize = false;
            } else {
                builder.append(c);
                capitalize = c == ' ';
            }
        }

        return builder.toString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
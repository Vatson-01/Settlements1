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

    private Button accrueDebtButton;
    private Button clearDebtButton;

    public SettlementResidentManageScreen(SettlementResidentManageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 376;
        this.imageHeight = 236;
        this.inventoryLabelY = 142;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        permissionPrevButton = Button.builder(
                        Component.literal("<"),
                        button -> pressButton(SettlementResidentManageMenu.BUTTON_PERMISSION_PAGE_PREV)
                )
                .bounds(left + 300, top + 6, 24, 18)
                .build();

        permissionNextButton = Button.builder(
                        Component.literal(">"),
                        button -> pressButton(SettlementResidentManageMenu.BUTTON_PERMISSION_PAGE_NEXT)
                )
                .bounds(left + 328, top + 6, 24, 18)
                .build();

        addRenderableWidget(permissionPrevButton);
        addRenderableWidget(permissionNextButton);

        int buttonX = left + 320;
        int buttonY = top + 40;
        for (int i = 0; i < permissionButtons.length; i++) {
            final int row = i;
            visiblePermissionOrdinals[i] = -1;
            permissionButtons[i] = Button.builder(
                            Component.literal("Выкл"),
                            button -> toggleVisiblePermission(row)
                    )
                    .bounds(buttonX, buttonY + i * 14, 44, 12)
                    .build();
            addRenderableWidget(permissionButtons[i]);
        }

        personalTaxMinus100Button = smallButton(left + 10, top + 78, "-100",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_MINUS_100));
        personalTaxMinus10Button = smallButton(left + 54, top + 78, "-10",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_MINUS_10));
        personalTaxPlus10Button = smallButton(left + 98, top + 78, "+10",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_PLUS_10));
        personalTaxPlus100Button = smallButton(left + 142, top + 78, "+100",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_PERSONAL_TAX_PLUS_100));

        shopTaxMinus10Button = smallButton(left + 10, top + 104, "-10",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_MINUS_10));
        shopTaxMinus1Button = smallButton(left + 54, top + 104, "-1",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_MINUS_1));
        shopTaxPlus1Button = smallButton(left + 98, top + 104, "+1",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_PLUS_1));
        shopTaxPlus10Button = smallButton(left + 142, top + 104, "+10",
                button -> pressButton(SettlementResidentManageMenu.BUTTON_SHOP_TAX_PLUS_10));

        addRenderableWidget(personalTaxMinus100Button);
        addRenderableWidget(personalTaxMinus10Button);
        addRenderableWidget(personalTaxPlus10Button);
        addRenderableWidget(personalTaxPlus100Button);

        addRenderableWidget(shopTaxMinus10Button);
        addRenderableWidget(shopTaxMinus1Button);
        addRenderableWidget(shopTaxPlus1Button);
        addRenderableWidget(shopTaxPlus10Button);

        accrueDebtButton = Button.builder(
                        Component.literal("Начислить долг"),
                        button -> pressButton(SettlementResidentManageMenu.BUTTON_ACCRUE_PERSONAL_DEBT)
                )
                .bounds(left + 10, top + 122, 84, 14)
                .build();

        clearDebtButton = Button.builder(
                        Component.literal("Списать долг"),
                        button -> pressButton(SettlementResidentManageMenu.BUTTON_CLEAR_PERSONAL_DEBT)
                )
                .bounds(left + 98, top + 122, 84, 14)
                .build();

        addRenderableWidget(accrueDebtButton);
        addRenderableWidget(clearDebtButton);

        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private Button smallButton(int x, int y, String label, Button.OnPress onPress) {
        return Button.builder(Component.literal(label), onPress)
                .bounds(x, y, 40, 14)
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
        boolean allowAccrueDebt = hasTarget && menu.canAccruePersonalDebt() && !targetLeader;
        boolean allowClearDebt = hasTarget && menu.canClearPersonalDebt() && !targetLeader;

        personalTaxMinus100Button.active = allowPersonalTax;
        personalTaxMinus10Button.active = allowPersonalTax;
        personalTaxPlus10Button.active = allowPersonalTax;
        personalTaxPlus100Button.active = allowPersonalTax;

        shopTaxMinus10Button.active = allowShopTax;
        shopTaxMinus1Button.active = allowShopTax;
        shopTaxPlus1Button.active = allowShopTax;
        shopTaxPlus10Button.active = allowShopTax;

        accrueDebtButton.visible = hasTarget;
        accrueDebtButton.active = allowAccrueDebt;

        clearDebtButton.visible = hasTarget;
        clearDebtButton.active = allowClearDebt;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF1F1F1F);
        graphics.fill(left + 6, top + 16, left + imageWidth - 6, top + 136, 0xFF2F2F2F);
        graphics.fill(left + 8, top + 18, left + 186, top + 130, 0xFF252525);
        graphics.fill(left + 192, top + 18, left + imageWidth - 8, top + 130, 0xFF252525);
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
        graphics.drawString(this.font, shorten("Поселение: " + menu.getSettlementName(), 40), 8, 6, 0xFFFFFF, false);

        if (!menu.hasTarget()) {
            graphics.drawString(this.font, "Житель не найден.", 8, 24, 0xFFB0B0, false);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFFFFFF, false);
            return;
        }

        graphics.drawString(
                this.font,
                shorten("Житель: " + menu.getTargetName(), 24),
                10,
                22,
                menu.isTargetLeader() ? 0xFFFF88 : 0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                shorten("UUID: " + menu.getTargetPlayerUuidString(), 28),
                10,
                34,
                0xD0D0D0,
                false
        );

        String debtText = menu.canViewTargetDebt() ? String.valueOf(menu.getTargetPersonalDebt()) : "скрыт";
        graphics.drawString(this.font, "Личный долг: " + debtText, 10, 50, 0xFFD8A8, false);

        graphics.drawString(this.font, "Личный налог: " + menu.getTargetPersonalTaxAmount(), 10, 66, 0xFFFFFF, false);
        graphics.drawString(this.font, "Налог магазина: " + menu.getTargetShopTaxPercent() + "%", 10, 92, 0xFFFFFF, false);

        graphics.drawString(this.font, "Права", 194, 12, 0xFFFFFF, false);
        if (menu.canViewPermissions()) {
            graphics.drawString(
                    this.font,
                    buildPageText(menu.getPermissionPage(), getPermissionMaxPage()),
                    244,
                    12,
                    0xFFFFFF,
                    false
            );

            SettlementPermission[] permissions = SettlementPermission.values();
            int start = menu.getPermissionPage() * SettlementResidentManageMenu.PERMISSION_ROWS;
            for (int row = 0; row < SettlementResidentManageMenu.PERMISSION_ROWS; row++) {
                int index = start + row;
                if (index >= permissions.length) {
                    continue;
                }

                SettlementPermission permission = permissions[index];
                int y = 42 + row * 14;
                int color = menu.targetHasPermission(permission) ? 0xA8FFA8 : 0xFFB0B0;
                String translated = formatPermissionName(permission);
                String fitted = trimToWidth(translated, 118);

                graphics.drawString(this.font, fitted, 194, y, color, false);
            }
        } else {
            graphics.drawString(this.font, "Нет права смотреть права.", 194, 46, 0xFFB0B0, false);
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

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String next = builder.toString() + c;
            if (this.font.width(next) + ellipsisWidth > maxWidth) {
                break;
            }
            builder.append(c);
        }

        return builder.toString() + ellipsis;
    }

    private String formatPermissionName(SettlementPermission permission) {
        if (permission == null) {
            return "";
        }

        switch (permission) {
            case INVITE_PLAYERS:
                return "Приглашать игроков";
            case KICK_PLAYERS:
                return "Исключать игроков";
            case VIEW_RESIDENTS:
                return "Просматривать жителей";
            case VIEW_RESIDENT_PERMISSIONS:
                return "Смотреть права жителей";
            case GRANT_PERMISSIONS:
                return "Выдавать права";

            case TRANSFER_PLOTS:
                return "Передавать участки";
            case SEIZE_PLOTS:
                return "Изымать участки";
            case BUY_CHUNKS:
                return "Покупать чанки";
            case REMOVE_CHUNKS:
                return "Убирать чанки";
            case ASSIGN_PERSONAL_PLOTS:
                return "Назначать личные участки";
            case ASSIGN_PUBLIC_PLOTS:
                return "Назначать общие участки";
            case VIEW_BOUNDARIES:
                return "Смотреть границы";

            case DEPOSIT_TREASURY:
                return "Вносить в казну";
            case WITHDRAW_TREASURY:
                return "Выводить из казны";
            case VIEW_TREASURY_BALANCE:
                return "Смотреть баланс казны";
            case VIEW_SETTLEMENT_DEBT:
                return "Смотреть долг поселения";
            case VIEW_PLAYER_DEBTS:
                return "Смотреть долги игроков";
            case CHANGE_PLAYER_TAX:
                return "Менять личный налог";
            case CHANGE_PLAYER_SHOP_TAX:
                return "Менять налог магазина";

            case PLACE_SHOP:
                return "Ставить магазин";
            case REMOVE_FOREIGN_SHOP:
                return "Удалять чужой магазин";
            case TRANSFER_FOREIGN_SHOP:
                return "Передавать чужой магазин";
            case VIEW_FOREIGN_SHOPS:
                return "Смотреть чужие магазины";

            case ENABLE_RECONSTRUCTION:
                return "Запускать реконструкцию";
            case DISABLE_RECONSTRUCTION:
                return "Останавливать реконструкцию";
            case OPEN_RECONSTRUCTION_STORAGE:
                return "Открывать склад реконструкции";
            case CONTRIBUTE_RECONSTRUCTION_RESOURCES:
                return "Вносить ресурсы";
            case CLEAR_RECONSTRUCTION:
                return "Очищать реконструкцию";

            case VIEW_WAR_STATUS:
                return "Смотреть войны";
            case MANAGE_VASSALAGE:
                return "Управлять вассалитетом";

            case CREATE_PUBLIC_DOORS:
                return "Создавать публичные двери";
            case CREATE_PUBLIC_CONTAINERS:
                return "Создавать публичные контейнеры";

            default:
                return humanizeFallback(permission.name());
        }
    }
    private String humanizeFallback(String rawName) {
        if (rawName == null) {
            return "";
        }

        String raw = rawName.toLowerCase().replace('_', ' ');
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

    private SettlementPermission getHoveredPermission(int mouseX, int mouseY) {
        if (!menu.canViewPermissions()) {
            return null;
        }

        int start = menu.getPermissionPage() * SettlementResidentManageMenu.PERMISSION_ROWS;
        SettlementPermission[] permissions = SettlementPermission.values();

        for (int row = 0; row < SettlementResidentManageMenu.PERMISSION_ROWS; row++) {
            int index = start + row;
            if (index >= permissions.length) {
                continue;
            }

            int x1 = this.leftPos + 194;
            int y1 = this.topPos + 42 + row * 14;
            int x2 = x1 + 118;
            int y2 = y1 + 12;

            if (mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2) {
                return permissions[index];
            }
        }

        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        SettlementPermission hoveredPermission = getHoveredPermission(mouseX, mouseY);
        if (hoveredPermission != null) {
            graphics.renderTooltip(this.font, Component.literal(formatPermissionName(hoveredPermission)), mouseX, mouseY);
        }

        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
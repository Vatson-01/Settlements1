package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

public class SettlementResidentManageMenu extends AbstractContainerMenu {
    public static final int PERMISSION_ROWS = 7;

    public static final int BUTTON_PERMISSION_PAGE_PREV = 10;
    public static final int BUTTON_PERMISSION_PAGE_NEXT = 11;

    public static final int BUTTON_PERSONAL_TAX_MINUS_100 = 20;
    public static final int BUTTON_PERSONAL_TAX_MINUS_10 = 21;
    public static final int BUTTON_PERSONAL_TAX_PLUS_10 = 22;
    public static final int BUTTON_PERSONAL_TAX_PLUS_100 = 23;

    public static final int BUTTON_SHOP_TAX_MINUS_10 = 24;
    public static final int BUTTON_SHOP_TAX_MINUS_1 = 25;
    public static final int BUTTON_SHOP_TAX_PLUS_1 = 26;
    public static final int BUTTON_SHOP_TAX_PLUS_10 = 27;

    public static final int BUTTON_TOGGLE_PERMISSION_BASE = 100;

    private static final int DATA_PERMISSION_PAGE = 0;
    private static final int DATA_TARGET_EXISTS = 1;
    private static final int DATA_TARGET_IS_LEADER = 2;
    private static final int DATA_CAN_EDIT_PERMISSIONS = 3;
    private static final int DATA_CAN_EDIT_PERSONAL_TAX = 4;
    private static final int DATA_CAN_EDIT_SHOP_TAX = 5;
    private static final int DATA_CAN_VIEW_TARGET_DEBT = 6;
    private static final int DATA_CAN_VIEW_PERMISSIONS = 7;
    private static final int DATA_TARGET_SHOP_TAX = 8;

    private static final int DATA_PERMISSION_MASK_WORD_0 = 9;
    private static final int DATA_PERMISSION_MASK_WORD_1 = 10;
    private static final int DATA_PERMISSION_MASK_WORD_2 = 11;
    private static final int DATA_PERMISSION_MASK_WORD_3 = 12;

    private static final int DATA_TARGET_PERSONAL_TAX_WORD_0 = 13;
    private static final int DATA_TARGET_PERSONAL_TAX_WORD_1 = 14;
    private static final int DATA_TARGET_PERSONAL_TAX_WORD_2 = 15;
    private static final int DATA_TARGET_PERSONAL_TAX_WORD_3 = 16;

    private static final int DATA_TARGET_DEBT_WORD_0 = 17;
    private static final int DATA_TARGET_DEBT_WORD_1 = 18;
    private static final int DATA_TARGET_DEBT_WORD_2 = 19;
    private static final int DATA_TARGET_DEBT_WORD_3 = 20;

    private static final int DATA_COUNT = 21;

    private final UUID settlementId;
    private final UUID targetPlayerUuid;
    private final String settlementName;
    private final String targetName;
    private final ContainerData menuData;

    public SettlementResidentManageMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                OpenData.read(buf),
                createClientData()
        );
    }

    private SettlementResidentManageMenu(
            int containerId,
            Inventory playerInventory,
            OpenData openData,
            ContainerData menuData
    ) {
        super(ModMenuTypes.SETTLEMENT_RESIDENT_MANAGE_MENU.get(), containerId);
        this.settlementId = openData.settlementId;
        this.targetPlayerUuid = openData.targetPlayerUuid;
        this.settlementName = openData.settlementName;
        this.targetName = openData.targetName;
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
    }

    public static void openFor(ServerPlayer serverPlayer, UUID settlementId, UUID targetPlayerUuid) {
        if (serverPlayer == null || serverPlayer.server == null || settlementId == null || targetPlayerUuid == null) {
            return;
        }

        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        SettlementMember self = settlement == null ? null : settlement.getMember(serverPlayer.getUUID());
        SettlementMember target = settlement == null ? null : settlement.getMember(targetPlayerUuid);

        if (settlement == null || target == null) {
            serverPlayer.displayClientMessage(Component.literal("Житель не найден."), true);
            return;
        }

        if (!canOpenMenu(settlement, self, serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.literal("Нет права открывать меню жителей."), true);
            return;
        }

        final OpenData openData = buildOpenData(serverPlayer.getInventory(), settlementId, targetPlayerUuid);

        NetworkHooks.openScreen(
                serverPlayer,
                new net.minecraft.world.SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new SettlementResidentManageMenu(
                                containerId,
                                playerInventory,
                                openData,
                                createServerData(playerInventory, openData, 0)
                        ),
                        Component.literal("Житель: " + openData.targetName)
                ),
                buf -> openData.write(buf)
        );
    }

    private static OpenData buildOpenData(Inventory playerInventory, UUID settlementId, UUID targetPlayerUuid) {
        if (!(playerInventory.player instanceof ServerPlayer)) {
            return OpenData.empty(settlementId, targetPlayerUuid);
        }

        ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            return OpenData.empty(settlementId, targetPlayerUuid);
        }

        SettlementMember target = settlement.getMember(targetPlayerUuid);
        String targetName = target == null ? targetPlayerUuid.toString() : resolvePlayerName(serverPlayer, target.getPlayerUuid());

        return new OpenData(
                settlementId,
                targetPlayerUuid,
                settlement.getName(),
                targetName
        );
    }

    public static void writeOpenData(FriendlyByteBuf buf, ServerPlayer player, UUID settlementId, UUID targetPlayerUuid) {
        OpenData openData = buildOpenData(player.getInventory(), settlementId, targetPlayerUuid);
        openData.write(buf);
    }

    private static ContainerData createClientData() {
        return new SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(
            final Inventory playerInventory,
            final OpenData openData,
            final int initialPermissionPage
    ) {
        return new ContainerData() {
            private int permissionPage = Math.max(0, initialPermissionPage);

            private Settlement getSettlement() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return null;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getSettlement(openData.settlementId);
            }

            private SettlementMember getSelfMember() {
                Settlement settlement = getSettlement();
                if (settlement == null) {
                    return null;
                }
                return settlement.getMember(playerInventory.player.getUUID());
            }

            private SettlementMember getTargetMember() {
                Settlement settlement = getSettlement();
                if (settlement == null) {
                    return null;
                }
                return settlement.getMember(openData.targetPlayerUuid);
            }

            @Override
            public int get(int index) {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                SettlementMember target = getTargetMember();
                UUID actorUuid = playerInventory.player.getUUID();

                if (index == DATA_PERMISSION_PAGE) {
                    return permissionPage;
                }
                if (index == DATA_TARGET_EXISTS) {
                    return target == null ? 0 : 1;
                }
                if (index == DATA_TARGET_IS_LEADER) {
                    return target != null && target.isLeader() ? 1 : 0;
                }
                if (index == DATA_CAN_EDIT_PERMISSIONS) {
                    return canEditPermissions(settlement, self, target, actorUuid) ? 1 : 0;
                }
                if (index == DATA_CAN_EDIT_PERSONAL_TAX) {
                    return canEditPersonalTax(settlement, self, target, actorUuid) ? 1 : 0;
                }
                if (index == DATA_CAN_EDIT_SHOP_TAX) {
                    return canEditShopTax(settlement, self, target, actorUuid) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_TARGET_DEBT) {
                    return canViewTargetDebt(settlement, self, target, actorUuid) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_PERMISSIONS) {
                    return canViewPermissions(settlement, self, actorUuid) ? 1 : 0;
                }
                if (index == DATA_TARGET_SHOP_TAX) {
                    return target == null ? 0 : target.getShopTaxPercent();
                }

                long permissionMask = encodePermissionMask(target);
                if (index == DATA_PERMISSION_MASK_WORD_0) {
                    return getWord(permissionMask, 0);
                }
                if (index == DATA_PERMISSION_MASK_WORD_1) {
                    return getWord(permissionMask, 1);
                }
                if (index == DATA_PERMISSION_MASK_WORD_2) {
                    return getWord(permissionMask, 2);
                }
                if (index == DATA_PERMISSION_MASK_WORD_3) {
                    return getWord(permissionMask, 3);
                }

                long personalTax = target == null ? 0L : target.getPersonalTaxAmount();
                if (index == DATA_TARGET_PERSONAL_TAX_WORD_0) {
                    return getWord(personalTax, 0);
                }
                if (index == DATA_TARGET_PERSONAL_TAX_WORD_1) {
                    return getWord(personalTax, 1);
                }
                if (index == DATA_TARGET_PERSONAL_TAX_WORD_2) {
                    return getWord(personalTax, 2);
                }
                if (index == DATA_TARGET_PERSONAL_TAX_WORD_3) {
                    return getWord(personalTax, 3);
                }

                long debt = target == null ? 0L : target.getPersonalTaxDebt();
                if (index == DATA_TARGET_DEBT_WORD_0) {
                    return getWord(debt, 0);
                }
                if (index == DATA_TARGET_DEBT_WORD_1) {
                    return getWord(debt, 1);
                }
                if (index == DATA_TARGET_DEBT_WORD_2) {
                    return getWord(debt, 2);
                }
                if (index == DATA_TARGET_DEBT_WORD_3) {
                    return getWord(debt, 3);
                }

                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_PERMISSION_PAGE) {
                    int maxPage = getMaxPage(SettlementPermission.values().length, PERMISSION_ROWS);
                    if (value < 0) {
                        permissionPage = 0;
                    } else {
                        permissionPage = Math.min(value, maxPage);
                    }
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static String resolvePlayerName(ServerPlayer opener, UUID playerUuid) {
        ServerPlayer online = opener.server.getPlayerList().getPlayer(playerUuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return playerUuid.toString();
    }

    private static boolean isLeader(Settlement settlement, UUID actorUuid) {
        return settlement != null && actorUuid != null && settlement.isLeader(actorUuid);
    }

    private static boolean hasPermission(Settlement settlement, SettlementMember self, UUID actorUuid, SettlementPermission permission) {
        if (permission == null) {
            return false;
        }
        if (isLeader(settlement, actorUuid)) {
            return true;
        }
        return self != null && self.getPermissionSet().has(permission);
    }

    private static boolean canOpenMenu(Settlement settlement, SettlementMember self, UUID actorUuid) {
        return hasPermission(settlement, self, actorUuid, SettlementPermission.VIEW_RESIDENTS)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.GRANT_PERMISSIONS)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.CHANGE_PLAYER_TAX)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.CHANGE_PLAYER_SHOP_TAX)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.VIEW_RESIDENT_PERMISSIONS);
    }

    private static boolean canViewPermissions(Settlement settlement, SettlementMember self, UUID actorUuid) {
        return hasPermission(settlement, self, actorUuid, SettlementPermission.GRANT_PERMISSIONS)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.VIEW_RESIDENT_PERMISSIONS);
    }

    private static boolean canEditPermissions(Settlement settlement, SettlementMember self, SettlementMember target, UUID actorUuid) {
        return target != null
                && !target.isLeader()
                && hasPermission(settlement, self, actorUuid, SettlementPermission.GRANT_PERMISSIONS);
    }

    private static boolean canEditPersonalTax(Settlement settlement, SettlementMember self, SettlementMember target, UUID actorUuid) {
        return target != null
                && !target.isLeader()
                && (hasPermission(settlement, self, actorUuid, SettlementPermission.CHANGE_PLAYER_TAX)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.GRANT_PERMISSIONS));
    }

    private static boolean canEditShopTax(Settlement settlement, SettlementMember self, SettlementMember target, UUID actorUuid) {
        return target != null
                && !target.isLeader()
                && (hasPermission(settlement, self, actorUuid, SettlementPermission.CHANGE_PLAYER_SHOP_TAX)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.GRANT_PERMISSIONS));
    }

    private static boolean canViewTargetDebt(Settlement settlement, SettlementMember self, SettlementMember target, UUID actorUuid) {
        if (target == null || actorUuid == null) {
            return false;
        }
        if (isLeader(settlement, actorUuid)) {
            return true;
        }
        if (actorUuid.equals(target.getPlayerUuid())) {
            return true;
        }
        if (canEditPersonalTax(settlement, self, target, actorUuid)) {
            return true;
        }
        return hasPermission(settlement, self, actorUuid, SettlementPermission.VIEW_PLAYER_DEBTS);
    }

    private static long encodePermissionMask(SettlementMember member) {
        if (member == null) {
            return 0L;
        }

        long mask = 0L;
        for (SettlementPermission permission : SettlementPermission.values()) {
            if (member.getPermissionSet().has(permission)) {
                mask |= (1L << permission.ordinal());
            }
        }
        return mask;
    }

    private static int getWord(long value, int wordIndex) {
        return (int) ((value >>> (wordIndex * 16)) & 0xFFFFL);
    }

    private static long readWords(ContainerData data, int firstIndex, int wordCount) {
        long result = 0L;
        for (int i = 0; i < wordCount; i++) {
            result |= ((long) data.get(firstIndex + i) & 0xFFFFL) << (i * 16);
        }
        return result;
    }

    private static int getMaxPage(int size, int pageSize) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / pageSize;
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 152;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 210;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, startX + column * 18, hotbarY));
        }
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public UUID getTargetPlayerUuid() {
        return targetPlayerUuid;
    }

    public String getSettlementName() {
        return settlementName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetPlayerUuidString() {
        return targetPlayerUuid == null ? "" : targetPlayerUuid.toString();
    }

    public int getPermissionPage() {
        return menuData.get(DATA_PERMISSION_PAGE);
    }

    public boolean hasTarget() {
        return menuData.get(DATA_TARGET_EXISTS) != 0;
    }

    public boolean isTargetLeader() {
        return menuData.get(DATA_TARGET_IS_LEADER) != 0;
    }

    public boolean canEditPermissions() {
        return menuData.get(DATA_CAN_EDIT_PERMISSIONS) != 0;
    }

    public boolean canEditPersonalTax() {
        return menuData.get(DATA_CAN_EDIT_PERSONAL_TAX) != 0;
    }

    public boolean canEditShopTax() {
        return menuData.get(DATA_CAN_EDIT_SHOP_TAX) != 0;
    }

    public boolean canViewTargetDebt() {
        return menuData.get(DATA_CAN_VIEW_TARGET_DEBT) != 0;
    }

    public boolean canViewPermissions() {
        return menuData.get(DATA_CAN_VIEW_PERMISSIONS) != 0;
    }

    public int getTargetShopTaxPercent() {
        return menuData.get(DATA_TARGET_SHOP_TAX);
    }

    public long getTargetPermissionMask() {
        return readWords(menuData, DATA_PERMISSION_MASK_WORD_0, 4);
    }

    public boolean targetHasPermission(SettlementPermission permission) {
        if (permission == null) {
            return false;
        }
        long mask = getTargetPermissionMask();
        return (mask & (1L << permission.ordinal())) != 0L;
    }

    public long getTargetPersonalTaxAmount() {
        return readWords(menuData, DATA_TARGET_PERSONAL_TAX_WORD_0, 4);
    }

    public long getTargetPersonalDebt() {
        return readWords(menuData, DATA_TARGET_DEBT_WORD_0, 4);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PERMISSION_PAGE_PREV) {
            menuData.set(DATA_PERMISSION_PAGE, Math.max(0, getPermissionPage() - 1));
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PERMISSION_PAGE_NEXT) {
            int maxPage = getMaxPage(SettlementPermission.values().length, PERMISSION_ROWS);
            menuData.set(DATA_PERMISSION_PAGE, Math.min(maxPage, getPermissionPage() + 1));
            broadcastChanges();
            return true;
        }

        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        try {
            SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
            Settlement settlement = data.getSettlement(settlementId);
            SettlementMember self = settlement == null ? null : settlement.getMember(serverPlayer.getUUID());
            SettlementMember target = settlement == null ? null : settlement.getMember(targetPlayerUuid);

            if (settlement == null || target == null) {
                throw new IllegalStateException("Житель не найден.");
            }

            if (buttonId == BUTTON_PERSONAL_TAX_MINUS_100
                    || buttonId == BUTTON_PERSONAL_TAX_MINUS_10
                    || buttonId == BUTTON_PERSONAL_TAX_PLUS_10
                    || buttonId == BUTTON_PERSONAL_TAX_PLUS_100) {

                if (!canEditPersonalTax(settlement, self, target, serverPlayer.getUUID())) {
                    throw new IllegalStateException("Нет права на изменение личного налога.");
                }

                long delta = buttonId == BUTTON_PERSONAL_TAX_MINUS_100 ? -100L
                        : buttonId == BUTTON_PERSONAL_TAX_MINUS_10 ? -10L
                        : buttonId == BUTTON_PERSONAL_TAX_PLUS_10 ? 10L
                        : 100L;

                long updatedTax = target.getPersonalTaxAmount() + delta;
                if (updatedTax < 0L) {
                    updatedTax = 0L;
                }

                target.setPersonalTaxAmount(updatedTax);
                data.setDirty();
                broadcastChanges();
                refreshOtherOpenMenusForTarget(serverPlayer, settlementId, targetPlayerUuid);
                return true;
            }

            if (buttonId == BUTTON_SHOP_TAX_MINUS_10
                    || buttonId == BUTTON_SHOP_TAX_MINUS_1
                    || buttonId == BUTTON_SHOP_TAX_PLUS_1
                    || buttonId == BUTTON_SHOP_TAX_PLUS_10) {

                if (!canEditShopTax(settlement, self, target, serverPlayer.getUUID())) {
                    throw new IllegalStateException("Нет права на изменение налога магазинов.");
                }

                int delta = buttonId == BUTTON_SHOP_TAX_MINUS_10 ? -10
                        : buttonId == BUTTON_SHOP_TAX_MINUS_1 ? -1
                        : buttonId == BUTTON_SHOP_TAX_PLUS_1 ? 1
                        : 10;

                int updated = target.getShopTaxPercent() + delta;
                if (updated < 0) {
                    updated = 0;
                }
                if (updated > 100) {
                    updated = 100;
                }

                target.setShopTaxPercent(updated);
                data.setDirty();
                broadcastChanges();
                refreshOtherOpenMenusForTarget(serverPlayer, settlementId, targetPlayerUuid);
                return true;
            }

            if (buttonId >= BUTTON_TOGGLE_PERMISSION_BASE
                    && buttonId < BUTTON_TOGGLE_PERMISSION_BASE + SettlementPermission.values().length) {

                int ordinal = buttonId - BUTTON_TOGGLE_PERMISSION_BASE;
                if (ordinal < 0 || ordinal >= SettlementPermission.values().length) {
                    return false;
                }

                if (!canEditPermissions(settlement, self, target, serverPlayer.getUUID())) {
                    throw new IllegalStateException("Нет права на изменение прав.");
                }

                SettlementPermission permission = SettlementPermission.values()[ordinal];
                if (target.getPermissionSet().has(permission)) {
                    target.getPermissionSet().revoke(permission);
                } else {
                    target.getPermissionSet().grant(permission);
                }

                data.setDirty();
                broadcastChanges();
                refreshOtherOpenMenusForTarget(serverPlayer, settlementId, targetPlayerUuid);
                return true;
            }
        } catch (Exception e) {
            serverPlayer.displayClientMessage(Component.literal(messageOrDefault(e, "Ошибка выполнения действия.")), true);
            return false;
        }

        return false;
    }

    private static void refreshOtherOpenMenusForTarget(ServerPlayer sourcePlayer, UUID settlementId, UUID targetPlayerUuid) {
        if (sourcePlayer == null || sourcePlayer.server == null || settlementId == null || targetPlayerUuid == null) {
            return;
        }

        List<ServerPlayer> players = sourcePlayer.server.getPlayerList().getPlayers();
        for (ServerPlayer online : players) {
            if (online == sourcePlayer) {
                continue;
            }
            if (!(online.containerMenu instanceof SettlementResidentManageMenu)) {
                continue;
            }

            SettlementResidentManageMenu openMenu = (SettlementResidentManageMenu) online.containerMenu;
            if (!settlementId.equals(openMenu.getSettlementId())) {
                continue;
            }
            if (!targetPlayerUuid.equals(openMenu.getTargetPlayerUuid())) {
                continue;
            }

            openMenu.reopenFor(online);
        }
    }

    private void reopenFor(final ServerPlayer serverPlayer) {
        final OpenData openData = buildOpenData(serverPlayer.getInventory(), settlementId, targetPlayerUuid);
        final int permissionPage = getPermissionPage();

        NetworkHooks.openScreen(
                serverPlayer,
                new net.minecraft.world.SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new SettlementResidentManageMenu(
                                containerId,
                                playerInventory,
                                openData,
                                createServerData(playerInventory, openData, permissionPage)
                        ),
                        Component.literal("Житель: " + openData.targetName)
                ),
                buf -> openData.write(buf)
        );
    }

    private static String messageOrDefault(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return true;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        Settlement settlement = SettlementSavedData.get(serverPlayer.server).getSettlementByPlayer(serverPlayer.getUUID());
        if (settlement == null || !settlement.getId().equals(settlementId)) {
            return false;
        }

        return settlement.getMember(targetPlayerUuid) != null;
    }

    private static final class OpenData {
        private final UUID settlementId;
        private final UUID targetPlayerUuid;
        private final String settlementName;
        private final String targetName;

        private OpenData(UUID settlementId, UUID targetPlayerUuid, String settlementName, String targetName) {
            this.settlementId = settlementId;
            this.targetPlayerUuid = targetPlayerUuid;
            this.settlementName = settlementName;
            this.targetName = targetName;
        }

        private static OpenData empty(UUID settlementId, UUID targetPlayerUuid) {
            return new OpenData(
                    settlementId,
                    targetPlayerUuid,
                    "Поселение",
                    "Неизвестно"
            );
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(settlementId);
            buf.writeUUID(targetPlayerUuid);
            buf.writeUtf(settlementName);
            buf.writeUtf(targetName);
        }

        private static OpenData read(FriendlyByteBuf buf) {
            UUID settlementId = buf.readUUID();
            UUID targetPlayerUuid = buf.readUUID();
            String settlementName = buf.readUtf();
            String targetName = buf.readUtf();
            return new OpenData(settlementId, targetPlayerUuid, settlementName, targetName);
        }
    }
}
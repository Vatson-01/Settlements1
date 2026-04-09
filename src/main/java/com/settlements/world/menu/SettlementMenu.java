package com.settlements.world.menu;

import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.ReconstructionBlockEntry;
import com.settlements.data.model.ReconstructionSession;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.SiegeState;
import com.settlements.data.model.WarRecord;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.ReconstructionRestoreResult;
import com.settlements.service.ReconstructionService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SettlementMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 10;

    public static final int BUTTON_TAB_OVERVIEW = 0;
    public static final int BUTTON_TAB_RESIDENTS = 1;
    public static final int BUTTON_TAB_WAR = 2;
    public static final int BUTTON_TAB_RECONSTRUCTION = 3;
    public static final int BUTTON_PAGE_PREV = 10;
    public static final int BUTTON_PAGE_NEXT = 11;
    public static final int BUTTON_OPEN_RECONSTRUCTION_STORAGE = 12;
    public static final int BUTTON_RESTORE_RECONSTRUCTION = 13;

    public static final int BUTTON_SELECT_RESIDENT_BASE = 1000;
    public static final int BUTTON_TOGGLE_SELECTED_PERMISSION_BASE = 2000;
    public static final int BUTTON_SELECTED_PERSONAL_TAX_MINUS_100 = 3000;
    public static final int BUTTON_SELECTED_PERSONAL_TAX_MINUS_10 = 3001;
    public static final int BUTTON_SELECTED_PERSONAL_TAX_PLUS_10 = 3002;
    public static final int BUTTON_SELECTED_PERSONAL_TAX_PLUS_100 = 3003;
    public static final int BUTTON_SELECTED_SHOP_TAX_MINUS_10 = 3010;
    public static final int BUTTON_SELECTED_SHOP_TAX_MINUS_1 = 3011;
    public static final int BUTTON_SELECTED_SHOP_TAX_PLUS_1 = 3012;
    public static final int BUTTON_SELECTED_SHOP_TAX_PLUS_10 = 3013;
    public static final int BUTTON_SKIP_RECON_ENTRY_BASE = 4000;
    public static final int BUTTON_STOP_RECONSTRUCTION = 1000000;

    private static final int DATA_SELECTED_TAB = 0;
    private static final int DATA_RESIDENT_PAGE = 1;
    private static final int DATA_WAR_PAGE = 2;
    private static final int DATA_RECONSTRUCTION_PAGE = 3;
    private static final int DATA_MEMBER_COUNT = 4;
    private static final int DATA_CLAIM_COUNT = 5;
    private static final int DATA_ALLOWANCE = 6;
    private static final int DATA_IS_LEADER = 7;
    private static final int DATA_TREASURY_LOW = 8;
    private static final int DATA_TREASURY_HIGH = 9;
    private static final int DATA_SETTLEMENT_DEBT_LOW = 10;
    private static final int DATA_SETTLEMENT_DEBT_HIGH = 11;
    private static final int DATA_PLAYER_DEBT_LOW = 12;
    private static final int DATA_PLAYER_DEBT_HIGH = 13;
    private static final int DATA_RECON_TOTAL = 14;
    private static final int DATA_RECON_PENDING = 15;
    private static final int DATA_RECON_RESTORED = 16;
    private static final int DATA_RECON_SKIPPED = 17;
    private static final int DATA_HAS_RECONSTRUCTION = 18;
    private static final int DATA_CAN_OPEN_RECON_STORAGE = 19;
    private static final int DATA_CAN_RESTORE_RECON = 20;
    private static final int DATA_ACTIVE_WAR_COUNT = 21;
    private static final int DATA_IS_UNDER_SIEGE = 22;
    private static final int DATA_IS_ATTACKING_SIEGE = 23;
    private static final int DATA_SELECTED_RESIDENT_INDEX = 24;
    private static final int DATA_SELECTED_RESIDENT_EXISTS = 25;
    private static final int DATA_SELECTED_RESIDENT_IS_LEADER = 26;
    private static final int DATA_SELECTED_RESIDENT_PERMISSION_MASK_LOW = 27;
    private static final int DATA_SELECTED_RESIDENT_PERMISSION_MASK_HIGH = 28;
    private static final int DATA_SELECTED_RESIDENT_PERSONAL_TAX_LOW = 29;
    private static final int DATA_SELECTED_RESIDENT_PERSONAL_TAX_HIGH = 30;
    private static final int DATA_SELECTED_RESIDENT_DEBT_LOW = 31;
    private static final int DATA_SELECTED_RESIDENT_DEBT_HIGH = 32;
    private static final int DATA_SELECTED_RESIDENT_SHOP_TAX = 33;
    private static final int DATA_CAN_EDIT_SELECTED_RESIDENT_PERMISSIONS = 34;
    private static final int DATA_CAN_EDIT_SELECTED_RESIDENT_PERSONAL_TAX = 35;
    private static final int DATA_CAN_EDIT_SELECTED_RESIDENT_SHOP_TAX = 36;
    private static final int DATA_CAN_STOP_RECONSTRUCTION = 37;
    private static final int DATA_CAN_ACCESS_RESIDENTS_TAB = 38;
    private static final int DATA_CAN_VIEW_RESIDENT_PERMISSION_PAGE = 39;
    private static final int DATA_CAN_VIEW_TREASURY = 40;
    private static final int DATA_CAN_VIEW_SETTLEMENT_DEBT = 41;
    private static final int DATA_CAN_VIEW_SELECTED_RESIDENT_DEBT = 42;
    private static final int DATA_COUNT = 43;

    private final UUID settlementId;
    private final String settlementName;
    private final String leaderName;
    private final List<SettlementResidentView> residentViews;
    private final List<SettlementWarView> warViews;
    private final List<SettlementReconstructionEntryView> reconstructionViews;
    private final ContainerData menuData;

    public SettlementMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                OpenData.read(buf),
                createClientData()
        );
    }

    public SettlementMenu(int containerId, Inventory playerInventory, UUID settlementId) {
        this(containerId, playerInventory, createServerOpenData(playerInventory, settlementId));
    }

    private SettlementMenu(int containerId, Inventory playerInventory, OpenData openData) {
        this(
                containerId,
                playerInventory,
                openData,
                createServerData(
                        playerInventory,
                        openData,
                        SettlementMenuTab.OVERVIEW.getIndex(),
                        0,
                        0,
                        0,
                        0
                )
        );
    }

    private SettlementMenu(
            int containerId,
            Inventory playerInventory,
            OpenData openData,
            ContainerData menuData
    ) {
        super(ModMenuTypes.SETTLEMENT_MENU.get(), containerId);
        this.settlementId = openData.settlementId;
        this.settlementName = openData.settlementName;
        this.leaderName = openData.leaderName;
        this.residentViews = new ArrayList<SettlementResidentView>(openData.residentViews);
        this.warViews = new ArrayList<SettlementWarView>(openData.warViews);
        this.reconstructionViews = new ArrayList<SettlementReconstructionEntryView>(openData.reconstructionViews);
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
    }

    private static OpenData createServerOpenData(Inventory playerInventory, UUID settlementId) {
        return buildOpenData(playerInventory, settlementId);
    }

    public static void writeOpenData(FriendlyByteBuf buf, ServerPlayer player, UUID settlementId) {
        OpenData openData = buildOpenData(player.getInventory(), settlementId);
        openData.write(buf);
    }

    private static OpenData buildOpenData(Inventory playerInventory, UUID settlementId) {
        if (!(playerInventory.player instanceof ServerPlayer)) {
            return OpenData.empty(settlementId);
        }

        ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            return OpenData.empty(settlementId);
        }

        String settlementName = settlement.getName();
        String leaderName = resolvePlayerName(serverPlayer, settlement.getLeaderUuid());

        List<SettlementResidentView> residentViews = new ArrayList<SettlementResidentView>();
        for (SettlementMember member : getOrderedMembers(serverPlayer, settlement)) {
            String displayName = resolvePlayerName(serverPlayer, member.getPlayerUuid());
            residentViews.add(new SettlementResidentView(
                    displayName,
                    member.getPlayerUuid().toString(),
                    member.isLeader(),
                    member.getPermissionSet().asReadOnlySet().size(),
                    encodePermissionMask(member),
                    member.getPersonalTaxAmount(),
                    member.getPersonalTaxDebt(),
                    member.getShopTaxPercent()
            ));
        }

        List<SettlementWarView> warViews = new ArrayList<SettlementWarView>();
        List<WarRecord> activeWars = data.getActiveWarsForSettlement(settlementId);
        SiegeState defendingSiege = data.getActiveSiegeForDefenderSettlement(settlementId);
        SiegeState attackingSiege = data.getActiveSiegeForAttackerSettlement(settlementId);

        for (WarRecord war : activeWars) {
            UUID otherId = war.getSettlementAId().equals(settlementId)
                    ? war.getSettlementBId()
                    : war.getSettlementAId();

            Settlement otherSettlement = data.getSettlement(otherId);
            String otherName = otherSettlement == null ? otherId.toString() : otherSettlement.getName();
            String detail = "Активная война";

            if (defendingSiege != null && defendingSiege.getWarId().equals(war.getId())) {
                detail = "Сейчас ваше поселение находится в осаде";
            } else if (attackingSiege != null && attackingSiege.getWarId().equals(war.getId())) {
                detail = "Сейчас ваше поселение ведет осаду";
            } else if (!war.getStartReason().isEmpty()) {
                detail = "Причина: " + war.getStartReason();
            }

            detail = detail + " | начало: " + war.getStartedAt();

            warViews.add(new SettlementWarView(
                    "Война с " + otherName,
                    detail
            ));
        }

        List<SettlementReconstructionEntryView> reconstructionViews = new ArrayList<SettlementReconstructionEntryView>();
        ReconstructionSession session = data.getActiveReconstructionForSettlement(settlementId);
        if (session != null) {
            for (int i = 0; i < session.getEntries().size(); i++) {
                ReconstructionBlockEntry entry = session.getEntries().get(i);
                reconstructionViews.add(new SettlementReconstructionEntryView(
                        i + 1,
                        entry.getRequiredItemId(),
                        entry.getRequiredCount(),
                        entry.getPos().toShortString(),
                        entry.getDimensionId().toString(),
                        entry.isSkipped(),
                        entry.isRestored()
                ));
            }
        }

        return new OpenData(
                settlementId,
                settlementName,
                leaderName,
                residentViews,
                warViews,
                reconstructionViews
        );
    }

    private static String resolvePlayerName(ServerPlayer opener, UUID playerUuid) {
        ServerPlayer online = opener.server.getPlayerList().getPlayer(playerUuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }

        return playerUuid.toString();
    }

    private static List<SettlementMember> getOrderedMembers(final ServerPlayer viewer, Settlement settlement) {
        List<SettlementMember> ordered = new ArrayList<SettlementMember>();
        if (settlement == null) {
            return ordered;
        }

        ordered.addAll(settlement.getMembers());
        Collections.sort(ordered, new Comparator<SettlementMember>() {
            @Override
            public int compare(SettlementMember first, SettlementMember second) {
                if (first == second) {
                    return 0;
                }
                if (first == null) {
                    return 1;
                }
                if (second == null) {
                    return -1;
                }
                if (first.isLeader() != second.isLeader()) {
                    return first.isLeader() ? -1 : 1;
                }

                String firstName = resolvePlayerName(viewer, first.getPlayerUuid());
                String secondName = resolvePlayerName(viewer, second.getPlayerUuid());
                int byName = firstName.compareToIgnoreCase(secondName);
                if (byName != 0) {
                    return byName;
                }
                return first.getPlayerUuid().toString().compareTo(second.getPlayerUuid().toString());
            }
        });
        return ordered;
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

    private static ContainerData createClientData() {
        return new SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(
            final Inventory playerInventory,
            final OpenData openData,
            final int initialSelectedTab,
            final int initialResidentPage,
            final int initialWarPage,
            final int initialReconstructionPage,
            final int initialSelectedResidentIndex
    ) {
        return new ContainerData() {
            private int selectedTab = initialSelectedTab;
            private int residentPage = Math.max(0, initialResidentPage);
            private int warPage = Math.max(0, initialWarPage);
            private int reconstructionPage = Math.max(0, initialReconstructionPage);
            private int selectedResidentIndex = Math.max(0, initialSelectedResidentIndex);

            private final UUID settlementId = openData.settlementId;
            private final List<UUID> residentOrder = createResidentOrderFromOpenData();

            private List<UUID> createResidentOrderFromOpenData() {
                List<UUID> result = new ArrayList<UUID>();

                for (SettlementResidentView residentView : openData.residentViews) {
                    if (residentView == null) {
                        continue;
                    }
                    try {
                        result.add(UUID.fromString(residentView.getPlayerUuid()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                return result;
            }

            private Settlement getSettlement() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return null;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getSettlement(settlementId);
            }

            private SettlementMember getSelfMember() {
                Settlement settlement = getSettlement();
                if (settlement == null) {
                    return null;
                }
                return settlement.getMember(playerInventory.player.getUUID());
            }

            private ReconstructionSession getReconstruction() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return null;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getActiveReconstructionForSettlement(settlementId);
            }

            private List<WarRecord> getActiveWars() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return Collections.emptyList();
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getActiveWarsForSettlement(settlementId);
            }

            private SettlementMember getSelectedResident(Settlement settlement) {
                if (settlement == null) {
                    return null;
                }
                if (selectedResidentIndex < 0 || selectedResidentIndex >= residentOrder.size()) {
                    return null;
                }

                UUID residentUuid = residentOrder.get(selectedResidentIndex);
                if (residentUuid == null) {
                    return null;
                }

                return settlement.getMember(residentUuid);
            }

            private boolean canEditSelectedResidentPermissions(Settlement settlement, SettlementMember self, SettlementMember target) {
                if (settlement == null || self == null || target == null || target.isLeader()) {
                    return false;
                }
                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }
                return self.getPermissionSet().has(SettlementPermission.GRANT_PERMISSIONS);
            }

            private boolean canEditSelectedResidentPersonalTax(Settlement settlement, SettlementMember self, SettlementMember target) {
                if (settlement == null || self == null || target == null || target.isLeader()) {
                    return false;
                }
                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }
                return self.getPermissionSet().has(SettlementPermission.CHANGE_PLAYER_TAX);
            }

            private boolean canEditSelectedResidentShopTax(Settlement settlement, SettlementMember self, SettlementMember target) {
                if (settlement == null || self == null || target == null || target.isLeader()) {
                    return false;
                }
                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }
                return self.getPermissionSet().has(SettlementPermission.CHANGE_PLAYER_SHOP_TAX);
            }

            private boolean canStopReconstruction(Settlement settlement, ReconstructionSession reconstruction) {
                return settlement != null
                        && reconstruction != null
                        && settlement.isLeader(playerInventory.player.getUUID());
            }

            private boolean isLeader(Settlement settlement) {
                return settlement != null && settlement.isLeader(playerInventory.player.getUUID());
            }

            private boolean hasNamedPermission(Settlement settlement, SettlementMember member, String permissionName) {
                if (permissionName == null || permissionName.isEmpty()) {
                    return false;
                }
                if (isLeader(settlement)) {
                    return true;
                }
                if (member == null) {
                    return false;
                }
                for (SettlementPermission permission : SettlementPermission.values()) {
                    if (permissionName.equals(permission.name()) && member.getPermissionSet().has(permission)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean hasPermission(Settlement settlement, SettlementMember member, SettlementPermission permission) {
                if (permission == null) {
                    return false;
                }
                if (isLeader(settlement)) {
                    return true;
                }
                return member != null && member.getPermissionSet().has(permission);
            }

            private boolean canAccessResidentsTab(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.VIEW_RESIDENTS)
                        || hasPermission(settlement, self, SettlementPermission.GRANT_PERMISSIONS)
                        || hasPermission(settlement, self, SettlementPermission.CHANGE_PLAYER_TAX)
                        || hasPermission(settlement, self, SettlementPermission.CHANGE_PLAYER_SHOP_TAX)
                        || hasNamedPermission(settlement, self, "VIEW_RESIDENT_PERMISSIONS");
            }

            private boolean canViewResidentPermissionPage(Settlement settlement, SettlementMember self) {
                return hasPermission(settlement, self, SettlementPermission.GRANT_PERMISSIONS)
                        || hasNamedPermission(settlement, self, "VIEW_RESIDENT_PERMISSIONS");
            }

            private boolean canViewTreasuryBalance(Settlement settlement, SettlementMember self) {
                return hasNamedPermission(settlement, self, "VIEW_TREASURY_BALANCE");
            }

            private boolean canViewSettlementDebt(Settlement settlement, SettlementMember self) {
                return hasNamedPermission(settlement, self, "VIEW_SETTLEMENT_DEBT");
            }

            private boolean canViewSelectedResidentDebt(Settlement settlement, SettlementMember self, SettlementMember target) {
                if (target == null) {
                    return false;
                }
                if (isLeader(settlement)) {
                    return true;
                }
                if (playerInventory.player.getUUID().equals(target.getPlayerUuid())) {
                    return true;
                }
                if (canEditSelectedResidentPersonalTax(settlement, self, target)) {
                    return true;
                }
                return hasNamedPermission(settlement, self, "VIEW_PLAYER_DEBTS");
            }

            @Override
            public int get(int index) {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                ReconstructionSession reconstruction = getReconstruction();

                if (index == DATA_SELECTED_TAB) {
                    return selectedTab;
                }
                if (index == DATA_RESIDENT_PAGE) {
                    return residentPage;
                }
                if (index == DATA_WAR_PAGE) {
                    return warPage;
                }
                if (index == DATA_RECONSTRUCTION_PAGE) {
                    return reconstructionPage;
                }
                if (index == DATA_MEMBER_COUNT) {
                    return settlement == null ? 0 : settlement.getMembers().size();
                }
                if (index == DATA_CLAIM_COUNT) {
                    return settlement == null ? 0 : settlement.getClaimedChunkCount();
                }
                if (index == DATA_ALLOWANCE) {
                    return settlement == null ? 0 : settlement.getPurchasedChunkAllowance();
                }
                if (index == DATA_IS_LEADER) {
                    return settlement != null && settlement.isLeader(playerInventory.player.getUUID()) ? 1 : 0;
                }

                long treasury = settlement == null ? 0L : settlement.getTreasuryBalance();
                if (index == DATA_TREASURY_LOW) {
                    return (int) (treasury & 0xFFFFFFFFL);
                }
                if (index == DATA_TREASURY_HIGH) {
                    return (int) ((treasury >>> 32) & 0xFFFFFFFFL);
                }

                long settlementDebt = settlement == null ? 0L : settlement.getSettlementDebt();
                if (index == DATA_SETTLEMENT_DEBT_LOW) {
                    return (int) (settlementDebt & 0xFFFFFFFFL);
                }
                if (index == DATA_SETTLEMENT_DEBT_HIGH) {
                    return (int) ((settlementDebt >>> 32) & 0xFFFFFFFFL);
                }

                long playerDebt = self == null ? 0L : self.getPersonalTaxDebt();
                if (index == DATA_PLAYER_DEBT_LOW) {
                    return (int) (playerDebt & 0xFFFFFFFFL);
                }
                if (index == DATA_PLAYER_DEBT_HIGH) {
                    return (int) ((playerDebt >>> 32) & 0xFFFFFFFFL);
                }

                if (index == DATA_RECON_TOTAL) {
                    return reconstruction == null ? 0 : reconstruction.getEntries().size();
                }
                if (index == DATA_RECON_PENDING) {
                    return reconstruction == null ? 0 : reconstruction.countPendingEntries();
                }
                if (index == DATA_RECON_RESTORED) {
                    return reconstruction == null ? 0 : reconstruction.countRestoredEntries();
                }
                if (index == DATA_RECON_SKIPPED) {
                    return reconstruction == null ? 0 : reconstruction.countSkippedEntries();
                }
                if (index == DATA_HAS_RECONSTRUCTION) {
                    return reconstruction == null ? 0 : 1;
                }
                if (index == DATA_CAN_OPEN_RECON_STORAGE) {
                    return canOpenReconstructionStorage(settlement, self, reconstruction) ? 1 : 0;
                }
                if (index == DATA_CAN_RESTORE_RECON) {
                    return canRestoreReconstruction(settlement, self, reconstruction) ? 1 : 0;
                }
                if (index == DATA_ACTIVE_WAR_COUNT) {
                    return getActiveWars().size();
                }
                if (index == DATA_IS_UNDER_SIEGE) {
                    if (!(playerInventory.player instanceof ServerPlayer)) {
                        return 0;
                    }
                    ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                    return SettlementSavedData.get(serverPlayer.server).getActiveSiegeForDefenderSettlement(settlementId) != null ? 1 : 0;
                }
                if (index == DATA_IS_ATTACKING_SIEGE) {
                    if (!(playerInventory.player instanceof ServerPlayer)) {
                        return 0;
                    }
                    ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                    return SettlementSavedData.get(serverPlayer.server).getActiveSiegeForAttackerSettlement(settlementId) != null ? 1 : 0;
                }

                SettlementMember selectedResident = getSelectedResident(settlement);
                if (index == DATA_SELECTED_RESIDENT_INDEX) {
                    return selectedResidentIndex;
                }
                if (index == DATA_SELECTED_RESIDENT_EXISTS) {
                    return selectedResident == null ? 0 : 1;
                }
                if (index == DATA_SELECTED_RESIDENT_IS_LEADER) {
                    return selectedResident != null && selectedResident.isLeader() ? 1 : 0;
                }

                long selectedPermissionMask = encodePermissionMask(selectedResident);
                if (index == DATA_SELECTED_RESIDENT_PERMISSION_MASK_LOW) {
                    return (int) (selectedPermissionMask & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_RESIDENT_PERMISSION_MASK_HIGH) {
                    return (int) ((selectedPermissionMask >>> 32) & 0xFFFFFFFFL);
                }

                long selectedPersonalTax = selectedResident == null ? 0L : selectedResident.getPersonalTaxAmount();
                if (index == DATA_SELECTED_RESIDENT_PERSONAL_TAX_LOW) {
                    return (int) (selectedPersonalTax & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_RESIDENT_PERSONAL_TAX_HIGH) {
                    return (int) ((selectedPersonalTax >>> 32) & 0xFFFFFFFFL);
                }

                long selectedDebt = selectedResident == null ? 0L : selectedResident.getPersonalTaxDebt();
                if (index == DATA_SELECTED_RESIDENT_DEBT_LOW) {
                    return (int) (selectedDebt & 0xFFFFFFFFL);
                }
                if (index == DATA_SELECTED_RESIDENT_DEBT_HIGH) {
                    return (int) ((selectedDebt >>> 32) & 0xFFFFFFFFL);
                }

                if (index == DATA_SELECTED_RESIDENT_SHOP_TAX) {
                    return selectedResident == null ? 0 : selectedResident.getShopTaxPercent();
                }
                if (index == DATA_CAN_EDIT_SELECTED_RESIDENT_PERMISSIONS) {
                    return canEditSelectedResidentPermissions(settlement, self, selectedResident) ? 1 : 0;
                }
                if (index == DATA_CAN_EDIT_SELECTED_RESIDENT_PERSONAL_TAX) {
                    return canEditSelectedResidentPersonalTax(settlement, self, selectedResident) ? 1 : 0;
                }
                if (index == DATA_CAN_EDIT_SELECTED_RESIDENT_SHOP_TAX) {
                    return canEditSelectedResidentShopTax(settlement, self, selectedResident) ? 1 : 0;
                }
                if (index == DATA_CAN_STOP_RECONSTRUCTION) {
                    return canStopReconstruction(settlement, reconstruction) ? 1 : 0;
                }
                if (index == DATA_CAN_ACCESS_RESIDENTS_TAB) {
                    return canAccessResidentsTab(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_RESIDENT_PERMISSION_PAGE) {
                    return canViewResidentPermissionPage(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_TREASURY) {
                    return canViewTreasuryBalance(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_SETTLEMENT_DEBT) {
                    return canViewSettlementDebt(settlement, self) ? 1 : 0;
                }
                if (index == DATA_CAN_VIEW_SELECTED_RESIDENT_DEBT) {
                    return canViewSelectedResidentDebt(settlement, self, selectedResident) ? 1 : 0;
                }

                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_SELECTED_TAB) {
                    selectedTab = value;
                    return;
                }
                if (index == DATA_RESIDENT_PAGE) {
                    residentPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_WAR_PAGE) {
                    warPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_RECONSTRUCTION_PAGE) {
                    reconstructionPage = Math.max(0, value);
                    return;
                }
                if (index == DATA_SELECTED_RESIDENT_INDEX) {
                    if (residentOrder.isEmpty()) {
                        selectedResidentIndex = 0;
                        return;
                    }

                    int clamped = value;
                    if (clamped < 0) {
                        clamped = 0;
                    }
                    if (clamped >= residentOrder.size()) {
                        clamped = residentOrder.size() - 1;
                    }

                    selectedResidentIndex = clamped;
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }

            private boolean canOpenReconstructionStorage(Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
                if (settlement == null || reconstruction == null || self == null) {
                    return false;
                }

                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }

                return self.getPermissionSet().has(SettlementPermission.OPEN_RECONSTRUCTION_STORAGE)
                        || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
            }

            private boolean canRestoreReconstruction(Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
                if (settlement == null || reconstruction == null || self == null) {
                    return false;
                }

                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }

                return self.getPermissionSet().has(SettlementPermission.ENABLE_RECONSTRUCTION)
                        || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
            }
        };
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 180;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 238;
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, startX + column * 18, hotbarY));
        }
    }

    public UUID getSettlementId() {
        return settlementId;
    }

    public String getSettlementName() {
        return settlementName;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public List<SettlementResidentView> getResidentViews() {
        return Collections.unmodifiableList(residentViews);
    }

    public List<SettlementWarView> getWarViews() {
        return Collections.unmodifiableList(warViews);
    }

    public List<SettlementReconstructionEntryView> getReconstructionViews() {
        return Collections.unmodifiableList(reconstructionViews);
    }

    public int getSelectedTabIndex() {
        return menuData.get(DATA_SELECTED_TAB);
    }

    public SettlementMenuTab getSelectedTab() {
        return SettlementMenuTab.fromIndex(getSelectedTabIndex());
    }

    public int getResidentPage() {
        return menuData.get(DATA_RESIDENT_PAGE);
    }

    public int getWarPage() {
        return menuData.get(DATA_WAR_PAGE);
    }

    public int getReconstructionPage() {
        return menuData.get(DATA_RECONSTRUCTION_PAGE);
    }

    public int getMemberCount() {
        return menuData.get(DATA_MEMBER_COUNT);
    }

    public int getClaimedChunkCount() {
        return menuData.get(DATA_CLAIM_COUNT);
    }

    public int getPurchasedChunkAllowance() {
        return menuData.get(DATA_ALLOWANCE);
    }

    public boolean isLeader() {
        return menuData.get(DATA_IS_LEADER) != 0;
    }

    public long getTreasuryBalance() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_TREASURY_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_TREASURY_HIGH));
        return low | (high << 32);
    }

    public long getSettlementDebt() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SETTLEMENT_DEBT_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SETTLEMENT_DEBT_HIGH));
        return low | (high << 32);
    }

    public long getPlayerDebt() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_PLAYER_DEBT_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_PLAYER_DEBT_HIGH));
        return low | (high << 32);
    }

    public int getReconstructionTotal() {
        return menuData.get(DATA_RECON_TOTAL);
    }

    public int getReconstructionPending() {
        return menuData.get(DATA_RECON_PENDING);
    }

    public int getReconstructionRestored() {
        return menuData.get(DATA_RECON_RESTORED);
    }

    public int getReconstructionSkipped() {
        return menuData.get(DATA_RECON_SKIPPED);
    }

    public boolean hasActiveReconstruction() {
        return menuData.get(DATA_HAS_RECONSTRUCTION) != 0;
    }

    public boolean canOpenReconstructionStorage() {
        return menuData.get(DATA_CAN_OPEN_RECON_STORAGE) != 0;
    }

    public boolean canRestoreReconstruction() {
        return menuData.get(DATA_CAN_RESTORE_RECON) != 0;
    }

    public int getActiveWarCount() {
        return menuData.get(DATA_ACTIVE_WAR_COUNT);
    }

    public boolean isUnderSiege() {
        return menuData.get(DATA_IS_UNDER_SIEGE) != 0;
    }

    public boolean isAttackingSiege() {
        return menuData.get(DATA_IS_ATTACKING_SIEGE) != 0;
    }

    public int getSelectedResidentIndex() {
        return menuData.get(DATA_SELECTED_RESIDENT_INDEX);
    }

    public boolean hasSelectedResident() {
        return menuData.get(DATA_SELECTED_RESIDENT_EXISTS) != 0;
    }

    public boolean isSelectedResidentLeader() {
        return menuData.get(DATA_SELECTED_RESIDENT_IS_LEADER) != 0;
    }

    public long getSelectedResidentPermissionMask() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_RESIDENT_PERMISSION_MASK_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_RESIDENT_PERMISSION_MASK_HIGH));
        return low | (high << 32);
    }

    public boolean selectedResidentHasPermission(SettlementPermission permission) {
        if (permission == null) {
            return false;
        }
        long mask = getSelectedResidentPermissionMask();
        return (mask & (1L << permission.ordinal())) != 0L;
    }

    public long getSelectedResidentPersonalTaxAmount() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_RESIDENT_PERSONAL_TAX_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_RESIDENT_PERSONAL_TAX_HIGH));
        return low | (high << 32);
    }

    public long getSelectedResidentPersonalDebt() {
        long low = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_RESIDENT_DEBT_LOW));
        long high = Integer.toUnsignedLong(menuData.get(DATA_SELECTED_RESIDENT_DEBT_HIGH));
        return low | (high << 32);
    }

    public int getSelectedResidentShopTaxPercent() {
        return menuData.get(DATA_SELECTED_RESIDENT_SHOP_TAX);
    }

    public boolean canEditSelectedResidentPermissions() {
        return menuData.get(DATA_CAN_EDIT_SELECTED_RESIDENT_PERMISSIONS) != 0;
    }

    public boolean canEditSelectedResidentPersonalTax() {
        return menuData.get(DATA_CAN_EDIT_SELECTED_RESIDENT_PERSONAL_TAX) != 0;
    }

    public boolean canEditSelectedResidentShopTax() {
        return menuData.get(DATA_CAN_EDIT_SELECTED_RESIDENT_SHOP_TAX) != 0;
    }

    public boolean canStopReconstruction() {
        return menuData.get(DATA_CAN_STOP_RECONSTRUCTION) != 0;
    }

    public boolean canAccessResidentsTab() {
        return menuData.get(DATA_CAN_ACCESS_RESIDENTS_TAB) != 0;
    }

    public boolean canViewResidentPermissionPage() {
        return menuData.get(DATA_CAN_VIEW_RESIDENT_PERMISSION_PAGE) != 0;
    }

    public boolean canViewTreasuryBalance() {
        return menuData.get(DATA_CAN_VIEW_TREASURY) != 0;
    }

    public boolean canViewSettlementDebt() {
        return menuData.get(DATA_CAN_VIEW_SETTLEMENT_DEBT) != 0;
    }

    public boolean canViewSelectedResidentDebt() {
        return menuData.get(DATA_CAN_VIEW_SELECTED_RESIDENT_DEBT) != 0;
    }

    public SettlementResidentView getResidentViewByIndex(int index) {
        if (index < 0 || index >= residentViews.size()) {
            return null;
        }
        return residentViews.get(index);
    }

    public void clientSelectResident(int index) {
        if (residentViews.isEmpty()) {
            menuData.set(DATA_SELECTED_RESIDENT_INDEX, 0);
            return;
        }

        int clamped = index;
        if (clamped < 0) {
            clamped = 0;
        }
        if (clamped >= residentViews.size()) {
            clamped = residentViews.size() - 1;
        }

        menuData.set(DATA_SELECTED_RESIDENT_INDEX, clamped);
    }

    public void clientSetReconstructionEntrySkipped(int oneBasedIndex, boolean skipped) {
        for (int i = 0; i < reconstructionViews.size(); i++) {
            SettlementReconstructionEntryView view = reconstructionViews.get(i);
            if (view.getIndex() == oneBasedIndex) {
                reconstructionViews.set(i, view.withSkipped(skipped));
                return;
            }
        }
    }

    public void clientMarkReconstructionEntrySkipped(int oneBasedIndex) {
        clientSetReconstructionEntrySkipped(oneBasedIndex, true);
    }

    private SettlementMember resolveSelectedResidentFromViews(Settlement settlement) {
        if (settlement == null) {
            return null;
        }

        int selectedIndex = menuData.get(DATA_SELECTED_RESIDENT_INDEX);
        if (selectedIndex < 0 || selectedIndex >= residentViews.size()) {
            return null;
        }

        SettlementResidentView selectedView = residentViews.get(selectedIndex);
        if (selectedView == null) {
            return null;
        }

        try {
            UUID selectedUuid = UUID.fromString(selectedView.getPlayerUuid());
            return settlement.getMember(selectedUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_TAB_OVERVIEW) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.OVERVIEW.getIndex());
            broadcastChanges();
            return true;
        }
        if (buttonId == BUTTON_TAB_RESIDENTS) {
            if (!canAccessResidentsTab()) {
                return false;
            }
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.RESIDENTS.getIndex());
            broadcastChanges();
            return true;
        }
        if (buttonId == BUTTON_TAB_WAR) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.WAR.getIndex());
            broadcastChanges();
            return true;
        }
        if (buttonId == BUTTON_TAB_RECONSTRUCTION) {
            menuData.set(DATA_SELECTED_TAB, SettlementMenuTab.RECONSTRUCTION.getIndex());
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PAGE_PREV) {
            decrementPage();
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PAGE_NEXT) {
            incrementPage();
            broadcastChanges();
            return true;
        }

        if (buttonId >= BUTTON_SELECT_RESIDENT_BASE && buttonId < BUTTON_TOGGLE_SELECTED_PERMISSION_BASE) {
            if (!canAccessResidentsTab()) {
                return false;
            }
            menuData.set(DATA_SELECTED_RESIDENT_INDEX, buttonId - BUTTON_SELECT_RESIDENT_BASE);
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
            SettlementMember selectedResident = resolveSelectedResidentFromViews(settlement);
            ReconstructionSession reconstruction = data.getActiveReconstructionForSettlement(settlementId);

            if (buttonId >= BUTTON_TOGGLE_SELECTED_PERMISSION_BASE
                    && buttonId < BUTTON_TOGGLE_SELECTED_PERMISSION_BASE + SettlementPermission.values().length) {
                SettlementPermission permission = SettlementPermission.values()[buttonId - BUTTON_TOGGLE_SELECTED_PERMISSION_BASE];
                if (!canEditResidentPermissions(serverPlayer, settlement, self, selectedResident)) {
                    throw new IllegalStateException("Нет права на изменение прав этого жителя.");
                }
                if (selectedResident.getPermissionSet().has(permission)) {
                    selectedResident.getPermissionSet().revoke(permission);
                } else {
                    selectedResident.getPermissionSet().grant(permission);
                }
                data.setDirty();
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                return true;
            }

            if (buttonId == BUTTON_SELECTED_PERSONAL_TAX_MINUS_100
                    || buttonId == BUTTON_SELECTED_PERSONAL_TAX_MINUS_10
                    || buttonId == BUTTON_SELECTED_PERSONAL_TAX_PLUS_10
                    || buttonId == BUTTON_SELECTED_PERSONAL_TAX_PLUS_100) {
                if (!canEditResidentPersonalTax(serverPlayer, settlement, self, selectedResident)) {
                    throw new IllegalStateException("Нет права на изменение личного налога этого жителя.");
                }
                long delta = buttonId == BUTTON_SELECTED_PERSONAL_TAX_MINUS_100 ? -100L
                        : buttonId == BUTTON_SELECTED_PERSONAL_TAX_MINUS_10 ? -10L
                        : buttonId == BUTTON_SELECTED_PERSONAL_TAX_PLUS_10 ? 10L
                        : 100L;
                selectedResident.setPersonalTaxAmount(selectedResident.getPersonalTaxAmount() + delta);
                data.setDirty();
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                return true;
            }

            if (buttonId == BUTTON_SELECTED_SHOP_TAX_MINUS_10
                    || buttonId == BUTTON_SELECTED_SHOP_TAX_MINUS_1
                    || buttonId == BUTTON_SELECTED_SHOP_TAX_PLUS_1
                    || buttonId == BUTTON_SELECTED_SHOP_TAX_PLUS_10) {
                if (!canEditResidentShopTax(serverPlayer, settlement, self, selectedResident)) {
                    throw new IllegalStateException("Нет права на изменение налога магазинов этого жителя.");
                }
                int delta = buttonId == BUTTON_SELECTED_SHOP_TAX_MINUS_10 ? -10
                        : buttonId == BUTTON_SELECTED_SHOP_TAX_MINUS_1 ? -1
                        : buttonId == BUTTON_SELECTED_SHOP_TAX_PLUS_1 ? 1
                        : 10;
                data.setDirty();
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                return true;
            }

            if (buttonId == BUTTON_STOP_RECONSTRUCTION) {
                if (!canForceStopReconstruction(serverPlayer, settlement, reconstruction)) {
                    throw new IllegalStateException("Нет права на принудительную остановку реконструкции.");
                }
                ReconstructionService.stopActive(serverPlayer);
                reopenFor(serverPlayer);
                serverPlayer.displayClientMessage(Component.literal("Реконструкция принудительно остановлена."), true);
                return true;
            }

            if (buttonId >= BUTTON_SKIP_RECON_ENTRY_BASE && buttonId < BUTTON_STOP_RECONSTRUCTION) {
                if (!canToggleReconstructionEntries(serverPlayer, settlement, self, reconstruction)) {
                    throw new IllegalStateException("Нет права изменять список блоков реконструкции.");
                }
                ReconstructionService.skipEntryByIndex(serverPlayer, buttonId - BUTTON_SKIP_RECON_ENTRY_BASE);
                refreshOpenMenusForSettlement(serverPlayer, settlementId);
                return true;
            }

            if (buttonId == BUTTON_OPEN_RECONSTRUCTION_STORAGE) {
                if (!canOpenReconstructionStorage(serverPlayer, settlement, self, reconstruction)) {
                    throw new IllegalStateException("Нет права открывать склад реконструкции.");
                }
                ReconstructionService.openStorage(serverPlayer);
                broadcastChanges();
                return true;
            }

            if (buttonId == BUTTON_RESTORE_RECONSTRUCTION) {
                if (!canRestoreReconstruction(serverPlayer, settlement, self, reconstruction)) {
                    throw new IllegalStateException("Нет права запускать восстановление реконструкции.");
                }
                ReconstructionRestoreResult result = ReconstructionService.restoreAvailable(serverPlayer);
                reopenFor(serverPlayer);
                serverPlayer.displayClientMessage(
                        Component.literal(
                                "Восстановлено: " + result.getRestored()
                                        + ", не хватает ресурсов: " + result.getMissingResources()
                                        + ", нет опоры: " + result.getBlockedBySupport()
                                        + ", позиция занята: " + result.getOccupied()
                                        + ", прочие блокировки: " + result.getOtherBlocked()
                                        + ", осталось ожидать: " + result.getRemainingPending()
                        ),
                        true
                );
                return true;
            }
        } catch (Exception e) {
            serverPlayer.displayClientMessage(Component.literal(messageOrDefault(e, "Ошибка выполнения действия.")), true);
            return false;
        }

        return false;
    }
    private static void refreshOpenMenusForSettlement(ServerPlayer sourcePlayer, UUID settlementId) {
        if (sourcePlayer == null || sourcePlayer.server == null || settlementId == null) {
            return;
        }

        List<ServerPlayer> players = sourcePlayer.server.getPlayerList().getPlayers();
        for (ServerPlayer online : players) {
            if (!(online.containerMenu instanceof SettlementMenu)) {
                continue;
            }

            SettlementMenu openMenu = (SettlementMenu) online.containerMenu;
            if (!settlementId.equals(openMenu.getSettlementId())) {
                continue;
            }

            openMenu.reopenFor(online);
        }
    }
    private static boolean canEditResidentPermissions(ServerPlayer actor, Settlement settlement, SettlementMember self, SettlementMember target) {
        if (settlement == null || self == null || target == null || target.isLeader()) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.GRANT_PERMISSIONS);
    }

    private static boolean canEditResidentPersonalTax(ServerPlayer actor, Settlement settlement, SettlementMember self, SettlementMember target) {
        if (settlement == null || self == null || target == null || target.isLeader()) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.CHANGE_PLAYER_TAX);
    }

    private static boolean canEditResidentShopTax(ServerPlayer actor, Settlement settlement, SettlementMember self, SettlementMember target) {
        if (settlement == null || self == null || target == null || target.isLeader()) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.CHANGE_PLAYER_SHOP_TAX);
    }

    private static boolean canOpenReconstructionStorage(ServerPlayer actor, Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
        if (settlement == null || reconstruction == null || self == null) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.OPEN_RECONSTRUCTION_STORAGE)
                || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
    }

    private static boolean canRestoreReconstruction(ServerPlayer actor, Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
        if (settlement == null || reconstruction == null || self == null) {
            return false;
        }
        if (settlement.isLeader(actor.getUUID())) {
            return true;
        }
        return self.getPermissionSet().has(SettlementPermission.ENABLE_RECONSTRUCTION)
                || self.getPermissionSet().has(SettlementPermission.CONTRIBUTE_RECONSTRUCTION_RESOURCES);
    }

    private static boolean canToggleReconstructionEntries(ServerPlayer actor, Settlement settlement, SettlementMember self, ReconstructionSession reconstruction) {
        return canRestoreReconstruction(actor, settlement, self, reconstruction);
    }

    private static boolean canForceStopReconstruction(ServerPlayer actor, Settlement settlement, ReconstructionSession reconstruction) {
        return settlement != null
                && reconstruction != null
                && settlement.isLeader(actor.getUUID());
    }

    private void reopenFor(final ServerPlayer serverPlayer) {
        final OpenData openData = buildOpenData(serverPlayer.getInventory(), settlementId);
        final int selectedTab = getSelectedTabIndex();
        final int residentPage = getResidentPage();
        final int warPage = getWarPage();
        final int reconstructionPage = getReconstructionPage();
        final int selectedResidentIndex = getSelectedResidentIndex();

        NetworkHooks.openScreen(
                serverPlayer,
                new net.minecraft.world.SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new SettlementMenu(
                                containerId,
                                playerInventory,
                                openData,
                                createServerData(
                                        playerInventory,
                                        openData,
                                        selectedTab,
                                        residentPage,
                                        warPage,
                                        reconstructionPage,
                                        selectedResidentIndex
                                )
                        ),
                        Component.literal(settlementName)
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

    private void decrementPage() {
        SettlementMenuTab tab = getSelectedTab();
        if (tab == SettlementMenuTab.RESIDENTS) {
            menuData.set(DATA_RESIDENT_PAGE, Math.max(0, getResidentPage() - 1));
            return;
        }
        if (tab == SettlementMenuTab.WAR) {
            menuData.set(DATA_WAR_PAGE, Math.max(0, getWarPage() - 1));
            return;
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            menuData.set(DATA_RECONSTRUCTION_PAGE, Math.max(0, getReconstructionPage() - 1));
        }
    }

    private void incrementPage() {
        SettlementMenuTab tab = getSelectedTab();
        if (tab == SettlementMenuTab.RESIDENTS) {
            int maxPage = getMaxPage(residentViews.size());
            menuData.set(DATA_RESIDENT_PAGE, Math.min(maxPage, getResidentPage() + 1));
            return;
        }
        if (tab == SettlementMenuTab.WAR) {
            int maxPage = getMaxPage(warViews.size());
            menuData.set(DATA_WAR_PAGE, Math.min(maxPage, getWarPage() + 1));
            return;
        }
        if (tab == SettlementMenuTab.RECONSTRUCTION) {
            int maxPage = getMaxPage(reconstructionViews.size());
            menuData.set(DATA_RECONSTRUCTION_PAGE, Math.min(maxPage, getReconstructionPage() + 1));
        }
    }

    private int getMaxPage(int size) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / PAGE_SIZE;
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
        return settlement != null && settlement.getId().equals(settlementId);
    }

    private static final class OpenData {
        private final UUID settlementId;
        private final String settlementName;
        private final String leaderName;
        private final List<SettlementResidentView> residentViews;
        private final List<SettlementWarView> warViews;
        private final List<SettlementReconstructionEntryView> reconstructionViews;

        private OpenData(
                UUID settlementId,
                String settlementName,
                String leaderName,
                List<SettlementResidentView> residentViews,
                List<SettlementWarView> warViews,
                List<SettlementReconstructionEntryView> reconstructionViews
        ) {
            this.settlementId = settlementId;
            this.settlementName = settlementName;
            this.leaderName = leaderName;
            this.residentViews = residentViews;
            this.warViews = warViews;
            this.reconstructionViews = reconstructionViews;
        }

        private static OpenData empty(UUID settlementId) {
            return new OpenData(
                    settlementId,
                    "Поселение",
                    "Неизвестно",
                    Collections.<SettlementResidentView>emptyList(),
                    Collections.<SettlementWarView>emptyList(),
                    Collections.<SettlementReconstructionEntryView>emptyList()
            );
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(settlementId);
            buf.writeUtf(settlementName);
            buf.writeUtf(leaderName);

            buf.writeInt(residentViews.size());
            for (SettlementResidentView residentView : residentViews) {
                residentView.write(buf);
            }

            buf.writeInt(warViews.size());
            for (SettlementWarView warView : warViews) {
                warView.write(buf);
            }

            buf.writeInt(reconstructionViews.size());
            for (SettlementReconstructionEntryView reconstructionView : reconstructionViews) {
                reconstructionView.write(buf);
            }
        }

        private static void refreshOpenMenusForSettlement(ServerPlayer sourcePlayer, UUID settlementId) {
            List<ServerPlayer> players = sourcePlayer.server.getPlayerList().getPlayers();
            for (ServerPlayer online : players) {
                if (!(online.containerMenu instanceof SettlementMenu)) {
                    continue;
                }

                SettlementMenu openMenu = (SettlementMenu) online.containerMenu;
                if (!settlementId.equals(openMenu.getSettlementId())) {
                    continue;
                }

                openMenu.reopenFor(online);
            }
        }
        private static OpenData read(FriendlyByteBuf buf) {
            UUID settlementId = buf.readUUID();
            String settlementName = buf.readUtf();
            String leaderName = buf.readUtf();

            int residentSize = buf.readInt();
            List<SettlementResidentView> residentViews = new ArrayList<SettlementResidentView>(residentSize);
            for (int i = 0; i < residentSize; i++) {
                residentViews.add(SettlementResidentView.read(buf));
            }

            int warSize = buf.readInt();
            List<SettlementWarView> warViews = new ArrayList<SettlementWarView>(warSize);
            for (int i = 0; i < warSize; i++) {
                warViews.add(SettlementWarView.read(buf));
            }

            int reconstructionSize = buf.readInt();
            List<SettlementReconstructionEntryView> reconstructionViews = new ArrayList<SettlementReconstructionEntryView>(reconstructionSize);
            for (int i = 0; i < reconstructionSize; i++) {
                reconstructionViews.add(SettlementReconstructionEntryView.read(buf));
            }

            return new OpenData(
                    settlementId,
                    settlementName,
                    leaderName,
                    residentViews,
                    warViews,
                    reconstructionViews
            );
        }
    }
}
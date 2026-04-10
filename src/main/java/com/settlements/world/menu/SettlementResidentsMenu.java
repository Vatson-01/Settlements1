package com.settlements.world.menu;

import com.mojang.authlib.GameProfile;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
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
import java.util.Optional;
import java.util.UUID;

public class SettlementResidentsMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 7;

    public static final int BUTTON_PAGE_PREV = 10;
    public static final int BUTTON_PAGE_NEXT = 11;
    public static final int BUTTON_OPEN_RESIDENT_BASE = 20;

    private static final int DATA_PAGE = 0;
    private static final int DATA_CAN_OPEN = 1;
    private static final int DATA_COUNT = 2;

    private final UUID settlementId;
    private final String settlementName;
    private final List<SettlementResidentListView> residentViews;
    private final ContainerData menuData;

    public SettlementResidentsMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(
                containerId,
                playerInventory,
                OpenData.read(buf),
                createClientData()
        );
    }

    private SettlementResidentsMenu(int containerId, Inventory playerInventory, OpenData openData, ContainerData menuData) {
        super(ModMenuTypes.SETTLEMENT_RESIDENTS_MENU.get(), containerId);
        this.settlementId = openData.settlementId;
        this.settlementName = openData.settlementName;
        this.residentViews = new ArrayList<SettlementResidentListView>(openData.residentViews);
        this.menuData = menuData;

        this.addDataSlots(menuData);
        addPlayerInventorySlots(playerInventory);
    }

    public static void openFor(ServerPlayer serverPlayer, UUID settlementId) {
        if (serverPlayer == null || serverPlayer.server == null || settlementId == null) {
            return;
        }

        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        SettlementMember self = settlement == null ? null : settlement.getMember(serverPlayer.getUUID());

        if (settlement == null) {
            serverPlayer.displayClientMessage(Component.literal("Поселение не найдено."), true);
            return;
        }

        if (!canOpenResidentsMenu(settlement, self, serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.literal("Нет права открывать список жителей."), true);
            return;
        }

        final OpenData openData = buildOpenData(serverPlayer.getInventory(), settlementId);

        NetworkHooks.openScreen(
                serverPlayer,
                new net.minecraft.world.SimpleMenuProvider(
                        (containerId, playerInventory, ignoredPlayer) -> new SettlementResidentsMenu(
                                containerId,
                                playerInventory,
                                openData,
                                createServerData(playerInventory, openData, 0)
                        ),
                        Component.literal("Жители")
                ),
                buf -> openData.write(buf)
        );
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

        List<SettlementResidentListView> residentViews = new ArrayList<SettlementResidentListView>();
        for (SettlementMember member : getOrderedMembers(serverPlayer, settlement)) {
            String displayName = resolvePlayerName(serverPlayer, member.getPlayerUuid());
            residentViews.add(new SettlementResidentListView(
                    displayName,
                    member.getPlayerUuid().toString(),
                    member.isLeader()
            ));
        }

        return new OpenData(
                settlementId,
                settlement.getName(),
                residentViews
        );
    }

    private static ContainerData createClientData() {
        return new SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(final Inventory playerInventory, final OpenData openData, final int initialPage) {
        return new ContainerData() {
            private int page = Math.max(0, initialPage);

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

            @Override
            public int get(int index) {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();

                if (index == DATA_PAGE) {
                    return page;
                }
                if (index == DATA_CAN_OPEN) {
                    return canOpenResidentsMenu(settlement, self, playerInventory.player.getUUID()) ? 1 : 0;
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_PAGE) {
                    int maxPage = getMaxPage(openData.residentViews.size(), PAGE_SIZE);
                    if (value < 0) {
                        page = 0;
                    } else {
                        page = Math.min(value, maxPage);
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

        GameProfileCache cache = opener.server.getProfileCache();
        if (cache != null) {
            Optional<GameProfile> profile = cache.get(playerUuid);
            if (profile.isPresent()) {
                String name = profile.get().getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
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

    private static boolean canOpenResidentsMenu(Settlement settlement, SettlementMember self, UUID actorUuid) {
        return hasPermission(settlement, self, actorUuid, SettlementPermission.VIEW_RESIDENTS)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.GRANT_PERMISSIONS)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.CHANGE_PLAYER_TAX)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.CHANGE_PLAYER_SHOP_TAX)
                || hasPermission(settlement, self, actorUuid, SettlementPermission.VIEW_RESIDENT_PERMISSIONS);
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

    public String getSettlementName() {
        return settlementName;
    }

    public List<SettlementResidentListView> getResidentViews() {
        return Collections.unmodifiableList(residentViews);
    }

    public int getPage() {
        return menuData.get(DATA_PAGE);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PAGE_PREV) {
            menuData.set(DATA_PAGE, Math.max(0, getPage() - 1));
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PAGE_NEXT) {
            int maxPage = getMaxPage(residentViews.size(), PAGE_SIZE);
            menuData.set(DATA_PAGE, Math.min(maxPage, getPage() + 1));
            broadcastChanges();
            return true;
        }

        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        if (buttonId >= BUTTON_OPEN_RESIDENT_BASE && buttonId < BUTTON_OPEN_RESIDENT_BASE + PAGE_SIZE) {
            ServerPlayer serverPlayer = (ServerPlayer) player;

            int row = buttonId - BUTTON_OPEN_RESIDENT_BASE;
            int selectedIndex = getPage() * PAGE_SIZE + row;
            if (selectedIndex < 0 || selectedIndex >= residentViews.size()) {
                return false;
            }

            SettlementResidentListView selectedView = residentViews.get(selectedIndex);
            if (selectedView == null) {
                return false;
            }

            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(selectedView.getPlayerUuid());
            } catch (IllegalArgumentException ignored) {
                return false;
            }

            SettlementResidentManageMenu.openFor(serverPlayer, settlementId, targetUuid);
            return true;
        }

        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static final class OpenData {
        private final UUID settlementId;
        private final String settlementName;
        private final List<SettlementResidentListView> residentViews;

        private OpenData(UUID settlementId, String settlementName, List<SettlementResidentListView> residentViews) {
            this.settlementId = settlementId;
            this.settlementName = settlementName;
            this.residentViews = residentViews;
        }

        private static OpenData empty(UUID settlementId) {
            return new OpenData(settlementId, "Поселение", Collections.<SettlementResidentListView>emptyList());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(settlementId);
            buf.writeUtf(settlementName);
            buf.writeInt(residentViews.size());
            for (SettlementResidentListView view : residentViews) {
                view.write(buf);
            }
        }

        private static OpenData read(FriendlyByteBuf buf) {
            UUID settlementId = buf.readUUID();
            String settlementName = buf.readUtf();
            int size = buf.readInt();

            List<SettlementResidentListView> residentViews = new ArrayList<SettlementResidentListView>(size);
            for (int i = 0; i < size; i++) {
                residentViews.add(SettlementResidentListView.read(buf));
            }

            return new OpenData(settlementId, settlementName, residentViews);
        }
    }
}
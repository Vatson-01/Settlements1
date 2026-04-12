package com.settlements.world.menu;

import com.mojang.authlib.GameProfile;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.PlotPermission;
import com.settlements.data.model.PlotPermissionSet;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementChunkClaim;
import com.settlements.data.model.SettlementMember;
import com.settlements.data.model.SettlementPermission;
import com.settlements.data.model.SettlementPlot;
import com.settlements.registry.ModMenuTypes;
import com.settlements.service.PlotMenuService;
import com.settlements.service.PlotService;
import com.settlements.service.SettlementMenuService;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlotMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 7;

    public static final int BUTTON_PAGE_PREV = 10;
    public static final int BUTTON_PAGE_NEXT = 11;
    public static final int BUTTON_SELECT_PLAYER_BASE = 100;
    public static final int BUTTON_ASSIGN_SELECTED = 200;
    public static final int BUTTON_UNASSIGN = 201;
    public static final int BUTTON_TOGGLE_BUILD = 210;
    public static final int BUTTON_TOGGLE_BREAK = 211;
    public static final int BUTTON_TOGGLE_OPEN_DOORS = 212;
    public static final int BUTTON_TOGGLE_USE_REDSTONE = 213;
    public static final int BUTTON_TOGGLE_OPEN_CONTAINERS = 214;
    public static final int BUTTON_BACK_TO_SETTLEMENT = 220;

    private static final int DATA_PAGE = 0;
    private static final int DATA_SELECTED_INDEX = 1;
    private static final int DATA_CAN_ASSIGN = 2;
    private static final int DATA_CAN_UNASSIGN = 3;
    private static final int DATA_CAN_EDIT_PERMISSIONS = 4;
    private static final int DATA_HAS_CLAIM = 5;
    private static final int DATA_HAS_PLOT = 6;
    private static final int DATA_COUNT = 7;

    private final UUID settlementId;
    private final String settlementName;
    private final String dimensionId;
    private final int chunkX;
    private final int chunkZ;
    private final boolean hasClaim;
    private final boolean hasPlot;
    private final String ownerName;
    private final int plotChunkCount;
    private final List<PlotPlayerView> playerViews;
    private final ContainerData menuData;

    public PlotMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, OpenData.read(buf), createClientData());
    }

    public PlotMenu(
            int containerId,
            Inventory playerInventory,
            UUID settlementId,
            ResourceKey<Level> dimension,
            ChunkPos chunkPos,
            int initialPage,
            int initialSelectedIndex
    ) {
        this(
                containerId,
                playerInventory,
                buildOpenData(playerInventory, settlementId, dimension, chunkPos, initialPage, initialSelectedIndex),
                createServerData(
                        playerInventory,
                        buildOpenData(playerInventory, settlementId, dimension, chunkPos, initialPage, initialSelectedIndex)
                )
        );
    }

    private PlotMenu(int containerId, Inventory playerInventory, OpenData openData, ContainerData menuData) {
        super(ModMenuTypes.PLOT_MENU.get(), containerId);
        this.settlementId = openData.settlementId;
        this.settlementName = openData.settlementName;
        this.dimensionId = openData.dimensionId;
        this.chunkX = openData.chunkX;
        this.chunkZ = openData.chunkZ;
        this.hasClaim = openData.hasClaim;
        this.hasPlot = openData.hasPlot;
        this.ownerName = openData.ownerName;
        this.plotChunkCount = openData.plotChunkCount;
        this.playerViews = new ArrayList<PlotPlayerView>(openData.playerViews);
        this.menuData = menuData;

        this.addDataSlots(menuData);
        this.menuData.set(DATA_PAGE, openData.initialPage);
        this.menuData.set(DATA_SELECTED_INDEX, openData.initialSelectedIndex);
        addPlayerInventorySlots(playerInventory);
    }

    public static void writeOpenData(
            FriendlyByteBuf buf,
            ServerPlayer player,
            UUID settlementId,
            ResourceKey<Level> dimension,
            ChunkPos chunkPos,
            int initialPage,
            int initialSelectedIndex
    ) {
        buildOpenData(player.getInventory(), settlementId, dimension, chunkPos, initialPage, initialSelectedIndex).write(buf);
    }

    private static OpenData buildOpenData(
            Inventory playerInventory,
            UUID settlementId,
            ResourceKey<Level> dimension,
            ChunkPos chunkPos,
            int initialPage,
            int initialSelectedIndex
    ) {
        if (!(playerInventory.player instanceof ServerPlayer) || settlementId == null || dimension == null || chunkPos == null) {
            return OpenData.empty(settlementId, dimension, chunkPos, initialPage, initialSelectedIndex);
        }

        ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
        SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
        Settlement settlement = data.getSettlement(settlementId);
        if (settlement == null) {
            return OpenData.empty(settlementId, dimension, chunkPos, initialPage, initialSelectedIndex);
        }

        SettlementChunkClaim claim = data.getClaim(dimension, chunkPos);
        Settlement territorySettlement = claim == null ? null : data.getSettlement(claim.getSettlementId());
        boolean hasClaim = territorySettlement != null && territorySettlement.getId().equals(settlementId);

        SettlementPlot plot = data.getPlotByChunkKey(toChunkKey(dimension, chunkPos));
        boolean hasPlot = plot != null && plot.getSettlementId().equals(settlementId);
        String ownerName = hasPlot ? resolvePlayerName(serverPlayer, plot.getOwnerUuid()) : "—";
        int plotChunkCount = hasPlot ? plot.getChunkKeys().size() : 0;

        List<PlotPlayerView> playerViews = new ArrayList<PlotPlayerView>();
        Set<UUID> included = new LinkedHashSet<UUID>();

        for (SettlementMember member : getOrderedMembers(serverPlayer, settlement, plot)) {
            UUID playerUuid = member.getPlayerUuid();
            included.add(playerUuid);
            PlotPermissionSet permissionSet = hasPlot ? plot.getAccessByPlayer().get(playerUuid) : null;
            playerViews.add(toPlayerView(serverPlayer, playerUuid, plot, permissionSet));
        }

        if (hasPlot) {
            for (UUID playerUuid : plot.getAccessByPlayer().keySet()) {
                if (included.contains(playerUuid)) {
                    continue;
                }
                PlotPermissionSet permissionSet = plot.getAccessByPlayer().get(playerUuid);
                playerViews.add(toPlayerView(serverPlayer, playerUuid, plot, permissionSet));
            }
        }

        int clampedSelectedIndex = chooseInitialSelectedIndex(initialSelectedIndex, playerViews);

        return new OpenData(
                settlementId,
                settlement.getName(),
                dimension.location().toString(),
                chunkPos.x,
                chunkPos.z,
                hasClaim,
                hasPlot,
                ownerName,
                plotChunkCount,
                Math.max(0, initialPage),
                clampedSelectedIndex,
                playerViews
        );
    }

    private static PlotPlayerView toPlayerView(ServerPlayer viewer, UUID playerUuid, SettlementPlot plot, PlotPermissionSet permissionSet) {
        return new PlotPlayerView(
                playerUuid,
                resolvePlayerName(viewer, playerUuid),
                plot != null && plot.isOwner(playerUuid),
                permissionSet != null && permissionSet.has(PlotPermission.BUILD),
                permissionSet != null && permissionSet.has(PlotPermission.BREAK),
                permissionSet != null && permissionSet.has(PlotPermission.OPEN_DOORS),
                permissionSet != null && permissionSet.has(PlotPermission.USE_REDSTONE),
                permissionSet != null && permissionSet.has(PlotPermission.OPEN_CONTAINERS)
        );
    }

    private static ContainerData createClientData() {
        return new SimpleContainerData(DATA_COUNT);
    }

    private static ContainerData createServerData(final Inventory playerInventory, final OpenData openData) {
        return new ContainerData() {
            private int page = openData.initialPage;
            private int selectedIndex = openData.initialSelectedIndex;

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

            private ResourceKey<Level> getDimensionKey() {
                return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(openData.dimensionId));
            }

            private ChunkPos getChunkPos() {
                return new ChunkPos(openData.chunkX, openData.chunkZ);
            }

            private boolean hasSettlementPermission(Settlement settlement, SettlementMember self, SettlementPermission permission) {
                if (settlement == null || permission == null) {
                    return false;
                }
                if (playerInventory.player instanceof ServerPlayer
                        && ((ServerPlayer) playerInventory.player).hasPermissions(2)) {
                    return true;
                }
                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }
                return self != null && self.getPermissionSet().has(permission);
            }

            private SettlementPlot getCurrentPlot() {
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return null;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                return SettlementSavedData.get(serverPlayer.server).getPlotByChunkKey(toChunkKey(getDimensionKey(), getChunkPos()));
            }

            private boolean canAssign() {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                if (!hasSettlementPermission(settlement, self, SettlementPermission.ASSIGN_PERSONAL_PLOTS)) {
                    return false;
                }
                if (!(playerInventory.player instanceof ServerPlayer)) {
                    return false;
                }
                ServerPlayer serverPlayer = (ServerPlayer) playerInventory.player;
                SettlementSavedData data = SettlementSavedData.get(serverPlayer.server);
                SettlementChunkClaim claim = data.getClaim(getDimensionKey(), getChunkPos());
                Settlement territorySettlement = claim == null ? null : data.getSettlement(claim.getSettlementId());
                return territorySettlement != null
                        && territorySettlement.getId().equals(openData.settlementId)
                        && !openData.playerViews.isEmpty();
            }

            private boolean canUnassign() {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                SettlementPlot plot = getCurrentPlot();
                return plot != null
                        && plot.getSettlementId().equals(openData.settlementId)
                        && hasSettlementPermission(settlement, self, SettlementPermission.ASSIGN_PUBLIC_PLOTS);
            }

            private boolean canEditPermissions() {
                Settlement settlement = getSettlement();
                SettlementMember self = getSelfMember();
                SettlementPlot plot = getCurrentPlot();
                if (settlement == null || plot == null || !plot.getSettlementId().equals(openData.settlementId)) {
                    return false;
                }

                if (playerInventory.player instanceof ServerPlayer
                        && ((ServerPlayer) playerInventory.player).hasPermissions(2)) {
                    return true;
                }
                if (settlement.isLeader(playerInventory.player.getUUID())) {
                    return true;
                }
                if (plot.isOwner(playerInventory.player.getUUID())) {
                    return true;
                }
                return self != null && self.getPermissionSet().has(SettlementPermission.ASSIGN_PERSONAL_PLOTS);
            }

            @Override
            public int get(int index) {
                if (index == DATA_PAGE) {
                    return page;
                }
                if (index == DATA_SELECTED_INDEX) {
                    return selectedIndex;
                }
                if (index == DATA_CAN_ASSIGN) {
                    return canAssign() ? 1 : 0;
                }
                if (index == DATA_CAN_UNASSIGN) {
                    return canUnassign() ? 1 : 0;
                }
                if (index == DATA_CAN_EDIT_PERMISSIONS) {
                    return canEditPermissions() ? 1 : 0;
                }
                if (index == DATA_HAS_CLAIM) {
                    return openData.hasClaim ? 1 : 0;
                }
                if (index == DATA_HAS_PLOT) {
                    return openData.hasPlot ? 1 : 0;
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == DATA_PAGE) {
                    page = clampPage(value, openData.playerViews.size());
                    return;
                }
                if (index == DATA_SELECTED_INDEX) {
                    selectedIndex = clampSelectedIndex(value, openData.playerViews.size());
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static String toChunkKey(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return dimension.location().toString() + "|" + chunkPos.x + "|" + chunkPos.z;
    }

    private static int clampPage(int value, int size) {
        int maxPage = getMaxPage(size, PAGE_SIZE);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, maxPage);
    }

    private static int clampSelectedIndex(int value, int size) {
        if (size <= 0) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        if (value >= size) {
            return size - 1;
        }
        return value;
    }

    private static int chooseInitialSelectedIndex(int requestedIndex, List<PlotPlayerView> playerViews) {
        if (playerViews == null || playerViews.isEmpty()) {
            return 0;
        }
        if (requestedIndex >= 0 && requestedIndex < playerViews.size()) {
            return requestedIndex;
        }
        for (int i = 0; i < playerViews.size(); i++) {
            PlotPlayerView view = playerViews.get(i);
            if (view != null && !view.isOwner()) {
                return i;
            }
        }
        return 0;
    }

    private static int getMaxPage(int size, int pageSize) {
        if (size <= 0) {
            return 0;
        }
        return (size - 1) / pageSize;
    }

    private static List<SettlementMember> getOrderedMembers(final ServerPlayer viewer, Settlement settlement, final SettlementPlot plot) {
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

                boolean firstOwner = plot != null && plot.isOwner(first.getPlayerUuid());
                boolean secondOwner = plot != null && plot.isOwner(second.getPlayerUuid());
                if (firstOwner != secondOwner) {
                    return firstOwner ? -1 : 1;
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

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 190;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, startX + column * 18, startY + row * 18));
            }
        }

        int hotbarY = 248;
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

    public String getDimensionId() {
        return dimensionId;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public boolean hasClaim() {
        return hasClaim;
    }

    public boolean hasPlot() {
        return hasPlot;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getPlotChunkCount() {
        return plotChunkCount;
    }

    public List<PlotPlayerView> getPlayerViews() {
        return Collections.unmodifiableList(playerViews);
    }

    public int getPage() {
        return menuData.get(DATA_PAGE);
    }

    public int getSelectedIndex() {
        return clampSelectedIndex(menuData.get(DATA_SELECTED_INDEX), playerViews.size());
    }

    public PlotPlayerView getSelectedPlayerView() {
        if (playerViews.isEmpty()) {
            return null;
        }
        int index = getSelectedIndex();
        if (index < 0 || index >= playerViews.size()) {
            return null;
        }
        return playerViews.get(index);
    }

    public boolean canAssign() {
        return menuData.get(DATA_CAN_ASSIGN) != 0;
    }

    public boolean canUnassign() {
        return menuData.get(DATA_CAN_UNASSIGN) != 0;
    }

    public boolean canEditPermissions() {
        return menuData.get(DATA_CAN_EDIT_PERMISSIONS) != 0;
    }

    public boolean hasClaimOnChunk() {
        return menuData.get(DATA_HAS_CLAIM) != 0;
    }

    public boolean hasPlotOnChunk() {
        return menuData.get(DATA_HAS_PLOT) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PAGE_PREV) {
            menuData.set(DATA_PAGE, Math.max(0, getPage() - 1));
            broadcastChanges();
            return true;
        }

        if (buttonId == BUTTON_PAGE_NEXT) {
            menuData.set(DATA_PAGE, clampPage(getPage() + 1, playerViews.size()));
            broadcastChanges();
            return true;
        }

        if (buttonId >= BUTTON_SELECT_PLAYER_BASE && buttonId < BUTTON_SELECT_PLAYER_BASE + PAGE_SIZE) {
            int row = buttonId - BUTTON_SELECT_PLAYER_BASE;
            int globalIndex = getPage() * PAGE_SIZE + row;
            if (globalIndex >= 0 && globalIndex < playerViews.size()) {
                menuData.set(DATA_SELECTED_INDEX, globalIndex);
                broadcastChanges();
                return true;
            }
            return false;
        }

        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

        try {
            if (buttonId == BUTTON_BACK_TO_SETTLEMENT) {
                SettlementMenuService.openMenu(serverPlayer, settlementId);
                return true;
            }

            PlotPlayerView selectedView = getSelectedPlayerView();

            if (buttonId == BUTTON_ASSIGN_SELECTED) {
                if (!canAssign()) {
                    throw new IllegalStateException("Нет права назначать личные участки.");
                }
                if (selectedView == null) {
                    throw new IllegalStateException("Не выбран житель для назначения участка.");
                }

                PlotService.assignChunkToPlayer(serverPlayer, selectedView.getPlayerUuid(), dimensionKey, chunkPos);
                reopenFor(serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal("Чанк назначен игроку: " + selectedView.getPlayerName()));
                return true;
            }

            if (buttonId == BUTTON_UNASSIGN) {
                if (!canUnassign()) {
                    throw new IllegalStateException("Нет права возвращать чанк в общую территорию.");
                }

                PlotService.unassignChunk(serverPlayer, dimensionKey, chunkPos);
                reopenFor(serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal("Чанк снова стал общей территорией."));
                return true;
            }

            if (buttonId == BUTTON_TOGGLE_BUILD) {
                return togglePermission(serverPlayer, selectedView, PlotPermission.BUILD, dimensionKey, chunkPos);
            }
            if (buttonId == BUTTON_TOGGLE_BREAK) {
                return togglePermission(serverPlayer, selectedView, PlotPermission.BREAK, dimensionKey, chunkPos);
            }
            if (buttonId == BUTTON_TOGGLE_OPEN_DOORS) {
                return togglePermission(serverPlayer, selectedView, PlotPermission.OPEN_DOORS, dimensionKey, chunkPos);
            }
            if (buttonId == BUTTON_TOGGLE_USE_REDSTONE) {
                return togglePermission(serverPlayer, selectedView, PlotPermission.USE_REDSTONE, dimensionKey, chunkPos);
            }
            if (buttonId == BUTTON_TOGGLE_OPEN_CONTAINERS) {
                return togglePermission(serverPlayer, selectedView, PlotPermission.OPEN_CONTAINERS, dimensionKey, chunkPos);
            }
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "Ошибка управления участком.";
            }
            serverPlayer.sendSystemMessage(Component.literal(message));
            return false;
        }

        return false;
    }

    private boolean togglePermission(
            ServerPlayer serverPlayer,
            PlotPlayerView selectedView,
            PlotPermission permission,
            ResourceKey<Level> dimensionKey,
            ChunkPos chunkPos
    ) {
        if (!canEditPermissions()) {
            throw new IllegalStateException("Нет права изменять локальные доступы участка.");
        }
        if (selectedView == null) {
            throw new IllegalStateException("Не выбран игрок.");
        }
        if (!hasPlotOnChunk()) {
            throw new IllegalStateException("На этом чанке нет личного участка.");
        }
        if (selectedView.isOwner()) {
            throw new IllegalStateException("Нельзя менять локальные права владельца участка.");
        }

        if (selectedView.hasPermission(permission)) {
            PlotService.revokePermissionOnPlot(serverPlayer, selectedView.getPlayerUuid(), permission, dimensionKey, chunkPos);
            reopenFor(serverPlayer);
            serverPlayer.sendSystemMessage(Component.literal("Снят доступ " + permission.name() + " у игрока " + selectedView.getPlayerName()));
            return true;
        }

        PlotService.grantPermissionOnPlot(serverPlayer, selectedView.getPlayerUuid(), permission, dimensionKey, chunkPos);
        reopenFor(serverPlayer);
        serverPlayer.sendSystemMessage(Component.literal("Выдан доступ " + permission.name() + " игроку " + selectedView.getPlayerName()));
        return true;
    }

    private void reopenFor(ServerPlayer serverPlayer) {
        PlotMenuService.openMenu(
                serverPlayer,
                settlementId,
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId)),
                new ChunkPos(chunkX, chunkZ),
                getPage(),
                getSelectedIndex()
        );
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
        Settlement settlement = SettlementSavedData.get(serverPlayer.server).getSettlement(settlementId);
        return settlement != null && (serverPlayer.hasPermissions(2) || settlement.isResident(serverPlayer.getUUID()));
    }

    private static final class OpenData {
        private final UUID settlementId;
        private final String settlementName;
        private final String dimensionId;
        private final int chunkX;
        private final int chunkZ;
        private final boolean hasClaim;
        private final boolean hasPlot;
        private final String ownerName;
        private final int plotChunkCount;
        private final int initialPage;
        private final int initialSelectedIndex;
        private final List<PlotPlayerView> playerViews;

        private OpenData(
                UUID settlementId,
                String settlementName,
                String dimensionId,
                int chunkX,
                int chunkZ,
                boolean hasClaim,
                boolean hasPlot,
                String ownerName,
                int plotChunkCount,
                int initialPage,
                int initialSelectedIndex,
                List<PlotPlayerView> playerViews
        ) {
            this.settlementId = settlementId;
            this.settlementName = settlementName;
            this.dimensionId = dimensionId;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.hasClaim = hasClaim;
            this.hasPlot = hasPlot;
            this.ownerName = ownerName;
            this.plotChunkCount = plotChunkCount;
            this.initialPage = Math.max(0, initialPage);
            this.initialSelectedIndex = Math.max(0, initialSelectedIndex);
            this.playerViews = playerViews;
        }

        private static OpenData empty(
                UUID settlementId,
                ResourceKey<Level> dimension,
                ChunkPos chunkPos,
                int initialPage,
                int initialSelectedIndex
        ) {
            return new OpenData(
                    settlementId == null ? UUID.randomUUID() : settlementId,
                    "Поселение",
                    dimension == null ? Level.OVERWORLD.location().toString() : dimension.location().toString(),
                    chunkPos == null ? 0 : chunkPos.x,
                    chunkPos == null ? 0 : chunkPos.z,
                    false,
                    false,
                    "—",
                    0,
                    initialPage,
                    initialSelectedIndex,
                    Collections.<PlotPlayerView>emptyList()
            );
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeUUID(settlementId);
            buf.writeUtf(settlementName);
            buf.writeUtf(dimensionId);
            buf.writeInt(chunkX);
            buf.writeInt(chunkZ);
            buf.writeBoolean(hasClaim);
            buf.writeBoolean(hasPlot);
            buf.writeUtf(ownerName);
            buf.writeInt(plotChunkCount);
            buf.writeInt(initialPage);
            buf.writeInt(initialSelectedIndex);

            buf.writeInt(playerViews.size());
            for (PlotPlayerView playerView : playerViews) {
                playerView.write(buf);
            }
        }

        private static OpenData read(FriendlyByteBuf buf) {
            UUID settlementId = buf.readUUID();
            String settlementName = buf.readUtf();
            String dimensionId = buf.readUtf();
            int chunkX = buf.readInt();
            int chunkZ = buf.readInt();
            boolean hasClaim = buf.readBoolean();
            boolean hasPlot = buf.readBoolean();
            String ownerName = buf.readUtf();
            int plotChunkCount = buf.readInt();
            int initialPage = buf.readInt();
            int initialSelectedIndex = buf.readInt();

            int size = buf.readInt();
            List<PlotPlayerView> playerViews = new ArrayList<PlotPlayerView>(size);
            for (int i = 0; i < size; i++) {
                playerViews.add(PlotPlayerView.read(buf));
            }

            return new OpenData(
                    settlementId,
                    settlementName,
                    dimensionId,
                    chunkX,
                    chunkZ,
                    hasClaim,
                    hasPlot,
                    ownerName,
                    plotChunkCount,
                    initialPage,
                    initialSelectedIndex,
                    playerViews
            );
        }
    }
}

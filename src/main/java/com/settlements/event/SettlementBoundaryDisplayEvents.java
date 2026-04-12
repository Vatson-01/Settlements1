package com.settlements.event;

import com.settlements.SettlementsMod;
import com.settlements.data.SettlementSavedData;
import com.settlements.data.model.Settlement;
import com.settlements.data.model.SettlementPlot;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SettlementBoundaryDisplayEvents {
    private static final String NBT_SHOW_BORDERS = "settlementsShowBorders";
    private static final String NBT_LAST_DIMENSION = "settlementsBoundaryLastDimension";
    private static final String NBT_LAST_CHUNK_X = "settlementsBoundaryLastChunkX";
    private static final String NBT_LAST_CHUNK_Z = "settlementsBoundaryLastChunkZ";
    private static final String NBT_LAST_SETTLEMENT_ID = "settlementsBoundaryLastSettlementId";

    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int RENDER_RADIUS_CHUNKS = 2;

    private static final DustParticleOptions OWN_SETTLEMENT_PARTICLE = new DustParticleOptions(new Vector3f(0.20F, 0.95F, 0.20F), 1.0F);
    private static final DustParticleOptions FOREIGN_SETTLEMENT_PARTICLE = new DustParticleOptions(new Vector3f(1.00F, 0.45F, 0.20F), 1.0F);
    private static final DustParticleOptions PLOT_PARTICLE = new DustParticleOptions(new Vector3f(1.00F, 0.95F, 0.20F), 1.0F);

    private SettlementBoundaryDisplayEvents() {
    }

    public static boolean isBordersEnabled(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(NBT_SHOW_BORDERS);
    }

    public static void setBordersEnabled(ServerPlayer player, boolean enabled) {
        if (player == null) {
            return;
        }
        player.getPersistentData().putBoolean(NBT_SHOW_BORDERS, enabled);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player rawPlayer = event.player;
        if (!(rawPlayer instanceof ServerPlayer)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) rawPlayer;
        if (player.server == null || player.level().isClientSide()) {
            return;
        }

        handleSettlementTransition(player);

        if (isBordersEnabled(player) && player.tickCount % PARTICLE_INTERVAL_TICKS == 0) {
            renderBoundaryParticles(player);
        }
    }

    private static void handleSettlementTransition(ServerPlayer player) {
        ChunkPos currentChunk = new ChunkPos(player.blockPosition());
        String currentDimensionId = player.level().dimension().location().toString();

        net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
        boolean initialized = persistentData.contains(NBT_LAST_DIMENSION)
                && persistentData.contains(NBT_LAST_CHUNK_X)
                && persistentData.contains(NBT_LAST_CHUNK_Z);

        SettlementSavedData data = SettlementSavedData.get(player.server);
        Settlement currentSettlement = data.getSettlementByChunk(player.level(), currentChunk);

        if (!initialized) {
            storeLastBoundaryState(player, currentDimensionId, currentChunk, currentSettlement);
            return;
        }

        String previousDimensionId = persistentData.getString(NBT_LAST_DIMENSION);
        int previousChunkX = persistentData.getInt(NBT_LAST_CHUNK_X);
        int previousChunkZ = persistentData.getInt(NBT_LAST_CHUNK_Z);

        if (currentDimensionId.equals(previousDimensionId)
                && currentChunk.x == previousChunkX
                && currentChunk.z == previousChunkZ) {
            return;
        }

        Settlement previousSettlement = null;
        if (persistentData.hasUUID(NBT_LAST_SETTLEMENT_ID)) {
            previousSettlement = data.getSettlement(persistentData.getUUID(NBT_LAST_SETTLEMENT_ID));
        }

        if (!sameSettlement(previousSettlement, currentSettlement)) {
            sendSettlementTransitionTitle(player, previousSettlement, currentSettlement);
        }

        storeLastBoundaryState(player, currentDimensionId, currentChunk, currentSettlement);
    }

    private static void storeLastBoundaryState(ServerPlayer player, String dimensionId, ChunkPos chunkPos, Settlement settlement) {
        net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
        persistentData.putString(NBT_LAST_DIMENSION, dimensionId);
        persistentData.putInt(NBT_LAST_CHUNK_X, chunkPos.x);
        persistentData.putInt(NBT_LAST_CHUNK_Z, chunkPos.z);
        if (settlement == null) {
            persistentData.remove(NBT_LAST_SETTLEMENT_ID);
        } else {
            persistentData.putUUID(NBT_LAST_SETTLEMENT_ID, settlement.getId());
        }
    }

    private static void sendSettlementTransitionTitle(ServerPlayer player, Settlement previousSettlement, Settlement currentSettlement) {
        if (currentSettlement != null) {
            sendTitle(
                    player,
                    Component.literal("Территория поселения"),
                    Component.literal("Вы на территории поселения \"" + currentSettlement.getName() + "\"")
            );
            return;
        }

        if (previousSettlement != null) {
            sendTitle(
                    player,
                    Component.literal("Нейтральная территория"),
                    Component.literal("Вы покинули поселение \"" + previousSettlement.getName() + "\"")
            );
        }
    }

    private static void sendTitle(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 45, 12));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    private static void renderBoundaryParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        SettlementSavedData data = SettlementSavedData.get(player.server);
        ChunkPos center = new ChunkPos(player.blockPosition());
        Settlement viewerSettlement = data.getSettlementByPlayer(player.getUUID());
        boolean admin = player.hasPermissions(2);

        for (int chunkX = center.x - RENDER_RADIUS_CHUNKS; chunkX <= center.x + RENDER_RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = center.z - RENDER_RADIUS_CHUNKS; chunkZ <= center.z + RENDER_RADIUS_CHUNKS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                Settlement settlement = data.getSettlementByChunk(level, chunkPos);
                if (settlement == null) {
                    continue;
                }

                renderSettlementEdges(player, level, data, chunkPos, settlement, viewerSettlement);
                if (canViewPlotBoundaries(player, settlement, admin)) {
                    renderPlotEdges(player, level, data, chunkPos, settlement);
                }
            }
        }
    }

    private static boolean canViewPlotBoundaries(ServerPlayer player, Settlement settlement, boolean admin) {
        return admin || (settlement != null && settlement.isResident(player.getUUID()));
    }

    private static void renderSettlementEdges(ServerPlayer player, ServerLevel level, SettlementSavedData data, ChunkPos chunkPos, Settlement settlement, Settlement viewerSettlement) {
        DustParticleOptions particle = viewerSettlement != null && viewerSettlement.getId().equals(settlement.getId())
                ? OWN_SETTLEMENT_PARTICLE
                : FOREIGN_SETTLEMENT_PARTICLE;

        if (!sameSettlement(settlement, data.getSettlementByChunk(level, new ChunkPos(chunkPos.x, chunkPos.z - 1)))) {
            renderEdge(player, level, chunkPos, Direction.NORTH, particle);
        }
        if (!sameSettlement(settlement, data.getSettlementByChunk(level, new ChunkPos(chunkPos.x, chunkPos.z + 1)))) {
            renderEdge(player, level, chunkPos, Direction.SOUTH, particle);
        }
        if (!sameSettlement(settlement, data.getSettlementByChunk(level, new ChunkPos(chunkPos.x - 1, chunkPos.z)))) {
            renderEdge(player, level, chunkPos, Direction.WEST, particle);
        }
        if (!sameSettlement(settlement, data.getSettlementByChunk(level, new ChunkPos(chunkPos.x + 1, chunkPos.z)))) {
            renderEdge(player, level, chunkPos, Direction.EAST, particle);
        }
    }

    private static void renderPlotEdges(ServerPlayer player, ServerLevel level, SettlementSavedData data, ChunkPos chunkPos, Settlement settlement) {
        SettlementPlot currentPlot = data.getPlotByChunk(level, chunkPos);
        if (currentPlot == null) {
            return;
        }

        renderPlotEdgeIfNeeded(player, level, data, chunkPos, settlement, currentPlot, Direction.NORTH);
        renderPlotEdgeIfNeeded(player, level, data, chunkPos, settlement, currentPlot, Direction.SOUTH);
        renderPlotEdgeIfNeeded(player, level, data, chunkPos, settlement, currentPlot, Direction.WEST);
        renderPlotEdgeIfNeeded(player, level, data, chunkPos, settlement, currentPlot, Direction.EAST);
    }

    private static void renderPlotEdgeIfNeeded(ServerPlayer player, ServerLevel level, SettlementSavedData data, ChunkPos chunkPos, Settlement settlement, SettlementPlot currentPlot, Direction direction) {
        ChunkPos neighborChunk = neighbor(chunkPos, direction);
        Settlement neighborSettlement = data.getSettlementByChunk(level, neighborChunk);
        if (!sameSettlement(settlement, neighborSettlement)) {
            return;
        }

        SettlementPlot neighborPlot = data.getPlotByChunk(level, neighborChunk);
        if (!shouldRenderPlotBoundary(currentPlot, neighborPlot)) {
            return;
        }

        renderEdge(player, level, chunkPos, direction, PLOT_PARTICLE);
    }

    private static boolean shouldRenderPlotBoundary(SettlementPlot currentPlot, SettlementPlot neighborPlot) {
        if (currentPlot == null) {
            return false;
        }
        if (neighborPlot == null) {
            return true;
        }
        if (currentPlot.getId().equals(neighborPlot.getId())) {
            return false;
        }
        return currentPlot.getId().toString().compareTo(neighborPlot.getId().toString()) < 0;
    }

    private static ChunkPos neighbor(ChunkPos chunkPos, Direction direction) {
        switch (direction) {
            case NORTH:
                return new ChunkPos(chunkPos.x, chunkPos.z - 1);
            case SOUTH:
                return new ChunkPos(chunkPos.x, chunkPos.z + 1);
            case WEST:
                return new ChunkPos(chunkPos.x - 1, chunkPos.z);
            case EAST:
                return new ChunkPos(chunkPos.x + 1, chunkPos.z);
            default:
                return chunkPos;
        }
    }

    private static boolean sameSettlement(Settlement first, Settlement second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getId().equals(second.getId());
    }

    private static void renderEdge(ServerPlayer player, ServerLevel level, ChunkPos chunkPos, Direction direction, DustParticleOptions particle) {
        double baseY = player.getY() + 0.20D;
        double upperY = baseY + 1.40D;
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        for (int offset = 0; offset <= 16; offset += 2) {
            double x;
            double z;
            switch (direction) {
                case NORTH:
                    x = minX + offset + 0.50D;
                    z = minZ + 0.05D;
                    break;
                case SOUTH:
                    x = minX + offset + 0.50D;
                    z = minZ + 16.95D;
                    break;
                case WEST:
                    x = minX + 0.05D;
                    z = minZ + offset + 0.50D;
                    break;
                case EAST:
                    x = minX + 16.95D;
                    z = minZ + offset + 0.50D;
                    break;
                default:
                    continue;
            }

            sendParticle(player, level, particle, x, baseY, z);
            sendParticle(player, level, particle, x, upperY, z);
        }
    }

    private static void sendParticle(ServerPlayer player, ServerLevel level, DustParticleOptions particle, double x, double y, double z) {
        level.sendParticles(player, particle, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}

package dev.toma.waystones.network;

import dev.toma.gunsrpg.api.common.data.IPlayerData;
import dev.toma.gunsrpg.api.common.data.IPointProvider;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.network.AbstractNetworkPacket;
import dev.toma.waystones.WaystoneProperties;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import dev.toma.waystones.common.world.WorldWaystones;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

public class C2S_RequestWaystoneTeleport extends AbstractNetworkPacket<C2S_RequestWaystoneTeleport> {

    private BlockPos startDest;
    private BlockPos targetDest;

    public C2S_RequestWaystoneTeleport() {
    }

    public C2S_RequestWaystoneTeleport(BlockPos startDest, BlockPos targetDest) {
        this.startDest = startDest;
        this.targetDest = targetDest;
    }

    @Override
    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(startDest);
        packetBuffer.writeBlockPos(targetDest);
    }

    @Override
    public C2S_RequestWaystoneTeleport decode(PacketBuffer packetBuffer) {
        return new C2S_RequestWaystoneTeleport(
                packetBuffer.readBlockPos(),
                packetBuffer.readBlockPos()
        );
    }

    @Override
    protected void handlePacket(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        player.getLevel().getCapability(WaystoneCapabilityProvider.CAPABILITY).ifPresent(provider -> {
            IPlayerData data = PlayerData.getUnsafe(player);
            IPointProvider perkProvider = data.getPerkProvider();
            WaystoneProperties startProps = provider.getWaystoneData(startDest);
            WaystoneProperties endProps = provider.getWaystoneData(targetDest);
            if (startProps == null || endProps == null) {
                return;
            }
            int distance = (int) Math.sqrt(startDest.distSqr(targetDest));
            int price = WorldWaystones.getPriceForDistance(distance);
            if (price <= perkProvider.getPoints()) {
                ServerWorld world = player.getLevel();
                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundCategory.MASTER, 1.0F, 1.0F);
                world.playSound(null, targetDest.getX(), targetDest.getY(), targetDest.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundCategory.MASTER, 1.0F, 1.0F);
                player.teleportTo(targetDest.getX() + 0.5, targetDest.getY() + 1.5, targetDest.getZ());
                if (price > 0) {
                    perkProvider.addPoints(-price);
                }
            }
        });
    }
}

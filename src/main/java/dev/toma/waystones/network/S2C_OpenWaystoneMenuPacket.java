package dev.toma.waystones.network;

import dev.toma.gunsrpg.network.AbstractNetworkPacket;
import dev.toma.waystones.client.WaystonesScreen;
import dev.toma.waystones.common.world.IWaystoneProvider;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

public class S2C_OpenWaystoneMenuPacket extends AbstractNetworkPacket<S2C_OpenWaystoneMenuPacket> {

    private BlockPos pos;
    private CompoundNBT waystoneNbt;

    public S2C_OpenWaystoneMenuPacket() {}

    public S2C_OpenWaystoneMenuPacket(BlockPos pos, CompoundNBT waystoneNbt) {
        this.pos = pos;
        this.waystoneNbt = waystoneNbt;
    }

    @Override
    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(pos);
        packetBuffer.writeNbt(waystoneNbt);
    }

    @Override
    public S2C_OpenWaystoneMenuPacket decode(PacketBuffer packetBuffer) {
        return new S2C_OpenWaystoneMenuPacket(packetBuffer.readBlockPos(), packetBuffer.readNbt());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void handlePacket(NetworkEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        World world = minecraft.level;
        IWaystoneProvider provider = world.getCapability(WaystoneCapabilityProvider.CAPABILITY).orElse(null);
        provider.deserializeNBT(waystoneNbt);
        boolean existsAt = provider.getWaystoneData(pos) != null;
        Screen screen = new WaystonesScreen(pos, !existsAt);
        minecraft.setScreen(screen);
    }
}

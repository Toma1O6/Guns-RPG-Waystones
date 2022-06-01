package dev.toma.waystones.network;

import dev.toma.gunsrpg.network.AbstractNetworkPacket;
import dev.toma.waystones.WaystoneProperties;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

public class C2S_ModifyWaystoneProperties extends AbstractNetworkPacket<C2S_ModifyWaystoneProperties> {

    private BlockPos pos;
    private String text;
    private TextFormatting formatting;

    public C2S_ModifyWaystoneProperties() {
    }

    public C2S_ModifyWaystoneProperties(BlockPos pos, String text, TextFormatting formatting) {
        this.pos = pos;
        this.text = text;
        this.formatting = formatting;
    }

    @Override
    public void encode(PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(text);
        buffer.writeEnum(formatting);
    }

    @Override
    public C2S_ModifyWaystoneProperties decode(PacketBuffer buffer) {
        return new C2S_ModifyWaystoneProperties(
                buffer.readBlockPos(),
                buffer.readUtf(),
                buffer.readEnum(TextFormatting.class)
        );
    }

    @Override
    protected void handlePacket(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        ServerWorld world = player.getLevel();
        if (world.isLoaded(pos)) {
            world.getCapability(WaystoneCapabilityProvider.CAPABILITY).ifPresent(provider -> {
                WaystoneProperties properties = provider.getWaystoneData(pos);
                if (properties != null && properties.isOwner(player)) {
                    properties.updateText(text, formatting);
                }
            });
        }
    }
}

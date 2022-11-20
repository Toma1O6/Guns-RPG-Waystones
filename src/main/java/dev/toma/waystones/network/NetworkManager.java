package dev.toma.waystones.network;

import dev.toma.gunsrpg.api.common.INetworkPacket;
import dev.toma.waystones.Waystones;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

public class NetworkManager {

    private static final String VERSION = "grpgwaystones-1.1.0";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Waystones.MODID, "network"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();
    private static byte ID;

    public static void init() {
        registerNetworkPacket(S2C_OpenWaystoneMenuPacket.class);
        registerNetworkPacket(C2S_BeginWaystoneActivation.class);
        registerNetworkPacket(C2S_ModifyWaystoneProperties.class);
        registerNetworkPacket(C2S_RequestWaystoneTeleport.class);
    }

    public static void sendServerPacket(INetworkPacket<?> packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendWorldPacket(World world, INetworkPacket<?> packet) {
        sendWorldPacket(world, packet, player -> true);
    }

    public static void sendWorldPacket(World world, INetworkPacket<?> packet, Predicate<ServerPlayerEntity> condition) {
        if (!(world instanceof ServerWorld)) {
            throw new UnsupportedOperationException("Cannot send world packet from client!");
        }
        world.players().stream()
                .map(pl -> (ServerPlayerEntity) pl)
                .filter(condition)
                .forEach(serverPlayerEntity -> sendClientPacket(serverPlayerEntity, packet));
    }

    public static void sendClientPacket(ServerPlayerEntity user, INetworkPacket<?> packet) {
        CHANNEL.sendTo(packet, user.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <P extends INetworkPacket<P>> void registerNetworkPacket(Class<P> packetType) {
        P packet;
        try {
            packet = packetType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Couldn't instantiate packet for registration. Make sure you have provided public constructor with no parameters."));
        }
        CHANNEL.registerMessage(ID++, packetType, INetworkPacket::encode, packet::decode, INetworkPacket::handle);
    }
}

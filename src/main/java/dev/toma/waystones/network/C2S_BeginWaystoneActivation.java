package dev.toma.waystones.network;

import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.api.common.data.IQuests;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.quests.QuestSystem;
import dev.toma.gunsrpg.common.quests.quest.QuestScheme;
import dev.toma.gunsrpg.common.quests.quest.QuestStatus;
import dev.toma.gunsrpg.network.AbstractNetworkPacket;
import dev.toma.waystones.common.quest.ActivateWaystoneData;
import dev.toma.waystones.common.quest.ActivateWaystoneQuest;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public class C2S_BeginWaystoneActivation extends AbstractNetworkPacket<C2S_BeginWaystoneActivation> {

    private BlockPos pos;

    public C2S_BeginWaystoneActivation() {
    }

    public C2S_BeginWaystoneActivation(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void encode(PacketBuffer packetBuffer) {
        packetBuffer.writeBlockPos(pos);
    }

    @Override
    public C2S_BeginWaystoneActivation decode(PacketBuffer packetBuffer) {
        return new C2S_BeginWaystoneActivation(
                packetBuffer.readBlockPos()
        );
    }

    @Override
    protected void handlePacket(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        PlayerData.get(player).ifPresent(data -> {
            if (player.level.getCapability(WaystoneCapabilityProvider.CAPABILITY).orElse(null).getWaystoneData(pos) != null) return;
            IQuests questProvider = data.getQuests();
            QuestSystem system = GunsRPG.getModLifecycle().quests();
            QuestScheme<ActivateWaystoneData> scheme = system.getQuestManager().getScheme(ActivateWaystoneQuest.SCHEME_LOCATION);
            if (scheme != null && !questProvider.getActiveQuest().isPresent()) {
                ActivateWaystoneQuest quest = new ActivateWaystoneQuest(scheme, Util.NIL_UUID);
                quest.setStatus(QuestStatus.ACTIVE);
                quest.assign(player);
                quest.setWaystonePosition(pos);
                questProvider.assignQuest(quest);
            }
        });
    }
}

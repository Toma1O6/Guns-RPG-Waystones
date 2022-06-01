package dev.toma.waystones.common.quest;

import dev.toma.gunsrpg.api.common.data.IQuests;
import dev.toma.gunsrpg.client.render.infobar.QuestDisplayDataModel;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.init.ModSounds;
import dev.toma.gunsrpg.common.quests.QuestProperties;
import dev.toma.gunsrpg.common.quests.quest.*;
import dev.toma.gunsrpg.common.quests.quest.area.IAreaQuest;
import dev.toma.gunsrpg.common.quests.quest.area.QuestArea;
import dev.toma.gunsrpg.common.quests.quest.area.QuestAreaScheme;
import dev.toma.gunsrpg.common.quests.trigger.ITriggerHandler;
import dev.toma.gunsrpg.common.quests.trigger.Trigger;
import dev.toma.gunsrpg.common.quests.trigger.TriggerResponseStatus;
import dev.toma.gunsrpg.util.Interval;
import dev.toma.gunsrpg.util.properties.IPropertyReader;
import dev.toma.waystones.Waystones;
import dev.toma.waystones.common.init.ModdedBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraftforge.common.util.Constants;

import java.util.UUID;

public class ActivateWaystoneQuest extends Quest<ActivateWaystoneData> implements IAreaQuest {

    public static final ResourceLocation SCHEME_LOCATION = new ResourceLocation(Waystones.MODID, "activate_waystone");

    public static final IQuestFactory<ActivateWaystoneData, ActivateWaystoneQuest> FACTORY = IQuestFactory.of(ActivateWaystoneQuest::new, ActivateWaystoneQuest::new);
    public static final ITextComponent TEXT_PROTECTION = new TranslationTextComponent("quest.protect_waystone");
    public static final ITextComponent ACTIVATE_SUCCESS = new TranslationTextComponent("quest.activate_waystone.success").withStyle(TextFormatting.GREEN);
    public static final ITextComponent ACTIVATE_FAILURE = new TranslationTextComponent("quest.activate_waystone.fail").withStyle(TextFormatting.RED);

    private int timeLeft;
    private int gracePeriod = 100;
    private boolean wasInArea;
    private QuestArea area;
    private BlockPos waystonePosition;

    public ActivateWaystoneQuest(QuestScheme<ActivateWaystoneData> scheme, UUID traderId) {
        super(scheme, traderId);
        this.timeLeft = this.getActiveData().getTicks();
    }

    public ActivateWaystoneQuest(QuestDeserializationContext<ActivateWaystoneData> context) {
        super(context);
    }

    public void setWaystonePosition(BlockPos waystonePos) {
        this.waystonePosition = waystonePos;
        QuestAreaScheme scheme = this.getActiveData().getAreaScheme();
        this.area = new QuestArea(scheme, waystonePos);
        trySyncClient();
    }

    @Override
    public void registerTriggers(ITriggerRegistration registration) {
        registration.addEntry(Trigger.TICK, this::onTick, this::handleSuccessfulTick);
        registration.addEntry(Trigger.PLAYER_DIED, this::handlePlayerDeath, ITriggerHandler.NONE);
        registration.addEntry(Waystones.BLOCK_DESTROYED, this::onBlockBroken, ITriggerHandler.NONE);
    }

    @Override
    public void tickQuest(PlayerEntity player) {
        if (this.getStatus() == QuestStatus.ACTIVE && area != null && area.isInArea(player)) {
            area.tickArea(player.level, player);
        }
        if (gracePeriod > 0) {
            --gracePeriod;
        }
    }

    @Override
    public QuestArea getQuestArea() {
        return area;
    }

    @Override
    public void onCompleted(PlayerEntity player) {
        if (!player.level.isClientSide) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            serverPlayer.connection.send(new SPlaySoundEffectPacket(SoundEvents.PLAYER_LEVELUP, SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 0.75F, 1.0F));
            serverPlayer.sendMessage(ACTIVATE_SUCCESS, ChatType.GAME_INFO, Util.NIL_UUID);
            PlayerData.get(player).ifPresent(data -> data.getQuests().clearActiveQuest());
            BlockState state = player.level.getBlockState(waystonePosition);
            if (state.getBlock() == ModdedBlocks.WAYSTONE) {
                ModdedBlocks.WAYSTONE.activate(player.level, waystonePosition, state, player);
            }
        }
    }

    @Override
    public void onFailed(PlayerEntity player) {
        if (!player.level.isClientSide) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            serverPlayer.connection.send(new SPlaySoundEffectPacket(ModSounds.USE_AVENGE_ME_FRIENDS, SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 0.75F, 1.0F));
            serverPlayer.sendMessage(ACTIVATE_FAILURE, ChatType.GAME_INFO, Util.NIL_UUID);
            PlayerData.get(player).ifPresent((data) -> {
                IQuests quests = data.getQuests();
                quests.clearActiveQuest();
            });
            player.level.destroyBlock(waystonePosition, false);
        }
    }

    @Override
    protected void writeQuestData(CompoundNBT nbt) {
        nbt.putInt("timeLeft", this.timeLeft);
        nbt.putInt("gracePeriod", this.gracePeriod);
        nbt.putBoolean("wasInArea", this.wasInArea);
        if (this.area != null && this.waystonePosition != null) {
            nbt.put("area", this.area.toNbt());
            nbt.put("pos", NBTUtil.writeBlockPos(this.waystonePosition));
        }
    }

    @Override
    protected void readQuestData(CompoundNBT nbt) {
        this.timeLeft = nbt.getInt("timeLeft");
        this.gracePeriod = nbt.getInt("gracePeriod");
        this.wasInArea = nbt.getBoolean("wasInArea");
        if (nbt.contains("area", Constants.NBT.TAG_COMPOUND)) {
            QuestAreaScheme areaScheme = this.getActiveData().getAreaScheme();
            this.area = QuestArea.fromNbt(areaScheme, nbt.getCompound("area"));
            this.waystonePosition = NBTUtil.readBlockPos(nbt.getCompound("pos"));
        }
    }

    @Override
    protected void fillDataModel(QuestDisplayDataModel model) {
        Interval.IFormatFactory format = f -> f.src(Interval.Unit.TICK).out(Interval.Unit.MINUTE, Interval.Unit.SECOND).compact();
        model.addQuestHeader(this, false);
        model.addInformationRow(TEXT_PROTECTION, this, q -> new StringTextComponent(Interval.format(timeLeft, format)));
        model.addInformationRow(QuestArea.STAY_IN_AREA, this, this::fillAreaInfo);
    }

    private TriggerResponseStatus onTick(Trigger trigger, IPropertyReader reader) {
        if (area == null) return TriggerResponseStatus.PASS;
        PlayerEntity player = reader.getProperty(QuestProperties.PLAYER);
        if (area.isInArea(player)) {
            wasInArea = true;
            return TriggerResponseStatus.OK;
        } else {
            return wasInArea && !isGracePeriod() ? TriggerResponseStatus.FAIL : TriggerResponseStatus.PASS;
        }
    }

    private void handleSuccessfulTick(Trigger trigger, IPropertyReader reader) {
        if (--timeLeft < 0) {
            setStatus(QuestStatus.COMPLETED);
        }
    }

    private TriggerResponseStatus handlePlayerDeath(Trigger trigger, IPropertyReader reader) {
        return TriggerResponseStatus.FAIL;
    }

    private TriggerResponseStatus onBlockBroken(Trigger trigger, IPropertyReader reader) {
        PlayerEntity player = reader.getProperty(QuestProperties.PLAYER);
        BlockState state = reader.getProperty(Waystones.BLOCKSTATE);
        if (state.getBlock() != ModdedBlocks.WAYSTONE) {
            return TriggerResponseStatus.OK;
        }
        BlockState atWaystone = player.level.getBlockState(waystonePosition);
        if (atWaystone.getBlock() != ModdedBlocks.WAYSTONE) {
            return TriggerResponseStatus.FAIL;
        }
        return TriggerResponseStatus.OK;
    }

    private boolean isGracePeriod() {
        return gracePeriod > 0;
    }

    private ITextComponent fillAreaInfo(ActivateWaystoneQuest quest) {
        PlayerEntity player = quest.player;
        QuestArea area = quest.area;
        double distance = area.getDistance(player);
        int intDist = (int)distance;
        String text = intDist + "m";
        int size = area.getScheme().getSize();
        boolean tooClose = size - intDist < 8;
        TextFormatting color = area.isInArea(player) ? (tooClose ? TextFormatting.YELLOW : TextFormatting.GREEN) : TextFormatting.RED;
        return new StringTextComponent(text).withStyle(color);
    }
}

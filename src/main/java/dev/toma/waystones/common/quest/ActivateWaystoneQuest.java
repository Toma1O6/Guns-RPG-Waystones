package dev.toma.waystones.common.quest;

import dev.toma.gunsrpg.client.render.infobar.QuestDisplayDataModel;
import dev.toma.gunsrpg.common.init.ModSounds;
import dev.toma.gunsrpg.common.quests.QuestProperties;
import dev.toma.gunsrpg.common.quests.quest.*;
import dev.toma.gunsrpg.common.quests.quest.area.QuestArea;
import dev.toma.gunsrpg.common.quests.quest.area.QuestAreaScheme;
import dev.toma.gunsrpg.common.quests.sharing.QuestingGroup;
import dev.toma.gunsrpg.common.quests.trigger.ITriggerHandler;
import dev.toma.gunsrpg.common.quests.trigger.Trigger;
import dev.toma.gunsrpg.common.quests.trigger.TriggerResponseStatus;
import dev.toma.gunsrpg.util.Interval;
import dev.toma.gunsrpg.util.properties.IPropertyReader;
import dev.toma.gunsrpg.world.cap.QuestingDataProvider;
import dev.toma.waystones.Waystones;
import dev.toma.waystones.common.init.ModdedBlocks;
import dev.toma.waystones.config.WaystonesConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import java.util.UUID;

public class ActivateWaystoneQuest extends AbstractAreaBasedQuest<ActivateWaystoneData> {

    public static final ResourceLocation SCHEME_LOCATION = new ResourceLocation(Waystones.MODID, "activate_waystone");

    public static final IQuestFactory<ActivateWaystoneData, ActivateWaystoneQuest> FACTORY = IQuestFactory.of(ActivateWaystoneQuest::new, ActivateWaystoneQuest::new);
    public static final ITextComponent TEXT_PROTECTION = new TranslationTextComponent("quest.protect_waystone");
    public static final ITextComponent ACTIVATE_SUCCESS = new TranslationTextComponent("quest.activate_waystone.success").withStyle(TextFormatting.GREEN);
    public static final ITextComponent ACTIVATE_FAILURE = new TranslationTextComponent("quest.activate_waystone.fail").withStyle(TextFormatting.RED);

    private int baseTime;
    private int timeLeft;
    private BlockPos waystonePosition;

    public ActivateWaystoneQuest(World world, QuestScheme<ActivateWaystoneData> scheme, UUID traderId) {
        super(world, scheme, traderId);
        this.baseTime = this.getActiveData().getTicks();
        this.timeLeft = this.baseTime;
    }

    public ActivateWaystoneQuest(QuestDeserializationContext<ActivateWaystoneData> context) {
        super(context);
    }

    @Override
    protected void onAssigned(QuestingGroup group) {
        this.onAreaEntered();
    }

    @Override
    protected void onAreaEntered() {
        WaystonesConfig config = Waystones.config;
        int multiplier = this.group != null ? this.group.getMemberCount() - 1 : 0;
        this.timeLeft = config.applyGroupDifficultyScaling ? this.baseTime + multiplier * config.scalingPerMember : this.baseTime;
    }

    @Override
    public Object[] getDescriptionArguments() {
        ActivateWaystoneData data = this.getActiveData();
        return new Object[] { Interval.format(data.getTicks(), f -> f.src(Interval.Unit.TICK).out(Interval.Unit.HOUR, Interval.Unit.MINUTE, Interval.Unit.SECOND).skipAllEmptyValues()) };
    }

    public void setWaystonePosition(BlockPos waystonePos) {
        this.waystonePosition = waystonePos;
        ActivateWaystoneData data = this.getActiveData();
        this.area = generateArea(data, this.level);
        trySyncClient(this.level);
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    protected void registerAdditionalTriggers(ITriggerRegistration registration) {
        registration.addEntry(Waystones.BLOCK_DESTROYED, this::onBlockBroken, ITriggerHandler.NONE);
    }

    @Override
    protected QuestArea generateArea(ActivateWaystoneData activeData, World world) {
        QuestAreaScheme scheme = activeData.getAreaScheme();
        return new QuestArea(scheme, waystonePosition);
    }

    @Override
    public void onCompleted() {
        if (!this.level.isClientSide) {
            this.group.accept(this.level, member -> {
                ServerPlayerEntity player = (ServerPlayerEntity) member;
                player.connection.send(new SPlaySoundEffectPacket(SoundEvents.PLAYER_LEVELUP, SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 0.75F, 1.0F));
                player.sendMessage(ACTIVATE_SUCCESS, ChatType.GAME_INFO, Util.NIL_UUID);
            });
            BlockState state = this.level.getBlockState(waystonePosition);
            if (state.getBlock() == ModdedBlocks.WAYSTONE) {
                MinecraftServer server = level.getServer();
                PlayerList playerList = server.getPlayerList();
                ServerPlayerEntity player = playerList.getPlayer(this.group.getGroupId());
                if (player != null) {
                    ModdedBlocks.WAYSTONE.activate(this.level, waystonePosition, player);
                }
            }
            QuestingDataProvider.getData(this.level).ifPresent(questing -> {
                questing.unassignQuest(this.group);
                questing.sendData();
            });
        }
    }

    @Override
    public void onFailed() {
        if (!this.level.isClientSide) {
            this.group.accept(this.level, player -> {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                serverPlayer.connection.send(new SPlaySoundEffectPacket(ModSounds.USE_AVENGE_ME_FRIENDS, SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 0.75F, 1.0F));
                serverPlayer.sendMessage(ACTIVATE_FAILURE, ChatType.GAME_INFO, Util.NIL_UUID);
            });
            this.level.destroyBlock(waystonePosition, false);
            QuestingDataProvider.getData(this.level).ifPresent(questing -> {
                questing.unassignQuest(this.group);
                questing.sendData();
            });
        }
    }

    @Override
    protected void writeAdditionalData(CompoundNBT nbt) {
        nbt.putInt("base", baseTime);
        nbt.putInt("timeLeft", timeLeft);
        if (waystonePosition != null) {
            nbt.put("pos", NBTUtil.writeBlockPos(waystonePosition));
        }
    }

    @Override
    protected void readAddtionalData(CompoundNBT nbt) {
        baseTime = nbt.getInt("base");
        timeLeft = nbt.getInt("timeLeft");
        if (nbt.contains("pos", Constants.NBT.TAG_COMPOUND)) {
            waystonePosition = NBTUtil.readBlockPos(nbt.getCompound("pos"));
        }
    }

    @Override
    protected void fillDataModel(QuestDisplayDataModel model) {
        Interval.IFormatFactory format = f -> f.src(Interval.Unit.TICK).out(Interval.Unit.MINUTE, Interval.Unit.SECOND).compact();
        model.addQuestHeader(this, false);
        model.addInformationRow(this, q -> TEXT_PROTECTION, q -> new StringTextComponent(Interval.format(timeLeft, format)));
        fillAreaDataModel(model);
    }

    @Override
    protected void handleSuccessfulTick(Trigger trigger, IPropertyReader reader) {
        PlayerEntity player = reader.getProperty(QuestProperties.PLAYER);
        World level = reader.getProperty(QuestProperties.LEVEL);
        if (!level.isClientSide() && !this.group.isLeader(player.getUUID()))
            return;
        if (level.isClientSide())
            if (!shouldTick(player))
                return;
        if (timeLeft % 100 == 0)
            this.requestTemplateFactory.sendSyncRequest();
        if (--timeLeft < 0)
            setStatus(QuestStatus.COMPLETED);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean shouldTick(PlayerEntity player) {
        Minecraft client = Minecraft.getInstance();
        return player == client.player;
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
}

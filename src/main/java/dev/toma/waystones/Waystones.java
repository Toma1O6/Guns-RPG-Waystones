package dev.toma.waystones;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.format.ConfigFormats;
import dev.toma.gunsrpg.GunsRPG;
import dev.toma.gunsrpg.api.common.data.IQuestingData;
import dev.toma.gunsrpg.common.quests.quest.QuestTypes;
import dev.toma.gunsrpg.common.quests.quest.area.IAreaQuest;
import dev.toma.gunsrpg.common.quests.trigger.Trigger;
import dev.toma.gunsrpg.util.properties.PropertyKey;
import dev.toma.gunsrpg.world.cap.QuestingDataProvider;
import dev.toma.waystones.common.blocks.WaystoneBlock;
import dev.toma.waystones.common.init.ModdedBlocks;
import dev.toma.waystones.common.quest.ActivateWaystoneData;
import dev.toma.waystones.common.quest.ActivateWaystoneQuest;
import dev.toma.waystones.common.world.IWaystoneProvider;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import dev.toma.waystones.common.world.WaystoneDataStorage;
import dev.toma.waystones.common.world.WorldWaystones;
import dev.toma.waystones.config.WaystonesConfig;
import dev.toma.waystones.network.NetworkManager;
import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Waystones.MODID)
public class Waystones {

    public static final String MODID = "grpgwaystones";
    public static final Trigger BLOCK_DESTROYED = Trigger.create();
    public static final PropertyKey<BlockState> BLOCKSTATE = PropertyKey.newKey("blockstate");

    public static WaystonesConfig config;

    public Waystones() {
        config = Configuration.registerConfig(WaystonesConfig.class, ConfigFormats.yaml()).getConfigInstance();
        QuestTypes.register(new ResourceLocation(MODID, "activate_waystone"), new ActivateWaystoneData.Serializer(), ActivateWaystoneQuest.FACTORY);

        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        forgeEventBus.addGenericListener(World.class, this::addWorldCapability);
        forgeEventBus.addListener(this::disableWaystoneBreaking);
        forgeEventBus.addListener(this::onBlockDestroyed);
        modEventBus.addListener(this::modInit);
    }

    private void modInit(FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(IWaystoneProvider.class, new WaystoneDataStorage(), WorldWaystones::new);
        NetworkManager.init();
    }

    private void addWorldCapability(AttachCapabilitiesEvent<World> event) {
        ResourceLocation capabilityId = new ResourceLocation(MODID, "waystones");
        event.addCapability(capabilityId, new WaystoneCapabilityProvider());
    }

    private void disableWaystoneBreaking(PlayerInteractEvent.LeftClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == ModdedBlocks.WAYSTONE && state.getValue(WaystoneBlock.ACTIVE)) {
            event.setCanceled(true);
        }
    }

    private void onBlockDestroyed(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        IWorld iworld = event.getWorld();
        boolean areaInteractionDisabled = GunsRPG.config.quests.disableQuestAreaInteractions;
        if (!areaInteractionDisabled) {
            return;
        }
        if (!(iworld instanceof World))
            return;
        World world = (World) iworld;
        IQuestingData questing = QuestingDataProvider.getQuesting(world);
        questing.triggerAll(BLOCK_DESTROYED, event.getPlayer(), holder -> {
            holder.setProperty(BLOCKSTATE, state);
        }, quest -> quest instanceof IAreaQuest && ((IAreaQuest) quest).getQuestArea().isInArea(pos.getX(), pos.getZ()));
    }
}

package dev.toma.waystones.common;

import dev.toma.gunsrpg.ModTabs;
import dev.toma.gunsrpg.common.skills.SimpleSkill;
import dev.toma.gunsrpg.common.skills.core.DisplayData;
import dev.toma.gunsrpg.common.skills.core.DisplayType;
import dev.toma.gunsrpg.common.skills.core.SkillType;
import dev.toma.waystones.Waystones;
import dev.toma.waystones.common.blocks.WaystoneBlock;
import dev.toma.waystones.common.init.ModdedBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Waystones.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Registry {

    private static List<Block> blocks = new ArrayList<>();

    @SubscribeEvent
    public static void registerSkills(RegistryEvent.Register<SkillType<?>> event) {
        event.getRegistry().registerAll(
                SkillType.Builder.create(SimpleSkill::new).description(2).render(type -> DisplayData.create(DisplayType.ITEM, new ItemStack(ModdedBlocks.WAYSTONE))).build().setRegistryName("waystones")
        );
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        Block block = new WaystoneBlock(AbstractBlock.Properties.of(Material.STONE).strength(3.3F, 3600000.0F).noOcclusion().lightLevel(value -> 6)).setRegistryName("waystone");
        blocks.add(block);
        event.getRegistry().register(block);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        blocks.forEach(block -> registry.register(
                new BlockItem(block, new Item.Properties().tab(ModTabs.BLOCK_TAB)).setRegistryName(block.getRegistryName())
        ));
        blocks = null;
    }
}

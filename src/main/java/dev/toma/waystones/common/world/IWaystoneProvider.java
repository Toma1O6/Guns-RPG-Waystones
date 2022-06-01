package dev.toma.waystones.common.world;

import dev.toma.waystones.WaystoneProperties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;
import java.util.Set;

public interface IWaystoneProvider extends INBTSerializable<CompoundNBT> {

    void registerNewWaystone(BlockPos platformPosition, PlayerEntity player);

    void removeWaystone(BlockPos platformPosition);

    WaystoneProperties getWaystoneData(BlockPos platformPosition);

    Set<Map.Entry<BlockPos, WaystoneProperties>> getRegistryContents();
}

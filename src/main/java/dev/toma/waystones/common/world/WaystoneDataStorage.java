package dev.toma.waystones.common.world;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class WaystoneDataStorage implements Capability.IStorage<IWaystoneProvider> {

    @Nullable
    @Override
    public INBT writeNBT(Capability<IWaystoneProvider> capability, IWaystoneProvider instance, Direction side) {
        return instance.serializeNBT();
    }

    @Override
    public void readNBT(Capability<IWaystoneProvider> capability, IWaystoneProvider instance, Direction side, INBT nbt) {
        instance.deserializeNBT(nbt instanceof CompoundNBT ? (CompoundNBT) nbt : new CompoundNBT());
    }
}

package dev.toma.waystones.common.world;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WaystoneCapabilityProvider implements ICapabilitySerializable<CompoundNBT> {

    @CapabilityInject(IWaystoneProvider.class)
    public static final Capability<IWaystoneProvider> CAPABILITY = null;
    private final LazyOptional<IWaystoneProvider> instance;

    public WaystoneCapabilityProvider() {
        this.instance = LazyOptional.of(WorldWaystones::new);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? instance.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return (CompoundNBT) CAPABILITY.getStorage().writeNBT(CAPABILITY, instance.orElseThrow(NullPointerException::new), null);
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        CAPABILITY.getStorage().readNBT(CAPABILITY, instance.orElseThrow(NullPointerException::new), null, nbt);
    }
}

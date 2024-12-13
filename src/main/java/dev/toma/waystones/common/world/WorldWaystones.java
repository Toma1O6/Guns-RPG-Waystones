package dev.toma.waystones.common.world;

import dev.toma.waystones.WaystoneProperties;
import dev.toma.waystones.Waystones;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WorldWaystones implements IWaystoneProvider {

    private final Map<BlockPos, WaystoneProperties> map = new HashMap<>();

    @Override
    public void registerNewWaystone(BlockPos platformPosition, PlayerEntity player) {
        map.put(platformPosition, new WaystoneProperties(player.getUUID()));
    }

    @Override
    public void removeWaystone(BlockPos platformPosition) {
        map.remove(platformPosition);
    }

    @Override
    public WaystoneProperties getWaystoneData(BlockPos platformPosition) {
        return map.get(platformPosition);
    }

    @Override
    public Set<Map.Entry<BlockPos, WaystoneProperties>> getRegistryContents() {
        return map.entrySet();
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        ListNBT list = new ListNBT();
        map.entrySet().stream().map(entry -> {
            CompoundNBT data = new CompoundNBT();
            data.put("position", NBTUtil.writeBlockPos(entry.getKey()));
            data.put("waystone", entry.getValue().serializeNBT());
            return data;
        }).forEach(list::add);
        nbt.put("waystones", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.map.clear();
        ListNBT list = nbt.getList("waystones", Constants.NBT.TAG_COMPOUND);
        list.forEach(inbt -> {
            CompoundNBT data = (CompoundNBT) inbt;
            BlockPos position = NBTUtil.readBlockPos(data.getCompound("position"));
            CompoundNBT waystoneNbt = data.getCompound("waystone");
            UUID owner = waystoneNbt.getUUID("owner");
            WaystoneProperties properties = new WaystoneProperties(owner);
            properties.deserializeNBT(waystoneNbt);
            map.put(position, properties);
        });
    }

    public static int getPriceForDistance(int distance) {
        int scaling = Waystones.config.waystoneTravelPriceScaleStep;
        if (scaling == 0) {
            return 0;
        }
        int base = distance / scaling;
        int extra = distance % scaling;
        if (extra > 0) {
            ++base;
        }
        return base;
    }
}

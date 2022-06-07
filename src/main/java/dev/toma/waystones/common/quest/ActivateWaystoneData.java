package dev.toma.waystones.common.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.toma.gunsrpg.common.quests.quest.IQuestData;
import dev.toma.gunsrpg.common.quests.quest.QuestType;
import dev.toma.gunsrpg.common.quests.quest.area.IQuestAreaProvider;
import dev.toma.gunsrpg.common.quests.quest.area.QuestAreaScheme;
import dev.toma.gunsrpg.util.IIntervalProvider;
import dev.toma.gunsrpg.util.Interval;
import dev.toma.gunsrpg.util.helper.JsonHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.JSONUtils;

public class ActivateWaystoneData implements IQuestData, IQuestAreaProvider {

    private final int ticks;
    private final QuestAreaScheme scheme;

    public ActivateWaystoneData(IIntervalProvider provider, QuestAreaScheme scheme) {
        this.ticks = provider.getTicks();
        this.scheme = scheme;
    }

    public int getTicks() {
        return ticks;
    }

    @Override
    public QuestAreaScheme getAreaScheme() {
        return this.scheme;
    }

    public String toString() {
        int ticks = this.getTicks();
        return String.format("Survival - Timer: [%s, (%d ticks)], Area: { %s }", Interval.format(ticks, (f) -> f.src(Interval.Unit.TICK).out(new Interval.Unit[]{Interval.Unit.MINUTE, Interval.Unit.SECOND}).compact()), ticks, this.scheme.toString());
    }

    public static final class Serializer implements QuestType.IQuestDataResolver<ActivateWaystoneData> {

        public ActivateWaystoneData resolve(JsonElement element) throws JsonParseException {
            JsonObject object = JsonHelper.asJsonObject(element);
            String duration = JSONUtils.getAsString(object, "duration");
            IIntervalProvider provider = Interval.parse(duration);
            QuestAreaScheme areaScheme = QuestAreaScheme.fromJson(JSONUtils.getAsJsonObject(object, "area"));
            return new ActivateWaystoneData(provider, areaScheme);
        }

        public CompoundNBT serialize(ActivateWaystoneData data) {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("ticks", data.getTicks());
            nbt.put("area", data.scheme.toNbt());
            return nbt;
        }

        public ActivateWaystoneData deserialize(CompoundNBT nbt) {
            int ticks = nbt.getInt("ticks");
            QuestAreaScheme scheme = QuestAreaScheme.fromNbt(nbt.getCompound("area"));
            return new ActivateWaystoneData(() -> ticks, scheme);
        }
    }
}

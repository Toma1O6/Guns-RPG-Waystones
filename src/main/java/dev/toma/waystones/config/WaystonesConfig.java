package dev.toma.waystones.config;

import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.gunsrpg.util.Interval;
import dev.toma.waystones.Waystones;

@Config(id = Waystones.MODID)
public final class WaystonesConfig {

    @Configurable
    @Configurable.Comment("Apply per member objective difficulty scaling")
    public boolean applyGroupDifficultyScaling = true;

    @Configurable
    @Configurable.Range(min = 0)
    @Configurable.Comment("Additional time [ticks] which is added to objective per party member")
    public int scalingPerMember = Interval.minutes(2).getTicks();

    @Configurable
    @Configurable.Range(min = 0)
    @Configurable.Synchronized
    @Configurable.Comment({"Travel price scaling between waystones in blocks", "Smaller number = bigger price for travel", "Use 0 for free travel"})
    public int waystoneTravelPriceScaleStep = 500;
}

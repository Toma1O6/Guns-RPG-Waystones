package dev.toma.waystones;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public final class WaystoneProperties implements INBTSerializable<CompoundNBT> {

    private final UUID owner;
    private TextFormatting formatting;
    private String text;
    private ITextComponent displayComponent;

    public WaystoneProperties(UUID owner) {
        this.owner = owner;
        this.formatting = TextFormatting.WHITE;
        this.text = "New waystone";

        this.refreshComponent();
    }

    public boolean isOwner(PlayerEntity player) {
        return player.getUUID().equals(owner);
    }

    public void updateText(String text, TextFormatting formatting) {
        this.text = text;
        this.formatting = formatting;
        refreshComponent();
    }

    public String getRawText() {
        return text;
    }

    public TextFormatting getFormatting() {
        return formatting;
    }

    public ITextComponent getDisplayComponent() {
        return displayComponent;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putUUID("owner", owner);
        nbt.putInt("formatIndex", formatting.ordinal());
        nbt.putString("text", text);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        formatting = TextFormatting.values()[nbt.getInt("formatIndex")];
        text = nbt.getString("text");
    }

    public void refreshComponent() {
        this.displayComponent = new StringTextComponent(formatting + text);
    }
}

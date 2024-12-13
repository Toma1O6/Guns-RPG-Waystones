package dev.toma.waystones.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.toma.gunsrpg.api.common.data.IPlayerData;
import dev.toma.gunsrpg.api.common.data.IPointProvider;
import dev.toma.gunsrpg.api.common.data.IQuestingData;
import dev.toma.gunsrpg.client.screen.widgets.ContainerWidget;
import dev.toma.gunsrpg.common.capability.PlayerData;
import dev.toma.gunsrpg.common.init.ModItems;
import dev.toma.gunsrpg.common.quests.quest.Quest;
import dev.toma.gunsrpg.common.quests.sharing.QuestingGroup;
import dev.toma.gunsrpg.util.ModUtils;
import dev.toma.gunsrpg.util.RenderUtils;
import dev.toma.gunsrpg.util.object.LazyLoader;
import dev.toma.gunsrpg.world.cap.QuestingDataProvider;
import dev.toma.waystones.WaystoneProperties;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import dev.toma.waystones.common.world.WorldWaystones;
import dev.toma.waystones.network.C2S_BeginWaystoneActivation;
import dev.toma.waystones.network.C2S_ModifyWaystoneProperties;
import dev.toma.waystones.network.C2S_RequestWaystoneTeleport;
import dev.toma.waystones.network.NetworkManager;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WaystonesScreen extends Screen {

    private static final ITextComponent TITLE = new StringTextComponent("Waystones");
    private static final ITextComponent ACTIVATE = new TranslationTextComponent("screen.waystones.activate_waystone");
    private static final ITextComponent COMPLETE_QUEST = new TranslationTextComponent("screen.waystones.complete_quest").withStyle(TextFormatting.RED);
    private static final ITextComponent NOT_A_LEADER = new TranslationTextComponent("command.gunsrpg.exception.not_group_leader").withStyle(TextFormatting.RED);
    private static final LazyLoader<ItemStack> ICON = new LazyLoader<>(() -> new ItemStack(ModItems.PERKPOINT_BOOK));

    private final BlockPos pos;
    private final boolean requireActivation;
    private int offset;
    private int displayLimit;
    private int displaySize;
    private int perkPointCount;

    private WaystoneEditPanel editPanel;

    public WaystonesScreen(BlockPos pos, boolean requireActivation) {
        super(TITLE);
        this.pos = pos;
        this.requireActivation = requireActivation;
    }

    @Override
    protected void init() {
        int third = width / 3;
        int half = width / 2;
        IQuestingData questing = QuestingDataProvider.getQuesting(this.minecraft.level);
        IPlayerData playerData = PlayerData.getUnsafe(this.minecraft.player);
        Quest<?> active = questing.getActiveQuestForPlayer(this.minecraft.player);
        QuestingGroup group = questing.getOrCreateGroup(this.minecraft.player);
        if (!group.isLeader(this.minecraft.player.getUUID()))
            return;
        if (requireActivation) {
            int centerY = height / 2;
            Button button = addButton(new Button(third, centerY - 11, third, 20, ACTIVATE, this::activateButtonClicked));
            if (active != null) {
                button.active = false;
                button.setMessage(COMPLETE_QUEST);
                return;
            }
            if (!group.isLeader(this.minecraft.player.getUUID())) {
                button.active = false;
                button.setMessage(NOT_A_LEADER);
                return;
            }
            return;
        }
        int usableHeight = height - 20;
        displayLimit = usableHeight / 25;
        int remainder = usableHeight - displayLimit * 25;
        int heightCorrection = remainder / displayLimit;
        editPanel = addButton(new WaystoneEditPanel(2 * third, 10, third - 10, height - 20));
        minecraft.level.getCapability(WaystoneCapabilityProvider.CAPABILITY).ifPresent(provider -> {
            IPointProvider perkProvider = playerData.getPerkProvider();
            Set<Map.Entry<BlockPos, WaystoneProperties>> entrySet = provider.getRegistryContents();
            this.displaySize = entrySet.size();
            List<Map.Entry<BlockPos, WaystoneProperties>> sortedList = entrySet.stream()
                    .filter(entry -> {
                        BlockPos position = entry.getKey();
                        return !position.equals(pos);
                    })
                    .sorted((e1, e2) -> {
                        BlockPos playerPos = minecraft.player.blockPosition();
                        int delta1 = (int) e1.getKey().distSqr(playerPos);
                        int delta2 = (int) e2.getKey().distSqr(playerPos);
                        return delta1 - delta2;
                    })
                    .collect(Collectors.toList());
            for (int i = offset; i < offset + displayLimit; i++) {
                if (i >= sortedList.size()) break;
                Map.Entry<BlockPos, WaystoneProperties> entry = sortedList.get(i);
                int j = i - offset;
                WaystoneProperties properties = entry.getValue();
                WaystoneWidget widget = new WaystoneWidget(10, 10 + j * 25 + heightCorrection, half, 20, entry.getKey(), properties, this::handleWaystoneButtonClick, pos);
                widget.active = perkProvider.getPoints() >= widget.price;
                addButton(widget);
            }

            WaystoneProperties currentWaystone = provider.getWaystoneData(pos);
            if (currentWaystone.isOwner(minecraft.player)) {
                this.editPanel.setEditingWaystone(new WaystoneWidget(0, 0, 0, 0, pos, currentWaystone, w -> {}, pos));
            }
            this.perkPointCount = perkProvider.getPoints();
        });
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrix);
        minecraft.getItemRenderer().renderGuiItem(ICON.get(), width - 21, 5);
        String points = String.valueOf(perkPointCount);
        int width = font.width(points);
        font.drawShadow(matrix, points, this.width - 26 - width, 9, 0x55ffff);
        super.render(matrix, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double p_231043_1_, double p_231043_3_, double amount) {
        int delta = (int) -amount;
        int index = this.offset + delta;
        if (index >= 0 && index < this.displaySize - this.displayLimit) {
            this.offset = index;
            reloadWidgets();
        }
        return true;
    }

    private void activateButtonClicked(Button button) {
        minecraft.setScreen(null);
        NetworkManager.sendServerPacket(new C2S_BeginWaystoneActivation(pos));
    }

    private void handleWaystoneButtonClick(WaystoneWidget widget) {
        minecraft.setScreen(null);
        NetworkManager.sendServerPacket(new C2S_RequestWaystoneTeleport(pos, widget.waystonePosition));
    }

    private void reloadWidgets() {
        MainWindow window = minecraft.getWindow();
        int x = window.getGuiScaledWidth();
        int y = window.getGuiScaledHeight();
        this.init(minecraft, x, y);
    }

    private static final class WaystoneWidget extends Widget {

        private final BlockPos waystonePosition;
        private final BlockPos sourcePosition;
        private final WaystoneProperties properties;
        private final Consumer<WaystoneWidget> clickResponder;
        private final int price;
        private final ItemStack icon;
        private final ITextComponent distComponent;
        private final ITextComponent priceComponent;

        public WaystoneWidget(int x, int y, int width, int height, BlockPos pos, WaystoneProperties props, Consumer<WaystoneWidget> clickResponder, BlockPos sourcePos) {
            super(x, y, width, height, props.getDisplayComponent());
            this.waystonePosition = pos;
            this.sourcePosition = sourcePos;
            this.properties = props;
            this.clickResponder = clickResponder;
            int distanceMeters = (int) (Math.sqrt(pos.distSqr(sourcePos)));
            this.price = WorldWaystones.getPriceForDistance(distanceMeters);
            this.icon = new ItemStack(ModItems.PERKPOINT_BOOK);
            this.distComponent = new StringTextComponent(TextFormatting.GREEN + "[" + TextFormatting.YELLOW + distanceMeters + "m" + TextFormatting.GREEN + "]");
            this.priceComponent = new StringTextComponent(TextFormatting.AQUA + String.valueOf(price));
            this.properties.refreshComponent();
        }

        @Override
        public void onClick(double p_230982_1_, double p_230982_3_) {
            clickResponder.accept(this);
        }

        @Override
        public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
            Minecraft minecraft = Minecraft.getInstance();
            FontRenderer font = minecraft.font;
            minecraft.getTextureManager().bind(WIDGETS_LOCATION);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
            int i = this.getYImage(this.isHovered());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            this.blit(matrix, this.x, this.y, 0, 46 + i * 20, this.width / 2, this.height);
            this.blit(matrix, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);

            float centerY = 1.0F + y + (height - font.lineHeight) / 2.0F;
            font.draw(matrix, properties.getDisplayComponent(), x + 5, centerY, 0xFFFFFF);

            int priceWidth = 0;
            if (this.price > 0) {
                ItemRenderer renderer = minecraft.getItemRenderer();
                renderer.renderGuiItem(icon, x + width - 19, y + 2);

                priceWidth = font.width(priceComponent);
                font.draw(matrix, priceComponent, x + width - 22 - priceWidth, centerY, 0xFFFFFF);
            }

            int distanceWidth = font.width(distComponent);
            font.draw(matrix, distComponent, x + width - 22 - priceWidth - distanceWidth - 10, centerY, 0xFFFFFF);
        }
    }

    private static final class ColorSelectorWidget extends Widget {

        private final TextFormatting[] values;
        private Consumer<TextFormatting> clickResponder;
        private int currentIndex;

        public ColorSelectorWidget(int x, int y, int width, int height) {
            super(x, y, width, height, StringTextComponent.EMPTY);
            this.values = Arrays.stream(TextFormatting.values())
                    .filter(TextFormatting::isColor)
                    .sorted(Comparator.comparingInt(TextFormatting::ordinal).reversed())
                    .toArray(TextFormatting[]::new);
        }

        public void setValue(TextFormatting formatting) {
            int indexOf = ModUtils.indexOf(values, formatting);
            this.currentIndex = indexOf == -1 ? currentIndex : indexOf;
        }

        public void setClickResponder(Consumer<TextFormatting> clickResponder) {
            this.clickResponder = clickResponder;
        }

        public TextFormatting getValue() {
            return values[currentIndex];
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int modifier = hasShiftDown() ? -1 : 1;
            int next = currentIndex + modifier;
            if (next < 0) {
                next = values.length - 1;
            }
            next %= values.length;
            this.currentIndex = next;
            if (clickResponder != null) {
                clickResponder.accept(this.getValue());
            }
        }

        @Override
        public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
            Matrix4f pose = matrix.last().pose();
            RenderUtils.drawSolid(pose, x, y, x + width, y + height, 0xFFFFFFFF);
            Integer color = this.getValue().getColor();
            if (color != null) {
                RenderUtils.drawSolid(pose, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF << 24 | color);
            }
        }
    }

    private static final class WaystoneEditPanel extends ContainerWidget {

        private static final Pattern VALIDATOR = Pattern.compile("[a-zA-Z\\s_\\-/]*");
        private static final ITextComponent TEXT_SAVE = new TranslationTextComponent("screen.animator.save");
        private WaystoneWidget widget;
        private TextFieldWidget textField;
        private ColorSelectorWidget colorSelector;

        public WaystoneEditPanel(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        public void setEditingWaystone(@Nullable WaystoneWidget widget) {
            this.visible = widget != null;
            this.widget = widget;
            this.clear();
            if (!this.visible) return;

            int bottomY = this.y + this.height;
            FontRenderer font = Minecraft.getInstance().font;
            WaystoneProperties properties = widget.properties;
            textField = new TextFieldWidget(font, this.x + 30, bottomY - 50, width - 30, 20, StringTextComponent.EMPTY);
            textField.setValue(properties.getRawText());
            textField.setMaxLength(24);
            textField.setFilter(text -> VALIDATOR.matcher(text).matches());
            addWidget(textField);
            colorSelector = new ColorSelectorWidget(this.x, bottomY - 50, 20, 20);
            colorSelector.setValue(properties.getFormatting());
            colorSelector.setClickResponder(this::onFormattingChanged);
            addWidget(colorSelector);
            addWidget(new Button(this.x + this.width - 60, bottomY - 20, 60, 20, TEXT_SAVE, this::onSave));

            this.onFormattingChanged(colorSelector.getValue());
        }

        private void onSave(Button button) {
            NetworkManager.sendServerPacket(new C2S_ModifyWaystoneProperties(widget.waystonePosition, textField.getValue(), colorSelector.getValue()));
            Minecraft.getInstance().setScreen(null);
        }

        private void onFormattingChanged(TextFormatting formatting) {
            textField.setTextColor(formatting.getColor());
        }
    }
}

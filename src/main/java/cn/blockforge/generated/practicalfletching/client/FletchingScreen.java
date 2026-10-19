package cn.blockforge.generated.practicalfletching.client;

import cn.blockforge.generated.practicalfletching.block.FletchingStationBlockEntity;
import cn.blockforge.generated.practicalfletching.menu.FletchingMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private static final int WIDTH = 348;
    private static final int HEIGHT = 222;

    // 深色羊皮纸配色：槽底加深以保证白色物品数量可读
    private static final int PAPER = 0xFFD3C29C;
    private static final int PANEL_FILL = 0xFFC3B089;
    private static final int PAPER_LIGHT = 0xFFE3D6B8;
    private static final int BORDER = 0xFF1F180F;
    private static final int BORDER_MID = 0xFF4A3C29;
    private static final int BORDER_SOFT = 0xFF78684C;
    private static final int SLOT_FILL = 0xFFBFA87D;
    private static final int SLOT_LOCK_FILL = 0xFF9C8862;
    private static final int TEXT = 0xFF1B130C;
    private static final int TEXT_MUTED = 0xFF51422F;
    private static final int TEXT_LOCKED = 0xFF847254;
    private static final int ARROW_FILL = 0xFF6B5939;
    private static final int ARROW_FILL_HOVER = 0xFF4B3C24;
    private static final int ARROW_FILL_OFF = 0xFFAE9B74;
    /** 附魔台样式：可负担时数字为绿，不足时为红褐。 */
    private static final int COST_OK = 0xFF2C521D;
    private static final int COST_NO = 0xFF7C3020;
    private static final int GLYPHS_OK = 0xFF655434;
    private static final int GLYPHS_NO = 0xFF98886A;
    /** 标准银河字母（原版附魔台所用），仅作装饰。 */
    private static final String GLYPH_SET = "ᔑʖᓵ↸ᒷ⍑╎ꖎᒲ⍊𝙹∷ᓭ⚍∨⨅";

    private static final int[] SLOT_REQUIRED_TIER = {0, 0, 0, 1, 2, 3, 4, 5, 0, 3, 3, 3, 5, 5, 5, 5, 5, 5, 0};
    private static final int[] SLOT_TAB = {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, -1};

    /** 额外效果材料图鉴：物品 ID、语言键、供应数、类别键。 */
    private static final String[][] EFFECT_GUIDE = {
            {"minecraft:firework_rocket", "guide.firework", "4", "cat.trajectory"},
            {"minecraft:slime_block", "guide.slime_block", "4", "cat.trajectory"},
            {"minecraft:echo_shard", "guide.echo", "4", "cat.trajectory"},
            {"minecraft:ender_pearl", "guide.pearl", "4", "cat.trajectory"},
            {"minecraft:chorus_fruit", "guide.chorus", "4", "cat.interact"},
            {"minecraft:slime_ball", "guide.slime", "4", "cat.interact"},
            {"minecraft:bell", "guide.bell", "4", "cat.interact"},
            {"minecraft:goat_horn", "guide.horn", "4", "cat.interact"},
            {"minecraft:honey_bottle", "guide.honey", "4", "cat.interact"},
            {"minecraft:blaze_powder", "guide.blaze", "4", "cat.terrain"},
            {"minecraft:cobweb", "guide.web", "4", "cat.terrain"},
            {"minecraft:bone_meal", "guide.bone", "4", "cat.terrain"},
            {"minecraft:snowball", "guide.snow", "4", "cat.terrain"},
            {"minecraft:glowstone_dust", "guide.glowstone", "4", "cat.terrain"},
            {"minecraft:gunpowder", "guide.gunpowder", "4", "cat.impact"},
            {"minecraft:dragon_breath", "guide.dragon", "4", "cat.impact"},
            {"minecraft:prismarine_shard", "guide.prismarine", "4", "cat.impact"},
            {"minecraft:amethyst_block", "guide.amethyst", "4", "cat.impact"},
    };
    private static final String[][] BOW_GUIDE = {
            {"minecraft:firework_rocket", "guide.bow_firework", "1", "cat.trajectory"},
            {"minecraft:blaze_powder", "guide.bow_blaze", "1", "cat.terrain"},
            {"minecraft:glowstone_dust", "guide.bow_glowstone", "1", "cat.terrain"},
    };

    private GuideButton guideButton;
    /** 图鉴打开状态、翻页与当前显示的材料组（guideBow = 弓改装材料）。 */
    private boolean guideOpen;
    private int guidePage;
    private boolean guideBow;
    private static final int GUIDE_PAGE_SIZE = 8;
    /** 图鉴内返回/翻页的手绘按钮区域（左上角 x/y，宽高），随 guidePage 每帧绘制。 */
    private static final int BACK_X = WIDTH - 52, BACK_Y = 8, BACK_W = 44, BACK_H = 14;

    private final List<OfferBar> offerBars = new ArrayList<>();
    private ChevronButton prevButton;
    private ChevronButton nextButton;

    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        offerBars.clear();
        prevButton = addRenderableWidget(new ChevronButton(leftPos + 102, topPos + 80, 13, 40, true, () -> switchPage(-1)));
        prevButton.setTooltip(Tooltip.create(Component.translatable("hint.z80z_practical_fletching.page.prev")));
        nextButton = addRenderableWidget(new ChevronButton(leftPos + 332, topPos + 80, 13, 40, false, () -> switchPage(1)));
        for (int i = 0; i < 3; i++) {
            offerBars.add(addRenderableWidget(new OfferBar(leftPos + 178, topPos + 78 + i * 20, 104, 16, i)));
        }
        // 额外效果图鉴入口：底部提示行右侧的问号
        guideButton = addRenderableWidget(new GuideButton(leftPos + 298, topPos + 147));
        lastTab = -1;
        lastTier = -1;
        lastGuideOpen = !guideOpen;
        updateVisibility();
    }

    private void switchPage(int delta) {
        int target = menu.getTab() + delta;
        if (target < 0 || target > 2) return;
        if (!isPageUnlocked(target)) return;
        menu.setTab(target);
        updateVisibility();
    }

    private boolean isPageUnlocked(int tab) {
        return switch (tab) {
            case 1 -> menu.getTier() >= 3;
            case 2 -> menu.getTier() >= 5;
            default -> true;
        };
    }

    /** 上次同步的界面状态，避免每 tick 重建 Tooltip 与重复赋值。 */
    private int lastTab = -1;
    private int lastTier = -1;
    private boolean lastGuideOpen = true;

    private void updateVisibility() {
        int tab = menu.getTab();
        int tier = menu.getTier();
        if (tab == lastTab && tier == lastTier && guideOpen == lastGuideOpen) return;
        lastTab = tab;
        lastTier = tier;
        lastGuideOpen = guideOpen;
        boolean enchantPage = tab == FletchingMenu.TAB_ENCHANT;
        for (OfferBar bar : offerBars) bar.visible = enchantPage;
        // 问号只在制作页显示；图鉴打开时隐藏其余控件（返回/翻页在图鉴页内手绘）
        guideButton.visible = tab == FletchingMenu.TAB_CRAFT && !guideOpen;
        prevButton.visible = tab > 0 && !guideOpen;
        nextButton.visible = tab < 2 && !guideOpen;
        nextButton.active = tab < 2 && isPageUnlocked(tab + 1);
        if (tab >= 2) return;
        Component hint;
        if (nextButton.active) hint = Component.translatable("hint.z80z_practical_fletching.page.next");
        else hint = Component.translatable(tab + 1 == 1
                ? "hint.z80z_practical_fletching.page.enchant_locked"
                : "hint.z80z_practical_fletching.page.bow_locked");
        nextButton.setTooltip(Tooltip.create(hint));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateVisibility();
        for (OfferBar bar : offerBars) {
            int index = bar.index;
            int offer = menu.getOffer(index);
            int cost = menu.getCost(index);
            int lapis = menu.slots.get(FletchingMenu.SLOT_ENCHANT_LAPIS).getItem().getCount();
            boolean xpOk = minecraft != null && minecraft.player != null
                    && (minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= cost);
            bar.active = menu.getTier() >= 3 && offer != 0 && lapis >= index + 1 && xpOk;
        }
    }

    /** 每帧记录的鼠标位置（供图鉴按钮悬停检测）。 */
    private int mouseRelX;
    private int mouseRelY;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        mouseRelX = mouseX - leftPos;
        mouseRelY = mouseY - topPos;
        renderBackground(graphics);
        if (guideOpen) {
            // 图鉴为独立页面：完全跳过容器渲染，避免槽位物品/手持物透出
            drawGuidePage(graphics);
            return;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSlotHints(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderSlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        if (guideOpen) return;
        Slot slot = hoveredSlot;
        if (slot == null || !slot.isActive()) slot = slotAt(mouseX, mouseY);
        if (slot == null || slot.hasItem()) return;
        int index = menu.slots.indexOf(slot);
        if (index < 0 || index >= FletchingStationBlockEntity.SLOT_COUNT) return;
        int slotTab = SLOT_TAB[index];
        if (slotTab >= 0 && slotTab != menu.getTab()) return;
        if (index == FletchingMenu.SLOT_UPGRADE && menu.getTier() >= 5) return;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("hint.z80z_practical_fletching.slot." + index).withStyle(ChatFormatting.GRAY));
        int required = SLOT_REQUIRED_TIER[index];
        if (required > 0 && menu.getTier() < required) {
            lines.add(Component.translatable("hint.z80z_practical_fletching.locked",
                    Component.translatable("tier.z80z_practical_fletching." + required)).withStyle(ChatFormatting.RED));
        }
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private Slot slotAt(int mouseX, int mouseY) {
        for (Slot slot : menu.slots) {
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) return slot;
        }
        return null;
    }

    /** 悬停批量材料时在物品 tooltip 末尾追加台子余量。 */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot == null) return lines;
        int index = menu.slots.indexOf(hoveredSlot);
        if (index < 0 || index >= FletchingStationBlockEntity.SLOT_COUNT) return lines;
        int ratio = bulkRatio(index, stack);
        if (ratio <= 0) return lines;
        int banked = menu.getRemainderFor(index);
        if (banked > 0) {
            int total = stack.getCount() * ratio;
            lines.add(Component.translatable("hint.z80z_practical_fletching.remainder", total - banked, total).withStyle(ChatFormatting.GRAY));
            if (stack.getCount() == 1) {
                lines.add(Component.translatable("hint.z80z_practical_fletching.locked_last").withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }

    /** 该槽位物品是否为按支数累计的批量材料，返回比例；否则 0。 */
    private static int bulkRatio(int slotIndex, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (slotIndex == FletchingMenu.SLOT_SHAFT) {
            return stack.is(net.minecraft.tags.ItemTags.PLANKS) ? 8 : 0;
        }
        boolean potion = stack.getItem() instanceof net.minecraft.world.item.PotionItem;
        if (slotIndex == FletchingMenu.SLOT_TIP) return FletchingMenu.tipSupplies(stack);
        if (slotIndex >= FletchingMenu.SLOT_EFFECT_1 && slotIndex < FletchingMenu.SLOT_EFFECT_1 + FletchingMenu.EFFECT_SLOT_COUNT) {
            return potion ? 8 : 4;
        }
        return 0;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // 双层边框
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, BORDER);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, PAPER);
        graphics.renderOutline(x + 3, y + 3, WIDTH - 6, HEIGHT - 6, BORDER_MID);

        // 左侧背包面板
        graphics.fill(x + 9, y + 16, x + 101, y + 204, PANEL_FILL);
        graphics.renderOutline(x + 9, y + 16, 92, 188, BORDER);
        graphics.renderOutline(x + 12, y + 19, 86, 182, BORDER_SOFT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.inventory"), x + 55, y + 23, TEXT);

        drawSlotFrames(graphics, x, y);
        drawUpgradeLine(graphics, x, y);

        switch (menu.getTab()) {
            case FletchingMenu.TAB_CRAFT -> drawCraftPage(graphics, x, y);
            case FletchingMenu.TAB_ENCHANT -> drawEnchantPage(graphics, x, y);
            case FletchingMenu.TAB_BOW -> drawBowPage(graphics, x, y);
        }
    }

    private void drawSlotFrames(GuiGraphics graphics, int x, int y) {
        int tab = menu.getTab();
        int tier = menu.getTier();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            if (i < FletchingStationBlockEntity.SLOT_COUNT) {
                int slotTab = SLOT_TAB[i];
                if (slotTab >= 0 && slotTab != tab) continue;
                if (i == FletchingMenu.SLOT_UPGRADE) {
                    // 满级且槽内无剩余材料时整块升级区域隐藏；有剩料则保留可交互槽
                    if (menu.getTier() >= 5 && !slot.hasItem()) continue;
                    graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_FILL);
                    graphics.renderOutline(sx, sy, 18, 18, BORDER);
                    continue;
                }
                if (i >= FletchingMenu.SLOT_EFFECT_1 && i < FletchingMenu.SLOT_EFFECT_1 + FletchingMenu.EFFECT_SLOT_COUNT) {
                    continue; // 效果槽连成长条，统一绘制
                }
                boolean locked = tier < SLOT_REQUIRED_TIER[i];
                graphics.fill(sx, sy, sx + 18, sy + 18, locked ? SLOT_LOCK_FILL : SLOT_FILL);
                graphics.renderOutline(sx, sy, 18, 18, locked ? BORDER_SOFT : BORDER_MID);
            } else {
                graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_FILL);
                graphics.renderOutline(sx, sy, 18, 18, BORDER_SOFT);
            }
        }
        if (tab == FletchingMenu.TAB_CRAFT) {
            drawEffectStrip(graphics, x, y, tier);
        }
    }

    /** 设计图中五格效果槽是一条带内部分隔线的长条。 */
    private void drawEffectStrip(GuiGraphics graphics, int x, int y, int tier) {
        Slot first = menu.slots.get(FletchingMenu.SLOT_EFFECT_1);
        int left = x + first.x - 1;
        int top = y + first.y - 1;
        int width = FletchingMenu.EFFECT_SLOT_COUNT * 18;
        graphics.fill(left, top, left + width, top + 18, SLOT_FILL);
        graphics.renderOutline(left, top, width, 18, BORDER_MID);
        for (int i = 0; i < FletchingMenu.EFFECT_SLOT_COUNT; i++) {
            if (tier < i + 1) {
                graphics.fill(left + i * 18 + 1, top + 1, left + (i + 1) * 18 - 1, top + 17, SLOT_LOCK_FILL);
            }
            if (i > 0) graphics.fill(left + i * 18, top + 1, left + i * 18 + 1, top + 17, BORDER_SOFT);
        }
    }

    /** 无阴影居中文字（drawCenteredString 自带阴影，视觉上会形成重影）。 */
    private void drawCenteredNoShadow(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    private void drawCraftPage(GuiGraphics graphics, int x, int y) {
        drawCenteredNoShadow(graphics, Component.translatable("page.z80z_practical_fletching.craft"), x + 218, y + 30, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("desc.z80z_practical_fletching.craft1"), x + 218, y + 48, TEXT_MUTED);
        drawCenteredNoShadow(graphics, Component.translatable("desc.z80z_practical_fletching.craft2"), x + 218, y + 62, TEXT_MUTED);

        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.feather"), x + 132, y + 80, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.shaft"), x + 156, y + 80, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.tip"), x + 180, y + 80, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.effects_tier"), x + 242, y + 80, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.output"), x + 304, y + 80, TEXT);

        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.planks"), x + 156, y + 114, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.potion"), x + 180, y + 114, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.effects"), x + 242, y + 114, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("note.z80z_practical_fletching.shaft"), x + 156, y + 128, TEXT_MUTED);
        drawCenteredNoShadow(graphics, Component.translatable("desc.z80z_practical_fletching.craft_hint"), x + 218, y + 152, TEXT_MUTED);
    }

    private void drawEnchantPage(GuiGraphics graphics, int x, int y) {
        drawCenteredNoShadow(graphics, Component.translatable("page.z80z_practical_fletching.enchant"), x + 218, y + 30, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.arrow"), x + 138, y + 84, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.lapis"), x + 162, y + 84, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.output"), x + 304, y + 84, TEXT);
    }

    private void drawBowPage(GuiGraphics graphics, int x, int y) {
        drawCenteredNoShadow(graphics, Component.translatable("page.z80z_practical_fletching.bow"), x + 218, y + 30, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.bow"), x + 218, y + 72, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("label.z80z_practical_fletching.bow_effects"), x + 218, y + 138, TEXT);
        if (menu.getTier() < 5) {
            drawCenteredNoShadow(graphics, Component.translatable("hint.z80z_practical_fletching.bow_locked"), x + 218, y + 156, TEXT_LOCKED);
        }
    }

    /** 图鉴左右两侧的翻页箭头（位于表格外）。 */
    private static final int PAGE_PREV_X = 4, PAGE_NEXT_X = WIDTH - 17, PAGE_W = 13, PAGE_H = 24;
    /** 图鉴表格布局参数（相对 GUI 左上角）。 */
    private static final int TABLE_X = 18, TABLE_Y = 40, TABLE_W = 312;
    private static final int COL_ICON = 26, COL_NAME = 70, COL_EFFECT = 180, COL_SUPPLY = 36;
    private static final int ROW_H = 18;

    /** 额外效果图鉴：表格形式（表头 + 网格线 + 统一行高），材料超过一页时可用左右箭头翻页。 */
    private void drawGuidePage(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos;
        long scrollClock = System.currentTimeMillis() / 45L;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, PAPER);
        graphics.renderOutline(x, y, WIDTH, HEIGHT, BORDER);
        graphics.renderOutline(x + 3, y + 3, WIDTH - 6, HEIGHT - 6, BORDER_MID);

        drawCenteredNoShadow(graphics, Component.translatable("guide.z80z_practical_fletching.title"), x + WIDTH / 2, y + 10, TEXT);
        drawCenteredNoShadow(graphics, Component.translatable("guide.z80z_practical_fletching.ratio"), x + WIDTH / 2, y + 24, TEXT_MUTED);

        String[][] all = visibleGuideRows();
        int start = guidePage * GUIDE_PAGE_SIZE;
        int end = Math.min(all.length, start + GUIDE_PAGE_SIZE);

        int tableLeft = x + TABLE_X;
        int tableRight = tableLeft + TABLE_W;
        int tableTop = y + TABLE_Y;
        int rowCount = end - start;
        int tableBottom = tableTop + ROW_H * (rowCount + 1);

        // 表头
        graphics.fill(tableLeft, tableTop, tableRight, tableTop + ROW_H, PANEL_FILL);
        drawCellCentered(graphics, Component.translatable("guide.z80z_practical_fletching.col.item"), tableLeft, tableTop, COL_ICON, TEXT);
        drawCellCentered(graphics, Component.translatable("guide.z80z_practical_fletching.col.name"),
                tableLeft + COL_ICON, tableTop, COL_NAME, TEXT);
        drawCellCentered(graphics, Component.translatable("guide.z80z_practical_fletching.col.effect"),
                tableLeft + COL_ICON + COL_NAME, tableTop, COL_EFFECT, TEXT);
        drawCellCentered(graphics, Component.translatable("guide.z80z_practical_fletching.col.supply"),
                tableLeft + COL_ICON + COL_NAME + COL_EFFECT, tableTop, COL_SUPPLY, TEXT);

        // 数据行（隔行底色；未使用过的道具以 ??? 遮挡）
        List<Component> hoverLines = null;
        for (int i = 0; i < rowCount; i++) {
            int guideIndex = start + i;
            String[] entry = all[guideIndex];
            int rowY = tableTop + ROW_H * (i + 1);
            if (i % 2 == 1) graphics.fill(tableLeft, rowY, tableRight, rowY + ROW_H, PAPER_LIGHT);
            boolean revealed = isRowRevealed(guideIndex);
            // 悬停整行时收集说明（未解锁只提示遮挡）
            if (mouseRelX >= tableLeft - x && mouseRelX < tableRight - x
                    && mouseRelY >= rowY - y && mouseRelY < rowY - y + ROW_H) {
                hoverLines = buildGuideTooltip(entry, entry[2], revealed);
            }
            if (!revealed) {
                graphics.drawCenteredString(font, "?", tableLeft + COL_ICON / 2, rowY + (ROW_H - 8) / 2, TEXT_LOCKED);
                graphics.drawCenteredString(font, "???", tableLeft + COL_ICON + COL_NAME / 2, rowY + (ROW_H - 8) / 2, TEXT_LOCKED);
                graphics.drawCenteredString(font, "???", tableLeft + COL_ICON + COL_NAME + COL_EFFECT / 2, rowY + (ROW_H - 8) / 2, TEXT_LOCKED);
                graphics.drawCenteredString(font, "?", tableLeft + COL_ICON + COL_NAME + COL_EFFECT + COL_SUPPLY / 2,
                        rowY + (ROW_H - 8) / 2, TEXT_LOCKED);
                continue;
            }
            ItemStack icon = guideIcon(entry);
            graphics.renderItem(icon, tableLeft + (COL_ICON - 16) / 2, rowY + (ROW_H - 16) / 2);
            drawCellText(graphics, guideText(entry, "name"), tableLeft + COL_ICON, rowY, COL_NAME, TEXT, scrollClock);
            drawCellText(graphics, guideText(entry, "effect"), tableLeft + COL_ICON + COL_NAME, rowY, COL_EFFECT, TEXT_MUTED, scrollClock);
            drawCellText(graphics, Component.literal(entry[2]),
                    tableLeft + COL_ICON + COL_NAME + COL_EFFECT, rowY, COL_SUPPLY, TEXT, scrollClock);
        }

        // 网格线
        for (int i = 0; i <= rowCount + 1; i++) {
            int lineY = tableTop + ROW_H * i;
            graphics.fill(tableLeft, lineY, tableRight, lineY + 1, BORDER_SOFT);
        }
        int[] colEdges = {tableLeft, tableLeft + COL_ICON, tableLeft + COL_ICON + COL_NAME,
                tableLeft + COL_ICON + COL_NAME + COL_EFFECT, tableRight};
        for (int edge : colEdges) {
            graphics.fill(edge, tableTop, edge + 1, tableBottom, BORDER_SOFT);
        }
        graphics.renderOutline(tableLeft, tableTop, TABLE_W, tableBottom - tableTop, BORDER_MID);

        // 页码
        int totalPages = (all.length + GUIDE_PAGE_SIZE - 1) / GUIDE_PAGE_SIZE;
        if (totalPages > 1) {
            drawCenteredNoShadow(graphics, Component.literal((guidePage + 1) + " / " + totalPages),
                    x + WIDTH / 2, y + 206, TEXT_MUTED);
        }

        // 返回按钮（右上角）
        boolean backHover = mouseInBounds(BACK_X, BACK_Y, BACK_W, BACK_H);
        graphics.fill(x + BACK_X, y + BACK_Y, x + BACK_X + BACK_W, y + BACK_Y + BACK_H,
                backHover ? 0xFFF6EFDF : PAPER_LIGHT);
        graphics.renderOutline(x + BACK_X, y + BACK_Y, BACK_W, BACK_H, backHover ? BORDER : BORDER_MID);
        drawCenteredNoShadow(graphics, Component.translatable("hint.z80z_practical_fletching.guide.back"),
                x + BACK_X + BACK_W / 2, y + BACK_Y + 3, TEXT);

        // 图鉴内翻页箭头：页面左右两侧，垂直居中于表格
        int pageY = y + TABLE_Y + (ROW_H * (GUIDE_PAGE_SIZE + 1) - PAGE_H) / 2;
        if (totalPages > 1) {
            if (guidePage > 0) drawGuideChevron(graphics, x + PAGE_PREV_X, pageY, PAGE_W, PAGE_H, true);
            if (guidePage < totalPages - 1) drawGuideChevron(graphics, x + PAGE_NEXT_X, pageY, PAGE_W, PAGE_H, false);
        }

        // 悬停行：显示详细说明 tooltip
        if (hoverLines != null) {
            graphics.renderComponentTooltip(font, hoverLines, x + mouseRelX, y + mouseRelY);
        }
    }

    /** 图鉴行图标缓存（按行数组与索引），避免每帧解析注册表并新建 ItemStack。 */
    private final java.util.Map<String, ItemStack> guideIconCache = new java.util.HashMap<>();
    private final java.util.Map<String, Component> guideTextCache = new java.util.HashMap<>();

    private ItemStack guideIcon(String[] entry) {
        return guideIconCache.computeIfAbsent(entry[0], this::iconFor);
    }

    private Component guideText(String[] entry, String field) {
        return guideTextCache.computeIfAbsent(entry[1] + "." + field,
                key -> Component.translatable("guide.z80z_practical_fletching." + entry[1] + "." + field));
    }

    /** 图鉴显示全部道具；未使用过的行以「???」遮挡内容。弓改装材料组直接完整显示。 */
    private String[][] visibleGuideRows() {
        return guideBow ? BOW_GUIDE : EFFECT_GUIDE;
    }

    /** 该行道具是否已被使用（未使用则遮挡内容）。 */
    private boolean isRowRevealed(int guideIndex) {
        return guideBow || menu.isGuideItemUsed(guideIndex);
    }

    /** 构建图鉴行的详细 tooltip：名称、效果与详细数值说明（未解锁时只显示提示）。 */
    private List<Component> buildGuideTooltip(String[] entry, String supply, boolean revealed) {
        List<Component> lines = new ArrayList<>();
        if (!revealed) {
            lines.add(Component.translatable("guide.z80z_practical_fletching.locked_row").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        lines.add(Component.translatable("guide.z80z_practical_fletching." + entry[1] + ".name").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("guide.z80z_practical_fletching." + entry[1] + ".effect").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("guide.z80z_practical_fletching." + entry[1] + ".detail").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("guide.z80z_practical_fletching.supply_line", supply).withStyle(ChatFormatting.GREEN));
        return lines;
    }

    /** 单元格文字：能放下就居中；放不下则裁剪并横向滚动。 */
    private void drawCellText(GuiGraphics graphics, Component text, int cellX, int cellY, int cellW, int color, long scrollClock) {
        int available = cellW - 4;
        int textWidth = font.width(text);
        int textY = cellY + (ROW_H - 8) / 2;
        if (textWidth <= available) {
            graphics.drawString(font, text, cellX + (cellW - textWidth) / 2, textY, color, false);
            return;
        }
        int gap = 24;
        long cycle = textWidth + gap;
        long offset = scrollClock % cycle;
        int clipX = cellX + 1;
        graphics.enableScissor(clipX, cellY, cellX + cellW - 1, cellY + ROW_H);
        graphics.drawString(font, text, (int) (clipX + 1 - offset), textY, color, false);
        graphics.drawString(font, text, (int) (clipX + 1 - offset + cycle), textY, color, false);
        graphics.disableScissor();
    }

    /** 在单元格内水平、垂直居中绘制文字。 */
    private void drawCellCentered(GuiGraphics graphics, Component text, int cellX, int cellY, int cellW, int color) {
        int textX = cellX + (cellW - font.width(text)) / 2;
        int textY = cellY + (ROW_H - 8) / 2;
        graphics.drawString(font, text, textX, textY, color, false);
    }

    private void drawGuideChevron(GuiGraphics graphics, int x, int y, int w, int h, boolean left) {
        int color = BORDER;
        for (int i = 0; i < w; i++) {
            int half = (h / 2) * (left ? i + 1 : w - i) / w;
            graphics.fill(x + i, y + h / 2 - half, x + i + 1, y + h / 2 + half, color);
        }
    }

    private boolean mouseInBounds(int relX, int relY, int w, int h) {
        return mouseRelX >= relX && mouseRelX < relX + w && mouseRelY >= relY && mouseRelY < relY + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (guideOpen) {
            if (button == 0) {
                int mx = (int) mouseX - leftPos;
                int my = (int) mouseY - topPos;
                if (over(mx, my, BACK_X, BACK_Y, BACK_W, BACK_H)) {
                    guideOpen = false;
                    guidePage = 0;
                    updateVisibility();
                } else {
                    int totalPages = (visibleGuideRows().length + GUIDE_PAGE_SIZE - 1) / GUIDE_PAGE_SIZE;
                    int pageY = TABLE_Y + (ROW_H * (GUIDE_PAGE_SIZE + 1) - PAGE_H) / 2;
                    if (guidePage > 0 && over(mx, my, PAGE_PREV_X, pageY, PAGE_W, PAGE_H)) guidePage--;
                    else if (guidePage < totalPages - 1 && over(mx, my, PAGE_NEXT_X, pageY, PAGE_W, PAGE_H)) guidePage++;
                }
            }
            // 图鉴页吞掉所有点击，防止误触隐藏的容器槽位
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private ItemStack iconFor(String itemId) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        net.minecraft.world.item.Item item = id == null ? null
                : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
        return new ItemStack(item == null ? net.minecraft.world.item.Items.BARRIER : item);
    }

    private void drawUpgradeLine(GuiGraphics graphics, int x, int y) {
        if (menu.getTier() >= 5 && !menu.slots.get(FletchingMenu.SLOT_UPGRADE).hasItem()) return;
        if (menu.getTier() >= 5) {
            graphics.drawString(font, Component.translatable("hint.z80z_practical_fletching.maxed"), x + 100, y + 186, 0xFF5E8C46, false);
            return;
        }
        Component text = Component.translatable("label.z80z_practical_fletching.upgrade_line");
        graphics.drawString(font, text, x + 100, y + 186, TEXT, false);
        int lineStart = x + 100 + font.width(text) + 4;
        graphics.fill(lineStart, y + 189, x + 296, y + 190, BORDER_SOFT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 所有文字均在 renderBg 中绘制
    }

    /** 大号立体翻页箭头。 */
    private final class ChevronButton extends Button {
        private final boolean left;
        private final Runnable action;

        private ChevronButton(int x, int y, int width, int height, boolean left, Runnable action) {
            super(new Builder(Component.empty(), b -> { }).bounds(x, y, width, height).createNarration(s -> Component.empty()));
            this.left = left;
            this.action = action;
        }

        @Override
        public void onPress() {
            if (active && visible) action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bx = getX();
            int by = getY();
            int w = getWidth();
            int h = getHeight();
            boolean hovered = isHovered() && active;
            int fill = !active ? ARROW_FILL_OFF : hovered ? ARROW_FILL_HOVER : ARROW_FILL;
            drawTriangle(graphics, bx, by, w, h, left, BORDER);
            drawTriangle(graphics, bx + 1, by + 2, w - 2, h - 4, left, fill);
        }

        private void drawTriangle(GuiGraphics graphics, int bx, int by, int w, int h, boolean left, int color) {
            for (int i = 0; i < w; i++) {
                int half = (h / 2) * (left ? i + 1 : w - i) / w;
                graphics.fill(bx + i, by + h / 2 - half, bx + i + 1, by + h / 2 + half, color);
            }
        }
    }

    /** 问号：打开额外效果图鉴。 */
    private final class GuideButton extends Button {
        private GuideButton(int x, int y) {
            super(new Builder(Component.empty(), b -> { }).bounds(x, y, 10, 10)
                    .createNarration(s -> Component.translatable("hint.z80z_practical_fletching.guide.open")));
        }

        @Override
        public void onPress() {
            if (active && visible) {
                guideOpen = true;
                updateVisibility();
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bx = getX();
            int by = getY();
            int color = isHovered() ? BORDER : BORDER_MID;
            graphics.fill(bx, by, bx + width, by + height, isHovered() ? 0xFFF6EFDF : PAPER_LIGHT);
            graphics.renderOutline(bx, by, width, height, color);
            drawCenteredNoShadow(graphics, Component.literal("?"), bx + width / 2, by + 1, color);
        }
    }

    /** 附魔选项行：与原版附魔台一致，仅显示神秘符文与所需等级，附魔内容未知。 */
    private final class OfferBar extends Button {
        private final int index;

        private OfferBar(int x, int y, int width, int height, int index) {
            super(new Builder(Component.empty(), b -> {
                if (Minecraft.getInstance().gameMode != null) {
                    Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 50 + index);
                }
            }).bounds(x, y, width, height));
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bx = getX();
            int by = getY();
            int w = getWidth();
            int h = getHeight();
            int offer = menu.getOffer(index);
            int cost = menu.getCost(index);
            boolean affordable = active;
            int fill = affordable ? (isHovered() ? 0xFFF6EFDF : PAPER_LIGHT) : SLOT_LOCK_FILL;
            graphics.fill(bx, by, bx + w, by + h, fill);
            graphics.renderOutline(bx, by, w, h, affordable ? BORDER_MID : BORDER_SOFT);

            // 神秘符文（每次选项固定，点击前不显示附魔内容）
            String glyphs = glyphRow(offer, index);
            graphics.drawString(font, glyphs, bx + 5, by + 4, affordable ? GLYPHS_OK : GLYPHS_NO, false);
            // 右侧显示所需经验等级（原版样式）
            String costText = String.valueOf(cost);
            graphics.drawString(font, costText, bx + w - 5 - font.width(costText), by + 4,
                    affordable ? COST_OK : COST_NO, false);
        }

        private String glyphRow(int offer, int index) {
            StringBuilder sb = new StringBuilder();
            long seed = offer * 1103515245L + index * 12345L + 7;
            for (int i = 0; i < 7; i++) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int pick = (int) Math.floorMod(seed >>> 16, (long) GLYPH_SET.length());
                sb.append(GLYPH_SET.charAt(pick));
                if (i == 2 || i == 4) sb.append(' ');
            }
            return sb.toString();
        }
    }
}

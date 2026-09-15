package dev.choruspreview;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private boolean error;
    public SettingsScreen(Screen parent) {
        super(Component.translatable("choruspreview.title")); this.parent = parent;
    }
    private Component label(String key, boolean state) {
        return Component.translatable("choruspreview." + key).append(": ")
            .append(Component.translatable(state ? "options.on" : "options.off"));
    }
    private Component modeLabel() {
        return Component.translatable("choruspreview.highlight").append(": ")
            .append(Component.translatable("choruspreview.highlight." + ChorusPreview.config.highlightMode));
    }
    private int top() { return Math.max(28, height / 2 - 102); }
    @Override protected void init() {
        int x = width / 2 - 150, y = top();
        Config c = ChorusPreview.config;
        addRenderableWidget(Button.builder(label("enabled", c.enabled), b -> {
            c.enabled = !c.enabled; b.setMessage(label("enabled", c.enabled)); ChorusPreview.invalidate();
        }).bounds(x, y, 146, 20).build());
        addRenderableWidget(Button.builder(label("walls", c.throughWalls), b -> {
            c.throughWalls = !c.throughWalls; b.setMessage(label("walls", c.throughWalls));
        }).bounds(x + 154, y, 146, 20).build());
        addRenderableWidget(Button.builder(label("hud", c.showHud), b -> {
            c.showHud = !c.showHud; rebuildWidgets();
        }).bounds(x, y + 24, 146, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("choruspreview.hud_position"), b ->
            c.hudY = c.hudY < 40 ? Math.max(8, height - 70) : 8
        ).bounds(x + 154, y + 24, 146, 20).build());
        if(c.showHud)addRenderableWidget(Button.builder(label("compact_hud",c.compactHud),b->{
            c.compactHud=!c.compactHud;b.setMessage(label("compact_hud",c.compactHud));
        }).bounds(x,y+48,146,20).build());
        addRenderableWidget(Button.builder(modeLabel(),b->{
            c.highlightMode=(c.highlightMode+1)%3;b.setMessage(modeLabel());
        }).bounds(x+154,y+48,146,20).build());
        addRenderableWidget(Button.builder(Component.translatable("choruspreview.refresh", c.refreshTicks), b -> {
            c.refreshTicks = c.refreshTicks >= 40 ? 2 : c.refreshTicks + 2;
            b.setMessage(Component.translatable("choruspreview.refresh", c.refreshTicks));
        }).bounds(x, y + 72, 146, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("choruspreview.budget", c.scanBudgetMillis), b -> {
            c.scanBudgetMillis = c.scanBudgetMillis >= 10 ? 1 : c.scanBudgetMillis + 1;
            b.setMessage(Component.translatable("choruspreview.budget", c.scanBudgetMillis));
        }).bounds(x + 154, y + 72, 146, 20).build());
        String[] names = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) {
            final int shift = 16 - 8 * i;
            EditBox field = new EditBox(font, x + 20 + i * 100, y + 104, 74, 20,
                Component.literal(names[i] + " (0–255)"));
            field.setMaxLength(3);
            field.setFilter(v -> v.isEmpty() || v.matches("[0-9]{1,3}") && Integer.parseInt(v) <= 255);
            field.setValue(Integer.toString((c.rgb() >> shift) & 255));
            field.setResponder(v -> {
                int value = v.isEmpty() ? 0 : Integer.parseInt(v);
                c.color = String.format(java.util.Locale.ROOT, "%06X", (c.rgb() & ~(255 << shift)) | (value << shift));
            });
            addRenderableWidget(field);
        }
        addRenderableWidget(Button.builder(Component.literal("−"), b -> changeTransparency(-1))
            .bounds(x + 200, y + 134, 44, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> changeTransparency(1))
            .bounds(x + 250, y + 134, 44, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), b -> {
            ChorusPreview.config = new Config(); ChorusPreview.invalidate(); rebuildWidgets();
        }).bounds(x, y + 168, 146, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
            .bounds(x + 154, y + 168, 146, 20).build());
    }
    private void changeTransparency(int direction) {
        Config c = ChorusPreview.config;
        int percent = Math.round((255 - c.opacity) * 100f / 255);
        percent = Math.max(0, Math.min(100, percent + direction));
        c.opacity = Math.round(255 * (100 - percent) / 100f);
    }
    @Override public void onClose() {
        if (!ChorusPreview.config.save()) { error = true; return; }
        ChorusPreview.invalidate(); minecraft.setScreen(parent);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int x = width / 2 - 150, y = top();
        g.drawCenteredString(font, title, width / 2, y - 22, 0xFFFFFFFF);
        String[] names = {"R", "G", "B"};
        for (int i = 0; i < 3; i++) g.drawString(font, names[i], x + i * 100, y + 110, 0xFFFFFFFF);
        int percent = Math.round((255 - ChorusPreview.config.opacity) * 100f / 255);
        g.drawString(font, Component.translatable("choruspreview.transparency", percent), x, y + 140, 0xFFFFFFFF);
        g.fill(x + 178, y + 137, x + 192, y + 151, 0xFF000000 | ChorusPreview.config.rgb());
        if (error) g.drawCenteredString(font, Component.translatable("choruspreview.save_error"), width / 2, y + 193, 0xFFFF5555);
    }
}

package dev.choruspreview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.*;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("choruspreview.json");
    public boolean enabled = true;
    public boolean throughWalls = true;
    public boolean showHud = true;
    public boolean compactHud = false;
    public int highlightMode = 0;
    public boolean volumes = false; // Legacy setting; rendering is always flat.
    public String color = "44FF66";
    public int opacity = 70;
    public int refreshTicks = 10;
    public int scanBudgetMillis = 5;
    public int hudX = 8;
    public int hudY = 8;

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) { c.normalize(); return c; }
            }
        } catch (IOException | RuntimeException e) { ChorusPreview.LOGGER.warn("Could not load config; using defaults", e); }
        return new Config();
    }

    public boolean save() {
        normalize();
        try {
            Files.createDirectories(FILE.getParent());
            Path temp = Files.createTempFile(FILE.getParent(), "choruspreview-", ".tmp");
            Files.writeString(temp, GSON.toJson(this));
            try { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, FILE, StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (IOException e) { ChorusPreview.LOGGER.error("Could not save config", e); return false; }
    }

    public void normalize() {
        if (color == null || !color.matches("[0-9a-fA-F]{6}")) color = "44FF66";
        highlightMode = Math.max(0, Math.min(2, highlightMode));
        opacity = Math.max(0, Math.min(255, opacity));
        refreshTicks = Math.max(2, Math.min(100, refreshTicks));
        scanBudgetMillis = Math.max(1, Math.min(10, scanBudgetMillis));
        hudX = Math.max(0, Math.min(4000, hudX)); hudY = Math.max(0, Math.min(4000, hudY));
    }

    public int rgb() { return Integer.parseInt(color, 16); }
}

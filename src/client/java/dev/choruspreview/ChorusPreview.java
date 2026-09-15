package dev.choruspreview;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChorusPreview implements ClientModInitializer {
    public static final Logger LOGGER=LoggerFactory.getLogger("choruspreview");
    public static Config config; public static Scan scan,shown; public static String problem; private static int age;
    @Override public void onInitializeClient(){
        config=Config.load();
        var toggle=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.choruspreview.toggle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_H,KeyMapping.Category.MISC));
        var settings=KeyBindingHelper.registerKeyBinding(new KeyMapping("key.choruspreview.settings",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(mc->{while(toggle.consumeClick()){config.enabled=!config.enabled;config.save();invalidate();if(mc.player!=null)mc.player.displayClientMessage(Component.translatable(config.enabled?"choruspreview.on":"choruspreview.off"),true);}while(settings.consumeClick())if(mc.screen==null)mc.setScreen(new SettingsScreen(null));tick(mc);});
        WorldRenderEvents.AFTER_ENTITIES.register(Overlay::world);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("choruspreview","status"),Overlay::hud);
        LOGGER.info("Chorus Preview 1.2.2 by razorbow initialized for Minecraft 1.21.11");
    }
    public static void invalidate(){scan=null;shown=null;age=0;problem=null;}
    public static ItemStack held(Minecraft mc){if(mc.player==null)return ItemStack.EMPTY;var p=mc.player;if(p.isUsingItem()&&p.getUseItem().is(Items.CHORUS_FRUIT))return p.getUseItem();if(p.getMainHandItem().is(Items.CHORUS_FRUIT))return p.getMainHandItem();if(p.getOffhandItem().is(Items.CHORUS_FRUIT))return p.getOffhandItem();return ItemStack.EMPTY;}
    public static boolean visible(Minecraft mc){return config!=null&&config.enabled&&mc.level!=null&&mc.player!=null&&mc.player.isAlive()&&!mc.player.isSpectator()&&!held(mc).isEmpty();}
    private static double diameter(ItemStack stack){var consume=stack.get(DataComponents.CONSUMABLE);if(consume==null)return 0;double d=0;for(var e:consume.onConsumeEffects())if(e instanceof TeleportRandomlyConsumeEffect t){if(d!=0)return 0;d=t.diameter();}return d;}
    private static void tick(Minecraft mc){
        if(!visible(mc)){invalidate();return;}if(mc.isPaused()||mc.screen!=null)return;double d=diameter(held(mc));if(!(d>0&&d<=32)){invalidate();problem="choruspreview.unsupported";return;}
        var p=mc.player;Scan ref=scan!=null?scan:shown;
        if(ref!=null&&(ref.level!=mc.level||ref.diameter!=d||Math.abs(ref.height-p.getBoundingBox().getYsize())>1e-6||Math.abs(ref.half-p.getBoundingBox().getXsize()/2)>1e-6||ref.origin.distanceToSqr(p.position())>16))invalidate();
        if(scan==null&&(shown==null||++age>=config.refreshTicks)){scan=new Scan(mc.level,p,d);age=0;}
        if(scan!=null)try{scan.advance(config.scanBudgetMillis*1_000_000L);if(scan.complete()){shown=scan;scan=null;age=0;problem=null;}}catch(RuntimeException e){LOGGER.error("Chorus scan failed",e);scan=null;shown=null;problem="choruspreview.error";config.enabled=false;p.displayClientMessage(Component.translatable("choruspreview.error"),false);}
    }
}

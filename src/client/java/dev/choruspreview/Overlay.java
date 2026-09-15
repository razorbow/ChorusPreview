package dev.choruspreview;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import java.util.LinkedHashSet;
import java.util.Locale;
import static dev.choruspreview.ChorusPreview.*;

public final class Overlay {
    private static final float MARKER_HALF=0.31f;
    private record Marker(int x,int y,int z){}
    private Overlay() {}
    public static void world(WorldRenderContext context){
        var mc=Minecraft.getInstance();if(!visible(mc)||mc.options.hideGui||mc.screen!=null)return;Scan s=shown!=null?shown:scan;if(s==null||s.level!=mc.level)return;
        var poses=context.matrices();var camera=context.worldState().cameraRenderState.pos;poses.pushPose();var matrix=poses.last().pose();
        VertexConsumer out=context.consumers().getBuffer(config.throughWalls?RenderTypes.textBackgroundSeeThrough():RenderTypes.textBackground());int color=(config.opacity<<24)|(config.rgb()&0xFFFFFF);
        var markers=new LinkedHashSet<Marker>();
        for(var b:s.regions.keySet())markers.add(new Marker(Geometry.markerCell(b.x0(),b.x1()),Geometry.markerLevel(b.y0()),Geometry.markerCell(b.z0(),b.z1())));
        for(var marker:markers){
            float cx=(float)(marker.x()+.5-camera.x),cz=(float)(marker.z()+.5-camera.z);
            float x0=cx-MARKER_HALF,x1=cx+MARKER_HALF,z0=cz-MARKER_HALF,z1=cz+MARKER_HALF,y0=(float)(marker.y()+.012-camera.y);
            if(config.highlightMode!=1)face(out,matrix,color,x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1);
            if(config.highlightMode!=0)outline(out,matrix,color,x0,y0+.002f,z0,x1,z1);
        }poses.popPose();
    }
    private static void outline(VertexConsumer out,Matrix4f matrix,int c,float x0,float y,float z0,float x1,float z1){
        float t=Math.min(.035f,Math.min((x1-x0)/2,(z1-z0)/2));if(t<=0)return;
        face(out,matrix,c,x0,y,z0,x1,y,z0,x1,y,z0+t,x0,y,z0+t);
        face(out,matrix,c,x0,y,z1-t,x1,y,z1-t,x1,y,z1,x0,y,z1);
        face(out,matrix,c,x0,y,z0+t,x0+t,y,z0+t,x0+t,y,z1-t,x0,y,z1-t);
        face(out,matrix,c,x1-t,y,z0+t,x1,y,z0+t,x1,y,z1-t,x1-t,y,z1-t);
    }
    private static void face(VertexConsumer out,Matrix4f matrix,int c,float... a){for(int i=0;i<4;i++)v(out,matrix,c,a,3*i);for(int i=3;i>=0;i--)v(out,matrix,c,a,3*i);}
    private static void v(VertexConsumer out,Matrix4f m,int c,float[] a,int i){out.addVertex(m,a[i],a[i+1],a[i+2]).setColor(c).setLight(0x00F000F0);}
    public static void hud(GuiGraphics g,DeltaTracker delta){
        var mc=Minecraft.getInstance();if(!visible(mc)||!config.showHud||mc.options.hideGui||mc.screen!=null)return;int x=Math.min(config.hudX,Math.max(0,g.guiWidth()-285)),y=Math.min(config.hudY,Math.max(0,g.guiHeight()-64));
        Scan s=shown;Component first;int color=config.rgb()|0xFF000000;
        if(problem!=null)first=Component.translatable(problem);else if(s==null)first=Component.translatable("choruspreview.scanning",scan==null?0:100*scan.done/scan.columns);else if(s.limited||s.unknown)first=Component.translatable("choruspreview.incomplete");else{double chance=Geometry.success(Math.min(1,s.probability))*100;String n=chance>99.995&&chance<100?">99.99":chance>0&&chance<.01?"<0.01":String.format(Locale.ROOT,"%.2f",chance);first=Component.translatable("choruspreview.chance",n);if(chance==0)color=0xFFFF5555;}
        if(config.compactHud){int right=Math.min(g.guiWidth(),x+mc.font.width(first)+4);g.fill(x-3,y-3,right,y+mc.font.lineHeight+3,0x99000000);g.drawString(mc.font,first,x,y,color);return;}
        g.fill(x-3,y-3,Math.min(g.guiWidth(),x+285),y+57,0x99000000);g.drawString(mc.font,first,x,y,color);Scan current=s!=null?s:scan;
        if(current!=null){g.drawString(mc.font,Component.translatable("choruspreview.height",String.format(Locale.ROOT,"%.2f",current.height),current.regions.size()),x,y+12,0xFFE0E0E0);boolean updating=scan!=null||current.origin.distanceToSqr(mc.player.position())>.0025;g.drawString(mc.font,Component.translatable(updating?"choruspreview.updating":"choruspreview.snapshot"),x,y+24,0xFFBBBBBB);}
        String state=mc.player.getCooldowns().isOnCooldown(held(mc))?"choruspreview.cooldown":"choruspreview.legend";g.drawString(mc.font,Component.translatable(state),x,y+36,0xFFE0E0E0);if(mc.player.isPassenger())g.drawString(mc.font,Component.translatable("choruspreview.riding"),x,y+48,0xFFFFCC66);
    }
}

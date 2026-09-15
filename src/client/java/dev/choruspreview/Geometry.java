package dev.choruspreview;

import java.util.*;

public final class Geometry {
    private Geometry() {}
    public record Box(double x0,double y0,double z0,double x1,double y1,double z1) {
        public double measure() { return (x1-x0)*(z1-z0)*(y1==y0 ? 1 : y1-y0); }
        public boolean valid() { return x1>x0 && z1>z0 && y1>=y0; }
    }
    public record Slice(int startFloor,double lowFraction,double highFraction,double weight) {}

    public static List<Box> subtract(Box a,Box b) {
        double x0=Math.max(a.x0,b.x0),x1=Math.min(a.x1,b.x1),y0=Math.max(a.y0,b.y0),y1=Math.min(a.y1,b.y1),z0=Math.max(a.z0,b.z0),z1=Math.min(a.z1,b.z1);
        boolean flat=a.y0==a.y1;
        if(x1<=x0||z1<=z0||(flat ? !(b.y0<a.y0&&b.y1>a.y0) : y1<=y0))return List.of(a);
        var out=new ArrayList<Box>(6);
        add(out,new Box(a.x0,a.y0,a.z0,x0,a.y1,a.z1)); add(out,new Box(x1,a.y0,a.z0,a.x1,a.y1,a.z1));
        add(out,new Box(x0,a.y0,a.z0,x1,a.y1,z0)); add(out,new Box(x0,a.y0,z1,x1,a.y1,a.z1));
        if(!flat) { if(y0>a.y0)add(out,new Box(x0,a.y0,z0,x1,y0,z1)); if(y1<a.y1)add(out,new Box(x0,y1,z0,x1,a.y1,z1)); }
        return out;
    }
    private static void add(List<Box> out,Box b) { if(b.valid())out.add(b); }
    public static int markerCell(double low,double high) { return (int)Math.floor((low+high)*.5); }
    public static int markerLevel(double low) { return (int)Math.floor(low+1e-7); }
    public static double success(double single) { if(single<=0)return 0;if(single>=1)return 1;return -Math.expm1(16*Math.log1p(-single)); }
    public static List<Slice> slices(double low,double high,int min,int max) {
        var out=new ArrayList<Slice>(); double lo=Math.max(low,min),hi=Math.min(high,max);
        for(int k=(int)Math.floor(lo);k<=Math.floor(hi);k++){double a=Math.max(lo,k),b=Math.min(hi,k+1d);if(b>a)out.add(new Slice(k,a-k,b-k,1));}
        double below=Math.max(0,Math.min(high,min)-low),above=Math.max(0,high-Math.max(low,max));
        if(below>0)out.add(new Slice(min,0,0,below)); if(above>0)out.add(new Slice(max,0,0,above)); return out;
    }
}

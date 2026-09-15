package dev.choruspreview;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.*;
import static dev.choruspreview.Geometry.*;

public final class Scan {
    public final ClientLevel level;
    public final Vec3 origin;
    public final double height,half,diameter;
    public final int columns;
    public int done;
    public boolean unknown,limited;
    public double probability;
    public final LinkedHashMap<Box,Double> regions=new LinkedHashMap<>();
    private final int x0,x1,z0,z1;
    private final double lowX,highX,lowZ,highZ,denominator;
    private final List<Slice> slices;
    private final LocalPlayer player;
    private static final double EPS=1e-7;

    public Scan(ClientLevel level,LocalPlayer player,double diameter) {
        this.level=level;this.player=player;this.diameter=diameter;origin=player.position();
        height=player.getBoundingBox().getYsize();half=player.getBoundingBox().getXsize()/2;
        lowX=origin.x-diameter/2;highX=origin.x+diameter/2;lowZ=origin.z-diameter/2;highZ=origin.z+diameter/2;
        x0=(int)Math.floor(lowX);x1=(int)Math.ceil(highX)-1;z0=(int)Math.floor(lowZ);z1=(int)Math.ceil(highZ)-1;
        columns=(x1-x0+1)*(z1-z0+1);denominator=diameter*diameter*diameter;
        slices=slices(origin.y-diameter/2,origin.y+diameter/2,level.getMinY(),level.getMinY()+level.dimensionType().logicalHeight()-1);
    }

    public boolean complete(){return done>=columns;}
    public void advance(long budgetNanos){long stop=System.nanoTime()+budgetNanos;do{if(complete())break;int x=x0+done%(x1-x0+1),z=z0+done/(x1-x0+1);done++;column(x,z);}while(System.nanoTime()<stop);}

    private void column(int x,int z) {
        int reach=(int)Math.ceil(half)+1;
        for(int dx=-reach;dx<=reach;dx++)for(int dz=-reach;dz<=reach;dz++)if(!level.hasChunkAt(new BlockPos(x+dx,0,z+dz))){unknown=true;return;}
        var border=level.getWorldBorder();
        double ax=Math.max(Math.max(lowX,x),border.getMinX()+half),bx=Math.min(Math.min(highX,x+1),border.getMaxX()-half);
        double az=Math.max(Math.max(lowZ,z),border.getMinZ()+half),bz=Math.min(Math.min(highZ,z+1),border.getMaxZ()-half);
        if(bx<=ax||bz<=az)return;
        int max=slices.stream().mapToInt(Slice::startFloor).max().orElse(level.getMinY()),min=level.getMinY();
        int[] support=new int[Math.max(0,max-min+1)];Arrays.fill(support,Integer.MIN_VALUE);int found=Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int k=min+1;k<=max;k++){pos.set(x,k-1,z);if(level.getBlockState(pos).blocksMotion())found=k-1;support[k-min]=found;}
        var candidates=new LinkedHashMap<Box,Double>();
        for(Slice s:slices){int k=s.startFloor();if(k<min||k>max||support[k-min]==Integer.MIN_VALUE)continue;double base=support[k-min]+1;Box b=new Box(ax,base+s.lowFraction(),az,bx,base+s.highFraction(),bz);candidates.merge(b,s.weight(),Double::sum);}
        candidates.forEach((b,w)->free(b,w,0));
    }

    private final class Probe extends EntityCollisionContext {
        private final double lo,hi; double split=Double.NaN;
        Probe(double y,double lo,double hi){super(player.isDescending(),false,y,player.getMainHandItem(),false,player);this.lo=lo;this.hi=hi;}
        @Override public boolean isAbove(VoxelShape shape,BlockPos pos,boolean fallback){double t=pos.getY()+shape.max(Direction.Axis.Y)-(double)1e-5f;if(t>lo+1e-10&&t<hi-1e-10)split=t;return super.isAbove(shape,pos,fallback);}
    }

    private void free(Box b,double weight,int depth) {
        if(depth>48||regions.size()>60000){limited=true;return;}
        AABB envelope=new AABB(b.x0()-half,b.y0(),b.z0()-half,b.x1()+half,b.y1()+height,b.z1()+half);
        Probe context=new Probe((b.y0()+b.y1())/2,b.y0(),b.y1());var obstacles=new ArrayList<AABB>();
        var it=new BlockCollisions<VoxelShape>(level,context,envelope,false,(pos,shape)->shape);while(it.hasNext())obstacles.addAll(it.next().toAabbs());
        if(!Double.isNaN(context.split)){free(new Box(b.x0(),b.y0(),b.z0(),b.x1(),context.split,b.z1()),weight,depth+1);free(new Box(b.x0(),context.split,b.z0(),b.x1(),b.y1(),b.z1()),weight,depth+1);return;}
        for(var shape:level.getEntityCollisions(player,envelope))obstacles.addAll(shape.toAabbs());
        var remaining=new ArrayList<Box>();remaining.add(b);
        for(AABB obstacle:obstacles){cut(remaining,new Box(obstacle.minX-half+EPS,obstacle.minY-height+EPS,obstacle.minZ-half+EPS,obstacle.maxX+half-EPS,obstacle.maxY-EPS,obstacle.maxZ+half-EPS));if(remaining.isEmpty())return;}
        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int x=(int)Math.floor(envelope.minX);x<Math.ceil(envelope.maxX);x++)for(int y=(int)Math.floor(envelope.minY);y<Math.ceil(envelope.maxY);y++)for(int z=(int)Math.floor(envelope.minZ);z<Math.ceil(envelope.maxZ);z++){
            pos.set(x,y,z);if(!level.getFluidState(pos).isEmpty())cut(remaining,new Box(x-half,y-height,z-half,x+1+half,y+1,z+1+half));if(remaining.isEmpty())return;
        }
        for(Box box:remaining){double mass=box.measure()*weight/denominator;probability+=mass;regions.merge(box,mass,Double::sum);}
    }
    private void cut(ArrayList<Box> list,Box forbidden){var next=new ArrayList<Box>();for(Box b:list)next.addAll(subtract(b,forbidden));list.clear();list.addAll(next);if(list.size()>60000){limited=true;list.clear();}}
}

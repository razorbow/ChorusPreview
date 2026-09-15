package dev.choruspreview;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static dev.choruspreview.Geometry.*;

public class GeometryTest {
    @Test void subtractConservesVolumeAndDoesNotOverlap(){
        Random r=new Random(771);Box full=new Box(0,0,0,1,1,1);
        for(int i=0;i<1000;i++){double x=r.nextDouble()*2-.5,y=r.nextDouble()*2-.5,z=r.nextDouble()*2-.5;Box cut=new Box(x,y,z,x+r.nextDouble(),y+r.nextDouble(),z+r.nextDouble());List<Box> pieces=subtract(full,cut);double ix=Math.max(0,Math.min(1,cut.x1())-Math.max(0,cut.x0())),iy=Math.max(0,Math.min(1,cut.y1())-Math.max(0,cut.y0())),iz=Math.max(0,Math.min(1,cut.z1())-Math.max(0,cut.z0()));assertEquals(1-ix*iy*iz,pieces.stream().mapToDouble(Box::measure).sum(),1e-12);for(int a=0;a<pieces.size();a++)for(int b=a+1;b<pieces.size();b++)assertEquals(0,intersection(pieces.get(a),pieces.get(b)),1e-12);}
    }
    private double intersection(Box a,Box b){return Math.max(0,Math.min(a.x1(),b.x1())-Math.max(a.x0(),b.x0()))*Math.max(0,Math.min(a.y1(),b.y1())-Math.max(a.y0(),b.y0()))*Math.max(0,Math.min(a.z1(),b.z1())-Math.max(a.z0(),b.z0()));}
    @Test void yClampPreservesProbabilityMass(){for(double center:new double[]{-100,-64,-63.8,0,63.37,319,330}){var s=slices(center-8,center+8,-64,319);double mass=s.stream().mapToDouble(v->v.weight()*(v.lowFraction()==v.highFraction()?1:v.highFraction()-v.lowFraction())).sum();assertEquals(16,mass,1e-12);}}
    @Test void sixteenAttemptsFormula(){assertEquals(0,success(0));assertEquals(1,success(1));assertEquals(1-Math.pow(.9,16),success(.1),1e-14);assertEquals(16e-12,success(1e-12),1e-20);}
    @Test void markersAreCenteredInTheirBlock(){assertEquals(0,markerCell(0,.85));assertEquals(4,markerCell(4.2,5));assertEquals(-2,markerCell(-2,-1.1));assertEquals(12,markerLevel(12.999));assertEquals(-3,markerLevel(-2.001));}
    @Test void actualPoseHeightChangesTunnelVolume(){assertEquals(.2,space(1.8,2),1e-12);assertEquals(.5,space(1.5,2),1e-12);assertEquals(.4,space(.6,1),1e-12);assertEquals(0,space(1.5,1.5),1e-12);}
    private double space(double h,double ceiling){return subtract(new Box(0,0,0,1,1,1),new Box(-1,ceiling-h,-1,2,10,2)).stream().mapToDouble(Box::measure).sum();}
}

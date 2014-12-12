/*
 * GLine.java
 *
 * Created on 25 juin 2003, 14:51
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import expressions.*;
import java.awt.*;
import java.math.*;

/**
 *
 * @author  Patrice Maupou
 */
public class GLine extends GObject {
  
  public GPoint A, B;
  private float sepA = 0F , sepB = 0F; // prolonge à gauche et à droite en %
  private int scale = -1; // nombre de graduations entre A et B
  
  /** Creates a new instance of GLine 
   * @param A 
   * @param B 
   */
  public GLine(GPoint A, GPoint B) {
    this.A = A;
    this.B = B;
    sepA = 0;
    sepB = 0;
    drawGObject = false;
  }
        
  public void setSepA(float sepA) {
    this.sepA = sepA;
  }
  
  public void setSepB(float sepB) {
    this.sepB = sepB;
  }
  
  public int getScale() {
    return scale;
  }
  
  public void setScale(int scale) {
    this.scale = scale;
  }
   
   
  public void paint(Graphics g) {
    Color save = g.getColor();
    if(color == null) color = save;
    g.setColor(color);
    Point RA = A.scale(base);
    Point RB = B.scale(base);
    int ux = RB.x-RA.x, uy = RB.y-RA.y; // direction (AB)
    RA = new Point(RA.x - Math.round(sepA*ux), RA.y - Math.round(sepA*uy));
    RB = new Point(RB.x + Math.round(sepB*ux), RB.y + Math.round(sepB*uy));
    g.drawLine(RA.x, RA.y, RB.x, RB.y);
    if(drawGObject) {
      A.paint(g);
      B.paint(g);
    }
    if(scale > 0) { // graduations éventuelles
      GPoint M;
      int na = (int)(sepA*scale), nb = (int)((1+sepB)*scale);
      for(int i = -na; i < nb+1; i++) {
        int j = scale -i;
        double AX = A.x.toDouble(), AY = A.y.toDouble(), BX = B.x.toDouble(), BY = B.y.toDouble();
        M = new GPoint(new Numeric((j*AX+i*BX)/scale), new Numeric((j*AY+i*BY)/scale));
        M.base = base;
        M.setColor(color);        
        M.symbol = symbol;
        M.paint(g);
      }
    }
    g.setColor(save);
  }

  
  public Rectangle frame(Numeric[] b) {
   return null;
  }
  
  @Override
public GPoint[] getPoints() {
    return new GPoint[]{A,B};
  }
  
  @Override
public Point[] getPointsRelative(Point p, double e) {
  Point[] pts = new Point[2];
  Point pA = A.scale(base), pB = B.scale(base);
  Point dAB = new Point(pB.x-pA.x, pB.y-pA.y);
  pts[0] = new Point(pA.x-p.x, pA.y-p.y);
  pts[1] = new Point(pB.x-p.x, pB.y-p.y);
  int det = pts[0].x*dAB.y - pts[0].y*dAB.x;
  int AB2 = dAB.x*dAB.x + dAB.y*dAB.y;
  if(det*det > e*e*AB2) return null; // trop loin
  else return pts;
}  
  
  
  @Override
public String toString() {
    return "droite(" + A.toString() + ", " + B.toString() + ")";
  }
  
 

}

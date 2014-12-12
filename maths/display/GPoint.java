/*
 * GPoint.java
 *
 * Created on 23 juin 2003, 15:10
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import expressions.*;
import java.awt.*;

/**
 *
 * @author  Patrice Maupou
 */
public class GPoint extends GObject {
  
  
  /** Creates a new instance of GPoint */
  public GPoint() {
  }
  
  public GPoint(Numeric x, Numeric y) {
    this.x = x;
    this.y = y;
    drawGObject = true;
    drawName = true;
  }  
  
  public GPoint(int x, int y) {
    this(new Numeric(x), new Numeric(y));
  }
  
  public GPoint(Point P, GBase b) {
    this(new Numeric((P.x-b.x0)/b.dx), new Numeric((P.y-b.y0)/b.dy));
    base = b;
  }
  
  /** intersection de droites
   */
  public GPoint(GLine D1, GLine D2) {
    GPoint[] DP1 = D1.getPoints(), DP2 = D2.getPoints();
    double xa = DP1[0].x.toDouble(), xb = DP1[1].x.toDouble(), xc = DP2[0].x.toDouble(), xd = DP2[1].x.toDouble();
    double ya = DP1[0].y.toDouble(), yb = DP1[1].y.toDouble(), yc = DP2[0].y.toDouble(), yd = DP2[1].y.toDouble();
    double d = (xa-xb)*(yc-yd)-(xc-xd)*(ya-yb);
    if(d != 0) {
      double k = ((xa-xc)*(yc-yd)-(xc-xd)*(ya-yc))/d;
      this.x = new Numeric(k*(xb-xa)+ xa);
      this.y = new Numeric(k*(yb-ya)+ ya);
      drawGObject = true;
      drawName = true;
    }
  }
  
  public Point scale(GBase base) {
    return new Point((int)(x.toDouble()*base.dx + base.x0), (int)(y.toDouble()*base.dy + base.y0));
  }
    
  @Override
  public void paint(Graphics g) {
    if(x == null || y == null) return;
    Color save = g.getColor();
    if(color == null) color = save;
    g.setColor(color);
    Point p = base.scale(this);
    if(drawGObject) {
      if(symbol.isEmpty()) g.fillOval(p.x - 2, p.y - 2, 4, 4);
      else {
          for (Object sy : symbol) {
              // définir la base
              GObject Symbol = (GObject) sy;
              Symbol.base = new GBase(base.clip, 0, p.x, p.y, 1, 1);
              Symbol.paint(g);
          }
      }
    }
    if(drawName) {
      try {
        BoxMetrics metrics = labelBox.getBoxMetrics();
        int bw = metrics.width, bh = metrics.height, bl = metrics.getBaseLine();
        float x = p.x, y = p.y;
        if(!Double.isNaN(PosAngle)) {
          double c = Math.cos(PosAngle*Math.PI/180), s = Math.sin(PosAngle*Math.PI/180);
          x = (float)(x - (bw/2 + 6)*(1-c) + 6);
          y = (float)(y + (1 + s)*(bl - bh));
        } else {
          x = x - bw/2;
          y = y - bh/2;
        }
        labelBox.paint(g, metrics , Math.round(x), Math.round(y));
      } catch(Exception exc) {}
    }
    g.setColor(save);
  }
  
  @Override
   public GPoint[] getPoints() {
     return new GPoint[] {this};
   }

  @Override
public Point[] getPointsRelative(Point p, double e) {
    Point pt = scale(base);
    Point d = new Point(pt.x-p.x, pt.y-p.y);
    if(d.x*d.x + d.y*d.y > e*e) return null;
    else return new Point[] {d};
  }
  
  
  @Override
public String toString() {
    String s = label.printout(false);
    if(x != null && y != null) {
      s = "point(" + s + "," + x.toString() + "," + y.toString() + ")";
    }
    return s;
  }
  
  
  @Override
public void setView() { // agrandit éventuellement la vue
    if(base != null) {
      Point P = base.scale(this);
      base.view.add(new Rectangle(P.x-20, P.y-20, 40, 40));
    }
  }

  public Numeric x, y;
  public Expression label = Expression.NULL;
  public SystemBox labelBox;
  public double PosAngle = 90;
  public int fontsize = 11, hotx, hoty;
  public boolean drawName;
  
}

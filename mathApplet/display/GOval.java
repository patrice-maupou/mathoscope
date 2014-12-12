/*
 * GOval.java
 *
 * Created on 18 novembre 2004, 15:03
 */

package display;

import expressions.*;
import java.awt.*;

/**
 *
 * @author  Patrice Maupou
 */
public class GOval extends GObject{
  
  private GPoint center;
  private Numeric ra, rb;
  private int startAngle, arcAngle;
  
  /** 
   * Création d'une ellipse
   * @param center
   * @param ra
   * @param rb
   */
  public GOval(GPoint center, Numeric ra, Numeric rb) {
    this.center = center;
    this.ra = ra;
    this.rb = rb;
    startAngle = 0;
    arcAngle = 360;
  }
  
  /**
   * Crée un arc d'ellipse
   * @param center le centre
   * @param ra le rayon horizontal
   * @param rb le second rayon
   * @param sa l'angle de départ
   * @param sb l'angle d'ouverture
   */
   public GOval(GPoint center, Numeric ra, Numeric rb, int sa, int sb) {
    this.center = center;
    this.ra = ra;
    this.rb = rb;
    startAngle = sa;
    arcAngle = sb;
  }
 
  
  public void paint(Graphics g) {
    Color save = g.getColor();
    if(color == null) color = save;
    g.setColor(color);
    Point C = base.scale(center);
    int a = Math.abs((int)(ra.toDouble()*base.dx)), b = Math.abs((int)(rb.toDouble()*base.dy));
    if(arcAngle == 360) {
      g.drawOval(C.x-a, C.y-b, 2*a, 2*b);
      if(fillcolor != null) {
        g.setColor(fillcolor);
        g.fillOval(C.x-a, C.y-b, 2*a, 2*b);
      }
    }
    else {
      g.drawArc(C.x-a, C.y-b, 2*a, 2*b, startAngle, arcAngle); 
      if(fillcolor != null) {
        g.setColor(fillcolor);
        g.fillArc(C.x-a, C.y+b, 2*a, 2*b, startAngle, arcAngle);
      }
    }
    g.setColor(save);
  }
    
  
  @Override
public GPoint[] getPoints() {
    return new GPoint[]{center};
  }
  
  @Override
public String toString() {
    String ret = "(" + center.toString() + ", "+ ra.printout(false);
    ret += rb.printout(false);
    if(arcAngle < 360) 
      return "arc" + ret + ", " + startAngle + ", " + arcAngle + ")";
    if(ra.toDouble() != rb.toDouble()) ret = "ellipse" + ret + ")";
    else ret = "cercle" + ret + ")";
    return ret;
  }
  
}

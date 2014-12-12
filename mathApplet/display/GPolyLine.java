/*
 * GPolyLine.java
 *
 * Created on 21 août 2004, 12:13
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import expressions.*;
import java.awt.*;

/** ligne brisée définie par une liste de points
 *
 * @author  Patrice Maupou
 */
public class GPolyLine extends GObject {
  
  private GPoint[] points;
  private boolean showpoints;
  
  /** Creates a new instance of GPolyLine 
   * @param points 
   * @param closed 
   */
  public GPolyLine(GPoint[] points,  boolean closed) {
    this.points = points;
    drawGObject = true;
    this.closed = closed;
    this.showpoints = true;
  }
  
  
  public void paint(Graphics g) {
    Color save = g.getColor();
    if(color == null) color = save;
    g.setColor(color);
    int n = points.length;
    int[] x = new int[n], y = new int[n];
    for(int i = 0; i < n; i++) {
      if(showpoints) {
        if(!symbol.isEmpty()) points[i].symbol = symbol;
        points[i].base = base;
        points[i].paint(g);
      }
      Point P = base.scale(points[i]);
      x[i] = P.x;
      y[i] = P.y;
    }
    if(closed) {
      if(fillcolor != null) {
        g.setColor(fillcolor);
        g.fillPolygon(x, y, n);
        g.setColor(color);
      }
      if(drawGObject) g.drawPolygon(x, y, n);
    }
    else if(drawGObject) g.drawPolyline(x, y, n);
    g.setColor(save);
  }

   
  @Override
public void setView() {
    for (int i = 0; i < points.length; i++) {points[i].setView();}
  }
  
  @Override
public String toString() {
    String ret = (closed)? "polygone(" : "ligne(";
    for(int i = 0; i < points.length; i++) {
      ret += points[i].toString();
      if(i < points.length-1) ret += ",";
    }
    return ret + ")";
  }

  /**
   * Setter for property showpoints.
   * @param showpoints New value of property showpoints.
   */
  public void setShowpoints(boolean showpoints) {
    this.showpoints = showpoints;
  }
  
  /**
   * Getter for property points.
   * @return Value of property points.
   */
  @Override
public GPoint[] getPoints() {
    return this.points;
  }
  
  /**
   * Setter for property points.
   * @param points New value of property points.
   */
  public void setPoints(display.GPoint[] points) {
    this.points = points;
  }

}

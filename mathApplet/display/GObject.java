/*
 * GObject.java
 *
 * Created on 25 juin 2003, 15:50
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import expressions.*;

import java.awt.*;
import java.util.ArrayList;

/**
 *
 * @author  Patrice Maupou
 */
public abstract class GObject {
  
  public Color color, fillcolor;
  public boolean drawGObject = true, closed;
  public ArrayList symbol = new ArrayList();
  public GBase base;
 
  public abstract void paint(Graphics g);
    
  public void setView() {};
        
  public Color getColor() {
    return this.color;
  }
  
  public void setColor(Color color) {
    this.color = color;
  }
  public int type = 0;
  public Numeric x;

  public GPoint[] getPoints() {
    return new GPoint[0];
  }   

  public java.awt.Point[] getPointsRelative(java.awt.Point p, double e) {
    return null;
  }
       
}

/*
 * GBase.java
 * Created on 29 août 2006, 14:26 
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import expressions.Expression;
import expressions.Numeric;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * @author Patrice Maupou
 */
public class GBase extends GObject {
  
  /** Creates a new instance of GBase 
   * @param frame 
   * @param shift 
   * @param x0 
   * @param y0 
   * @param dx 
   * @param dy 
   */
  public GBase(Rectangle frame, int shift, int x0, int y0, double dx, double dy) {
    int w = frame.width, h = frame.height;
    clip = new Rectangle(0, shift, w, h);
    this.x0 = x0;
    this.y0 = y0;
    this.dx = dx;
    this.dy = dy;
    view = new Rectangle(x0 - 2, y0 - 2, 5, 5);
  }
  
  public GBase(Rectangle frame, int shift) { // base par défaut
    this(frame, shift, frame.width/2, frame.height/2 + shift, 20, -20);
  }
  
  
  public GBase(Expression[] r, int shift, Rectangle frame) {
    int[] ns = Numeric.getInts(r);
    clip = new Rectangle(ns[0], ns[1] + shift, ns[2], ns[3]);
    clip = frame.intersection(clip);
    x0 = clip.x + clip.width/2;
    y0 = clip.y + clip.height/2;
    dx = 20;
    dy = -20;
    view = new Rectangle(x0 - 2, y0 - 2, 5, 5);
  }  
  
  public void setOrigin(Expression[] o, int shift) {
    x0 = ((Numeric)o[0]).toInt();
    y0 = ((Numeric)o[1]).toInt() + shift;
  }
  
  public void setUnits(Expression[] u) {
    dx = ((Numeric)u[0]).toDouble();
    dy = ((Numeric)u[1]).toDouble();
  }
  
  public void setFillColor(Expression[] c) {
    int[] ns = Numeric.getInts(c);
    fillcolor = new Color(ns[0], ns[1], ns[2]);
  }
  
  public void setScale(Expression n) {
    resize = true;
    scale = (n instanceof Numeric)? ((Numeric)n).toInt() : 0 ;
  }
    
  public void paint(Graphics g) {
    g.setClip(clip);
    if(resize) {
      double rx = ((double)clip.width-scale) / ((double)view.width);
      double ry = ((double)clip.height-scale) / ((double)view.height);
      if(!view.equals(clip)) {
        x0 = (int)((x0 - view.x)*rx + clip.x + scale/2);
        y0 = (int)((y0 - view.y)*ry + clip.y + scale/2);
        dx = dx * rx;
        dy = dy * ry;
      }
    }
    if(fillcolor != null) {
      Color save = g.getColor();
      g.setColor(fillcolor);
      g.fillRect(clip.x, clip.y, clip.width, clip.height);
      g.setColor(save);
    }
    resize = false;
  }

  /** renvoie le point de l'écran défini par le GPoint P dans le repère d'origine 
   *  (x0,y0) et de graduations (dx,dy)
   * @param p le GPoint
   * @return le point dans les coordonnées d'origine
   */
  public Point scale(GPoint p) {
    return new Point((int)(p.x.toDouble()*dx + x0), (int)(p.y.toDouble()*dy + y0));
  }

 
    
  @Override
public String toString() {
    return "origine : " + x0 + " " + y0 + " , unités :"+ dx + " " + dy
            +" clip :"+clip.x+" "+clip.y+" "+clip.width+" "+clip.height;
  }
    
  
  public int x0, y0, scale; // origine, unités, marges
  public double dx, dy;
  public Rectangle clip, view;
  public boolean resize = false;

}

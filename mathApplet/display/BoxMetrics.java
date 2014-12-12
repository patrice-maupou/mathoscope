/*
 * BoxMetrics.java
 * after Klaus Hartlage (HartMath Plugin)
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import java.awt.*;

/** on ajoute une ligne de base à une boîte de largeur et de hauteur données
 */
public class BoxMetrics extends Dimension {
  
  public BoxMetrics(int width, int height) {
    super(width, height);
    baseLine = 0;
  }
  
  public BoxMetrics(int width, int height, int baseLine) {
    super(width, height);
    this.baseLine = baseLine;
  }
  
  public int getBaseLine() {
    return baseLine;
  }
  
  public void setBaseLine(int baseLine) {
    this.baseLine = baseLine;
  }
  
  public int baseLine;
}
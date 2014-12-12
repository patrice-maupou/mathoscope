/*   Box.java
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import java.awt.Graphics;
import java.util.*;

/**  C'est la classe de base du package, le moule pour les autres classes
 */

public abstract class Box {
  
  public Box() {
  }
  
  
  public BoxMetrics getBoxMetrics() {
    return size;
  }
  
  public abstract void paint(Graphics g, BoxMetrics size, int i, int j);
  
  
  public int getFontSize() { return fontSize; }
  
  
  public BoxMetrics size;
  public int fontSize = FONT_SIZE;
  public static String FONT_NAME = "SansSerif";
  public static int FONT_SIZE = 14;
  
}
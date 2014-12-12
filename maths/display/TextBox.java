/* Source File Name:   TextBox.java
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package display;

import java.awt.*;


public class TextBox extends Box {
    
  public TextBox(Object obj, Graphics g, int fontSize) {
    this(obj, g, new Font(g.getFont().getName(), Font.PLAIN, fontSize));
    this.fontSize = fontSize;
  }

  /**
   * méthode plus générale
   * @param obj à transformer en texte
   * @param g contexte graphique
   * @param font
   */
  public TextBox(Object obj, Graphics g, Font font) {
    this.font = font;
    this.text = obj.toString();
    if(text.startsWith("`") && text.endsWith("`")) text = text.substring(1, text.length()-1);
    else if("infini".equals(text)) text = "+\u221E";
    else if("-infini".equals(text)) text = "-\u221E";
    else if("PI".equals(text)) text = "\u03c0";
    fm = g.getFontMetrics(font);
    int height = fm.getHeight(), demi = fm.stringWidth("|");
    int width = (text.equals("caret"))? demi : fm.stringWidth(text);
    if(text.equals("emsp")) width = demi/4;
    size = new BoxMetrics(width, height, height / 2);
    leading_ascent = fm.getLeading() + fm.getAscent();
  }
  
  
  
  @Override
  public void paint(Graphics g, BoxMetrics size, int x, int y) {
    g.setFont(font);
    if(!text.equals("caret") && !text.equals("emsp")) 
      g.drawString(text, x, y + leading_ascent);
  }
 
  private String text;
  private Font font;
  FontMetrics fm;
  private int leading_ascent;
}
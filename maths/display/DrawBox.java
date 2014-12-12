package display;
/*
 * Created on 11 mai 2005
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;


/**
 * @author Patrice Maupou
 * Boîte pouvant s'étendre, angle, accolades, intégrale .., 
 * le dessin est dans une boîte carrée de 60 sur 60.
 * Exemple de vecteur :
 * {droite(point(50,0),point(50,100))}
 * {point(50,0),caché,style(ligne(point(0,0),point(0,10),point(-10,10)}
 */
public class DrawBox extends Box {
	
  private ArrayList fig;
  private String bracket = "";
  
  /**
   * dessin dans une boîte de dimensions données
   * @param dims largeur, hauteur et alignement
   * @param fig la liste des objets graphiques
   */
  public DrawBox(int[] dims, ArrayList fig) {
    this.fig = fig;
    size = new BoxMetrics(dims[0], dims[1],dims[2]);
  }
  
  /**
   * dessin d'une parenthèse, crochet, etc..
   * @param box la boîte de référence à parenthéser
   * @param bracket le type de parenthèse : () [] {}
   */
  public DrawBox(Box box, String bracket) {
    BoxMetrics BM = box.getBoxMetrics();
    size = new BoxMetrics(10, BM.height, BM.baseLine);
    this.bracket = bracket;
  }
  
  /**	 *
   * @param box boîte le long de laquelle on dessine
   * @param fig la liste des objets graphiques
   * @param width l'épaisseur du dessin
   * @param dir horizontal si positif, vertical si négatif
   */
  public DrawBox(Box box, ArrayList fig, int width, int dir) {
    this.fig = fig;
    BoxMetrics BM = box.getBoxMetrics();
    size = new BoxMetrics(BM.width, BM.height, BM.baseLine);
    if(dir > 0) size.width = width;
    else size.height = width;
  }
  
   
  /** dessine une flèche sur la boîte (pour un vecteur)
   */
  public static DrawBox arrowBox(Box box) {
    ArrayList<GObject> arrow = new ArrayList<GObject>(), symbol = new ArrayList<GObject>();
    GPoint[] pts = new GPoint[]{new GPoint(-2,2),new GPoint(0,0),new GPoint(-2,-2)};
    GPolyLine tip = new GPolyLine(pts, false);
    tip.setShowpoints(false);
    symbol.add(tip);
    GPoint end = new GPoint(60,60);
    end.symbol = symbol;
    arrow.add(new GLine(new GPoint(0,60), end));
    arrow.add(end);
    return new DrawBox(box, arrow, 4, -1);
  }
  
  /**
   * dessins prédéfinis
   * @param box
   * @param figname nom du dessin
   * @param width
   * @param dir
   * @return la boîte du dessin
   */
  public static DrawBox get(Box box, String figname, int width, int dir) {
    ArrayList<GObject> fig = new ArrayList<GObject>(), style = new ArrayList<GObject>();
    if(figname.equals("radg")) {
      GPoint start = new GPoint(0,40), end = new GPoint(60,0);
      GPoint[] pts = new GPoint[] {start,new GPoint(30,60), end};
      GPolyLine line = new GPolyLine(pts, false);
      line.setShowpoints(false);
      fig.add(line);
      style.add(new GLine(new GPoint(0,0), new GPoint(-2,0)));
      start.symbol = style;
      fig.add(start);
    } else if(figname.equals("radh")) {
      fig.add(new GLine(new GPoint(0,0), new GPoint(60,0)));
    }
    return new DrawBox(box, fig, width, dir);
  }
  
  /** dessine toutes sortes de parenthèses
   */
  private void drawBracket(Graphics g, BoxMetrics size, int x, int y) {
    int h = Math.round(size.height/2);
    if(bracket.equals("("))
      g.drawArc(x+2 ,y , 9, size.height, 100, 160);
    else if(bracket.equals("|"))
      g.drawLine(x+4, y, x+4, y+size.height);
    else if(bracket.equals("[")) {
      g.drawLine(x+6, y, x+2, y);
      g.drawLine(x+2, y, x+2, y+size.height);
      g.drawLine(x+2, y+size.height, x+6, y+size.height);
    } else if(bracket.equals("{")) {
      g.drawArc(x+4, y, 4, 6, 100, 80);
      g.drawLine(x+4, y+3, x+4, y+h-3);
      g.drawArc(x, y+h-5, 4, 5, 0, -90);
      g.drawArc(x, y+h, 4, 4, 90, -90);
      g.drawLine(x+4, y+h+3, x+4, y+size.height-3);
      g.drawArc(x+4, y+size.height-4, 4, 4, 180, 90);
    } else if(bracket.equals(")"))
      g.drawArc(x, y, 7, size.height, 80, -160);
    else if(bracket.equals("]")) {
      g.drawLine(x+2, y, x+6, y);
      g.drawLine(x+6, y, x+6, y+size.height);
      g.drawLine(x+6, y+size.height, x+2, y+size.height);
    } else if(bracket.equals("}")) {
      g.drawArc(x, y, 4, 4, 90, -90);
      g.drawLine(x+4, y+3, x+4, y+h-3);
      g.drawArc(x+4, y+h-4, 4, 4, 180, 90);
      g.drawArc(x+4, y+h, 4, 6, 100, 80);
      g.drawLine(x+4, y+h+3, x+4, y+size.height-3);
      g.drawArc(x, y+size.height-5, 4, 5, 0, -90);
    }
  }
  
  /** on dessine les éléments du dessin dans fig, le cadre est (x,y), 
   * les unités dx et dy dépendent de la taille de la boîte
   * @param g 
   * @param size 
   * @param x 
   * @param y 
   */
  public void paint(Graphics g, BoxMetrics size, int x, int y) {
    if(bracket.equals("")) {
      Rectangle cadre = new Rectangle(x, y, size.width, size.height);
      GBase base = new GBase(cadre, 0, x, y, size.width/60.0, size.height/60.0);
      for(int i = 0; i < fig.size(); i++) {
        GObject gObj = (GObject) fig.get(i);
        gObj.base = base;
        gObj.paint(g);
      }
    } else drawBracket(g, size, x, y);
  }

}

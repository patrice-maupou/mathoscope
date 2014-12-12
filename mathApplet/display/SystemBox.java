/*
 * Created on 27 mai 2005
 *
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */
package display;

import java.awt.Rectangle;
import root.GeomPanel;
import expressions.Expression;
import static expressions.Expression.*;
import expressions.Numeric;
import expressions.Relation;
import java.awt.Color;
import java.awt.Graphics;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;

/**
 * Boîte englobant des boîtes situées à des points donnés
 * @author Patrice Maupou
 * 
 */

public class SystemBox extends Box {
  
  private ArrayList<Box> boxes; // liste des boîtes
  private ArrayList<int[]> coors; // liste des coordonnées des points d'ancrage
  private static SystemBox activeBox;
  private int active = 0;
  private int[] frame; // cadre
  private Expression expr; 
  private GeomPanel GPanel;
  private boolean hot = false; // vrai si l'expression est active
  
  public SystemBox() {
    boxes = new ArrayList<Box>();
    coors = new ArrayList<int[]>();
  }
  
  /**
   * Fixe la boîte principale du système
   * @param base première boîte du système
   */
  public SystemBox(Box base) {
    this();
    boxes.add(base);
    BoxMetrics b = base.getBoxMetrics();
    coors.add(new int[]{0,0});
    size = new BoxMetrics(b.width, b.height, b.baseLine);
    upActiveBox(base);
  }
  
  /**
   * Fixe la boîte principale du système
   * @param base première boîte du système
   * @param insets marges
   */
  public SystemBox(Box base, int[] insets) {
    this(base);
    size.width += insets[1] + insets[3];
    size.height += insets[0] + insets[2];
    size.baseLine += insets[0];
    coors.set(0, new int[]{insets[1],insets[0]});
  }
  
  
  
  /**
   * transforme en une expression à afficher en gardant les références aux variables initiales
   * @param e l'expression à transformer
   * @param displays les règles de transformation
   * @return l'expression à afficher avec les variables liées à celles de e
   */
  public static Expression codeDisplays(Expression e, Expression displays) {
    Expression ret = e;
    if(displays != null && displays.R instanceof Expression[]) {
      Expression[] rules = (Expression[])displays.R;
      for (int i = 0; i < rules.length; i++) {
        ArrayList<Expression> vars = new ArrayList<Expression>();
        Expression rule = rules[i].copy();
        Expression[] conds = (Expression[])rule.R;
        Expression display = (Expression) rule.L; // l'affichage
        Relation cond = null;
        for (int j = 0; j < conds.length; j++) {
          cond = (Relation)conds[j];
          if(var("image").equals(cond.R)) break;
        }
        cond.R = e; // remplace image par e : vec(A,B)->vec(M,P)
        try {
          if(cond.valueOf(vars, 0, -100)) { // la liste vars contient les variables de remplacement 
            ArrayList<Expression> evars = ((Expression)cond.L).extractVars();
            for (int j = 0; j < evars.size(); j++) {
              evars.set(j, (Expression)vars.get(j).R);              
            }       
            ret = copyref(display, vars); 
          }
        } catch(Exception ex){} 
      }
    }
    return ret;
  }

  
  /**
   * retourne une copie de l'expression e avec des variables identiques à celles de la liste
   * @param e
   * @return la copie
   */
  public static Expression copyref(Expression e, List<Expression> vars) {
    Expression ret = e;
    if(e.op == VAR) {
      if(vars.contains(e)) {ret = (Expression)vars.get(vars.indexOf(e)).R;}
      return ret;
    }
    Object nL = e.L, nR = e.R;
    if(nL instanceof Expression) nL = copyref((Expression)nL, vars);
    if(nR instanceof Expression) nR = copyref((Expression)nR, vars);
    else if(nR instanceof Expression[]) {
      Expression[] Rs = (Expression[])nR;
      Expression[] nRs = new Expression[Rs.length];
      for (int i = 0; i < Rs.length; i++) {
        nRs[i] = copyref(Rs[i], vars);
      }
      nR = nRs;
    }
    return build(e.op, nL, nR);
  }
  
  
  /** une boîte contenant le tracé de l'expression
   * @param expr
   * @param displays
   * @param GPanel 
   * @param size la taille de la fonte utilisée
   * @param brackets s'il faut mettre entre parenthèses ou pas
   * @param hot vrai si l'expression est active
   * @return la boîte à dessiner
   */
  public static SystemBox exprBox(Expression expr, Expression displays, GeomPanel GPanel, int size, 
    boolean brackets, boolean hot) {
    Graphics g = GPanel.getGraphics();
    Expression e = codeDisplays(expr, displays);
    int place = e.place;
    SystemBox box = new SystemBox(), lbox, rbox;
    String bracketType = "()", s = "", temp;
    if (e.op < IS) {
      if (e.op == NUL) {
        if(e == Relation.TRUE) box = new SystemBox(new TextBox("VRAI", g, size), new int[] {1,1,1,1});
        else if(e == Relation.FALSE) box = new SystemBox(new TextBox("FAUX", g, size), new int[] {1,1,1,1});
        return box;
      }
      else if (e.op == VAR || e.op == INT || e.op == DEC) {
        box = new SystemBox(new TextBox(e.L, g, size), new int[] {1,1,1,1});
        if(e.equals(var("caret"))) {
          int status = (e.R == null)? -2 : 2;
          box.setActiveBox(box, status);
        }
      } else if(e.op == FUNC && ("BLOC".equals(e.L) || "INVISIBLE".equals(e.L) || "STOP".equals(e.L))) {
        Expression[] coors = (Expression[])e.R;
        box = exprBox(coors[0], displays, GPanel, size, false, hot);
      } else if(e.op == FUNC && "CADRE".equals(e.L)) { // encadrer l'expression
        Expression[] coors = (Expression[])e.R;
        box = exprBox(coors[0], displays, GPanel, size, false, hot);
        if(coors.length > 1 && coors[1] instanceof Numeric) {
          int[] ints = new int[] {((Numeric)coors[1]).toInt(),-1,-1,-1,-1};
          for (int i = 2; i < coors.length; i++) {
            if(coors[i] instanceof Numeric) ints[1] = ((Numeric)coors[i]).toInt();
            else if(coors[i].op == Expression.FUNC && "couleur".equals(coors[i].L)) {
              Expression[] levels = (Expression[])coors[i].R;
              for (int j = 0; j < 3; j++) {ints[j+2] = ((Numeric)levels[j]).toInt();}
            }
          }
          box.setFrame(ints);
        }
      }  
      else if(e.op == FUNC && "ARROW".equals(e.L)) {
        box = new SystemBox(exprBox(((Expression[])e.R)[0], displays, GPanel, size, false, hot));
        //box.arrowBox();
        box.add(DrawBox.arrowBox(box), "NORD");
      }
      else if(e.op == FUNC && "MARGES".equals(e.L)) {
        Expression[] coors = (Expression[])e.R;
        box = exprBox(coors[0], displays, GPanel, size, false, hot);
        box = new SystemBox(box, Numeric.getInts((Expression[])coors[1].R));
      } else if(e.op == FUNC && "UNICODE".equals(e.L)) {
        Expression[] coors = (Expression[])e.R;
        if(coors[0].op == VAR) {
          String text = (String)coors[0].L;
          int n = Integer.parseInt(text, 16);
          char[] chars = Character.toChars(n); // version 5 !
          text = String.copyValueOf(chars);
          box = new SystemBox(new TextBox(text, g, size), new int[]{1,1,1,1});
        }
      } else if(e.op == FUNC && "TBOX".equals(e.L)) {
        Expression[] coors = (Expression[])e.R, c2;
        for(int i = 0; i < coors.length; i++) {
          if(coors[i].op == JUX || coors[i].equals(var("caret"))) {
            // TBOX(a,b,JUX(c,d),x) -> JUX(JUX(TBOX(a,b),JUX(c,d)),x)
            // TBOX(a,b,|,x) -> JUX(JUX(TBOX(a,b),|),x)
            Expression e2 = coors[i];
            if(i > 0) { // rassembler avant le JUX ou le caret
              c2 = new Expression[i];
              System.arraycopy(coors, 0, c2, 0, i);
              e2 = build(JUX, build(FUNC, "TBOX", c2), e2);
            }
            if(i < coors.length-1) { // rassembler après
              c2 = new Expression[coors.length-i-1];
              System.arraycopy(coors, i+1, c2, 0, coors.length-i-1);
              e2 = build(JUX, e2, build(FUNC, "TBOX", c2));
            }
            return exprBox(e2, displays, GPanel, size, false, hot);
          }
          if(coors[i].L.equals("TBOX"))
            temp = ((Expression[])coors[i].R)[0].toString();
          else temp = coors[i].toString();
          if(temp.startsWith("`") && temp.endsWith("`"))
            temp = temp.substring(1, temp.length()-1);
          s += temp;
        }
        box = new SystemBox(new TextBox(var(s), g, size), new int[]{1,1,1,1});
        if(" ".equals(s)) box.setActive(-4); // FIXME ne fonctionne pas bien
      } 
      else if(e.op == FUNC && "ABOX".equals(e.L)) { //Ajoute une boîte SystemBox
        //ABOX(PPbox sbox, (point(),point(),PPbox abox, int size, <int rang>),..)
        try {
          Expression[] coors = (Expression[])e.R, params;
          box = new SystemBox(exprBox(coors[0], displays, GPanel, size, brackets, hot));
          for(int i = 1; i < coors.length; i++) {
            int rang = 0; // rang de la boîte système à laquelle attacher
            params = (Expression[])coors[i].R;
            if(params.length == 5) rang = ((BigInteger)params[4].L).intValue();
            int asize = ((BigInteger)params[3].L).intValue();
            Box abox = exprBox(params[2], displays, GPanel, asize, brackets, hot);
            int[] ptcoors = Numeric.getInts((Expression[])params[0].R);
            GPoint sPt = new GPoint(ptcoors[0], ptcoors[1]);
            ptcoors = Numeric.getInts((Expression[])params[1].R);
            GPoint aPt = new GPoint(ptcoors[0], ptcoors[1]);
            box.add(rang, sPt, aPt, abox);
          }
        } catch(Exception exc) {System.out.println(exc.getMessage());}
      } 
      else if(e.op == FUNC && "DRAW".equals(e.L)) { // DRAW(expr, fig, width, dir) ou DRAW(DIMS(w,h,b), fig>)
        Expression[] coors = (Expression[])e.R;
        Expression figname = coors[1];
        ArrayList fig = new ArrayList();
        int idx = decofigs.indexOf(figname);
        if(idx != -1) fig = (ArrayList)(decofigs.get(idx)).R;
        DrawBox deco;
        if(coors[0].op == FUNC && "DIMS".equals(coors[0].L))
          deco = new DrawBox(Numeric.getInts((Expression[])coors[0].R), fig);
        else {
          int width = ((Numeric)coors[3]).toInt();
          int dir = ((BigInteger)coors[3].L).signum();
          SystemBox draw = exprBox(coors[0], displays, GPanel, size, false, hot);
          deco = new DrawBox(draw, fig, width, dir);
        }
        box.add(deco, "");
      } 
      else if(e.op == FUNC && "DECO".equals(e.L)) { // DECO(dir,expr,fig,width)
        Expression[] coors = (Expression[])e.R;
        ArrayList fig = new ArrayList();
        int idx = decofigs.indexOf(coors[2]);
        if(idx != -1) fig = (ArrayList)(decofigs.get(idx)).R;
        box = new SystemBox(exprBox(coors[1],null, GPanel, size, false, hot));
        int dir = ("SUD".equals(coors[0].L) || "NORD".equals(coors[0].L))? -1 : 1;
        DrawBox deco = new DrawBox(box, fig, ((Numeric)coors[3]).toInt(), dir);
        box.add(deco, (String)coors[0].L);
      } 
      else if (e.R instanceof Expression[]) { // fonction ou suite
        Expression[] coors = (Expression[])e.R;
        if("VBOX".equals(e.L) && coors[0].op == SEQ) coors = (Expression[])coors[0].R;
        int nsize = (e.op == SER)? size-2 : size;
        String comma = (e.op == SER)? "," : ", ";
        for (int i = 0; i < coors.length; i++) {
          if("VBOX".equals(e.L)) {
            box.add(exprBox(coors[i], displays, GPanel, nsize, false, hot), "SUD");
          } else if("VLBOX".equals(e.L)) {
            box.add(exprBox(coors[i], displays, GPanel, nsize, false, hot), "SUDOUEST");
          } else { // tous les autres cas : fonctions, suites, séquences, hbox
            box.add(exprBox(coors[i], displays, GPanel, nsize, coors[i].op == SEQ, hot), "EST");
            if (i < coors.length - 1 && !"HBOX".equals(e.L))
              box.add(new TextBox(comma, g, nsize), "EST");
          }
        }
        if("VBOX".equals(e.L) || "VLBOX".equals(e.L))
          box.getBoxMetrics().baseLine = box.getBoxMetrics().height/2;
        else if(e.op == SET) {
          if (e.L instanceof Expression) {// {x€A,y€B : x-y}
            box.add(new TextBox(" : ", g, size), "EST");
            Expression l = (Expression)e.L;
            box.add(exprBox(l, displays, GPanel, size, l.op == SEQ, hot), "EST");
            box = box.getBrackets("{}", nsize, GPanel);
          } else {
            if(coors.length == 0)
              box = new SystemBox(new TextBox("Ø",g,size),new int[]{1,1,1,1});
            else {
              String type = (e.L instanceof String)? (String)e.L : "{}";
              box = box.getBrackets(type, nsize, GPanel);
            }
          }
        } else if(e.op == SER) { // pour les indices
          SystemBox sbox = new SystemBox(new TextBox(e.L, g, size));
          sbox.add(0, new GPoint(60, 40), new GPoint(0, 0), box);
          box = sbox;
        } else if("||".equals(e.L)) {
          box = box.getBrackets("||", size, GPanel);
        } else if("rac".equals(e.L)) {
          box = new SystemBox(box, new int[]{0,2,0,1});
          box.add(DrawBox.get(box, "radh", 2, -1), "NORD");
          box = new SystemBox(box);
          box.add(DrawBox.get(box, "radg", 6, 1), "OUEST");
          box = new SystemBox(box, new int[]{1,2,1,0});
        } else if("BBOX".equals(e.L)) { // accolades
          if(coors.length == 2) bracketType = (String)coors[1].L;
          box = exprBox(coors[0], displays, GPanel, nsize, false, hot).getBrackets(bracketType, nsize, GPanel);
        } else if(e.op != SEQ && !"HBOX".equals(e.L)) {//fonction normale
          box = new SystemBox(box.getBrackets("()", nsize, GPanel));
          Box nameBox;
          if(e.L instanceof String) nameBox = new TextBox(e.L, g, size);
          else nameBox = exprBox((Expression)e.L, displays, GPanel, size, false, hot);
          box.add(nameBox, "OUEST");
        }
      }
    } 
    else box = binaryBox(e, displays, GPanel, size, false, hot);
    if (brackets) box = box.getBrackets(bracketType, size, GPanel);
    e.parent = expr.parent;
    e.place = place;
    box.expr = expr;
    box.GPanel = GPanel;
    box.hot = hot;
    return box;
  }
  
  /**
   * retourne une boîte contenant une opération binaire (a+b, a-b, ..)
   * @param e
   * @param displays
   * @param GPanel
   * @param size
   * @param brackets
   * @param hot
   * @return la boîte
   */
  public static SystemBox binaryBox(Expression e, Expression displays, GeomPanel GPanel, int size,
    boolean brackets, boolean hot) {    
    SystemBox box = new SystemBox(), lbox, rbox;
    Expression l = (Expression) e.L, r = (Expression) e.R;
    if (e.op == DIV) {
      lbox = exprBox(l, displays, GPanel, size, false, hot);
      rbox = exprBox(r, displays, GPanel, size, false, hot);
      if (l.op == DIV) {lbox = lbox.getBrackets("  ", size, GPanel);}
      if (r.op == DIV) {rbox = rbox.getBrackets("  ", size, GPanel);}
      int mw = lbox.getBoxMetrics().width;
      if (mw < rbox.getBoxMetrics().width) {mw = rbox.getBoxMetrics().width;}
      ArrayList<GObject> line = new ArrayList<GObject>();
      line.add(new GLine(new GPoint(0, 30), new GPoint(60, 30)));
      box = new SystemBox(new DrawBox(new int[]{mw, 4, 1}, line), new int[]{0, 2, 0, 2});
      box.add(0, new GPoint(30, 0), new GPoint(30, 60), lbox);
      box.add(0, new GPoint(30, 60), new GPoint(30, 0), rbox);
    } else if (e.op == EXP) {
      box = new SystemBox(exprBox(l, displays, GPanel, size, l.op > IS && l.op != JUX, hot));
      box.add(0, new GPoint(60, 30), new GPoint(0, 60), exprBox(r, null, GPanel, size - 2, false, hot));
    } else if (e.op == JUX) {
      if ("caret".equals(l.L)) { // caret à gauche
        int active = "blanc".equals(l.R) ? -1 : 1;
        box = exprBox(r, displays, GPanel, size, false, hot);
        box.setActiveBox(box, active);
      } else if ("caret".equals(r.L)) { // caret à droite
        int active = "blanc".equals(r.R) ? -3 : 3;
        box = exprBox(l, displays, GPanel, size, false, hot);
        box.setActiveBox(box, active);
      } else {
        /* avant modif 10 juillet 2008
        box = exprBox(l, displays, GPanel, size, (l.op == SEQ || l.op > IS) && !l.L.equals("TBOX") && l.op != JUX, hot);
        box = new SystemBox(box);
        box.add(exprBox(r, displays, GPanel, size, (r.op == SEQ || r.op > IS) && !r.L.equals("TBOX"), hot), "EST");
         //*/
        //* modif 10 juillet 2008
        box = exprBox(l, displays, GPanel, size, false, hot);
        box = new SystemBox(box);
        box.add(exprBox(r, displays, GPanel, size, false, hot), "EST");
        //*/        
      }
    } else {
      if (!e.isNegative() && e.op != NOT) { // pas de -<expr>
        box = new SystemBox(exprBox(l, displays, GPanel, size, l.op > IS && e.op != SUB && l.op < e.op, hot));
      } else if (r.op == VAR && "infini".equals(r.L)) {
        return exprBox(var("-infini"), displays, GPanel, size, brackets, hot);
      }
      rbox = new SystemBox(new TextBox(e.op(), GPanel.getGraphics(), size), new int[]{0, 2, 0, 2});
      if (!e.op().equals("")) {box.add(rbox, "EST");}
      if (r.op == DIV || r.op == EXP || r.op == JUX) {
        rbox = exprBox(r, displays, GPanel, size, false, hot);
      } else {
        rbox = exprBox(r, displays, GPanel, size, r.op > IS && r.op < e.op + 2, hot);
      }
      box.add(rbox, "EST");
    }
    return box;
  }
  
  /**
   * met des parenthèses autour de la boîte
   * @param box boîte de référence
   * @param type le type de parenthèse : (, [, {
   * @param size de la fonte
   * @param g le contexte graphique
   * @param first si vrai, on ouvre
   * @param last si vrai, on ferme
   * @return
   */
  private SystemBox getBrackets(String type, int size, GeomPanel GPanel) {
    GPoint P0 = new GPoint(0,0), P1 = new GPoint(60,0);
    SystemBox box = new SystemBox(this);
    int n = type.length(), h = box.getBoxMetrics().height;
    if(n > 0) {
      if(h > 21) box.add(0, P0, P1, new DrawBox(box, type.substring(0,1)));
      else {
        SystemBox temp = exprBox(var(type.substring(0,1)), null, GPanel, size, false, false);
        box.add(0, P0, P1, temp);
      }
    }
    if(n == 2) {
      if(h > 21) box.add(0, P1, P0, new DrawBox(box, type.substring(1)));
      else {
        SystemBox temp = exprBox(var(type.substring(1)), null, GPanel, size, false, false);
        box.add(0, P1, P0, temp);
      }
    }
    return new SystemBox(box);
  }
  
  
  /**
   * Ajoute une boîte au système
   * @param a le rang de la boîte du système
   * @param Pa le point d'attache de boxes[rg] (à l'échelle 60*60)
   * @param Pb le point d'attache de box (à l'échelle 60*60)
   * @param box la boîte ajoutée
   */
  public void add(int a, GPoint Pa, GPoint Pb, Box box) {
    int left = 0, up = 0;
    Box boxA = boxes.get(a);
    int[] coorsA = coors.get(a);
    // ancrage de box dans la boîte actuelle
    BoxMetrics sizeA = boxA.getBoxMetrics();
    BoxMetrics sizeB = box.getBoxMetrics();
    int bx = (int)Pb.x.toDouble()*sizeB.width/60;
    int by = (int)Pb.y.toDouble()*sizeB.height/60;
    int Bx = coorsA[0] - bx + (int)Pa.x.toDouble()*sizeA.width/60;
    int By = coorsA[1] - by + (int)Pa.y.toDouble()*sizeA.height/60;
    boxes.add(box);
    coors.add(new int[] {Bx, By}); // coordonnées provisoires
    // agrandissement éventuel du système
    int right = Bx + sizeB.width - size.width;
    int bottom = By + sizeB.height - size.height;
    if(Bx < 0) left = -Bx;
    if(By < 0) up = -By;
    if(right > 0) size.width += right;
    if(bottom > 0) size.height += bottom;
    size.width += left;
    size.height += up;
    size.baseLine += up;
    // correction des coordonnées des points d'ancrage
    if(left + up >0) {
      for(int i = 0; i < coors.size(); i++) {
        coorsA = coors.get(i);
        coorsA[0] += left;
        coorsA[1] += up;
      }
    }    
    upActiveBox(box);
  }

/**
 * ajoute horizontalement ou verticalement une boîte à la dernière boîte ou s'il n'y a rien équivaut
 * à SystemBox(box)
 * @param box la boîte à ajouter
 * @param dir les quatre directions : OUEST, EST, NORD et SUD
 */  
public void add(Box box, String dir) {
  int index = boxes.size()-1;
  if(index == -1) {
    boxes.add(box);
    coors.add(new int[] {0,0});
    BoxMetrics b = box.getBoxMetrics();
    size = new BoxMetrics(b.width, b.height, b.baseLine);   
    upActiveBox(box);
  }
  else {
    Box last = boxes.get(index);
    GPoint A = new GPoint(30, 60), B = new GPoint(30, 0);
    BoxMetrics lastBM = last.getBoxMetrics(), BM = box.getBoxMetrics();
    if(dir.equalsIgnoreCase("EST")) {
      A = new GPoint(60, lastBM.baseLine*60/lastBM.height); 
      B = new GPoint(0, BM.baseLine*60/BM.height);          
    }      
    else if(dir.equalsIgnoreCase("OUEST")) {
      A = new GPoint(0, lastBM.baseLine*60/lastBM.height); 
      B = new GPoint(60, BM.baseLine*60/BM.height);          
    }      
    else if(dir.equalsIgnoreCase("SUDOUEST")) {
      A = new GPoint(0, 60); 
      B = new GPoint(0, 0);          
    }
    else if(dir.equalsIgnoreCase("NORD")) {
      A = new GPoint(30, 0);
      B = new GPoint(30, 60);
    }
    add(index, A, B, box);
  }
}
	

public void paint(Graphics g, BoxMetrics size, int x, int y) {
  int w = getBoxMetrics().width, h = getBoxMetrics().height;
  if(hot && GPanel != null && expr.parent != NULL && expr.op != VAR) {
    GPanel.getAreas().add(new Rectangle(x, y, w, h));
    GPanel.getExprs().add(expr);
  }  
  Color save = g.getColor();
  if(frame != null) { // cadre autour
    int margin = Math.abs(frame[0]), radius = frame[1];
    if(frame[2] != -1) { // dessin du cadre
      g.setColor(new Color(frame[2], frame[3], frame[4]));
      if(radius == -1) g.fillRect(x-margin, y-margin, w+2*margin, h+2*margin);
      else g.fillRoundRect(x-margin, y-margin, w+2*margin, h+2*margin, radius, radius);
      g.setColor(save);
    }
    if(frame[0] >= 0) { // dessin du cadre
      if(radius == -1) g.drawRect(x-margin, y-margin, w+2*margin, h+2*margin);
      else g.drawRoundRect(x-margin, y-margin, w+2*margin, h+2*margin, radius, radius);
    }
  }
  if(active != 0) {
    g.setColor(Color.YELLOW);
    g.fillRect(x+1, y, w-2, h);
    g.setColor(save);
    switch (active) {
      case 1 : g.drawLine(x+1, y, x+1, y+h); break;
      case 2 : g.drawLine(x+w/2, y+2, x+w/2, y+h-2); break;
      case 3 : g.drawLine(x+w-1, y, x+w-1, y+h);
    }
  }
  for(int i = 0; i < boxes.size(); i++) {
    int[] anchor = coors.get(i);
    Box box = boxes.get(i);
    box.paint(g, box.getBoxMetrics(), x+anchor[0], y+anchor[1]);
  }
}

  public int getActive() {
   return active;
  }

  public void setActive(int active) {
    this.active = active;
  }

  public void negActive() {
    active = -active;
  }

  /** transfert l'activeBox de box dans la boîte actuelle
   */
  private void upActiveBox(Box box) {
    if (box instanceof SystemBox) { 
      SystemBox sbase = ((SystemBox)box).getActiveBox();
      if(sbase != null) 
        setActiveBox(sbase, sbase.getActive());
    }
  }
  
  public SystemBox getActiveBox() {
    return activeBox;
  }

  public void setActiveBox(SystemBox activeBox, int active) {
    activeBox.setActive(active);
    SystemBox.activeBox = activeBox;
  }

  public void setFrame(int[] frame) {
    this.frame = frame;
  }

  public void setExpr(Expression expr) {
    this.expr = expr;
  }


}

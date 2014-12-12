package root;

import expressions.*;
import display.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import static expressions.Expression.*;

public class Actions implements Runnable {

  ExpressionApplet userApplet;
  GeomPanel GPanel;
  int pause = 300;
  int keycode;
  String keyText;
  String name;
  Point point;
  Point[] dPts;
  GBase base;
  GObject GObjDrag;
  boolean wake = true; // pour dragged
  HashMap<String, Integer> spots = new HashMap<String, Integer>(), 
    startdefs = new HashMap<String, Integer>(), enddefs = new HashMap<String, Integer>();
  HashMap<String, Expression> stopcond = new HashMap<String, Expression>();
  ArrayList<Expression> spotsdefs = new ArrayList<Expression>();
  Thread anim;

  /**
   * constructeur de la classe
   * @param applet l'applet courante
   */  
  public Actions(ExpressionApplet applet) {
    userApplet = applet;
    GPanel = applet.GPanel;
  }

    
  
  protected void keyPressed(int keycode) { // (GPanelKeyPressed est exécuté avant)
    keyText = KeyEvent.getKeyText(keycode);
    int index = spotsdefs.indexOf(var("PRESSER"));
    if (index != -1) {
      if(keyText.equals("Entrée")) { // équivalent à keycode == VK_ENTER
        Expression answer = ExpressionApplet.getDefinition("réponse", userApplet.definitions);
        Integer i = spots.get("curseur");
        if(i != null && answer != null) { // affichage de l'entrée
          GPoint Pt = (GPoint) userApplet.Gobjets.get(i);
          Pt.labelBox = SystemBox.exprBox(answer, userApplet.DISPLAYS, GPanel, Pt.fontsize, false, false);
        }
        GPanel.drawFig(userApplet.Gobjets, 0, -1);
        i = userApplet.definitions.indexOf(var("entrée"));
        if(i != -1) userApplet.definitions.get(i).R = null;
      }
      Expression key = (Expression) spotsdefs.get(index).R;
      name = (String)key.L;
      key.R = var(keyText);
      int i = userApplet.definitions.indexOf(key);
      if(i != -1) userApplet.definitions.get(i).R = key.R; // mise à jour de la variable key
      if(keycode == 0) name = "init";
      updateSpots(name);
    }
    keyText = null;
  }
  

  protected void mouseClicked(MouseEvent evt) {
    point = evt.getPoint();
    base = userApplet.clicsBase;
    int index = spotsdefs.indexOf(var("CLIQUER"));
    if (index != -1 && base.clip.contains(point)) {
      Expression def = (Expression) spotsdefs.get(index).R;
      if (def.op == VAR) {
        name = (String)def.L;
        dPts = new Point[] {new Point(0,0)};
        updatePts(name, dPts);
      }
      updateSpots(name);
    }
  }

  protected void mouseDragged(MouseEvent evt) {
    point = evt.getPoint();
    if (wake) { // recherche de l'objet à traîner
      wake = false;
      name = null;
      Set<String> names = spots.keySet();
      for (String next : names) {
        int place = spots.get(next).intValue();
        GObjDrag = userApplet.Gobjets.get(place);
        if (GObjDrag.type != 1) {continue;}
        base = GObjDrag.base;
        dPts = GObjDrag.getPointsRelative(point, 4); // les Points relatifs définissant l'objet
        if (dPts != null) {
          name = next;
          break;
        }
      }
    }
    if (name != null) { // l'objet graphique a pour nom name
      updatePts(name, dPts);
      updateSpots(name);
    }
  }
  
  /** mise à jour des coordonnées des points dans les définitions (décalages dans Pts)
   *  @param name le nom de l'objet graphique
   *  @param dPts point représentant le décalage avec les points définissant l'objet graphique
   */
  private void updatePts(String name, Point[] dPts) {
    try {
      GPoint[] Pts = new GPoint[dPts.length];
      for (int i = 0; i < Pts.length; i++) { // les nouveaux GPoints
        Pts[i] = new GPoint(new Point(point.x + dPts[i].x, point.y + dPts[i].y), base);
      }
      Expression def = ExpressionApplet.getDefinition(name, userApplet.getDefinitions());
      if (def.op == SET) {
        def = ((Expression[]) def.R)[0];
      }
      Expression[] defcoors = ("point".equals(def.L)) ? new Expression[]{def} : (Expression[]) def.R;
      for (int i = 0; i < Pts.length; i++) { // mise à jour des définitions des GPoints
        if (defcoors[i].op == SET) {
          defcoors[i] = ((Expression[]) defcoors[i].R)[0];
        }
        defcoors[i].R = new Numeric[]{Pts[i].x, Pts[i].y};
      }
    } catch (java.lang.Exception ex) {
      System.out.println("erreur dans la définition " + name);
      System.out.println(ex.getMessage());
    }
  }

  /** mise à jour des définitions de spotsdefs
   *  @param name le nom du point tiré
   */
  private void updateSpots(String name) {
    Expression stop = stopcond.get(name);
    try {
      if(stop != null) {
        if(!((Relation)stop).valueOf(userApplet.getDefinitions(), 0, 50)) return;
      }
    } catch(Exception exc) {}
    Integer start = startdefs.get(name), end = enddefs.get(name);
    if(end == null) end = spotsdefs.size();
    if(start == null) start = 0;
    for (int k = start; k < end; k++) {
      Expression expr = spotsdefs.get(k);
      try {
        if(expr.op == IS) {
          int[] ranges = expr.define(userApplet.getDefinitions(), true);
          for (int j = 0; j < ranges.length; j++) {
            Expression var = userApplet.getDefinitions().get(ranges[j]);
            String varname = (String)var.L;
            Expression defname = (Expression)var.R;
            if("texte".equals(varname)) {
              userApplet.drawText(defname);
              continue;
            }
            else if("exécuter".equals(varname) && defname instanceof Numeric) {
              int npart = ((Numeric)defname).toInt();
              if(npart >= 0) userApplet.exercice(npart);
              if(npart == -1) {
                GPanel.updateImg();
                int n = userApplet.Gobjets.size();
                userApplet.Gobjets.subList(2, n).clear(); // on garde introBase et introPt
                n = userApplet.definitions.size();
                userApplet.definitions.subList(userApplet.getCnt_create()[0]+4, n).clear();
                int size0 = userApplet.definitions0.size();
                userApplet.definitions.addAll(userApplet.definitions0.subList(4, size0));
                userApplet.display(true);
              }
              if(npart > -2) return;
            }
            Integer range = spots.get(varname);
            if (range != null) { // objet graphique
              GObject obj = userApplet.Gobjets.get(range);
              GObject nobj = userApplet.createGObject(defname, obj.base);
              if (obj.type == 0) { // non trainable ou non cliqué
                userApplet.Gobjets.set(range, nobj);
                if("curseur".equals(varname)) {
                  userApplet.activerange = range;
                  userApplet.startCurseur(defname, (GPoint)nobj);
                }
              }
              else { // mise à jour objet trainé ou cliqué
                if (varname.equals(name)) {
                  GPoint[] pts = obj.getPoints();
                  GPoint[] npts = nobj.getPoints();
                  for (int i = 0; i < npts.length; i++) {
                    pts[i].x = npts[i].x;
                    pts[i].y = npts[i].y;
                  }
                }
              }
            }
          }
        }
      } catch (Exception exception) {
          //[ifframe]
          userApplet.mathFrame.textArea.append("\ndéfinition " + expr + " non valable");
          //[endframe]
          System.out.println(exception.getMessage());
       }
    }
    GPanel.drawFig(userApplet.Gobjets, userApplet.endfigbkg, userApplet.endfigbkg);
  }

  /**
   * processus d'animation de la figure
   */
  public void run() {
    while (anim == Thread.currentThread()) {
      int index = spotsdefs.indexOf(var("ANIMER"));
      if (index != -1) {
        Expression time = (Expression) spotsdefs.get(index).R;
        name = (String)time.L;
        updateSpots(name);
      }
      try {
        Thread.sleep(pause);
      } catch(InterruptedException ie) {}
    }
  }

}

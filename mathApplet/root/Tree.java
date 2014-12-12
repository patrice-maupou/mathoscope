package root;

import expressions.*;
import static expressions.Expression.*;
import static java.awt.event.KeyEvent.*;
import java.math.BigInteger;
/*
 * java *
 * Created on 4 juillet 2005, 17:01
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html
 *
 */

/**
 *
 * @author Patrice Maupou
 */
public class Tree {

  public static int LEFT = -2;
  public static int RIGHT = -1;
  public static int LAST = -3;
  public static int ROOT = -4;

  
  /**
   *  transforme les variables en TBOX
   * @param e Expression à transformer
   * @param curseur Expression retournée si done est faux, ou "all" ou e.L = ?
   * @param pre Expression
   * @param tst change suivant la variable e, si e=? on retourne le curseur si test=4 et espace si test=3,
   *        si tst=7 ou 2 on retourne pre et tst=6 ou 1 le curseur, tst=5 ou 0 c'est l'espace
   * @return l'expression avec les TBOX
   */
  public static Expression varsToBox(Expression e, Expression curseur, Expression pre, Numeric tst) {
    Expression ret = e.copy();
    int type = tst.toInt();
    if (e.op == VAR) {
      if (!"?".equals(e.L) && !var("symbol").equals(e.R)) {
        return e;
      }
      switch (type) {
        case 4:
          if (e.L.equals("?")) {
            ret = curseur;
            type--;
            break;
          }
        case 3:
          if (e.L.equals("?")) {
            return build(FUNC, "TBOX", new Expression[]{var(" ")});
          }
          break;
        case 7:
        case 2:
          ret = pre;
          type--;
          break;
        case 6:
        case 1:
          ret = curseur;
          type--;
          break;
        case 5:
        case 0:
          return build(FUNC, "TBOX", new Expression[]{var(" ")});
      }
      tst.L = new BigInteger("" + type);
      return ret;
    } else if (ret.op == FUNC && "jux".equals(ret.L)) {
      Expression[] coors = (Expression[]) ret.R;
      if (coors.length == 2) {
        ret = build(JUX, coors[0], coors[1]);
        if (coors[0].op == VAR && !"?".equals(coors[0].L)) {
          ret.L = build(FUNC, "TBOX", new Expression[]{
              coors[0]
            });
        }
        if (coors[1].op == VAR && !"?".equals(coors[1].L)) {
          ret.R = build(FUNC, "TBOX", new Expression[]{
              coors[1]
            });
        }
      }
    } else if (!"TBOX".equals(ret.L) && ret.R instanceof Expression[]) {
      Expression[] nR = (Expression[]) ret.R;
      int n = (e.L.equals("BBOX")) ? 1 : nR.length;
      if (type < 5) {
        for (int i = 0; i < n; i++) {
          nR[i] = varsToBox(nR[i], curseur, pre, tst);
        }
      } else {
        for (int i = n - 1; -1 < i; i--) {
          nR[i] = varsToBox(nR[i], curseur, pre, tst);
        }
      }
      return ret;
    }
    Object eL = (type < 5) ? ret.L : ret.R;
    Object eR = (type < 5) ? ret.R : ret.L;
    if (eL instanceof Expression) {
      eL = varsToBox((Expression) eL, curseur, pre, tst);
    }
    if (eR instanceof Expression) {
      eR = varsToBox((Expression) eR, curseur, pre, tst);
    }
    return (type < 5) ? build(ret.op, eL, eR) : build(ret.op, eR, eL);
  }

  /** enlève le curseur et transforme les TBOX en expressions
   * @param e
   * @return
   * @throws java.lang.Exception
   */
  public static Expression strip(Expression e) throws Exception {
    Expression ret = e.copy(), l;
    if (ret.equals(var("caret"))) {
      throw new Exception("Expression incomplète");
    }
    else if (ret.op == SUB && var("caret").equals(ret.L)) { // moins unaire
      ret.L = ZERO;
      ret.R = strip((Expression) ret.R);
    }
    else if (ret.op == JUX) {
      if (var("caret").equals(ret.L)) {ret = strip((Expression) ret.R);}
      else if (var("caret").equals(ret.R)) {ret = strip((Expression) ret.L);}
      else throw new Exception("Opérateur manquant");
    }
    else if (ret.L.equals("BLOC")) { // à retirer
      Expression[] coors = (Expression[]) ret.R;
      return strip(coors[0]);
    }
    else if (ret.L.equals("TBOX")) {
      Expression[] coors = (Expression[]) ret.R;
      String s = "", temp;
      for (int i = 0; i < coors.length; i++) {
        temp = coors[i].toString();
        if (temp.startsWith("`") && temp.endsWith("`")) {temp = temp.substring(1, temp.length() - 1);}
        s += temp;
      }
      ret = Parser.parse(s);
    }
    else {
      if (ret.L instanceof Expression) {ret.L = strip((Expression) ret.L);}
      if (ret.R instanceof Expression) {ret.R = strip((Expression) ret.R);}
      else if (ret.R instanceof Expression[]) {
        Expression[] coors = (Expression[]) ret.R;
        for (int i = 0; i < coors.length; i++) {
          coors[i] = strip(coors[i]);
        }
        if (ret.L instanceof Expression) {
          l = (Expression) ret.L;
          if (l.op == VAR) {ret.L = l.L;}
        }
      }
    }
    return ret;
  }

  /**
   * Etablit les liens entre les sous-expressions en descendant
   * @param e
   */
  public static void setTree(Expression e) {
    if (e.L instanceof Expression) {
      Expression nL = (Expression) e.L;
      nL.parent = e;
      nL.place = LEFT;
      setTree(nL);
    }
    if (e.R instanceof Expression) {
      Expression nR = (Expression) e.R;
      nR.parent = e;
      nR.place = RIGHT;
      setTree(nR);
    } else if (e.R instanceof Expression[]) {
      Expression[] nR = (Expression[]) e.R;
      for (int i = 0; i < nR.length; i++) {
        setTree(nR[i]);
        nR[i].parent = e;
        nR[i].place = i;
      }
    }
  }

  /**
   * remplace une feuille e par une autre n
   * @param e
   * @param n
   * @return
   */
  public static Expression substitute(Expression e, Expression n) {
    n.parent = e.parent;
    n.place = e.place;
    if (e.place != ROOT) {
      if (n.place == LEFT) {
        e.parent.L = n;
      } else if (n.place == RIGHT) {
        e.parent.R = n;
      } else if (e.parent.R != null && e.parent.R instanceof Expression[]) {
        Expression[] coors = (Expression[]) e.parent.R;
        coors[n.place] = n;
      }
    }
    setTree(n);
    return n;
  }

  /**
   * Si e a une liste de composants, on ajoute le composant n à la place
   *  indiquée.
   *  exemple : insert(f(a,b,c), x+2, LAST) donne f(a,b,c,x+2)
   * @param e expression à transformer
   * @param n expression à ajouter
   * @param place le rang de la nouvelle coordonnée
   * @return l'expression transformée
   */
  public static Expression insertNode(Expression e, Expression n, int place) {
    Expression replace = e.copy(), ret = e;
    if (place == LEFT) { // juxtaposer n à gauche de e remplace e

      replace = build(JUX, n, e);
    } else if (place == RIGHT) {
      replace = build(JUX, e, n);
    } else if (e.R instanceof Expression[]) {// e=f(x,y), n=2, place=2 donne f(x,y,2)

      Expression[] pcoors = (Expression[]) e.R;
      int nb = pcoors.length;
      if (place == LAST) {
        place = nb; // à droite de la dernière coordonnée

      }
      if (place <= nb) {
        Expression[] temp = new Expression[nb + 1];
        if (place > 0) {
          System.arraycopy(pcoors, 0, temp, 0, place);
        }
        if (place < nb) {
          System.arraycopy(pcoors, place, temp, place + 1, nb - place);
        }
        temp[place] = n; // nouvelle coordonnée

        replace.R = temp;
        ret = temp[place];
      }
    }
    substitute(e, replace);
    return ret;
  }

  /** supprime un élément d'une liste, retourne NULL s'il n'y a plus rien dans la liste
   * @param e
   * @param place
   * @return
   */
  public static Expression deleteNode(Expression e, int place) {
    Expression ret = e;
    if (place == LEFT) {
      if (e.L instanceof Expression && e.R instanceof Expression) {
        ret = substitute(e, (Expression) e.R);
      }
    } else if (place == RIGHT) {
      if (e.L instanceof Expression && e.R instanceof Expression) {
        ret = substitute(e, (Expression) e.L);
      }
    } else if (e.R instanceof Expression[]) {
      Expression[] coors = (Expression[]) e.R;
      int nb = coors.length;
      if (nb == 1) {
        return NULL;
      }
      if (place == LAST) {
        place = nb - 1;
      }
      if (place >= 0 && place < nb) {
        Expression[] temp = new Expression[nb - 1];
        System.arraycopy(coors, 0, temp, 0, place);
        System.arraycopy(coors, place + 1, temp, place, nb - place - 1);
        e.R = temp;
        setTree(e);
      }
    }
    return ret;
  }

  /**
   * insertion d'une lettre ou d'un chiffre
   * @param c
   * @param active
   * @return
   */
  private static boolean insertAlphanum(char c, Expression active) {
    String s = String.valueOf(c);
    Expression[] coors = new Expression[]{var(s)};
    Expression p = active.parent, n = build(FUNC, "TBOX", coors);
    if (p != null && p.op == JUX) {
      if (active == p.R) {
        Expression nl = (Expression) p.L; // JUX(nl,|)

        boolean jux = nl.L.equals("TBOX");
        if (jux) {// JUX(TBOX(..),|) il faut agrandir la TBOX à droite
          // modifier si TBOX(1,2) et c est une lettre, on multiplie

          s = "";
          Expression[] chars = (Expression[]) nl.R;
          for (int i = 0; i < chars.length; i++) {
            s += (String) chars[i].L;
          }
          try {
            if (!Character.isLetter(c) && c != '.') {
              throw new NumberFormatException();
            }
            Double.parseDouble(s);
            if (c == '.') {
              insertNode(nl, coors[0], LAST);
            } // JUX(TBOX(12.),|)
            else { // JUX(TBOX(12),a) -> TBOX(12)*JUX(a,|)

              p.op = MUL;
              substitute(active, build(JUX, n, active));
            }
          } catch (NumberFormatException NFE) {
            if (c != '.') {
              insertNode(nl, coors[0], LAST);
            } // ab ou a1

          }
        } else {
          p.op = MUL;
          substitute(active, build(JUX, n, active));
        }
      } else if (active == p.L) { // JUX(|,nr)

        Expression nr = (Expression) p.R;
        boolean jux = nr.L.equals("TBOX"); // 12 1. a2 ab, mais pas 2a

        if (jux) {
          Expression[] chars = (Expression[]) nr.R;
          char toright = ((String) chars[0].L).charAt(0);
          jux = !Character.isDigit(c) || !Character.isLetter(toright);
        }
        if (jux) // JUX(|,TBOX(..)) il faut agrandir la TBOX à gauche
        {
          insertNode(nr, coors[0], 0);
        } else {
          p.op = MUL;
          substitute(active, build(JUX, n, active));
        }
      }
    } else { // début de la construction

      substitute(active, build(JUX, n, active));
    //active.insert(n, JUX, ExprLEFT);
    }
    return true;
  }

  /**
   * insèrtion d'une opération
   * @param op le code entier de l'opération
   * @param active
   * @return true
   */
  private static boolean insertOperator(int op, Expression active, Expression space) {
    Expression p = active.parent, temp;
    if (op < SUM && p.place != ROOT && p.parent.op <= EXP && p.parent.op >= SUM) {
      return up(active, space);
    } // relation algébrique
    else if (p.op == JUX) { // vec(u)| devient vec(u)+|      
      //* modif 10 juillet 2008
      if(p.parent.op == JUX) {// TODO : si JUX est plus haut, le remplacer par l'opérateur : a| b-2 <+> a+ |b-2
        p.parent.op = op;
        if(p.place == LEFT) {
          temp = (Expression)p.parent.R;
          deleteNode(p, RIGHT);
          insertNode(temp, active, LEFT);
        }        
      }
      else {
        temp = build(op, p.L, p.R);
        substitute(p, temp);        
      }
      //*/
      /* avant modif 10 juillet 2008
      temp = build(op, p.L, p.R);
      substitute(p, temp);
      //*/
    } 
    else if (op == SUB) {// moins unaire
      temp = build(SUB, ZERO, active);
      substitute(active, temp);
    } 
    else if (op == EQU) { // a < |
      if (p.op == LT) {p.op = LE;} 
      else if (p.op == GT) {p.op = GE;}
    }
    return true;
  }

  /**
   *
   * @param active
   * @return vrai si un l'arbre est modifié
   */
  private static boolean insertSpace(Expression active) {
    Expression p = active.parent;
    if (active.place == RIGHT) {
      Expression l = (Expression) p.L;
      if (l.L.equals("TBOX")) {
        insertNode(l, var(" "), LAST);
        return true;
      }
    }
    return false;
  }

  /**
   * On tape une parenthèse ouvrante pour définir une fonction
   * @param active
   * @return
   */
  private static boolean insertFunc(Expression active) {
    Expression p = active.parent, temp;
    Expression[] r = new Expression[]{active};
    Expression l = (Expression) p.L;
    if (l.L.equals("TBOX")) {
      Expression[] chars = (Expression[]) l.R;
      String s = "";
      for (int i = 0; i < chars.length; i++) {
        s += (String) chars[i].L;
      }
      try {
        temp = Parser.parse(s);
        if (temp.op == VAR) {
          substitute(p, build(FUNC, l, r));
        }
      } catch (Exception exc) {
        return false;
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * insertion d'une virgule
   * @param active
   * @return
   */
  private static boolean comma(Expression active, Expression space) {
    Expression p = active.parent, temp;
    if (active.place == RIGHT && p.op == JUX) { // a|

      if (p.place >= 0) { // f(a,b|,c) doit donner f(a,b,|,c)

        substitute(p, (Expression) p.L);
        insertNode(p.parent, active, p.place + 1);
      } else if (p.parent.op < IS) { // x + a| pas valide

        Expression l = (Expression) p.L;
        l = build(SEQ, "()", new Expression[]{l, active});
        substitute(p, l);
      } else {
        return up(active, space);
      }
      return true;
    }
    return false;
  }

  /**
   * passage à la branche de gauche
   * @param active
   * @return
   */
  private static boolean left(Expression active, Expression space) {
    Expression p = active.parent, temp;
    if (active.place == RIGHT) { // -|, 3-|

      if (p.isNegative()) {
        p = substitute(p, build(p.op, active, space));
      } else if (p.op == JUX) { //  (ab|)c

        temp = (Expression) p.L;
        if ("TBOX".equals(temp.L)) {
          Expression[] coors = (Expression[]) temp.R;
          Expression r = coors[coors.length - 1];
          Expression l = deleteNode(temp, LAST);
          if (l.equals(NULL)) {
            p = substitute(p, active);
          }
          if (p.place == LEFT && p.parent.op == JUX) {
            insertNode((Expression) p.parent.R, r, 0);
          } else {
            r = build(FUNC, "TBOX", new Expression[]{r});
            insertNode(p, r, RIGHT);
          }
        } else {
          p = substitute(p, build(JUX, active, p.L));
        }
      } else if (p.L instanceof Expression) {
        substitute(active, space);
        insertNode((Expression) p.L, active, RIGHT);
      }
    } else if (active.place > 0) { // f(a,|,b) -> f(a|, ,b)

      substitute(active, space);
      insertNode(((Expression[]) p.R)[active.place - 1], active, RIGHT);
    } else if (active.place != ROOT) { // lEFT ou FIRST

      if (active.place == 0) { // FIRST

        substitute(active, space);
        if (p.L instanceof Expression) {
          insertNode((Expression) p.L, active, RIGHT);
        } else {
          return up(active, space);
        }
      } else if (p.place == LEFT) {
        return up(active, space);
      } else { // LEFT

        if (p.op == JUX) {
          temp = deleteNode(p, LEFT);
        } else {
          substitute(active, space);
          temp = p;
        }
        if (temp.place == RIGHT) { // b+|a

          if (temp.parent.isNegative()) {
            insertNode(temp.parent, active, LEFT);
          } else {
            insertNode((Expression) p.parent.L, active, RIGHT);
          }
        } else if (temp.place == 0) { // f(|a,b) -> f|(a,b) ou (|a,b) -> |(a,b)

          if (temp.parent.L instanceof Expression) {
            insertNode((Expression) temp.parent.L, active, RIGHT);
          } else {
            insertNode(temp.parent, active, RIGHT);
          }
        } else { // f(a,|b,c)

          insertNode(((Expression[]) temp.parent.R)[temp.place - 1], active, RIGHT);
        }
      }
    }
    return true;
  }

  /**
   * passage à la branche de droite
   * @param active
   * @return
   */
  private static boolean right(Expression active, Expression space) {
    Expression p = active.parent, temp;
    if (active.place == LEFT) { // |ab, |-2,

      if (p.op == JUX) {
        temp = (Expression) p.R;
        if (temp.L.equals("TBOX") && ((Expression[]) temp.R).length > 1) {// |ab -> (a|)b

          Expression l = ((Expression[]) temp.R)[0];
          l = build(FUNC, "TBOX", new Expression[]{l});
          insertNode(active, l, LEFT);
          deleteNode(temp, 0);
        } else {
          p = substitute(p, build(JUX, p.R, active));
        }
      } else if (p.op == SUB) {
        substitute(active, ZERO);
        insertNode((Expression) p.R, active, LEFT);
      } else {
        substitute(active, space);
        insertNode((Expression) p.R, active, LEFT);
      }
    } else if (active.place >= 0) { // f(a,|,b) -> f(a, ,|b)

      substitute(active, space);
      Expression[] coors = (Expression[]) p.R;
      if (active.place < coors.length - 1) {
        insertNode(coors[active.place + 1], active, LEFT);
      } else {
        insertNode(p, active, RIGHT);
      }
    } else if (active.place == ROOT) {
      return false;
    } else {
      if (active.place == LAST || p.place == RIGHT) {
        return up(active, space);
      } else if (p.op == JUX && p.parent.op == JUX) { // (ab|)c -> abc|

        temp = (Expression) p.parent.R;
        if ("TBOX".equals(temp.L)) {
          Expression[] coors = (Expression[]) temp.R;
          Expression l = coors[0];
          Expression r = deleteNode(temp, 0);
          if (r.equals(NULL)) {
            p = substitute(p.parent, p);
          }
          insertNode((Expression) p.L, l, LAST);
        }
      } else { // RIGHT : <expr>|+3 ou f(<expr>|, 2) ou vec(a|) ou a+|

        if (p.op == JUX) {
          temp = deleteNode(p, RIGHT);
        } else {
          substitute(active, space);
          temp = p;
        }
        if (temp.place == LEFT) {// <expr>|+3
          if (temp.parent.R == null) {
            insertNode(temp.parent, active, RIGHT);
          } else {
            temp = (temp.parent.R instanceof Expression)? (Expression)temp.parent.R : ((Expression[])temp.parent.R)[0];
            insertNode(temp, active, LEFT);
          }
        } else if (temp.place >= 0) { // f(<expr>|, 2) ou vec(a|)
          Expression[] coors = (Expression[]) temp.parent.R;
          if (temp.place < coors.length - 1) {
            coors[temp.place] = temp;
            p = insertNode(coors[temp.place + 1], active, LEFT); // pas toujours possible !

          } else {
            return up(active, space);
          }
        } else if (temp.place == ROOT) {
          insertNode(temp, active, RIGHT);
        } else {
          insertNode(temp.parent, active, LEFT);
        }
      }
    }
    return true;
  }

  /**
   * remontée vers la branche supérieure
   * @param active
   * @return vrai si l'arbre a changé
   */
  private static boolean up(Expression active, Expression space) {
    Expression p = active.parent, temp;
    if (p.place == ROOT && p.op == JUX) {
      return false;
    }
    if (p.op == JUX) {
      temp = deleteNode(p, active.place).parent;
    } else {
      substitute(active, space);
      temp = p;
    }
    if (p.op == SUB && active.place == LEFT) { // |-x

      substitute(active, ZERO); // si -(|,a) alors |(-a)

    }
    if (active.place < 0) {
      p = insertNode(temp, active, active.place);
    } else {
      int n = (active.place > ((Expression[]) temp.R).length / 2) ? RIGHT : LEFT;
      p = insertNode(temp, active, n);
    }
    return true;
  }

  /**
   * descente branche inférieure
   * @param active
   * @param space
   * @return vrai si l'arbre a changé
   */
  private static boolean down(Expression active) {
    Expression p = active.parent, temp, down;
    if (p.op != JUX) {
      return false;
    }
    int dir = active.place;
    temp = (dir == LEFT) ? (Expression) p.R : (Expression) p.L;
    if (temp.op == FUNC && "BLOC".equals(temp.L)) {
      temp = ((Expression[]) temp.R)[0];
    }
    if (temp.op >= IS) { // descendre du même côté

      down = (dir == LEFT) ? (Expression) temp.L : (Expression) temp.R;
      if (down == null || (down.op <= VAR && down.op >= INT)) {
        down = (Expression) temp.L;
      }
      if (down != null && down.op > VAR || down.parent.isNegative()) {
        if (down.parent.isNegative() && down.place == LEFT) {
          p = substitute(p, temp);
          down = (Expression) down.parent.R;
          substitute(down, build(JUX, active, down));
        } else {
          p = substitute(p, temp);
          if (dir == LEFT) {
            substitute(down, build(JUX, active, down));
          } else {
            substitute(down, build(JUX, down, active));
          }
        }
      }
    } else if (temp.op == FUNC && dir == LEFT && temp.L instanceof Expression) {
      p = substitute(p, temp);
      down = (Expression) temp.L;
      substitute(down, build(JUX, active, down));
    } else if (!"TBOX".equals(temp.L) && temp.R instanceof Expression[]) { // fonction, séquence ou ensemble
      p = substitute(p, temp);
      Expression[] coors = (Expression[]) temp.R;
      // si fonction constante, on descend
      while (coors[0].op == FUNC && coors[0].L instanceof String) {
        if (!"TBOX".equals(coors[0].L)) {
          coors = (Expression[]) coors[0].R;
        } else {
          break;
        }
      }
      if (dir == LEFT) {
        insertNode(coors[0], active, dir);
      } else {
        insertNode(coors[coors.length - 1], active, RIGHT);
      }
    } else {
      return false;
    }
    return true;
  }

  // TODO : envisager la juxtaposition d'expressions pour une plus grande souplesse, 3+|4 <BACKSPACE> 3|4 <-> 3-|4
  private static boolean backSpace(Expression active) {
    Expression p = active.parent, temp;
    temp = active;
    while (temp.place == LEFT) { // on remonte tant que le curseur est à gauche
      temp = temp.parent;
    }
    if (temp.place == RIGHT) { // on supprime ce qui est immédiatement à gauche
      temp = temp.parent;
      Expression l = (Expression) temp.L;
      if (temp.op == JUX && l.L.equals("TBOX") && ((Expression[]) l.R).length > 1) {
        l = deleteNode(l, LAST);
      }
      else if (temp.op == JUX && l.L.equals("BLOC")) {return false;}
      else if (temp.op == JUX && l.op == VAR) {
        substitute(temp, active);
      }
      else if (temp.equals(p) && p.op != JUX) {
        if (temp.isNegative()) {substitute(temp, active);}
        else {p.op = JUX;} // <expr>+| devient <expr>|
      }
      else if (temp.op >= IS) { // opérateur binaire
        /* avant modif 10 juillet 2008
        temp = substitute(temp, (Expression) temp.R);
        //*/
        //* modif 10 juillet 2008
        if(temp.op == JUX) {temp = substitute(temp, (Expression) temp.R);}
        else {
          temp.op = JUX;
          if (active != temp.R) {
            Expression left = (Expression) temp.L;
            deleteNode(p, LEFT);
            insertNode(left, active, RIGHT);
          }
        }
        //*/
      }
    }
    else if (temp.place > 0) { // argument d'une suite ou fonction
      deleteNode(temp.parent, temp.place - 1);
    }
    else {return false;}
    return true;
  }

  private static boolean delete(Expression active) {
    Expression p = active.parent, temp;
    temp = active;
    while (temp.place == RIGHT) {
      temp = temp.parent;
    }
    if (temp.place == LEFT) { // exemple : 3|+2 avec temp = 3|
      temp = temp.parent;
      Expression right = (Expression) temp.R; // 2
      if (temp.op == JUX && right.L.equals("TBOX") && ((Expression[]) right.R).length > 1) {
        right = deleteNode(right, 0);
      } else if (temp.op == JUX && right.L.equals("BLOC")) {
        return false;
      } else if (temp.op == JUX && right.op == VAR) {
        substitute(temp, active);
      } else if (temp.equals(p) && p.op != JUX) {
        p.op = JUX;
      } else if (temp.op >= IS) { // 
        //* modif 10 juillet 2008
        if(temp.op == JUX) {temp = substitute(temp, (Expression) temp.L);} // a| 7
        else if(p == temp.L) { // exemple : (2-a|) +8 pas de changement
          temp.op = JUX;
          if (active == p.R) {
            deleteNode(p, RIGHT);
            insertNode(right, active, LEFT);
          }
        }
        //*/
        /* avant modif 10 juillet 2008
        temp = substitute(temp, (Expression) temp.L);
        //*/
      }
    } else if (temp.place >= 0 && temp.place != LAST) {
      deleteNode(temp.parent, temp.place + 1);
    } else {
      return false;
    }
    return true;
  }

  /**
   * transforme l'arbre root contenant l'expression active
   * @param kcode le code entier de l'événement
   * @param c le charactère
   * @param active
   * @return vrai si l'arbre a changé
   */
  public static boolean handleKey(int kcode, char c, Expression active) {
    Expression p = active.parent, temp, space = var(" ");
    space = build(FUNC, "TBOX", new Expression[]{space});
    boolean changed = false;
    if (Character.isLetterOrDigit(c) || c == '.' || c == '\'') {
      changed = insertAlphanum(c, active);
    } else if (Character.isSpaceChar(c)) {
      changed = insertSpace(active);
    } else if (c == '(') {
      changed = insertFunc(active);
    } else if ("+-*/^=<>".indexOf(c) != -1) {
      changed = insertOperator(Parser.parseOp(c), active, space);
    } else if (c == ',') {
      changed = comma(active, space);
    } else if (kcode == VK_LEFT) {
      changed = left(active, space);
    } else if (kcode == VK_RIGHT) {
      changed = right(active, space);
    } else if (kcode == VK_UP) {
      changed = up(active, space);
    } else if (kcode == VK_DOWN) {
      changed = down(active);
    } else if (kcode == VK_BACK_SPACE) {
      changed = backSpace(active);
    } else if (kcode == VK_DELETE) {
      changed = delete(active);
    }
    if (changed && active.parent != null && active.parent.op != NUL) {
      p = active.parent;
      if (p.L.equals(space) || p.R.equals(space)) {
        substitute(active.parent, active);
      } // enlever l'espace inutile

      while (p.op == JUX && p.parent != null && ("BBOX".equals(p.parent.L) || "BLOC".equals(p.parent.L))) {
        deleteNode(p, active.place);
        insertNode(p.parent, active, active.place);
        p = active.parent;
      }
    }
    return changed;
  }
}

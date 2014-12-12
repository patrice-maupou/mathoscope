/*
 * Expression.java
 *
 * Created on 2 novembre 2002, 10:55
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html
 */

package expressions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import root.Tree;

/** Classe des expressions mathématiques
 * @author Patrice Maupou
 */
public class Expression {
  
  /** Creates a new instance of Expression */
  public Expression() {
    op = NUL;
    parent = NULL;
    place = Tree.ROOT;
  }
  
  /** construit une expression à partir d'une opération binaire
   * @param L membre de gauche
   * @param R membre droit
   * @param op opérateur
   */
  private Expression(int op, Object L, Object R) {
    this.op = op;
    this.L = L;
    this.R = R;
    parent = NULL;
    place = Tree.ROOT;
  }
  
  /** construit une expression numérique ou algébrique suivant le type d'opérande
   *  (méthode de type factory), attention aux expressions à évaluer de types :
   *  applique(expr, ..) ou transforme(expr, ...)
   * @param op l'opérateur
   * @param e0 premier opérande
   * @param e1 second opérande
   * @return l'expression construite ou NULL
   */
  public static Expression build(int op, Object e0, Object e1) {
    Expression expr = NULL;
    Object s0 = getType(e0), s1 = getType(e1);
    boolean g0 = ALL.equals(s0), g1 = ALL.equals(s1), g = false;
    if(e0 instanceof Expression && "caret".equals(((Expression)e0).L))  g = true;
    else if(e1 instanceof Expression && "caret".equals(((Expression)e1).L))  g = true;
    else if(op == JUX) g = true;
    switch (op) {
      case NUL :
        if(e0 instanceof Boolean)
          return (((Boolean)e0))? Relation.TRUE : Relation.FALSE;
        break;
      case SEQ : // e0 est "h" ou "v"
      case SET : // build(SET, "{}", (1,2))
        g = g1 || e1 instanceof Expression[];
        g = g || e0 instanceof String || e0 instanceof Expression[];
        break;
      case INT :
        return (e0 instanceof BigInteger)? new Numeric((BigInteger)e0) : NULL;
      case DEC :
        return (e0 instanceof BigDecimal)? new Numeric((BigDecimal)e0) : NULL;
      case VAR :
        g = e0 instanceof String;
        break;
      case SER :
      case FUNC :
        g = e0 instanceof Expression || e0 instanceof String;
        g = g && e1 instanceof Expression[];
        break;
      case EQU :
      case LT :
      case LE :
      case GT :
      case GE:
      case IN :
        return Relation.build(op, e0, e1);
      case SUM :
      case SUB :
      case MUL :
      case DIV :
      case EXP :
        if (e0 instanceof Numeric && e1 instanceof Numeric)
          return new Numeric(op, (Numeric)e0, (Numeric)e1);
        else g = (g0 || ALG.equals(s0)) && (g1 || ALG.equals(s1));
        break;
      case CUP :
      case CAP :
        return Relation.build(op, e0, e1);
      case OR :
      case AND :
      case IMP :
      case NOT :
      case IS :
        return Relation.build(op, e0, e1);
      default :
        break;
    }
    if(g) expr = new Expression(op, e0, e1);
    return expr;
  }
  
  /** Création d'une variable à partir d'un nom
   * @param name le nom de la variable
   * @return la variable créée
   */
  public static Expression var(String name) {
    return build(VAR, name, null);
  }  
    
  /** Extrait les variables de l'expression
   * @return la liste des variables
   */
  public ArrayList<Expression> extractVars() {
    ArrayList<Expression> listvars = new ArrayList<Expression>();
    if(this.op == VAR && !var("constant").equals(R)) listvars.add(this);
    else {
      if(L instanceof Expression) listvars.addAll(((Expression)L).extractVars());
      if(R instanceof Expression) listvars.addAll(((Expression)R).extractVars());
      else if (R instanceof Expression[]){
        Expression[] Rs = (Expression[])R;
          for (Expression e : Rs) {
              listvars.addAll(e.extractVars());
          }
      }
    }
    return listvars;
  }
  
 
  
  /** copie une expression
   * @return une copie de l'expression
   */
  public Expression copy() {    
    if("caret".equals(L)) return this;
    Expression ret;
    if(op == NUL || op == INT || op == DEC) return build(op, L, R);
    Object nL = L, nR = R;
    if(L instanceof Expression) nL = ((Expression)L).copy();
    if(R instanceof Expression) nR = ((Expression)R).copy();
    else if(R instanceof Expression[]) {
      Expression[] coors = (Expression[])R;
      Expression[] ncoors = new Expression[coors.length];
      for (int i = 0; i < coors.length; i++) {
        ncoors[i] = coors[i].copy();
      }
      nR = ncoors;
    }
    ret = (L instanceof String)? build(op, L, nR) : build(op, nL, nR);
    ret.parent = parent;
    ret.place = place;
    return ret;
  }
    
  /** marque les variables locales et paramètres d'une fonction
   * f(x,y,z)=applique(..) , marquer x,y,z, puis examiner applique(..)
   * TODO : marquer les fonctions génériques de type ?f
   * @param v sous expression examinée
   * @param defs la liste des définitions
   * @param start début de la liste
   * @param func si vrai, on scanne la sous-expression
   * @return l'expression marquée
   */
  public Expression markFuncVars(Expression v, AbstractList<Expression> defs, int start, boolean func) {
    Expression ret = this, w = v;
    if(v.op == VAR && !((String)v.L).contains("$")) { // variable non encore marquée 
      int n = -1;
      do {        
        n++;
        w = var((String)v.L + "$" + n);
      } while (defs.indexOf(w) != -1);
      ret = ret.substitute(v, w);
    }
    else if(!(v.R instanceof Expression[]) && v.op != EQU && v.op != IS) return ret;
    else {
      if(v.op == IS || v.op == EQU) { // (a,b)->(2,3) ou f(x)= 2 + x ou f(x)=applique(..)
        ret = ret.markFuncVars((Expression)v.L, defs, start, true);
        v = (Expression)v.R;
      }
      if(v.op == SEQ || v.op == SET || (func && v.op == FUNC)) { // fonction, séquence, ensemble
        Expression[] coors = (Expression[])v.R;
        func = !"applique".equals(v.L);
          for (Expression coor : coors) {
              if (coor.op != SET) {
                  ret = ret.markFuncVars(coor, defs, start, func); // TODO : à revoir
              }
          }
      }      
    }
    return ret;
  }
  
  /**
   * change le nom d'une fonction générique dans l'expression
   * @param name le nom actuel à changer s'il commence par ?
   * @param newname le nouveau nom ?f0, ?f1, etc..
   * @return l'expression
   */
  private Expression changeFuncNames(AbstractList<Expression> defs, int start, HashMap<String, String> change) {
    Expression ret = copy();
    if (ret.L instanceof String) {
      String name = (String) ret.L, newname;
      if (name.startsWith("?")) {
        newname = change.get(name);
        if (op == FUNC && newname == null) {
          newname = name;
          int n = -1;
          while (defs.indexOf(var(newname)) != -1) {
            n++;
            newname = newname + n;
          }
          change.put(name, newname);
        }
        ret.L = newname;
      }
    }
    else {
      if (ret.L instanceof Expression) {
        ret.L = ((Expression) ret.L).changeFuncNames(defs, start, change);
      }
      if (ret.R instanceof Expression) {
        ret.R = ((Expression) ret.R).changeFuncNames(defs, start, change);
      }
      else if (ret.R instanceof Expression[]) {
        Expression[] coors = (Expression[]) ret.R;
        for (int i = 0; i < coors.length; i++) {
          coors[i] = coors[i].changeFuncNames(defs, start, change);
        }
      }
    }
    return ret;
  }
  
  
  /** remplace une expression v par une expression e
   * @param v la variable
   * @param e l'expression à substituer
   * @return l'expression résultat
   */
  public Expression substitute(Expression v, Expression e) {
    Expression r = build(op, L, R);
    Object nL = L, nR = R;
    if (r.equals(v)) r = (r.op == VAR && e.op == VAR)? build(VAR, e.L, R) : e;
    else {
      if(L instanceof Expression) nL = ((Expression)L).substitute(v, e);
      if(R instanceof Expression) nR = ((Expression)R).substitute(v, e);
      else if(R instanceof Expression[]) {
        Expression[] coors = (Expression[])R;
        Expression[] ncoors = new Expression[coors.length];
        for (int i = 0; i < coors.length; i++)
          ncoors[i] = coors[i].substitute(v, e);
        return build(op, nL, ncoors);
      }
      r = build(op, nL, nR);
    }
    return r;
  }
  
  
  /** c'est une égalité structurelle
   * @param obj l'objet à comparer
   * @return vrai s'il y a égalité
   */
  @Override
  public boolean equals(Object obj) {
    boolean retValue = super.equals(obj);
    if (obj instanceof Expression) {
      Expression e = (Expression)obj;
      if (op >= IS) {
        retValue = (op == e.op && L.equals(e.L) && R.equals(e.R));
      } else {
        switch (op) {
          case NUL :
            retValue = (e.op == NUL);
            break;
          case INT :
          case DEC :
          case VAR :
            retValue = (op == e.op) && L.equals(e.L);
            break;
          case SET :
          case SEQ :
          case FUNC :
          case SER :
            retValue = (op == e.op) && (L.equals(e.L) || op == SEQ);
            if(retValue) {
              Expression[] e1 = (Expression[])R;
              Expression[] e2 = (Expression[])e.R;
              for (int i = 0; i < e1.length && i < e2.length && retValue; i++) {
                retValue = e1[i].equals(e2[i]);
              }
            }
            break;
          default :
            break;
        }
      }
    }
    return retValue;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 11 * hash + (this.L != null ? this.L.hashCode() : 0);
    hash = 11 * hash + (this.R != null ? this.R.hashCode() : 0);
    hash = 11 * hash + this.op;
    return hash;
  }
  
  /** compare avec l'objet obj
   * @param obj l'objet à comparer
   * @return -1 si plus petit, 0 si égal, 1 si plus grand
   *
   */
  public int compareTo(Object obj) {
    Expression e = (Expression)obj, t = copy();
    if(!(t instanceof Numeric) && obj instanceof Numeric) return 1;
    if(!(obj instanceof Numeric) && t instanceof Numeric) return -1;
    int value = t.op - e.op;
    if (value == 0) {
      if (t.op == VAR)
        value = ((String)t.L).compareTo((String)e.L);
      else if (t.op == INT)
        value = ((BigInteger)t.L).compareTo((BigInteger)e.L);
      else if (t.op == DEC)
        value = ((BigDecimal)t.L).compareTo((BigDecimal)e.L);
      else if (t.R instanceof Expression[]) {
        if(t.L instanceof Expression)
          value = ((Expression)t.L).compareTo(e.L);
        else if(t.op != SEQ)
          value = ((String)t.L).compareTo((String)e.L);
        if (value == 0) {
          Expression[] c1 = (Expression[])t.R;
          Expression[] c2 = (Expression[])e.R;
          for (int i = 0; i < c1.length && i < c2.length; i++) {
            if((value = c1[i].compareTo(c2[i])) != 0) break;
          }
          if(value == 0) value = c1.length - c2.length;
        }
      } else if(t.op >= IS) {
        //TODO : est-ce nécessaire de distinguer les deux cas ?
        if(t.L instanceof Numeric && e.L instanceof Numeric && t.op != EXP) {
          value = ((Expression)t.R).compareTo(e.R);
          if(value == 0) value = ((Expression)t.L).compareTo(e.L);
        } else {
          value = ((Expression)t.L).compareTo(e.L);
          if(value == 0) value = ((Expression)t.R).compareTo(e.R);
        }
      }
    }
    if(value > 0) value = 1;
    else if(value < 0) value = -1;
    return value;
  }
  
  /**
   * retourne le type de l'expression si c'est une variable
   * @param o l'objet à examiner
   * @return le type de o
   */
  protected static Object getType(Object o) {
    Object ret = o;
    if(ret instanceof Expression) {
      Expression e = (Expression)ret;
      if(e.op == VAR) {
        if(e.L.equals("caret") || e.R == null) return ALL;
        else if(e.R instanceof Expression) {
          ret = e.R;
        }
      }
      if(ret instanceof Expression) {
        e = (Expression)ret;
        if(e.op == SET || e.op == CUP || e.op == CAP || ENSEMBLE.equals(ret)
        || INTERVAL.equals(ret))
          ret = ENSEMBLE;
        else if(e.op == SEQ || SEQUENCE.equals(ret))
          ret = SEQUENCE;
        else if(e.op == FUNC || e.op == JUX) ret = ALL;
        else if((INT<=e.op && e.op<=FUNC) || (SUM<=e.op && e.op<=EXP)) ret = ALG;
      }
    }
    return ret;
  }
  
  /** vérifie si les types correspondent
   * @param type le modèle d'expression, par exemple var^num
   * @param mark si vrai, met le type dans la variable
   * (a->num donne build(VAR,"a",num))
   * @return la relation TRUE ou FALSE
   */
  public Relation isType(Expression type, boolean mark) {
    boolean ret = false;
    try {
      Expression e = (op == VAR && R instanceof Expression)? (Expression)R : this;
      if(type.op == VAR) {
        //if("symbol".equals(type.L)) ret = e.R == null;
        if("symbol".equals(type.L)) ret = true; // modif 19 janvier 2008
        else if("num".equals(type.L)) ret = e instanceof Numeric;
        else if("relation".equals(type.L)) ret = e instanceof Relation;
        else if("alg".equals(type.L))
          ret = ((INT <= e.op && e.op <= VAR) || (SUM <= e.op && e.op <= EXP));
        else if("var".equals(type.L)) ret = (e.op == VAR);
        else if("entier".equals(type.L)) ret = (e.op == INT);
        else if("décimal".equals(type.L)) ret = (e.op == DEC);
        else if("fonction".equals(type.L)) ret = (e.op == FUNC);
        else if("ensemble".equals(type.L))
          ret = (e.op == SET || e.op == CAP || e.op == CUP);
        else if("seq".equals(type.L)) ret = (e.op == SEQ);
        else if("intervalle".equals(type.L) && e.op == SET && e.L instanceof String) {
          ret = ((String)e.L).charAt(0) == '[' || ((String)e.L).charAt(0) == ']';
        }
      } 
      else if(type.op == INT || type.op == DEC) {
        //ret = e.equals(type);
        ret = e.op == type.op;
      }
      else if(type.op == SEQ || type.op == SET) {
        if(!mark && e.op == type.op && e.L.equals(type.L)) {
          Expression[] ec = (Expression[])e.R, tc = (Expression[])type.R;
          if(ec.length == tc.length) {
            ret = true;
            for(int i = 0; i < ec.length; i++)
              ret = ret && ec[i].isType(tc[i], mark).valueOf(new ArrayList<Expression>(), -1,1);
          }
        }
      } else if(type.op == e.op) {// types composés
        if(e.L instanceof Expression && type.L instanceof Expression)
          ret = (((Expression)e.L).isType((Expression)type.L, mark)).valueOf(
                  new ArrayList<Expression>(), 0,1);
        else ret = e.L.equals(type.L);
        if(e.R instanceof Expression && type.R instanceof Expression) {
          Expression eR = (Expression)e.R;
          ret = ret && (eR.isType((Expression)type.R, mark)).valueOf(new ArrayList<Expression>(), 0,1);
        } else if(e.R == null) ret = ret && type.R == null;
      }
      if(mark && op == VAR) R = type;
    } catch(Exception exc) {}
    return (ret)? Relation.TRUE : Relation.FALSE;
  }
  
  /** vérifie si l'expression est de la forme 0 - expr
   * @return vrai si l'expression est de la forme -expr
   */
  public boolean isNegative() {
    return op == SUB && L.equals(ZERO);
  }
  
  
  /**
   * produit des définitions de variables placées dans la liste Vars
   * @param recursive si vrai, applique récursivement les définitions
   * @param Vars la liste des variables avec leur définition
   * @return les rangs des définitions dans la liste
   */
  public int[] define(ArrayList<Expression> Vars, boolean recursive) {
    int[] rgs = new int[0];
    Expression var, value;
    if(op == IS){   // définition directe : b->a+2
      var = ((Expression)L).copy();
      if(var.op == VAR) {
        value = (Expression)R; // la variable contient l'expression de remplacement
        try {
          boolean isFuncDef = value.op == EQU && var.L.equals(((Expression)value.L).L);
          var.R = (recursive && !isFuncDef && !"interactions".equals(var.L))? value.applyDefs(Vars, 0, 50, null) : value;
        } catch(Exception exc) {}
        int n = Vars.indexOf(var);
        if(n != -1) Vars.remove(n);
        else n = Vars.size();
        Vars.add(n, var);
        rgs = new int[]{n};
      } else if(var.op == SET || var.op == SEQ) { // {x,y}->.. ou (x,y)->..
        Expression[] listvars = (Expression[])var.R;
        try {
          value = ((Expression)R).applyDefs(Vars, 0, 50, null);
          if(value.op == var.op) {
            Expression[] defs = (Expression[])value.R;
            if(listvars.length == defs.length) {
              rgs = new int[defs.length];
              for(int i = 0; i < defs.length; i++) {
                rgs[i] = build(IS, listvars[i], defs[i]).define(Vars, recursive)[0];
              }
            }
          }
        } catch(Exception exc) {}
      }
    } else if(op == VAR) { // nouvel ensemble de règles
      build(IS, this, build(FUNC,"schéma",new Expression[0])).define(Vars,false);
    }
    return rgs;
  }
  
  
  /**
   * applique un ensemble de définitions
   * @return l'expression transformée
   * @throws java.lang.Exception 
   * @param defs la liste des variables contenant les définitions
   * @param start on ignore les variables précédentes
   * @param level niveau de récurrence, si nul, on sort.
   * @param trace pour debug
   */
  public Expression applyDefs(ArrayList<Expression> defs, int start, int level,
          ArrayList<Expression> trace) throws Exception {
    if (level == -100) {return this;}
    Expression ret = copy(), var;
    int index = start - 1;
    boolean numeric = true;
    if (defs.size() > 1000) {throw new Exception(" trop de variables");}
    if (level < -20) {throw new Exception(" trop d'appels ");}
    if (start != -1 && ret.op == VAR && !var("constant").equals(ret.R)) {
      int n = defs.subList(index + 1, defs.size()).indexOf(ret);
      if (n != -1) {
        index += n + 1;
        var = defs.get(index); // la variable
        if (var.equals(var.R)) {
          return ret; // pour un symbole
        }
        var = (Expression) var.R; // sa valeur
        if ("entrée".equals(var.L)) {
          return var; // la valeur est externe
        }
        if (!(var.op == EQU && ret.L.equals(((Expression) var.L).L))) { // f(x)=x^2
          ret = var;
          if (level > 0) {
            ret = ret.applyDefs(defs, start, level - 1, trace); // récurrence
          }
        }
      }
      else { // variable non reconnue
        String s = (String) ret.L;
        if (s.startsWith("`") && s.endsWith("`")) {
          ret = Parser.parse(s).applyDefs(defs, start, level, trace);
        }
      }
    }
    else if (ret.op == FUNC) {
      Expression[] coors = (Expression[]) ret.R;
      String name = (ret.L instanceof String) ? (String) ret.L : "";
      int idx = defs.lastIndexOf(var(name));
      if (idx != -1) {
        Expression varf = (Expression) defs.get(idx).R;
        if (name.startsWith("?")) { // fonction générique
          ret.L = varf.L;
        }
      }
      if ("applique".equals(name)) { // schema -> fonctions
        ret = coors[0].applyDefs(defs, start, level, trace); // l'opérande
        for (int i = 1; i < coors.length; i++) {
          try {
            if (coors[i].op == IS) { // t+1->2*a : substitution
              Expression l = ((Expression) coors[i].L).applyDefs(defs, start, level - 1, trace);
              Expression r = ((Expression) coors[i].R).applyDefs(defs, start, level - 1, trace);
              ret = ret.substitute(l, r);
            }
            else { // <schéma> ou application
              //if (coors[i].op == VAR) // nom d'un schéma
              coors[i] = coors[i].applyDefs(defs, 0, 0, trace);
              if (coors[i].op == FUNC && "schéma".equals(coors[i].L)) {
                coors[i].L = "applique";
                ret = ret.applyFunc(coors[i], defs, defs.size(), true, trace);
              }
              else if (ret.op == VAR && coors.length == 2) { // applique(f,(1,2))
                if (coors[i].op == SEQ) {
                  ret = build(FUNC, ret.L, coors[i].R);
                }
                else {
                  ret = build(FUNC, ret.L, new Expression[]{coors[i]});
                }
                ret = functionFactory.apply((String) ret.L, (Expression[]) ret.R, ret, defs, start, 50);
              }
            }
          } catch (Exception exc) {
          } // exception de calcul
        }
      }
      else { // fonction
        boolean frozen = false;
        for (int i = 0; i < coors.length; i++) {
          try {
            if (!name.equals("choix") && (!name.equals("mappe")) && (!name.equals("tests") || i % 2 == 1)) {
              coors[i] = coors[i].applyDefs(defs, start, level - 1, trace);
            }
          } catch (Exception exc) {
          }
          numeric = numeric && (coors[i] instanceof Numeric);
        }
        frozen = frozen || name.equals("tests");
        if (!frozen && start != -1 && (index = defs.indexOf(var(name))) != -1) { // fonction utilisateur
          var = defs.get(index); // f
          Expression def = ((Expression) var.R).copy(), func; // f(x) ou f(x)=...
          if (def.op == VAR) {
            if (!ret.L.equals(def.L)) { // le nom doit être différent
              ret.L = def;
              return ret.applyDefs(defs, start, level - 1, trace);
            }
            else {
              return ret;
            }
          }
          else if (def.op == EQU) { // f(x)=applique(f,x)
            Expression l = (Expression) def.L, r = (Expression) def.R;
            if (r.op == FUNC && "applique".equals(r.L)) {
              Expression fname = ((Expression[]) r.R)[0];
              if (l.L.equals(fname.L)) {
                Expression[] args = (Expression[]) ret.R;
                ((Expression[]) r.R)[1] = (args.length == 1) ? ((Expression[]) ret.R)[0] : 
                        build(SEQ, "()", args);
                return r.applyDefs(defs, start, level - 1, trace);
              }
            }
          }
          int defsize = defs.size();
          def = def.markFuncVars(def, defs, start, true); // renomme les paramètres de la fonction
          Expression mold = def.op != EQU ? def : (Expression) def.L;
          if (mold.op != FUNC) {
            return ret;
          }
          Expression[] symbs = (Expression[]) mold.R;
          if (symbs.length != 1) {
            if (coors.length == 1 && coors[0].op == SEQ) {
              coors = (Expression[]) coors[0].R;
            }
            if (symbs.length != coors.length) {
              return ret; // le nombre de variables ne correspond pas
            }
          }
          else if (coors.length != 1) // variables -> une séquence
          {
            coors = new Expression[]{build(SEQ, "()", coors)};
          }
          ret.R = coors;
          if (def.op == EQU) { // f(x,y)=applique(x+y,{x<0:-x+y}))
            func = (Expression) def.R;
            for (int i = 0; i < coors.length; i++) {
              symbs[i].R = coors[i];
              defs.add(symbs[i]);
            }
            ret = ret.applyFunc(func, defs, defsize, false, trace);
          }
        }
        if (name.equals(ret.L)) { // fonctions prédéfinies
          ret = functionFactory.apply(name, coors, ret, defs, start, 50);
        }
      }
    }
    else { // induction sur les sous-expressions
      if (ret.L instanceof Expression && ret.op != IS && !var("curseur").equals(ret.L)) {
        ret.L = ((Expression) ret.L).applyDefs(defs, start, level - 1, trace);
      }
      if (ret.R instanceof Expression) {
        ret.R = ((Expression) ret.R).applyDefs(defs, start, level - 1, trace);
      }
      else if (ret.R instanceof Expression[]) {
        Expression[] coors = (Expression[]) ret.R;
        for (int i = 0; i < coors.length; i++) {// séquence,ensemble
          try {
            coors[i] = coors[i].applyDefs(defs, start, level - 1, trace);
          } catch (Exception exc) {
          }
          numeric = numeric && (coors[i] instanceof Numeric);
        }
      }
    }
    return (ret.op == NUL) ? ret : ret.copy();
  }

  /** applique une fonction : applique(x+y, {x+y<0:-x+y}) avec x->2, y->3
   * @param func 
   * @param defs la liste des définitions
   * @param start début de la liste
   * @param schema vrai si on applique un schéma récursif, faux pour une fonction normale
   * @param trace liste pour tracer l'éxécution de la fonction
   * @return l'expression transformée
   * @throws Exception s'il n'y a pas de relation attendue, etc..
   */
  public Expression applyFunc(Expression func, ArrayList<Expression> defs, int start, boolean schema, 
    ArrayList<Expression> trace) throws Exception {
    boolean stop = false; // utilisé pour STOP(...)
    Expression ret = copy(), func0 = func.copy(), last = ret;
    defs.add(build(VAR, "image", ret));
    if(trace != null){
      trace.add(var("fonction :"));
      trace.add(ret);      
    }
    int oldsize = defs.size(), k;
    Expression img = defs.get(oldsize - 1);
    if(func.op == FUNC && "applique".equals(func.L)) {
      Expression steps[] = (Expression[])func0.R;
      for (int i = 0; i < steps.length; i++) {
        defs.subList(oldsize, defs.size()).clear();
        boolean success = false;
        //Expression step = steps[i].changeFuncNames(defs, start, new HashMap<String, String>()); // modif !
        steps[i] = steps[i].changeFuncNames(defs, start, new HashMap<String, String>());
        if(steps[i].op == SET && (steps[i].L instanceof Expression)) { // transformer
          Expression conds[] = (Expression[])steps[i].R;
          for(k = 0; k < conds.length; k++) {
            try {
              conds[k] = conds[k].applyDefs(defs, start, 1, null);
              if(!((Relation)conds[k]).valueOf(defs, start,1)) break;
            } catch(Exception exception) {break;} 
          }
          if(k == conds.length) { // succès
            ret = (Expression)steps[i].L; // l'expression transformée
            stop = ret.op == FUNC && "STOP".equals(ret.L);
            if(stop) {
              Expression coors[] = (Expression[])ret.R;
              ret = coors.length != 1 ? build(SEQ, "()", coors) : coors[0];
            }
            if(ret.op != IS) ret = ret.applyDefs(defs, start, 1, trace);
            else ret.R = ((Expression)ret.R).applyDefs(defs, start, 1, trace); // {cond : a->b} employé ?
            success = true;
            img.R = ret;
          }
          defs.subList(oldsize, defs.size()).clear();
        } 
        else if(steps[i].op == IS) { // variables locales
          if(trace != null){
            trace.add(var("variables :"));
            trace.add(steps[i]);      
          }
          ret = steps[i].copy(); 
          boolean newvars = ((Relation)ret).addVars(defs, start,1);
          if(trace != null && newvars) {
            trace.add(var("valeur :"));
            Expression[] varsdefs = new Expression[defs.size() - oldsize];
            for (int j = oldsize; j < defs.size(); j++)
              varsdefs[j-oldsize] = build(IS, defs.get(j), defs.get(j).R);
            trace.add(build(SEQ, "()", varsdefs));
          } 
          oldsize = defs.size();
          success = false;
        } 
        else { // expression quelconque
          ret = steps[i].applyDefs(defs, start, 1, trace);
          success = !ret.equals(steps[i]);
          img.R = ret;
        }
        if(trace != null && success) {
          trace.add(steps[i]);
          trace.add(ret);
        }
        ret = (Expression)img.R;
        if(stop) break;
      }
      if(schema && !stop){
        if(ret.op > IS) {
          Expression l = ((Expression)ret.L).applyFunc(func, defs, defs.size(), true, trace);
          Expression r = ((Expression)ret.R).applyFunc(func, defs, defs.size(), true, trace);
          ret = build(ret.op, l, r);
        } 
        else if(ret.R instanceof Expression[]) {
          Expression[] coors = (Expression[])ret.R;
          for(int i1 = 0; i1 < coors.length; i1++)
            coors[i1] = coors[i1].applyFunc(func, defs, defs.size(), true, trace);
          ret.R = coors;
        }
        if(!ret.equals(last))  // refaire un tour
          ret = ret.applyFunc(func, defs, defs.size(), true, trace);
      }
    } else {
      func = build(FUNC, "applique", new Expression[] {func});
      ret = ret.applyFunc(func, defs, start, schema, trace);
    }
    defs.subList(start, defs.size()).clear();
    if(trace != null) {
      trace.add(Expression.var("valeur :"));
      trace.add(ret);
    }
    return ret;
  }
  
  
  
  /**
   * Approximation décimale d'une expression numérique
   * @return le double s'il peut être calculé, Double.NaN sinon
   * @throws ArithmeticException
   */
  public double toDouble() throws ArithmeticException {
    double d = Double.NaN, d1, d2;
    Expression e = copy();
    if(e.op == VAR && "PI".equals(L)) d = Math.PI;
    if(op == FUNC) {
      String name = (String)L;
      Expression[] coors = (Expression[])R;
      d1 = coors[0].toDouble();
      if(d1 != Double.NaN) {
        if(coors.length == 1) {
          if(name.equals("cos")) d = Math.cos(d1);
          else if(name.equals("sin")) d = Math.sin(d1);
          else if(name.equals("tan")) d = Math.tan(d1);
          else if(name.equals("exp")) d = Math.exp(d1);
          else if(name.equals("ln")) d = Math.log(d1);
          else if(name.equals("log")) d = Math.log10(d1);
          else if(name.equals("rac")) d = Math.sqrt(d1);
          else if(name.equals("||")) d = Math.abs(d1);
        } 
        else if(coors.length == 2 && name.equals("angle")) {
            d2 = coors[1].toDouble();
            d = (d1 < 0)? Math.atan2(-d2, -d1) + Math.PI : Math.atan2(d2, d1);
        }
      }
    }
    if(e.L instanceof Expression) {
      if((d1 = ((Expression)e.L).toDouble()) != Double.NaN )
        e.L = build(DEC, new BigDecimal((new Double(d1)).toString()), null);
    }
    if(e.R instanceof Expression) {
      if((d1 = ((Expression)e.R).toDouble()) != Double.NaN )
        e.R = build(DEC, new BigDecimal((new Double(d1)).toString()), null);
    } else if(e.R instanceof Expression[]) {
      Expression[] coors = (Expression[])e.R;
      for(int i = 0; i < coors.length; i++) {
        if((d1 = coors[i].toDouble()) != Double.NaN )
          coors[i] = build(DEC, new BigDecimal((new Double(d1)).toString()), null);
      }
      e.R = coors;
    }
    e = build(e.op, e.L, e.R);
    if(e instanceof Numeric) d = e.toDouble();
    return d;
  }
  

  /** une représentation de l'arbre expression
   * @return la chaîne résultat
   */
  @Override
  public String toString() {
    String ret = "";
    if (op == NUL) ret = "?";
    else if (op == INT) ret = ((BigInteger)L).toString();
    else if (op == DEC) ret = ((BigDecimal)L).toString();
    else if (op == VAR) {
      ret = (String)L;
      try {
        Expression e = Parser.parse(ret);
        if(e.op == SET) ret = e.toString();
        else if(e.op != VAR) ret = "`" + ret + "`";
      } catch(Exception exc) {ret = "`" + ret + "`";}
    } else if (op == FUNC || op == SET || op == SEQ || op == SER) {
      String lastBracket = ")";
      if(op == FUNC) {
        if("unicode".equals(L)) { // unicode(0x03c0) donne PI
          Numeric ucode = (Numeric)((Expression[])R)[0];
          return String.valueOf((char)ucode.toInt());
        } else ret = L.toString() + "(";
      } else if(op == SER) {
        lastBracket = "]";
        ret = (String)L + "[";
      } else if(op == SEQ) ret = "(";
      else if(op == SET) {
        ret = "{";
        lastBracket = "}";
        if(L instanceof String) {
          ret = ((String)L).substring(0,1);
          lastBracket = ((String)L).substring(1);
        }
      }
      Expression[] coors = (Expression[])R;
        for (Expression coor : coors) {
            ret += coor.toString() + ",";
        }
      ret = ret.substring(0, ret.length() - 1);
      if(op == SET && L instanceof Expression) {// {x€A,y€B : x-y}
        ret +=  " : " + ((Expression)L).toString();
      }
      ret += lastBracket;
      if(op == SET && coors.length == 0) ret = "Ø";
    } else if (op >= 0) {
      ret = opString() + "( " + L.toString() + " " + R.toString() + ")";
    }
    return ret;
  }
  
  /** une représentation de l'expression style calculette
   * @param brackets parenthèses ou non
   * @return la chaîne de caractères
   */
  public String printout(boolean brackets) {
    String s = "", lastBracket = ")";
    if (op == FUNC || op == SER || op == SET || op == SEQ) {
      if (op == FUNC) {
        if(L instanceof Expression) {s = ((Expression)L).printout(false) + "(";}
        else if (((String)L).equals("||")) {
          s = "|";
          lastBracket = "|";
        }
        else {s = (String)L + "(";}
      }
      else if (op == SER) {
        s = (String)L + "[";
        lastBracket = "]";
      }
      else if (op == SET) {
        s = "{";
        lastBracket = "}";
        if (L instanceof String) {
          s = ((String) L).substring(0, 1);
          lastBracket = ((String) L).substring(1);
        }
      }
      else if (op == SEQ) {
        if ("i".equals(L)) {
          s = "seq(";
          brackets = true;
        }
        else if (brackets) {s = "(";}
      }
      Expression[] coors = (Expression[]) R;
        for (Expression coor : coors) {
            s += coor.printout(coor.op == SEQ) + ",";
        }
      if (coors.length != 0) {s = s.substring(0, s.length() - 1);}
      if (op == SET && L instanceof Expression) {// {x€A,y€B : x-y}
        Expression l = (Expression) L;
        s += " : " + l.printout(l.op == SEQ);
      }
      if (op != SEQ || brackets) {s += lastBracket;}
      if (op == SET && coors.length == 0) {s = "Ø";}
    }
    else if (op < IS) {s = toString();}
    else if (op >= IS) {
      Expression l = (Expression) L, r = (Expression) R;
      if (!isNegative()) {
        s += l.printout((l.op > IS) && (l.op < op));
      }
      s += opString();
      if (r.isNegative()) {
        s += r.printout(true);
      }
      else {
        s += r.printout(((r.op != IS) && (r.op < op + 2)) || (r.op <= SUB && r.lastExpression(true).isNegative())); 
        // inutile ?
      }
      if (brackets) {s = "(" + s + ")";}
    }
    return s;
  }
 
  /** donne le signe opératoire "+", "-", etc..
   * @return l'écriture du signe opératoire
   */
  public String opString() {
    String s = "";
    switch (op) {
      case NUL :
        return "?";
      case EQU :
        s += "=";
        break;
      case LT :
        s += "<";
        break;
      case GT :
        s += ">";
        break;
      case LE :
        s += "<=";
        break;
      case GE :
        s += ">=";
        break;
      case SUM :
        s += "+";
        break;
      case MUL :
        s += "×";
        break;
      case DIV :
        s += "/";
        break;
      case SUB :
        s += "-";
        break;
      case EXP :
        s += "^";
        break;
      case OR :
        s += " ou ";
        break;
      case AND :
        s += " et ";
        break;
      case IMP :
        s += " donc ";
        break;
      case NOT :
        s += "non";
        break;
      case IN :
        s += "€";
        break;
      case IS :
        s += "->";
        break;
      case CUP :
        s += " union ";
        break;
      case CAP :
        s += " inter ";
        break;
    }
    return s;
  }
  
  
  /** pour MUL trouver dans L le dernier atome et dans R le premier
   *  op dépend de la nature des deux
   * @return la chaîne formée du signe opératoire
   */
  public String op() {
    String s = "?";
    if (op == EQU) s = " = ";
    else if (op == LT) s = " < ";
    else if (op == GT) s = " > ";
    else if (op == LE) s = " \u2264 ";
    else if (op == GE) s = " \u2265 ";
    else if (op == SUM) s = "+";
    else if (op == MUL) { //TODO : mettre un signe si la juxtaposition est une expression valide
      s = "";
      Expression al, ar, l = (Expression)L, r = (Expression)R;
      al = l.lastFactor(false); // L = x*2 -> 2
      ar = r.lastFactor(true); // R = 3*1 -> 3
      if ((al instanceof Numeric || al.op == VAR) && ar instanceof Numeric)
        s = "×";
      else if (l.op == DIV && r.op == DIV)
        s = "×";
      else if(l.L.equals("TBOX") || r.L.equals("TBOX")) 
        s = "×";
      else if(r.op == FUNC) s = "emsp";
      else if(l.equals(var("caret")) || r.equals(var("caret"))) 
        s = "×";
      else if(l.op == JUX || r.op == JUX) s = "×";
    } 
    else if (op == SUB) s = "-";
    else if (op == DIV) s = "/";
    else if (op == EXP) s = "^";
    else if (op == OR) s = " ou ";
    else if (op == AND) s = " et ";
    else if (op == IMP) s = " donc ";
    else if (op == NOT) s = "non";
    else if (op == IN) s = "\u2208";
    else if (op == IS) s = "\u21a6";
    else if(op == CAP) s = "\u2229";
    else if(op == CUP) s = "\u222A";
    else if(op == JUX) s = "";
    return s;
  }
  
  /** retourne le dernier facteur gauche ou droit de l'expression, <br>
   * exemple : (2*(x*1))*(5*3*1) renvoie 1 ou 5
   * @param dir si true, recherche le dernier facteur de l'expression à gauche
   * @return le dernier atome (variable ou rationnel) à gauche ou à droite
   * de l'expression
   */
  private Expression lastFactor(boolean dir) {
    Expression a = copy();
    if (op == MUL)
      a = (dir) ? ((Expression)L).lastFactor(dir) : ((Expression)R).lastFactor(dir);
    return a;
  }
  
  /** dernière expression à gauche ou à droite avant un atome
   * @param dir 
   * @return 
   */
  protected Expression lastExpression(boolean dir) {
    Expression a = this;
    if (a.op < 0)
      return a;
    else if (dir) {
      if (((Expression)a.L).op > 0)
        a = ((Expression)a.L).lastExpression(dir);
    } else {
      if (((Expression)a.R).op > 0)
        a = ((Expression)a.R).lastExpression(dir);
    }
    return a;
  }
  
  
  public Object L, R; // opérandes
  public Expression parent;
  public int op, place; // type d'opération
  public static ArrayList<Expression> decofigs;
  
  public static final int NUL = -10;
  public static final int SEQ = -9;
  public static final int SET = -8;
  public static final int INT = -7;
  public static final int DEC = -6;
  public static final int VAR = -5;
  public static final int SER = -4;
  public static final int FUNC = -3;
  public static final int IS = -1;
  public static final int OR = 1; 
  public static final int AND = 2; 
  public static final int IMP = 3;
  public static final int NOT = 4;
  public static final int IN = 5;
  public static final int EQU = 6;
  public static final int LT = 7;
  public static final int LE = 8;
  public static final int GE = 9;
  public static final int GT = 10;
  public static final int SUM = 12;
  public static final int SUB = 13;
  public static final int MUL = 15;
  public static final int DIV = 16;
  public static final int EXP = 18;
  public static final int CUP = 21; // union
  public static final int CAP = 22; // inter
  public static final int JUX = 100;
  
  public final static Numeric ZERO = new Numeric(0);
  public final static Numeric ONE = new Numeric(1);
  public static final Expression NULL = new Expression();
  public static final Expression ALL = new Expression(VAR, "tout", null);
  public final static Expression ENSEMBLE =  build(VAR, "ensemble", null);
  public final static Expression INTERVAL =  build(VAR, "intervalle", null);
  public final static Expression SEQUENCE =  build(VAR, "sequence", null);
  public final static Expression ALG =  build(VAR, "alg", null);
  public final static Expression ENTIER =  build(VAR, "entier", null);
  public final static Expression DECIMAL =  build(VAR, "décimal", null);
  
  public final static FunctionFactory functionFactory = new FunctionFactory();
  
}
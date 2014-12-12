/*
 * Relation.java
 *
 * Created on 1 décembre 2002, 16:31
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package expressions;

import java.util.*;

/** Décrit une relation entre deux expressions
 *  mathématiques : = , < , >  logiques : non, et, ou , implique
 * @author  Patrice Maupou
 */
public class Relation extends Expression {
  
  /** Creates a new instance of Relation */
  public Relation() {
    op = NUL;
  }
  
  private Relation(int op, Object L, Object R) {
    this.op = op;
    this.L = L;
    this.R = R;
  }
  
  
  /** construit une relation
   */
  public static Expression build(int op, Object e0, Object e1) throws ClassCastException {
    Object s0 = Expression.getType(e0), s1 = Expression.getType(e1);
    boolean g0 = ALL.equals(s0), g1 = ALL.equals(s1), g = false;
    switch (op) {
      case IS:
        g = ((Expression)e0).op == VAR;
        g = g || ((g0 || s0 instanceof Expression) && (g1 || s1 instanceof Expression));
        break;
      case IN:
        g = g1 || s1.equals(ENSEMBLE);
        break;
      case EQU:
      case LT:
      case GT:
      case LE:
      case GE:
        g = (g0 || ALG.equals(s0)) && (g1 ||  ALG.equals(s1));
        break;
      case AND:
      case OR:
      case IMP:
        g = (g0 || s0 instanceof Relation) && (g1 || s1 instanceof Relation);
        break;
      case NOT:
        g = g0 ||(s0 instanceof Relation && s1.equals(s0));
        break;
    }
    return new Relation(op, e0, e1);
  }
  
  /** substitue l'expression e à l'expression v dans la relation
   */
  @Override
public Expression substitute(Expression v, Expression e) {
    if(op == NUL) return this;
    Expression l = (Expression)L, r = (Expression)R;
    if((op >= IS && op <= GT))
      return build(op, l.substitute(v, e), r.substitute(v, e));
    else if(op == CAP && op == CUP)
      return build(op, l.substitute(v, e), r.substitute(v, e));
    else return this;
  }
  
  /** ajoute des variables à la liste defs si l'expression convient :  x+y->3+2
   *  opérande(a,1)->3+2 ou opérande(s,[3,5])->2, donc remplacer par la variable `opérande(s,[3,5])`
   * @param defs liste courante des variables
   * @param start entier à partir duquel on ajoute les variable à la liste
   * @return vrai si l'expression convient
   */
  
  public boolean addVars(ArrayList<Expression> defs, int start, int level) {
    boolean value = true;
    ArrayList<Expression> nvars = new ArrayList<Expression>(), nvals = new ArrayList<Expression>();
    try {
      Expression L1 = (Expression)L, R1 = (Expression)R;
      Expression e = schema(L1, "_", defs, start).applyDefs(defs, start, level, null);
      R1 = R1.applyDefs(defs, start, level, null);
      value = match(R1, e, nvars, nvals);
      if(value) { // ajouter les variables à la liste defs
        for(int m = 0; m < nvars.size(); m++) {
          Expression nvar = nvars.get(m);
          Expression nval = nvals.get(m);
          nvar.L = ((String)nvar.L).substring(1);
          nvar.R = nval;
          defs.add(nvar);
        }
      }
    } catch(Exception exc) {value = false;}
    return value;
  }
  
    
  /**
   * met un préfixe (_ ou $) devant chaque variable d'une Expression qui n'est pas répertoriée dans vars
   * si newvar == false, ou au contraire ajoute un préfixe pour créer une nouvelle variable (appelé uniquement
   * par la méthode addvars(defs, start, level)
   * @return le schéma
   * @param vars les variables à ne pas changer
   * @param pre le caractère marquant la variable
   * @param p l'expression
   * @param s le rang de départ de vars
   */
  public static Expression schema(Expression p, String pre, AbstractList<Expression> vars, int s) {
    String[] constants = {"?", "num", "relation", "alg", "vec", "var", "entier", "décimal",
      "fonction", "ensemble", "seq", "infini", "intervalle"};
    if (p.op == VAR && !((String)p.L).startsWith("?")) {
      if (p.R != null && var("constant").equals(p.R)) {return p;}
      for (int i = constants.length - 1; i > -1; i--) {
        if (constants[i].equals(p.L)) return p;
      }
      if (vars.subList(s, vars.size()).indexOf(p) == -1) {// la variable n'est pas dans vars
        return Expression.build(VAR, pre + (String) p.L, p.R);        
      }      
    }
    else if (!(p instanceof Numeric)) {
      if (p.L instanceof Expression) {p.L = schema((Expression) p.L, pre, vars, s);}
      if (p.R == null) {return build(p.op, p.L, null);}
      else if (p.R instanceof Expression) {return build(p.op, p.L, schema((Expression) p.R, pre, vars, s));}
      else if (p.R instanceof Expression[]) {
        Expression[] newcoors = new Expression[((Expression[]) p.R).length];
        for (int i = 0; i < newcoors.length; i++) {
          newcoors[i] = schema(((Expression[]) p.R)[i], pre, vars, s);
        }
        return Expression.build(p.op, p.L, newcoors);
      }
    }
    return p;
  }
  
  /** vérifie si l'expression e est conforme au modèle p qui se transforme en e si vrai
   * @param e l'expression à tester
   * @param p le modèle
   * @param vars liste des variables
   * @param exprs les valeurs des variables
   * @return vrai si l'expression est conforme au modèle
   */
  public static boolean match(Expression e, Expression p, ArrayList<Expression> vars, ArrayList<Expression> exprs) {
    boolean ret = true;
    if (p.op == VAR && (((String)p.L).startsWith("_") || ((String)p.L).startsWith("?"))) {
      int i = vars.indexOf(p);
      if (i != -1) { // déjà dans la liste vars
        Expression ei = exprs.get(i);
        if (!e.equals(ei)) return false; 
      } 
     else {// nouvelle variable
       try {
          if(p.R != null && !e.isType((Expression)p.R, false).valueOf(new ArrayList<Expression>(), -1,1))
            return false;         
       } catch (Exception exc) {return false;}              
       vars.add(var((String)p.L)); 
       exprs.add(e);
     } // p devient égal à e
      p.op = e.op;
      p.L = e.L;
      p.R = e.R;
    } 
    else if (p.op == e.op && p.R instanceof Expression[] && e.R instanceof Expression[]) {
      if(e.op == FUNC && ((String)p.L).startsWith("?")) { // fonction générique
        vars.add(var("_"+(String)p.L));
        p.L = e.L;
        exprs.add(var((String)e.L));
      }
      else if(e.op != SEQ && !p.L.equals(e.L)) return false;
      Expression[] eR = (Expression[])e.R, pR = (Expression[])p.R;
      if(eR.length != pR.length) return false;
      for (int k = 0; k < eR.length; k++) { // match les coordonnées
        ret = ret && match(eR[k], pR[k], vars, exprs);
      }
    }
    else if (p.op >= IS) { // opérateur binaire
      if (p.op == e.op) {
        ret = match((Expression)e.L, (Expression)p.L, vars, exprs);
        ret = ret && !(p.R == null ^ e.R == null);
        if(ret && p.R != null) 
          ret = match((Expression)e.R, (Expression)p.R, vars, exprs);
      }
      else ret = false;
    }
    else ret = p.equals(e); // entier ou décimal
    return ret;
  }
  
  /** renvoie la valeur logique de la relation numérique
   * @param defs liste de définitions à prendre en compte
   * @param start index de début des définitions
   * @return la valeur logique de la relation
   * @throws Exception si indétermination
   */
  public boolean valueOf(ArrayList<Expression> defs, int start, int level) throws Exception {
    boolean value = false;
    if(this == TRUE) return true;
    else if(this == FALSE) return false;
    if(op == OR || op == AND || op == IMP) {
      Relation Rl = (Relation)L;
      Relation Rr = (Relation)R;
      if(op == OR) value = Rl.valueOf(defs, start, level) || Rr.valueOf(defs, start, level);
      else if(op == AND) value = Rl.valueOf(defs, start,level) && Rr.valueOf(defs, start,level);
      else if(op == IMP) value = !Rl.valueOf(defs, start, level) || Rr.valueOf(defs, start, level);
    }
    else if(op == NOT) {
      value = !((Relation)L).valueOf(defs, start, level);
    }
    else if(op == IS) { // u->vec, u+v->2+3, non(u+v->x+1)
      Expression L1 = (Expression)L , R1 = (Expression)R;
      if(start == -1) // pour les anciens schémas
        return L1.isType(R1, false).valueOf(defs, start, level);
      if (R1.op == VAR) { // vérifie un type
        if("constant".equals(R1.L)) return true;
        String[] constants =
        {"num","alg","vec","var","entier","décimal","fonction", "ensemble","seq","intervalle"};
        for(int i = constants.length-1; i > -1; i--) {
          if(constants[i].equals(R1.L)) {
            if(L1.op != VAR || defs.subList(start, defs.size()).indexOf(L1) != -1) {
              L1 = L1.applyDefs(defs, start, 1,null);
              return L1.isType(R1, false).valueOf(defs, start, level);
            }
            else return true; // nouvelle variable
          }
        }
      }
      value = addVars(defs, start, level); // a+b->1+2, ajouter les variables a->1 et b->2
    }
    else {
      Expression L1 = ((Expression)L), R1 = ((Expression)R);
      L1 = L1.applyDefs(defs, start, 1,null);
      R1 = R1.applyDefs(defs, start, 1,null);
      if(op == IN && (L1.op != SET || !"{}".equals(L1.L))) { // construit un ensemble
      	L1 = Expression.build(SET, "{}", new Expression[] {L1});
      }
      if((op == EQU || op == IN) && L1.op == SET && R1.op == SET) { // égalité ou inclusion d'ensembles
        Expression[] coors1 = (Expression[])L1.R, coors2 = (Expression[])R1.R;
        // TODO : cas des intervalles, 3€[1,5], etc
        if(!"{}".equals(R1.L)) {
          char[] brackets = ((String)R1.L).toCharArray();
          if("{}".equals(L1.L)) {
            for (int i = 0; i < coors1.length; i++) {
              int op1 = (brackets[0] == '[')? GE : GT;
              if (!((Relation)build(op1, coors1[i], coors2[0])).valueOf(defs, start,1))
                return false;
              int op2 = (brackets[1] == ']')? LE : LT;
              if (!((Relation)build(op2, coors1[i], coors2[1])).valueOf(defs, start,1))
                return false;
            }
            return true;
          }
        }
        int m = coors1.length, n = coors2.length;
        ArrayList<Expression> v1 = new ArrayList<Expression>(m), v2 = new ArrayList<Expression>(n);
        for(int i = n-1; i > -1; i--) v2.add(coors2[i]);
        for(int i = m-1; i > -1; i--) {
          if(v2.indexOf(coors1[i]) == -1) return false;
          v1.add(coors1[i]);
        }
        if (op == EQU) {
          if(m != n) return false;
          for (int i = n - 1; i > -1; i--) {if (v1.indexOf(coors2[i]) == -1) return false;}
        }
        return true;
      }
      else if(op == EQU && L1.equals(R1)) return true;
      double dl = ((Numeric)L1).compute().toDouble(), dr =((Numeric)R1).compute().toDouble();      
      switch (op) { // on compare les approximations de format double
        case Expression.EQU :
          value = (dl == dr);
          break;
        case Expression.LT :
          value = (dl < dr);
          break;
        case Expression.GT :
          value = (dl > dr);
          break;
        case Expression.LE :
          value = (dl <= dr);
          break;
        case Expression.GE :
          value = (dl >= dr);
          break;       
      }
    }
    return value;
  }
  
    
  /** une représentation de la relation style calculette
   * @param brackets parenthèses ou non
   * @return la chaîne de caractères
   *
   */
  @Override
public String printout(boolean brackets) {
    if(this == TRUE) return "TRUE";
    else if(this == FALSE) return "FALSE";
    Expression l = (Expression)L, r = (Expression)R;
    boolean inBrackets = (op <= GT || op == IN)? false : true;
    String s = l.printout(inBrackets || l.op == SEQ) + opString()
              + r.printout(inBrackets || r.op == SEQ);
    if(op == NOT) s = opString() + "(" + l.printout(false) + ")";
    if(brackets) s = "(" + s + ")";
    return s;
  }
  
  /** une représentation de l'arbre relation
   * @return la chaîne résultat
   *
   */
  @Override
public String toString() {
    String s;
    if(this == TRUE) s = "TRUE";
    else if(this == FALSE) s = "FALSE";
    else if(op == OR || op == AND || op == IMP)
      s = "("+ L.toString()+")" + opString() +"("+ R.toString()+")";
    else if(op == NOT)
      s = opString() +"("+ L.toString()+")";
    else s = L.toString() + opString() + R.toString();
    return s;
  }
  
  public static Relation TRUE = new Relation(NUL, Boolean.TRUE, null);
  public static Relation FALSE = new Relation(NUL, Boolean.FALSE, null);
  
}

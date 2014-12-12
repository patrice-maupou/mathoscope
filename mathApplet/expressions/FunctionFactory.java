/*
 * FunctionFactory.java
 *
 * Créé le 19 mai 2007, 16:43 
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package expressions;

import static expressions.Expression.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;


/**
 *
 * @author Patrice Maupou
 */
public class FunctionFactory {
  
  static String[] names = {"approche", "arrangement","calcule", "card", "choix", "compare", "compose", "décompose",
      "ensemble", "euclide", "mappe", "opérande", "joint", "sépare", "pgcd", "seq", "tests", "var","scinde","dans",
      "stylefonc", "styleop"};
  static String[] methods = {"round", "permutation","compute", "card", "choice", "compare", "makeup", "breakdown",
       "set", "euclide", "map", "operand", "join", "split", "gcd", "seq", "tests", "makevar","splitvar","contains",
       "func_style", "op_style"};
  static HashMap<String, Method> table = new HashMap<String, Method>(25);
  private static String[] opStrs = {"PLUS","MOINS","MUL","DIV","EXP","EGAL","INF","SUP","INFEG","SUPEG","NON",
                                    "OU","ET","DONC","UNION","INTER"};
  private static int[] ops = {SUM,SUB,MUL,DIV,EXP,EQU,LT,GT,LE,GE,NOT,OR,AND,IMP,CUP,CAP};
  static HashMap<String, Integer> toOp = new HashMap<String, Integer>();
  static HashMap<Integer, String> toVar = new HashMap<Integer, String>();
  ArrayList<Expression> defs = new ArrayList<Expression>();
  int start = 0, level = 0;  
  Random rnd = new Random();
  private boolean active = true;
  
  
  /** Creates a new instance of FunctionFactory */
  public FunctionFactory() {
    for (int i = 0; i < opStrs.length; i++) {
      toOp.put(opStrs[i], ops[i]);
      toVar.put(ops[i], opStrs[i]);
    }    
    try {
      for (int i = 0; i < names.length; i++) {
        Method method = getClass().getMethod(methods[i], Expression[].class);
        table.put(names[i], method);
      }
    } catch (SecurityException ex) {
      ex.getMessage();
    } catch (NoSuchMethodException ex) {
      ex.getMessage();
    }
  }

  /** applique une méthode de nom donné, en tenant compte d'une liste de définitions
   * @param name nom de la méthode invoquée
   * @param coors liste des paramètres
   * @param old l'expression initiale en cas d'échec
   * @param defs la liste des définitions
   * @param start début de la liste
   * @param level niveau de récurrence
   * @return 
   */
  public Expression apply(String name, Expression[] coors, Expression old, ArrayList<Expression> defs,
          int start, int level) {
    Expression ret = old;
    if(active) {
      this.defs = defs;
      this.level = level;
      int defsize = this.defs.size();
      try {
      /* développer pour les tests :
      boolean numeric = true;
      for (int i = 0; i < coors.length; i++) {
        if((!name.equals("mappe")) && (!name.equals("tests") || i%2==1))
          coors[i] = coors[i].applyDefs(defs, start, level-1);
        numeric = numeric && (coors[i] instanceof Numeric);
      }
      //*/
        ret = (Expression)table.get(name).invoke(this, (Object)coors);
      } catch (Exception ex) {
        ret = old;
      }
      defs.subList(defsize, defs.size()).clear();
    }
    return ret;
  }
  
  /** approche(a,14.1,3), le plus proche à 14.1 près à partir de 3
   */
  public Expression round(Expression[] coors) throws Exception {
    Expression ret = null;
    int scale = 0;
    double x0 = coors[0].toDouble(), x = Math.abs(x0), d = 1;
    if(coors.length >= 2) { // arrondi à d=1 par défaut, 10^(-d) di d<0
      d = coors[1].toDouble();
      if(d < 0) {
        scale = (int)(-d);
        d = Math.pow(10, d);
      }
      else scale = BigDecimal.valueOf(d).scale();
    }
    double d0 = (coors.length == 3)? coors[2].toDouble() : 0;
    x = d0 + d * Math.round((x - d0)/d);
    if(Math.floor(x) == x) ret = new Numeric((int)x);
    else {
      long X = BigDecimal.valueOf(x).scaleByPowerOfTen(scale).longValue();
      ret = build(DEC, BigDecimal.valueOf(X, scale), null);
    }
    if(x0 < 0 && x != 0) ret = build(SUB, ZERO, ret);
    return ret;
  }
  
  /** arrangement(3,{5,8,9,4}), suite de 3 éléments différents de l'ensemble {5,8,9,4}
   */
  public Expression permutation(Expression[] coors) throws Exception {
    Expression ret = null, set = coors[1].copy(), temp;
    if(coors.length == 2 && coors[0].L instanceof BigInteger && set.op == SET && "{}".equals(set.L)) {
      Expression[] seq = (Expression[])set.R;
      int p = ((BigInteger)coors[0].L).intValue(), n;
      if(p <= seq.length) {
        Expression[] vals = new Expression[p]; // p valeurs
        for (int i = 0; i < vals.length; i++) {
          n = rnd.nextInt(seq.length-i) + i;
          temp = seq[i];
          seq[i] = seq[n];
          seq[n] = temp;
        }
        System.arraycopy(seq, 0, vals, 0, p);
        ret = build(SEQ, "()", vals);
      }
    }
    else throw new Exception();
    return ret;
  }
  
 
  
  /** calcul de fractions, calcule(2-1/3) donne 5/3, calcule une séquence ou un ensemble d'expressions
   *
   */
  public Expression compute(Expression[] coors) throws Exception {
    Expression[] ncoors = new Expression[coors.length];
    Expression temp;
    System.arraycopy(coors, 0, ncoors, 0, coors.length);
    for (int i = 0; i < ncoors.length; i++) {
      if(ncoors[i] instanceof Numeric) 
        ncoors[i] = ((Numeric)ncoors[i]).compute();
      else if(ncoors[i].op == FUNC && "||".equals(ncoors[i].L)) { // |3-5|
        Expression val = compute((Expression[])ncoors[i].R);
        if(val instanceof Numeric) {
          if(((Numeric)val).toDouble() < 0) val = ((Numeric)build(SUB, ZERO, val)).compute();
          ncoors[i] = val;
        }
      } 
      else if(ncoors[i] instanceof Relation) {
        if(((Relation)coors[0]).valueOf(defs, start,1)) ncoors[i] = Relation.TRUE;
        else ncoors[i] = Relation.FALSE;
      } 
      else { // sous-expressions
        if(ncoors[i].L instanceof Expression)
          ncoors[i].L = compute(new Expression[]{(Expression)ncoors[i].L});
        if(ncoors[i].R instanceof Expression)
          ncoors[i].R = compute(new Expression[]{(Expression)ncoors[i].R});
        else if(ncoors[i].R instanceof Expression[]) {
          temp = compute((Expression[])ncoors[i].R);
          if(temp.op == SEQ) ncoors[i].R = temp.R;
          else ncoors[i].R = new Expression[]{temp};
        }        
      }
    }
    return (ncoors.length == 1)? ncoors[0] : build(SEQ, "()", ncoors);
  }
  
  /** nombre d'éléments d'une séquence ou d'un ensemble
   */
  public Expression card(Expression[] coors) {
    Expression ret = new Numeric(1);
    if(coors.length == 1 && (coors[0].op == SEQ || coors[0].op == SET)) {
      ret = new Numeric(((Expression[])coors[0].R).length);
    }
    return ret;
  }
  
  /** choix aléatoire d'éléments dans des ensembles
   *  choix([2,7],[3,8],a < b) choisit un couple (a,b) d'entiers avec a€[2,7], b€[3,8] et a<b
   *  choix([0,1],a->décimal(10)), choisit un nombre à 10 décimales entre 0 et 1
   * @param coors
   * @return 
   * @throws Exception
   */
  public Expression choice(Expression[] coors) throws Exception {
    Expression ret = null;
    ArrayList<Expression> vars = new ArrayList<Expression>(), sets = new ArrayList<Expression>(),
      conds = new ArrayList<Expression>();
    Expression set, value, type, var, previous = null;
    int defsize = defs.size(), idx;
    char c = 'a';
    for (int i = 0; i < coors.length; i++) {
      Expression e = coors[i];
      value = e.applyDefs(defs, 0, 50,null);
      if(i > 0 && value.L instanceof BigInteger && previous.op == SET) { // ensemble répété
        idx = ((BigInteger)value.L).intValue()-1;
        for (int j = 0; j < idx; j++) {
          sets.add(sets.get(sets.size()-1));
          vars.add(var(Character.toString(c)));
          c++;
        }
      }
      else if(e.op == IS && ((Expression)e.L).op == VAR) { // x->type
        idx = vars.indexOf(e.L);
        if(idx != -1) vars.get(idx).isType((Expression)e.R, true); // marquage de la variable
      }
      else if(value instanceof Relation) {conds.add(value);} // condition
      else {
        if(value.op == SET && value.L instanceof String) { // ensemble
          sets.add(e);
          vars.add(var(Character.toString(c)));
          c++;        
        }
      }
      previous = value;
    }
    // boucle de choix, on essaie 1000 fois
    choices : for(int i = 1000; i > -1; i--) {
      for(int k = 0; k < vars.size() ; k++) {
        var = vars.get(k).copy();
        if(defs.subList(defsize, defs.size()).indexOf(var) != -1) continue;
        set = sets.get(k).applyDefs(defs, 0, 50,null);
        coors = (Expression[])set.R;
        if("{}".equals(set.L)) { // x€{1,2,3}
          int n = (int)Math.floor(rnd.nextDouble()*coors.length);
          var.R = coors[n];
        } 
        else { // intervalle : x€[2,7]
          double a = ((Numeric)coors[0]).toDouble(), b = ((Numeric)coors[1]).toDouble();
          value = new Numeric(a + rnd.nextDouble()*(b-a)); // par défaut
          type = (var.R == null)? ENTIER : (Expression)var.R;
          if(type.op == FUNC && "décimal".equals(type.L)) {
            Numeric nd = (Numeric)((Expression[])type.R)[0];
            int n = (int)nd.toDouble(); // nombre de décimales
            double d = a + rnd.nextDouble()*(b-a);
            BigDecimal F = (new BigDecimal(d)).setScale(n,BigDecimal.ROUND_FLOOR);
            var.R = new Numeric(F);
          } 
          else { // nombre entier
            int ia = (int)a, ib = (int)(b+1);
            var.R = new Numeric(ia + (int)Math.floor(rnd.nextDouble()*(ib-ia)));
          }
        }
        defs.add(var);
        // vérification des conditions
        for(int j = 0; j < conds.size(); j++) { // valider les conditions
          Relation cond = (Relation)conds.get(j);
          try {
            if(!cond.valueOf(defs, 0,1)) { // retirer les variables en cause
              for(int m = 0; m < vars.size(); m++) {
                idx = defs.indexOf(vars.get(m));
                if(idx != -1) {
                  var = defs.get(idx);
                  if(!cond.equals(cond.substitute(var,(Expression)var.R)))
                    defs.remove(idx);
                }
              }
              if(i==0) System.out.println("choix non trouvé !");
              continue choices;
            }
          } catch(Exception exc) {}
        }
      }
      // retourne une séquence
      coors = new Expression[defs.size() - defsize];
      for (int k = 0; k < coors.length; k++) {coors[k] = (Expression)defs.get(k + defsize).R;}
      ret = (coors.length == 1)? coors[0] : build(SEQ, "()", coors);
      break;
    }
    return ret;
  }
  
  /** compare(x+1,y+1) = -1, 0 ou 1
   */
  public Expression compare(Expression[] coors) throws Exception {    
    return new Numeric(coors[0].compareTo(coors[1]));
  }
  
  /** Décompose l'expression récursivement
   */
  public Expression breakdown(Expression[] coors) throws Exception {
    Expression ret = null, e = coors[0], l = e;
    int n =  (coors.length == 1)? -1 : 0;
    if(coors.length == 2 && coors[1] instanceof Numeric)
      n = ((Numeric)coors[1]).toInt();
    if(n == 0) return e;
    Numeric step = new Numeric(n-1);
    Expression[] components = new Expression[3], operands;
    components[0] = new Numeric(e.op);
    String s = (e.L instanceof String)? (String)e.L : "";
    components[1] = Expression.var(s);
    if(e.R instanceof Expression[]) { // séquence, ensemble ou fonction
      operands = (Expression[])e.R;
      for (int i = 0; i < operands.length; i++) 
        operands[i] = breakdown(new Expression[]{operands[i], step});
      components[2] = Expression.build(SEQ, "()", operands);
      if(e.L instanceof Expression) components[1] = (Expression)e.L;
    } 
    else if(e.R == null || e.op == VAR) { // non décomposable
      components[0] = new Numeric(NUL);
      components[2] = e;
      if(e.op == VAR) {
        String[] letters = ((String)e.L).split("");
        Expression[] vars = new Expression[letters.length-1];
        for (int i = 1; i < letters.length; i++) {
          vars[i-1] = var(letters[i]);
        }
        components[1] = build(SEQ, "()", vars);
      }
    }
    else { // (3+2)+1 devrait donner (+,``,(3,2,1))
      operands = new Expression[] {(Expression)e.R};
      do {
        l = (Expression)l.L;
        Expression[] temp = new Expression[operands.length+1];
        temp[0] = (l.op == e.op)? (Expression)l.R : l;
        System.arraycopy(operands, 0, temp, 1, operands.length);
        operands = temp;
      } while (l.op == e.op && l.L instanceof Expression);
      for (int i = 0; i < operands.length; i++) 
        operands[i] = breakdown(new Expression[]{operands[i], step});
      components[2] = Expression.build(SEQ, "()", operands);              
    }
    ret = Expression.build(SEQ, "i", components); 
    return ret;
  }
  
  /** Reconstruit une expression déjà décomposée
   */
  public Expression makeup(Expression[] coors) {
    Expression ret = null, e = coors[0];
    int n =  (coors.length == 1)? -1 : 0;
    if(coors.length == 2 && coors[1] instanceof Numeric)
      n = ((Numeric)coors[1]).toInt();
    if(n == 0 || e.op != SEQ || !(e.R instanceof Expression[])) return e;
    coors = (Expression[])e.R;
    if(coors.length != 3) return ret;
    int op = ((Numeric)coors[0]).toInt();
    Numeric step = new Numeric(n-1);
    if(op == NUL) ret = coors[2]; // atome
    else {
      Expression[] components = new Expression[2];
      if(coors[2].op == SEQ) {
        components = (Expression[])coors[2].R;
        Object l = coors[1].L;
        if("".equals(l)) { // (+,``,(2,3,5,7)), opérateur binaire
          ret = makeup(new Expression[]{components[0], step});
          for (int i = 1; i < components.length; i++) {
            ret = build(op, ret, makeup(new Expression[]{components[i], step}));
          }
        }
        else {
          if(op == SET && l instanceof Expression) 
            l = makeup(new Expression[]{coors[1], step});
          for (int i = 0; i < components.length; i++) {
            components[i] = makeup(new Expression[]{components[i], step});
          }
          ret = build(op, l, components);// fonction : f(..)
        }
      } 
    }
    return ret;
  }
  
  /**
   * TODO transforme les opérations en fonctions, a+b devient PLUS(a,b), etc
   * @param coors liste contenant une seule expression à transformer
   * @return l'expression transformée
   */
  public Expression func_style(Expression[] coors) {
    Expression ret = coors[0].copy();
    if(ret.L instanceof Expression) ret.L = func_style(new Expression[]{(Expression)ret.L});
    if(ret.R instanceof Expression) ret.R = func_style(new Expression[]{(Expression)ret.R});
    if (ret.R instanceof Expression[]) {
      Expression[] es = (Expression[]) ret.R;
      for (int i = 0; i < es.length; i++) {
        es[i] = func_style(new Expression[]{(Expression) es[i]});
      }
      ret.R = es;
    }
    String fname = toVar.get(new Integer(ret.op));
    if(fname != null) {
      Expression l = (Expression)ret.L, r = (Expression)ret.R;
      if(r == null) {
        if(r.op == FUNC && r.L.equals(fname)) { // --<exp> ou non(non<exp>)
          ret = build(FUNC, fname, r.R);          
        }
        else build(FUNC, fname, r.L);
      }
      else {
        if(l.op == FUNC && l.L.equals(fname)) { // r->PLUS(a,b,c)
          Expression[] lcoors = (Expression[])l.R, ncoors = new Expression[lcoors.length+1];
          System.arraycopy(lcoors, 0, ncoors, 0, lcoors.length);
          ncoors[lcoors.length] = r;
          ret = build(FUNC, fname, ncoors);
        }
        else ret = build(FUNC, fname, new Expression[]{l,r});
      }
    }
    return ret;
  }
  
  /**
   * TODO : transforme les fonctions en opérations, PLUS(a,b) devient a+b, etc
   * @param coors liste contenant une seule expression à transformer
   * @return l'expression transformée
   */
  public Expression op_style(Expression[] coors) {
    Expression ret = coors[0].copy();
    if(ret.L instanceof Expression) ret.L = op_style(new Expression[]{(Expression)ret.L});
    if (ret.R instanceof Expression[]) {
      Expression[] es = (Expression[]) ret.R;
      for (int i = 0; i < es.length; i++) {
        es[i] = op_style(new Expression[]{(Expression) es[i]});
      }
      ret.R = es;
    }
    if(ret.op == FUNC) { // exemple : MOINS(a,b) avec la variable MOINS
      Integer op = toOp.get((String)ret.L); // le nom doit être une variable
      if(op != null) { // opération trouvée
        coors = (Expression[])ret.R; // liste d'opérandes
        ret = coors[0];
        if(coors.length == 1) ret = build(op, ret, null);
        else {          
          for (int i = 1; i < coors.length; i++) {ret = build(op, ret, coors[i]);}
        }
      }
    }
    return ret;
  }
  
  /** ensemble((2, 3)) donne {2,3}
   */
  public Expression set(Expression[] coors) {
    Expression ret = null;
    if(coors[0].op == SEQ) {
      ret = coors[0];
      ret.L = "{}";
      ret.op = SET;
    } else ret = build(SET, "{}", new Expression[]{coors[0]});
    return ret;
  }
  
  /** transforme une expression en suite
   */
  public Expression seq(Expression[] coors) {
    Expression ret = null;
    if(coors.length > 1) ret = build(SEQ, "i", coors);
    else if(coors[0].op == SEQ) {// seq((1,2,3)) donne ((1,2,3))
      ret = coors[0];
      coors[0].L = "i";
    } else if(coors[0].op == SET) {
      ret = coors[0];
      if(ret.L.equals("[]")) {// seq([1,4]) doit donner (1,2,3,4) et non [1,4]
        coors = (Expression[])ret.R;
        int i0 = ((Numeric)coors[0]).toInt(), i1 = ((Numeric)coors[1]).toInt(), i;
        if(i0 <= i1) {
          coors = new Expression[i1-i0+1];
          for (int j = i0; j < i1+1; j++) coors[j-i0] = new Numeric(j);
        } else {
          coors = new Expression[i0-i1+1];
          for (int j = i0; j >= i1; j--) coors[i0-j] = new Numeric(j);
        }
        ret.R = coors;
      }
      ret.L = "()";
      ret.op = SEQ;
    }
    return ret;
  }
  
  
  /** joint les ensembles ou les séquences
   *  exemples : joint({1,2} union {3}) donne {1,2,3}, 
   *    joint((1,2),3) donne (1,2,3)
   *    joint((1,2),(3,4)) donne ((1,2),3,4)
   *  @return le nouvel ensemble ou la nouvelle séquence
   */
  public Expression join(Expression[] coors) {
    Expression ret = null;
    if(coors.length == 1 && coors[0].op == SEQ && !"i".equals(coors[0].L)) { // parenthèse inutile
      coors = (Expression[])coors[0].R;
    }    
    Expression[] res = new Expression[0], n = null, u;
    boolean set = coors[0].op == CUP;
    if(set) coors = new Expression[]{(Expression)coors[0].L, (Expression)coors[0].R};
    for(int i = 0; i < coors.length; i++) {
      if(coors[i] == null || coors[i].op == NUL) continue;
      else if(set && coors[i].op == SET && "{}".equals(coors[i].L)) {
        n = (Expression[])coors[i].R;
      }
      else { // suites
        n = (coors[i].op == SEQ && !"i".equals(coors[i].L))? (Expression[])coors[i].R : new Expression[]{coors[i]};
      }
      u = new Expression[res.length + n.length];
      System.arraycopy(res, 0, u, 0, res.length);
      System.arraycopy(n, 0, u, res.length, n.length);
      res = u;
    }
    if(set) ret = build(SET, "{}", res);
    else if(res.length == 1) ret = res[0];
    else ret = build(SEQ, "()", res);    
    return ret;
  }
  
    
  /** scinde l'ensemble ou la séquence this en deux parties (sauf si insécable):
   *  A contenant les n premiers éléments et B les derniers
   * @param n le nombre d'éléments de A
   * @return A union B si les paramètres sont corrects, sinon rien ne change
   */
  public Expression split(Expression[] coors) {
    Expression ret = coors[0].copy();
    int n = 1;
    if(coors.length > 1) {
      int sgn = (coors[1].isNegative())? -1 : 1;
      if(sgn == -1) coors[1] = (Expression)coors[1].R;
      if(coors[1].op == INT) n = sgn*((BigInteger)coors[1].L).intValue();
    }
    if((ret.op == SEQ && !"i".equals(ret.L)) || (ret.op == SET && "{}".equals(ret.L))) {
      coors = (Expression[])ret.R;
      int len = coors.length;
      if(n < 0) n = len + n;
      if(0 < n  && n < len) {
        Expression[] A = new Expression[len-n];
        Expression[] B = new Expression[n];
        System.arraycopy(coors, 0, A, 0, len-n);
        System.arraycopy(coors, len-n, B, 0, n);
        if(ret.op == SET) ret = build(CUP, build(SET, "{}", A), build(SET, "{}", B));
        else {
          coors = new Expression[2];
          coors[0] = (A.length == 1)? A[0] : build(SEQ, ret.L, A);
          coors[1] = (B.length == 1)? B[0] : build(SEQ, ret.L, B);
          ret = build(SEQ, "()", coors);
        }
      }
    }
    return ret;
  }
  
  /** pgcd d'une suite d'entiers
   */
  public Expression gcd(Expression[] coors) throws Exception {
    BigInteger gcd  = (BigInteger)coors[0].L;
    for (int i = 0; i < coors.length; i++) {
      gcd = gcd.gcd((BigInteger)coors[i].L);
    }    
    return build(INT, gcd, null);
  }
  
  /** euclide(17,4) renvoie 4*4+1
   */
  public Expression euclide(Expression[] coors) throws Exception {
    BigInteger d = (BigInteger)coors[1].L;
    BigInteger[] dr = ((BigInteger)coors[0].L).divideAndRemainder(d);
    Expression ret = build(MUL, new Numeric(d), new Numeric(dr[0]));
    return build(SUM, ret, new Numeric(dr[1]));
  }
  
  /** tests(defaut, condition1, valeur1,..) renvoie la valeur qui suit la dernière condition vraie
   */
  public Expression tests(Expression[] coors) throws Exception {
    Expression ret = coors[0].applyDefs(defs, start, level - 1,null);
    for(int i = 1; i < coors.length; i += 2) { 
      if(coors[i] instanceof Relation && ((Relation)coors[i]).valueOf(defs, start,1))
        ret = coors[i+1].applyDefs(defs, start, level-1,null);
    }
    return ret;
  }
 
  /** opérande(a+b,1) donne a, opérande((1,2,3),3) donne 3, opérande((1,2,3,4),[2,4]) donne (2,3,4)
   *  opérande({2,3}) donne (2,3)
   * Si les rangs sont négatifs, on compte à rebours, opérande((1,2,3,4),[-4,-2]) donne (1,2,3)
   * @param coors 
   * @return l'opérande ou la liste des opérandes de rangs indiqués
   * @throws java.lang.Exception 
   */
  public Expression operand(Expression[] coors) throws Exception {
    Expression ret = coors[0], l, r;
    if(coors[0].R instanceof Expression) { // opération binaire
      r = (Expression)coors[0].R;
      if(coors[0].L instanceof Expression) {
        l = (Expression)coors[0].L;
        if(coors.length == 1) ret = build(SEQ, "()",new Expression[]{l,r});
        else {
          int k = ((Numeric)coors[1]).toInt()-1;
          ret = (k <= 0)? l : r;
        }
      }
    }
    else if(coors[0].R instanceof Expression[]) { // suite ou fonction
      if(coors.length == 1) ret = build(SEQ, "()",(Expression[])coors[0].R);
      else if(coors[1] instanceof Numeric) {
        int k = ((Numeric)coors[1]).toInt(), rg = k-1;
        Expression[] opers = (Expression[])coors[0].R;
        if(k < 0) k = opers.length + k + 1; // à rebours
        if(k > opers.length) k = opers.length;
        ret = opers[k-1];
      } 
      else if(coors[1].op == SET && coors[1].L.equals("[]")) { // opérande((1,2,3,4,5,6),[2,4])
        Expression[] bounds = (Expression[])coors[1].R, list = (Expression[])coors[0].R;
        bounds = (Expression[])compute(bounds).R;
        if((bounds[0] instanceof Numeric) && (bounds[1] instanceof Numeric)) {
          int d0 = ((Numeric)bounds[0]).toInt(), d1 = ((Numeric)bounds[1]).toInt();
          if(d0 < 0) d0 = list.length + d0 + 1;
          if(d1 < 0) d1 = list.length + d1 + 1;
          if(d1 == -1 || d1 > list.length) d1 = list.length;
          if(d1 >= d0 && d0 > 0) {
            Expression[] opers = new Expression[d1-d0+1];
            System.arraycopy(list, d0-1, opers, 0, d1-d0+1);
            coors[0].R = opers;
            ret = coors[0];
            ret = build(SEQ, "()", opers);
          }
        }
      } 
      else if(coors[1].op == SEQ) {
        Expression[] list = (Expression[])coors[0].R, rgs = (Expression[])coors[1].R;
        Expression[] nlist = new Expression[rgs.length];
        for (int i = 0; i < rgs.length; i++) {
          int rg = (rgs[i] instanceof Numeric)? ((Numeric)rgs[i]).toInt()-1 : -1;
          if(0 <= rg && rg < list.length) nlist[i] = list[rg];
          else if(rg < 0 && list.length + rg + 1 >= 0) {
            nlist[i] = list[list.length + rg + 1];
          }
        }
        ret = build(SEQ, "()", nlist);
      }
      else throw new Exception();
    }
    else throw new Exception();
    return ret;
  }
  
  /** mappe(f,(x,y,z),(a,b)) donne (f(x,a),f(y,b),f(z,b))
   * @param coors les paramètres de la fonction
   * @return le résultat
   * @throws java.lang.Exception 
   */
  public Expression map(Expression[] coors) throws Exception {
    Expression ret = null;
    int k = coors.length, nb = 1, i = 0, lead = 0;
    Expression[] operands = new Expression[k-1], component, vars, conds, conds0 = new Expression[0];
    if(coors[0].op == SET && coors[0].L instanceof Expression) { // avec conditions
      conds0 = (Expression[]) coors[0].R;
      coors[0] = (Expression) coors[0].L;
    }
    if(coors[0].op == IS) { // (x,y)->f(x,y)+g(x)
      Expression listvars = (Expression) coors[0].L;
      coors[0] = (Expression) coors[0].R;
      vars = (listvars.op == SEQ)? (Expression[]) listvars.R : new Expression[]{listvars};
    } 
    else if(coors[0].op == FUNC) // fonction simple : f(x,y)
      vars = (Expression[])coors[0].R;
    else if(coors[0].op == VAR) { // fonction déterminée par une variable: mappe(fonc,s,t)
      coors[0] = coors[0].applyDefs(defs, start, level,null);
      vars = new Expression[k-1];
      for (int j = 0; j < k-1; j++) {vars[j] = var("v"+j);}
      coors[0] = build(FUNC, coors[0].L, vars);
    } 
    else throw new Exception();
    if(k-1 != vars.length && k-2 != vars.length) throw new Exception(); // cohérence avec les variables
    ArrayList<Expression> list = new ArrayList<Expression>();
    boolean first = true;
    do { // boucle sur la suite des valeurs à appliquer
      Expression next = coors[0]; // reprise du moule
      boolean ok = true;
      conds = new Expression[conds0.length];
      System.arraycopy(conds0, 0, conds, 0, conds0.length);
      for (int j = 1; j <= vars.length; j++) { // calcul de l'image de rang j
        coors[j] = coors[j].applyDefs(defs, start, level-1,null);
        if(coors[j].op == SEQ && !"i".equals(coors[j].L)) { // coordonnées de rang j
          component = (Expression[])coors[j].R;
          if(component.length == 0) return coors[j];
          operands[j-1] = (component.length <= i)? component[component.length-1] : component[i];
          if(i == 0 && nb < component.length) { // on détermine la plus grande longueur des coordonnées
            nb = component.length;
            lead = j; // l'indice qui détermine le nombre de coordonnées en sortie
          }
        } else operands[j-1] = coors[j]; // toujours le même
      }
      for (int j = 0; j < vars.length; j++) { // on remplace les variables
        if(vars[j].op != VAR) return ret;
        next = next.substitute(vars[j], operands[j]);
        if(first) coors[k-1] = coors[k-1].substitute(vars[j], operands[j]);
        for (int l = 0; l < conds.length; l++) {
          conds[l] = conds[l].substitute(vars[j], operands[j]);
          conds[l] = conds[l].substitute(var("rang"), new Numeric(i+1));
        }
      }
      for (int l = 0; l < conds.length; l++) {
        if(vars.length == k - 2)
          conds[l] = conds[l].substitute(var("image"),coors[k-1]).applyDefs(defs, start, level-1,null);
        ok = ((Relation)conds[l]).valueOf(defs, start,1);
        if(!ok) break;
      }
      if(!first && vars.length == k - 2 && ok) {// mappe((x,y)->image*x+y, (4,5,6), (7,8), x+y)
        coors[k-1] = next.substitute(var("image"),coors[k-1]).applyDefs(defs, start, level-1,null);
      } else if(ok) list.add(next.applyDefs(defs, start, level-1,null)); // résultat suivant si conditions vraies
      i++;
      first = false;
    } while (i < nb);
    if(vars.length == k - 2)
      return coors[k-1].applyDefs(defs, start, level-1,null); // définition récursive
    nb = list.size();
    Expression[] result = new Expression[nb];
    list.toArray(result);
    ret = coors[lead];
    if(nb > 1) ret.R = result;
    else ret = (nb == 1)? result[0] : build(SEQ, "()", result);
    return ret;
  }
  
  /** fabrique une variable par concaténation, exemple : var(a,b)
   * @param coors 
   * @return la variable
   */
  public Expression makevar(Expression[] coors) {
    String s = "";
    if(coors.length == 1 && coors[0].op == SEQ) {
      coors = (Expression[])coors[0].R;
    }
    for (int i = 0; i < coors.length; i++) {
      s += (coors[i].op == VAR)? (String)coors[i].L : coors[i].printout(false);
    }
    return var(s);
  }
  
  /** sépare les lettres d'une variable en séquence de variables
   */
  public Expression splitvar(Expression[] coors) throws Exception {
    String[] s = ((String)coors[0].L).split("");
    Expression[] ncoors = new Expression[s.length-1];
    for (int i = 0; i < ncoors.length; i++) {
      ncoors[i] = var(s[i+1]);
    }
    return build(SEQ, "()", ncoors);
  }
  
  /** 
   * teste si la seconde expression contient la première : dans(x,3-x) donne VRAI
   * @param coors le contenu et le contenant
   * @return Relation vraie ou fausse
   */
  public Expression contains(Expression[] coors) {
    Expression ret = (coors[0].equals(coors[1]))? Relation.TRUE : Relation.FALSE;
    if(ret == Relation.FALSE && coors[1].L instanceof Expression) {
      ret = contains(new Expression[]{coors[0],(Expression)coors[1].L});      
    }
    if(ret == Relation.FALSE && coors[1].R instanceof Expression) {
      ret = contains(new Expression[]{coors[0],(Expression)coors[1].R});      
    }
    else if(ret == Relation.FALSE && coors[1].R instanceof Expression[]) {
      Expression[] items = (Expression[])coors[1].R;
      for (int i = 0; i < items.length; i++) {
        ret = contains(new Expression[]{coors[0],items[i]});
        if(ret == Relation.TRUE) break;
      }
    }
    return ret;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
  
}

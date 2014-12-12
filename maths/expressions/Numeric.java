/*
 * Numeric.java
 *
 * Created on 8 décembre 2002, 18:43
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package expressions;

import java.math.*;
/**
 *
 * @author  Patrice Maupou
 * Classe ou l'on peut donner un résultat numérique
 */
public class Numeric extends Expression {
  
  
  public Numeric(int nb) {
    if(nb < 0) {
      op = SUB;
      L = new Numeric(0);
      R = new Numeric(Math.abs(nb));
    }
    else {
      op = Expression.INT;
      L = new BigInteger(""+nb);
    }    
  }
  
  public Numeric(double d) {
    if(d < 0) {
      op = SUB;
      L = new Numeric(0);
      R = new Numeric(Math.abs(d));
    }
    else {
      op = Expression.DEC;
      L = new BigDecimal(d);
    }    
  }
  
  /** construit le numeric n/d
   * @param n numérateur
   * @param d dénominateur
   */
  public Numeric(int n, int d) {
    op = DIV;    
    L = new Numeric(n);
    R = new Numeric(d);
  }
  
  protected Numeric(BigInteger nb) {
    op = Expression.INT;
    L = nb;
  }
  
  protected Numeric(BigDecimal nb) {
    op = Expression.DEC;
    L = nb;
  }
  
  protected Numeric(int op, Numeric a, Numeric b) {
    this.op = op;
    L = a;
    R = b;
  }  

  
  /** calcule une expression numérique sous forme réduite
   * 	on transforme en rationnel si on peut
     * @return 
   */
  public Expression compute() {
    Expression ret = copy();
    if(op == DEC) {
      BigDecimal D = (BigDecimal)L;
      int p = D.scale();
      BigInteger scale = (new BigInteger("10")).pow(p);
      Numeric d = new Numeric(scale); 
      Numeric n = new Numeric(D.multiply(new BigDecimal(scale)).toBigInteger());
      ret = (p == 0)? n :((Numeric)build(DIV, n, d));
    }
    if(op >= SUM && op <= EXP) { 
      Numeric l = (Numeric)((Numeric)ret.L).compute();
      Numeric r = (Numeric)((Numeric)ret.R).compute();
      try {
        BigInteger[] a = l.toBigIntegers(), b = r.toBigIntegers();
        BigInteger n = a[0], d = b[0];
        switch (op) {
          case SUM :
            n = a[0].multiply(b[1]).add(b[0].multiply(a[1]));
            d = a[1].multiply(b[1]);
            break;
          case SUB :
            n = a[0].multiply(b[1]).subtract(b[0].multiply(a[1]));
            d = a[1].multiply(b[1]);
            break;
          case MUL :
            n = a[0].multiply(b[0]);
            d = a[1].multiply(b[1]);
            break;
          case DIV :
            if(b[0].signum() == -1) {
              b[0] = b[0].negate();
              b[1] = b[1].negate();
            }
            n = a[0].multiply(b[1]);
            d = a[1].multiply(b[0]);
            break;
          case EXP :
            if(b[1].equals(ONE.L)) {
              int e = b[0].intValue();
              n = a[0].pow(e);
              d = a[1].pow(e);
            }
            else throw new ClassCastException();
            break;
          default :
            break;
        }
        // simplifier
        BigInteger pgcd = n.gcd(d);
        n = n.divide(pgcd);
        l = new Numeric(n.abs());
        r = new Numeric(d.divide(pgcd));
        ret = (r.equals(ONE))? l : build(DIV, l, r);
        if(n.signum() == -1) ret = build(SUB, ZERO, ret);
      } catch(ClassCastException CCE) {ret = build(ret.op, l, r);}   
    }
    return ret;
  } 

  /**
   * Si c'est une fraction, retourne les éléments
   * @throws ClassCastException si ce n'est pas l'une des formes a/b ou -a/b
   * @return numérateur (éventuellement négatif) et dénominateur
   */
  private BigInteger[] toBigIntegers() throws ClassCastException {
    BigInteger n, d;
    boolean neg = (op == SUB && ZERO.equals(L));
    Numeric e = (neg)? (Numeric)R : this;
    if(e.op == INT) {
      n = (BigInteger)e.L;
      d = (BigInteger)ONE.L;
    }
    else if(e.op == DIV) {
      n = (BigInteger)((Expression)e.L).L;
      d = (BigInteger)((Expression)e.R).L;
    }
    else throw new ClassCastException();
    if(neg) n = n.negate();
    return new BigInteger[] {n,d};
  }
  
  /**
   * retoure une liste d'entiers à partir d'une liste de Numerics
   * @param nums la liste de Numerics
   * @return la liste des entiers
   */
  public static int[] getInts(Expression[] nums) {
    int n = nums.length;
    int[] ret = new int[n];
    for(int i = 0; i < ret.length; i++) {
      if(nums[i].op == INT) ret[i] = ((BigInteger)nums[i].L).intValue();
    }
    return ret;
  }
  
  /** Approximation décimale d'une expression numérique
     * @return 
   */
  @Override
  public double toDouble() throws ArithmeticException {
    double d = Double.NaN, dl, dr;
    switch (op) {
      case INT :
        d = ((BigInteger)L).doubleValue();
        break;
      case DEC :
        d = ((BigDecimal)L).doubleValue();
        break;
      case SUM :
        d = ((Numeric)L).toDouble() + ((Numeric)R).toDouble();
        break;
      case SUB :
        d = ((Numeric)L).toDouble() - ((Numeric)R).toDouble();
        break;
      case MUL :
        d = ((Numeric)L).toDouble() * ((Numeric)R).toDouble();
        break;
      case DIV :
        d = ((Numeric)L).toDouble() / ((Numeric)R).toDouble();
        break;
      case EXP :
        dl = ((Numeric)L).toDouble();
        dr = ((Numeric)R).toDouble();
        if(dl > 0) d = Math.exp(dr*Math.log(dl));
        else if(Math.floor(dr) == dr) { // exposant entier
          if(dl == 0 && dr >= 0) d = 0;
          else if(dl != 0) {
            d = Math.exp(dr*Math.log(Math.abs(dl)));
            if(((int)dr)%2 == 1) d = -d;
          }
        }
        break;
    }
    return d;
  }

  /**
   * @return l'entier le plus proche
   */
  public int toInt() {
    return (int)Math.round(toDouble());
  }
  
  /** Renvoie la forme de la division euclidienne de l'entier a par l'entier b
     * @param b
     * @return 
   */
  public Expression euclide(Expression b) {
    Expression ret = this;
    if(op == INT && b.op == INT) {     
      BigInteger B = (BigInteger)b.L;
      BigInteger[] dr = ((BigInteger)L).divideAndRemainder(B);
      ret = build(MUL, b, new Numeric(dr[0]));
      ret = build(SUM, ret, new Numeric(dr[1]));
    }
    return ret;
  }
      
  @Override
  public String toString() {
    return super.toString().replace('(', '[').replace(')', ']');
  }  

}
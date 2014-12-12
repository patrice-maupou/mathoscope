/*
 * Parser.java
 *
 * Created on 19 octobre 2002, 09:51
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html 
 */

package expressions;


import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.regex.Pattern;
import static expressions.Expression.*;

/**
 * @author Patrice Maupou
 */
public class Parser {
    
  
  /** Creates new Parser */
  public Parser() {}
    
  
  /**
   * renvoie une VBOX avec les lignes de texte mathématique
   * @param s le texte
   * @throws java.lang.Exception 
   * @return VLBOX
   */
  public static Expression parseText(String s) throws Exception {
    Pattern p = Pattern.compile("\n");
    String[] items = p.split(s);
    Expression[] lines = new Expression[items.length];
    for (int i = 0; i < items.length; i++) {
      lines[i] = parseLine(items[i]);
    }
    if(items.length == 1) return lines[0];  
    return build(FUNC, "VLBOX", lines);
  }
  
  
  /**
   * renvoie une HBOX correspondant à une ligne de texte mathématique
   * @param s le texte
   * @throws java.lang.Exception 
   * @return expression HBOX(..)
   */
  private static Expression parseLine(String s) throws Exception {
    Pattern p = Pattern.compile("\\$");
    String[] items = p.split(s);
    boolean math = s.startsWith("\\$");
    Expression[] hboxes = new Expression[items.length];
    for (int i = 0; i < items.length; i++) {
      if(math) {hboxes[i] = parse(items[i]);}
      else hboxes[i] = var(items[i]);
      math = !math;
    }
    if(items.length == 1) return hboxes[0];
    return build(FUNC, "HBOX", hboxes);
  }

  
  /**
   * Interprète une chaîne en expression mathématique
   * 
   * @return l'expression sous forme d'arbre binaire, un arbre vide si l'entrée est
   * incorrecte.
   * @param s la chaîne de caractère entrée
   * @throws java.lang.Exception 
   */
  public static Expression parse(String s) throws Exception { 
    s = replace(s, "€"," in ");
    s = replace(s, "->", "#");
    s = replace(s, "*", "×");
    s = replace(s, "<=", " \u2264 ");
    s = replace(s, ">=", " \u2265 ");
    StreamTokenizer sk = new StreamTokenizer(new StringReader(s));
    sk.resetSyntax();
    sk.eolIsSignificant(false);
    sk.wordChars('a', 'z');
    sk.wordChars('A', 'Z');
    sk.whitespaceChars(0, ' ');
    sk.commentChar(';');
    sk.quoteChar('"');
    sk.wordChars('?','?');
    sk.wordChars('$','$');
    sk.wordChars('_','_');
    sk.wordChars('à','ö');
    sk.wordChars('¶','¶');
    sk.quoteChar('`');
    sk.parseNumbers();
    sk.ordinaryChars('-','-');
    vars = new ArrayList<Expression>();
    failSer = false;
    Expression r = build(SEQ, "()", new Expression[0]);
    r = parseMulti(r, 0, sk);
    sk.pushBack();
    if (sk.nextToken() != StreamTokenizer.TT_EOF)
      throw new Exception("Expression mal formée");
    return r;
  }
  
  /**
   * la sous-expression sélectionnée est remplacée par la variable dummy dans
   *  l'expression analysée
   * @param s 
   * @param subStart 
   * @param subEnd 
   * @throws java.lang.Exception 
   * @return 
   */
  public static Expression[] parse(String s, int subStart, int subEnd) throws Exception {
    Expression exprorg, expr, subExpr, newExpr, dum;
    exprorg = parse(s);
    subExpr = parse(s.substring(subStart, subEnd));
    dum = build(VAR, "dummy",null);
    s = s.substring(0, subStart) + "dummy" + s.substring(subEnd);
    expr = parse(s);
    newExpr = expr.substitute(dum, subExpr);
    if (!exprorg.equals(newExpr))
      throw new Exception("ce n'est pas une sous-expression");
    return new Expression[] { expr, subExpr };
  }
  
  /**
   * remplace une chaîne de charactères par une autre dans une chaîne donnée
   * @param in 
   * @param f la chaîne à remplacer
   * @param r la chaîne de remplacement
   * @return la chaîne transformée
   */
  public static String replace(String in, String f, String r) {
    String out = in;
    boolean ok = true;
    int m = r.length(), n = f.length(), k = 0, i, j;
    while ((i = out.indexOf(f, k)) != -1) {
      j = out.indexOf("`", k);
      if(j == -1) j = out.length();
      if(ok && i + n - 1 < j) {
        try {
          out = out.substring(0, i) + r + out.substring(i + n);
          k = i + m;
        } catch(IndexOutOfBoundsException exc) {
          System.out.println(exc.toString()+" "+out.substring(0,i));
        }
      }
      else {
        k = j + 1;
        ok = !ok;
      }
    }
    return out;
  }
  
  /**
   * renvoie une expression à  partir d'une suite de tokens
   * @param op
   * @param rbrackets
   * @throws Exception si erreur de syntaxe
   * @return l'expression
   */
  private static Expression parseExpr(int op, int rbrackets, StreamTokenizer sk) throws Exception {
    Expression r = new Expression(), right, mold;
    int lastOp = op, type = '(';
    while (type != StreamTokenizer.TT_EOF) {
      type = sk.nextToken();
      if(type == '"') type = StreamTokenizer.TT_WORD;
      if(type == StreamTokenizer.TT_WORD) {
        if(sk.sval.equals("et")) type = '&';
        else if(sk.sval.equals("ou")) type = '¦';
        else if(sk.sval.equals("non")) type = '¬';
        else if(sk.sval.equals("donc")) type = '»';
        else if(sk.sval.equals("in")) type = 129;
        else if(sk.sval.equals("union")) type = 'Û';
        else if(sk.sval.equals("inter")) type = 'Î';
        else if(sk.sval.equals("\u2264")) type = '\u2264';
        else if(sk.sval.equals("\u2265")) type = '\u2265';
      }
      else if((type=='['||type==']'||type==')'||type=='}') && matchBracket(rbrackets, type)) {
        break;
      }
      else if (type == '|' && rbrackets == '|') {
        if(r.op == NUL) { // début de la valeur absolue
          r = subExpression('|', sk);
          type = sk.nextToken();
          if(type == '|') break;
        }
        else break;
      }
      else if(type == ',' || type == ':') {
        sk.pushBack();
        break;
      }
      else if (type == StreamTokenizer.TT_EOF) {
        if (rbrackets != 0) throw new Exception("Erreur de parenthèses");
        break;
      }
      if (r.op == NUL) { // rien avant, on cherche le membre à  gauche
        if (type == '-' && op <= GT) { // négation d'une expression
          right = parseExpr(SUB, 0, sk);
          r = build(SUB, ZERO, right);
        }
        else if(type == '¬') { //négation d'une relation
          right = parseExpr(NOT, 0, sk);
          r = build(NOT, right, right);
        }
        else { // première expression
          r = subExpression(type, sk);
        }
        if (r.op == NUL) throw new Exception("Expression mal formée");
      }
      else {
        if(type == '\'' && r.op == VAR)  { // ajoute un accent à la variable
          r.L = (String)r.L + "'";
          continue;
        }
        //* modif pour autoriser les phrases
        else if(type == StreamTokenizer.TT_WORD && r.op == VAR) {
          r.L = (String)r.L + " " + sk.sval;
          continue;
        }
        //*/
        op = parseOp(type);  // on cherche l'opération
        if(op == FUNC || op == SER && r.op == VAR) { // f(..) ou a[..]
          mold = build(op, r.L, new Expression[0]);
          try {
            r = parseMulti(mold, sk.ttype, sk);
          } catch(Exception e) { // intervalle
            if(sk.ttype == StreamTokenizer.TT_EOF) sk.ttype = '[';
            sk.pushBack();
            failSer = true;
            break;
          }
        }
        else {
          if(op - lastOp > 1) {// dernière opération prioritaire: op="*" lastOp="+"
            right = parseExpr(op, 0, sk); 
            r = build(op, r, right); // a+b*c -> a+(b*c)
          }
          else  { // a*b +c
            sk.pushBack(); // renvoie a*b comme sous-expression
            break;
          }
        }
      }
    }
    if (r.op == NUL) throw new Exception("Expression mal formée");
    return r;
  }
  
  /** parse une sous-expression, second opérande ou ensemble ou valeur absolue
   */
  private static Expression subExpression(int type, StreamTokenizer sk) throws Exception {
    Expression r = new Expression(), mold;
    if (type == StreamTokenizer.TT_NUMBER) {
      int op = DEC;
      Double d = new Double(sk.nval);
      Object L = new BigDecimal(d.toString());
      if(Math.floor(sk.nval) == sk.nval) {
        L = ((BigDecimal)L).toBigInteger();
        op = INT;
      }
      r = build(op, L, null);
    }
    else if(type == '`') {
      r = parseText(sk.sval);
      if(r.op == VAR) r.R = var("constant");
    }   
    else if (type == StreamTokenizer.TT_WORD) {
      r = build(VAR, sk.sval, null);
      int idx = vars.indexOf(r);  // la variable désigne une expression ?
      if(idx != -1) r = vars.get(idx);
    }
    else if(type == 'Ø') {
      r = build(SET, "{}", new Expression[] {});
    }
    else if (type == '|') {
      Expression[] coors = new Expression[] {parseExpr(NUL, '|', sk)};
      r = build(FUNC, "||", coors);
    }
    else if (type == '(') {
      mold = build(SEQ, "()", new Expression[0]);
      r = parseMulti(mold, '(', sk);
    } 
    else if(type == '[' || type == ']') {
      String s = String.valueOf((char)type)+ "[]";
      mold = build(SET, s, new Expression[0]);
      r = parseMulti(mold, type, sk);
    } 
    else if (type == '{') {
      mold = build(SET, "{}", new Expression[0]);
      r = parseMulti(mold, '{', sk);
    }
    return r;
  }
  
  /**
   * retourne une suite d'expressions séparées par une virgule
   *  si x->entier, ajoute la variable x et son type à vars
   * ou (FUNC, name, Ex[0]), (SER, name, Ex[0]), (SET,"{}",Ex[0]),
   * @param mold le "moule" de l'expression à renvoyer,
   *    (SET,"[[",Ex[0]), (FUNC, name, Ex[0]), (SER, name, Ex[0])
   * @param lbracket le type de parenthèses "(", "[", "{"
   * @return l'expression complète
   */
  private static Expression parseMulti(Expression mold, int lbracket, StreamTokenizer sk) throws Exception {
    Expression cur, var;
    Expression[] coors = new Expression[0], temp;
    String pars = (String)mold.L;
    int type, oldsize = vars.size(), k;
    do {
      cur = parseExpr(NUL, lbracket, sk);
      if(cur.op == IS && ((Expression)cur.L).op == VAR) { // a->[1,2]
        var = (Expression)cur.L;
        vars.add(build(VAR, var.L, cur.R));
      }
      temp = new Expression[coors.length + 1];
      System.arraycopy(coors, 0, temp, 0, coors.length);
      temp[coors.length] = cur.copy();
      coors = temp;
      if(failSer) {
        type = '[';
        failSer = false;
      } 
      else {
        sk.pushBack();
        type = sk.nextToken();
      }
      if(type == ':' && lbracket == '{') {
        mold.L = parseExpr(NUL, lbracket, sk);
        sk.pushBack();
        type = sk.nextToken();
        break;
      }
    } while (type == ',');
    vars.subList(oldsize, vars.size()).clear();
    if(mold.op == FUNC && type == ')')      
      mold.R = coors;
    else if(mold.op == SER && type == ']')
      mold.R = coors;
    else if(mold.L instanceof Expression  && type == '}')
      mold.R = coors;
    else if(lbracket == 0 && type == StreamTokenizer.TT_EOF)
      mold.R = coors;
    else if((k = pars.indexOf((char)type,1)) != -1) {
      mold.L = pars.substring(0,1) + pars.substring(k,k+1);
      if(mold.op == SET && (type == ']' || type == '[')) {
        if (coors.length != 2) throw new Exception("intervalle incorrect");
      }
      mold.R = coors;
    } 
    else {throw new Exception("paire incorrecte sur "+ build(SET, "{}", coors).toString());}
    if(mold.op == SEQ) {
      if(coors.length == 1) mold = coors[0];
      else mold.L = "()";
    }
    return mold;
  }
 
  /** vérifie le couplage des parenthèses
   * @param first
   * @param last
   * @return vrai si les parenthèses sont compatibles
   */
  private static boolean matchBracket(int first, int last) {
    boolean ret = false;
    if(first == '(' && last == ')') ret = true;
    else if(first == '{' && last == '}') ret = true;
    else if(first == '|' && last == '|') ret = true;
    else if((first == '[' || first == ']') && (last == '[' || last == ']'))
      ret = true;
    else if (first == '{' && last == '}')
      ret = true;
    else if (first == '|' && last == '|')
      ret = true;
    else if (first == 0 && last == StreamTokenizer.TT_EOF) ret = true; // 8,2
    return ret;
  }
  
  /**
   * retourne l'entier op correspondant au caractère type
   * @param type 
   * @return 
   */
  public static int parseOp(int type) {
    int op = NUL;
    if (type == '=') op = EQU;
    else if (type == '<') op = LT;
    else if (type == '>') op = GT;
    else if (type == '\u2264') op = LE;
    else if (type == '\u2265') op = GE;
    else if (type == '+') op = SUM;
    else if (type == '*') op = MUL;
    else if (type == '×') op = MUL;
    else if (type == '-') op = SUB;
    else if (type == '/') op = DIV;
    else if (type == '^') op = EXP;
    else if (type == '¦') op = OR;
    else if (type == '&') op = AND;
    else if (type == '»') op = IMP;
    else if (type == '¬') op = NOT;
    else if (type == '(') op = FUNC;
    else if (type == '[') op = SER;
    else if (type == '{' || type == 'Ø') op = SET;
    else if (type == 129) op = IN;
    else if (type == '#') op = IS;
    else if (type == 'Û') op = CUP;
    else if (type == 'Î') op = CAP;
    return op;
  }
  
  /**
   * utilitaire de débug
   * @param op
   * @return
   */
  public static String op(int op) {
    String r = "?";
    if(op == VAR) r = "variable";
    else if (op == INT) r = "entier";
    else if (op == DEC) r = "décimal";
    else if (op == FUNC) r = "fonction";
    else if (op == SER) r = "suite";
    else if (op == SET) r = "ensemble";
    else if (op == EQU) r = "=";
    else if (op == LT) r = "<";
    else if (op == GT) r = ">";
    else if (op == LE) r = "<=";
    else if (op == GE) r = ">=";
    else if (op == SUM) r = "+";
    else if (op == MUL) r = "×";
    else if (op == SUB) r = "-";
    else if (op == DIV) r = "/";
    else if (op == EXP) r = "^";
    else if (op == OR) r = "ou";
    else if (op == AND) r = "et";
    else if (op == IMP) r = "donc";
    else if (op == NOT) r = "non";
    else if (op == IN) r = "dans";
    else if (op == IS) r = "->";
    else if (op == CUP) r = "union";
    else if (op == CAP) r = "inter";
    return r;
  }
  
  /**
   * utilisé en débug
   * @param type
   * @return
   */
  private static String type(int type) {
    String r = "?";
    if (type == StreamTokenizer.TT_NUMBER) r = "nombre";
    else if (type == StreamTokenizer.TT_WORD) r = "variable";
    else if (type == '=') r = "=";
    else if (type == '<') r = "<";
    else if (type == '>') r = ">";
    else if (type == '\u2264') r = "<=";
    else if (type == '\u2265') r = ">=";
    else if (type == '+') r = "+";
    else if (type == '×') r = "×";
    else if (type == '-') r = "-";
    else if (type == '/') r = "/";
    else if (type == '^') r = "^";
    else if (type == '¬') r = "¬";
    else if (type == '(') r = "(";
    else if (type == ')') r = ")";
    else if (type == '{') r = "{";
    else if (type == '}') r = "}";
    else if (type == '[') r = "[";
    else if (type == ']') r = "]"; 
    else if (type == '¦') r = "ou";
    else if (type == '&') r = "et";
    else if (type == '»') r = "donc";
    else if (type == '¬') r = "non";
    else if (type == ',') r = ",";
    else if (type == ':') r = "tel que";
    else if (type == '#') r = "->";
    else if(type == 'Û') r = "union";
    else if(type == 'Î') r = "inter";
    else if (type == StreamTokenizer.TT_EOF) r = "EOF";
    return r;
  }

  private static ArrayList<Expression> vars;
  private static boolean failSer;
  
}
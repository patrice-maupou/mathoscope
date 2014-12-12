/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package expressions;

import java.math.BigInteger;
import java.util.ArrayList;
import junit.framework.TestCase;
import static expressions.Expression.*;

/**
 *
 * @author Patrice Maupou
 */
public class ExpressionTest extends TestCase {
    
   Expression MATHDEFS,  INVDISPLAYS, DISPLAYS;
  
    public ExpressionTest(String testName) {
        super(testName);
    }            

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

  /**
   * Test of build method, of class Expression.
   */
  public void testBuild() {
    System.out.println("build");
    int[] ops = new int[]{VAR, INT};
    Object[] e0 = new Object[]{"a", new BigInteger("3")};
    Object[] e1 = new Object[]{null, null};
    /*
    Expression expResult = null;
    Expression result = Expression.build(op, e0, e1);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
    //*/
  }

  /**
   * Test of var method, of class Expression.
   */
  public void testVar() {
    System.out.println("var");
    String name = "a";
    Expression result = Expression.var(name);
    assertEquals(name, result.L);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of copy method, of class Expression.
   */
  public void testCopy() throws Exception {
    System.out.println("copy");
    String name = "schéma({A union B->image:A union B})";
    Expression instance = Parser.parse(name);
    Expression result = instance.copy();
    System.out.println(result.toString());
    assertEquals(instance, result);
  }

  /**
   * Test of markFuncVars method, of class Expression.
   */
  public void testMarkFuncVars() throws Exception {
    System.out.println("markFuncVars");
    ArrayList<Expression> defs = new ArrayList<Expression>();
    int start = 0;
    boolean func = false;
    String[] s = new String[]{
      "C_AV(p,x)=applique({?f(a,b)->image,?f€{PLUS,MUL,MOINS,DIV} : ?f(C_AV(a,x),C_AV(b,x))})",
      "f(x)=2+x"
    };
    String[] r = new String[]{
      "C_AV(p$0,x$0)=applique({?f(a,b)->image,?f€{PLUS,MUL,MOINS,DIV} : ?f(C_AV(a,x$0),C_AV(b,x$0))})" ,
        "f(x$0)=2+x$0"
    };
    for (int i = 0; i < s.length; i++) {
      Expression instance = Parser.parse(s[i]);
      Expression v = instance;
      Expression expResult = Parser.parse(r[i]);
      Expression result = instance.markFuncVars(v, defs, start, func);
      System.out.println(result.printout(false));
      assertEquals(expResult, result);
    }

  }
  
  public void testChangeFuncNames() throws Exception {
    System.out.println("changeFuncNames");
    ArrayList<Expression> defs = new ArrayList<Expression>();
    int start = 0;
    String[] sdefs = new String[]{"?f","g"};
    String s = "{?f}";
    for (int i = 0; i < sdefs.length; i = i + 2) {
      Expression e = Parser.parse(sdefs[i]);
      e.R = Parser.parse(sdefs[i+1]);
      defs.add(e);
    }
    
  }

  /**
   * Test of substitute method, of class Expression.
   */
  public void testSubstitute() {
    System.out.println("substitute");
    Expression v = null;
    Expression e = null;
    Expression instance = new Expression();
    Expression expResult = null;
    Expression result = instance.substitute(v, e);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of equals method, of class Expression.
   */
  public void testEquals() {
    System.out.println("equals");
    Object obj = null;
    Expression instance = new Expression();
    boolean expResult = false;
    boolean result = instance.equals(obj);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of hashCode method, of class Expression.
   */
  public void testHashCode() {
    System.out.println("hashCode");
    Expression instance = new Expression();
    int expResult = 0;
    int result = instance.hashCode();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of compareTo method, of class Expression.
   */
  public void testCompareTo() {
    System.out.println("compareTo");
    Object obj = null;
    Expression instance = new Expression();
    int expResult = 0;
    int result = instance.compareTo(obj);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getType method, of class Expression.
   */
  public void testGetType() {
    System.out.println("getType");
    Object o = null;
    Object expResult = null;
    Object result = Expression.getType(o);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of isType method, of class Expression.
   */
  public void testIsType() {
    System.out.println("isType");
    Expression type = null;
    boolean mark = false;
    Expression instance = new Expression();
    Relation expResult = null;
    Relation result = instance.isType(type, mark);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }


  /**
   * Test of define method, of class Expression.
   */
  public void testDefine() {
    System.out.println("define");
    ArrayList<Expression> Vars = null;
    boolean recursive = false;
    Expression instance = new Expression();
    int[] expResult = null;
    int[] result = instance.define(Vars, recursive);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of applyDefs method, of class Expression.
   * @throws Exception 
   */
  public void testApplyDefs() throws Exception {
    System.out.println("applyDefs");
    int start = 0;
    int level = 50;
    Expression instance = new Expression();
    ArrayList<Expression> defs = new ArrayList<Expression>();
    String[] stExprs = new String[] {
      "c_av(x*2,x)", "stylefonc(2-x*2)", "C_AV(MOINS(2,MUL(x,2)),x)",
      "c_av(2-x*2,x)", "c_av(7*(2-x*2),x)",
      "ASSOC(PLUS(1,PLUS(2,3)))", "ASSOC(PLUS(PLUS(1,4),PLUS(2,3)))",
      "assoc(1+(2+3))",
      "applique(f,(1,2))", "applique(couleur,rouge)", "applique(calcule,3)"
    };
    String[] stResults = new String[] {
      "2*x","MOINS(2,MUL(x,2))", "MOINS(2,MUL(2,x))",
      "2-2*x","7*(2-2*x)",
      "PLUS(PLUS(1,2),3)", "PLUS(PLUS(PLUS(1,4),2),3)",
      "1+2+3",
      "f(1,2)", "couleur(255,0,0)", "3"
    };
    String[] stdefs = new String[] { //TODO : OPS ne fonctionne pas
       "rouge->(255,0,0)",
       "C_AV->C_AV(p,x)=applique(p,{MUL(a,b)->image,dans(x,a),non(dans(x,b)) : STOP(MUL(b,C_AV(a,x)))}," +
         "{?f(a,b)->image : ?f(C_AV(a,x),C_AV(b,x))})",
       "c_av->c_av(p,x)=applique(styleop(C_AV(stylefonc(p),x)))",
       "ASSOC->ASSOC(p)=applique(p,{?f(a,?f(b,c))->image,?f€{PLUS,MUL} : ?f(ASSOC(?f(a,b)),ASSOC(c))}," +
         "{?f(?f(a,b),?f(c,d))->image,?f€{PLUS,MUL} : ?f(?f(ASSOC(?f(a,b)),ASSOC(c)),ASSOC(d))})",
        "assoc->assoc(p)=applique(styleop(ASSOC(stylefonc(p))))"
      };
    for (int i = 0; i < stdefs.length; i++) {
      Expression e0 = Parser.parse(stdefs[i]);
      Expression e = (Expression)e0.L; // la variable
      e.R = e0.R;
      defs.add(e);
    }
    for (int i = 0; i < stExprs.length; i++) {
      instance = Parser.parse(stExprs[i]);
      Expression expResult = Parser.parse(stResults[i]);
      Expression result = instance.applyDefs(defs, start, level, null);
      assertEquals(expResult, result);
      System.out.println(result.printout(false));
    }
  }

  /**
   * Test of applyFunc method, of class Expression.
   */
  public void testApplyFunc() throws Exception {
    System.out.println("applyFunc");
    Expression func = null;
    ArrayList<Expression> defs = null;
    int start = 0;
    boolean schema = false;
    Expression instance = new Expression();
    Expression expResult = null;
    Expression result = instance.applyFunc(func, defs, start, schema,null);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toDouble method, of class Expression.
   */
  public void testToDouble() {
    System.out.println("toDouble");
    Expression instance = new Expression();
    double expResult = 0.0;
    double result = instance.toDouble();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toString method, of class Expression.
   */
  public void testToString() {
    System.out.println("toString");
    Expression instance = new Expression();
    String expResult = "";
    String result = instance.toString();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of printout method, of class Expression.
   */
  public void testPrintout() {
    System.out.println("printout");
    boolean brackets = false;
    Expression instance = new Expression();
    String expResult = "";
    String result = instance.printout(brackets);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of opString method, of class Expression.
   */
  public void testOpString() {
    System.out.println("opString");
    Expression instance = new Expression();
    String expResult = "";
    String result = instance.opString();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of op method, of class Expression.
   */
  public void testOp() {
    System.out.println("op");
    Expression instance = new Expression();
    String expResult = "";
    String result = instance.op();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of lastExpression method, of class Expression.
   */
  public void testLastExpression() {
    System.out.println("lastExpression");
    boolean dir = false;
    Expression instance = new Expression();
    Expression expResult = null;
    Expression result = instance.lastExpression(dir);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

}

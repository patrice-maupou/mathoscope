/*
 * RelationTest.java
 * JUnit based test
 *
 * Created on 19 août 2006, 15:49
 */

package expressions;

import junit.framework.*;
import java.util.*;
import static expressions.Expression.*;

/**
 *
 * @author Patrice Maupou
 */
public class RelationTest extends TestCase {
  
  public RelationTest(String testName) {
    super(testName);
  }

  @Override
protected void setUp() throws Exception {
  }

  @Override
protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(RelationTest.class);
    
    return suite;
  }
  
  
  public void testAddVars() {
    System.out.println("addVars");
    ArrayList<String> items = new ArrayList<String>();
    
    items.add("x+y->3+4");
    items.add("?f(a,b)->g(2,3)");
    boolean[] results = new boolean[]{true, true};
    ArrayList<Expression> vars = new ArrayList<Expression>();
    try {
      for (int i = 0; i < results.length; i++) {
        Relation instance = (Relation) Parser.parse(items.get(i));
        boolean result = instance.addVars(vars, 0,1);
        assertEquals(results[i], result);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void testMatch() {
    System.out.println("match");
    ArrayList<String> items = new ArrayList<String>(), schemas = new ArrayList<String>();
    ArrayList<Expression> defs = new ArrayList<Expression>();
    int start = 0;
    
    items.add("3×4");
    schemas.add("_a×x");
    items.add("{point(1,2),label(a,0,12)}");
    schemas.add("{point(_x,_y),label(_e,_p,_t)}");
    items.add("3-4");
    schemas.add("_a+_b");
    items.add("Ø");
    schemas.add("_a/_b");
    items.add("3+3");
    schemas.add("_a+_a");
    items.add("3+4");
    schemas.add("_a+_a");
    items.add("{a}");
    schemas.add("]_a,_b]");
    items.add("-5");
    schemas.add("-_b");
    items.add("4+(-5)");
    schemas.add("4+(-_b)");
    items.add("couleur(c)=applique(couleur,c)");
    schemas.add("?f(_x)=applique(?f,_x)");
    
    
    boolean[] results = new boolean[]{false, true, false, false, true, false, false, true, true, true};
    try {
      for (int i = 0; i < results.length; i++) {
        ArrayList<Expression> vars = new ArrayList<Expression>(), exprs = new ArrayList<Expression>();
        Expression instance = Parser.parse(items.get(i));
        Expression p = Parser.parse(schemas.get(i));
        boolean result = Relation.match(instance, p, vars, exprs);
        System.out.println(instance.printout(false) + "  " + p.printout(false) + "  " + result);
        assertEquals(results[i], result);
      }
      Expression instance = build(JUX, var("a"), var("b"));
      instance = build(FUNC, "f", new Expression[]{var("c"), instance});
      Expression p = Parser.parse("f(_a,_b)");
      boolean result = Relation.match(instance, p, new ArrayList<Expression>(), new ArrayList<Expression>());
      System.out.println(instance.printout(false) + "  " + p.printout(false) + "  " + result);
      assertEquals(true, result);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
  }
  
  /**
   * Test of build method, of class expressions.Relation.
   */
  public void testBuild() {
    System.out.println("build");
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of substitute method, of class expressions.Relation.
   */
  public void testSubstitute() {
    System.out.println("substitute");
    
    Expression v = null;
    Expression e = null;
    Relation instance = new Relation();
    
    Expression expResult = null;
    Expression result = instance.substitute(v, e);
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of valueOf method, of class expressions.Relation.
   */
  public void testValueOf() throws Exception {
    System.out.println("valueOf");
    
    ArrayList<Expression> defs = new ArrayList<Expression>();
    int start = 0;    
    String[] textItems = {"x+y->2+3","3€[2,5]","3€[5,8]"};    
    boolean[] results = new boolean[]{true, true, false};
    try {
      for (int i = 0; i < textItems.length; i++) {
        Relation item = (Relation) Parser.parse(textItems[i]);
        boolean result = item.valueOf(defs, start,1);
        assertEquals(results[i], result);  
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Test of printout method, of class expressions.Relation.
   */
  public void testPrintout() {
    System.out.println("printout");
    
    boolean brackets = true;
    Relation instance = new Relation();
    
    String expResult = "";
    String result = instance.printout(brackets);
  }

  /**
   * Test of toString method, of class expressions.Relation.
   */
  public void testToString() {
    System.out.println("toString");
    
    Relation instance = new Relation();
    
    String expResult = "";
    String result = instance.toString();
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }



  /**
   * Test of schema method, of class Relation.
   * @throws Exception 
   */
  public void testSchema() throws Exception {
    System.out.println("schema");
    String pre = "_";
    ArrayList<Expression> vars = new ArrayList<Expression>();
    int vars_start = 0;
    String[] s = new String[]{"(r,g,b)", "?f(a,b)","?f(x)=applique(?f,2)"};
    String[] r = new String[]{"(_r,_g,_b)", "?f(_a,_b)","?f(_x)=applique(?f,2)"};
    for (int i = 0; i < s.length; i++) {
      Expression p = Parser.parse(s[i]);
      Expression expResult = Parser.parse(r[i]);
      Expression result = Relation.schema(p, pre, vars, vars_start);
      assertEquals(expResult, result);
    }
  }

  
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package display;

import expressions.*;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import root.GeomPanel;
import static expressions.Expression.*;

/**
 *
 * @author Patrice Maupou
 */
public class SystemBoxTest extends TestCase {
    
    public SystemBoxTest(String testName) {
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
   * Test of codeDisplays method, of class SystemBox.
   */
  public void testCodeDisplays() {
    System.out.println("codeDisplays");
    Expression e = NULL;
    Expression displays = null;
    try {
      e = Parser.parse("angle(M,N,P)");
      displays = Parser.parse("schema({angle(A,B,C)->image:HBOX(A,B,C)})");
    } catch (Exception ex) {
      Logger.getLogger(SystemBoxTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    Expression result = SystemBox.codeDisplays(e, displays);
    Expression[] results = (Expression[])result.R;
    Expression[] args = (Expression[])e.R;
    for (int i = 0; i < 3; i++) {
      assertSame(args[i], results[i]);
    }
  }

  /**
   * Test of copyref method, of class SystemBox.
   */
  public void testCopyref() {
    System.out.println("copyref");
    Expression a = var("A"), b = var("B"), c = var("C");
    a.R = var("M");
    b.R = var("N");
    c.R = var("P");
    ArrayList<Expression> vars = new ArrayList<Expression>();
    vars.add(a);
    vars.add(b);
    vars.add(c);
    Expression e = NULL;
    try {
      e = Parser.parse("angle(A,B,C)");
    } catch (Exception ex) {
      Logger.getLogger(SystemBoxTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    Expression result = SystemBox.copyref(e, vars);
    Expression[] args = (Expression[])result.R;
    assertSame(args[0], a.R);
    assertSame(args[1], b.R);
    assertSame(args[2], c.R);
  }

  /**
   * Test of exprBox method, of class SystemBox.
   */
  public void testExprBox() {
    System.out.println("exprBox");
    Expression expr = null;
    Expression displays = null;
    GeomPanel GPanel = null;
    int size = 0;
    boolean brackets = false;
    boolean hot = false;
    SystemBox expResult = null;
    SystemBox result = SystemBox.exprBox(expr, displays, GPanel, size, brackets, hot);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of binaryBox method, of class SystemBox.
   */
  public void testBinaryBox() {
    System.out.println("binaryBox");
    Expression e = null;
    Expression displays = null;
    GeomPanel GPanel = null;
    int size = 0;
    boolean brackets = false;
    boolean hot = false;
    SystemBox expResult = null;
    SystemBox result = SystemBox.binaryBox(e, displays, GPanel, size, brackets, hot);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of add method, of class SystemBox.
   */
  public void testAdd_4args() {
    System.out.println("add");
    int a = 0;
    GPoint Pa = null;
    GPoint Pb = null;
    Box box = null;
    SystemBox instance = new SystemBox();
    instance.add(a, Pa, Pb, box);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of add method, of class SystemBox.
   */
  public void testAdd_Box_String() {
    System.out.println("add");
    Box box = null;
    String dir = "";
    SystemBox instance = new SystemBox();
    instance.add(box, dir);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of paint method, of class SystemBox.
   */
  public void testPaint() {
    System.out.println("paint");
    Graphics g = null;
    BoxMetrics size = null;
    int x = 0;
    int y = 0;
    SystemBox instance = new SystemBox();
    instance.paint(g, size, x, y);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getActive method, of class SystemBox.
   */
  public void testGetActive() {
    System.out.println("getActive");
    SystemBox instance = new SystemBox();
    int expResult = 0;
    int result = instance.getActive();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setActive method, of class SystemBox.
   */
  public void testSetActive() {
    System.out.println("setActive");
    int active = 0;
    SystemBox instance = new SystemBox();
    instance.setActive(active);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of negActive method, of class SystemBox.
   */
  public void testNegActive() {
    System.out.println("negActive");
    SystemBox instance = new SystemBox();
    instance.negActive();
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getActiveBox method, of class SystemBox.
   */
  public void testGetActiveBox() {
    System.out.println("getActiveBox");
    SystemBox instance = new SystemBox();
    SystemBox expResult = null;
    SystemBox result = instance.getActiveBox();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setActiveBox method, of class SystemBox.
   */
  public void testSetActiveBox() {
    System.out.println("setActiveBox");
    SystemBox activeBox = null;
    int active = 0;
    SystemBox instance = new SystemBox();
    instance.setActiveBox(activeBox, active);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setFrame method, of class SystemBox.
   */
  public void testSetFrame() {
    System.out.println("setFrame");
    int[] frame = null;
    SystemBox instance = new SystemBox();
    instance.setFrame(frame);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of setExpr method, of class SystemBox.
   */
  public void testSetExpr() {
    System.out.println("setExpr");
    Expression expr = null;
    SystemBox instance = new SystemBox();
    instance.setExpr(expr);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

}

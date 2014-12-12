/*
 * NumericTest.java
 * JUnit based test
 *
 * Created on 31 août 2006, 14:19
 */

package expressions;

import junit.framework.*;
import java.math.*;

/**
 *
 * @author Patrice Maupou
 */
public class NumericTest extends TestCase {
  
  public NumericTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(NumericTest.class);
    
    return suite;
  }

  /**
   * Test of compute method, of class expressions.Numeric.
   */
  public void testCompute() {
    System.out.println("compute");
    
    Numeric instance = null;
    
    Expression expResult = null;
    Expression result = instance.compute();
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getInts method, of class expressions.Numeric.
   */
  public void testGetInts() {
    System.out.println("getInts");
    String s = "(1,2,3,4)";
    try {      
      Expression[] nums = (Expression[])Parser.parse(s).R; 
      int[] expResult = new int[] {1,2,3,4};
      int[] result = Numeric.getInts(nums);
      for (int i = 0; i < expResult.length; i++) {        
        assertEquals(expResult[i], result[i]);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }    
  }

  /**
   * Test of toDouble method, of class expressions.Numeric.
   */
  public void testToDouble() {
    System.out.println("toDouble");
    
    Numeric instance = null;
    
    double expResult = 0.0;
    double result = instance.toDouble();
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toInt method, of class expressions.Numeric.
   */
  public void testToInt() {
    System.out.println("toInt");
    
    Numeric instance = null;
    
    int expResult = 0;
    int result = instance.toInt();
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of euclide method, of class expressions.Numeric.
   */
  public void testEuclide() {
    System.out.println("euclide");
    
    Expression b = null;
    Numeric instance = null;
    
    Expression expResult = null;
    Expression result = instance.euclide(b);
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toString method, of class expressions.Numeric.
   */
  public void testToString() {
    System.out.println("toString");
    
    Numeric instance = null;
    
    String expResult = "";
    String result = instance.toString();
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }
  
}

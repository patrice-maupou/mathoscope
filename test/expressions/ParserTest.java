/*
 * ParserTest.java
 * JUnit based test
 *
 * Created on 26 juin 2006, 16:18
 */

package expressions;

import java.util.regex.Matcher;
import junit.framework.*;
import java.util.regex.Pattern;

/**
 *
 * @author Patrice Maupou
 */
public class ParserTest extends TestCase {
  
  public ParserTest(String testName) {
    super(testName);
  }

  @Override
protected void setUp() throws Exception {
  }

  @Override
protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(ParserTest.class);
    
    return suite;
  }

  /**
   * Test of parseText method, of class expressions.Parser.
   */
  public void testParseText() throws Exception {
    System.out.println("parseText");
    
    String s = "exemple : $x^2-1$ se factorise en $(x-1)*(x+1)$\n autre ligne";
    s = "`Le signe d'un produit (ou d'un quotient)\n dépend du signe de chacun`";
    
    
    Expression expResult = null;
    Expression result = Parser.parseText(s);
    System.out.println(result.printout(false));
    //assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    //fail("The test case is a prototype.");
  }

  /**
   * Test of parse method, of class expressions.Parser.
   */
  public void testParse() throws Exception {
    System.out.println("parse");
    String[] s = new String[10];
    s[0] = "texte->`ceci est un essai avec $2×x$`";
    s[1] = "texte->`Le signe d'un produit ¶ dépend du signe de chaque facteur`";
    s[2] = "3+`x`";
    s[3] = "ab cd";
    s[4] = "`texte commentaire`";    
    s[5] ="{A union B->image:A union B}";
    for (int i = 0; i < 6; i++) {
      Expression result = Parser.parse(s[i]);
      System.out.println(result.printout(false));
      System.out.println(result.toString());
    }
    
  }

  /**
   * Test of replace method, of class expressions.Parser.
   */
  public void testReplace() {
    /*
    System.out.println("replace");
    
    String in = "";
    String f = "";
    String r = "";
    
    String expResult = "";
    String result = Parser.replace(in, f, r);
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
    //*/
  }

  /**
   * Test of parseOp method, of class expressions.Parser.
   */
  public void testParseOp() {
    /*
    System.out.println("parseOp");
    
    int type = 0;
    
    int expResult = 0;
    int result = Parser.parseOp(type);
    assertEquals(expResult, result);
    
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
    //*/
  }
  
  public void testRegex() {
    String c = "\\+";
    Pattern p = Pattern.compile(c);
    Matcher m = p.matcher("(4+2)+3");
    System.out.println(m.group());
  }
  
}

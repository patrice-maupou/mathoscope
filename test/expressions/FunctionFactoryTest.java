/*
 * FunctionFactoryTest.java
 * JUnit based test
 *
 * Created on 19 mai 2007, 21:02
 */

package expressions;

import junit.framework.*;
import static expressions.Expression.*;
import java.util.ArrayList;

/**
 *
 * @author Patrice Maupou
 */
public class FunctionFactoryTest extends TestCase {
  
    FunctionFactory factory;
    String[] exprStrs = {"approche(5/3,0.01)","approche(19,4)","approche(2/3,-4)","truc(2)"};
    String[] resultStrs = {"1.67","20","0.6667","truc(2)"};
    String[] names = new String[exprStrs.length], exprDefsStrings = new String[0];
    Expression[] exprs = new Expression[exprStrs.length], results = new Expression[exprStrs.length];
    ArrayList<Expression> defs = new ArrayList<Expression>();
  
  public FunctionFactoryTest(String testName) throws NoSuchMethodException {
    super(testName);
    factory = new FunctionFactory();
  }

  @Override
protected void setUp() throws Exception {
    defs = new ArrayList<Expression>();
    exprs = new Expression[exprStrs.length];
    results = new Expression[exprStrs.length];
    names = new String[exprStrs.length];
    for (int i = 0; i < exprStrs.length; i++) {
      exprs[i] = Parser.parse(exprStrs[i]);
      names[i] = (String)exprs[i].L;
      results[i] = Parser.parse(resultStrs[i]);
    }
    for (int i = 0; i < exprDefsStrings.length; i++) {
      Expression def = Parser.parse(exprDefsStrings[i]);
      Expression value = (Expression)def.R;
      def = (Expression)def.L;
      def.R = value;
      defs.add(def);
    }
  }

  @Override
protected void tearDown() throws Exception {
  }

  /**
   * Test of apply method, of class expressions.FunctionFactory.
   */
  public void testApply() {
    System.out.println("apply");
    
    for (int i = 0; i < exprStrs.length; i++) {
      assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 0));
    }
  }

  /**
   * Test of round method, of class expressions.FunctionFactory.
   */
  public void testRound() {
    System.out.println("round");
    exprStrs = new String[]{"approche(5/3,0.01)","approche(19,4)","approche(2/3,-4)","approche(log(100)-0.5)"};
    resultStrs = new String[]{"1.67","20","0.6667","2"};
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of compute method, of class expressions.FunctionFactory.
   */
  public void testCompute() {
    System.out.println("compute");
    exprStrs = new String[]{"calcule(5/3-2)","calcule(2.5-2)","calcule(2+1,5-1)",
    "calcule({2+1,5-1})","calcule({2+3})","calcule(|2|)","calcule(point(2+1,3+2))"};
    resultStrs = new String[]{"-1/3","1/2","3,4","{3,4}","{5}","2","point(3,5)"};
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of card method, of class expressions.FunctionFactory.
   */
  public void testCard() {
    System.out.println("card");
    exprStrs = new String[]{"card({1,2,8})","card([1,5])"};
    resultStrs = new String[]{"3","2"};
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of choice method, of class expressions.FunctionFactory.
   * @throws java.lang.Exception 
   */
  public void testChoice() throws Exception {
    System.out.println("choice");
    exprDefsStrings = new String[]{"N->{1,2,3,4}","P->{2,4,6}"};
    exprStrs = new String[]{"choix([5,10])","choix({1,8,9})","choix([-0.5,0.5],a->décimal(18))",
        "choix({2,4},[2,4],pgcd(a,b)=1)","choix([1,3],2)","choix({2,3,4,5},4,pgcd(b,c)=1,pgcd(a,d)=1)",
        "choix([1,7],[a,7])","choix(ensemble(mappe(x->calcule(2*x),(1,2))))",
        "choix({1,2,3,4},{mappe((x,y)->calcule(x+y),a,3)},a+b>a+5)",
        "choix({(0,3),(1,4)},{mappe((x,y)->calcule(x+y),a,(1,1))})",
        "choix({1,2,3,4},2)", "choix(N,2)"};
    resultStrs = new String[]{"x0€[5,10]","x0€{1,8,9}","x0€[-0.5,0.5[","pgcd(x0,x1)=1","(x0€[1,3])et(x1€[1,3])",
        "(pgcd(x1,x2)=1)et(pgcd(x0,x3)=1)","x0€[1,7] et x1€[x0,7]","x0€{2,4}","x1€[4,7]",
        "(opérande(x0,1)€[0,1])et(opérande(x1,1)€[1,2])",
        "x0€{1,2,3,4} et x1€{1,2,3,4}", "x0€{1,2,3,4} et x1€{1,2,3,4}"};
    Expression[] gets, vars;
    Relation tst;
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        Expression e = factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50);
        System.out.println(exprStrs[i] + "   " + e.printout(false));
        if(e.op == SEQ) gets = (Expression[])e.R;
        else gets = new Expression[]{e};
        vars = new Expression[gets.length];
        tst = (Relation)results[i];
        for (int j = 0; j < gets.length; j++) {
          vars[j] = var("x"+j);
          tst = (Relation) tst.substitute(vars[j], gets[j]);
        }
        assertTrue(tst.printout(false), tst.valueOf(new ArrayList<Expression>(), 0,1));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of permutation method, of class expressions.FunctionFactory.
   */
  public void testPermutation() {
    System.out.println("permutation");
    exprStrs = new String[]{"arrangement(3,{1,2,3,4,5})","arrangement(3,{1,8,9})"};
    resultStrs = new String[]{"{1,2,3,4,5}","{1,8,9}"};
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        Expression e = factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50);
        System.out.println(e.printout(false));
        e = build(SET, "{}", e.R);
        Relation tst = (Relation) Relation.build(IN, e, results[i]);
        boolean b = tst.valueOf(new ArrayList<Expression>(), 0,1);
        assertTrue(e.printout(false), tst.valueOf(new ArrayList<Expression>(), 0,1));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of compare method, of class expressions.FunctionFactory.
   */
  public void testCompare() {
    System.out.println("compare");
    exprStrs = new String[]{"compare(a+b=1,a+b=1)","compare(1.2,1.20)"};
    resultStrs = new String[]{"0","0"};    
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of breakdown method, of class expressions.FunctionFactory.
   */
  public void testBreakdown() {
    System.out.println("breakdown");
    exprStrs = new String[]{"décompose(a+b)"};
    resultStrs = new String[]{"(12,``,((-10,seq(a),a),(-10,seq(b),b)))"};    
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of makeup method, of class expressions.FunctionFactory.
   */
  public void testMakeup() {
    System.out.println("makeup");
    exprStrs = new String[]{"compose((12,``,((-10,(a),a),(-10,(b),b))))"};
    resultStrs = new String[]{"a+b"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  public void testFunc_style() {
    System.out.println("func_style");
    exprStrs = new String[]{"stylefonc(a+b+f(1))", "stylefonc(f(3*x-2)/(1+x))"};
    resultStrs = new String[]{"PLUS(a,b,f(1))", "DIV(f(MOINS(MUL(3,x),2)),PLUS(1,x))"};
    Expression e = new Expression();
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        e = factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50);
        String s = e.printout(false);
        e = Parser.parse(s);
        assertEquals(results[i], e);
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }
  
  public void testOp_style() {
    System.out.println("op_style");
    exprStrs = new String[]{"styleop(PLUS(a,b,f(1)))", "styleop(DIV(f(MOINS(MUL(3,x),2)),PLUS(1,x)))"};
    resultStrs = new String[]{"a+b+f(1)", "f(3*x-2)/(1+x)"};
    Expression e = new Expression();
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {        
        e = factory.func_style(new Expression[]{results[i]});
        e = factory.op_style(new Expression[]{e});  
        assertEquals(results[i], e);
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }
  
  /**
   * Test of set method, of class expressions.FunctionFactory.
   */
  public void testSet() {
    System.out.println("set");
    exprStrs = new String[]{"ensemble(12,4,7,6)"};
    resultStrs = new String[]{"{12,4,7,6}"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of seq method, of class expressions.FunctionFactory.
   */
  public void testSeq() {
    System.out.println("seq");
    exprStrs = new String[]{"seq([2,6])"};
    resultStrs = new String[]{"(2,3,4,5,6)"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of join method, of class expressions.FunctionFactory.
   */
  public void testJoin() {
    System.out.println("join");
    exprStrs = new String[]{"joint((1,2,3),(4,5))","joint(({1,2},{3,4}))","joint(({1,2} union {3,4}))"};
    resultStrs = new String[]{"(1,2,3,4,5)","{1,2},{3,4}","{1,2,3,4}"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of split method, of class expressions.FunctionFactory.
   */
  public void testSplit() {
    System.out.println("split");
    exprStrs = new String[]{"sépare((1,2,3,4,5),2)"};
    resultStrs = new String[]{"(1,2,3),(4,5)"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of gcd method, of class expressions.FunctionFactory.
   * 
   */
  public void testGcd() {
    System.out.println("gcd");
    exprStrs = new String[]{"pgcd(6,8)","pgcd(12,16,8)"};
    resultStrs = new String[]{"2","4"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of euclide method, of class expressions.FunctionFactory.
   */
  public void testEuclide() {
    System.out.println("euclide");
    exprStrs = new String[]{"euclide(12,5)"};
    resultStrs = new String[]{"5*2+2"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of tests method, of class expressions.FunctionFactory.
   */
  public void testTests() {
    System.out.println("tests");
    exprStrs = new String[]{"tests(0,1<2,1,2=0,2)"};
    resultStrs = new String[]{"1"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of operand method, of class expressions.FunctionFactory.
   */
  public void testOperand() {
    System.out.println("operand");
    exprStrs = new String[]{"opérande((1,2,3,4,5),[2,4])","opérande({2,3})","opérande(a+8)","opérande(M,1)"};
    resultStrs = new String[]{"(2,3,4)","2,3","(a,8)","opérande(M,1)"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of map method, of class expressions.FunctionFactory.
   */
  public void testMap() {
    System.out.println("map"); 
    exprDefsStrings = new String[]{"pts->({point(0,0),caché},{point(1,0),caché},{point(2,0),caché})",
        "points->mappe(x->{point(x,0),caché},(2,3,4))"};
    exprStrs = new String[]{"mappe({a<3:a->a},(1,2,3,4))", "mappe(x->opérande(x,1),pts)",
        "mappe(x->opérande(x,1),mappe(x->opérande(x,1),pts))", "mappe(x->opérande(x,1),points)",
        "mappe({non(compare(d,Ø)=0) : d->d},(1,2,Ø))", "mappe(x->image+x,(1,2,3),x)",
        "mappe({x>=image : x->x},(2,5,4,3),x)", "mappe(x->opérande(x,(2,3)),f(1,2,3,4))",
        "mappe(x->opérande(x,[1,3]),f(1,2,3,4))"};
    resultStrs = new String[]{"(1,2)","(point(0,0),point(1,0),point(2,0))","0,1,2","(point(2,0),point(3,0),point(4,0))",
        "(1,2)","1+2+3","5","(2,3)","(1,2,3)"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }

  /**
   * Test of makevar method, of class expressions.FunctionFactory.
   */
  public void testMakevar() {
    System.out.println("makevar");
    exprStrs = new String[]{"var(a,`+`,b)"};
    resultStrs = new String[]{"`a+b`"};        
    try {
      setUp();
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }
  }
  
  /**
   * Test of contains method, of class expressions.FunctionFactory.
   */
  public void testContains() {
    System.out.println("contains"); 
    exprStrs = new String[]{"dans(x,3*a-2)", "dans(x,(3*a-2)*(x+1))"};
    resultStrs = new String[]{"FAUX", "VRAI"};     
    try {
      setUp();
      results = new Relation[] {Relation.FALSE, Relation.TRUE};
      for (int i = 0; i < exprStrs.length; i++) {
        assertEquals(results[i], factory.apply(names[i], (Expression[])exprs[i].R, exprs[i], defs, 0, 50));
      }
    } catch (Exception ex) {
      fail("erreur d'initialisation");
    }    
  }
  
}

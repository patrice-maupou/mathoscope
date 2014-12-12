/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package expressions;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import static org.junit.Assert.assertEquals;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Patrice
 */
public class ExpressionNodeTest {

  private SyntaxNode root;
  private String[] entries, expResults;

    public ExpressionNodeTest() {
    }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

    @Before
    public void setUp() {
      File syntax = new File("exercices/definitions/math_syntax.xml");
      File logic = new File("exercices/definitions/logic_syntax.xml"); // syntaxe ajoutée
      File test = new File("test/expressions/test_syntax.xml");
    try {
      // création de l'arbre syntaxique root
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.parse(syntax);

      root = SyntaxNode.build(document, "SYNTAX_TREE");
      document = documentBuilder.parse(logic);
      SyntaxNode logic_root = SyntaxNode.build(document, "SYNTAX_TREE");
      //*
      root.merge(logic_root);
      //*/
      root.setMaps(root.getPatternsOfName());

      // mets la liste des expressions dans entries
      document = documentBuilder.parse(test);
      NodeList nodeList = document.getElementsByTagName("texts");
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        sb.append(node.getTextContent());
      }
      String s = sb.toString();
      entries = s.split("\",\\s+\"");

      // mets la liste des résultats dans expResults
      nodeList = document.getElementsByTagName("results");
      sb = new StringBuilder();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        sb.append(node.getTextContent());
      }
      s = sb.toString();
      expResults = s.split("\",\\s+\"");
      s = "";
    } catch (ParserConfigurationException parserConfigurationException) {
      System.out.println(parserConfigurationException.getMessage());
    } catch (SAXException sAXException) {
      System.out.println(sAXException.getMessage());
    } catch (IOException iOException) {
      System.out.println(iOException.getMessage());   }
    }

    @After
    public void tearDown() {
    }




  /**
   * Test of toString method, of class ExpressionNode.
   * l'exemple a+3*(17+b) montre que le dernier "+" n'est pas forcément le bon
   */
  @Test
  public void testToString() {
    System.out.println("toString");
    int i = 0, n = expResults.length;
    for (i = 1; i < n-1; i++) {
      ExpressionNode en = new ExpressionNode(entries[i], root);
      String result = en.toString();
      if(en.isValid()) {
        System.out.println(result +  "  de modèle : " + en.getModel() + ",   de type : " + en.getType());
      }
      else {
        result = en.getMessage();
        System.out.println(en.getMessage() + " (modèle : " + en.getModel() + ")");
      }
      assertEquals("Erreur sur : " + entries[i], expResults[i], result);
    }
  }

}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package expressions;

import java.util.AbstractMap;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.Set;
import java.util.TreeMap;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;
import static org.junit.Assert.*;

/**
 *
 * @author Patrice
 */
public class SyntaxNodeTest {

  private SyntaxNode root;
  private Document document;

  public SyntaxNodeTest() {
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
    File logic = new File("exercices/definitions/logic_syntax.xml");
    File test = new File("test/expressions/test_syntax.xml");
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document0 = documentBuilder.parse(syntax);
      root = SyntaxNode.build(document0, "SYNTAX_TREE");
      //*
      Document document1 = documentBuilder.parse(logic);
      SyntaxNode logic_root = SyntaxNode.build(document1, "SYNTAX_TREE");
      root.merge(logic_root);
      //*/
      document = documentBuilder.parse(test);
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
   * Test of getChilds method, of class SyntaxNode.
  @Test
  public void testGetChilds() {
    System.out.println("getChilds");
    String expResult = "unaryAr binaryAr function par log_ops compare add_ops signed prod_ops pow_ops number ";
    ArrayList<SyntaxNode> result = root.getChilds();
    String resultToString = "";
    for (SyntaxNode node : result) {
      resultToString += node.getId() + " ";
      System.out.println("patterns of name" + node.getPatternsOfName());
    }

    assertEquals(expResult, resultToString);
  }
   */

  @Test
  public void testSetMaps() {
    System.out.println("setMaps");
    Element first = document.getDocumentElement();
    HashMap<String, ArrayList<SyntaxNode>> map = root.getPatternsOfName();

    System.out.println(" sous-types");
    HashMap<String, ArrayList<String>> subtypes = root.setMaps(map);
    NodeList list = first.getElementsByTagName("Subtypes");
    if(list.getLength() != 1) {fail("Subtypes ou répété");}
    assertEquals("sous-types non conformes", getMap((Element) list.item(0), "String"), subtypes);

    System.out.println(" patterns of Names");
    Set<String> keys = map.keySet();
    list = first.getElementsByTagName("patternsOfNames");
    if(list.getLength() != 1) {fail("patternsOfNames non trouvé ou répété");}
    AbstractMap<?, ArrayList<String>> mapElems = getMap((Element) list.item(0), "String");
    for (String key : keys) {
      if (!key.equals("SYNTAX_TREE")) {
        ArrayList<SyntaxNode> nodes = map.get(key);
        ArrayList<String> expResult = mapElems.get(key);
        ArrayList<String> result = new ArrayList<String>();
        for (SyntaxNode syntaxNode : nodes) {
          result.add(syntaxNode.getId()+syntaxNode.getPattern());
        }
        assertEquals("patterns of Names non conforme",expResult, result);
      }
    }

    System.out.println(" patterns of childs");
    list = first.getElementsByTagName("ChildsTypes");
    if(list.getLength() != 1) {fail("ChildsTypes ou répété");}
    checkChildPatterns(root, (Element) list.item(0));

    System.out.println(" noeuds de recherche pour les enfants");
  }

/**
 *
 * @param syntaxNode
 * @param elem
 */
  private void checkChildPatterns(SyntaxNode syntaxNode, Element elem) {
    TreeMap<Integer, ArrayList<String>> map = syntaxNode.getChildsTypes();
    if (map != null) {
      assertEquals("childPatterns différents pour " + elem.getTagName(), map, getMap(elem, "Integer"));
    }
    NodeList list = elem.getChildNodes();
    int n = 0;
    ArrayList<SyntaxNode> childs = syntaxNode.getChilds();
    for (int i = 0; i < childs.size(); i++) {
      if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
        if (childs.get(i).getId().equals(list.item(n).getNodeName())) {
          checkChildPatterns(childs.get(i), (Element) list.item(n));
        }
        n++;
      }
    }
  }

  /**
   * donne le texte des childPatterns
   * @param syntaxNode
   * @param text
   * @param indent
   */
  private void childPatterns(SyntaxNode syntaxNode, StringBuilder text, String indent) {
    String indent2 = indent + "  ", indent4 = indent + "    ";
    TreeMap<Integer, ArrayList<String>> map = syntaxNode.getChildsTypes();
    String name = (syntaxNode.getId().equals("SYNTAX_TREE"))? "ChildsTypes" : syntaxNode.getId();
    text.append(indent).append("<").append(name).append(">\n");
    if (map != null) {
      for (Iterator<Integer> it = map.keySet().iterator(); it.hasNext();) {
        Integer nextrange = it.next();
        String next = "D" + nextrange;
        text.append(indent2).append("<").append(next).append(">\n").append(indent4);
        ArrayList<String> types = map.get(nextrange);
        for (String type : types) {
          text.append("<").append(type).append("/>");
        }
        text.append("\n").append(indent2).append("</").append(next).append(">\n");
      }
    }
    for (SyntaxNode child : syntaxNode.getChilds()) {
      childPatterns(child, text, indent2);
    }
    text.append(indent).append("</").append(name).append(">\n");
  }

  /**
   *
   * @param tagName
   * @return la table associant à un élément une liste de chaînes
   */
  private AbstractMap<?, ArrayList<String>> getMap(Element first, String type) {
    TreeMap<Integer, ArrayList<String>> mapIntegers = new TreeMap<Integer, ArrayList<String>>();
    HashMap<String, ArrayList<String>> mapElems = new HashMap<String, ArrayList<String>>();
    NodeList nodeList = first.getChildNodes();
    ArrayList<Element> elements = new ArrayList<Element>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if(node.getNodeType() == Node.ELEMENT_NODE) {
        elements.add((Element) node);
      }
    }
    for (Element e : elements) {
      ArrayList<String> list = new ArrayList<String>();
      NodeList nl = e.getElementsByTagName("*");
      for (int i = 0; i < nl.getLength(); i++) {
        Element elem = (Element) nl.item(i);
        String v = elem.getTagName();
        NodeList dataNode = elem.getChildNodes();
        for (int j = 0; j < dataNode.getLength(); j++) {
          if (dataNode.item(j).getNodeType() == Node.CDATA_SECTION_NODE) {
            v += dataNode.item(j).getNodeValue();
          }
        }
        list.add(v);
      }
      if (type.equals("String")) {
        mapElems.put(e.getTagName(), list);
      }
      else if (type.equals("Integer")) {
        String s = e.getTagName();
        if (s.matches("D(\\d+)")) {
          mapIntegers.put(Integer.parseInt(s.substring(1)), list);
        }
      }
    }
    return (type.equals("String"))? mapElems : mapIntegers;
  }

/*
  @Test
  public void testGetTreeMap() {
    System.out.println("getTreeMap");
    TreeMap<Integer, ArrayList<String>> map = SyntaxNode.getTreemap("child");
  }
//*/

  /**
   * Test of getId method, of class SyntaxNode.
   */
  @Test
  public void testGetId() {
    System.out.println("getId");
    String expResult = "SYNTAX_TREE";
    String result = root.getId();
    assertEquals(expResult, result);
  }

  /**
   * Test of getPattern method, of class SyntaxNode.
   */
  @Test
  public void testGetPattern() {
    System.out.println("getPattern");
    Pattern expResult = null;
    Pattern result = root.getPattern();
    assertEquals(expResult, result);
    System.out.println("");
  }




  /**
   * Test of getMapVariables method, of class SyntaxNode.
   */
  @Test
  public void testGetMapVariables() {
    System.out.println("getMapVariables");
    HashMap expResult = null;
    HashMap result = root.getMapVariables();
    assertEquals(expResult, result);
  }

  /**
   * Test of toString method, of class SyntaxNode.
   */
  @Test
  public void testToString() {
    System.out.println("toString");
    //print(root);
  }

  private void print(SyntaxNode root) {
    System.out.println(root);
    for (SyntaxNode child : root.getChilds()) {
      print(child);
    }
  }

  /**
   * Test of isValid method, of class SyntaxNode.
   */
  @Test
  public void testIsValid() {
    System.out.println("isValid");
    boolean expResult = true;
    boolean result = root.isValid();
    assertEquals(expResult, result);
  }


}

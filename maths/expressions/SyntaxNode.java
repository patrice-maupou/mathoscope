package expressions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * Classe de noeuds, la racine est créée à partir d'un document décrivant la grammaire, les autres
 * noeuds à partir du premier.
 */
public class SyntaxNode {

  private SyntaxNode parent;
  private String rootId;
  private ArrayList<SyntaxNode> childs;                            // liste des descendants directs
  private HashMap<String, String> mapVariables;                    // table des variables
  private HashMap<String, ArrayList<SyntaxNode>> patternsOfName;   // modèles conduisant au nom
  private HashMap<String, ArrayList<SyntaxNode>> patternsOfVar;    // modèles des variables
  private TreeMap<Integer, ArrayList<SyntaxNode>> childSearchNodes;// modèles pour les enfants
  private TreeMap<Integer, ArrayList<String>> childsTypes;         // noms des modèles pour les enfants
  private TreeMap<Integer, ArrayList<String>> groupNames;
  private String id;                                               // tag du noeud
  private Pattern pattern;                                         // patron si regex
  private boolean valid, afterFirstPattern;
  private regexMethod method;



  /**
   * types de méthodes utilisées pour les modèles
   */
  public static enum regexMethod {

    MATCH, FIND, SPLIT
  }


  /**
   * méthode de type factory pour un arbre syntaxique à partir d'un document
   * @param document
   * @param rootId le nom de la racine
   * @return le noeud racine
   */
  public static SyntaxNode build(Document document, String rootId) {
    Element elem;
    NodeList nodelist = document.getElementsByTagName("expression");
    elem = (Element) nodelist.item(0);
    elem.normalize();
    nodelist = elem.getElementsByTagName("include");
    for (int i = 0; i < nodelist.getLength(); i++) {
        System.out.println(nodelist.item(i).getNodeName());
    }
    nodelist = elem.getElementsByTagName("autre");
    for (int i = 0; i < nodelist.getLength(); i++) {
      elem = (Element) nodelist.item(i);
      System.out.println(elem.getNodeName());
      //include(elem);
    }
    SyntaxNode root = new SyntaxNode(document, rootId);
    return root;
  }


  /**
   * Insère un noeud dans l'arbre syntaxique
   * //TODO : utiliser plutôt un tag import file=""
   * @param newNode à fusionner avec le noeud courant
   */
  public void merge(SyntaxNode newNode) {
    ArrayList<SyntaxNode> newNodeChilds = newNode.getChilds();
    int last = 0;
    ArrayList<SyntaxNode> childscurrent = new ArrayList<SyntaxNode>();
    childscurrent.addAll(childs);
    loopNewNode:
    for (SyntaxNode newNodeChild : newNodeChilds) {
      for (int i = 0; i < childscurrent.size(); i++) {
        SyntaxNode child = childscurrent.get(i);
        if (newNodeChild.id.equals(child.id)) { // même nom de noeud
          last = i + 2;
          child.merge(newNodeChild);
          continue loopNewNode;
        }
      }
      childs.add(last, newNodeChild); // non trouvé, on insère
    }
  }

  /**
   * Crée un noeud syntaxique et ses descendants à partir d'une chaîne dans un document.
   * @param document créé à partir du fichier xml définissant la syntaxe
   */
  private SyntaxNode(Document document, String rootId) {
    id = rootId;
    Element elem;
    childs = new ArrayList<SyntaxNode>();
    NodeList PatternList = document.getElementsByTagName(id); // un seul noeud en principe
    for (int i = 0; i < PatternList.getLength(); i++) {
      elem = (Element) PatternList.item(i);
      if (valid = elem != null) {
        addChilds(elem, false);
      }
    }
    patternsOfName = new HashMap<String, ArrayList<SyntaxNode>>();
    childSearchNodes = new TreeMap<Integer, ArrayList<SyntaxNode>>();
  }


  private void include(Element elem) {
    try {
      Node nextNote = elem.getNextSibling();
      Node parentNode = elem.getParentNode();
      NamedNodeMap map = elem.getAttributes();
      Node nodeFile = map.getNamedItem("file");
      String filename = nodeFile.getNodeValue();

    } catch (NullPointerException e) {
    }
  }

  /**
   * Crée un noeud syntaxique et ses descendants à partir d'un élément de la syntaxe
   * @param parent
   * @param elem
   */
  private SyntaxNode(SyntaxNode parent, Element elem, boolean afterFirstPattern) {
    this.id = elem.getNodeName();
    valid = true;
    this.parent = parent;
    this.afterFirstPattern = afterFirstPattern;
    mapVariables = new HashMap<String, String>();
    patternsOfVar = new HashMap<String, ArrayList<SyntaxNode>>();
    patternsOfName = new HashMap<String, ArrayList<SyntaxNode>>();
    NamedNodeMap attribs = elem.getAttributes();
    boolean primaryPattern = false;
    int active = -1;
    for (int i = 0; i < attribs.getLength(); i++) {
      Node attr = attribs.item(i);
      String name = attr.getNodeName();
      String value = attr.getNodeValue();
      if (name.equals("match")) {
        method = regexMethod.MATCH;
      }
      else if (name.equals("find")) {
        method = regexMethod.FIND;
      }
      else if (name.equals("split")) {
        method = regexMethod.SPLIT;
      }
      else if (name.equals("groups") && !value.isEmpty()) {
        try {
          groupNames = getTreemap(value);
        } catch (NumberFormatException e) {
          valid = false;
          System.out.println("entier non trouvé");
        }
      }
      else if (name.equals("childspattern")) {
        try {
          childsTypes = getTreemap(value);
          childSearchNodes = new TreeMap<Integer, ArrayList<SyntaxNode>>();
        } catch (NumberFormatException e) {
          valid = false;
          System.out.println("entier non trouvé");
        }
      }
      else {
        mapVariables.put(name, value);
      }
      if (method != null) {
        active++;
      }
      if (active == 0 && !value.isEmpty()) {
        if ("CDATA".equals(value)) {
          NodeList nodeList = elem.getChildNodes();
          for (int j = 0; j < nodeList.getLength(); j++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
              value = node.getNodeValue();
              break;
            }
          }
          if ("CDATA".equals(value)) valid = false;
        }
        this.pattern = Pattern.compile(value, Pattern.UNICODE_CASE);
        if (!afterFirstPattern) {
          primaryPattern = true;
        }
      }
    }
    childs = new ArrayList<SyntaxNode>();
    addChilds(elem, afterFirstPattern || primaryPattern);
    if (!valid) {
      System.out.println("arbre syntaxique non valide");
    }
  }


  /**
   * complète les différentes tables de l'arbre, après la création de l'objet.
   * patternsOfName : les noeuds de recherche amenant à la chaîne
   * childSearchNodes : les noeuds de recherche amenant à l'enfant de rang inférieur à l'entrée
   * @param patterns liste vide à remplir
   * @return la table des sous-types
   */
  public HashMap<String, ArrayList<String>> setMaps(HashMap<String, ArrayList<SyntaxNode>> patterns) {
    HashMap<String, ArrayList<String>> subtypes = new HashMap<String, ArrayList<String>>();
    typeHerarchy(subtypes);
    refineChildsTypes(subtypes);
    setPatternsOfName(patterns);
    setChildSearchNodes(patterns);
    setPatternsOfVar(patterns);
    return subtypes;
  }

  /**
   * crée la liste des descendants directs de l'élément
   * @param elem élément d'un document
   * @return les noms des tags ajoutés
   */
  private void addChilds(Element elem, boolean afterFirstPattern) {
    ArrayList<String> childsNames = new ArrayList();
    NodeList nodeList = elem.getElementsByTagName("*"); // la liste de tous les éléments en dessous
    for (int i = 0; i < nodeList.getLength(); i++) {
      Element node = (Element) nodeList.item(i);
      if (node.getParentNode().equals(elem)) { // descendant direct
        SyntaxNode child = new SyntaxNode(this, node, afterFirstPattern);
        valid &= child.valid;
        childs.add(child);
        childsNames.add(node.getTagName());

      }
    }
  }


  /**
   * établit la table associant le rang du modèle suivant au patrons de recherche
   * Exemple :
   * @param text
   * @return la table
   * @throws NumberFormatException pas d'entier possible
   */
  public static TreeMap<Integer, ArrayList<String>> getTreemap(String text) throws NumberFormatException {
    TreeMap<Integer, ArrayList<String>> map = new TreeMap<Integer, ArrayList<String>>();
    String[] strings = text.split(",");
    int nextrange = 0; // le rang suivant
      for (String string : strings) {
          String repeat = "1";
          Matcher m = Pattern.compile("\\[\\d*\\]").matcher(string);
          if (m.find()) { // pattern répété : [] ou [3]
              repeat = m.group().substring(1, m.group().length() - 1);
              string = string.substring(0, m.start());
          }
          ArrayList<String> list = new ArrayList<String>();
          String[] s = string.split("\\|");
          list.addAll(Arrays.asList(s));
          int n = (repeat.isEmpty()) ? Integer.MAX_VALUE : Integer.parseInt(repeat);
          nextrange += n;
          map.put(nextrange, list);
      }
    return map;
  }

  /** modèles donnés par childspattern
   * associe au rang suivant des enfants les noeuds de recherche
   * @return the childsTypes
   */
  public TreeMap<Integer, ArrayList<SyntaxNode>> getChildsSearchNodes() {
    return childSearchNodes;
  }


  /**
   * associe au rang suivant des enfants les noeuds de recherche dans la table childSearchNodes
   * exemple : childspattern="number[2]", donc 2=number
   * @param patterns liste des modèles conduisant au nom
   */
  private void setChildSearchNodes(HashMap<String, ArrayList<SyntaxNode>> patterns) {
    if(getChildsTypes() != null) {
      for (Iterator<Integer> range = getChildsTypes().keySet().iterator(); range.hasNext();) {
        int r = range.next();
        ArrayList<String> names = getChildsTypes().get(r); // noeuds de recherche
        ArrayList<SyntaxNode> nodes = new ArrayList<SyntaxNode>();
        for (String name : names) {
          ArrayList<SyntaxNode> newnodes = patterns.get(name);
          if (newnodes != null) {
            nodes.addAll(newnodes);
          }
        }
        getChildsSearchNodes().put(r, nodes);
      }
    }
    for (SyntaxNode child : getChilds()) {
      child.setChildSearchNodes(patterns);
    }
  }

  /**
   * @return the childsTypes
   */
  public TreeMap<Integer, ArrayList<String>> getChildsTypes() {
    return childsTypes;
  }

  /**
   * remplit la table subtypes récursivement pour chaque noeud et ses descendants
   * ex : nombre -> [nombre,integer,natural]
   *
   * @param subtypes associe à un type les sous-types correspondants,
   */
  private void typeHerarchy(HashMap<String, ArrayList<String>> subtypes) {
    SyntaxNode ancestor = this;
    do { // boucle sur les ascendants
      ArrayList<String> types = subtypes.get(ancestor.id);
      if (types == null) {
        types = new ArrayList<String>();
      }
      if (!types.contains(id)) {
        types.add(id);
      }
      subtypes.put(ancestor.id, types);
    } while ((ancestor = ancestor.getParent()) != null);
    for (SyntaxNode child : childs) {
      child.typeHerarchy(subtypes);
    }
  }

  /**
   * complète la table subtypes récursivement avec les sous-types
   * Exemple : si 2=number, 2=[number,intvar,..]
   * @param childsTypes la table associant au rang suivant les types à rechercher
   */
  private void refineChildsTypes(HashMap<String, ArrayList<String>> subtypes) {
    TreeMap<Integer, ArrayList<String>> mapChildSearchNames = getChildsTypes();
    if(mapChildSearchNames != null) {
      for (Iterator<Integer> range = mapChildSearchNames.keySet().iterator(); range.hasNext();) {
        Integer r = range.next();
        ArrayList<String> types = mapChildSearchNames.get(r);
        ArrayList<String> newtypes = new ArrayList<String>();
        for (String type : types) {
          newtypes.addAll(subtypes.get(type));
        }
        mapChildSearchNames.put(r, newtypes);
      }
    }
    for (SyntaxNode child : childs) {
      child.refineChildsTypes(subtypes);
    }
  }

  /**
   * remplit la table patterns à  partir de la racine
   * @param patterns la table des modèles conduisant au nom
   */
  private void setPatternsOfName(HashMap<String, ArrayList<SyntaxNode>> patterns) {
    ArrayList<SyntaxNode> nodePatterns = patterns.get(id);
    if (nodePatterns == null) { // initialisation
      nodePatterns = new ArrayList<SyntaxNode>();
      patterns.put(id, nodePatterns);
    }
    if (isAfterFirstPattern()) { // le patron est un ancêtre à trouver
      SyntaxNode primaryPattern = getParent();
      while (primaryPattern.isAfterFirstPattern()) { // il faut remonter au premier false
        primaryPattern = primaryPattern.getParent();
      }
      if (!nodePatterns.contains(primaryPattern)) {
        nodePatterns.add(primaryPattern);
      }
    }
    else if (getPattern() != null) { // patron primaire, les ancêtres s'y réfèrent
      SyntaxNode ancestor = this;
      while (ancestor != null) {
        ArrayList<SyntaxNode> ancestorPatterns = patterns.get(ancestor.getId());
        ancestorPatterns.add(this);
        ancestor = ancestor.getParent();
      }
    }
    for (SyntaxNode child : getChilds()) {
      child.setPatternsOfName(patterns);
    }
  }

/**
 * @return patternsOfName
 */
  public HashMap<String, ArrayList<SyntaxNode>> getPatternsOfName() {
    return patternsOfName;
  }

  /**
   * @return the patternsOfVar
   */
  public HashMap<String, ArrayList<SyntaxNode>> getPatternsOfVar() {
    return patternsOfVar;
  }


  /**
   * certaines variables sont à rechercher dans les modèles
   * @param patterns la table des noeuds de recherche pour un nom donné
   */
  private void setPatternsOfVar(HashMap<String, ArrayList<SyntaxNode>> patterns) {
    if(!getId().equals("SYNTAX_TREE")) {
      for (String key : getMapVariables().keySet()) {
        String value = getMapVariables().get(key);
        if (patterns.containsKey(value)) {
          ArrayList<SyntaxNode> nodes = patterns.get(value);
          getPatternsOfVar().put(key, nodes);
        }
      }
    }
    for (SyntaxNode child : getChilds()) {
      child.setPatternsOfVar(patterns);
    }
  }

  public SyntaxNode getParent() {
    return parent;
  }

  public ArrayList<SyntaxNode> getChilds() {
    return childs;
  }

  /**
   * fournit le nom du tag
   * @return le nom du noeud
   */
  public String getId() {
    return id;
  }

  /**
   * @return the afterFirstPattern
   */
  public boolean isAfterFirstPattern() {
    return afterFirstPattern;
  }

  public Pattern getPattern() {
    return pattern;
  }

  /**
   * @return the method
   */
  public regexMethod getMethod() {
    return method;
  }

  /**
   * @return the groupNames
   */
  public TreeMap<Integer, ArrayList<String>> getGroupNames() {
    return groupNames;
  }

  /**
   * @return the attributes
   */
  public HashMap<String, String> getMapVariables() {
    return mapVariables;
  }

  /**
   * @return the valid
   */
  public boolean isValid() {
    return valid;
  }


  @Override
  public boolean equals(Object obj) {
    boolean ret = obj instanceof SyntaxNode;
    if(ret) {
      SyntaxNode sn = (SyntaxNode) obj;
      ret = id.equals(sn.id) && pattern.equals(sn.pattern) && method.equals(sn.method) && valid == sn.valid;
      ret &= mapVariables.equals(sn.mapVariables);
    }
    return ret;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 37 * hash + (this.mapVariables != null ? this.mapVariables.hashCode() : 0);
    hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
    hash = 37 * hash + (this.pattern != null ? this.pattern.hashCode() : 0);
    hash = 37 * hash + (this.valid ? 1 : 0);
    hash = 37 * hash + (this.method != null ? this.method.hashCode() : 0);
    return hash;
  }


  @Override
  public String toString() {
    String text = "name : " + id;
    if (pattern != null) {
      text += "\n  pattern : " + pattern.toString() + " type : " + method;
    }
    if (mapVariables != null && !mapVariables.isEmpty()) {
      text += "\n  variables : \n";
      Set<String> vars = mapVariables.keySet();
      for (String var : vars) {
        text += var + "=" + mapVariables.get(var) + "  ";
      }
    }
    return text;
  }
}

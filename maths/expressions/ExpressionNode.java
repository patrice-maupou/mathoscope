/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static expressions.SyntaxNode.*;

/**
 *
 * @author Patrice
 */
public class ExpressionNode {

  private String name, type, message, model;
  private ExpressionNode parent;
  private ArrayList<ExpressionNode> childs;
  private boolean valid;


  /**
   * crée une expression correspondant à text et la la syntaxe définie par root
   * @param text le texte de l'expression
   * @param root la racine de l'arbre syntaxique
   */
  public ExpressionNode(String text, SyntaxNode root) {
    ArrayList<SyntaxNode> primaryPatterns = root.getPatternsOfName().get(root.getId());
    setParams(text, primaryPatterns, null);
    if(name == null) valid = false;
  }


  /**
   * Crée un noeud ainsi que les descendants représentant une expression à partir d'un texte et d'une
   * syntaxe
   */
  private ExpressionNode(String text, ExpressionNode parent, ArrayList<SyntaxNode> searchPatterns,
          ArrayList<String> types) {
    this.parent = parent;
    setParams(text, searchPatterns, types);
  }

  /**
   * établit les paramètres d'un noeud et de ses descendants
   * Exemple : ADD(3,4), syntaxParent.id = SYNTAX_TREE, syntaxParent.childs = function, add_ops,..
   *  function -> fname:ADD params:3,4 dans groups
   *  params -> child:3 params:4
   *  params -> child:4
   *  number -> name:fname:ADD child..
   * @param text l'écriture habituelle de l'expression
   * @param searchPatterns les modèles de recherche
   * @param types les types possibles pour l'expression
   */
  private void setParams(String text, ArrayList<SyntaxNode> searchPatterns, ArrayList<String> types) {
    valid = false;
    childs = new ArrayList<ExpressionNode>();
    String Id = null;
    message = "";
    HashMap<String, String> groups = new HashMap<String, String>();
    ArrayList<String> childstext = new ArrayList<String>();
    boolean noPatternFound = true;
    nextnode:
    for (SyntaxNode syntaxNode : searchPatterns) { // on cherche le patron correct
      Id = syntaxNode.getId();
      ArrayList<String[]> groupsList = searchGroups(text, syntaxNode, groups);
      if (syntaxNode.getMethod() != regexMethod.SPLIT) {
        if (noPatternFound = groupsList.isEmpty()) {
          continue;
        }
        for (String[] solution : groupsList) {
          childstext = setGroups(solution, syntaxNode, groups);
          setVariables(text, syntaxNode, groups);
          if (valid) {
            if (childstext.isEmpty() && !groups.isEmpty()) { // groups contient des variables
              valid = searchNodeMatch(text, syntaxNode, groups);
            }
            else if(!childstext.isEmpty()) {
              valid = setChilds(childstext, syntaxNode, groups, false);
            }
          }
          if (valid) {
            break nextnode;
          }
        }
      }
      else if(valid) noPatternFound = false;
      if(valid) break;
    }
    if(!message.isEmpty()) message += ", ";
    if (!valid && noPatternFound) {
      //message += text + " : non valide, modèle non trouvé";
      message = text + " : non valide, modèle non trouvé";
      type = null;
      name = null;
    }
    else if(name == null) {
      //message += text + " : non valide, pas de nom";
      message = text + " : non valide, pas de nom";
    }
    else if (types != null) {
      //TODO : types.contains(type) est incorrect, integer et [number, intvar, positive]
      if (!(valid = types.contains(type))) message = "type incorrect";
    }
    if(!noPatternFound) model = Id;
  }


  /**
   * cherche si un enfant de syntaxNode modèle l'un des groupes (une variable). Sinon, on établit
   * les variables
   * @param text la chaîne à interpréter
   * @param syntaxNode le parent des noeuds de recherche
   * @param groups
   * @return vrai si l'un des enfants de syntaxNode modèle un groupe
   */
  private boolean searchNodeMatch(String text, SyntaxNode syntaxNode, HashMap<String, String> groups) {
    boolean found = true;
    nodes:
    for (SyntaxNode searchNode : syntaxNode.getChilds()) { // number, boolean, ..
      final String id = searchNode.getId();
      if (groups.containsKey(id)) { // est-ce un modèle ? (ex : params ou groups:{inside->3+4})
        String idGroup = groups.get(id);
        if (searchNode.getPattern() == null) {
          message = "pas de modèle pour : " + id;
        }
        else {
          ArrayList<String[]> groupsList = searchGroups(idGroup, searchNode, groups);
          found = valid;
          if (!groupsList.isEmpty()) { // il y a des groupes à chercher
            for (String[] idgroups : groupsList) {
              ArrayList<String> childstext = setGroups(idgroups, searchNode, groups);
              if(found = setChilds(childstext, searchNode, groups, true)) // searchNode convient ?
                break nodes;
            }
          }
        }
      }
      else if (found = setVariables(text, searchNode, groups)) { // rien à chercher
        break;
      }
    }
    return found;
  }


  /**
   *
   * @param text la chaîne d'entrée à interpréter
   * @param model le modèle proposé
   * @param method le type de traitement
   * @return les possibilités de groupes
   */
  private ArrayList<String[]> searchGroups(String text, SyntaxNode syntaxNode,
          HashMap<String,String> groups) {
    ArrayList<String[]> groupsList = new ArrayList<String[]>();
    Pattern pattern = syntaxNode.getPattern();
    Matcher matcher = pattern.matcher(text);
    switch (syntaxNode.getMethod()) {
      case MATCH:
        if (matcher.matches()) { // patron trouvé, on cherche groupes et enfants
          groupsList.add(getGroups(matcher)); // refaire un setGroups(subgroups, syntaxNode, groups)
        }
        break;
      case FIND:
        while (matcher.find()) {        // 2 ou 3 groupes sont créés
          String before = text.substring(0, matcher.start());
          String after = text.substring(matcher.end());
          String middle = matcher.group();
          if (matcher.groupCount() == 2) { // la fin et le début
            before = before + matcher.group(1);
            middle = text.substring(matcher.end(1), matcher.start(2));
            after = matcher.group(2) + after;
          }
          /* modif
          ArrayList<String> list = new ArrayList<String>();
          //if(!before.isEmpty())
            list.add(before);
          if(!middle.isEmpty()) list.add(middle);
          //if(!after.isEmpty())
            list.add(after);
          groupsList.add(0, list.toArray(new String[list.size()]));
          //*/
          //* avant
          if (middle.isEmpty()) {
            groupsList.add(0, new String[]{before, after});
          }
          else {
            groupsList.add(0, new String[]{before, middle, after});
          }
          //*/
        }
        break;
      case SPLIT:
        ArrayList<SyntaxNode> nodes = syntaxNode.getChilds();
        ArrayList<ExpressionNode> list = new ArrayList<ExpressionNode>();
        for (int i = 0; i < nodes.size(); i++) {
          SyntaxNode inode = nodes.get(i);
          if (valid = setVariables(text, inode, groups)) {
            if (valid = findChilds(text, pattern, i, nodes, list, 0) && list.size() > 1) {
              childs = list;
              setType(inode.getId());
              break;
            }
          }
        }
      default:
    }
    return groupsList;
  }

/**
 * recherche des enfants à partir d'un séparateur (, ou ; ou .)
 * @param text à interpréter
 * @param p le model du séparateur
 * @param current l'index du noeud déjà trouvé
 * @param nodes la liste des noeuds pouvant convenir
 * @param list liste courante des enfants déjà obtenus
 * @return
 */
  private boolean findChilds(String text, Pattern p, int current, ArrayList<SyntaxNode> nodes,
          ArrayList<ExpressionNode> list, int nb) {
    boolean found = false, afterIsEmpty = false;
    Matcher matcher = p.matcher(text);
    while (matcher.find()) {
      String childtext = text.substring(0, matcher.start());
      String after = text.substring(matcher.end());
      try {
        if (found = nodeMatch(childtext, nodes.get(current), list, null)) {
          if (!after.isEmpty()) { // récurrence
            found = findChilds(after, p, current, nodes, list, nb + 1);
          }
          else {
            afterIsEmpty = true;
            found = false;
            break;
          }
          if (found) {
            break;
          }
        }
      } catch (IndexOutOfBoundsException e) {// current dépasse la taille de nodes
      }
    }
    if (!afterIsEmpty && !found && nb > 0) { // essai avec le dernier (il doit y avoir des enfants avant)
      found = nodeMatch(text, nodes.get(current), list, null);
    }
    return found;
  }

  /**
   * ajoute à list l'expression si elle est conforme au noeud de nodes de rang current
   * TODO : boucle infinie possible par new ExpressionNode(text, this, searchNodes)
   * @param text à interpréter
   * @param node le noeud syntaxique pouvant convenir
   * @param list à compléter si l'expression est valide
   * @param types les types possibles
   * @return la validité de l'expressionNode créée
   */
  private boolean nodeMatch(String text, SyntaxNode node, ArrayList<ExpressionNode> list,
          ArrayList<String> types) throws IndexOutOfBoundsException {

    TreeMap<Integer, ArrayList<SyntaxNode>> childspattern = node.getChildsSearchNodes();
    int nextrange = 0;
    try {
      nextrange = childspattern.higherKey(list.size());
      ArrayList<SyntaxNode> searchNodes = childspattern.get(nextrange);
      ExpressionNode nextchild = new ExpressionNode(text, this, searchNodes, types);
      if (valid = nextchild.isValid()) {
        list.add(nextchild);
      }
    } catch (NullPointerException e) {
      valid = false;
    }
    //else message = nextchild.getMessage();
    return valid;
  }

  /**
   * associe les variables aux groupes
   * @param text
   * @param syntaxNode le noeud syntaxique courant
   * @param groups
   * @return
   */
  private boolean setVariables(String text, SyntaxNode syntaxNode, HashMap<String, String> groups) {
    HashMap<String, String> vars = syntaxNode.getMapVariables();
    ArrayList<String> childstext = new ArrayList<String>();
    valid = true;
    for (String key : vars.keySet()) {
      String value = vars.get(key);
      if (key.equals("name")) { // recherche du nom du noeud
        setType(syntaxNode.getId());
        if (value.equals("match")) {
          name = text;
        }
        else { // la valeur est le nom
          String groupval = groups.get(value);
          name = (groupval == null) ? value : groupval;
        }
      }
      else { // la clé est le nom du groupe, la valeur est le nom du modèle
        String group = groups.get(key);
        HashMap<String, ArrayList<SyntaxNode>> searchNodes = syntaxNode.getPatternsOfVar();
        if (group != null && isValid()) {
          if (value.equals("child")) {
            childstext.add(group);
          }
          else if (searchNodes != null && searchNodes.get(key) != null) { // pour inside
            ExpressionNode newNode = new ExpressionNode(group, parent, searchNodes.get(key), null);
            name = newNode.name;
            if(name == null) name = group;
            setType(newNode.getType());
            childs = newNode.childs;
            valid = newNode.isValid();
          }
          else if (!(valid = group.matches(value))) {
            String path = syntaxNode.getParent().getId() + "->" + syntaxNode.getId();
            //message = "noeud : " + path + ", ne trouve pas : " + value;
            break;
          }
        }
      }
    }
    if (isValid() && !childstext.isEmpty()) {
      valid = setChilds(childstext, syntaxNode, new HashMap<String, String>(), true);
    }
    return isValid();
  }

  /**
   * Crée la liste des enfants
   * @param syntaxNode le noeud syntaxique étudié
   * @param childstext la liste des textes décrivant les enfants
   * @param groups
   * @param varsDone true si setVariables a été effectué
   */
  private boolean setChilds(ArrayList<String> childstext, SyntaxNode syntaxNode,
          HashMap<String, String> groups, boolean varsDone) {
    TreeMap<Integer, ArrayList<SyntaxNode>> childSearchNodes = syntaxNode.getChildsSearchNodes();
    if (childSearchNodes == null) { // rechercher dans le niveau suivant
      for (SyntaxNode childNode : syntaxNode.getChilds()) { // ex : ADD, SUB
        if (valid = setChilds(childstext, childNode, groups, false)) {
          break;
        }
      }
      return isValid();
    }
    boolean varsOK = (varsDone)? true : setVariables(null, syntaxNode, groups);
    if (varsOK) {
      TreeMap<Integer, ArrayList<String>> treeMap = syntaxNode.getChildsTypes();
      childs = new ArrayList<ExpressionNode>();
      for (int i = 0; i < childstext.size(); i++) {
        int nextrange = treeMap.higherKey(i);
        if(!(valid = nodeMatch(childstext.get(i), syntaxNode, childs, treeMap.get(nextrange)))) {
          //message += " Expression non valide : " + childstext.get(i);
          break;
        }
      }
    }
    if(valid) setType(syntaxNode.getId());
    return isValid();
  }

  /**
   * Si le groupe a pour nom "child", il est ajouté à la liste childstext, sinon il est placé
   * dans la table "groups"
   * @param matchgroups les groupes déterminés par le matcher
   * @param syntaxNode noeud courant
   * @param groups les noms des groupes du noeud courant
   * @return
   */
  private ArrayList<String> setGroups(String[] matchgroups, SyntaxNode syntaxNode,
          HashMap<String, String> groups) {
    ArrayList<String> childstrings = new ArrayList<String>();
    TreeMap<Integer, ArrayList<String>> groupNames = syntaxNode.getGroupNames();
    if (groupNames != null) {
      Iterator<Integer> ranges = groupNames.keySet().iterator();
      int nextrange = ranges.next();
      ArrayList<String> groupValues = groupNames.get(nextrange);
      for (int i = 0; i < matchgroups.length; i++) {
        if (i == nextrange) { // noeud suivant
          try {
            nextrange = ranges.next();
            groupValues = groupNames.get(nextrange);
          } catch (NoSuchElementException e) {
            //message = "pas d'élément du noeud" + syntaxNode.getId() + " à partir de" + nextrange;
          }
        }
        for (String groupValue : groupValues) {
          if ("child".equals(groupValue)) { // on ajoute le texte d'un descendant
            childstrings.add(matchgroups[i]);
          }
          else { // nouveau nom pour le groupe
            groups.put(groupValue, matchgroups[i]);
            break;
          }
        }
      }
    }
    return childstrings;
  }

  /**
   * retourne une String[] des groupes d'un matcher
   * @param matcher
   * @return liste des groupes
   */
  private String[] getGroups(Matcher matcher) {
    String[] groups = new String[matcher.groupCount()];
    for (int i = 0; i < groups.length; i++) {
      groups[i] = matcher.group(i + 1);
    }
    return groups;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }
  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the model
   */
  public String getModel() {
    return model;
  }

  /**
   * @return the valid
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return the parent
   */
  public ExpressionNode getParent() {
    return parent;
  }

  @Override
  public boolean equals(Object obj) {
    boolean ret = obj instanceof ExpressionNode;
    if(ret) {
      ExpressionNode en = (ExpressionNode) obj;
      if(ret = name.equals(en.name) && getType().equals(en.getType()) && valid == en.valid &&
              childs.size() == en.childs.size()) {
        for (int i = 0; i < childs.size(); i++) {
          if(!childs.get(i).equals(en.childs.get(i))) break;
        }
      }
    }
    return ret;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
    hash = 59 * hash + (this.getType() != null ? this.getType().hashCode() : 0);
    hash = 59 * hash + (this.childs != null ? this.childs.hashCode() : 0);
    hash = 59 * hash + (this.valid ? 1 : 0);
    return hash;
  }


  @Override
  public String toString() {
    String text = name;
    if (!childs.isEmpty()) {
      text = "(" + name;
      for (ExpressionNode child : childs) {
        text += "," + child.toString();
      }
      text += ")";
    }
    return text;
  }

}

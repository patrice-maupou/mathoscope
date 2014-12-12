package root;

import expressions.*;
import static expressions.Expression.*;
import display.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;
import java.util.ArrayList;
import static java.awt.event.KeyEvent.*;

/*
 * ExpressionApplet.java
 *
 * Created on 23 d�cember 2002
 * the code source is distributed under the GPL.
 * Please see http://www.fsf.org/copyleft/gpl.html
 */
/**
 *
 * @author Patrice MAUPOU
 *
 */
public class ExpressionApplet extends java.applet.Applet implements Runnable {

    /**
     * Initializes the applet ExpressionApplet
     */
    @Override
    public void init() {
        initComponents();
        validate();
        GPanel.updateImg();
        introPt = new GPoint(0, 0);
        introPt.drawGObject = false; // cach�
        introPt.PosAngle = Double.NaN; // centr�
        if (!inframe) {
            initParameters();
        }
        setBackground(Color.white);
        userActions = new Actions(this);
        GPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent evt) {
                userActions.mouseClicked(evt);
                GPanelMouseClicked(evt);
            }

            @Override
            public void mousePressed(MouseEvent evt) {
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                userActions.point = evt.getPoint();
                userActions.wake = true;
            }
        });
        GPanel.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent evt) {
                int keycode = GPanelKeyPressed(evt);
                userActions.keyPressed(keycode);
            }
        });
        GPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent evt) {
                userActions.mouseDragged(evt);
            }
        });
        answer = new Expression();
        //[ifframe]
        exercices = new String[0];
        definitions = new ArrayList<Expression>();
        decorations = new ArrayList<Expression>();
        //[endframe]
        Partie = -1;
        cnt_create = new int[]{0};
        Gobjets = new ArrayList<GObject>();
        choice.setVisible(true);
        demo = false;
        String version = System.getProperty("java.version").substring(0, 3);
        VERSION = Double.parseDouble(version);
        active = var("caret");
    }

    @Override
    public void start() {
        //[ifframe]
        error = new MathDialog(mathFrame);
        mathFrame.decorations = decorations;
        Expression fig = getDefinition("figure", mathFrame.getDefinitions());
        initGraph(fig);
        //[endframe]
        symbols = new ArrayList<Expression>();
        Expression a = build(VAR, "a", var("symbol")), b = build(VAR, "b", var("symbol"));
        Expression infini = build(VAR, "infini", var("constant"));
        symbols.add(build(DIV, a, b));
        symbols.add(build(MUL, a, b));
        symbols.add(build(FUNC, "rac", new Expression[]{a}));
        symbols.add(build(EXP, a, b));
        symbols.add(infini);
        symbols.add(build(SUB, ZERO, infini));
        symbols.add(build(SET, "{}", new Expression[]{a}));
        symbsize = symbols.size();
        Tools = new Frame();
        Tools.setResizable(false);
        if (VERSION >= 1.4) {
            Tools.setUndecorated(true); // java 1.4
        }
        Palette = new GeomPanel();
        Palette.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                PaletteMouseClicked(evt.getX(), evt.getY());
            }
        });
        /* utilise java 1.5
         Palette.setPreferredSize(new Dimension(nw*wm, nh*hm)); // Java 5 !
         Tools.add(Palette, java.awt.BorderLayout.CENTER);
         Tools.pack();
         //*/
        //* avec java 1, on doit montrer puis cacher la palette
        Tools.add(Palette, java.awt.BorderLayout.CENTER);
        Tools.pack();
        //*/
        if (!decorations.isEmpty()) {
            setDecorations(decorations);
        } else {
            setTools();
        }
    }

    /**
     * d�marre l'activit� du curseur
     *
     * @param value expression d�crivant le point d'ancrage de l'entr�e, exemple
     * : {point(x,y),label(e,..)}
     * @param pt le point o� l'on �crit l'expression
     * @return vrai si c'est possible
     * @throws Exception
     */
    public boolean startCurseur(Expression value, GPoint pt) throws Exception {
        boolean ret = false;
        if (value.op != SET) {
            return false;
        }
        Expression[] coors = (Expression[]) value.R;
        for (int j = coors.length - 1; j > -1; j--) {
            if (coors[j].L.equals("label")) {
                itlabel = (Expression[]) coors[j].R;
                active.parent = Expression.NULL;
                active.place = Tree.ROOT;
                root = Tree.varsToBox(itlabel[0], active, null, new Numeric(4));
                Tree.setTree(root);
                pt.labelBox = SystemBox.exprBox(root, DISPLAYS, GPanel, pt.fontsize, false, false);
                if (runner == null) {
                    runner = new Thread(this);
                    runner.start();
                }
                ret = true;
                hotspot = pt; // modif 16 novembre 2008
            }
        }
        return ret;
    }

    /**
     * �teint et allume le curseur dans l'expression active
     * TODO : remplacer par un timer
     */
    @Override
    public void run() {
        while (runner == Thread.currentThread()) {
            active.R = (active.R == null) ? "blanc" : null;
            try {
                SystemBox box = ((GPoint) Gobjets.get(activerange)).labelBox;
                box.getActiveBox().negActive();
                GPanel.drawFig(Gobjets, endfigbkg, endfigbkg);
            } catch (Exception exc) {
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
        }
    }

    /**
     * param�tres des exercices : r�gles et d�finitions globales, exercices
     */
    private void initParameters() {
        numero = -1;
        decorations = new ArrayList<Expression>();
        try {
            w = Integer.parseInt(getParameter("width"));
            h = Integer.parseInt(getParameter("height"));
        } catch (Exception e) {
            w = 450;
            h = 300;
        }
        String intro = getParameter("pr�sentation");
        if (intro == null) {
            intro = "Mathoscope";
        }
        String defs = "", decos = "", nxt, txt;
        if ((txt = getParameter("importer")) != null) {
            StringBuffer defsbuf = new StringBuffer();
            StringBuffer decosbuf = new StringBuffer();
            getImports(getCodeBase().toString(), txt, defsbuf, decosbuf, false);
            defs = defsbuf.toString();
            decos = decosbuf.toString();
        }
        if ((nxt = getParameter("d�finitions globales")) != null) {
            defs = (defs.equals("")) ? nxt : defs + "," + nxt;
        }
        if ((nxt = getParameter("d�corations")) != null) {
            decos = (decos.equals("")) ? nxt : decos + "," + nxt;
        }
        try {
            int nbexos = Integer.parseInt(getParameter("nombre d'exercices"));
            exercices = new String[nbexos];
            for (int i = 0; i < nbexos; i++) {
                int k = i + 1;
                exercices[i] = getParameter("Exercice " + k);
            }
        } catch (Exception exc) {
            exercices = new String[0];
            System.out.println("pas d'exercices");
        }
        loadExercices(exercices, intro, defs, decos);
        Object anchor = getParent();
        while (!(anchor instanceof Frame)) {
            anchor = ((Component) anchor).getParent();
        }
        error = new MathDialog((Frame) anchor, true);
    }

    /**
     * Ajoute r�cursivement les importations des fichiers de d�finition
     *
     * @param context
     * @param imports cha�ne des fichiers de d�finitions
     * @param defs tampon des d�finitions globales obtenues
     * @param decos tampon des d�corations obtenues
     * @param defFile
     * @return la liste des importations avec les d�finitions
     */
    public ArrayList<Expression> getImports(String context, String imports,
            StringBuffer defs, StringBuffer decos, boolean defFile) {
        StringTokenizer sk = new StringTokenizer(imports, ",");
        ArrayList<Expression> importdefs = new ArrayList<Expression>(), importsplus = new ArrayList<Expression>();
        while (sk.hasMoreTokens()) {
            String txt = "", next = sk.nextToken(), s5 = context, line, s0 = "", s1 = "", s2 = "";
            Expression e = Expression.var(next);
            try {
                URL url = new URL(context + next);
                BufferedReader bf = new BufferedReader(new InputStreamReader(url.openStream()));
                while ((line = bf.readLine()) != null) {
                    txt = txt + line;
                }
                bf.close();
                int i = txt.indexOf("<importer>");
                if (i != -1) {
                    String impsuppl = txt.substring(i + 10, txt.indexOf("</importer>"));
                    if (impsuppl.length() > 1) {
                        int j = next.lastIndexOf('/');
                        if (j != -1) {
                            s5 = s5 + next.substring(0, j + 1);
                        }
                        importsplus = getImports(s5, impsuppl, defs, decos, false);
                        //[ifframe]
                        for (Expression expression : importsplus) {
                            Expression elem = (Expression) expression;
                            s2 += elem.printout(false) + ",";
                        }
                        //[endframe]
                    }
                }
                i = txt.indexOf("<definitions>");
                if (i != -1) {
                    s0 = txt.substring(i + 13, txt.indexOf("</definitions>"));
                    if (defs.length() != 0) {
                        defs.append(",");
                    }
                    defs.append(s0);
                }
                i = txt.indexOf("<decorations>");
                if (i != -1) {
                    s1 = txt.substring(i + 13, txt.indexOf("</decorations>"));
                    if (decos.length() != 0) {
                        decos.append(",");
                    }
                    decos.append(s1);
                }
                //[ifframe]
                try {
                    e.R = Parser.parse(s2 + s0 + s1);
                    i = txt.indexOf("<commentaires>");
                    if (i != -1) {
                        s0 = txt.substring(i + 14, txt.indexOf("</commentaires>"));
                        Expression[] list = (Expression[]) Parser.parse(s0).R;
                        mathFrame.comments = new ArrayList<Expression>();
                        for (Expression list1 : list) {
                            Expression f = (Expression) list1.L;
                            f.R = (Expression) list1.R;
                            mathFrame.comments.add(f);
                        }
                    }
                } catch (Exception ex) {
                }
                importdefs.add(e);
                //[endframe]
            } catch (FileNotFoundException fnf) {
                System.out.println(next + " : " + fnf.getMessage());
            } catch (IOException io) {
                System.out.println(next + " : " + io.getMessage());
            }
        }
        return (defFile) ? importsplus : importdefs;
    }

    /**
     * This method is called from within the init() method to initialize the
     * form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        actions = new Panel();
        note = new Label();
        choice = new Choice();
        GPanel = new GeomPanel();

        setLayout(new BorderLayout());

        note.setText("Note :                    ");
        actions.add(note);

        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                choiceItemStateChanged(evt);
            }
        });
        actions.add(choice);

        add(actions, BorderLayout.SOUTH);
        add(GPanel, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * S
     * Dessine les symboles dans le tampon de Palette
     */
    public void setTools() {
        nw = (int) Math.ceil(Math.sqrt(symbols.size()));
        if (nw < 5) {
            nw = 5;
        }
        try {
            ArrayList<Box> glyphs = new ArrayList<Box>();
            int width = 0, before = 0;
            for (int i = 0; i < symbols.size(); i++) {
                SystemBox box = SystemBox.exprBox(symbols.get(i), DISPLAYS, GPanel, 12, false, false);
                if (box.getBoxMetrics().width > wm) {
                    wm = box.getBoxMetrics().width;
                }
                if (box.getBoxMetrics().height > hm) {
                    hm = box.getBoxMetrics().height;
                }
                glyphs.add(box);
                width += wm;
                if (width > 200) {
                    nw = Math.min(nw, i - before);
                    before = i;
                    width = 0;
                }
            }
            nh = (int) Math.ceil((double) symbols.size() / (double) nw);
            int dh = Tools.getInsets().top + Tools.getInsets().bottom;
            int dw = Tools.getInsets().left + Tools.getInsets().right;
            Tools.setSize(nw * wm + dw, nh * hm + dh);
            Tools.setLocation(-10000, 0); // bidon
            Tools.setVisible(true); // obligatoire pour obtenir l'image
            Palette.updateImg();
            Tools.setVisible(false);
            Graphics g = Palette.getImg().getGraphics();
            for (int i = 0; i < symbols.size(); i++) {
                SystemBox box = (SystemBox) glyphs.get(i);
                BoxMetrics BM = box.getBoxMetrics();
                int x = i % nw, y = i / nw;
                box.paint(g, BM, x * wm + (wm - BM.width) / 2, y * hm + (hm - BM.height) / 2);
                g.drawLine((i + 1) * wm, 0, (i + 1) * wm, nh * hm); // ligne verticale
                g.drawLine(0, y * hm, (nw + 1) * wm, y * hm); // ligne horizontale
            }
            Palette.repaint();
        } catch (Exception exc) {
            exc.getMessage();
        }

    }

    /**
     * affiche le symbole de la palette trouv� en x et y
     *
     * @param x
     * @param y
     */
    public void PaletteMouseClicked(int x, int y) {
        int i = x / wm, j = y / hm; // arrondis pour la grille
        int n = i % nw + j * nw;
        if (n < symbols.size()) {
            Expression symbol = symbols.get(n);
            Expression old = root.copy(), p = active.parent;
            if (p != null && p.op == JUX) { // <expr>|
                Numeric tst = (active.place == Tree.LEFT) ? new Numeric(7) : new Numeric(2);
                Expression pre = (active.place == Tree.LEFT) ? (Expression) p.R : (Expression) p.L;
                symbol = Tree.varsToBox(symbol, active, pre, tst);
                int t = tst.toInt();
                if (t % 5 == 2) {
                    return;
                }
                if (t % 5 == 1) {
                    symbol = build(JUX, symbol, active);
                }
                Tree.substitute(p, symbol);
            } else {
                Numeric tst = new Numeric(1);
                symbol = Tree.varsToBox(symbol, active, null, tst);
                if (tst.toInt() == 1) {
                    symbol = build(JUX, symbol, active);
                }
                Tree.substitute(active, symbol);
            }
            root = upTree(old);
        }
        Tools.setVisible(false);
        GPanel.requestFocus();
    }

    /**
     * choix d'un exercice
     */
	private void choiceItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_choiceItemStateChanged
            initExercise();
	}//GEN-LAST:event_choiceItemStateChanged

    /**
     * S�lectionne un nouvel exercice d'apr�s la bo�te de choix (choice)
     */
    public void initExercise() {
        Partie = 0;
        Total = 0;
        Note = 0;
        numero = choice.getSelectedIndex();
        try {
            Parameters = setExercice(numero);
            nbParties = Parameters.size();
            //[ifframe]
            if (cnt_create.length != 0 && cnt_create[0] < definitions.size()) {
                definitions.subList(cnt_create[0], definitions.size()).clear();
            }
            //[endframe]
            cnt_create = new int[2 * nbParties + 1];
            cnt_create[0] = definitions.size(); //  taille initiale des d�finitions globales
            addVar("total", ZERO);
            defsize = addVar("note", ZERO);
        } catch (Exception e) {
            System.out.println("Exception dans les param�tres");
            return;
        }
        nbAnswers = 0;
        exercice(0);
    }

    /**
     * l'entr�e au clavier est dessin�e directement sur le GPanel la liste root
     * contient les parents successifs � partir du premier, active, qui pointe
     * l'�l�ment contenant le caret, l'actualisation se fait dans le thread par
     * la m�thode run. exemples : JUX(caret,32) , SEQ(3,caret,7), caret
     */
    private int GPanelKeyPressed(KeyEvent evt) {
        if (!GPanelActive && !answered) {
            return VK_UNDEFINED; // �v�nement autre que clavier attendu
        }
        int kcode = evt.getKeyCode(), index;
        if (kcode == VK_SHIFT || kcode == VK_CONTROL) {
            return kcode;
        }
        if (kcode == VK_ENTER) {
            try {
                if (answered || tutorial) { // passer � la question suivante
                    nbAnswers++;
                    answered = false;
                    addVar("r�ponse", NULL);
                    Expression Part = compute(getDefinition("partie", definitions));
                    int part = ((Numeric) Part).toInt();
                    if (tutorial) {
                        exercice(part + 1);
                    } else if (part == -1) { // termin�
                        runner = null;
                        if (demo) {
                            exercice(0);
                        } else {
                            drawText(var("Fin de l'exercice"));
                            Partie = -1;
                        }
                    } else if (Partie != part) {// nouvelle question
                        exercice(part);
                    } else { // m�me question, on recommence
                        exprInFig = false;
                        GPanel.updateImg();
                        Gobjets.subList(2, Gobjets.size()).clear(); // on garde introBase et introPt
                        display(false);
                    }
                    kcode = VK_UNDEFINED;
                } else { // examiner la r�ponse donn�e
                    active.R = "blanc";
                    answer = (demo && Partie != -1) ? itlabel[0] : Tree.strip(root);
                    runner = null;
                    if (Partie != -1) {
                        addVar("entr�e", answer);
                        checkAnswer();
                        if (!answered) {
                            display(false);
                        }
                    } //[ifframe]
                    else if (exprInFig) { // pas d'exercice en cours
                        GPoint Gpoint = (GPoint) Gobjets.get(Gobjets.size() - 1);
                        Gpoint.labelBox = SystemBox.exprBox(answer, DISPLAYS, GPanel, Gpoint.fontsize, false, false);
                        GPanel.drawFig(Gobjets, 0, -1);
                        mathFrame.enter.setText(answer.printout(false));
                        mathFrame.setExpression(answer);
                    }
                    //[endframe]
                }
            } catch (Exception exc) {
                System.out.println(exc.getMessage());
                setCursor(Cursor.getDefaultCursor());
                //exc.printStackTrace();
            }
            return kcode;
        }
        if (!GPanelActive) {
            return kcode;
        }
        if (evt.getModifiers() == 2) { // CTRL
            if (kcode == VK_Z) { // undo
                root = save.substitute(var("caret"), var("?"));
                root = Tree.varsToBox(root, active, null, new Numeric(4));
                Tree.setTree(root);
                root = upTree(root);
            }
            return kcode;
        }
        if (kcode == VK_ESCAPE) {
            Gobjets.subList(2, Gobjets.size()).clear();
            display(true);
            return kcode;
        }
        save = root.copy();
        if (Tree.handleKey(kcode, evt.getKeyChar(), active)) {
            root = upTree(save);
        }
        return kcode;
    }

    /**
     * Valide le nouvel arbre expression en reconstuisant � partir de active et
     * change l'affichage du dernier point
     *
     * @param old expression pr�c�dente
     * @return root si l'expression est valide ou old sinon.
     */
    private Expression upTree(Expression old) {
        root = active;
        while (root.place != Tree.ROOT) {
            root = root.parent;
            if (!root.equals(root.copy())) { // reprendre l'expression pr�c�dente
                old = old.substitute(var("caret"), var("?"));
                root = Tree.varsToBox(old, active, null, new Numeric(4));
                Tree.setTree(root);
                break;
            }
        }
        GPoint pt = (GPoint) Gobjets.get(Gobjets.size() - 1);
        Graphics g = GPanel.getImg().getGraphics();
        pt.labelBox = SystemBox.exprBox(root, DISPLAYS, GPanel, pt.fontsize, false, true);
        pt.label = root;
        return root;
    }

    /**
     * la r�ponse est un point cliqu� de la figure
     */
    private void GPanelMouseClicked(java.awt.event.MouseEvent evt) {
        if (GPanelActive) {
            if (evt.getButton() == MouseEvent.BUTTON3) {
                showTools(evt.getX(), evt.getY());
            } else if (runner != null && active.parent != null) { // changer le curseur de place
                Point pt = new Point(evt.getX(), evt.getY());
                int index = -1, imax = -1, size = GPanel.getAreas().size();
                Rectangle r = new Rectangle(), rmax = new Rectangle();
                for (int i = 0; i < size; i++) {
                    Rectangle R = GPanel.getAreas().get(i);
                    if (R.contains(pt) && (r.contains(R) || r.isEmpty())) {
                        index = i;
                        r = R;
                    } else {
                        R.grow(2, 2);
                        if (R.contains(pt) && (R.contains(rmax) || rmax.isEmpty())) {
                            imax = i;
                            rmax = R;
                        }
                    }
                    if (imax != -1) {
                        index = imax;
                    }
                }
                if (index != -1) {
                    r = GPanel.getAreas().get(index);
                    Expression e = GPanel.getExprs().get(index), p = active.parent;
                    Expression space = build(FUNC, "TBOX", new Expression[]{Expression.var(" ")});
                    if (!e.equals(p)) {
                        int nplace = (pt.x - r.x < r.width / 2) ? Tree.LEFT : Tree.RIGHT, place = active.place;
                        if (p.op == JUX) {
                            Tree.deleteNode(p, place);
                        } else if (p.op == SUB && place == Tree.LEFT) {
                            Tree.substitute(active, ZERO);
                        } else {
                            Tree.substitute(active, space);
                        }
                        if (space.equals(e)) {
                            Tree.substitute(e, active);
                        } else {
                            Tree.insertNode(e, active, nplace); // replace le curseur
                        }
                        root = upTree(save);
                    }
                }
            }
        } else if (nbActions != 0) {
            GPoint P = new GPoint(evt.getPoint(), clicsBase);
            addVar("entr�e", build(FUNC, "point", new Expression[]{P.x, P.y}));
            try {
                checkAnswer();
            } catch (Exception exc) {
                setCursor(Cursor.getDefaultCursor());
                System.out.println("erreur dans les tests");
            }
            if (demo && !answered) {
                display(false);
            }
        }
    }

    /**
     * montre la palette de symboles sur le GPanel
     *
     * @param x
     * @param y
     */
    public void showTools(int x, int y) {
        if (GPanelActive) {
            Point P = GPanel.getLocationOnScreen();
            Tools.setLocation(P.x + x, P.y + y);
            Tools.setVisible(true);
        }
    }

    /**
     * V�rification de la r�ponse donn�e et affichage
     *
     * @throws Exception param�tres incorrects
     */
    private void checkAnswer() throws Exception {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        nbActions--;
        addVar("actions", new Numeric(nbActions));
        listDefs += "," + params[CHECK].substring(1, params[CHECK].length() - 1);
        Expression[] defs = (Expression[]) Parser.parse("{" + listDefs + "}").R;
        startdefs = addDefs(defs, startdefs);
        nbActions = ((Numeric) getDefinition("actions", definitions)).toInt();
        answered = (nbActions == 0);
        //[ifframe]
        if (inframe) {
            mathFrame.updateChoiceDefs("", Partie, 2);
        }
        //[endframe]
        answer = getDefinition("r�ponse", definitions);
        if (!GPanelActive) { // apr�s un clic, on affiche la r�ponse
            Gobjets.subList(figsize, Gobjets.size()).clear();
            GPanel.drawFig(addFig(Gobjets, 0, answer, clicsBase), endfigbkg, endfigbkg);
        } else { // on affiche la ou les r�ponses
            Expression[] nlist = new Expression[]{answer};
            if (answer.op == Expression.SEQ && entries.length > 1) {
                nlist = (Expression[]) answer.R;
            }
            for (int i = 0; i < nlist.length; i++) {
                int j = figsize + i;
                if (j < Gobjets.size()) {
                    GPoint point = (GPoint) Gobjets.get(j);
                    point.labelBox = SystemBox.exprBox(nlist[i], DISPLAYS, GPanel, point.fontsize, false, false);
                }
            }
            GPanel.drawFig(Gobjets, endfigbkg, endfigbkg);
        }
        if (!demo && nbActions == 0) {
            try {
                curdef = compute(getDefinition("note", definitions));
                int nNote = ((BigInteger) curdef.L).intValue();
                curdef = compute(getDefinition("total", definitions));
                int nTotal = ((BigInteger) curdef.L).intValue();
                curdef = compute(getDefinition("essai", definitions));
                int trials = ((BigInteger) curdef.L).intValue();
                trials++;
                addVar("essai", new Numeric(trials));
                addVar("total", new Numeric(nTotal));
                Note = nNote;
                Total = nTotal;
                note.setText("Note : " + Note + "/" + Total);
                note.repaint();
            } catch (Exception ex) {
                System.out.println("D�finition manquante ou calcul impossible");
            }
        }
        setCursor(Cursor.getDefaultCursor());
        drawText(getDefinition("texte", definitions));
    }

    /**
     * les param�tres de l'exercice se trouvent dans l'applet
     *
     * @param numero le rang de l'exercice compt� � partir de 0
     * @throws Exception si le nombre de questions est mal format�
     * @return la liste des parties de l'exercice de rang n
     */
    public ArrayList<String[]> setExercice(int numero) throws Exception {
        ArrayList<String[]> sections = new ArrayList<String[]>();
        String textExo = exercices[numero].replace('\r', ' ').replace('\n', ' ');
        title = textSelect("titre : ", textExo);
        int k = textExo.indexOf("�nonc� :"), start = 0, end;
        while (k != -1) {
            start += k; // d�but de la partie actuelle
            textExo = textExo.substring(k); // �nonc� : ...
            k = textExo.indexOf("�nonc� :", 7); // d�but de la partie suivante
            String part = (k == -1) ? textExo : textExo.substring(0, k);
            end = start + part.length();
            String[] elements = {"", "", "", ""};
            elements[INTRO] = textSelect("�nonc� :", part);
            elements[DEFS] = "{" + textSelect("d�finitions :", part) + "}";
            elements[CHECK] = "{" + textSelect("v�rification :", part) + "}";
            elements[MARKS] = start + " " + end; // d�but et fin de la partie
            sections.add(elements);
        }
        return sections;
    }

    /**
     * retourne la sous-cha�ne de source commen�ant apr�s startlabel et se
     * terminant par le tilde ~
     */
    private String textSelect(String startlabel, String source) {
        String s = "";
        source = source.replace('�', '\n');
        int i0 = source.indexOf(startlabel);
        int i1 = source.indexOf("~", i0);
        if (i0 != -1 && i1 != -1) {
            s = source.substring(i0 + startlabel.length(), i1);
        }
        return s;
    }

    /**
     * pr�sente l'exercice suivant
     *
     * @param part le num�ro de la question (� partir de 0)
     */
    public void exercice(int part) {
        userActions.anim = null;
        setCursor(Cursor.getPredefinedCursor(0));
        definitions0 = new ArrayList<Expression>();
        Partie = part % nbParties;
        nbActions = 0;
        endfigbkg = -1;
        activerange = -1;
        int index = definitions.indexOf(Expression.var("format"));
        if (index != -1) {
            definitions.remove(index);
        }
        index = definitions.indexOf(Expression.var("texte"));
        if (index != -1) {
            definitions.remove(index);
        }
        Trials = 0;
        nbclics = 0;
        try {
            params = Parameters.get(Partie);
        } catch (IndexOutOfBoundsException ex) {
            ex.getMessage();
            return;
        }
        if (Partie == 0) {
            if (defsize < definitions.size()) {
                definitions.subList(defsize, definitions.size()).clear();
            }
            listDefs = "";
            startdefs = 0;
            for (int i = 0; i < cnt_create.length; i++) {
                cnt_create[i] = defsize - 2;
            }
            //[ifframe]
            if (inframe) {
                mathFrame.initDefinitions();
            }
            //[endframe]
        }
        addVar("essai", ZERO);
        addVar("clics", ZERO);
        addVar("actions", ZERO);
        addVar("partie", new Numeric(Partie));
        addVar("entr�es", new Numeric(nbAnswers));
        addVar("figure", NULL);
        tutorial = params[CHECK].equals("{}");
        try {
            addVar("texte", Parser.parseText(params[INTRO]));
            if (!params[DEFS].equals("{}")) { // d�finitions
                if (!listDefs.equals("")) {
                    listDefs += ",";
                }
                listDefs += params[DEFS].substring(1, params[DEFS].length() - 1);
                Expression[] defs = (Expression[]) Parser.parse("{" + listDefs + "}").R;
                //[ifframe]
                if (inframe) {
                    for (int i = startdefs; i < defs.length; i++) {
                        mathFrame.addDefinition(defs[i], 2 * Partie, "");
                    }
                }
                //[endframe]
                startdefs = addDefs(defs, startdefs);
                definitions0.addAll(definitions);
            }
            GPanel.updateImg();
            Expression txt = getDefinition("texte", definitions);
            SystemBox introbox = SystemBox.exprBox(txt, DISPLAYS, GPanel, 12, false, false);
            introPt.labelBox = introbox;
            //[ifframe]
            if (inframe) {
                mathFrame.updateChoiceDefs("", Partie, 1);
                mathFrame.textArea.setText(params[INTRO]);
            }
            //[endframe]
            Expression cliptxt = getDefinition("cliptexte", definitions); // clip(0,0,w,h)
            if (Partie == 0) {// On d�finit le clip pour tout l'exercice
                Gobjets = new ArrayList<GObject>();
                if (cliptxt != Expression.NULL) {
                    Gobjets = addFig(Gobjets, 0, cliptxt, GPanel.base);
                } else {
                    introBase = new GBase(new Rectangle(0, 0, GPanel.getWidth(), introbox.getBoxMetrics().height), 0);
                    Gobjets.add(introBase);
                }
                introPt.base = (GBase) Gobjets.get(0);
                textheight = introPt.base.clip.height;
                Gobjets.add(introPt);
            } else {
                Gobjets.subList(2, Gobjets.size()).clear();
            }
            answer = getDefinition("demo", definitions);
            if (answer instanceof Relation) {
                tutorial = ((Relation) answer).valueOf(definitions, startdefs, 1) && !demo;
            }
            if (!params[CHECK].equals("{}")) { // �valuations � afficher
                Expression[] defs = (Expression[]) Parser.parse(params[CHECK]).R;
                //[ifframe]
                for (Expression def : defs) {
                    mathFrame.addDefinition(def, 2 * Partie + 1, "");
                }
                //[endframe]
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("Exception dans les param�tres, r�appuyer dans la bo�te d'entr�e");
            initGraph(null);
            //[ifframe]
            mathFrame.textArea.setText("Exception dans les param�tres, r�appuyer dans la bo�te d'entr�e"
                    + "\nsur le m�me exercice");
      //[endframe]
            //e.printStackTrace();
            return;
        }
        if (demo) {
            note.setText("Test            ");
        } else if (tutorial) {
            note.setText("appuyer sur entr�e");
        } else if (Total == 0) {
            note.setText("Note :          ");
        }
        exprInFig = false;
        runner = null;
        form = getDefinition("format", definitions).copy();
        entries = (form.op == Expression.SEQ) ? (Expression[]) form.R : new Expression[]{form};
        addVar("r�ponse", Expression.NULL);
        display(false);
    }

    /**
     * affichage de l'exercice : expressions et figure, solution, question,
     * etc...
     *
     * @param first vrai pour le d�but de la question
     */
    public void display(boolean first) {
        try {
            if (entries != null && (nbActions == 0 || first)) {// figure d'origine
                userActions.spots.clear();
                userActions.spotsdefs.clear();
                userActions.startdefs.clear();
                userActions.stopcond.clear();
                userActions.anim = null;
                Rectangle R = GPanel.base.clip;
                R.height -= textheight;
                lastBase = new GBase(R, textheight);
                if (first) {
                    hotspot = null;
                }
                Gobjets = addFig(Gobjets, textheight, getDefinition("figure", definitions), lastBase);
                if (endfigbkg == -1) {
                    endfigbkg = Gobjets.size();
                }
                Gobjets = addFig(Gobjets, textheight, getDefinition("interactions", definitions), lastBase);
                figsize = Gobjets.size();
                GPanel.drawFig(Gobjets, 0, endfigbkg);
                GPanelActive = (nbclics == 0 && entries.length > 0);
                nbActions = (nbclics != 0) ? nbclics : entries.length; // on recharge
                addVar("actions", new Numeric(nbActions));
                int index = userActions.spotsdefs.indexOf(var("ANIMER"));
                if (index != -1 && userActions.anim == null) {
                    userActions.anim = new Thread(userActions);
                    userActions.anim.start();
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("Erreurs dans la d�finition de la figure");
            initGraph(null);
            //[ifframe]
            mathFrame.textArea.setText("Erreurs dans la d�finition de la figure");
            //[endframe]
            return;
        }
        //[ifframe]
        mathFrame.textArea.repaint();
        //[endframe]
        answered = false;
        Expression solution = getDefinition("solution", definitions);
        if (entries.length > 1 || !GPanelActive && solution.op == Expression.SEQ) {
            solutions = (Expression[]) solution.R;
        } else {
            solutions = new Expression[]{solution};
        }
        GPanel.requestFocus();
        if (!GPanelActive) { // clics
            try {
                if (tutorial) {
                    nbclics = 0;
                    nbActions = 0;
                    GPanel.drawFig(addFig(Gobjets, 0, solution, lastBase), figsize, -1);
                } else if (demo) {
                    Gobjets = addFig(Gobjets, 0, solutions[nbclics - nbActions], lastBase);
                    GPanel.drawFig(Gobjets, figsize, -1);
                }
            } catch (Exception exc) {
                System.out.println(exc.getMessage());
            }
            answered = tutorial;
        } else if (Partie > -1) {
            for (int k = 0; k < entries.length; k++) {
                form = entries[k].copy();
                if (form != Expression.NULL) { // entr�e au clavier en un point donn�
                    try {
                        Expression[] items = (Expression[]) form.R; // {point(x,y), cach�, label(exp,..)}
                        for (int i = items.length - 1; i > -1; i--) {
                            if (items[i].L.equals("label")) {
                                itlabel = (Expression[]) items[i].R;
                                break;
                            }
                        }
                        if (tutorial) {
                            itlabel[0] = solutions[k];
                            GPanel.drawFig(addFig(Gobjets, 0, form, lastBase), figsize, -1);
                        } else if (k == entries.length - nbActions) {
                            activerange = Gobjets.size();
                            if (demo) {
                                itlabel[0] = solutions[k];
                            } else {
                                active.parent = Expression.NULL;
                                active.place = Tree.ROOT;
                                root = Tree.varsToBox(itlabel[0], active, null, new Numeric(4));
                                Tree.setTree(root);
                                itlabel[0] = root;
                            }
                            addFig(Gobjets, 0, form, lastBase); // expression suivante � compl�ter
                            if (demo) {
                                GPanel.drawFig(Gobjets, Gobjets.size() - 1, -1);
                            } else if (runner == null) {
                                runner = new Thread(this);
                                runner.start();
                            }
                            break;
                        }
                    } catch (Exception exc) {
                    }
                }
            }
        }
        //[ifframe]
        if (inframe) {
            mathFrame.validate();
        }
    }

    /**
     * Etablit les d�corations graphiques � partir d'un Vecteur de d�finitions
     *
     * @param decorations liste de variables contenant un objet graphique
     */
    public void setDecorations(ArrayList<Expression> decorations) {
        Expression.decofigs = new ArrayList<Expression>();
        ArrayList<Expression> vdisplays = new ArrayList<Expression>(), vars = new ArrayList<Expression>();
        for (Expression decoration : decorations) {
            ArrayList<GObject> fig = new ArrayList<GObject>();
            Expression name = decoration.copy();
            Expression def;
            try {
                if (name.op == SET && (name.L instanceof Expression)) {// {F(x)->image : HBOX..}
                    Expression[] defs = (Expression[]) name.R;
                    for (Expression def1 : defs) {
                        markVars(def1);
                        if (var("image").equals(def1.R)) {
                            def = (Expression) def1.L;
                            break;
                        }
                    }
                    vdisplays.add(name);
                } else if (name.op == Expression.VAR) { // il faut une variable pour un �l�ment graphique
                    Expression deco = ((Expression) name.R).applyDefs(definitions, 0, 1, null);
                    Expression list[], elems[];
                    if (deco.op == SEQ) {
                        list = (Expression[]) deco.R;
                    } else {
                        list = (new Expression[]{deco});
                    }
                    for (Expression list1 : list) {
                        // liste d'instructions graphiques
                        if (list1.op == SET) {
                            elems = (Expression[]) list1.R;
                        } else {
                            elems = (new Expression[]{list1});
                        }
                        Object obj = createGObject(elems[0], null); // FIXME : base ?
                        GObject gobject = (GObject) obj;
                        GrOptions(elems, gobject);
                        fig.add(gobject);
                    }
                    name.R = fig; // l'�l�ment graphique dans la variable
                    decofigs.add(name);
                }
            } catch (Exception exc) {
                System.out.println("d�coration erron�e");
            }
        }
        Expression[] displays = vdisplays.toArray(new expressions.Expression[0]);
        DISPLAYS = build(FUNC, "sch�ma", displays);
        symbols.subList(symbsize, symbols.size()).clear();
        if (displays != null) { // ajoute les nouveaux symboles
            for (Expression display1 : displays) {
                boolean visible = true;
                Expression display = (Expression) display1.L; // l'expression transform�e
                if ("STOP".equals(display.L)) {
                    display = ((Expression[]) display.R)[0];
                }
                if ("INVISIBLE".equals(display.L)) {
                    visible = false;
                }
                Expression[] displaydef = (Expression[]) display1.R;
                for (Expression displaydef1 : displaydef) {
                    if (displaydef1.R.equals(Expression.var("image"))) {
                        Expression symbol = (Expression) displaydef1.L; // l'expression initiale
                        if (visible) {
                            symbols.add(symbol);
                        }
                        //display = build(IS, display, var("image"));
                        break;
                    }
                }
            }
            setTools();
        }
    }

    /**
     *
     * @param e chaque variable de list doit �tre marqu�e � droite "symbol"
     * @param list liste des variables � marquer
     * @return l'expression marqu�e
     */
    private static void markVars(Expression e) {
        Expression ret = e;
        if (ret.op == VAR && ret.R == null) {
            ret.R = var("symbol");
        } else {
            if (ret.L instanceof Expression) {
                markVars((Expression) ret.L);
            }
            if (ret.R instanceof Expression) {
                markVars((Expression) ret.R);
            } else if (ret.R instanceof Expression[]) {
                Expression[] coors = (Expression[]) ret.R;
                for (Expression coor : coors) {
                    markVars(coor);
                }
            }
        }
    }

    /**
     * ajoute une liste d'objets graphiques d�finie par l'expression fig � une
     * liste
     *
     * @param Gobjs la liste des objets g�om�triques
     * @param shift d�calage vertical du � la place de l'�nonc�
     * @param fig l'expression d�crivant la figure
     * @param base le cadre de r�f�rence initial
     * @return la liste des objets graphiques
     */
    private ArrayList<GObject> addFig(ArrayList<GObject> gobjs, int shift, Expression fig, GBase base)
            throws Exception {
        if (fig.equals(Expression.NULL)) {
            return gobjs;
        }
        if (base != null) {
            lastBase = base;
        }
        Expression[] elems = new Expression[]{fig}, grElems;
        if (fig.op == SEQ) {
            elems = (Expression[]) fig.R;
        }
        for (Expression elem : elems) {
            GObject GObj = null;
            Object type = elem.L;
            if (elem.op == SEQ) {
                //s�quence de s�quences, il faut une r�cursivit�
                gobjs = addFig(gobjs, shift, elem, lastBase);
            } else if (type.equals("cliquer")) {
                clicsBase = lastBase;
                grElems = (Expression[]) elem.R;
                addVar("clics", grElems[0]);
                nbclics = ((BigInteger) grElems[0].L).intValue();
            } else if (type.equals("clip")) {
                // nouveau cadre
                grElems = (Expression[]) elem.R;
                lastBase = new GBase(grElems, shift, new Rectangle(GPanel.getSize())); // nouvelle base
                for (int k = 4; k < grElems.length; k++) { // options
                    if (grElems[k].L.equals("recadre")) {
                        lastBase.setScale(((Expression[]) grElems[k].R)[0]);
                    } else if (grElems[k].L.equals("origine")) {
                        lastBase.setOrigin((Expression[]) grElems[k].R, shift);
                    } else if (grElems[k].L.equals("unit�s")) {
                        lastBase.setUnits((Expression[]) grElems[k].R);
                    } else if (grElems[k].L.equals("couleur")) // int�grer la couleur de fond
                    {
                        lastBase.setFillColor((Expression[]) grElems[k].R);
                    }
                }
                gobjs.add(lastBase);
            } else if (type.equals("FIN")) {
                // fin des interactions li�es � plusieurs objets
                Expression[] coors = (Expression[]) elem.R;
                for (Expression coor : coors) {
                    String objname = (String) coor.L;
                    userActions.enddefs.put(objname, userActions.spotsdefs.size());
                }
            } else if (type.equals("INIT")) {
                userActions.spotsdefs.add(build(VAR, type, var("init")));
                userActions.startdefs.put("init", userActions.spotsdefs.size());
                Expression[] coors = (Expression[]) elem.R;
                userActions.stopcond.put("init", coors[0]);
            } else if (("CLIQUER".equals(type) || "PRESSER".equals(type) || "ANIMER".equals(type))
                    && elem.op == FUNC) {
                Expression[] coors = (Expression[]) elem.R;
                String objname = (String) coors[0].L;
                Expression value = coors[1].applyDefs(definitions, 0, 50, null); // valeur initiale
                addVar(objname, value);
                userActions.spotsdefs.add(build(VAR, type, coors[0]));
                userActions.startdefs.put(objname, userActions.spotsdefs.size());
                if ("CLIQUER".equals(type)) {
                    clicsBase = lastBase;
                }
                if ("ANIMER".equals(type)) {
                    userActions.pause = ((Numeric) coors[3]).toInt();
                }
                if (coors.length >= 3) { // condition d'arr�t
                    userActions.stopcond.put(objname, coors[2]);
                }
            } else {
                if ("TRAINER".equals(type) && elem.op == FUNC) {
                    Expression[] coors = (Expression[]) elem.R;
                    String objname = (String) coors[0].L;
                    Expression value = coors[1].applyDefs(definitions, 0, 50, null); // valeur initiale
                    addVar(objname, value);
                    GObj = createGObject(value, lastBase);
                    if (GObj != null) {
                        userActions.spots.put(objname, gobjs.size()); // son rang dans la figure
                        GObj.type = 1;
                    }
                } else if (elem.op == IS) {
                    // variable modifiable ou suite
                    Expression var = (Expression) type;
                    userActions.spotsdefs.add(elem); // on ajoute la d�finition aux interactions
                    elem.define(definitions, true);
                    if (var.op == VAR) {
                        String name = (String) var.L;
                        Expression value = getDefinition(name, definitions);
                        try {
                            Integer place = userActions.spots.get(name);
                            if (place == null) { // cr�ation �ventuelle d'un objet graphique
                                GObj = createGObject(value, lastBase);
                                if (GObj != null) {
                                    userActions.spots.put(name, gobjs.size());
                                }
                            } else if (GObj != null) {
                                GObj.type = gobjs.get(place).type;
                                gobjs.set(place, GObj);
                                continue;
                            }
                        } catch (Exception exception) {
                            GObj = null;
                        }
                    }
                } else {
                    GObj = createGObject(elem, lastBase);
                }
                if (GObj != null) {
                    GObj.setView();
                    gobjs.add(GObj);
                }
            }
        }
        return gobjs;
    }

    /**
     * cr�e un objet graphique � partir d'une expression point(-3,4) etc..
     *
     * @param GrElem l'expression � transformer
     * @param base
     * @return l'objet graphique ou null
     * @throws java.lang.Exception
     */
    public GObject createGObject(Expression GrElem, GBase base) throws Exception {
        Expression GrEl = (GrElem.op == SET) ? ((Expression[]) GrElem.R)[0] : GrElem;
        GObject GObj = null;
        if (GrEl.op == VAR) {
            Integer place = userActions.spots.get((String) GrEl.L);
            if (place != null) {
                return (GPoint) Gobjets.get(place);
            }
        } else if (GrEl.op == FUNC) {
            String id = (String) GrEl.L;
            Expression[] coors = (Expression[]) GrEl.R;
            /* DEBUG
             for (int i = 0; i < coors.length; i++) {
             System.out.println(coors[i]);
             }
             //*/
            try {
                if (id.equals("point")) { // point(2,3)
                    coors = (Expression[]) compute(GrEl).R;
                    GObj = new GPoint((Numeric) coors[0], (Numeric) coors[1]);
                } else if (id.equals("ligne") || id.equals("polygone")) {
                    if (coors.length == 1 && coors[0].op == Expression.SEQ) {
                        coors = (Expression[]) coors[0].R;
                    }
                    GPoint[] points = new GPoint[coors.length];
                    for (int i = 0; i < coors.length; i++) {
                        points[i] = getGPoint(coors[i], base);
                    }
                    GObj = new GPolyLine(points, id.equals("polygone"));
                } else if (id.equals("interLL")) {
                    GLine D1 = (GLine) createGObject(coors[0], base), D2 = (GLine) createGObject(coors[1], base);
                    GObj = new GPoint(D1, D2);
                } else if (id.equals("droite")) {
                    //* avant modif 3 ao�t 2007
                    GObj = new GLine(getGPoint(coors[0], base), getGPoint(coors[1], base));
          //*/
            /* modif 3 ao�t 2007
                     GPoint PA = (GPoint)createGObject(coors[0], base), PB = (GPoint)createGObject(coors[0], base);
                     GObj = new GLine(PA, PB);
                     //*/
                } else if (id.equals("cercle") || id.equals("ellipse") || id.equals("arc")) {
                    GPoint C = getGPoint(coors[0], base); // centre
                    Numeric ra = (Numeric) coors[1]; // rayon x
                    Numeric rb = id.equals("cercle") ? ra : (Numeric) coors[2]; // rayon y
                    GObj = new GOval(C, ra, rb);
                    if (id.equals("arc")) { // angle d�part, angle de trac�
                        int sa = ((Numeric) coors[3]).toInt();
                        int sb = ((Numeric) coors[4]).toInt();
                        GObj = new GOval(C, ra, rb, sa, sb);
                    }
                }
            } catch (Exception exc) {
                System.out.println("createGObject: " + id + " " + exc.getMessage());
            }
        }
        if (GObj != null) {
            GObj.base = base;
            if (GrElem.op == SET) {
                GrOptions((Expression[]) GrElem.R, GObj);
            }
        }
        return GObj;
    }

    /**
     * attributs d'un objet graphique
     *
     * @param options la liste des options
     * @param GObj l'objet graphique
     * @throws Exception
     */
    private void GrOptions(Expression[] options, GObject GObj) throws Exception {
        for (int i = 1; i < options.length; i++) { // on �carte l'index 0
            if ("cach�".equals(options[i].L)) {
                GObj.drawGObject = false;
            } else if ("visible".equals(options[i].L)) {
                GObj.drawGObject = true;
            } else if (options[i].op == FUNC) {
                Expression[] values = (Expression[]) options[i].R;
                if ("label".equals(options[i].L) && GObj instanceof GPoint) {
                    GPoint pt = (GPoint) GObj;
                    pt.label = values[0];
                    if (values.length >= 2) { // position donn�e par un angle
                        if (values[1].equals(var("centr�"))) {
                            pt.PosAngle = Double.NaN;
                        } else {
                            pt.PosAngle = ((Numeric) values[1]).toDouble();
                        }
                    }
                    if (values.length >= 3 && values[2].op == INT) { // taille fonte
                        pt.fontsize = ((BigInteger) values[2].L).intValue();
                    }
                    pt.labelBox = SystemBox.exprBox(values[0], DISPLAYS, GPanel, pt.fontsize, false, false);
                } else if ("remplit".equals(options[i].L) && GObj.closed && values.length == 3) {
                    int[] ns = Numeric.getInts(values);
                    GObj.fillcolor = new Color(ns[0], ns[1], ns[2]);
                } else if ("style".equals(options[i].L)) { // pour un point ou une droite
                    if (values[0].equals(var("nul"))) {
                        GObj.symbol = null;
                    } else {
                        GObj.symbol = addFig(new ArrayList<GObject>(), 0, values[0], lastBase); // base discutable
                    }
                } else if ("couleur".equals(options[i].L) && values.length == 3) { // couleur(rouge)
                    int[] ns = Numeric.getInts(values);
                    GObj.setColor(new Color(ns[0], ns[1], ns[2]));
                } else if ("graduations".equals(options[i].L)) {
                    Numeric n = (Numeric) values[0];
                    double sc = Math.abs(n.toDouble());
                    ((GLine) GObj).setScale((int) sc);
                } else if ("sepA".equals(options[i].L)) {
                    double sep = ((Numeric) values[0]).toDouble();
                    ((GLine) GObj).setSepA((float) sep);
                } else if ("sepB".equals(options[i].L)) {
                    double sep = ((Numeric) values[0]).toDouble();
                    ((GLine) GObj).setSepB((float) sep);
                }
            }
        }
    }

    /**
     * retourne un GPoint, exemple : {point(2,3),label(A,45,12)}
     *
     * @param e l'expression d�crivant le point
     * @return le point g�om�trique
     * @throws Exception si la description est erron�e
     */
    private GPoint getGPoint(Expression e, GBase base) throws Exception {
        GPoint ret;
        if (e.op == VAR) {
            Integer place = userActions.spots.get((String) e.L);
            if (place != null) {
                return (GPoint) Gobjets.get(place);
            }
        }
        Expression[] coors = (Expression[]) e.R;
        if (e.op == Expression.SET) {
            ret = (GPoint) createGObject(coors[0], base);
            GrOptions(coors, ret);
        } else {
            ret = (GPoint) createGObject(e, base);
            ret.drawGObject = false; // cach� par d�faut � cause de Polyline
        }
        ret.base = base;
        return ret;
    }

    /**
     * ajoute et met � jour des d�finitions dans le ArrayList definitions
     *
     * @param listdefs la liste des expressions � ajouter
     * @throws Exception si l'une des d�finitions est incorrecte
     */
    private int addDefs(Expression[] defs, int start) throws Exception {
        for (int i = start; i < defs.length; i++) {
            defs[i].define(definitions, true); // les choix sont faits
        }
        return defs.length;
    }

    /**
     * ajoute ou remplace une variable dans definitions (� la place de addObj)
     *
     * @param name le nom de la variable
     * @param def la d�finition de remplacement de la variable
     * @return la taille de la liste
     */
    protected int addVar(String name, Expression def) {
        Expression var = Expression.build(Expression.VAR, name, def);
        int n = definitions.indexOf(var);
        if (n == -1) {
            definitions.add(var);
        } else {
            definitions.set(n, var);
        }
        return definitions.size();
    }

    /**
     * met � jour le vecteur definitions par rapport au vecteur defs
     * (d�finitions globales) de MathFrame
     *
     * @param defs
     * @param globalsize
     */
//[ifframe]
    public void updateDefinitions(AbstractList<Expression> defs, int globalsize) {
        if (!inframe) {
            return;
        }
        for (int i = 0; i < globalsize; i++) // on enl�ve les anciennes
        {
            definitions.remove(0);
        }
        for (int i = 0; i < cnt_create[0]; i++) { // on remet les nouvelles
            definitions.add(i, defs.get(i));
        }
    }
//[endframe]

    /**
     * remise � z�ro des param�tres, pas d'exercice
     */
    public void reset() {
        exercices = new String[0];
        definitions = new ArrayList<Expression>();
        decorations = new ArrayList<Expression>();
        setDecorations(decorations);
        Partie = -1;
        cnt_create = new int[]{0};
        choice.removeAll();
        initGraph(null);
    }

    /**
     * �tablit le context graphique d'origine du GPanel
     *
     * @param figure dessine la figure correspondante
     */
    public void initGraph(Expression figure) {
        Gobjets = new ArrayList<GObject>();
        if (runner != null) {
            runner = null;
        }
        GPanel.updateImg();
        Gobjets.add(GPanel.base);
        if (figure != null) {
            try {
                Gobjets = addFig(Gobjets, 0, figure, GPanel.base);
            } catch (Exception ex) {
                ex.getMessage();
            }
        }
        GPanel.drawFig(Gobjets, 0, -1);
        exprInFig = false;
    }

    /**
     * affiche l'expression e, centr�e au point de coordonn�es (x,y)
     *
     * @param e
     * @param x
     * @param y
     * @param isFigure
     */
    public void drawExpression(Expression e, int x, int y, boolean isFigure) {
        try {
            if (isFigure) {
                Gobjets = addFig(new ArrayList<GObject>(), 0, e, GPanel.base);
            } else {
                Gobjets = new ArrayList<GObject>();
                introPt.labelBox = SystemBox.exprBox(e, DISPLAYS, GPanel, 14, false, true);
                introPt.base = GPanel.base;
                introPt.fontsize = 14;
                Gobjets.add(introPt);
                endfigbkg = Gobjets.size() - 1;
            }
            GPanel.drawFig(Gobjets, 0, -1);
            exprInFig = true;
        } catch (Exception exc) {
            exc.getMessage();
        }
        //[ifframe]
        if (inframe) {
            mathFrame.setExpression(e);
        }
        //[endframe]
    }

    /**
     * dessine le texte dans le bandeau
     *
     * @param e
     */
    protected void drawText(Expression e) {
        introPt.labelBox = SystemBox.exprBox(e, DISPLAYS, GPanel, 12, false, false);
        GPanel.drawFig(Gobjets, 0, endfigbkg);
    }

    /**
     * calcule une Expression
     *
     * @param e l'expression � calculer
     * @return le calcul
     * @throws java.lang.Exception
     */
    public Expression compute(Expression e) throws Exception {
        return functionFactory.compute(new Expression[]{e});
    }

    /**
     * entr�e dans le cas d'une utilisation autonome (java MathFrame w h) o�
     * xdim et ydim sont les dimensions de l'�cran.
     *
     * @param args vide pour le programme complet, "�l�ve" sinon
     */
//[ifframe]
    public static void main(String args[]) {
        w = 600;
        h = 500;
        ExpressionApplet applet = new ExpressionApplet();
        inframe = true;
        boolean hide = args.length == 1 && "�l�ve".equals(args[0]);
        if (hide) {
            h = 300;
        }
        applet.init();
        MathFrame mathFrame = new MathFrame(applet, hide);
        applet.mathFrame = mathFrame;
        mathFrame.add(applet, BorderLayout.CENTER);
        mathFrame.setVisible(true);
        applet.start();
    }

//[endframe]
    /**
     * Remplit la bo�te des choix.
     *
     * @param exercices description des exercices
     * @param intro la pr�sentation de l'applet
     * @param defs les d�finitions gobales
     * @param decos les d�corations
     */
    public void loadExercices(String exercices[], String intro, String defs, String decos) {
        this.exercices = exercices;
        try {
            if (!"".equals(defs)) {
                definitions = new ArrayList<Expression>();
                ArrayList<Expression> mathdefs = new ArrayList<Expression>();
                Expression adefs[] = (Expression[]) Parser.parse("{" + defs + "}").R;
                for (Expression adef : adefs) {
                    if (adef.op == -8) {
                        mathdefs.add(adef);
                    }
                    adef.define(definitions, false);
                }
                if (!mathdefs.isEmpty()) {
                    Expression amathdefs[] = mathdefs.toArray(new expressions.Expression[0]);
                    MATHDEFS = build(FUNC, "applique", amathdefs);
                }
            }
        } catch (Exception exc) {
            System.out.println("erreur dans les d�finitions globales");
        }
        try {
            if (!decos.equals("")) {
                Expression adecos[] = (Expression[]) Parser.parse("{" + decos + "}").R;
                for (Expression deco : adecos) {
                    if (deco.op == -1) {
                        // �l�ment graphique
                        deco = (Expression) deco.L;
                        deco.R = (Expression) deco.R;
                    }
                    int l = decorations.indexOf(deco);
                    if (l == -1) {
                        decorations.add(deco);
                    } else {
                        decorations.set(l, deco);
                    }
                }
                if (inframe) {
                    setDecorations(decorations);
                }
            }
        } catch (Exception exc) {
            System.out.println("erreur dans les d�corations");
        }
        choice.removeAll();
        for (int i = 0; i < exercices.length; i++) {
            StringTokenizer tk = new StringTokenizer(exercices[i], "\r\n");
            exercices[i] = "";
            while (tk.hasMoreTokens()) {
                exercices[i] += tk.nextToken();
            }
            choice.add(textSelect("titre : ", exercices[i]));
        }
        actions.validate();
        if (intro == null || intro.equals("") || !inframe) {
            intro = "Choisir un exercice � l'aide de la bo�te de choix ci-dessous";
        }
        try {
            drawExpression(Parser.parseText(intro), 0, 0, false);
        } catch (Exception ex) {
            System.out.println("erreur dans l'intro");
        }
        Partie = -1;
    }

    /**
     * Getter for property Parameters.
     *
     * @return Value of property Parameters.
     */
    public ArrayList<String[]> getParameters() {
        return Parameters;
    }

    /**
     * Setter for property Parameters.
     *
     * @param Parameters New value of property Parameters.
     */
    public void setParameters(ArrayList<String[]> Parameters) {
        this.Parameters = Parameters;
    }

    /**
     * Getter for property numero.
     *
     * @return le num�ro de l'exercice
     */
    public int getNumero() {
        return numero;
    }

    /**
     * @return le titre de l'exercice en cours
     */
    public String getTitle() {
        return choice.getSelectedItem();
    }

    /**
     * met ou remplace un titre dans la bo�te de choix
     *
     * @param title le titre � placer
     * @param replace remplace l'ancien titre si vrai, ajoute si faux
     */
    public void setTitle(String title, boolean replace) {
        int index = choice.getSelectedIndex();
        if (replace && index != -1) { // changement de titre
            choice.remove(index);
            choice.insert(title, index);
        } else {
            choice.addItem(title);
            choice.select(title);
            initExercise();
        }
        actions.validate();
    }

    /**
     * Getter for property definitions.
     *
     * @return Value of property definitions.
     */
    public ArrayList<Expression> getDefinitions() {
        return definitions;
    }

    /**
     * Setter for property definitions.
     *
     * @param definitions New value of property definitions.
     */
    public void setDefinitions(ArrayList<Expression> definitions) {
        this.definitions = definitions;
    }

    /**
     * @param name le nom de la d�finition
     * @param defs la liste de r�f�rence
     * @return la d�finition ou NULL s'il n'y a rien
     */
    public static Expression getDefinition(String name, ArrayList defs) {
        Expression ret = Expression.NULL, def;
        int index = defs.indexOf(Expression.var(name));
        if (index != -1) {
            def = (Expression) defs.get(index);
            if (def.R != null) {
                ret = (Expression) def.R;
            }
        }
        return ret;
    }

    /**
     * Getter for property cnt_create.
     *
     * @return Value of property cnt_create.
     */
    public int[] getCnt_create() {
        return this.cnt_create;
    }

    /**
     * Setter for property cnt_create.
     *
     * @param cnt_create New value of property cnt_create.
     */
    public void setCnt_create(int[] cnt_create) {
        this.cnt_create = cnt_create;
    }

    /**
     * Getter for property Partie.
     *
     * @return Value of property Partie.
     */
    public int getPartie() {
        return Partie;
    }

    /**
     * Setter for property Partie.
     *
     * @param Partie New value of property Partie.
     */
    public void setPartie(int Partie) {
        this.Partie = Partie;
    }

    /**
     * Getter for property exercices.
     *
     *
     * @return Value of property exercices.
     */
    public java.lang.String[] getExercices() {
        return this.exercices;
    }

    /**
     * Setter for property exercices.
     *
     * @param exercices New value of property exercices.
     */
    public void setExercices(java.lang.String[] exercices) {
        this.exercices = exercices;
    }
//[ifframe]
    protected MathFrame mathFrame;
//[endframe]
    // Variables declaration - do not modify//GEN-BEGIN:variables
    transient GeomPanel GPanel;
    Panel actions;
    Choice choice;
    Label note;
    // End of variables declaration//GEN-END:variables
  MathDialog error;
    public double VERSION;
    private Frame Tools;
    private GeomPanel Palette;
    public static int w, h, wm = 0, hm = 0, nw = 5, nh = 2;
    boolean demo, GPanelActive = false, exprInFig = false, answered, tutorial;
    public static boolean inframe = false;
    String title, listDefs;
    Expression answer, answers, curdef, save, form, root, active;
    public Expression[] itlabel, entries, solutions;
    String[] exercices, params;
    int Note, Total, Trials;
    int Partie, nbParties, nbclics, nbActions, numero, nbAnswers;
    int figsize, symbsize, startdefs, defsize, endfigbkg;
    protected int activerange;
    ArrayList<GObject> Gobjets;
    ArrayList<Expression> definitions;
    ArrayList<String[]> Parameters;
    protected ArrayList<Expression> definitions0, symbols, decorations;
    protected GBase clicsBase, lastBase, introBase;
    protected GPoint introPt, hotspot;
    private int textheight;
    public Expression MATHDEFS, INVDISPLAYS, DISPLAYS;
    public Thread runner;
    private Actions userActions;
    private int[] cnt_create;
    public static final int INTRO = 0;
    public static final int DEFS = 1;
    public static final int CHECK = 2;
    public static final int MARKS = 3;
}

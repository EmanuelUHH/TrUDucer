package nats.truducer.gui;

import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import nats.truducer.data.Rule;
import nats.truducer.deprel.TreeConversionStats;
import nats.truducer.io.CoNLLMatcher;
import nats.truducer.startup.Main;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import static nats.truducer.io.TreeReaderWriter.defaultNodeClassifier;
import static nats.truducer.io.TreeReaderWriter.fileToTree;
import static nats.truducer.io.TreeReaderWriter.pathToTransducer;

public class sessionGUIController implements TreeSelectionListener, ActionListener, MouseListener {
    private sessionGUI gui;
    private DefaultMutableTreeNode root;

    private DefaultMutableTreeNode catRoot;
    private DefaultMutableTreeNode ruleRoot;
    private HashMap<String, DefaultMutableTreeNode> frontiers;
    private HashMap<Rule, DefaultMutableTreeNode> rules;

    private String inPath;
    private String outPath;

    private String transducerPath;

    public sessionGUIController(String inPath, String outPath) {
        createRootTreeNode();
        frontiers = new HashMap<>();
        rules = new HashMap<>();
        this.inPath = inPath;
        this.outPath = outPath;
        this.transducerPath = "sample_rules_hdt.tud";


    }

    public void initWindow() {
        gui = new sessionGUI(this, root);
    }

    public void add(File file, File srcFile, TreeConversionStats stats) {
        stats.check();
        if(stats.getRulesUsed() != null) {
            stats.getRulesUsed().forEach(r -> {
                if(r == null)
                    return;
                DefaultMutableTreeNode rule;
                if(!rules.containsKey(r)) {
                    rule = new DefaultMutableTreeNode(r);
                    rules.put(r, rule);
                    ruleRoot.add(rule);
                } else {
                    rule = rules.get(r);
                }
                rule.add(new DefaultMutableTreeNode(new ConversionResult(file, srcFile, stats)));
            });
        }

        if(!stats.isTreeFullyConverted()) {
            stats.getBlockerNodes().stream().map(Node::getDeprel).distinct().forEach(s -> {
                DefaultMutableTreeNode category;
                if(!frontiers.containsKey(s)) {
                    category = new DefaultMutableTreeNode(s);
                    frontiers.put(s, category);
                    catRoot.add(category);
                } else {
                    category = frontiers.get(s);
                }
                category.add(new DefaultMutableTreeNode(new ConversionResult(file, srcFile, stats)));
            });
        }
    }

    private void createRootTreeNode() {
        root = new DefaultMutableTreeNode("all");
        catRoot = new DefaultMutableTreeNode("failed conversions");
        root.add(catRoot);
        ruleRoot = new DefaultMutableTreeNode("conversions by rule");
        root.add(ruleRoot);
    }

    public void setPercentage(double percentage) {
        catRoot.setUserObject(String.format("%s overall: %2.4f%%", catRoot.getUserObject().toString(), percentage));
    }

    public void setPercentage(String s, double percentage) {
        DefaultMutableTreeNode category = frontiers.get(s);
        if(category != null) {
            category.setUserObject(String.format("%s: %2.4f%%", category.getUserObject().toString(), percentage));
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) gui.tree.getLastSelectedPathComponent();
        if(node == null) {
            // nothing selected
            return;
        }

        if(!node.isLeaf() || !(node.getUserObject() instanceof ConversionResult)) {
            // we can't render categories
            return;
        }

        ConversionResult cr = (ConversionResult) node.getUserObject();
        gui.setTree(fileToTree(cr.getFile()), defaultNodeClassifier);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == gui.setTransducer) {
            JFileChooser chooser = new JFileChooser(".");
            int returnVal = chooser.showOpenDialog(chooser);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    gui.deptreeViewPane.setTransducer(pathToTransducer(f.getPath()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                this.transducerPath = f.getPath();
            }
        }

        else if(e.getSource() == gui.getOriginalTree) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) gui.tree.getLastSelectedPathComponent();
            if(node == null) {
                // nothing selected
                return;
            }

            if(!node.isLeaf() || !(node.getUserObject() instanceof ConversionResult)) {
                // we can't render categories
                return;
            }

            ConversionResult cr = (ConversionResult) node.getUserObject();
            gui.setTree(fileToTree(cr.getSrcFile()), defaultNodeClassifier);
        }

        else if(e.getSource() == gui.convert) {
            gui.frame.setEnabled(false);

            if(gui.reloadTransducer.getState()) {
                try {
                    gui.deptreeViewPane.setTransducer(pathToTransducer(transducerPath));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            createRootTreeNode();
            frontiers.clear();
            rules.clear();
            Main.convertDir(gui.deptreeViewPane.getTransducer(), inPath, outPath, gui.rememberRules.getState(), this);
            ((DefaultTreeModel)gui.treeModel).setRoot(root);
            ((DefaultTreeModel)gui.treeModel).reload(root);
            gui.frame.setEnabled(true);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getSource() == gui.tree) {
            if(!SwingUtilities.isRightMouseButton(e)) {
                return;
            }

            int row = gui.tree.getClosestRowForLocation(e.getX(), e.getY());
            gui.tree.setSelectionRow(row);

            if(!gui.tree.getLastSelectedPathComponent().toString().equals("conversions by rule")) {
                return;
            }

            JPopupMenu menu = new JPopupMenu();
            JMenuItem filterRules = new JMenuItem("filter rules");
            filterRules.addActionListener(a -> {
                String filterText = JOptionPane.showInputDialog(gui.frame, "filter rules for: ");

                ruleRoot.removeAllChildren();

                if(filterText == null || filterText.equals("")) {
                    for(Rule rule: rules.keySet()) {
                        ruleRoot.add(rules.get(rule));
                    }
                } else {
                    for(Rule rule: rules.keySet()) {
                        if(rule.toString().contains(filterText)) {
                            ruleRoot.add(rules.get(rule));
                        }
                    }
                }
                ((DefaultTreeModel)gui.treeModel).reload();

            });
            menu.add(filterRules);

            JMenuItem filterStructure = new JMenuItem("filter structure");
            filterStructure.addActionListener(a -> {
                String filterText = JOptionPane.showInputDialog(gui.frame, "filter sentences for: ");

                if(filterText == null || filterText.equals("")) {
                    ruleRoot.removeAllChildren();
                    for (Rule rule: rules.keySet()) {
                        ruleRoot.add(rules.get(rule));
                    }
                } else {
                    ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();

                    DefaultMutableTreeNode currNode = (DefaultMutableTreeNode) ruleRoot.getFirstChild();
                    while(currNode != null) {
                        Enumeration<TreeNode> children = currNode.children();

                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(currNode.getUserObject().toString());
                        while (children.hasMoreElements()) {
                            DefaultMutableTreeNode currChild = (DefaultMutableTreeNode) children.nextElement();
                            ConversionResult res = (ConversionResult) currChild.getUserObject();

                            if(CoNLLMatcher.checkForMatching(filterText, res.getSrcFile()) || CoNLLMatcher.checkForMatching(filterText, res.getFile())) {
                                newNode.add(new DefaultMutableTreeNode(res));
                            }

                        }
                        if(newNode.getChildCount() > 0)
                            nodes.add(newNode);

                        currNode = (DefaultMutableTreeNode) ruleRoot.getChildAfter(currNode);
                    }

                    ruleRoot.removeAllChildren();

                    for(DefaultMutableTreeNode node: nodes) {
                        ruleRoot.add(node);
                    }
                }
                ((DefaultTreeModel)gui.treeModel).reload();
            });
            menu.add(filterStructure);

            menu.show(e.getComponent(), e.getX(), e.getY());

        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public void setTransducerPath(String transducerPath) {
        this.transducerPath = transducerPath;
        try {
            gui.deptreeViewPane.setTransducer(pathToTransducer(transducerPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

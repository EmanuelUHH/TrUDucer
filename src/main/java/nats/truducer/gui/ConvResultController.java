package nats.truducer.gui;

import cz.ufal.udapi.core.Node;
import nats.truducer.data.Rule;
import nats.truducer.deprel.TreeConversionStats;
import nats.truducer.startup.Main;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static nats.truducer.io.TreeReaderWriter.fileToTree;
import static nats.truducer.io.TreeReaderWriter.pathToTransducer;

public class ConvResultController implements TreeSelectionListener, ActionListener {
    private ConvResultGUI gui;
    private DefaultMutableTreeNode root;

    private DefaultMutableTreeNode catRoot;
    private DefaultMutableTreeNode ruleRoot;
    private HashMap<String, DefaultMutableTreeNode> frontiers;
    private HashMap<Rule, DefaultMutableTreeNode> rules;

    private String inPath;
    private String outPath;

    public ConvResultController(String inPath, String outPath) {
        createRootTreeNode();
        frontiers = new HashMap<>();
        rules = new HashMap<>();
        this.inPath = inPath;
        this.outPath = outPath;
    }

    public void initWindow() {
        gui = new ConvResultGUI(this, root);
    }

    public void add(File file, File srcFile, TreeConversionStats stats) {
        stats.check();
        if(stats.getRulesUsed() != null) {
            stats.getRulesUsed().forEach(r -> {
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

        if(!node.isLeaf()) {
            // we can't render categories
            return;
        }

        ConversionResult cr = (ConversionResult) node.getUserObject();
        try {
            gui.setTree(fileToTree(cr.getFile()), pathToTransducer("sample_rules_hdt.tud").nClassifier);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
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
            }
        }

        else if(e.getSource() == gui.getOriginalTree) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) gui.tree.getLastSelectedPathComponent();
            if(node == null) {
                // nothing selected
                return;
            }

            if(!node.isLeaf()) {
                // we can't render categories
                return;
            }

            ConversionResult cr = (ConversionResult) node.getUserObject();
            try {
                gui.setTree(fileToTree(cr.getSrcFile()), pathToTransducer("sample_rules_hdt.tud").nClassifier);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        else if(e.getSource() == gui.convert) {
            gui.frame.setEnabled(false);
            createRootTreeNode();
            frontiers.clear();
            rules.clear();
            Main.convertDir(gui.deptreeViewPane.getTransducer(), inPath, outPath, gui.rememberRules.getState(), this);
            ((DefaultTreeModel)gui.treeModel).setRoot(root);
            ((DefaultTreeModel)gui.treeModel).reload(root);
            gui.frame.setEnabled(true);
        }
    }
}

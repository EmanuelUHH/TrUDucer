package nats.truducer.gui;

import cz.ufal.udapi.core.Node;
import nats.truducer.deprel.TreeConversionStats;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static nats.io.TreeReaderWriter.fileToTree;
import static nats.io.TreeReaderWriter.pathToTransducer;

public class ConvResultController implements TreeSelectionListener, ActionListener {
    private ConvResultGUI gui;
    private DefaultMutableTreeNode root;
    private HashMap<String, DefaultMutableTreeNode> frontiers;

    public ConvResultController() {
        root = createRootTreeNode();
        frontiers = new HashMap<>();

    }

    public void initWindow() {
        gui = new ConvResultGUI(this, root);
    }

    public static void main(String... args) {
        new ConvResultController().initWindow();
    }

    public void add(File file, File srcFile, TreeConversionStats stats) {
        stats.check();
        if(!stats.isTreeFullyConverted()) {
            System.out.println(file.getName());
            stats.getBlockerNodes().stream().map(Node::getDeprel).distinct().forEach(s -> {
                DefaultMutableTreeNode category;
                if(!frontiers.containsKey(s)) {
                    category = new DefaultMutableTreeNode(s);
                    frontiers.put(s, category);
                    root.add(category);
                } else {
                    category = frontiers.get(s);
                }
                category.add(new DefaultMutableTreeNode(new ConversionResult(file, srcFile, stats)));
            });
        }
    }

    private DefaultMutableTreeNode createRootTreeNode() {
        return new DefaultMutableTreeNode("failed conversions");
    }

    public void setPercentage(double percentage) {
        root.setUserObject(String.format("%s overall: %2.4f%%", root.getUserObject().toString(), percentage));
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
    }
}

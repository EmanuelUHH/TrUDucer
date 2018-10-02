package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import nats.truducer.data.NodeClassifier;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class ConvResultGUI {

    public final JFrame frame;
    public final JTree tree;
    public final DeptreeViewPane deptreeViewPane;

    public final JMenuItem setTransducer;
    public final JMenuItem getOriginalTree;

    public ConvResultGUI(ConvResultController controller, DefaultMutableTreeNode root) {
        frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();

        JMenu transducerMenu = new JMenu("Transducer");
        menuBar.add(transducerMenu);

        setTransducer = new JMenuItem("set Transducer");
        transducerMenu.add(setTransducer);
        setTransducer.addActionListener(controller);

        JMenu treeMenu = new JMenu("Tree");
        menuBar.add(treeMenu);

        getOriginalTree = new JMenuItem("load original tree");
        treeMenu.add(getOriginalTree);
        getOriginalTree.addActionListener(controller);

        frame.setJMenuBar(menuBar);

        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        tree = new JTree(root);

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(controller);

        JScrollPane scrollpane = new JScrollPane(tree);

        pane.add(scrollpane, BorderLayout.WEST);

        // DepTree View
        deptreeViewPane = new DeptreeViewPane();

        pane.add(deptreeViewPane.getPanel(), BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }

    public void setTree(Root tree, NodeClassifier nodeClassifier) {
        deptreeViewPane.setTree(tree, nodeClassifier);
    }
}

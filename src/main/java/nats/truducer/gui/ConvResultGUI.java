package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import nats.truducer.data.NodeClassifier;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

public class ConvResultGUI {

    public final JFrame frame;
    public final JTree tree;
    public final TreeModel treeModel;
    public final DeptreeViewPane deptreeViewPane;

    public final JScrollPane scrollPane;

    public final JMenuItem setTransducer;
    public final JMenuItem getOriginalTree;

    public final JCheckBoxMenuItem rememberRules;
    public final JMenuItem convert;

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

        JMenu conversionMenu = new JMenu("Conversion");
        menuBar.add(conversionMenu);

        rememberRules = new JCheckBoxMenuItem("show fully converted trees");
        conversionMenu.add(rememberRules);
        convert = new JMenuItem("convert all");
        convert.addActionListener(controller);
        conversionMenu.add(convert);

        frame.setJMenuBar(menuBar);

        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        treeModel = new DefaultTreeModel(root);

        tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(controller);

        scrollPane = new JScrollPane(tree);
        scrollPane.setMaximumSize(new Dimension(400, 1800));
        scrollPane.setPreferredSize(new Dimension(400, 600));

        pane.add(scrollPane, BorderLayout.WEST);

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

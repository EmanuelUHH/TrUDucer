package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import io.gitlab.nats.deptreeviz.DepTree;
import io.gitlab.nats.deptreeviz.SimpleParse;
import io.gitlab.nats.deptreeviz.SimpleWord;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by felix on 22/02/17.
 */
public class MainWindow {

    private static final SimpleParse EMPTY_PARSE = new SimpleParse(new ArrayList<SimpleWord>(), new ArrayList<Integer>(), new ArrayList<String>());
    public final JFrame frame;
    public DepTree<SimpleParse, SimpleWord> depTree = null;
    public DeptreeViewPane depTreeViewPane;

    public final JMenuItem menuItem;

    public MainWindow(MainWindowController controller) {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container pane = frame.getContentPane();

        // MenuBar
        JMenuBar menuBar = new JMenuBar();

        JMenu menu = new JMenu("Tree");
        menu.setMnemonic(KeyEvent.VK_T);
        menuBar.add(menu);

        menuItem = new JMenuItem("Show CoNLL");
        menuItem.addActionListener(controller);
        menu.add(menuItem);

        frame.setJMenuBar(menuBar);


        // tabbed Pane, in the center
        JTabbedPane tabbedPane = new JTabbedPane();
        pane.add(tabbedPane, BorderLayout.CENTER);

        // DepTree View
        depTreeViewPane = new DeptreeViewPane();
        JPanel deptreeViewPane = depTreeViewPane.getPanel();
        tabbedPane.addTab("Tree Conversion", deptreeViewPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}

package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import io.gitlab.nats.deptreeviz.DepTree;
import io.gitlab.nats.deptreeviz.SimpleParse;
import io.gitlab.nats.deptreeviz.SimpleWord;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian on 19/09/18.
 * dirty copy of felix's MaximWindow
 * TODO either remove zoom slider or make it accessible (at the moment it is blocked by the popup)
 */
public class ConvGUI {

    private static final SimpleParse EMPTY_PARSE = new SimpleParse(new ArrayList<SimpleWord>(), new ArrayList<Integer>(), new ArrayList<String>());
    public final JFrame frame;
    public final JSlider zoomSlider;
    public final JTextField ruleTextField;
    public DepTree<SimpleParse, SimpleWord> depTree = null;
    private final JScrollPane scrollPane;


    public ConvGUI(ConvGUIController controller) {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setSize(new Dimension(1600, 400));

        Container pane = frame.getContentPane();

        // tabbed Pane, in the center
        JTabbedPane tabbedPane = new JTabbedPane();
        pane.add(tabbedPane, BorderLayout.CENTER);

        // DepTree View
        JPanel deptreeViewPane = new JPanel();
        deptreeViewPane.setLayout(new BorderLayout());
        tabbedPane.addTab("Tree Conversion", deptreeViewPane);

        // Zoom Slider
        zoomSlider = new JSlider(JSlider.VERTICAL, -10, 10, -3);
        zoomSlider.addChangeListener(controller);
        deptreeViewPane.add(zoomSlider, BorderLayout.WEST);

        // Scroll Pane containing the actual tree
        scrollPane = new JScrollPane();
        depTree = new DepTree<>();

        // * edited by Maximilian Wendt
        // set highlight color for visualization of changed nodes
        depTree.setHighlightColor(Color.MAGENTA);


        scrollPane.setViewportView(depTree.getNodesCanvas());
        int fontSize = depTree.getFont().getSize();
        scrollPane.getHorizontalScrollBar()
                .setUnitIncrement(fontSize * 3);
        scrollPane.getVerticalScrollBar().setUnitIncrement(fontSize * 3);
        scrollPane.setSize(depTree.getCanvasSize());
        deptreeViewPane.add(scrollPane, BorderLayout.CENTER);

        // Bottom row of controls, in the depTreeView Tab
        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.LINE_AXIS));
        ruleTextField = new JTextField();
        ruleTextField.setEditable(false);
        bottomPane.add(ruleTextField);
        deptreeViewPane.add(bottomPane, BorderLayout.SOUTH);

        //Display the window.
        // frame.pack();
        // frame.setVisible(true);
    }

    /**
     * Set the tree to be displayed in the center panel
     * @param tree
     */
    public void setTree(Root tree) {
        SimpleParse p = null;
        if (tree != null) {
            p = UdapiToDeptreeviz.getParse(tree);
        }

        // ** edited by Maximilian
        // reset markers
        depTree.resetMarkedNodes();

        depTree.setDecParse(p);

        depTree.draw(p);

        setZoomLevel();

        scrollPane.setViewportView(depTree.getNodesCanvas());  //wichtig
    }

    /**
     * highlight nodes with given indices (starting with 0, not like the Ord index in ConversionState nodes!!!)
     * @param indices List of indices specifying the nodes, which should be highlighted
     */
    public void highlightNodes(List<Integer> indices) {
        if(indices != null) {
            for(Integer i: indices) {
                depTree.getNode(i).setMarkedLevels(new ArrayList<String>(){{add("SYN");}});
                depTree.highlight(depTree.getNode(i));
            }
        }
    }

    public void setZoomLevel() {
        if (depTree != null) {
            depTree.setZoomFactor(Math.pow(2.0, (((double) zoomSlider.getValue()) / 4.0)));
            depTree.redraw();
            // size changed, set it again in the scrollPane to update
            scrollPane.setViewportView(depTree.getNodesCanvas());
        }

        depTree.highlight(depTree.getNode(0));
        depTree.highlight(depTree.getNode(1));
        depTree.highlight(depTree.getNode(2));
    }
}

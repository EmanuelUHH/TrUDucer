package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import io.gitlab.nats.deptreeviz.DepTree;
import io.gitlab.nats.deptreeviz.SimpleParse;
import io.gitlab.nats.deptreeviz.SimpleWord;
import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.swing.JSVGCanvas;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by felix on 22/02/17.
 */
public class MainWindow {

    private static final SimpleParse EMPTY_PARSE = new SimpleParse(new ArrayList<SimpleWord>(), new ArrayList<Integer>(), new ArrayList<String>());
    public final JFrame frame;
    public final JSlider zoomSlider;
    public final JTextField ruleTextField;
    public final JButton prevButton;
    public final JButton nextButton;
    public DepTree<SimpleParse, SimpleWord> depTree = null;
    private JScrollPane scrollPane;

    public MainWindow(MainWindowController controller) {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container pane = frame.getContentPane();

        JPanel centerPane = new JPanel();
        centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.LINE_AXIS));
        pane.add(centerPane, BorderLayout.CENTER);



        // Zoom Slider
        zoomSlider = new JSlider(JSlider.VERTICAL, -10, 10, 0);
        zoomSlider.addChangeListener(controller);
        centerPane.add(zoomSlider);

        // DepTree View
        scrollPane = new JScrollPane();
        depTree = new DepTree<>();
        scrollPane.setViewportView(depTree.getNodesCanvas());
        int fontSize = depTree.getFont().getSize();
        scrollPane.getHorizontalScrollBar()
                .setUnitIncrement(fontSize * 3);
        scrollPane.getVerticalScrollBar().setUnitIncrement(fontSize * 3);
        scrollPane.setSize(depTree.getCanvasSize());
        centerPane.add(scrollPane);

        // Bottom row of controls
        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.LINE_AXIS));
        ruleTextField = new JTextField();
        ruleTextField.setEditable(false);
        bottomPane.add(ruleTextField);
        prevButton = new JButton("<");
        prevButton.addActionListener(controller);
        bottomPane.add(prevButton);
        nextButton = new JButton(">");
        nextButton.addActionListener(controller);
        bottomPane.add(nextButton);

        pane.add(bottomPane, BorderLayout.SOUTH);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
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
        depTree.setDecParse(p);
        depTree.draw(p);

        scrollPane.setViewportView(depTree.getNodesCanvas());  //wichtig
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

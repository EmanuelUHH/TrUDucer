package nats.truducer.gui;

import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.impl.DefaultDocument;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import io.gitlab.nats.deptreeviz.DepTree;
import io.gitlab.nats.deptreeviz.SimpleParse;
import io.gitlab.nats.deptreeviz.SimpleWord;
import nats.truducer.data.ConversionState;
import nats.truducer.data.NodeClassifier;
import nats.truducer.data.Rule;
import nats.truducer.data.Transducer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DeptreeViewPane implements ChangeListener, ActionListener {

    private DepTree<SimpleParse, SimpleWord> depTree;
    private Stack<ConversionState> convStack = new Stack<>();
    private int currentlyDisplayedTree = -1;
    private Transducer transducer;
    private InteractiveConversionGUI interactiveGUI;

    private final JPanel panel;
    private final JSlider zoomSlider;
    private final JScrollPane scrollPane;
    private final JTextField ruleTextField;
    private final JButton firstButton;
    private final JButton prevButton;
    private final JButton nextButton;
    private final JButton lastButton;

    public DeptreeViewPane() {
        panel = new JPanel();

        panel.setLayout(new BorderLayout());

        // Zoom Slider
        zoomSlider = new JSlider(JSlider.VERTICAL, -10, 10, 0);
        zoomSlider.addChangeListener(this);
        panel.add(zoomSlider, BorderLayout.WEST);

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
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom row of controls, in the depTreeView Tab
        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.LINE_AXIS));
        ruleTextField = new JTextField();
        ruleTextField.setEditable(false);
        bottomPane.add(ruleTextField);
        firstButton = new JButton("<<");
        firstButton.addActionListener(this);
        bottomPane.add(firstButton);
        prevButton = new JButton("<");
        prevButton.addActionListener(this);
        bottomPane.add(prevButton);
        nextButton = new JButton(">");
        nextButton.addActionListener(this);
        bottomPane.add(nextButton);
        lastButton = new JButton(">>");
        lastButton.addActionListener(this);
        bottomPane.add(lastButton);
        panel.add(bottomPane, BorderLayout.SOUTH);
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(nextButton)) {
            if (currentlyDisplayedTree == convStack.size() - 1) {
                ConversionState next = transducer.step(convStack.peek());
                if (next != null) {
                    convStack.push(next);
                    currentlyDisplayedTree += 1;
                } else {
                    nextButton.setEnabled(false);
                }
            } else {
                currentlyDisplayedTree += 1;
            }
            afterTreeOrTransducerUpdate();
        } else if (actionEvent.getSource().equals(prevButton)) {
            currentlyDisplayedTree -= 1;
            afterTreeOrTransducerUpdate();
            nextButton.setEnabled(true);
        } else if (actionEvent.getSource().equals(lastButton)) {
            currentlyDisplayedTree = convStack.size() - 1; // skip to last already calculated step
            ConversionState next = transducer.step(convStack.peek());
            while (next != null) {
                convStack.push(next);
                currentlyDisplayedTree += 1;
                next = transducer.step(next);
            }
            nextButton.setEnabled(false);
            afterTreeOrTransducerUpdate();
        } else if (actionEvent.getSource().equals(firstButton)) {
            currentlyDisplayedTree = 0;
            afterTreeOrTransducerUpdate();
            nextButton.setEnabled(true);
        }
    }


    @Override
    public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals(zoomSlider)) {
            setZoomLevel();
        }
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

    public void setTree(Root tree, NodeClassifier nodeClassifier) {
        convStack = new Stack<>();
        currentlyDisplayedTree = -1;
        if (tree != null) {
            convStack.push(new ConversionState(tree, nodeClassifier));
            currentlyDisplayedTree = 0;
        }
        afterTreeOrTransducerUpdate();
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

        scrollPane.setViewportView(depTree.getNodesCanvas());  //wichtig
    }

    public void setTransducer(Transducer transducer) {
        this.transducer = transducer;
        for (Rule r : this.transducer.rules) {
            // set the handler for interactive conversion triggered by the groovy code
            r.setInteractiveConversion(interactiveGUI);
        }
        nextButton.setEnabled(true);
        afterTreeOrTransducerUpdate();
    }

    private void afterTreeOrTransducerUpdate() {
        boolean wasNextButtonEnabled = nextButton.isEnabled();
        prevButton.setEnabled(false);
        nextButton.setEnabled(false);
        ruleTextField.setText("");
        setTree(null);
        if (convStack.empty()) {
            ruleTextField.setText("No tree given.");
            return;
        }
        ConversionState currentlyShown = convStack.get(currentlyDisplayedTree);
        setTree(currentlyShown.getTree());

        // ** inserted by Maximilian
        // highlights changed nodes in the tree viewer
        highlightNodes(new ArrayList<Integer>(){{
            if(currentlyShown.getChangedNodes() != null && currentlyShown.getChangedNodes().size() > 0)
                for(Node n: currentlyShown.getChangedNodes()) {
                    if(n != null && n.getOrd() > 0)
                        add(n.getOrd() - 1);
                }
        }});

        // ** inserted by Maximilian
        // show applied rule in the viewer
        if(currentlyShown.getAppliedRule() != null) {
            ruleTextField.setText(currentlyShown.getAppliedRule().toString());
        } else {
            ruleTextField.setText("-");
        }

        if (transducer == null) {
            ruleTextField.setText("No transducer given.");
            return;
        }
        nextButton.setEnabled(wasNextButtonEnabled);
        if (currentlyDisplayedTree > 0) {
            prevButton.setEnabled(true);
        }
        firstButton.setEnabled(prevButton.isEnabled());
        lastButton.setEnabled(nextButton.isEnabled());
    }

    public void showConLL(Frame frame) {
        Root tree = convStack.get(currentlyDisplayedTree).getTree();
        Document outDoc = new DefaultDocument();
        outDoc.createBundle().addTree(tree);
        StringWriter sw = new StringWriter();
        BufferedWriter bw = new BufferedWriter(sw);

        new CoNLLUWriter().writeDocument(outDoc, bw);

        new TextBoxPopup(frame, sw.toString());
    }

    public Transducer getTransducer() {
        return transducer;
    }
}

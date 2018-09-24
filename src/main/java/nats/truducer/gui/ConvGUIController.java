package nats.truducer.gui;

import cz.ufal.udapi.core.Document;
import cz.ufal.udapi.core.Node;
import cz.ufal.udapi.core.Root;
import cz.ufal.udapi.core.impl.DefaultDocument;
import cz.ufal.udapi.core.io.impl.CoNLLUWriter;
import nats.truducer.data.ConversionState;
import nats.truducer.data.NodeClassifier;
import nats.truducer.data.Rule;
import nats.truducer.data.Transducer;
import nats.truducer.exceptions.BlockedInteractionException;
import nats.truducer.groovy.GroovyDictionary;
import nats.truducer.interactive.InteractiveConversion;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The class controlling the ConvGUI main window.
 * It creates the ConvGUI main window.
 * it calls the interactive Conversion GUI, when needed, and otherwise hides the main window.
 */
public class ConvGUIController implements ChangeListener, ActionListener, InteractiveConversion {
    private ConvGUI convGUI;
    private InteractiveConversionGUI interactiveGUI;
    private ConversionState currentlyShown;
    private Transducer transducer;
    private boolean interactiveAllowed;

    private GroovyDictionary groovyDictionary;
    private HashMap<String, String> storedLabels;

    public ConvGUIController() {
        this.interactiveAllowed = true;
        this.storedLabels = new HashMap<>();
        this.groovyDictionary = new GroovyDictionary();
    }

    public void initWindow() {
        convGUI = new ConvGUI(this);
        interactiveGUI = new InteractiveConversionGUI(convGUI.frame);
    }

    @Override
    public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals(convGUI.zoomSlider)) {
            convGUI.setZoomLevel();
        }
    }

    public void setCurrentlyShown(ConversionState currentlyShown) {
        this.currentlyShown = currentlyShown;
    }

    public void setTransducer(Transducer transducer) {
        this.transducer = transducer;
        for (Rule r : this.transducer.rules) {
            // set the handler for interactive conversion triggered by the groovy code
            r.setInteractiveConversion(interactiveGUI);
            r.setGroovyDictionary(groovyDictionary);
        }

        // afterTreeOrTransducerUpdate();
    }

    private void afterTreeOrTransducerUpdate() {
        convGUI.ruleTextField.setText("");
        convGUI.setTree(currentlyShown.getTree());

        // ** inserted by Maximilian
        // highlights changed nodes in the tree viewer
        convGUI.highlightNodes(new ArrayList<Integer>(){{
            if(currentlyShown.getChangedNodes() != null)
                for(Node n: currentlyShown.getChangedNodes()) {
                    if(n != null && n.getOrd() > 0)
                        add(n.getOrd() - 1);
                }
        }});

        // ** inserted by Maximilian
        // show applied rule in the viewer
        if(currentlyShown.getAppliedRule() != null) {
            convGUI.ruleTextField.setText(currentlyShown.getAppliedRule().toString());
        } else {
            convGUI.ruleTextField.setText("-");
        }

        if (transducer == null) {
            convGUI.ruleTextField.setText("No transducer given.");
            return;
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        // nothing to listen to, right??
    }

    @Override
    public void decideLabel(Node node, String label1, String label2) throws BlockedInteractionException {
        if(!interactiveAllowed) {
            throw new BlockedInteractionException("interactive conversions are not allowed right now!");
        }

        String storedResult = storedLabels.get(node.getForm());
        if(storedResult != null) {
            node.setDeprel(storedResult);
            return;
        }
        afterTreeOrTransducerUpdate();
        convGUI.frame.setVisible(true);
        try {
            // sleep to avoid problems with asynchronous accesses
            Thread.sleep(500);
        } catch (Exception e) {
            System.err.println("can't sleep :(");
        }
        interactiveGUI.decideLabel(node, label1, label2);
        this.storedLabels.put(node.getForm(), node.getDeprel());
        convGUI.frame.setVisible(false);
    }

    public void close() {
        convGUI.frame.dispose();
    }

    public void setInteractiveAllowed(boolean interactiveAllowed) {
        this.interactiveAllowed = interactiveAllowed;
    }
}

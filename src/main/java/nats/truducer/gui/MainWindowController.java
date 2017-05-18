package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import nats.truducer.data.ConversionState;
import nats.truducer.data.Rule;
import nats.truducer.data.Transducer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;

/**
 * The class controlling the main window.
 * It creates the main window and is the listener for controls (buttons, sliders).
 */
public class MainWindowController implements ChangeListener, ActionListener {
    private MainWindow mainWindow;
    private InteractiveConversionGUI interactiveGUI;
    private Stack<ConversionState> convStack = new Stack<>();
    private int currentlyDisplayedTree = -1;
    private Transducer transducer;

    public MainWindowController() {
    }

    public void initWindow() {
        mainWindow = new MainWindow(this);
        interactiveGUI = new InteractiveConversionGUI(mainWindow.frame);
    }

    @Override
    public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals(mainWindow.zoomSlider)) {
            mainWindow.setZoomLevel();
        }
    }

    public void setTree(Root tree) {
        convStack = new Stack<>();
        currentlyDisplayedTree = -1;
        if (tree != null) {
            convStack.push(new ConversionState(tree));
            currentlyDisplayedTree = 0;
        }
        afterTreeOrTransducerUpdate();
    }

    public void setTransducer(Transducer transducer) {
        this.transducer = transducer;
        for (Rule r : this.transducer.rules) {
            // set the handler for interactive conversion triggered by the groovy code
            r.setInteractiveConversion(interactiveGUI);
        }

        afterTreeOrTransducerUpdate();
    }

    private void afterTreeOrTransducerUpdate() {
        mainWindow.prevButton.setEnabled(false);
        mainWindow.nextButton.setEnabled(false);
        mainWindow.ruleTextField.setText("");
        mainWindow.setTree(null);
        if (convStack.empty()) {
            mainWindow.ruleTextField.setText("No tree given.");
            return;
        }
        mainWindow.setTree(convStack.get(currentlyDisplayedTree).getTree());

        if (transducer == null) {
            mainWindow.ruleTextField.setText("No transducer given.");
            return;
        }
        mainWindow.nextButton.setEnabled(true);
        if (currentlyDisplayedTree > 0) {
            mainWindow.prevButton.setEnabled(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(mainWindow.nextButton)) {
            if (currentlyDisplayedTree == convStack.size() - 1) {
                ConversionState next = transducer.step(convStack.peek());
                if (next != null) {
                    convStack.push(next);
                    currentlyDisplayedTree += 1;
                } else {
                    // TODO somehow disable the button, conversion is not finished but cannot continue
                }
            } else {
                currentlyDisplayedTree += 1;
            }
            afterTreeOrTransducerUpdate();
        } else if (actionEvent.getSource().equals(mainWindow.prevButton)) {
            currentlyDisplayedTree -= 1;
            afterTreeOrTransducerUpdate();
        }
    }
}

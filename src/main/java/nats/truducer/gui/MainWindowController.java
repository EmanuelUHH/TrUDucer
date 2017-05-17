package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import nats.truducer.data.ConversionState;
import nats.truducer.data.Transducer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Stack;

/**
 * Created by felix on 17/05/17.
 */
public class MainWindowController implements ChangeListener, ActionListener {
    private MainWindow mainWindow;
    private Stack<ConversionState> convStack = new Stack<>();
    private ConversionState nextState = null;
    private Transducer transducer;

    public MainWindowController() {
    }

    public void initWindow() {
        mainWindow = new MainWindow(this);

    }

    @Override
    public void stateChanged(ChangeEvent changeEvent) {
        if (changeEvent.getSource().equals(mainWindow.zoomSlider)) {
            mainWindow.setZoomLevel();
        }
    }

    public void setTree(Root tree) {
        convStack = new Stack<>();
        if (tree != null) {
            convStack.push(new ConversionState(tree));
        }
        afterTreeOrTransducerUpdate();
    }

    public void setTransducer(Transducer transducer) {
        this.transducer = transducer;
        afterTreeOrTransducerUpdate();
    }

    private void afterTreeOrTransducerUpdate() {
        nextState = null;
        mainWindow.nextButton.setEnabled(false);
        mainWindow.ruleTextField.setText("");
        mainWindow.setTree(null);
        if (convStack.empty())
            return;
        mainWindow.setTree(convStack.peek().getTree());

        if (transducer == null) {
            return;
        }

        // update next State
        updateNextState();
    }

    private void updateNextState() {
        assert !convStack.empty();
        assert transducer != null;
        nextState = transducer.step(convStack.peek());
        if (nextState != null) {
            mainWindow.nextButton.setEnabled(true);
            mainWindow.ruleTextField.setText(nextState.getAppliedRule().toString());
        } else {
            mainWindow.nextButton.setEnabled(false);
            mainWindow.ruleTextField.setText("");
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(mainWindow.nextButton)) {
            // Button should only be clickable if next state exists
            convStack.push(nextState);
            afterTreeOrTransducerUpdate();
        }
    }
}

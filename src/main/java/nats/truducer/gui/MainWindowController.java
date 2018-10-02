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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Stack;

/**
 * The class controlling the main window.
 * It creates the main window and is the listener for controls (buttons, sliders).
 */
public class MainWindowController implements ActionListener {
    private MainWindow mainWindow;

    public MainWindowController() {
    }

    public void initWindow() {
        mainWindow = new MainWindow(this);
    }

    public void setTitle(String title) {
        mainWindow.frame.setTitle(title);
    }

    public void setTree(Root tree, NodeClassifier nodeClassifier) {
        mainWindow.depTreeViewPane.setTree(tree, nodeClassifier);
    }

    public void setTransducer(Transducer transducer) {
        mainWindow.depTreeViewPane.setTransducer(transducer);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == mainWindow.menuItem) {
            mainWindow.depTreeViewPane.showConLL(mainWindow.frame);
        }
    }
}

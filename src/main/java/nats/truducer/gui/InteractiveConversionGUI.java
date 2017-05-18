package nats.truducer.gui;

import cz.ufal.udapi.core.Node;
import nats.truducer.interactive.InteractiveConversion;

import javax.swing.*;
import java.awt.*;

/**
 * Created by felix on 18/05/17.
 */
public class InteractiveConversionGUI implements InteractiveConversion {

    private final Frame frame;

    public InteractiveConversionGUI(Frame frame) {
        this.frame = frame;
    }

    @Override
    public void decideLabel(Node node, String label1, String label2) {
        Object[] possibilities = {label1, label2};
        String chosen = null;
        while (chosen == null) {
            chosen = (String) JOptionPane.showInputDialog(frame, String.format("Decide the label for the Node '%s' (%s)", node.getForm(), node.getOrd()),
                    "Interactive Conversion", JOptionPane.PLAIN_MESSAGE, null, possibilities, label1);
        }
        node.setDeprel(chosen);
    }
}

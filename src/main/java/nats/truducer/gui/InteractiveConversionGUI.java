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
        JOptionPane.showMessageDialog(frame, String.format("Node: A: %s, B: %s", label1, label2));
    }
}

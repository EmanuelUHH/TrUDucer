package nats.truducer.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by felix on 19/03/18.
 */
public class TextBoxPopup {

    public TextBoxPopup(Frame frame, String text) {
        JDialog dialog = new JDialog(frame);
        JScrollPane scrollPane = new JScrollPane();
        JTextArea ta = new JTextArea();
        ta.setText(text);
        ta.setEditable(false);
        scrollPane.setViewportView(ta);
        dialog.setContentPane(scrollPane);
        dialog.pack();
        dialog.setLocationRelativeTo(null); // center on screen
        dialog.setVisible(true);
    }
}

package nats.truducer.gui;

import cz.ufal.udapi.core.Root;
import io.gitlab.nats.deptreeviz.DepTree;
import io.gitlab.nats.deptreeviz.SimpleParse;
import io.gitlab.nats.deptreeviz.SimpleWord;
import org.apache.batik.swing.JSVGCanvas;

import javax.swing.*;

/**
 * Created by felix on 22/02/17.
 */
public class MainWindow {

    public void createAndShow(Root tree) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DepTree<SimpleParse, SimpleWord> dt = new DepTree<>(UdapiToDeptreeviz.getParse(tree));
        JSVGCanvas canvas = dt.getNodesCanvas();
        frame.getContentPane().add(canvas);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}

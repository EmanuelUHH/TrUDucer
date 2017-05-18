package nats.truducer.interactive;

import cz.ufal.udapi.core.Node;

/**
 * Created by felix on 18/05/17.
 */
public interface InteractiveConversion {

    void decideLabel(Node node, String label1, String label2);

}

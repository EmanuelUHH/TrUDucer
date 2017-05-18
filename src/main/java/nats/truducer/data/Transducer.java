package nats.truducer.data;

import cz.ufal.udapi.core.Root;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Stack;

/**
 * Created by felix on 16/01/17.
 */
public class Transducer {

    private static final Logger logger = Logger.getLogger(Transducer.class);

    public final List<Rule> rules;

    public Transducer(List<Rule> rules) {
        this.rules = rules;
    }


    public Root applyTo(Root root) {
        logger.info(String.format("Transducer applied to %s", root));
        Stack<ConversionState> states = new Stack<>();
        ConversionState init = new ConversionState(root);
        states.push(init);

        while(!states.peek().finished()) {
            ConversionState current = states.peek();
            ConversionState next = step(current);
            if (next != null) {
                states.push(next);
            } else {
                break;
            }
        }

        return states.peek().getTree();
    }

    /**
     * Calculate the next conversion state after this one.
     * Returns null if no rule can be applied.
     */
    public ConversionState step(ConversionState current) {
        logger.info("State: " + current.toString());
        ConversionState next = null;
        for (Rule r : this.rules) {
            logger.debug(String.format("Testing rule: %s", r));
            for (int i = 0; i < current.getFrontier().size(); i++) {
                next = r.apply(current, i);
                if (next != null) {
                    break;
                }
            }
            if (next != null) {
                break;
            }
        }
        return next;
    }
}

package nats.truducer.data;

import cz.ufal.udapi.core.Root;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Stack;

/**
 * A Transducer consists mainly of a ruleset.
 * It can be applied to a dependency tree which creates a converted tree.
 * The transducer does not operate directly on trees, but rather on ConversionStates,
 * which store information about the base tree and the frontier as well.
 * Also, a lot of relevant code is in the Rule class.
 */
public class Transducer {

    private static final Logger logger = Logger.getLogger(Transducer.class);

    public final List<Rule> rules;
    public final NodeClassifier nClassifier = new NodeClassifier();

    public Transducer(List<Rule> rules) {
        this.rules = rules;
    }


    public Root applyTo(Root root) {
        logger.info(String.format("Transducer applied to %s", root));
        Stack<ConversionState> states = new Stack<>();
        ConversionState init = new ConversionState(root, nClassifier);
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
     * Get the node classifier for the transducer.
     * The node classifier implicitly contains the set of source and target nodes.
     */
    public NodeClassifier getNodeClassifier() {
        return nClassifier;
    }

    /**
     * Calculate the next conversion state after this one.
     * Returns null if no rule can be applied.
     *
     * The next state is calculated by trying each of the rules in the rulset in order and
     * testing each rule on each frontier node in the current ConversionState.
     * If at some point no rule matches, null is returned, as no next step can be calculated.
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

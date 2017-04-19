package nats.drt.data;

import cz.ufal.udapi.core.Node;

/**
 * Created by felix on 18/01/17.
 */
public interface Constraint {

    public boolean isSatisfied(Node node);
}

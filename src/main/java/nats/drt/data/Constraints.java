package nats.drt.data;

import cz.ufal.udapi.core.Node;

/**
 * Created by felix on 18/01/17.
 */
public class Constraints {

    public static Constraint deprelEquals(String value) {
        return (Node n) -> n.getDeprel().equals(value);
    }

    public static Constraint postagEquals(String value) {
        return (Node n) -> n.getUpos().equals(value);
    }
}

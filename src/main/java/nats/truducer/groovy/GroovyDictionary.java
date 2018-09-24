package nats.truducer.groovy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroovyDictionary extends HashMap<String, List<String>> {

    @Override
    public List<String> get(Object key) {
        if (containsKey(key)) {
            return super.get(key);
        }
        List<String> newList = new ArrayList<>();
        if(key instanceof String)
            put((String) key, newList);
        return newList;
    }
}

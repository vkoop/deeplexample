package de.vkoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

public class MapUtils {

    private MapUtils() {
    }

    public static void setMapValue(Map<String, Object> map, List<String> keyList, String value){
        if(keyList.isEmpty()){
            //Do nothing...
        }
        else if(keyList.size() == 1){
            map.put(keyList.get(0), value);
        } else {
            final String key = keyList.get(0);
            map.computeIfAbsent(key, theKey -> new HashMap<>());

            final List<String> keyList1 = keyList.subList(1, keyList.size() );
            setMapValue((Map<String, Object>) map.get(key), keyList1, value );
        }
    }

    public static Map<String, Object> map(Map<String, Object> nestedMap, UnaryOperator<String> valueTransformer) {
        final Map<String, Object> resultMap = new HashMap<>();
        traverseMapAccum(nestedMap, (keyList, value) -> setMapValue(resultMap, keyList, valueTransformer.apply(value) ), List.of());
        return resultMap;
    }

    private static void traverseMapAccum(Map<String, Object> nestedMap, BiConsumer<List<String>, String> consumer, List<String> accumulatedKey) {
        for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            final ArrayList<String> accumulatedKeyList = new ArrayList<>(accumulatedKey);
            accumulatedKeyList.add(k);

            if (v instanceof String valueString) {
                consumer.accept(accumulatedKeyList, valueString);
            } else if (v instanceof Map) {
                traverseMapAccum((Map<String, Object>) nestedMap.get(k), consumer, accumulatedKeyList);
            }
        }
    }
}

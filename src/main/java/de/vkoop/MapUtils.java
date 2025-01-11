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

    public static void setMapValue(Map<String, Object> map, List<String> keyList, String value) {
        if (keyList.isEmpty()) {
            return;
        }

        String firstElement = keyList.getFirst();
        if (keyList.size() == 1) {
            map.put(firstElement, value);
        } else {
            map.computeIfAbsent(firstElement, theKey -> new HashMap<>());

            final List<String> keyList1 = keyList.subList(1, keyList.size());
            setMapValue((Map<String, Object>) map.get(firstElement), keyList1, value);
        }
    }

    public static Map<String, Object> map(Map<String, Object> nestedMap, UnaryOperator<String> valueTransformer) {
        final Map<String, Object> resultMap = new HashMap<>();
        traverseMapAccum(nestedMap, (keyList, value) -> setMapValue(resultMap, keyList, valueTransformer.apply(value)), List.of());
        return resultMap;
    }


     static void traverseMapAccum(Map<String, Object> nestedMap, BiConsumer<List<String>, String> consumer, List<String> accumulatedKey) {
        for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
            String key = entry.getKey();
            Object valueObject = entry.getValue();

            final ArrayList<String> accumulatedKeyList = new ArrayList<>(accumulatedKey);
            accumulatedKeyList.add(key);

            // leaf node and string value
            if (valueObject instanceof String valueString) {
                consumer.accept(accumulatedKeyList, valueString);
            } else if (valueObject instanceof Map) {
                traverseMapAccum((Map<String, Object>) nestedMap.get(key), consumer, accumulatedKeyList);
            }
        }
    }
}

package de.vkoop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapUtilsTest {

    @Test
    public void testSetMapValue_SingleKey() {
        Map<String, Object> map = new HashMap<>();
        List<String> keys = Collections.singletonList("key");
        String value = "value";

        MapUtils.setMapValue(map, keys, value);

        assertEquals("value", map.get("key"));
    }

    @Test
    public void testSetMapValue_MultipleKeys() {
        Map<String, Object> map = new HashMap<>();
        List<String> keys = Arrays.asList("level1", "level2", "level3");
        String value = "deepValue";

        MapUtils.setMapValue(map, keys, value);

        Map<String, Object> level1Map = (Map<String, Object>) map.get("level1");
        assertTrue(level1Map.containsKey("level2"));

        Map<String, Object> level2Map = (Map<String, Object>) level1Map.get(
            "level2"
        );
        assertEquals("deepValue", level2Map.get("level3"));
    }

    @Test
    public void testSetMapValue_EmptyKeys() {
        Map<String, Object> map = new HashMap<>();
        List<String> keys = Collections.emptyList();
        String value = "value";

        MapUtils.setMapValue(map, keys, value);

        assertTrue(
            map.isEmpty(),
            "Map should remain empty when key list is empty"
        );
    }

    @Test
    public void testSetMapValue_OverwriteValue() {
        Map<String, Object> map = new HashMap<>();
        List<String> keys = Arrays.asList("key1", "key2");
        String initialValue = "initialValue";
        String newValue = "newValue";

        MapUtils.setMapValue(map, keys, initialValue);
        MapUtils.setMapValue(map, keys, newValue);

        Map<String, Object> level1Map = (Map<String, Object>) map.get("key1");
        assertEquals("newValue", level1Map.get("key2"));
    }

    @Test
    public void traverseMapAccum_SingleLevelMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        List<String> result = new ArrayList<>();
        MapUtils.traverseMapAccum(
            map,
            (keys, value) -> result.add(String.join(".", keys) + "=" + value),
            List.of()
        );

        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=value2"));
    }

    @Test
    public void traverseMapAccum_NestedMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key2", "value2");
        map.put("key1", nestedMap);

        List<String> result = new ArrayList<>();
        MapUtils.traverseMapAccum(
            map,
            (keys, value) -> result.add(String.join(".", keys) + "=" + value),
            List.of()
        );

        assertTrue(result.contains("key1.key2=value2"));
    }

    @Test
    public void traverseMapAccum_EmptyMap() {
        Map<String, Object> map = new HashMap<>();

        List<String> result = new ArrayList<>();
        MapUtils.traverseMapAccum(
            map,
            (keys, value) -> result.add(String.join(".", keys) + "=" + value),
            List.of()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void traverseMapAccum_MixedContentMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key3", "value3");
        map.put("key2", nestedMap);

        List<String> result = new ArrayList<>();
        MapUtils.traverseMapAccum(
            map,
            (keys, value) -> result.add(String.join(".", keys) + "=" + value),
            List.of()
        );

        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2.key3=value3"));
    }

    @Test
    public void map_SingleLevelMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        Map<String, Object> result = MapUtils.map(map, String::toUpperCase);

        assertEquals("VALUE1", result.get("key1"));
        assertEquals("VALUE2", result.get("key2"));
    }

    @Test
    public void map_NestedMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key2", "value2");
        map.put("key1", nestedMap);

        Map<String, Object> result = MapUtils.map(map, String::toUpperCase);

        Map<String, Object> resultNestedMap = (Map<String, Object>) result.get(
            "key1"
        );
        assertEquals("VALUE2", resultNestedMap.get("key2"));
    }

    @Test
    public void map_EmptyMap() {
        Map<String, Object> map = new HashMap<>();

        Map<String, Object> result = MapUtils.map(map, String::toUpperCase);

        assertTrue(result.isEmpty());
    }

    @Test
    public void map_MixedContentMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key3", "value3");
        map.put("key2", nestedMap);

        Map<String, Object> result = MapUtils.map(map, String::toUpperCase);

        assertEquals("VALUE1", result.get("key1"));
        Map<String, Object> resultNestedMap = (Map<String, Object>) result.get(
            "key2"
        );
        assertEquals("VALUE3", resultNestedMap.get("key3"));
    }

    @Test
    public void map_NullValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", null);

        Map<String, Object> result = MapUtils.map(map, String::toUpperCase);

        assertNull(result.get("key1"));
    }
}

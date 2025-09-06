package org.jsonrepairj;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class ObjectComparer {

    /**
     * Recursively compares two objects and ensures that:
     * - Their types match
     * - Their keys/structure match
     */
    public static boolean isSameObject(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }

        if (obj1 == null || obj2 == null) {
            return false;
        }

        if (obj1.getClass() != obj2.getClass()) {
            // Fail immediately if the types don't match
            return false;
        }

        if (obj1 instanceof Map) {
            Map<?, ?> map1 = (Map<?, ?>) obj1;
            Map<?, ?> map2 = (Map<?, ?>) obj2;

            // Check that both are maps and same length
            if (map1.size() != map2.size()) {
                return false;
            }

            for (Object key : map1.keySet()) {
                if (!map2.containsKey(key)) {
                    return false;
                }
                // Recursively compare each value
                if (!isSameObject(map1.get(key), map2.get(key))) {
                    return false;
                }
            }
            return true;
        } else if (obj1 instanceof List) {
            List<?> list1 = (List<?>) obj1;
            List<?> list2 = (List<?>) obj2;

            // Check that both are lists and same length
            if (list1.size() != list2.size()) {
                return false;
            }

            // Recursively compare each item
            for (int i = 0; i < list1.size(); i++) {
                if (!isSameObject(list1.get(i), list2.get(i))) {
                    return false;
                }
            }
            return true;
        }

        // For atomic values: types already match, so return True
        return obj1.equals(obj2);
    }

    /**
     * Returns True if value is an empty container (String, List, Map).
     * Returns False for non-containers like null, numbers, etc.
     */
    public static boolean isStrictlyEmpty(JsonNode value) {
        if (value.isContainerNode()) {
            return value.isEmpty();
        } else if (value.isTextual()) {
            return value.asText().isEmpty();
        } else if (value.isMissingNode()) {
            return true;
        } else {
            return false;
        }
    }
}
package compiler.utils;

import java.util.List;

public class ListUtils {
    public static void ensureSize(List<?> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }
}

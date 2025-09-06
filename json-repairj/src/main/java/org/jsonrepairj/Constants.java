package org.jsonrepairj;

import java.util.*;

public class Constants {
    public static final Set<Character> STRING_DELIMITERS = new HashSet<>(Arrays.asList('"', '\'', '“', '”'));
    public static final Set<Character> NUMBER_CHARS = new HashSet<>(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', 'e', 'E', '/', ','));
    public static final char END = '\0';
}
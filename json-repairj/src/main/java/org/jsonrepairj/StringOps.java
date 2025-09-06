package org.jsonrepairj;

import static org.jsonrepairj.Constants.END;

public class StringOps {
    public static char last(StringBuilder sb) {
        if (sb.length() == 0) {
            return END;
        }
        return sb.charAt(sb.length() - 1);
    }

    public static void setLast(StringBuilder sb, char c) {
        if (sb.length() == 0) {
            sb.append(c);
        }
        sb.setCharAt(sb.length() - 1, c);
    }

    public static StringBuilder rstrip(StringBuilder sb) {
        int i;
        for (i = sb.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(sb.charAt(i))) {
                break;
            }
        }
        sb.setLength(i + 1);
        return sb;
    }

    public static boolean in(char c, char... list) {
        for (char item : list) {
            if (c == item) {
                return true;
            }
        }
        return false;
    }

    public static boolean notIn(char c, char... list) {
        for (char item : list) {
            if (c == item) {
                return false;
            }
        }
        return true;
    }
}

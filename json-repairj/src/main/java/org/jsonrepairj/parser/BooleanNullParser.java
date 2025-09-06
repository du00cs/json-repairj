package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.Json.FACTORY;

public class BooleanNullParser implements NodeParser {

    final static String[] values = {"true", "false", "null"};

    @Override
    public JsonNode parse(JSONParser parser) {
        // <boolean> is one of the literal strings 'true', 'false', or 'null' (unquoted)
        int startingIndex = parser.getIndex();
        char ch = Character.toLowerCase(parser.getCharAt());

        for (int v = 0; v < values.length; v++) {
            String value = values[v];
            if (ch == value.charAt(0)) {
                int i = 0;
                while (ch != END && i < value.length() && ch == value.charAt(i)) {
                    i += 1;
                    parser.shift();
                    ch = Character.toLowerCase(parser.getCharAt());
                }
                if (i == value.length()) {
                    switch (value) {
                        case "true":
                            return FACTORY.booleanNode(true);
                        case "false":
                            return FACTORY.booleanNode(false);
                        case "null":
                            return FACTORY.nullNode();
                    }
                }
            }
        }

        // If nothing works reset the index before returning
        parser.setIndex(startingIndex);
        return FACTORY.missingNode();
    }
}
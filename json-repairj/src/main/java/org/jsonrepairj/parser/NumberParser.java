package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.Constants.NUMBER_CHARS;
import static org.jsonrepairj.Json.FACTORY;
import static org.jsonrepairj.StringOps.in;
import static org.jsonrepairj.StringOps.last;

public class NumberParser implements NodeParser {

    @Override
    public JsonNode parse(JSONParser parser) {
        // <number> is a valid real number expressed in one of a number of given formats
        StringBuilder number = new StringBuilder();
        char ch = parser.getCharAt();
        boolean isArray = parser.getCurrentContext() == ContextValue.ARRAY;
        while (ch != END && NUMBER_CHARS.contains(ch) && (!isArray || ch != ',')) {
            number.append(ch);
            parser.shift();
            ch = parser.getCharAt();
        }
        if (number.length() > 0 && in(last(number), '-', 'e', 'E', '/', ',')) {
            // The number ends with a non valid character for a number/currency, rolling back one
            number.setLength(number.length() - 1);
            parser.shift(-1);
        } else if (parser.getCharAt() != END && Character.isLetter(parser.getCharAt())) {
            // this was a string instead, sorry
            parser.shift(- number.length());
            return parser.parseString();
        }
        if (number.length() == 0) {
            return FACTORY.missingNode();
        } else if (number.indexOf(",") != -1) {
            return FACTORY.textNode(number.toString());
        }
        try {
            if (number.indexOf(".") != -1 || number.indexOf("e") != -1 || number.indexOf("E") != -1) {
                return FACTORY.numberNode(Double.parseDouble(number.toString()));
            } else {
                return FACTORY.numberNode(Long.parseLong(number.toString()));
            }
        } catch (NumberFormatException e) {
            return FACTORY.textNode(number.toString());
        }
    }

}
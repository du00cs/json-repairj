package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jsonrepairj.Json;
import org.jsonrepairj.ObjectComparer;
import org.jsonrepairj.StringOps;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.Constants.STRING_DELIMITERS;
import static org.jsonrepairj.StringOps.notIn;

public class ArrayParser implements NodeParser {

    @Override
    public JsonNode parse(JSONParser parser) {
        // <array> ::= '[' [ <json> *(', ' <json>) ] ']' ; A sequence of JSON values separated by commas
        ArrayNode array = Json.FACTORY.arrayNode();
        parser.getContext().set(ContextValue.ARRAY);
        // Stop when you either find the closing parentheses or you have iterated over the entire string
        char ch = parser.getCharAt();
        while (notIn(ch, END, ']', '}')) {
            parser.skipWhitespacesAt();
            JsonNode value;
            if (STRING_DELIMITERS.contains(ch)) {
                // Sometimes it can happen that LLMs forget to start an object and then you think it's a string in an array
                // So we are going to check if this string is followed by a : or not
                // And either parse the string or parse the object
                int i = 1;
                i = parser.skipToCharacter(ch, i);
                i = parser.skipWhitespacesAt(i + 1, false);
                value = (parser.getCharAt(i) == ':') ? parser.parseObject() : parser.parseString();
            } else {
                value = parser.parseJson();
            }

            // It is possible that parseJson() returns nothing valid, so we increase by 1
            if (ObjectComparer.isStrictlyEmpty(value)) {
                parser.shift();
            } else if (value instanceof TextNode && value.textValue().equals("...") && parser.getCharAt(-1) == '.') {
                parser._log("While parsing an array, found a stray '...'; ignoring it");
            } else if (!(value instanceof MissingNode)) { //skip comments
                array.add(value);
            }

            // skip over whitespace after a value but before closing ]
            ch = parser.getCharAt();
            while (notIn(ch, END, ']', '}') && (Character.isWhitespace(ch) || ch == ',')) {
                parser.shift();
                ch = parser.getCharAt();
            }
        }

        // Especially at the end of an LLM generated json you might miss the last "]"
        if (notIn(ch, END, ']', '}')) {
            parser._log("While parsing an array we missed the closing ], ignoring it");
        }

        parser.shift();
        parser.getContext().reset();
        return array;
    }

}
package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import org.jsonrepairj.Constants;
import org.jsonrepairj.Json;

import java.util.List;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.StringOps.in;

public class ObjectParser implements NodeParser {

    @Override
    public JsonNode parse(JSONParser parser) {
        // <object> ::= '{' [ <member> *(', ' <member>) ] '}' ; A sequence of 'members'
        ObjectNode obj = Json.FACTORY.objectNode();
        List<String> keys = Lists.newArrayList();
        // Stop when you either find the closing parentheses or you have iterated over the entire string
        while (parser.getCharAt() != END && parser.getCharAt() != '}') {
            // This is what we expect to find:
            // <member> ::= <string> ': ' <json>

            // Skip filler whitespaces
            parser.skipWhitespacesAt();

            // Sometimes LLMs do weird things, if we find a ":" so early, we'll change it to "," and move on
            if (parser.getCharAt() == ':') {
                parser._log("While parsing an object we found a : before a key, ignoring");
                parser.shift();
            }

            // We are now searching for they string key
            // Context is used in the string parser to manage the lack of quotes
            parser.getContext().set(ContextValue.OBJECT_KEY);

            // Save this index in case we need find a duplicate key
            int rollbackIndex = parser.getIndex();

            // <member> starts with a <string>
            String key = "";
            while (parser.getCharAt() != END) {
                // The rollback index needs to be updated here in case the key is empty
                rollbackIndex = parser.getIndex();
                if (parser.getCharAt() == '[' && key.equals("")) {
                    // Is this an array?
                    // Need to check if the previous parsed value contained in obj is an array and in that case parse and merge the two
                    String prevKey = !keys.isEmpty() ? keys.get(keys.size() - 1) : null;
                    if (prevKey != null && obj.get(prevKey).isArray()) {
                        // If the previous key's value is an array, parse the new array and merge
                        parser.shift();
                        JsonNode newArray = parser.parseArray();
                        if (newArray instanceof ArrayNode) {
                            // Merge and flatten the arrays
                            JsonNode prevValue = obj.get(prevKey);
                            if (prevValue instanceof ArrayNode) {
                                ((ArrayNode) prevValue).addAll((ArrayNode) newArray);
                            }
                            parser.skipWhitespacesAt();
                            if (parser.getCharAt() == ',') {
                                parser.shift();
                            }
                            parser.skipWhitespacesAt();
                            continue;
                        }
                    }
                }
                JsonNode keyNode = parser.parseString();
                if (keyNode instanceof TextNode) {
                    key = keyNode.textValue();
                } else {
                    key = "";
                }
                if (key.equals("")) {
                    parser.skipWhitespacesAt();
                }
                if (!key.equals("") || (key.equals("") && (parser.getCharAt() == ':' || parser.getCharAt() == '}'))) {
                    // If the string is empty but there is a object divider, we are done here
                    break;
                }
            }
            if (parser.getContext().contains(ContextValue.ARRAY) && obj.has(key)) {
                parser._log("While parsing an object we found a duplicate key, closing the object here and rolling back the index");
                parser.setIndex(rollbackIndex - 1);
                // add an opening curly brace to make this work
                String jsonStr = parser.getJsonStr();
                jsonStr = jsonStr.substring(0, parser.getIndex() + 1) + "{" + jsonStr.substring(parser.getIndex() + 1);
                parser.setJsonStr(jsonStr);
                break;
            }

            // Skip filler whitespaces
            parser.skipWhitespacesAt();

            // We reached the end here
            if (in(parser.getCharAt(), END, '}')) {
                continue;
            }

            parser.skipWhitespacesAt();

            // An extreme case of missing ":" after a key
            if (parser.getCharAt() != ':') {
                parser._log("While parsing an object we missed a : after a key");
            }

            parser.shift();
            parser.getContext().reset();
            parser.getContext().set(ContextValue.OBJECT_VALUE);
            // The value can be any valid json
            parser.skipWhitespacesAt();
            // Corner case, a lone comma
            JsonNode value = Json.FACTORY.missingNode();
            if (in(parser.getCharAt(), ',', '}')) {
                parser._log("While parsing an object value we found a stray , ignoring it");
            } else {
                value = parser.parseJson();
            }

            // Reset context since our job is done
            parser.getContext().reset();
            obj.set(key, value);
            keys.add(key);

            if (in(parser.getCharAt(), END, ',', '\'', '"')) {
                parser.shift();
            }

            // Remove trailing spaces
            parser.skipWhitespacesAt();
        }

        parser.shift();
        return obj;
    }
}
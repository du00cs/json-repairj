package org.jsonrepairj;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.jsonrepairj.parser.JSONParser;

import java.util.ArrayList;

import static org.jsonrepairj.Json.MAPPER;

/**
 * This module will parse the JSON file following the BNF definition:
 *
 * <json> ::= <container>
 *
 * <primitive> ::= <number> | <string> | <boolean>
 * ; Where:
 * ; <number> is a valid real number expressed in one of a number of given formats
 * ; <string> is a string of valid characters enclosed in quotes
 * ; <boolean> is one of the literal strings 'true', 'false', or 'null' (unquoted)
 *
 * <container> ::= <object> | <array>
 * <array> ::= '[' [ <json> *(', ' <json>) ] ']' ; A sequence of JSON values separated by commas
 * <object> ::= '{' [ <member> *(', ' <member>) ] '}' ; A sequence of 'members'
 * <member> ::= <string> ': ' <json> ; A pair consisting of a name, and a JSON value
 * <p>
 * If something is wrong (a missing parentheses or quotes for example) it will use a few simple heuristics to fix the JSON string:
 * - Add the missing parentheses if the parser believes that the array or object should be closed
 * - Quote strings or add missing single quotes
 * - Adjust whitespaces and remove line breaks
 * <p>
 * All supported use cases are in the unit tests
 */
public class JsonRepair {


    /**
     * Given a json formatted string, it will try to decode it and, if it fails, it will try to fix it.
     *
     * @param jsonStr       The JSON string to repair.
     * @param skipJsonLoads If True, skip calling the built-in json.loads() function to verify that the json is valid before attempting to repair. Defaults to False.
     * @param streamStable  When the json to be repaired is the accumulation of streaming json at a certain moment.If this parameter to True will keep the repair results stable.
     * @return The repaired JSON or a tuple with the repaired JSON and repair log.
     */
    public static ParseResult parseJson(
            @NonNull String jsonStr,
            boolean skipJsonLoads,
            boolean streamStable) {

        JSONParser parser = new JSONParser(jsonStr, streamStable);
        ParseResult parsedJson;

        if (skipJsonLoads) {
            parsedJson = parser.parse();
        } else {
            try {
                // Try to parse with Jackson first
                JsonNode node = MAPPER.readTree(jsonStr);
                parsedJson = new ParseResult(node, new ArrayList<>());
            } catch (Exception e) {
                parsedJson = parser.parse();
            }
        }
        return parsedJson;
    }

    public static String repairJson(String jsonStr) {
        ParseResult result = parseJson(jsonStr, false, false);
        if (result.getJson().isMissingNode()) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(result.getJson());
        } catch (Exception e) {
            return "";
        }
    }
}
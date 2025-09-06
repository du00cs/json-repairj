package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import lombok.Getter;
import lombok.Setter;
import org.jsonrepairj.Constants;
import org.jsonrepairj.FixInfo;
import org.jsonrepairj.Json;
import org.jsonrepairj.ParseResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.StringOps.in;

public class JSONParser {
    private final ArrayParser array;
    private final BooleanNullParser booleanNull;
    private final NumberParser number;
    private final StringParser string;
    private final ObjectParser object;
    private final CommentParser comment;
    @Getter
    @Setter
    private String jsonStr;
    @Getter
    private boolean streamStable;
    @Setter
    @Getter
    private int index;
    @Getter
    private JsonContext context;
    private List<FixInfo> logger;

    public JSONParser(String jsonStr, boolean streamStable) {
        this.jsonStr = jsonStr != null ? jsonStr : "";
        this.streamStable = streamStable;
        this.index = 0;
        this.context = new JsonContext();
        this.logger = new ArrayList<>();

        this.array = new ArrayParser();
        this.booleanNull = new BooleanNullParser();
        this.number = new NumberParser();
        this.object = new ObjectParser();
        this.string = new StringParser();
        this.comment = new CommentParser();
    }

    public ParseResult parse() {
        JsonNode json = parseJson();
        if (index < jsonStr.length()) {
            _log("The parser returned early, checking if there's more json elements");
            ArrayNode arrayNode = Json.FACTORY.arrayNode();
            arrayNode.add(json);

            while (index < jsonStr.length()) {
                JsonNode j = parseJson();
                if (!(j instanceof MissingNode)) {
                    // not that necessary, just comment out
                    // if (ObjectComparer.isSameObject(jsonArray.get(jsonArray.size() - 1), j)) {
                    //     // replace the last entry with the new one since the new one seems an update
                    //     jsonArray.remove(jsonArray.size() - 1);
                    // }
                    arrayNode.add(j);
                } else {
                    // this was a bust, move the index
                    index += 1;
                }
            }

            // If nothing extra was found, don't return an array
            if (arrayNode.size() == 1) {
                _log("There were no more elements, returning the element without the array");
            } else {
                json = arrayNode;
            }
        }
        return new ParseResult(json, logger);
    }

    public JsonNode parseJson() {
        while (true) {
            char ch = getCharAt();
            // False means that we are at the end of the string provided
            if (ch == END) {
                return Json.MAPPER.missingNode();
            }
            // <object> starts with '{'
            else if (ch == '{') {
                index += 1;
                return parseObject();
            }
            // <array> starts with '['
            else if (ch == '[') {
                index += 1;
                return parseArray();
            }
            // <string> starts with a quote
            else if (!context.isEmpty() && (Constants.STRING_DELIMITERS.contains(ch) || Character.isLetter(ch))) {
                return parseString();
            }
            // <number> starts with [0-9] or minus
            else if (!context.isEmpty() && (Character.isDigit(ch) || ch == '-' || ch == '.')) {
                return parseNumber();
            } else if (in(ch, '#', '/')) {
                return parseComment();
            }
            // If everything else fails, we just ignore and move on
            else {
                index += 1;
            }
        }
    }

    public char getCharAt() {
        return getCharAt(0);
    }

    public char getCharAt(int count) {
        if (jsonStr.length() > index + count) {
            return jsonStr.charAt(index + count);
        } else {
            return END;
        }
    }

    public void _log(String text) {
        int window = 10;
        int start = Math.max(index - window, 0);
        int end = Math.min(index + window, jsonStr.length());
        String contextStr = jsonStr.substring(start, end);

        logger.add(new FixInfo(text, contextStr));
    }

    public JsonNode parseObject() {
        return object.parse(this);
    }

    public JsonNode parseArray() {
        return array.parse(this);
    }

    public JsonNode parseString() {
        return string.parse(this);
    }

    public JsonNode parseNumber() {
        return number.parse(this);
    }

    public JsonNode parseComment() {
        return comment.parse(this);
    }

    public JsonNode parseBooleanOrNull() {
        return booleanNull.parse(this);
    }

    public int skipWhitespacesAt() {
        return skipWhitespacesAt(0, true);
    }

    public int skipWhitespacesAt(int idx, boolean moveMainIndex) {
        if (index + idx >= jsonStr.length()) {
            return idx;
        }
        char ch = jsonStr.charAt(index + idx);

        while (Character.isWhitespace(ch)) {
            if (moveMainIndex) {
                index += 1;
            } else {
                idx += 1;
            }
            if (index + idx >= jsonStr.length()) {
                return idx;
            }
            ch = jsonStr.charAt(index + idx);
        }
        return idx;
    }

    public int skipToCharacter(char character) {
        return skipToCharacter(character, 0);
    }

    public int skipToCharacter(char character, int idx) {
        return skipToCharacter(new char[]{character}, idx);
    }

    public int skipToCharacter(char[] characters, int idx) {
        if (index + idx >= jsonStr.length()) {
            return idx;
        }
        char ch = jsonStr.charAt(index + idx);

        Set<Character> charSet = new HashSet<>();
        for (char c : characters) {
            charSet.add(c);
        }

        while (!charSet.contains(ch)) {
            idx += 1;
            if (index + idx >= jsonStr.length()) {
                return idx;
            }
            ch = jsonStr.charAt(index + idx);
        }

        if (jsonStr.charAt(index + idx - 1) == '\\') {
            // Ah shoot this was actually escaped, continue
            return skipToCharacter(characters, idx + 1);
        }
        return idx;
    }

    public void shift() {
        this.index++;
    }

    public void shift(int i) {
        this.index += i;
    }

    public ContextValue getCurrentContext() {
        return context.getCurrent();
    }

    @Override
    public String toString() {
        return String.format("[%d](%c) %s", index, getCharAt(), context);
    }
}
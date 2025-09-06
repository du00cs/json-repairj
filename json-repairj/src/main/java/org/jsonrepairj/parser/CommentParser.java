package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.List;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.Json.FACTORY;
import static org.jsonrepairj.parser.ContextValue.*;

public class CommentParser implements NodeParser {

    @Override
    public JsonNode parse(JSONParser parser) {
        /**
         * Parse code-like comments:
         *
         * - "# comment": A line comment that continues until a newline.
         * - "// comment": A line comment that continues until a newline.
         * - "* comment *": A block comment that continues until the closing delimiter.
         *
         * The comment is skipped over and an empty string is returned so that comments do not interfere
         * with the actual JSON elements.
         */
        char ch = parser.getCharAt();
        List<Character> terminationCharacters = Lists.newArrayList('\n', '\r');
        if (parser.getContext().contains(ARRAY)) {
            terminationCharacters.add(']');
        }
        if (parser.getContext().contains(OBJECT_VALUE)) {
            terminationCharacters.add('}');
        }
        if (parser.getContext().contains(OBJECT_KEY)) {
            terminationCharacters.add(':');
        }
        // Line comment starting with #
        if (ch == '#') {
            StringBuilder comment = new StringBuilder();
            while (ch != END && !contains(terminationCharacters, ch)) {
                comment.append(ch);
                parser.shift();
                ch = parser.getCharAt();
            }
            parser._log(String.format("Found line comment: %s, ignoring", comment));
        }
        // Comments starting with '/'
        else if (ch == '/') {
            char nextChar = parser.getCharAt(1);
            // Handle line comment starting with //
            if (nextChar == '/') {
                StringBuilder comment = new StringBuilder("//");
                parser.setIndex(parser.getIndex() + 2); // Skip both slashes.
                ch = parser.getCharAt();
                while (ch != END && !contains(terminationCharacters, ch)) {
                    comment.append(ch);
                    parser.shift();
                    ch = parser.getCharAt();
                }
                parser._log(String.format("Found line comment: %s, ignoring", comment));
            }
            // Handle block comment starting with /*
            else if (nextChar == '*') {
                StringBuilder comment = new StringBuilder("/*");
                parser.shift(2); // Skip '/*'
                while (true) {
                    ch = parser.getCharAt();
                    if (ch == END) {
                        parser._log("Reached end-of-string while parsing block comment; unclosed block comment.");
                        break;
                    }
                    comment.append(ch);
                    parser.shift();
                    if (comment.length() >= 2 && comment.charAt(comment.length() - 2) == '*' && comment.charAt(comment.length() - 1) == '/') {
                        break;
                    }
                }
                parser._log(String.format("Found block comment: %s, ignoring", comment));
            } else {
                // Skip standalone '/' characters that are not part of a comment
                // to avoid getting stuck in an infinite loop
                parser.shift();
            }
        }
        if (parser.getContext().isEmpty()) {
            return parser.parseJson();
        } else {
            return FACTORY.missingNode();
        }
    }

    private static boolean contains(List<Character> array, char value) {
        return array.contains(value);
    }
}
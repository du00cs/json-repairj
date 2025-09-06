package org.jsonrepairj.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.jsonrepairj.StringOps;

import java.util.Map;

import static org.jsonrepairj.Constants.END;
import static org.jsonrepairj.Constants.STRING_DELIMITERS;
import static org.jsonrepairj.Json.EMPTY_STRING;
import static org.jsonrepairj.Json.FACTORY;
import static org.jsonrepairj.StringOps.in;
import static org.jsonrepairj.StringOps.notIn;
import static org.jsonrepairj.parser.ContextValue.*;

public class StringParser implements NodeParser {

    static final Map<Character, Character> ESCAPE_SEQS = ImmutableMap.of(
            't', '\t',
            'n', '\n',
            'r', '\r',
            'b', '\b'
    );

    @Override
    public JsonNode parse(JSONParser parser) {
        // <string> is a string of valid characters enclosed in quotes
        // i.e. { name: "John" }
        // Somehow all weird cases in an invalid JSON happen to be resolved in this function, so be careful here

        // Flag to manage corner cases related to missing starting quote
        boolean missingQuotes = false;
        boolean doubledQuotes = false;
        char lstringDelimiter = '"';
        char rstringDelimiter = '"';

        char ch = parser.getCharAt();
        if (ch == '#' || ch == '/') {
            return parser.parseComment();
        }
        // A valid string can only start with a valid quote or, in our case, with a literal
        while (ch != END && !STRING_DELIMITERS.contains(ch) && !Character.isLetterOrDigit(ch)) {
            parser.shift();
            ch = parser.getCharAt();
        }

        if (ch == END) {
            // This is an empty string
            return FACTORY.missingNode();
        }

        // Ensuring we use the right delimiter
        if (ch == '\'') {
            lstringDelimiter = rstringDelimiter = '\'';
        } else if (ch == '“') {
            lstringDelimiter = '“';
            rstringDelimiter = '”';
        } else if (Character.isLetterOrDigit(ch)) {
            // This could be a <boolean> and not a string. Because (T)rue or (F)alse or (N)ull are valid
            // But remember, object keys are only of type string
            if ("tfn".indexOf(Character.toLowerCase(ch)) != -1 &&
                    parser.getCurrentContext() != OBJECT_KEY) {
                JsonNode value = parser.parseBooleanOrNull();
                if (!value.isMissingNode()) {
                    return value;
                }
            }
            parser._log("While parsing a string, we found a literal instead of a quote");
            missingQuotes = true;
        }

        if (!missingQuotes) {
            parser.shift();
        }

        // There is sometimes a weird case of doubled quotes, we manage this also later in the while loop
        if (STRING_DELIMITERS.contains(parser.getCharAt()) && parser.getCharAt() == lstringDelimiter) {
            // If it's an empty key, this was easy
            if ((parser.getCurrentContext() == OBJECT_KEY && parser.getCharAt(1) == ':') ||
                    (parser.getCurrentContext() == OBJECT_VALUE &&
                            (parser.getCharAt(1) == ',' || parser.getCharAt(1) == '}'))) {
                parser.shift();
                return EMPTY_STRING;
            } else if (parser.getCharAt(1) == lstringDelimiter) {
                // There's something fishy about this, we found doubled quotes and then again quotes
                parser._log("While parsing a string, we found a doubled quote and then a quote again, ignoring it");
                return EMPTY_STRING;
            }
            // Find the next delimiter
            int i = parser.skipToCharacter(rstringDelimiter, 1);
            char nextC = parser.getCharAt(i);
            // Now check that the next character is also a delimiter to ensure that we have "".....""
            // In that case we ignore this rstring delimiter
            if (nextC != END && parser.getCharAt(i + 1) == rstringDelimiter) {
                parser._log("While parsing a string, we found a valid starting doubled quote");
                doubledQuotes = true;
                parser.shift();
            } else {
                // Ok this is not a doubled quote, check if this is an empty string or not
                i = parser.skipWhitespacesAt(1, false);
                nextC = parser.getCharAt(i);
                if (nextC != END && (STRING_DELIMITERS.contains(nextC) ||
                        nextC == '{' || nextC == '[')) {
                    // something fishy is going on here
                    parser._log("While parsing a string, we found a doubled quote but also another quote afterwards, ignoring it");
                    parser.shift();
                    return EMPTY_STRING;
                } else if (",]}".indexOf(nextC) == -1) {
                    parser._log("While parsing a string, we found a doubled quote but it was a mistake, removing one quote");
                    parser.shift();
                }
            }
        }

        // Initialize our return value
        StringBuilder stringAcc = new StringBuilder();

        // Here things get a bit hairy because a string missing the final quote can also be a key or a value in an object
        // In that case we need to use the ":|,|}" characters as terminators of the string
        // So this will stop if:
        // * It finds a closing quote
        // * It iterated over the entire sequence
        // * If we are fixing missing quotes in an object, when it finds the special terminators
        ch = parser.getCharAt();
        boolean unmatchedDelimiter = false;
        while (notIn(ch, END, rstringDelimiter)) {
            if (missingQuotes) {
                if (parser.getCurrentContext() == OBJECT_KEY && (ch == ':' || Character.isWhitespace(ch))) {
                    parser._log("While parsing a string missing the left delimiter in object key context, we found a :, stopping here");
                    break;
                } else if (parser.getCurrentContext() == ARRAY && (in(ch, ']', ','))) {
                    parser._log("While parsing a string missing the left delimiter in array context, we found a ] or ,, stopping here");
                    break;
                }
            }

            if (!parser.isStreamStable() &&
                    parser.getCurrentContext() == OBJECT_VALUE &&
                    (in(ch, ',', '}')) &&
                    (stringAcc.length() == 0 || StringOps.last(stringAcc) != rstringDelimiter)
            ) {
                boolean rstringDelimiterMissing = true;
                // check if this is a case in which the closing comma is NOT missing instead
                parser.skipWhitespacesAt();
                if (parser.getCharAt(1) == '\\') {
                    // Ok this is a quoted string, skip
                    rstringDelimiterMissing = false;
                }
                int i = parser.skipToCharacter(rstringDelimiter, 1);
                char nextC = parser.getCharAt(i);
                if (nextC != END) {
                    i++;
                    // found a delimiter, now we need to check that is followed strictly by a comma or brace
                    // or the string ended
                    i = parser.skipWhitespacesAt(i, false);
                    nextC = parser.getCharAt(i);
                    if (in(nextC, END, ',', '}')) {
                        rstringDelimiterMissing = false;
                    } else {
                        // OK but this could still be some garbage at the end of the string
                        // So we need to check if we find a new lstring_delimiter afterwards
                        // If we do, maybe this is a missing delimiter
                        i = parser.skipToCharacter(lstringDelimiter, i);
                        nextC = parser.getCharAt(i);
                        if (nextC == END) {
                            rstringDelimiterMissing = false;
                        } else {
                            // But again, this could just be something a bit stupid like "lorem, "ipsum" sic"
                            // Check if we find a : afterwards (skipping space)
                            i = parser.skipWhitespacesAt(i + 1, false);
                            nextC = parser.getCharAt(i);
                            if (nextC != END && nextC != ':') {
                                rstringDelimiterMissing = false;
                            }
                        }
                    }
                } else {
                    // There could be a case in which even the next key:value is missing delimiters
                    // because it might be a systemic issue with the output
                    // So let's check if we can find a : in the string instead
                    i = parser.skipToCharacter(':', 1);
                    nextC = parser.getCharAt(i);
                    if (nextC != END) {
                        // OK then this is a systemic issue with the output
                        break;
                    } else {
                        // skip any whitespace first
                        i = parser.skipWhitespacesAt(1, false);
                        // We couldn't find any rstring_delimeter before the end of the string
                        // check if this is the last string of an object and therefore we can keep going
                        // make an exception if this is the last char before the closing brace
                        int j = parser.skipToCharacter('}', i);
                        if (j - i > 1) {
                            // # Ok it's not right after the comma
                            //                        # Let's ignore
                            rstringDelimiterMissing = false;
                        }
                        // Check that j was not out of bound
                        else if (parser.getCharAt(j) != END) {
                            for (int k = stringAcc.length() - 1; k >= 0; k--) {
                                if (stringAcc.charAt(k) == '{') {
                                    // Ok then this is part of the string
                                    rstringDelimiterMissing = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (rstringDelimiterMissing) {
                    parser._log("While parsing a string missing the left delimiter in object value context, we found a , or } and we couldn't determine that a right delimiter was present. Stopping here");
                    break;
                }
            }

            if (!parser.isStreamStable() && ch == ']'
                    && parser.getContext().contains(ARRAY)
                    && StringOps.last(stringAcc) != rstringDelimiter
            ) {
                // We found the end of an array and we are in array context
                // So let's check if we find a rstring_delimiter forward otherwise end early
                int i = parser.skipToCharacter(rstringDelimiter);
                if (parser.getCharAt(i) == END) {
                    // No delimiter found
                    break;
                }
            }

            // Additional logic would go here
            stringAcc.append(ch);
            parser.shift();
            ch = parser.getCharAt();

            // Unclosed string ends with a \ character. This character is ignored if stream_stable = True.
            if (parser.isStreamStable() && ch != END && StringOps.last(stringAcc) == '\\') {
                stringAcc.setLength(stringAcc.length() - 1);
            }
            if (ch != END && StringOps.last(stringAcc) == '\\') {
                // This is a special case, if people use real strings this might happen
                parser._log("Found a stray escape sequence, normalizing it");
                if (in(ch, rstringDelimiter, 't', 'n', 'r', 'b', '\\')) {
                    StringOps.setLast(stringAcc, ESCAPE_SEQS.getOrDefault(ch, ch));
                    parser.shift();
                    ch = parser.getCharAt();
                    while (ch != END && StringOps.last(stringAcc) == '\\' && in(ch, rstringDelimiter, '\\')) {
                        // this is a bit of a special case, if I don't do this it will close the loop or create a train of \\
                        // I don't love it though
                        StringOps.setLast(stringAcc, ch);
                        parser.shift();
                        ch = parser.getCharAt();
                    }
                    continue;
                } else if (in(ch, 'u', 'x')) {
                    // If we find a unicode escape sequence, normalize it
                    int numChars = ch == 'u' ? 4 : 2;
                    String nextChars = parser.getJsonStr().substring(parser.getIndex() + 1, parser.getIndex() + 1 + numChars);
                    if (nextChars.length() == numChars) {
                        if (nextChars.chars().allMatch(c -> "0123456789abcdefABCDEF".indexOf(c) != -1)) {
                            parser._log("Found a unicode escape sequence, normalizing it");
                            StringOps.setLast(stringAcc, (char) Integer.parseInt(nextChars, 16));
                            parser.shift(1 + numChars);
                            ch = parser.getCharAt();
                            continue;
                        }
                    }
                } else if (STRING_DELIMITERS.contains(ch) && ch != rstringDelimiter) {
                    parser._log("Found a delimiter that was escaped but shouldn't be escaped, removing the escape");
                    StringOps.setLast(stringAcc, ch);
                    parser.shift();
                    ch = parser.getCharAt();
                    continue;
                }
            }
            // If we are in object key context and we find a colon, it could be a missing right quote
            if (ch == ':' && !missingQuotes && parser.getCurrentContext() == OBJECT_KEY) {
                // Ok now we need to check if this is followed by a value like "..."
                int i = parser.skipToCharacter(lstringDelimiter, 1);
                char nextC = parser.getCharAt(i);
                if (nextC != END) {
                    i += 1;
                    // found the first delimiter
                    i = parser.skipToCharacter(rstringDelimiter, i);
                    nextC = parser.getCharAt(i);
                    if (nextC != END) {
                        // found a second delimiter
                        i += 1;
                        // Skip spaces
                        i = parser.skipWhitespacesAt(i, false);
                        nextC = parser.getCharAt(i);
                        if (in(nextC, ',', '}')) {
                            // Ok then this is a missing right quote
                            parser._log("While parsing a string missing the right delimiter in object key context, we found a :, stopping here");
                            break;
                        }
                    }
                } else {
                    parser._log("While parsing a string missing the right delimiter in object key context, we found a :, stopping here");
                    break;
                }
            }
            // ChatGPT sometimes forget to quote stuff in html tags or markdown, so we do this whole thing here
            if (ch == rstringDelimiter && StringOps.last(stringAcc) != '\\') {
                // Special case here, in case of double quotes one after another
                if (doubledQuotes && parser.getCharAt(1) == rstringDelimiter) {
                    parser._log("While parsing a string, we found a doubled quote, ignoring it");
                    parser.shift();
                } else if (missingQuotes && parser.getCurrentContext() == OBJECT_VALUE) {
                    // In case of missing starting quote I need to check if the delimeter is the end or the beginning of a key
                    int i = 1;
                    char nextC = parser.getCharAt(i);
                    while (!notIn(nextC, END, rstringDelimiter, lstringDelimiter)) {
                        i++;
                        nextC = parser.getCharAt(i);
                    }
                    if (nextC != END) {
                        i++;
                        i = parser.skipWhitespacesAt(i, false);
                        nextC = parser.getCharAt(i);
                        if (nextC == ':') {
                            parser.shift(-1);
                            ch = parser.getCharAt();
                            parser._log("In a string with missing quotes and object value context, I found a delimeter but it turns out it was the beginning on the next key. Stopping here.");
                            break;
                        }
                    }
                } else if (unmatchedDelimiter) {
                    unmatchedDelimiter = false;
                    stringAcc.append(ch);
                    parser.shift();
                    ch = parser.getCharAt();
                } else {
                    // Check if eventually there is a rstring delimiter, otherwise we bail
                    int i = 1;
                    char nextC = parser.getCharAt(i);
                    boolean checkCommaInObjectValue = true;
                    while (nextC != END && nextC != rstringDelimiter && nextC != lstringDelimiter) {
                        // This is a bit of a weird workaround, essentially in object_value context we don't always break on commas
                        // This is because the routine after will make sure to correct any bad guess and this solves a corner case
                        if (checkCommaInObjectValue && Character.isLetterOrDigit(nextC)) {
                            checkCommaInObjectValue = false;
                        }
                        // If we are in an object context, let's check for the right delimiters
                        if (parser.getContext().contains(OBJECT_KEY) && in(nextC, ':', '}') ||
                                parser.getContext().contains(OBJECT_VALUE) && nextC == '}' ||
                                parser.getContext().contains(ARRAY) && in(nextC, ']', ',') ||
                                checkCommaInObjectValue && parser.getCurrentContext() == OBJECT_VALUE && nextC == ','
                        ) {
                            break;
                        }
                        i++;
                        nextC = parser.getCharAt(i);
                    }
                    // If we stopped for a comma in object_value context, let's check if find a "} at the end of the string
                    if (nextC == ',' && parser.getCurrentContext() == OBJECT_VALUE) {
                        i++;
                        i = parser.skipToCharacter(rstringDelimiter, i);
                        nextC = parser.getCharAt(i);
                        // Ok now I found a delimiter, let's skip whitespaces and see if next we find a } or a ,
                        i++;
                        i = parser.skipWhitespacesAt(i, false);
                        nextC = parser.getCharAt(i);
                        if (nextC == '}' || nextC == ',') {
                            parser._log("While parsing a string, we a misplaced quote that would have closed the string but has a different meaning here, ignoring it");
                            stringAcc.append(ch);
                            parser.shift();
                            ch = parser.getCharAt();
                            continue;
                        }
                    } else if (nextC == rstringDelimiter && parser.getCharAt(i - 1) != '\\') {
                        // Check if self.index:self.index+i is only whitespaces, break if that's the case
                        boolean allSpace = true;
                        for (int j = 1; j < i; j++) {
                            if (!Character.isWhitespace(parser.getCharAt(j))) {
                                allSpace = false;
                                break;
                            }
                        }
                        if (allSpace) {
                            break;
                        }
                        if (parser.getCurrentContext() == OBJECT_VALUE) {
                            i = parser.skipWhitespacesAt(i + 1, false);
                            if (parser.getCharAt(i) == ',') {
                                // So we found a comma, this could be a case of a single quote like "va"lue",
                                // Search if it's followed by another key, starting with the first delimeter
                                i = parser.skipToCharacter(lstringDelimiter, i + 1);
                                i++;
                                i = parser.skipToCharacter(rstringDelimiter, i + 1);
                                i++;
                                i = parser.skipWhitespacesAt(i, false);
                                nextC = parser.getCharAt(i);
                                if (nextC == ':') {
                                    parser._log("\"While parsing a string, we a misplaced quote that would have closed the string but has a different meaning here, ignoring it");
                                    stringAcc.append(ch);
                                    parser.shift();
                                    ch = parser.getCharAt();
                                    continue;
                                }
                            }
                            // We found a delimiter and we need to check if this is a key
                            // so find a rstring_delimiter and a colon after
                            i = parser.skipToCharacter(rstringDelimiter, i + 1);
                            i++;
                            nextC = parser.getCharAt(i);
                            while (notIn(nextC, END, ':')) {
                                if (in(nextC, ',', '}', ']') || nextC == rstringDelimiter && parser.getCharAt(i - 1) != '\\') {
                                    break;
                                }
                                i++;
                                nextC = parser.getCharAt(i);
                            }
                            // Only if we fail to find a ':' then we know this is misplaced quote
                            if (nextC != ':') {
                                parser._log("While parsing a string, we a misplaced quote that would have closed the string but has a different meaning here, ignoring it");
                                unmatchedDelimiter = !unmatchedDelimiter;
                                stringAcc.append(ch);
                                parser.shift();
                                ch = parser.getCharAt();
                            }
                        } else if (parser.getCurrentContext() == ARRAY) {
                            // Let's check if after this quote there are two quotes in a row followed by a comma or a closing bracket
                            char[] limit = new char[]{rstringDelimiter, ']'};
                            i = parser.skipToCharacter(limit, i + 1);
                            nextC = parser.getCharAt(i);
                            boolean evenDelimiters = nextC != END && nextC == rstringDelimiter;
                            while (evenDelimiters && nextC != END && nextC == rstringDelimiter) {
                                i = parser.skipToCharacter(limit, i + 1);
                                i = parser.skipToCharacter(limit, i + 1);
                                nextC = parser.getCharAt(i);
                            }
                            if (evenDelimiters && nextC != ']') {
                                // If we got up to here it means that this is a situation like this:
                                // ["bla bla bla "puppy" bla bla bla "kitty" bla bla"]
                                // So we need to ignore this quote
                                parser._log("While parsing a string in Array context, we detected a quoted section that would have closed the string but has a different meaning here, ignoring it");
                                unmatchedDelimiter = !unmatchedDelimiter;
                                stringAcc.append(ch);
                                parser.shift();
                                ch = parser.getCharAt();
                            } else {
                                break;
                            }
                        } else if (parser.getCurrentContext() == OBJECT_KEY) {
                            // In this case we just ignore this and move on
                            parser._log("While parsing a string in Object Key context, we detected a quoted section that would have closed the string but has a different meaning here, ignoring it");
                            stringAcc.append(ch);
                            parser.shift();
                            ch = parser.getCharAt();
                        }
                    }
                }

            }
        }

        if (ch != END && missingQuotes && parser.getCurrentContext() == OBJECT_KEY && Character.isWhitespace(ch)) {
            parser._log("While parsing a string, handling an extreme corner case in which the LLM added a comment instead of valid string, invalidate the string and return an empty value");
            parser.skipWhitespacesAt();
            char c = parser.getCharAt();
            if (c != ':' && c != ',') {
                return EMPTY_STRING; // TODO or missing ?
            }
        }

        // A fallout of the previous special case in the while loop,
        // we need to update the index only if we had a closing quote
        if (ch != rstringDelimiter) {
            // if stream_stable = True, unclosed strings do not trim trailing whitespace characters
            if (!parser.isStreamStable()) {
                parser._log("While parsing a string, we missed the closing quote, ignoring");
                StringOps.rstrip(stringAcc);
            }
        } else {
            parser.shift();
        }

        if (!parser.isStreamStable() && (missingQuotes || StringOps.last(stringAcc) == '\n')) {
            // Clean the whitespaces for some corner cases
            StringOps.rstrip(stringAcc);
        }
        return FACTORY.textNode(stringAcc.toString());
    }
}
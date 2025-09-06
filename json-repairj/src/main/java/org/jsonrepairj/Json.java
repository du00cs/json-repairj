package org.jsonrepairj;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

public class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            // jackson will stop at first complete json, and we need to parse multiple
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public static JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    public final static TextNode EMPTY_STRING = FACTORY.textNode("");
}

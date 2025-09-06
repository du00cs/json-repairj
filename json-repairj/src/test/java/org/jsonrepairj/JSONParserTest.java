package org.jsonrepairj;

import org.assertj.core.api.Assertions;
import org.jsonrepairj.parser.JSONParser;
import org.junit.jupiter.api.Test;

class JSONParserTest {

    @Test
    void skipToCharacter() {
        JSONParser parser = new JSONParser("{\"key:\"value\"}", false);
        Assertions.assertThat(parser.skipToCharacter('"', 1)).isEqualTo(1);
        parser.setIndex(5);
        Assertions.assertThat(parser.skipToCharacter('"', 2)).isEqualTo(7);
    }
}
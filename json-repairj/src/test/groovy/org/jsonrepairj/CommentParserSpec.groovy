package org.jsonrepairj

import spock.lang.Specification

class CommentParserSpec extends Specification {
    def "test_parse_comment"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                             | expected
        "/"                                                               | ""
        '/* comment */ {"key": "value"}'                                  | '{"key":"value"}'
        '{ "key": { "key2": "value2" // comment }, "key3": "value3" }'    | '{"key":{"key2":"value2"},"key3":"value3"}'
        '{ "key": { "key2": "value2" # comment }, "key3": "value3" }'     | '{"key":{"key2":"value2"},"key3":"value3"}'
        '{ "key": { "key2": "value2" /* comment */ }, "key3": "value3" }' | '{"key":{"key2":"value2"},"key3":"value3"}'
        '[ "value", /* comment */ "value2" ]'                             | '["value","value2"]'
        '{ "key": "value" /* comment'                                     | '{"key":"value"}'
    }
}

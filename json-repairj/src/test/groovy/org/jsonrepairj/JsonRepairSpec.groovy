package org.jsonrepairj

import spock.lang.Specification

class JsonRepairSpec extends Specification {
    def "test_valid_json"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                             | expected
        '{"name": "John", "age": 30, "city": "New York"}' | '{"name":"John","age":30,"city":"New York"}'
        '{"employees":["John", "Anna", "Peter"]} '        | '{"employees":["John","Anna","Peter"]}'
        '{"key": "value:value"}'                          | '{"key":"value:value"}'
        '{"text": "The quick brown fox,"}'                | '{"text":"The quick brown fox,"}'
        '{"text": "The quick brown fox won\'t jump"}'     | '{"text":"The quick brown fox won\'t jump"}'
        '{"key": ""}'                                     | '{"key":""}'
        '{"key1": {"key2": [1, 2, 3]}}'                   | '{"key1":{"key2":[1,2,3]}}'
        '{"key": 12345678901234567890}'                   | '{"key":12345678901234567890}'
        '{"key": "value\u263a"}'                          | '{"key":"value\u263a"}'
        '{"key": "value\\nvalue"}'                        | '{"key":"value\\nvalue"}'
        "{'test_中国人_ascii':'统一码'}"                  | '{"test_中国人_ascii":"统一码"}'
    }

    def "test_multiple_jsons"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                                 | expected
        "[]{}"                                                                | "[[],{}]"
        "{}[]{}"                                                              | "[{},[],{}]"
        '{"key":"value"}[1,2,3,True]'                                         | '[{"key":"value"},[1,2,3,true]]'
        'lorem ```json {"key":"value"} ``` ipsum ```json [1,2,3,True] ``` 42' | '[{"key":"value"},[1,2,3,true]]'
        // feels like unnecessary
//        '[{"key":"value"}][{"key":"value_after"}]'                            | '[{"key":"value_after"}]'
    }
}

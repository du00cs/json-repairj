package org.jsonrepairj

import spock.lang.Specification

class ObjectParserSpec extends Specification {
    def "test_parse_object"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                         | expected
        "{}"                                          | "{}"
        '{ "key": "value", "key2": 1, "key3": True }' | '{"key":"value","key2":1,"key3":true}'
        "{"                                           | '{}'
        '{ "key": value, "key2": 1 "key3": null }'    | '{"key":"value","key2":1,"key3":null}'
        "   {  }   "                                  | "{}"
        "{"                                           | "{}"
        "}"                                           | ""
        '{"'                                          | "{}"
    }

    def "test_parse_object_edge_cases"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                                           | expected
        "{foo: [}"                                                                      | '{"foo":[]}'
        "{       "                                                                      | "{}"
        '{"": "value"'                                                                  | '{"":"value"}'
        '{"value_1": true, COMMENT "value_2": "data"}'                                  | '{"value_1":true,"value_2":"data"}'
        '{"value_1": true, SHOULD_NOT_EXIST "value_2": "data" AAAA }'                   | '{"value_1":true,"value_2":"data"}'
        '{"" : true, "key2": "value2"}'                                                 | '{"":true,"key2":"value2"}'
        """{""answer"":[{""traits"":''Female aged 60+'',""answer1"":""5""}]}"""         | '{"answer":[{"traits":"Female aged 60+","answer1":"5"}]}'
        '{ "words": abcdef", "numbers": 12345", "words2": ghijkl" }'                    | '{"words":"abcdef","numbers":12345,"words2":"ghijkl"}'
        """{"number": 1,"reason": "According...""ans": "YES"}"""                        | '{"number":1,"reason":"According...","ans":"YES"}'
        """{ "a" : "{ b": {} }" }"""                                                    | '{"a":"{ b"}'
        """{"b": "xxxxx" true}"""                                                       | '{"b":"xxxxx"}'
        '{"key": "Lorem "ipsum" s,"}'                                                   | '{"key":"Lorem \\"ipsum\\" s,"}'
        '{"lorem": ipsum, sic, datum.",}'                                               | '{"lorem":"ipsum, sic, datum."}'
        '{"lorem": sic tamet. "ipsum": sic tamet, quick brown fox. "sic": ipsum}'       | '{"lorem":"sic tamet.","ipsum":"sic tamet","sic":"ipsum"}'
        '{"lorem_ipsum": "sic tamet, quick brown fox. }'                                | '{"lorem_ipsum":"sic tamet, quick brown fox."}'
        '{"key":value, " key2":"value2" }'                                              | '{"key":"value"," key2":"value2"}'
        '{"key":value "key2":"value2" }'                                                | '{"key":"value","key2":"value2"}'
        "{'text': 'words{words in brackets}more words'}"                                | '{"text":"words{words in brackets}more words"}'
        "{text:words{words in brackets}}"                                               | '{"text":"words{words in brackets}"}'
        "{text:words{words in brackets}m}"                                              | '{"text":"words{words in brackets}m"}'
        '{"key": "value, value2"```'                                                    | '{"key":"value, value2"}'
        "{key:value,key2:value2}"                                                       | '{"key":"value","key2":"value2"}'
        '{"key:"value"}'                                                                | '{"key":"value"}'
        '{"key:value}'                                                                  | '{"key":"value"}'
        '[{"lorem": {"ipsum": "sic"}, """" "lorem": {"ipsum": "sic"}]'                  | '[{"lorem":{"ipsum":"sic"}},{"lorem":{"ipsum":"sic"}}]'
        '{ "key": ["arrayvalue"], ["arrayvalue1"], ["arrayvalue2"], "key3": "value3" }' | '{"key":["arrayvalue","arrayvalue1","arrayvalue2"],"key3":"value3"}'
        '{ "key": ["arrayvalue"], "key3": "value3", ["arrayvalue1"] }'                  | '{"key":["arrayvalue"],"key3":"value3","arrayvalue1":null}'
        '{"key": "{\\\\"key\\\\\\":[\\"value\\\\\\"],\\"key2\\":"value2"}"}'            | '{"key":"{\\"key\\":[\\"value\\"],\\"key2\\":\\"value2\\"}"}'
        '{"key": , "key2": "value2"}'                                                   | '{"key":null,"key2":"value2"}'
    }
}

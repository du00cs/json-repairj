package org.jsonrepairj

import spock.lang.Specification

class ArrayParserSpec extends Specification {
    def "test_parse_array"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input          | expected
        "[]"           | "[]"
        "[1, 2, 3, 4]" | "[1,2,3,4]"
        "["            | "[]"
        "[[1\n\n]"     | "[[1]]"
    }

    def "test_parse_array_edge_cases"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                                                         | expected
        "[{]"                                                                                         | "[]"
        "["                                                                                           | "[]"
        '["'                                                                                          | "[]"
        "]"                                                                                           | ""
        "[1, 2, 3,"                                                                                   | "[1,2,3]"
        "[1, 2, 3, ...]"                                                                              | "[1,2,3]"
        "[1, 2, ... , 3]"                                                                             | "[1,2,3]"
        "[1, 2, '...', 3]"                                                                            | '[1,2,"...",3]'
        "[true, false, null, ...]"                                                                    | "[true,false,null]"
        '["a" "b" "c" 1'                                                                              | '["a","b","c",1]'
        '{"employees":["John", "Anna",'                                                               | '{"employees":["John","Anna"]}'
        '{"employees":["John", "Anna", "Peter'                                                        | '{"employees":["John","Anna","Peter"]}'
        '{"key1": {"key2": [1, 2, 3'                                                                  | '{"key1":{"key2":[1,2,3]}}'
        '{"key": ["value]}'                                                                           | '{"key":["value"]}'
        '["lorem "ipsum" sic"]'                                                                       | '["lorem \\"ipsum\\" sic"]'
        '{"key1": ["value1", "value2"], "key2": ["value3", "value4"]}'                                | '{"key1":["value1","value2"],"key2":["value3","value4"]}'
        '{"key": ["value" "value1" "value2"]}'                                                        | '{"key":["value","value1","value2"]}'
        '{"key": ["lorem "ipsum" dolor "sit" amet, "consectetur" ", "lorem "ipsum" dolor", "lorem"]}' | '{"key":["lorem \\"ipsum\\" dolor \\"sit\\" amet, \\"consectetur\\" ","lorem \\"ipsum\\" dolor","lorem"]}'
        '{"k"e"y": "value"}'                                                                          | '{"k\\"e\\"y":"value"}'
        '["key":"value"}]'                                                                            | '[{"key":"value"}]'
        '[{"key": "value", "key'                                                                      | '[{"key":"value"}]'
        '["value1" value2", "value3"]'                                                                | '["value1","value2","value3"]'
        '{"bad_one":["Lorem Ipsum", "consectetur" comment" ], "good_one":[ "elit", "sed", "tempor"]}' | '{"bad_one":["Lorem Ipsum","consectetur","comment"],"good_one":["elit","sed","tempor"]}'
        '{"bad_one": ["Lorem Ipsum","consectetur" comment],"good_one": ["elit","sed","tempor"]}'      | '{"bad_one":["Lorem Ipsum","consectetur","comment"],"good_one":["elit","sed","tempor"]}'
    }
}

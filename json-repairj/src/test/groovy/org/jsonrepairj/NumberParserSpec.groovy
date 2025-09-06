package org.jsonrepairj

import spock.lang.Specification

class NumberParserSpec extends Specification {
    def "test_parse_number"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input | expected
        "1"   | "1"
        "1.2" | "1.2"
    }

    def "test_parse_number_edge_cases"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                              | expected
        ' - { "test_key": ["test_value", "test_value2"] }' | '{"test_key":["test_value","test_value2"]}'
        '{"key": 1/3}'                                     | '{"key":"1/3"}'
        '{"key": .25}'                                     | '{"key":0.25}'
        '{"here": "now", "key": 1/3, "foo": "bar"}'        | '{"here":"now","key":"1/3","foo":"bar"}'
        '{"key": 12345/67890}'                             | '{"key":"12345/67890"}'
        "[105,12"                                          | "[105,12]"
        '{"key", 105,12,'                                  | '{"key":"105,12"}'
        '{"key": 1/3, "foo": "bar"}'                       | '{"key":"1/3","foo":"bar"}'
        '{"key": 10-20}'                                   | '{"key":"10-20"}'
        '{"key": 1.1.1}'                                   | '{"key":"1.1.1"}'
        "[- "                                              | "[]"
        '{"key": 1. }'                                     | '{"key":1.0}'
        '{"key": 1e10 }'                                   | '{"key":1.0E10}'
        '{"key": 1e }'                                     | '{"key":1}'
        '{"key": 1notanumber }'                            | '{"key":"1notanumber"}'
        "[1, 2notanumber]"                                 | '[1,"2notanumber"]'
    }
}

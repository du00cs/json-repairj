package org.jsonrepairj

import spock.lang.Specification

class BooleanNullParserSpec extends Specification {
    def "test_parse_boolean_or_null"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                           | expected
        "True"                                          | ""
        "False"                                         | ""
        "Null"                                          | ""
        "true"                                          | "true"
        "false"                                         | "false"
        "null"                                          | "null"
        '  {"key": true, "key2": false, "key3": null}'  | '{"key":true,"key2":false,"key3":null}'
        '{"key": TRUE, "key2": FALSE, "key3": Null}   ' | '{"key":true,"key2":false,"key3":null}'
    }
}

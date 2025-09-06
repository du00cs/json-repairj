package org.jsonrepairj


import spock.lang.Specification

class StringParserSpec extends Specification {

    def "test_parse_string"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                   | expected
        '"'                     | ""
        "\n"                    | ""
        " "                     | ""
        "string"                | ""
        "stringbeforeobject {}" | "{}"
    }

    def "test_missing_and_mixed_quotes"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                                  | expected
        "{'key': 'string', 'key2': false, \"key3\": null, \"key4\": unquoted}" | '{"key":"string","key2":false,"key3":null,"key4":"unquoted"}'
        '{"name": "John", "age": 30, "city": "New York'                        | '{"name":"John","age":30,"city":"New York"}'
        '{"name": "John", "age": 30, city: "New York"}'                        | '{"name":"John","age":30,"city":"New York"}'
        '{"name": "John", "age": 30, "city": New York}'                        | '{"name":"John","age":30,"city":"New York"}'
        '{"name": John, "age": 30, "city": "New York"}'                        | '{"name":"John","age":30,"city":"New York"}'
        '{“slanted_delimiter”: "value"}'                                       | '{"slanted_delimiter":"value"}'
        '{"name": "John", "age": 30, "city": "New'                             | '{"name":"John","age":30,"city":"New"}'
        '{"name": "John", "age": 30, "city": "New York, "gender": "male"}'     | '{"name":"John","age":30,"city":"New York","gender":"male"}'
        '[{"key": "value", COMMENT "notes": "lorem "ipsum", sic." }]'          | '[{"key":"value","notes":"lorem \\"ipsum\\", sic."}]'
        '{"key": ""value"}'                                                    | '{"key":"value"}'
        '{"key": "value", 5: "value"}'                                         | '{"key":"value","5":"value"}'
        '{"foo": "\\"bar\\""'                                                  | '{"foo":"\\"bar\\""}'
        '{"" key":"val"'                                                       | '{" key":"val"}'
        '{"key": value "key2" : "value2" '                                     | '{"key":"value","key2":"value2"}'
        '{"key": "lorem ipsum ... "sic " tamet. ...}'                          | '{"key":"lorem ipsum ... \\"sic \\" tamet. ..."}'
        '{"key": value , }'                                                    | '{"key":"value"}'
        '{"comment": "lorem, "ipsum" sic "tamet". To improve"}'                | '{"comment":"lorem, \\"ipsum\\" sic \\"tamet\\". To improve"}'
        '{"key": "v"alu"e"} key:'                                              | '{"key":"v\\"alu\\"e"}'
        '{"key": "v"alue", "key2": "value2"}'                                  | '{"key":"v\\"alue","key2":"value2"}'
        '[{"key": "v"alu,e", "key2": "value2"}]'                               | '[{"key":"v\\"alu,e","key2":"value2"}]'
    }

    def "test_escaping"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                                                                          | expected
        "'\"'"                                                                                                         | ""
        '{"key": \'string"\n\t\\le\''                                                                                  | '{"key":"string\\"\\n\\t\\\\le"}'
        '{"real_content": "Some string: Some other string \\t Some string <a href=\\"https://domain.com\\">Some link</a>"' | '{"real_content":"Some string: Some other string \\t Some string <a href=\\"https://domain.com\\">Some link</a>"}'
        '{"key_1\n": "value"}'                                                                                         | '{"key_1":"value"}'
        '{"key\t_": "value"}'                                                                                          | '{"key\\t_":"value"}'
        "{\"key\": '\u0076\u0061\u006c\u0075\u0065'}"                                                                  | '{"key":"value"}'
        '{"key": "\\u0076\\u0061\\u006C\\u0075\\u0065"}'                                                               | '{"key":"value"}'
        '{"key": "valu\'e"}'                                                                                           | '{"key":"valu\'e"}'
        '{\'key\': "{\\"key\\": 1, \\"key2\\": 1}"}'                                                                   | '{"key":"{\\"key\\": 1, \\"key2\\": 1}"}'
    }

    def "test_markdown"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                           | expected
        '{ "content": "[LINK]("https://google.com")" }' | '{"content":"[LINK](\\"https://google.com\\")"}'
        '{ "content": "[LINK](" }'                      | '{"content":"[LINK]("}'
        '{ "content": "[LINK](", "key": true }'         | '{"content":"[LINK](","key":true}'
    }

    def "test_leading_trailing_characters"() {
        given:

        expect:
        JsonRepair.repairJson(input) == expected

        where:
        input                                                                                          | expected
        '````{ "key": "value" }```'                                                                    | '{"key":"value"}'
        '{    "a": "",    "b": [ { "c": 1} ] \n}```'                                                   | '{"a":"","b":[{"c":1}]}'
        "Based on the information extracted, here is the filled JSON output: ```json { 'a': 'b' } ```" | '{"a":"b"}'
        '\n' +
                '                       The next 64 elements are:\n' +
                '                       ```json\n' +
                '                       { "key": "value" }\n' +
                '                       ```'                                                           | '{"key":"value"}'
    }
}
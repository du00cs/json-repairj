# json-repairj

> Like Rust port [llm_json](https://github.com/oramasearch/llm_json)

A java library to repair broken JSON strings, particularly useful for handling malformed JSON output from Large Language Models.

This is a porting of the Python library [json_repair](https://github.com/mangiucugna/json_repair), written by [Stefano Baccianella](https://github.com/mangiucugna) and published under the MIT license.

All credits go to the original author for the amazing work.

## Setup

```xml
<dependency>
    <groupId>org.json-repairj</groupId>
    <artifactId>json-repairj</artifactId>
    <version>...</version>
</dependency>
```

```groovy
implementation 'org.json-repairj:json-repairj:${jsonRepairjVersion}'
```

## Usage

```java
import org.jsonrepair.JsonRepair;

String result = JsonRepair.repairJson(str);

// or more control (result = jackson node + warnings)
ParseResult result = JsonRepair.parseJson(str, skipLoads, streamStable);
```

## Not Implemented

Some features are not implemented yet:
- identity check in multiple json
- read from file
- stream stable not tested

## License

[MIT](/LICENSE.md)
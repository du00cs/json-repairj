[![Maven Central](https://img.shields.io/maven-central/v/io.github.du00cs/json-repairj.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.du00cs%22%20AND%20a:%22json-repairj%22)
[![Build](https://github.com/du00cs/json-repairj/actions/workflows/test.yml/badge.svg)](https://github.com/du00cs/json-repairj/actions/workflows/test.yml)

# json-repairj

A java library to repair broken JSON strings, particularly useful for handling malformed JSON output from Large Language Models.

This is a porting of the Python library [json_repair](https://github.com/mangiucugna/json_repair), written by [Stefano Baccianella](https://github.com/mangiucugna) and published under the MIT license.

All credits go to the original author for the amazing work.

## Setup

We will try to keep up to tags of the original project, just push/fire an issue if delay.

```xml
<dependency>
    <groupId>io.github.du00cs</groupId>
    <artifactId>json-repairj</artifactId>
    <version>${jsonRepairjVersion}</version>
</dependency>
```

```groovy
implementation 'io.github.du00cs:json-repairj:${jsonRepairjVersion}'
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
package org.jsonrepairj;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/*
 * 使用 https://github.com/HAibiiin/json-repair 中的5个测试用例
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class BenchmarkTests {

    @Param({
            "{\"f\":\"v\", \"f2\":\"v2\"",
            "{\"f\":\"v\", \"a\":[1",
            "{\"f\":\"v\", \"a\":[1,2], \"o1\":{\"f1\":\"v1\"}, ",
            "\"f\":\"v\", \"a\":[1,2], \"o1\":{\"f1\":\"v1\"}",
            "f:v",
            "{\"name\": \"volume_set\", \"arguments\": {\"volume\": \"+\", \"+\", \"position\": \"空\", \"source\": \"空\", \"target\": \"空\"}}"
    })
    String anomalyJSON;

    @Benchmark
    public void testSimpleRepairStrategy(Blackhole blackhole) {
        blackhole.consume(JsonRepair.repairJson(anomalyJSON));
    }
}

package org.jsonrepairj;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FixInfo {
    String text;
    String context;
}

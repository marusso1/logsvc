package com.sevcosecurity.logsvc.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedLogMessage {
    private String dt;
    private String level;
    private String module;
    private String msg;

    public ParsedLogMessage(String rawMsg) {
        final String[] tokens = rawMsg.split(" ", 4);
        switch (tokens.length) {
            case 4: {
                if (tokens[3].startsWith("- "))
                    msg = tokens[3].substring(2);
                else
                    msg = tokens[3];
            }
            case 3: module = tokens[2];
            case 2: level = tokens[1];
            case 1: dt = tokens[0];
        }
    }
}

package com.sevcosecurity.logsvc.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchParams {
    private String start;
    private String end;
    private String level;
    private String module;
    private String message;
}

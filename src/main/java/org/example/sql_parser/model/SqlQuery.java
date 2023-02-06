package org.example.sql_parser.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class SqlQuery {
    private List<SelectEntry> selectEntries;
    private List<Source> fromSources;
    private List<WhereClause> whereClauses;
    private List<SelectEntry> groupByColumns;
    private List<WhereClause> havingClauses;
    private Sort sort;
    private Integer limit;
    private Integer offset;
}

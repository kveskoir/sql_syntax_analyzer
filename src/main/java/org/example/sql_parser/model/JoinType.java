package org.example.sql_parser.model;

import lombok.ToString;

@ToString
public enum JoinType {
    INNER,
    LEFT,
    RIGHT,
    FULL,
}

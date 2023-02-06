package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SqlQuery;

import java.util.function.Consumer;

public interface StateMachineState {
    void process(SqlQuery.SqlQueryBuilder builder, char c, Consumer<StateMachineState> newState);
}

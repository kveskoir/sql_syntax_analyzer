package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SqlQuery;

import java.util.function.Consumer;

public class FinishedState implements StateMachineState {

    @Override
    public void process(SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState) {
        //throw new RuntimeException("SQL reading has been finished!");
    }
}

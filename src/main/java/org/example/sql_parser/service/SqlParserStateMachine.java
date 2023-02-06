package org.example.sql_parser.service;

import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.service.states.SelectState;
import org.example.sql_parser.service.states.StateMachineState;

import javax.management.Query;

public class SqlParserStateMachine implements QueryParser {
    private StateMachineState currentState = new SelectState();

    @Override
    public SqlQuery parseQuery(final String queryString) {
        char[] chars = queryString.toCharArray();
        SqlQuery.SqlQueryBuilder builder = SqlQuery.builder();
        for (final char c : chars) {
            currentState.process(builder, c, newState -> this.currentState = newState);
        }
        return builder.build();
    }

}

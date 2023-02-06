package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.function.Consumer;

public class SelectState implements StateMachineState {
    private StringBuffer buffer = new StringBuffer();

    @Override
    public void process(SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (buffer.toString().equals("SELECT")) {
                newState.accept(new SelectFieldsState());
            } else {
                throw new RuntimeException("Wrong select statement!");
            }
        }
        buffer.append(c);
    }
}

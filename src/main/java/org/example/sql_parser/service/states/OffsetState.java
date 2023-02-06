package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SqlQuery;

import java.util.function.Consumer;

public class OffsetState implements StateMachineState {
    private final StringBuffer numberBuffer = new StringBuffer();
    private Integer number = null;
    private final StringBuffer wordBuffer = new StringBuffer();

    @Override
    public void process(SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState) {
        if (Character.isWhitespace(c)) {
            return;
        }
        if (!Character.isDigit(c)) {
            if (!numberBuffer.isEmpty()) {
                number = Integer.parseInt(numberBuffer.toString());
            }
            if (c == ';' || c == ')') {
                if (number == null && !wordBuffer.isEmpty()) {
                    throw new RuntimeException("Empty OFFSET parameter!");
                }
                newState.accept(new FinishedState());
                sqlBuilder.offset(number);
                return;
            }
            if (Character.isAlphabetic(c)) {
                if (number == null) {
                    throw new RuntimeException("Empty OFFSET parameter!");
                }
                wordBuffer.append(c);
                if (wordBuffer.length() > "LIMIT".length()) {
                    throw new RuntimeException("Wrong OFFSET statement!");
                }
                if (wordBuffer.toString().equalsIgnoreCase("LIMIT")) {
                    newState.accept(new OffsetState());
                    sqlBuilder.offset(number);
                }
                if (wordBuffer.toString().equalsIgnoreCase("ORDER")) {
                    newState.accept(new OrderByState());
                    sqlBuilder.offset(number);
                }
                return;
            }
            throw new RuntimeException("Wrong OFFSET statement!");
        }
        numberBuffer.append(c);
    }
}

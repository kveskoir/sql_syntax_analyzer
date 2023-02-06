package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SqlQuery;

import java.util.function.Consumer;

public class LimitState implements StateMachineState {
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
                    throw new RuntimeException("Empty LIMIT parameter!");
                }
                newState.accept(new FinishedState());
                sqlBuilder.limit(number);
                return;
            }
            if (Character.isAlphabetic(c)) {
                if (number == null) {
                    throw new RuntimeException("Empty LIMIT parameter!");
                }
                wordBuffer.append(c);
                if (wordBuffer.length() > "OFFSET".length()) {
                    throw new RuntimeException("Wrong LIMIT statement!");
                }
                if (wordBuffer.toString().equalsIgnoreCase("OFFSET")) {
                    newState.accept(new OffsetState());
                    sqlBuilder.limit(number);
                }
                if (wordBuffer.toString().equalsIgnoreCase("ORDER")) {
                    newState.accept(new OrderByState());
                    sqlBuilder.limit(number);
                }
                return;
            }
            throw new RuntimeException("Wrong LIMIT statement!");
        }
        numberBuffer.append(c);
    }
}

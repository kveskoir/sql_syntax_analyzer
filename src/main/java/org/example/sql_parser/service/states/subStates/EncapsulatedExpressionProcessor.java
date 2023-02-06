package org.example.sql_parser.service.states.subStates;

import java.util.function.Consumer;

public class EncapsulatedExpressionProcessor {
    private StringBuffer buffer = new StringBuffer("(");
    private int bracketsBalance = 1;

    public void processExpression(
            char c,
            Consumer<String> completeExpression
    ) {
        if (c == '(') {
            bracketsBalance++;
        }
        if (c == ')') {
            bracketsBalance--;
        }
        buffer.append(c);
        if (bracketsBalance == 0) {
            completeExpression.accept(buffer.toString());
        }
    }
}

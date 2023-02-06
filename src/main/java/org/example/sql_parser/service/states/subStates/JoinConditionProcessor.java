package org.example.sql_parser.service.states.subStates;

import org.example.sql_parser.model.Join;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.List;
import java.util.function.Consumer;

public class JoinConditionProcessor {
    private StringBuffer buffer = new StringBuffer();
    private State state = State.OPENING_BRACKET;

    public void processCondition(
            Join.JoinBuilder joinBuilder,
            char c,
            Consumer<Boolean> isConditionComplete
    ) {
        switch (state) {
            case OPENING_BRACKET -> processOpeningBracketState(c);
            case CONDITION -> processConditionState(joinBuilder, c, isConditionComplete);
        }
    }

    private void processOpeningBracketState(
            char c
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == '(') {
                this.state = State.CONDITION;
                resetBuffer();
                return;
            }
            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong join condition syntax!");
        }
        buffer.append(c);
    }

    private void processConditionState(
            Join.JoinBuilder joinBuilder,
            char c,
            Consumer<Boolean> isConditionComplete
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ')') {
                if (buffer.isEmpty()) {
                    throw new RuntimeException("Empty table name!");
                }
                joinBuilder.condition(buffer.toString());
                isConditionComplete.accept(true);
                resetBuffer();
                return;
            }
            if (Character.isWhitespace(c) ||
                    List.of('!', '<', '>', '.', '=').contains(c)
            ) {
                buffer.append(c);
                return;
            }
            throw new RuntimeException("Wrong join condition syntax!");
        }
        buffer.append(c);
    }

    private void processClosingBracketState(
            Join.JoinBuilder joinBuilder,
            char c,
            Consumer<Boolean> subQueryIsFinished
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ')') {
                checkIfBufferIfEmptyAndThrowException("column");
//                selectEntryBuilder.column(buffer.toString());
                subQueryIsFinished.accept(true);
                return;
            }
            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong selected column syntax!");
        }
        buffer.append(c);
    }

    private void resetBuffer() {
        this.buffer = new StringBuffer();
    }

    private void checkIfBufferIfEmptyAndThrowException(final String errorSubject) {
        if (buffer.toString().isEmpty()) {
            throw new RuntimeException(String.format("%s is empty!", errorSubject));
        }
    }

    private enum State {
        OPENING_BRACKET,
        CONDITION,
        CLOSING_BRACKET,
        ;
    }
}

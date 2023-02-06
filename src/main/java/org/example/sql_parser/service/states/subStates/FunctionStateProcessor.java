package org.example.sql_parser.service.states.subStates;

import org.example.sql_parser.model.SelectEntry;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.function.Consumer;

public class FunctionStateProcessor {
    private StringBuffer buffer = new StringBuffer();
    private State state = State.TABLE;

    public void processFunction(
            SelectEntry.SelectEntryBuilder selectEntryBuilder,
            char c,
            Consumer<Boolean> subQueryIsFinished
    ) {
        switch (state) {
            case TABLE -> processTableState(selectEntryBuilder, c);
            case COLUMN -> processColumnState(selectEntryBuilder, c, subQueryIsFinished);
        }
    }

    private void processTableState(
            SelectEntry.SelectEntryBuilder selectEntryBuilder,
            char c
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == '.') {
                if (buffer.isEmpty()) {
                    throw new RuntimeException("Empty table name!");
                }
                this.state = State.COLUMN;
                selectEntryBuilder.table(buffer.toString());
                resetBuffer();
                return;
            }
            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong selected column syntax!");
        }
        buffer.append(c);
    }

    private void processColumnState(
            SelectEntry.SelectEntryBuilder selectEntryBuilder,
            char c,
            Consumer<Boolean> subQueryIsFinished
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ')') {
                checkIfBufferIfEmptyAndThrowException("column");
                selectEntryBuilder.column(buffer.toString());
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
        TABLE,
        COLUMN,
        ;
    }
}


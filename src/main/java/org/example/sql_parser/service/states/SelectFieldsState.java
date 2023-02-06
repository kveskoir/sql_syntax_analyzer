package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SelectEntry;
import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.service.states.subStates.FunctionStateProcessor;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SelectFieldsState implements StateMachineState {
    private StringBuffer buffer = new StringBuffer();
    private State state = State.FIRST_WORD;
    private final List<SelectEntry> selectEntries = new ArrayList<>();
    private SelectEntry.SelectEntryBuilder entryBuilder = SelectEntry.builder();
    private FunctionStateProcessor functionStateProcessor = new FunctionStateProcessor();

    @Override
    public void process(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        switch (state) {
            case FIRST_WORD -> processFirstWordState(sqlBuilder, c, newState);
            case COLUMN -> processColumnState(sqlBuilder, c, newState);
            case FUNCTION -> processFunctionState(sqlBuilder, c, newState);
            case POSSIBLE_ALIAS -> processPossibleAlias(sqlBuilder, c, newState);
            case ALIAS -> processAlias(sqlBuilder, c, newState);
            case END_OF_THE_ENTRY -> processEndOfTheEntry(sqlBuilder, c, newState);
        }
    }

    private void processFirstWordState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == '.') {
                this.state = State.COLUMN;
                if (buffer.isEmpty()) {
                    throw new RuntimeException("Empty table name!");
                }
                entryBuilder.table(buffer.toString());
                resetBuffer();
                return;
            }
            if (c == '(') {
                checkIfBufferIfEmptyAndThrowException("function");
                if (buffer.toString().equalsIgnoreCase("COUNT")) {
                    entryBuilder.function("COUNT");
                    resetBuffer();
                    this.state = State.FUNCTION;
                    return;
                } else if (buffer.toString().equalsIgnoreCase("SUM")) {
                    entryBuilder.function("SUM");
                    resetBuffer();
                    this.state = State.FUNCTION;
                    return;
                }
                throw new RuntimeException("Wrong function syntax!");
            }
            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong selected column syntax!");
        }
        buffer.append(c);
    }

    private void processColumnState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                checkIfBufferIfEmptyAndThrowException("column");
                entryBuilder.column(buffer.toString());
                flushEntry();
                this.state = State.FIRST_WORD;
            }
            if (Character.isWhitespace(c)) {
                this.state = State.POSSIBLE_ALIAS;
                entryBuilder.column(buffer.toString());
                resetBuffer();
                return;
            }
            throw new RuntimeException("Wrong selected column syntax!");
        }
        buffer.append(c);
    }

    private void processFunctionState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        functionStateProcessor.processFunction(entryBuilder, c, isSubQueryFinished -> {
            if (isSubQueryFinished != null && isSubQueryFinished) {
                this.state = State.END_OF_THE_ENTRY;
                this.functionStateProcessor = new FunctionStateProcessor();
                resetBuffer();
            }
        });
    }

    private void processPossibleAlias(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                flushEntry();
                this.state = State.FIRST_WORD;
                return;
            }

            if (Character.isWhitespace(c)) {
                if (!buffer.isEmpty()) {
                    if (buffer.toString().trim().equalsIgnoreCase("AS")) {
                        this.state = State.ALIAS;
                        resetBuffer();
                        return;
                    }
                    if (buffer.toString().trim().equalsIgnoreCase("FROM")) {
                        flushEntry();
                        sqlBuilder.selectEntries(selectEntries);
                        newState.accept(new FromState());
                        return;
                    }
                }
                return;
            }
            throw new RuntimeException("Wrong selected column syntax!");
        }
        buffer.append(c);
    }

    private void processAlias(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                if (buffer.isEmpty()) {
                    throw new RuntimeException("Alias is empty!");
                }
                entryBuilder.alias(buffer.toString());
                flushEntry();
                this.state = State.FIRST_WORD;
                return;
            }
            if (Character.isWhitespace(c)) {
                if (!buffer.isEmpty()) {
                    if (buffer.toString().trim().equalsIgnoreCase("FROM")) {
                        throw new RuntimeException("Alias is empty!");
                    }
                    entryBuilder.alias(buffer.toString());
                    flushEntry();
                    this.state = State.END_OF_THE_ENTRY;
                }
                return;
            }
            throw new RuntimeException("Wrong selected column syntax!");
        }
        buffer.append(c);
    }

    private void processEndOfTheEntry(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                this.state = State.FIRST_WORD;
                flushEntry();
                return;
            }
            if (!buffer.isEmpty()) {
                if (buffer.toString().trim().equalsIgnoreCase("FROM")) {
                    flushEntry();
                    sqlBuilder.selectEntries(selectEntries);
                    newState.accept(new FromState());
                    return;
                }
                throw new RuntimeException("Wrong selected column syntax!");
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

    private void resetBuilder() {
        this.entryBuilder = SelectEntry.builder();
    }

    private void flushEntry() {
        selectEntries.add(entryBuilder.build());
        resetBuffer();
        resetBuilder();
    }

    private void checkIfBufferIfEmptyAndThrowException(final String errorSubject) {
        if (buffer.toString().isEmpty()) {
            throw new RuntimeException(String.format("%s is empty!", errorSubject));
        }
    }

    private void checkIfEntryIfEmptyAndThrowException(final String errorSubject) {
        if (entryBuilder.isEmpty()) {
            throw new RuntimeException(String.format("%s is empty!", errorSubject));
        }
    }

    private enum State {
        FIRST_WORD,
        FUNCTION,
        COLUMN,
        END_OF_THE_ENTRY,
        POSSIBLE_ALIAS,
        ALIAS,
        ;
    }
}

package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SelectEntry;
import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.service.states.subStates.FunctionStateProcessor;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GroupByState implements StateMachineState {
    private StringBuffer buffer = new StringBuffer();
    private State state = State.FIRST_WORD;
    private final List<SelectEntry> groupByEntries = new ArrayList<>();
    private SelectEntry.SelectEntryBuilder entryBuilder = SelectEntry.builder();
    private FunctionStateProcessor functionStateProcessor = new FunctionStateProcessor();
    private boolean isGroupBySentenceValid = false;

    @Override
    public void process(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        switch (state) {
            case FIRST_WORD -> processFirstWordState(sqlBuilder, c, newState);
            case COLUMN -> processColumnState(sqlBuilder, c, newState);
            case FUNCTION -> processFunctionState(sqlBuilder, c, newState);
            case END_OF_THE_ENTRY -> processEndOfTheEntry(sqlBuilder, c, newState);
        }
    }

    private void processFirstWordState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c) && isGroupBySentenceValid) {
            if (c == ',') {
                checkIfBufferIfEmptyAndThrowException("column");
                entryBuilder.column(buffer.toString());
                flushEntry();
                this.state = State.FIRST_WORD;
                return;
            }
            if (Character.isWhitespace(c) && !buffer.isEmpty()) {
                entryBuilder.column(buffer.toString());
                resetBuffer();
                this.state = State.END_OF_THE_ENTRY;
                return;
            }
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
            throw new RuntimeException("Wrong group by column syntax!");
        }
        buffer.append(c);
        if (!isGroupBySentenceValid) {
            if (buffer.length() == 3) {
                if (buffer.toString().trim().equalsIgnoreCase("BY")) {
                    isGroupBySentenceValid = true;
                    resetBuffer();
                    return;
                }
                throw new RuntimeException("Invalid GROUP BY syntax");
            }

        }
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
                return;
            }
            if (Character.isWhitespace(c)) {
                checkIfBufferIfEmptyAndThrowException("column");
                entryBuilder.column(buffer.toString());
                resetBuffer();
                this.state = State.END_OF_THE_ENTRY;
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

    private void processEndOfTheEntry(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                this.state = State.FIRST_WORD;
                flushEntry();
                return;
            }
            if (c == ';' || c == ')') {
                flushEntry();
                newState.accept(new FinishedState());
                sqlBuilder.groupByColumns(groupByEntries);
                return;
            }
            if (!buffer.isEmpty()) {
                if (checkForNewStageTransition(sqlBuilder, newState)) {
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

    private boolean checkForNewStageTransition(
            final SqlQuery.SqlQueryBuilder sqlBuilder, final Consumer<StateMachineState> newState
    ) {
        if (buffer.toString().trim().equalsIgnoreCase("HAVING")) {
            flushEntry();
            sqlBuilder.groupByColumns(groupByEntries);
            newState.accept(new HavingState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("LIMIT")) {
            flushEntry();
            sqlBuilder.groupByColumns(groupByEntries);
            newState.accept(new LimitState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("OFFSET")) {
            flushEntry();
            sqlBuilder.groupByColumns(groupByEntries);
            newState.accept(new OffsetState());
            return true;
        }
        return false;
    }

    private void resetBuffer() {
        this.buffer = new StringBuffer();
    }

    private void resetBuilder() {
        this.entryBuilder = SelectEntry.builder();
    }

    private void flushEntry() {
        groupByEntries.add(entryBuilder.build());
        resetBuffer();
        resetBuilder();
    }

    private void checkIfBufferIfEmptyAndThrowException(final String errorSubject) {
        if (buffer.toString().isEmpty()) {
            throw new RuntimeException(String.format("%s is empty!", errorSubject));
        }
    }

    private enum State {
        FIRST_WORD,
        FUNCTION,
        COLUMN,
        END_OF_THE_ENTRY,
        ;
    }
}

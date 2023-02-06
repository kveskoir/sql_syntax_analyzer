package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SelectEntry;
import org.example.sql_parser.model.Sort;
import org.example.sql_parser.model.SortType;
import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OrderByState implements StateMachineState {
    private StringBuffer buffer = new StringBuffer();
    private State state = State.FIRST_WORD;
    private final List<SelectEntry> orderByEntries = new ArrayList<>();
    private SelectEntry.SelectEntryBuilder entryBuilder = SelectEntry.builder();
    private Sort.SortBuilder sortBuilder = Sort.builder();
    private SortType sortType = null;
    private boolean isOrderBySentenceValid = false;

    @Override
    public void process(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        switch (state) {
            case FIRST_WORD -> processFirstWordState(sqlBuilder, c, newState);
            case COLUMN -> processColumnState(sqlBuilder, c, newState);
            case END_OF_THE_ENTRY -> processEndOfTheEntry(sqlBuilder, c, newState);
        }
    }

    private void processFirstWordState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c) && isOrderBySentenceValid) {
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
            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong group by column syntax!");
        }
        buffer.append(c);
        if (!isOrderBySentenceValid) {
            if (buffer.length() == 3) {
                if (buffer.toString().trim().equalsIgnoreCase("BY")) {
                    isOrderBySentenceValid = true;
                    resetBuffer();
                    return;
                }
                throw new RuntimeException("Invalid ORDER BY syntax");
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

    private void processEndOfTheEntry(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                if (sortType != null) {
                    throw new RuntimeException("Invalid syntax at ORDER BY clause");
                }
                this.state = State.FIRST_WORD;
                flushEntry();
                return;
            }
            if (c == ';' || c == ')') {
                if (sortType == null) {
                    throw new RuntimeException("Empty sorting type!");
                }
                flushEntry();
                newState.accept(new FinishedState());
                sortBuilder.sortingFields(orderByEntries);
                sortBuilder.type(sortType);
                sqlBuilder.sort(sortBuilder.build());
                return;
            }
            if (!buffer.isEmpty()) {
                if (sortType == null) {
                    if (checkForSortType()) {
                        return;
                    }
                }
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

    private boolean checkForSortType() {
        if (buffer.toString().trim().equalsIgnoreCase("DESC")) {
            sortType = SortType.DESC;
            resetBuffer();
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("ASC")) {
            sortType = SortType.ASC;
            resetBuffer();
            return true;
        }
        return false;
    }

    private boolean checkForNewStageTransition(
            final SqlQuery.SqlQueryBuilder sqlBuilder, final Consumer<StateMachineState> newState
    ) {
        if (buffer.toString().trim().equalsIgnoreCase("LIMIT")) {
            flushEntry();
            sortBuilder.sortingFields(orderByEntries);
            sortBuilder.type(sortType);
            sqlBuilder.sort(sortBuilder.build());
            newState.accept(new LimitState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("OFFSET")) {
            flushEntry();
            sortBuilder.sortingFields(orderByEntries);
            sortBuilder.type(sortType);
            sqlBuilder.sort(sortBuilder.build());
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
        orderByEntries.add(entryBuilder.build());
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
        COLUMN,
        END_OF_THE_ENTRY,
        ;
    }
}

package org.example.sql_parser.service.states;

import org.example.sql_parser.model.Join;
import org.example.sql_parser.model.JoinType;
import org.example.sql_parser.model.Source;
import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.service.states.subStates.JoinConditionProcessor;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FromState implements StateMachineState {
    private StringBuffer buffer = new StringBuffer();
    private State state = State.NEW_ENTRY;
    private final List<Source> sourceEntries = new ArrayList<>();
    private final List<Join> joins = new ArrayList<>();
    private Source.SourceBuilder entryBuilder = Source.builder();
    private Join.JoinBuilder joinBuilder = Join.builder();
    private JoinConditionProcessor joinConditionProcessor = new JoinConditionProcessor();

    private StateMachineState subQueryState = new SelectState();
    SqlQuery.SqlQueryBuilder subQueryBuilder = SqlQuery.builder();

    @Override
    public void process(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        switch (state) {
            case NEW_ENTRY -> processNewEntryState(sqlBuilder, c, newState);
            case SUBQUERY -> processSubQueryState(sqlBuilder, c, newState);
            case POSSIBLE_ALIAS_OR_JOIN -> processPossibleAliasOrJoinState(sqlBuilder, c, newState);
            case POSSIBLE_JOIN -> processPossibleJoinState(sqlBuilder, c, newState);
            case ALIAS -> processAliasState(sqlBuilder, c, newState);
            case JOIN -> processJoinState(sqlBuilder, c, newState);
            case OUTER_JOIN -> processOuterJoinState(sqlBuilder, c, newState);
            case JOIN_CONDITION -> processJoinConditionState(sqlBuilder, c, newState);
        }
    }

    private void processNewEntryState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (Character.isWhitespace(c)) {
            if (!buffer.isEmpty()) {
                this.state = State.POSSIBLE_ALIAS_OR_JOIN;
                entryBuilder.tableName(buffer.toString());
                resetBuffer();
            }
            return;
        }
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ';' || c == ')') {
                if (buffer.isEmpty()) {
                    throw new RuntimeException("Wrong FROM statement!");
                }
                entryBuilder.tableName(buffer.toString());
                flushEntry();
                sqlBuilder.fromSources(sourceEntries);
                newState.accept(new FinishedState());
                return;
            }
            if (c == ',') {
                checkIfBufferIfEmptyAndThrowException("table name");
                this.state = State.NEW_ENTRY;
                entryBuilder.tableName(buffer.toString());
                flushEntry();
                return;
            }
            if (c == '(') {
                if (buffer.isEmpty()) {
                    this.state = State.SUBQUERY;
                    resetBuffer();
                    return;
                }
                throw new RuntimeException("Wrong FROM entry!");
            }
            if (Character.isWhitespace(c)) {
                if (!buffer.isEmpty()) {
                    buffer.append(c);
                }
                return;
            }
            throw new RuntimeException("Wrong FROM statement!");
        }
        if (!buffer.isEmpty() && Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
            this.state = State.POSSIBLE_ALIAS_OR_JOIN;
            entryBuilder.tableName(buffer.toString());
            resetBuffer();
            buffer.append(c);
            return;
        }
        buffer.append(c);
    }

    private void processSubQueryState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        subQueryState.process(subQueryBuilder, c, subQueryState -> this.subQueryState = subQueryState);
        if (subQueryState instanceof FinishedState) {
            this.state = State.POSSIBLE_ALIAS_OR_JOIN;
            if (joins.isEmpty()) {
                entryBuilder.query(subQueryBuilder.build());
            } else {
                joinBuilder.query(subQueryBuilder.build());
            }
            subQueryState = new SelectState();
            subQueryBuilder = SqlQuery.builder();
        }
    }

    private void processPossibleJoinState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ';' || c == ')') {
                flushEntry();
                sqlBuilder.fromSources(sourceEntries);
                newState.accept(new FinishedState());
                return;
            }
            if (!buffer.isEmpty()) {
                if (!joinBuilder.isEmpty()) {
                    if (checkForJoinCondition()) {
                        this.state = State.JOIN_CONDITION;
                        resetBuffer();
                        return;
                    }
                    throw new RuntimeException("Empty join condition!");
                }
                if (checkForJoins()) {
                    return;
                }
                if (checkForNewStageTransition(sqlBuilder, newState)) {
                    return;
                }
            } else if (c == ',') {
                flushEntry();
                this.state = State.NEW_ENTRY;
                return;
            }

            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong FROM statement!");
        }
        buffer.append(c);
    }

    private void processPossibleAliasOrJoinState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (!buffer.isEmpty()) {
                if (buffer.toString().trim().equalsIgnoreCase("AS")) {
                    this.state = State.ALIAS;
                    resetBuffer();
                    return;
                }
            }
        }
        processPossibleJoinState(sqlBuilder, c, newState);
    }

    private void processOuterJoinState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (!buffer.isEmpty()) {
                final String possibleJoin = buffer.toString().trim();
                if (possibleJoin.equalsIgnoreCase("OUTER")) {
                    buffer.append('_');
                    return;
                }
                if (possibleJoin.equalsIgnoreCase("JOIN")
                        || possibleJoin.equalsIgnoreCase("OUTER_JOIN")
                ) {
                    this.state = State.JOIN;
                    resetBuffer();
                    return;
                }
            }
            if (Character.isWhitespace(c)) {
                return;
            }
            throw new RuntimeException("Wrong FROM statement!");
        }
        buffer.append(c);
    }

    private void processJoinState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == '(') {
                if (buffer.isEmpty()) {
                    this.state = State.SUBQUERY;
                    resetBuffer();
                    return;
                }
                throw new RuntimeException("Wrong FROM entry!");
            }
            if (Character.isWhitespace(c)) {
                if (!buffer.isEmpty()) {
                    this.state = State.POSSIBLE_ALIAS_OR_JOIN;
                    joinBuilder.tableName(buffer.toString());
                    resetBuffer();
                }
                return;
            }
            throw new RuntimeException("Wrong FROM statement!");
        }
        buffer.append(c);
    }

    private void processJoinConditionState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        joinConditionProcessor.processCondition(joinBuilder, c, isConditionCompleted -> {
            if (isConditionCompleted != null && isConditionCompleted) {
                this.state = State.POSSIBLE_JOIN;
                flushJoin();
            }
        });
    }

    private void processAliasState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ',') {
                if (buffer.isEmpty()) {
                    throw new RuntimeException("Alias is empty!");
                }
                if (joinBuilder.isEmpty()) {
                    entryBuilder.alias(buffer.toString());
                } else {
                    joinBuilder.alias(buffer.toString());
                }
                flushEntry();
                this.state = State.NEW_ENTRY;
                return;
            }
            if (Character.isWhitespace(c)) {
                if (!buffer.isEmpty()) {
                    if (joinBuilder.isEmpty()) {
                        entryBuilder.alias(buffer.toString());
                    } else {
                        joinBuilder.alias(buffer.toString());
                    }
                    resetBuffer();
                    this.state = State.POSSIBLE_JOIN;
                }
                return;
            }
            throw new RuntimeException("Wrong alias syntax!");
        }
        buffer.append(c);
    }

    ///////////////////////////////
    ///         UTILITY         ///
    ///////////////////////////////
    private void resetBuffer() {
        this.buffer = new StringBuffer();
    }


    private void flushEntry() {
        entryBuilder.joins(List.copyOf(joins));
        joins.clear();
        sourceEntries.add(entryBuilder.build());
        this.entryBuilder = Source.builder();
        resetBuffer();
    }

    private void flushJoin() {
        joins.add(joinBuilder.build());
        this.joinConditionProcessor = new JoinConditionProcessor();
        this.joinBuilder = Join.builder();
        resetBuffer();
    }

    private void checkIfBufferIfEmptyAndThrowException(final String errorSubject) {
        if (buffer.toString().isEmpty()) {
            throw new RuntimeException(String.format("%s is empty!", errorSubject));
        }
    }

    private boolean checkForJoins() {
        if (buffer.toString().trim().equalsIgnoreCase("JOIN")) {
            this.state = State.JOIN;
            joinBuilder.joinType(JoinType.INNER);
            resetBuffer();
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("LEFT")) {
            this.state = State.OUTER_JOIN;
            joinBuilder.joinType(JoinType.LEFT);
            resetBuffer();
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("RIGHT")) {
            this.state = State.OUTER_JOIN;
            joinBuilder.joinType(JoinType.RIGHT);
            resetBuffer();
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("FULL")) {
            this.state = State.OUTER_JOIN;
            joinBuilder.joinType(JoinType.FULL);
            resetBuffer();
            return true;
        }
        return false;
    }

    private boolean checkForJoinCondition() {
        if (buffer.toString().trim().equalsIgnoreCase("ON")) {
            this.state = State.JOIN_CONDITION;
            resetBuffer();
            return true;
        }
        return false;
    }

    private boolean checkForNewStageTransition(
            final SqlQuery.SqlQueryBuilder sqlBuilder, final Consumer<StateMachineState> newState
    ) {
        if (buffer.toString().trim().equalsIgnoreCase("WHERE")) {
            flushEntry();
            sqlBuilder.fromSources(sourceEntries);
            newState.accept(new WhereState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("GROUP")) {
            flushEntry();
            sqlBuilder.fromSources(sourceEntries);
            newState.accept(new GroupByState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("ORDER")) {
            flushEntry();
            sqlBuilder.fromSources(sourceEntries);
            newState.accept(new OrderByState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("LIMIT")) {
            flushEntry();
            sqlBuilder.fromSources(sourceEntries);
            newState.accept(new LimitState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("OFFSET")) {
            flushEntry();
            sqlBuilder.fromSources(sourceEntries);
            newState.accept(new OffsetState());
            return true;
        }
        return false;
    }

    private enum State {
        NEW_ENTRY,
        SUBQUERY,
        POSSIBLE_ALIAS_OR_JOIN,
        POSSIBLE_JOIN,
        ALIAS,
        JOIN,
        OUTER_JOIN,
        JOIN_CONDITION,
        ;
    }
}

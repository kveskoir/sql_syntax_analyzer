package org.example.sql_parser.service.states;

import org.example.sql_parser.model.BooleanOperator;
import org.example.sql_parser.model.SqlQuery;
import org.example.sql_parser.model.WhereClause;
import org.example.sql_parser.service.states.subStates.EncapsulatedExpressionProcessor;
import org.example.sql_parser.service.utility.SyntaxUtility;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public abstract class ConditionStateBase implements StateMachineState {
    protected StringBuffer buffer = new StringBuffer();
    protected StringBuffer currentExpressionBuffer = new StringBuffer();
    protected State state = State.NEW_ENTRY;
    protected final List<WhereClause> whereClauseList = new LinkedList<>();
    protected WhereClause.WhereClauseBuilder whereClauseBuilder = WhereClause.builder();
    protected EncapsulatedExpressionProcessor expressionProcessor = new EncapsulatedExpressionProcessor();

    @Override
    public void process(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        //This analyzer could be enriched with detailed operations supports, but it will take a lot of time
        switch (state) {
            case NEW_ENTRY -> processNewEntryState(sqlBuilder, c, newState);
            case ENCAPSULATED_EXPRESSION -> processEncapsulatedExpressionState(c);
        }
    }

    private void processNewEntryState(
            SqlQuery.SqlQueryBuilder sqlBuilder, char c, Consumer<StateMachineState> newState
    ) {
        if (!SyntaxUtility.isAcceptableCharacter(c)) {
            if (c == ';' || c == ')') {
                if (!currentExpressionBuffer.isEmpty()) {
                    whereClauseBuilder.expression(currentExpressionBuffer.toString());
                    flushEntry();
                    newState.accept(new FinishedState());
                    populateCondition(sqlBuilder);
                    return;
                }
                throw new RuntimeException("Empty WHERE expression!");
            }
            if (c == '(') {
                this.state = State.ENCAPSULATED_EXPRESSION;
                currentExpressionBuffer.append(buffer);
                resetBuffer();
                return;
            }
            checkForOperators();
            if (checkForNewStageTransition(sqlBuilder, newState)) {
                return;
            }
            currentExpressionBuffer.append(buffer);
            currentExpressionBuffer.append(c);
            resetBuffer();
            return;
        }
        buffer.append(c);
    }

    private void processEncapsulatedExpressionState(char c) {
        expressionProcessor.processExpression(c, completeExpression -> {
            if (completeExpression != null) {
                this.state = State.NEW_ENTRY;
                currentExpressionBuffer.append(completeExpression);
                resetBuffer();
                this.expressionProcessor = new EncapsulatedExpressionProcessor();
            }
        });
    }

    ///////////////////////////////
    ///         UTILITY         ///
    ///////////////////////////////

    abstract protected void populateCondition(SqlQuery.SqlQueryBuilder sqlBuilder);

    private void resetBuffer() {
        this.buffer = new StringBuffer();
    }

    private void resetCurrentExpressionBuffer() {
        this.currentExpressionBuffer = new StringBuffer();
    }

    private void resetAllBuffers() {
        resetBuffer();
        resetCurrentExpressionBuffer();
    }

    protected void flushEntry() {
        whereClauseList.add(whereClauseBuilder.build());
        this.whereClauseBuilder = WhereClause.builder();
        resetAllBuffers();
    }

    private boolean checkForOperators() {
        if (buffer.toString().trim().equalsIgnoreCase("AND")) {
            moveToNewEntry(BooleanOperator.AND);
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("OR")) {
            moveToNewEntry(BooleanOperator.OR);
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("NOT")) {
            moveToNewEntry(BooleanOperator.NOT);
            return true;
        }
        return false;
    }

    private void moveToNewEntry(BooleanOperator booleanOperator) {
        if (currentExpressionBuffer.isEmpty()) {
            throw new RuntimeException("Empty WHERE expression!");
        }
        whereClauseBuilder.expression(currentExpressionBuffer.toString());
        this.state = State.NEW_ENTRY;
        whereClauseBuilder.followingOperator(booleanOperator);
        flushEntry();
    }

    abstract protected boolean checkForNewStageTransition(
            final SqlQuery.SqlQueryBuilder sqlBuilder, final Consumer<StateMachineState> newState
    );

    protected enum State {
        NEW_ENTRY,
        ENCAPSULATED_EXPRESSION,
        ;
    }
}

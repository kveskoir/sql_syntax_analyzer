package org.example.sql_parser.service.states;

import org.example.sql_parser.model.SqlQuery;

import java.util.function.Consumer;

public class WhereState extends ConditionStateBase {

    @Override
    protected void populateCondition(SqlQuery.SqlQueryBuilder sqlBuilder) {
        sqlBuilder.whereClauses(whereClauseList);
    }

    @Override
    protected boolean checkForNewStageTransition(
            final SqlQuery.SqlQueryBuilder sqlBuilder, final Consumer<StateMachineState> newState
    ) {
        if (buffer.toString().trim().equalsIgnoreCase("GROUP")) {
            whereClauseBuilder.expression(currentExpressionBuffer.toString());
            flushEntry();
            populateCondition(sqlBuilder);
            newState.accept(new GroupByState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("LIMIT")) {
            whereClauseBuilder.expression(currentExpressionBuffer.toString());
            flushEntry();
            populateCondition(sqlBuilder);
            newState.accept(new LimitState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("OFFSET")) {
            whereClauseBuilder.expression(currentExpressionBuffer.toString());
            flushEntry();
            populateCondition(sqlBuilder);
            newState.accept(new OffsetState());
            return true;
        }
        if (buffer.toString().trim().equalsIgnoreCase("ORDER")) {
            whereClauseBuilder.expression(currentExpressionBuffer.toString());
            flushEntry();
            populateCondition(sqlBuilder);
            newState.accept(new OrderByState());
            return true;
        }
        return false;
    }
}

package org.example.sql_parser.model;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@ToString
public class WhereClause {
    @Nonnull
    private final String expression;
    @Nullable
    private final BooleanOperator followingOperator;

    WhereClause(@Nonnull String expression, @Nullable BooleanOperator followingOperator) {
        this.expression = expression;
        this.followingOperator = followingOperator;
    }

    public static WhereClauseBuilder builder() {
        return new WhereClauseBuilder();
    }

    @Getter
    public static class WhereClauseBuilder {
        private String expression;
        private BooleanOperator followingOperator;

        WhereClauseBuilder() {
        }

        public WhereClauseBuilder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public WhereClauseBuilder followingOperator(BooleanOperator followingOperator) {
            this.followingOperator = followingOperator;
            return this;
        }

        public WhereClause build() {
            return new WhereClause(expression, followingOperator);
        }

        public String toString() {
            return "WhereClause.WhereClauseBuilder(expression=" + this.expression + ", followingOperator=" + this.followingOperator + ")";
        }
    }
}

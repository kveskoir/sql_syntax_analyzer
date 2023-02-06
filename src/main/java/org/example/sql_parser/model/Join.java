package org.example.sql_parser.model;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@ToString
public class Join {
    @Nonnull
    private final JoinType joinType;
    @Nullable
    private final SqlQuery query;
    @Nullable
    private final String tableName;
    @Nullable
    private final String alias;
    @Nonnull
    private final String condition;

    Join(JoinType joinType, String tableName, SqlQuery query, String alias, String condition) {
        this.joinType = joinType;
        this.tableName = tableName;
        this.query = query;
        this.alias = alias;
        this.condition = condition;
    }

    public static JoinBuilder builder() {
        return new JoinBuilder();
    }

    @Getter
    public static class JoinBuilder {
        private JoinType joinType;
        private String tableName;
        private SqlQuery query;
        private String alias;
        private String condition;

        JoinBuilder() {
        }

        public boolean isEmpty() {
            return joinType == null && tableName == null && query == null && alias == null && condition == null;
        }

        public JoinBuilder joinType(JoinType joinType) {
            this.joinType = joinType;
            return this;
        }

        public JoinBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public JoinBuilder query(SqlQuery query) {
            this.query = query;
            return this;
        }

        public JoinBuilder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public JoinBuilder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Join build() {
            return new Join(joinType, tableName, query, alias, condition);
        }

        public String toString() {
            return "Join.JoinBuilder(joinType=" + this.joinType + ", tableName=" + this.tableName + ", alias=" + this.alias + ", condition=" + this.condition + ")";
        }
    }
}

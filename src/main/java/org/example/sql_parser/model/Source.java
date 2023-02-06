package org.example.sql_parser.model;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@ToString
public class Source {
    @Nullable
    final SqlQuery query;
    @Nullable
    final String tableName;
    @Nullable
    final String alias;
    @Nullable
    final List<Join> joins;

    Source(@Nullable SqlQuery query, @Nullable String tableName, @Nullable String alias, @Nullable List<Join> joins) {
        this.query = query;
        this.tableName = tableName;
        this.alias = alias;
        this.joins = joins;
    }

    public static SourceBuilder builder() {
        return new SourceBuilder();
    }

    @Getter
    public static class SourceBuilder {
        private SqlQuery query;
        private String tableName;
        private String alias;
        private List<Join> joins;

        SourceBuilder() {
        }

        public boolean isEmpty() {
            return query == null && tableName == null && alias == null && joins == null;
        }

        public SourceBuilder query(SqlQuery query) {
            this.query = query;
            return this;
        }

        public SourceBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public SourceBuilder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public SourceBuilder joins(List<Join> joins) {
            this.joins = joins;
            return this;
        }

        public Source build() {
            return new Source(query, tableName, alias, joins);
        }

        public String toString() {
            return "Source.SourceBuilder(query=" + this.query + ", tableName=" + this.tableName + ", alias=" + this.alias + ", joins=" + this.joins + ")";
        }
    }
}

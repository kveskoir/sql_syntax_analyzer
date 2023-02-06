package org.example.sql_parser.model;

import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@ToString
public class SelectEntry {
    @Nullable
    private final String function;
    @Nonnull
    private final String table;
    @Nonnull
    private final String column;

    @Nullable
    private final String alias;

    SelectEntry(@Nullable String function, @Nonnull String table, @Nonnull String column, @Nullable String alias) {
        this.function = function;
        this.table = table;
        this.column = column;
        this.alias = alias;
    }

    public static SelectEntryBuilder builder() {
        return new SelectEntryBuilder();
    }

    @Getter
    public static class SelectEntryBuilder {
        private String function;
        private String table;
        private String column;
        private String alias;

        SelectEntryBuilder() {
        }

        public boolean isEmpty() {
            return function == null &&
                    table == null &&
                    column == null &&
                    alias == null;
        }

        public SelectEntryBuilder function(String function) {
            this.function = function;
            return this;
        }

        public SelectEntryBuilder table(String table) {
            this.table = table;
            return this;
        }

        public SelectEntryBuilder column(String column) {
            this.column = column;
            return this;
        }

        public SelectEntryBuilder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public SelectEntry build() {
            return new SelectEntry(function, table, column, alias);
        }

        public String toString() {
            return "SelectEntry.SelectEntryBuilder(function=" + this.function + ", table=" + this.table + ", column="
                    + this.column + ", alias=" + this.alias + ")";
        }
    }
}

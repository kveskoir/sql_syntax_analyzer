package org.example.sql_parser.model;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class Sort {
    private final List<SelectEntry> sortingFields;
    private final SortType type;

    Sort(List<SelectEntry> sortingFields, SortType type) {
        this.sortingFields = sortingFields;
        this.type = type;
    }

    public static SortBuilder builder() {
        return new SortBuilder();
    }

    public static class SortBuilder {
        private List<SelectEntry> sortingFields;
        private SortType type;

        SortBuilder() {
        }

        public SortBuilder sortingFields(List<SelectEntry> sortingFields) {
            this.sortingFields = sortingFields;
            return this;
        }

        public SortBuilder type(SortType type) {
            this.type = type;
            return this;
        }

        public Sort build() {
            return new Sort(sortingFields, type);
        }

        public String toString() {
            return "Sort.SortBuilder(sortingFields=" + this.sortingFields + ", type=" + this.type + ")";
        }
    }
}

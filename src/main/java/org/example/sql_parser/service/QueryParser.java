package org.example.sql_parser.service;

import org.example.sql_parser.model.SqlQuery;

public interface QueryParser {
    SqlQuery parseQuery(String queryString);
}

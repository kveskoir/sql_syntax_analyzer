package org.example;

import org.example.sql_parser.service.QueryParser;
import org.example.sql_parser.service.SqlParserStateMachine;

public class Main {
    public static void main(String[] args) {
        final String sql = """
                SELECT author.name as test, count(book.id), sum(book.cost), count(test.test)
                FROM abc, test, author
                LEFT JOIN book ON (author.id = book.author_id)
                JOIN (SELECT asfasf.asf FROM a) as a ON (as.safasf = asfa.asf)
                WHERE (3 * (5 - 3)) < 5 AND tatata.asasf = 5 OR FALSE
                GROUP BY author.name, asfasf   , SUM(safas.asf)
                HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
                LIMIT 10
                ORDER BY safasf.asfas, asfasf, asads DESC
                OFFSET 5;
                """;
        QueryParser parser = new SqlParserStateMachine();

        System.out.println(parser.parseQuery(sql).toString());
    }
}
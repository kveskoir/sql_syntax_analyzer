package org.example.sql_parser.service.utility;

import java.util.List;

public class SyntaxUtility {
    private static final List<Character> EXTRA_CHARACTERS = List.of('_', '-', '*');
    public static boolean isAcceptableCharacter(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c) || EXTRA_CHARACTERS.contains(c);
    }

}

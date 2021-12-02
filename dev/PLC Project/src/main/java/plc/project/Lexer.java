package plc.project;

import java.util.ArrayList;
import java.util.List;

import static plc.project.Token.Type.*;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #//lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */



public final class Lexer {
    String identifier = "[A-Za-z0-9_-]";
    String identifier1 = "[A-Za-z@]";
    String integer = "[0-9-]";
    String integer19 = "[0-9]";
    String decimal = "[.]";
    String apostrophe = "'";
    String character = "[^ \b\n\r\t]";
    String string = "[^\"\b\n\r\t]";
    String bnrt = "[bnrt\"]";
    String quote = "\"";
    String escape = "\\\\";
    String operator = "[!=?&|]";
    String notwhitespace = "[^ \b\n\r\t]";
    String whitespace = "[ \b\n\r\t]";
    private final CharStream chars;


    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #//lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {//calls lextoken
        //filters white space and calls lextoken on the characters that are not the whitespace
        List<Token> tokens = new ArrayList<>();
        Token token = null;
        while(chars.has(0)){
            if(peek(whitespace)){
                while(match(whitespace)){};
                chars.skip();
            }
            if(chars.has(0))
            {token = lexToken();}
            if(peek(whitespace)){
                while(match(whitespace)){};
            chars.skip();
            }
            if(token == null){
                break;
            }
            else{
                tokens.add(token);
            }
        }
        
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}//
     */
    public Token lexToken() {//calls the below lex functions

        if(peek(identifier1)){
            return lexIdentifier();
        }
        else if(peek(apostrophe)){
            return lexCharacter();
        }
        else if(peek(quote)){
            return lexString();
        }
        else if(peek(integer)){
            return lexNumber();
        }
        else if(!peek(whitespace)){
            return lexOperator();
        }

        throw new ParseException("type not found", chars.getIndex());
    }

    public Token lexIdentifier() {//[A-Za-z@][0-9A-Za-z_-]*
        if(peek(identifier1)){
            match(identifier1);
            while(peek(identifier)){
                match(identifier);}
            return chars.emit(IDENTIFIER);
        }
        throw new ParseException("Identifer Parsing Error", chars.getIndex());
    }

    public Token lexNumber() {//[0-9]
        if(peek("0", integer)){
            throw new ParseException("Leading 0s", chars.getIndex());
        }
        if(peek(integer)){
            match(integer);
        }
        while(peek(integer19)){
            match(integer19);
        }
        if(peek(decimal)){
            match(decimal);
            if(peek(integer19)){
            while(peek(integer19)){
                match(integer19);
            }
            return chars.emit(DECIMAL);}
            else
                throw new ParseException("Too many decimals", chars.getIndex());
        }
        else{
        return chars.emit(INTEGER);}
    }

    public Token lexCharacter() {//(.|\n)
        if(peek(apostrophe)){
            match(apostrophe);
            if(peek(character)){
                match(character);
                if(peek(bnrt)){
                    match(bnrt);}
                if(peek(apostrophe)){
                    match(apostrophe);
                    return chars.emit(CHARACTER); //TODO
                }
            }
        }
        throw new ParseException("character parsed incorrectly", chars.getIndex());

    }

    public Token lexString() {// \"(([^\\\\\b\n\r\t\"'])|(\\\\[\\\\bnrt'"]))*\"
        if(peek(quote)){
            match(quote);
        }
        while(peek(string)){
            if(peek(escape)){
                match(escape);
                if(peek(bnrt)){
                match(bnrt);}
                else{throw new ParseException("Invalid escape", chars.getIndex());}
            }
            else
            match(string);
        }
        if(peek(quote)){
            match(quote);
            return chars.emit(STRING);
        }

        throw new ParseException("Unterminated", chars.getIndex());//TODO
    }

    public void lexEscape() {
         //TODO
    }

    public Token lexOperator() {
        if(peek(operator)){
            match(operator);
            if(peek(operator)){
                match(operator);
            }
            return chars.emit(OPERATOR);
        }
        else{
            if(peek(notwhitespace)){
                match(notwhitespace);
                return chars.emit(OPERATOR);}
        }
         throw new ParseException("something happened to operator, guess it couldn't help you take this call", chars.get(0));//TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i < patterns.length; i++){

            if(!chars.has(i)|| !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {

        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public int getIndex() {return index;}

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}

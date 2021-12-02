package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {
    Ast.Expression previousExpression;

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<Ast.Global>();
        List<Ast.Function> functions = new ArrayList<Ast.Function>();
while(tokens.has(0)) {
    if (peek("VAL") | peek("VAR") | peek("LIST")) {
        if (match("VAL")) {
            globals.add(parseImmutable());
        }
        if (match("VAR")) {
            globals.add(parseMutable());
        }
        if (match("LIST")) {
            globals.add(parseList());
        }
    }
    if (peek("LET")) {
        match("LET");
        globals.add(parseGlobal());
    }
    if (peek("FUN")) {
        match("FUN");
        functions.add(parseFunction());
    }
}
        return new Ast.Source(globals, functions);
        //throw new ParseException("not global or function", tokens.index); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {

        try {
            return new Ast.Global(tokens.get(0).getLiteral(), tokens.get(0).getType().toString(), true, Optional.ofNullable(parseExpression()));
        }
        catch(Exception e){return new Ast.Global(tokens.get(0).getLiteral(), true, Optional.ofNullable(parseExpression()));
        }//throw new ParseException("Is it really a global?", 0);
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        if(peek(Token.Type.IDENTIFIER)){
            String typename = "Any";
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if(match(":")){
                typename = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
            }
            if(match("=")){
                if(peek("[")){
                    //match("[");
                    if(peek("[", "]")){
                        throw new ParseException("Missing insides", tokens.get(0).getIndex());
                    }

                    Ast.Global g = new Ast.Global(name, typename, true, Optional.ofNullable(parseExpression()));
                    match(";");
                    return g;
                }
            }
            Ast.Global g = new Ast.Global(name, typename,true, Optional.empty());
            match(";");
            return g;
        }
        throw new ParseException("no Identifier", tokens.get(-1).getIndex()); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        if(!peek(Token.Type.IDENTIFIER)){ throw new ParseException("No Identifier After VAR", tokens.get(-1).getIndex());}
        String typeName = "Any";
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER);
        if(peek(":")){
            match(":");
            typeName = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }
        if(peek("=")){
            match("=");}
            if(tokens.has(0)){
                Ast.Global Returnable =  new Ast.Global(name, typeName, true, Optional.ofNullable(parseExpression()));
                if(match(";"))
                 return Returnable;
                else throw new ParseException("no ;", tokens.get(-1).getIndex());
            }
            else throw new ParseException("no expression", tokens.get(-1).getIndex());

    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String name = tokens.get(0).getLiteral();
        String typename = "Any";
        match(Token.Type.IDENTIFIER);
        if(match(":")){
            typename = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
        }
        if(peek("=")){
            match("=");
            if(tokens.has(0)){Ast.Global Returnable = new Ast.Global(name, typename, false, Optional.ofNullable(parseExpression()));
            if(match(";")){
                return Returnable;}
            else throw new ParseException("Expected Semicolon", tokens.get(-1).getIndex());}

            else throw new ParseException("no expression", tokens.get(-1).getIndex());
        }
        else throw new ParseException("no assignment for immutable type", tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        String returntype = "Any";
        if(peek(Token.Type.IDENTIFIER)){
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            List<String> parameters = new ArrayList<String>();
            List<String> parameterTypes = new ArrayList<String>();
            List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
            if(!match("(")){
                throw new ParseException("No Parenthesis", tokens.get(0).getIndex());
            }
            while(!match(")")){
                if(peek(Token.Type.IDENTIFIER)){
                parameters.add(tokens.get(0).getLiteral());
                match(Token.Type.IDENTIFIER);}
                else if(peek(":")){match(":");
                    if(peek(Token.Type.IDENTIFIER)){
                        parameterTypes.add(tokens.get(0).getLiteral());
                        match(Token.Type.IDENTIFIER);
                    }
                }
                else match(",");
            }
            if(parameters.size() != parameterTypes.size()){throw new ParseException("Missing Type", tokens.index - 1);}
            if(peek(":")){match(":");
                if(peek(Token.Type.IDENTIFIER)){
                    returntype = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                }
            }
            if(!match("DO")){throw new ParseException("No DO", tokens.get(0).getIndex());}
            while(!peek("END")){
                while( !peek(";") && !peek("END")){
                statements.add(parseStatement());}
                match(";");
            }
            if(!match("END")){throw new ParseException("No END", tokens.get(0).getIndex()); }

        return new Ast.Function(name, parameters, parameterTypes, Optional.of(returntype), statements);}
        throw new ParseException("SOMETHING", tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
        while(!peek("END") && !peek("ELSE") && !peek("DEFAULT") && !peek("CASE") && !peek("DO")){
            while( !peek(";") && !peek("END") && !peek("ELSE") && !peek("DEFAULT") && !peek("CASE") && !peek("DO")){
                statements.add(parseStatement());
            }
            match(";");
        }
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek(Token.Type.IDENTIFIER) | peek(Token.Type.OPERATOR)){
            if(peek("IF")){
                match("IF");
                return parseIfStatement();
            }
            else if(peek("SWITCH")){
                return parseSwitchStatement();
            }
            else if(peek("WHILE")){
                match("WHILE");
                return parseWhileStatement();
            }
            else if(peek("RETURN")){
                match("RETURN");
                return parseReturnStatement();
            }
            else if(peek("LET")){
                return parseDeclarationStatement();
            }
            else if(peek(Token.Type.IDENTIFIER, "=")){
                Ast.Statement.Assignment returnable = new Ast.Statement.Assignment(parseExpression(), parseExpression());
                if(peek(";")){
                    match(";");
                    return returnable;
                }
                else throw new ParseException("Missing semicolon", 1);
            }
            else{
                Ast.Statement.Expression returnable = new Ast.Statement.Expression(parseExpression());
                if(peek(";")){
                    match(";");
                    return returnable;
                }
                else throw new ParseException("Missing semicolon", 1);
            }
        }
        throw new ParseException("Something", tokens.get(0).getIndex());
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if(match("LET")){
        if(peek(Token.Type.IDENTIFIER)){
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            String typeName = null;
            if(match(":")){
            typeName = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);}
            if(peek(";")){
                match(";");
                return new Ast.Statement.Declaration(name, Optional.ofNullable(typeName), Optional.empty());
            }

            if(tokens.has(0) && !peek("=")){throw new ParseException("no =", tokens.get(0).getIndex());}
        return new Ast.Statement.Declaration(name, Optional.ofNullable(typeName) ,java.util.Optional.ofNullable(parseExpression()));}
        else throw new ParseException("No Identifier after \"LET\"", tokens.get(0).getIndex());}
        else throw new ParseException("Something wrong with LET", tokens.get(0).getIndex());
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        if(peek(Token.Type.IDENTIFIER)){
            Ast.Expression condition = parseExpression();
            if(peek("DO")){
                match("DO");
                List<Ast.Statement> DoLoop = parseBlock();
                match("ELSE");
                List<Ast.Statement> elseLoop = parseBlock();
                if(match("END")){
                return new Ast.Statement.If(condition, DoLoop, elseLoop);}
                else throw new ParseException("NO END", tokens.get(0).getIndex());
            }
            throw new ParseException("NO Do", tokens.get(0).getIndex());
        }
        throw new ParseException("no Identifier after IF", tokens.get(0).getIndex());
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        match("SWITCH");
        Ast.Expression expression = parseExpression();
        List<Ast.Statement.Case> list = new ArrayList<Ast.Statement.Case>();
        if(peek("CASE")){
            while (peek("CASE")){
                list.add(parseCaseStatement());
            }
        }
        if(!peek("DEFAULT")){throw new ParseException("No Default Case", tokens.get(0).getIndex());}
        list.add(parseCaseStatement());
        if(!match("END")){throw new ParseException("No End", tokens.get(0).getIndex());}
        return new Ast.Statement.Switch(expression, list);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if(match("CASE")) {
            Ast.Expression dog = parseExpression();
            if(!match(":")){
                throw new ParseException("No colon", tokens.get(0).getIndex());
            }
            return new Ast.Statement.Case(Optional.ofNullable(dog), parseBlock());
        }
        else if(match("DEFAULT")) {
            return new Ast.Statement.Case(Optional.empty(), parseBlock());
        }
        throw new ParseException("Something went terribly wrong in the case statement", tokens.get(0).getIndex());//TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        if(peek(Token.Type.IDENTIFIER)){
            Ast.Expression condition = parseExpression();
            if(!match("DO")){
                if(tokens.has(0)){throw new ParseException("no Do", tokens.get(0).getIndex());}
                throw new ParseException("no Do", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            List<Ast.Statement> statements = parseBlock();
        if(!match("END")){
            if(tokens.has(0))throw new ParseException("no end",tokens.get(0).getIndex() );
        else throw new ParseException("no end", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}

        return new Ast.Statement.While(condition, statements);}
        else {
            if(tokens.has(0)){throw new ParseException("no Identifier", tokens.get(0).getIndex());}
            throw new ParseException("no Identifier", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }

    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        if(!peek(Token.Type.IDENTIFIER)){throw new ParseException("No Identifier after return", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
        return new Ast.Statement.Return(parseExpression()); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {

            if (peek( "||") | peek( "&&") |peek(Token.Type.IDENTIFIER, "||") | peek(Token.Type.IDENTIFIER, "&&")) {
                return parseLogicalExpression();
            } else if (peek( "<") | peek( ">") | peek( "==") |  peek( "!=") | peek(Token.Type.IDENTIFIER, "<") | peek(Token.Type.IDENTIFIER, ">") | peek(Token.Type.IDENTIFIER, "==") | peek(Token.Type.IDENTIFIER, "!=")) {
                return parseComparisonExpression();

            } else if (peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") | peek( "-") | peek( "+")) {
                    return parseAdditiveExpression();

            } else if (peek( "*") | peek( "/") | peek( "^") | peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/") | peek(Token.Type.IDENTIFIER, "^")) {
                return parseMultiplicativeExpression();
            } else if (peek(Token.Type.IDENTIFIER) | peek(Token.Type.INTEGER) | peek(Token.Type.OPERATOR)| peek(Token.Type.DECIMAL) | peek(Token.Type.CHARACTER) | peek(Token.Type.STRING)) {
                return parsePrimaryExpression();
            }

        return previousExpression;
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        String name = tokens.get(0).getLiteral();
        if(!match(Token.Type.IDENTIFIER)){
            if(match("&&")){
                if(!tokens.has(0)){ throw new ParseException("no Identifier after Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && (peek(Token.Type.IDENTIFIER, "||") | peek(Token.Type.IDENTIFIER, "&&") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("&&", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("&&", previousExpression, parseExpression());}
            else if(match("||")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && (peek(Token.Type.IDENTIFIER, "||") | peek(Token.Type.IDENTIFIER, "&&") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("||", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("||", previousExpression, parseExpression());}

        }
        if(match("&&")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && (peek(Token.Type.IDENTIFIER, "||") | peek(Token.Type.IDENTIFIER, "&&") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("&&", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();}
            return new Ast.Expression.Binary("&&", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match("||")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && (peek(Token.Type.IDENTIFIER, "||") | peek(Token.Type.IDENTIFIER, "&&") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("||", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();}
            return new Ast.Expression.Binary("||", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else throw new ParseException("Something went wrong logically", tokens.get(0).getIndex() );
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        String name = tokens.get(0).getLiteral();
        if( !match(Token.Type.IDENTIFIER)){
            if(match("<")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("<", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("<", previousExpression, parseExpression());}
            else if (match(">")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary(">", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary(">", previousExpression, parseExpression());}
            else if (match("==")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("==", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("==", previousExpression, parseExpression());}
            else if (match("!=")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("!=", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("!=", previousExpression, parseExpression());}
        }
        if(match("<")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("<", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();}
            return new Ast.Expression.Binary("<", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match(">")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary(">", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();}
            return new Ast.Expression.Binary(">", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match("==")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();}
            return new Ast.Expression.Binary("==", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match("!=")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^")  | peek(Token.Type.IDENTIFIER, "+") | peek(Token.Type.IDENTIFIER, "-") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("!=", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();}
            return new Ast.Expression.Binary("!=", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else throw new ParseException("Something went wrong Comparitively", tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {

        String name = tokens.get(0).getLiteral();
        if(!match(Token.Type.IDENTIFIER)){
            if (match("-")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("-", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("-", previousExpression, parseExpression());
            }
            else if (match("+")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^") )){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("+", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("+", previousExpression, parseExpression());
            }
        }

        if(match("+")){
            if(!tokens.has(0)){throw new ParseException("No following Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();
            }
            return new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match("-")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR) && !(peek(Token.Type.IDENTIFIER, "*") | peek(Token.Type.IDENTIFIER, "/")  | peek(Token.Type.IDENTIFIER, "^") )){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("-", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();
            }
            return new Ast.Expression.Binary("-", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else throw new ParseException("Something went wrong Additionally", tokens.get(0).getIndex());
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        String name = tokens.get(0).getLiteral();
        if(!match(Token.Type.IDENTIFIER)){
            if(match("*")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR)){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("*", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("*", previousExpression, parseExpression());
            }
            if(match("^")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR)){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("^", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("^", previousExpression, parseExpression());
            }
            if(match("/")){
                if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
                if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR)){
                    String name1 = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    previousExpression = new Ast.Expression.Binary("/", previousExpression, new Ast.Expression.Access(Optional.empty(), name1));
                    return parseExpression();
                }
                return new Ast.Expression.Binary("/", previousExpression, parseExpression());
            }
        }
        if(match("*")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR)){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("*", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();
            }
            return new Ast.Expression.Binary("*", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match("/")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR)){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("/", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();
            }
            return new Ast.Expression.Binary("/", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else if (match("^")){
            if(!tokens.has(0)){throw new ParseException("no Identifier After Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());}
            if(peek(Token.Type.IDENTIFIER, Token.Type.OPERATOR)){
                String name1 = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
                previousExpression = new Ast.Expression.Binary("^", new Ast.Expression.Access(Optional.empty(), name), new Ast.Expression.Access(Optional.empty(), name1));
                return parseExpression();
            }
            return new Ast.Expression.Binary("^", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
        }
        else throw new ParseException("Something went wrong Multilicatively", tokens.get(0).getIndex() +1);
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if(peek("TRUE")){
            match("TRUE");
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if(peek("FALSE")){
            match("FALSE");
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if(peek("NIL")){
            match("NIL");
            return new Ast.Expression.Literal(null);
        }
        else if(peek(Token.Type.DECIMAL)){
            BigDecimal ant = new BigDecimal(tokens.get(0).getLiteral());
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal( ant );
        }
        else if(peek(Token.Type.INTEGER)){
            BigInteger ant = new BigInteger(tokens.get(0).getLiteral());
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal( ant );
        }
        else if(peek(Token.Type.CHARACTER)){
            char c = tokens.get(0).getLiteral().charAt(1);
            if(c == '\\'){
                String string = "" + c + tokens.get(0).getLiteral().charAt(2);
                switch (string) {
                    case "\\t": {
                        c = '\t';
                        break;
                    }
                    case "\\b": {
                        c = '\b';
                        break;
                    }
                    case "\\n": {
                        c = '\n';
                        break;
                    }
                    case "\\r": {
                        c = '\r';
                        break;
                    }
                    case "\\\"": {
                        c = '\"';
                        break;
                    }
                    case "\\\'": {
                        c = '\'';
                        break;
                    }
                    case "\\\\": {
                        c = '\\';
                        break;
                    }
                }
            }
            match(Token.Type.CHARACTER);
            return new Ast.Expression.Literal( c );
        }
        else if(peek(Token.Type.STRING)){
            String string = "";
            String chara = "";
            char c = '\\';
            for(int i = 1; i < tokens.get(0).getLiteral().length() -1; i++){
                if(tokens.get(0).getLiteral().charAt(i) == '\\'){
                    chara = "" + tokens.get(0).getLiteral().charAt(i) + tokens.get(0).getLiteral().charAt(i+1);
                    i++;
                    switch (chara){
                        case "\\t":{
                          c = '\t';
                          break;
                        }
                        case "\\b":{
                            c = '\b';
                            break;
                        }case "\\n":{
                            c = '\n';
                            break;
                        }
                        case "\\r":{
                            c = '\r';
                            break;
                        }
                        case "\\\'":{
                            c = '\'';
                            break;
                        }case "\\\"":{
                            c = '\"';
                            break;
                        }case "\\\\":{
                            c = '\\';
                            break;
                        }

                    }
                    string = string + c;
                    chara = "";
                }
                else{
                    string = string + tokens.get(0).getLiteral().charAt(i);
                }
            }
            match(Token.Type.STRING);
            return new Ast.Expression.Literal( string );
        }
        else if(peek(Token.Type.OPERATOR)){
            if(peek("(")) {
                //match("(");
                List<Ast.Expression> list = new ArrayList<Ast.Expression>();
                match("(");

                if(tokens.has(1) && !peek(")")){
                    while(!peek(")")){
                        list.add(parseExpression());
                    }
                }
                if(match(")")){ previousExpression = new Ast.Expression.Group(list.get(0));
                    return previousExpression;}
                else throw new ParseException("No CLosing par", tokens.get(-1).getIndex() +tokens.get(-1).getLiteral().length());
            }
            if(peek(")")){return previousExpression;}
            if(peek("]")){return previousExpression;}
            if(peek("[")) {
                match("[");
                List<Ast.Expression> list = new ArrayList<Ast.Expression>();

                if(tokens.has(1) && !peek(")")){
                    while(!peek("]")){
                        list.add(parseExpression());
                    }
                }
                if(match("]")){previousExpression = new Ast.Expression.PlcList(list);
                return previousExpression;}
                else throw new ParseException("No CLosing par", tokens.get(-1).getIndex() +tokens.get(-1).getLiteral().length());
            }
            if(peek(";")){
                return previousExpression;
            }
            match(Token.Type.OPERATOR);
            return parseExpression();
        }

        else if (peek(Token.Type.IDENTIFIER)){
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if(peek(Token.Type.OPERATOR)){
                if(peek("(")){
                    List<Ast.Expression> list = new ArrayList<Ast.Expression>();
                    match("(");

                    if(tokens.has(1) && !peek(")")){
                    while(!peek(")")){
                        list.add(parseExpression());
                    }
                    }
                    if(match(")")){return new Ast.Expression.Function(name, list);}
                    else throw new ParseException("No CLosing par", tokens.get(-1).getIndex() +tokens.get(-1).getLiteral().length());

                }
                else if(peek("=")){
                    return new Ast.Expression.Access(Optional.empty(), name);
                }
                else if(peek("[")){
                    match("[");
                    List<Ast.Expression> list = new ArrayList<Ast.Expression>();
                    //match("(");

                    if(tokens.has(1) && !peek("]")){
                        while(!peek("]")){
                            list.add(parseExpression());
                        }
                    }
                    if(match("]")){ previousExpression = new Ast.Expression.Access(java.util.Optional.ofNullable(list.get(0)), name);
                        return previousExpression;}
                    else throw new ParseException("No CLosing par", tokens.get(-1).getIndex() +tokens.get(-1).getLiteral().length());

                }
                else if(peek("+")){
                    match("+");
                    return new Ast.Expression.Binary("+", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
                }
                else if(peek("&&")){
                    match("&&");
                    return new Ast.Expression.Binary("&&", new Ast.Expression.Access(Optional.empty(), name), parseExpression());
                }
                else if(peek(",")){
                    if(peek(",", Token.Type.IDENTIFIER)){
                        return new Ast.Expression.Access( Optional.empty(), name);
                    }
                    else{ throw new ParseException("Trailing Comma", tokens.get(0).getIndex());}
                    }
                else if(peek("]")){
                    //match("]", ";");
                    return new Ast.Expression.Access(Optional.empty(), name);
                }
                else{
                    return new Ast.Expression.Access(Optional.empty(), name);

                }
            }
            else{
                return new Ast.Expression.Access(Optional.empty(), name);
            }



        }
    throw new ParseException("Not A Primary Expression", tokens.get(0).getIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {

       for( int i = 0; i < patterns.length; i++){
           if(!tokens.has(i)){
               return false;
           }
           else if(patterns[i] instanceof Token.Type){
               if(patterns[i] != tokens.get(i).getType()){
                   return false;
               }
           }
           else if(patterns[i] instanceof String){
               if(!patterns[i].equals(tokens.get(i).getLiteral())){
                   return false;
               }
           }
           else{
               throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
           }
       }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {

        boolean peek = peek(patterns);
        if(peek){
            for ( int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}

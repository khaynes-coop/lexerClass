package plc.project;

//import com.sun.org.apache.xpath.internal.operations.Bool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
        scope.defineFunction("main", "System.out.main", Arrays.asList(Environment.Type.ANY), Environment.Type.INTEGER, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for(int i = 0; i < ast.getGlobals().size(); i++){
        visit(ast.getGlobals().get(i));}
        for(int i = 0; i < ast.getFunctions().size(); i++){
        visit(ast.getFunctions().get(i));

        //throw new RuntimeException(ast.getFunctions().get(i).getName());

        }


        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {

        Environment.Type type;
        switch (ast.getTypeName()){
            case"Integer":{type = Environment.Type.INTEGER; break;}
            case"Decimal":{type = Environment.Type.DECIMAL; break;}
            case"Boolean":{type = Environment.Type.BOOLEAN; break;}
            case"String":{type = Environment.Type.STRING; break;}
            case"Character":{type = Environment.Type.CHARACTER; break;}
            case"Any":{type = Environment.Type.ANY; break;}
            case"Nil":{type = Environment.Type.NIL; break;}
            case"Comparable":{type = Environment.Type.COMPARABLE; break;}
            //TODO Other Cases
            default: throw new RuntimeException("Missing/Unknown type");
        }
        if(!ast.getValue().isPresent()){
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, ast.getMutable(), Environment.NIL));
        }
        else{
            visit(ast.getValue().get());
            requireAssignable(ast.getValue().get().getType(), type);
            try{ast.setVariable(scope.lookupVariable(ast.getName()));}
            catch(Exception e){
                ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, ast.getMutable(), Environment.create(ast.getValue().get())));
            }
        }
            //requireAssignable

        return null;

        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        try{ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));}
        catch(RuntimeException e){

            for(int i = 0; i < ast.getStatements().size(); i++){
                visit(ast.getStatements().get(i));}
            for(int i = 0; i < ast.getParameters().size(); i++){scope.lookupVariable(ast.getParameters().get(i));}

            //java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> funs = ast.getParameters(), ;

            List<Environment.Type> types = new ArrayList<>();

            for(int i = 0; i < ast.getParameterTypeNames().size(); i++){
                switch (ast.getParameterTypeNames().get(i)){
                    case"Integer":{types.add(Environment.Type.INTEGER); break;}
                    case"Decimal":{types.add(Environment.Type.DECIMAL); break;}
                    case"Boolean":{types.add(Environment.Type.BOOLEAN); break;}
                    case"String":{types.add(Environment.Type.STRING); break;}
                    case"Character":{types.add(Environment.Type.CHARACTER); break;}
                    case"Any":{types.add(Environment.Type.ANY); break;}
                    case"Nil":{types.add(Environment.Type.NIL); break;}
                    case"Comparable":{types.add(Environment.Type.COMPARABLE); break;}
                    //TODO Other Cases
                    default: throw new RuntimeException("Missing/Unknown type");
                }
            }

            Environment.Type returnType = Environment.Type.NIL;
            if(ast.getReturnTypeName().isPresent()){
            switch (ast.getReturnTypeName().get()){
                case"Integer":{returnType = Environment.Type.INTEGER; break;}
                case"Decimal":{returnType = Environment.Type.DECIMAL; break;}
                case"Boolean":{returnType = Environment.Type.BOOLEAN; break;}
                case"String":{returnType = Environment.Type.STRING; break;}
                case"Character":{returnType = Environment.Type.CHARACTER; break;}
                case"Any":{returnType = Environment.Type.ANY; break;}
                case"Nil":{returnType = Environment.Type.NIL; break;}
                case"Comparable":{returnType = Environment.Type.COMPARABLE; break;}
                //TODO Other Cases
                default: throw new RuntimeException("Missing/Unknown type");
            }
            try{
                Ast.Statement.Return ret = (Ast.Statement.Return) ast.getStatements().get(ast.getStatements().size() -1);
                requireAssignable(returnType, ret.getValue().getType() );}
            catch (ClassCastException g){}
            }
            else{throw new RuntimeException("No Return Type");}


            //throw new RuntimeException(ast.getReturnTypeName().get() + " " + ret.getValue().getType().getName());

            ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), types, returnType, args -> Environment.NIL));
        }

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if(ast.getExpression().getClass().getSimpleName().equals("Literal")){
            throw new RuntimeException("Class is Literal, expected not Literal");
        }
        visit(ast.getExpression());
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {


        Environment.PlcObject obj;
        Environment.Type type;
        try{
            visit(ast.getValue().get());
            obj = Environment.create(ast.getValue().get());}
        catch(Exception e ){
            obj = Environment.create(Environment.NIL);}
        try{
            type = ast.getValue().get().getType();
        } catch (Exception e){
            if(!ast.getTypeName().isPresent()){
                throw new RuntimeException("No Type Present");
            }
            switch (ast.getTypeName().get()){
                case"Integer":{type = Environment.Type.INTEGER; break;}
                case"Decimal":{type = Environment.Type.DECIMAL; break;}
                case"Boolean":{type = Environment.Type.BOOLEAN; break;}
                case"String":{type = Environment.Type.STRING; break;}
                case"Character":{type = Environment.Type.CHARACTER; break;}
                case"Any":{type = Environment.Type.ANY; break;}
                case"Nil":{type = Environment.Type.NIL; break;}
                case"Comparable":{type = Environment.Type.COMPARABLE; break;}
                //TODO Other Cases
                default: throw new RuntimeException("Missing/Unknown type");
            }


        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, true, obj));
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        visit(ast.getValue());
        visit(ast.getReceiver());
        if(!ast.getValue().getType().equals(ast.getReceiver().getType())){
           throw new RuntimeException("Type Mismatch in Assignment");
        }

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        Ast.Expression.Literal lit = (Ast.Expression.Literal) ast.getCondition();
        if((ast.getThenStatements().size() == 0 && lit.getLiteral().toString().equals("true")) || ast.getElseStatements().size() == 0 && lit.getLiteral().toString().equals("false")){
            throw new RuntimeException("Empty Statements");
        }
        for(int i = 0; i < ast.getThenStatements().size(); i++){visit(ast.getThenStatements().get(i));}
        for(int i = 0; i < ast.getElseStatements().size(); i++){visit(ast.getElseStatements().get(i));}
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);
        return null;
        //else throw new RuntimeException("Condition not Boolean");
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        for(int i = 0; i < ast.getCases().size(); i++){
            visit(ast.getCases().get(i));
            boolean present = ast.getCases().get(i).getValue().isPresent();
            if(present)
            requireAssignable(ast.getCondition().getType(), ast.getCases().get(i).getValue().get().getType());
            }
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {

        try{visit(ast.getValue().get());}
        catch(NoSuchElementException e){}
        for(int i = 0; i < ast.getStatements().size(); i++){visit(ast.getStatements().get(i));}
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);
        visit(ast.getCondition());

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        //throw new RuntimeException( ast.toString());
        //requireAssignable(ast.getValue().getType(), scope.lookupVariable(ast.toString()).getType());
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        if(ast.getLiteral() instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        }
        else if(ast.getLiteral() instanceof String){
            ast.setType(Environment.Type.STRING);
            return null;
        }
        else if(ast.getLiteral() instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
            return null;
        }
        else if(ast.getLiteral() instanceof BigInteger){
            if ((((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE))) > 0)
                throw new RuntimeException("Error in AST LITERAL");
            ast.setType(Environment.Type.INTEGER);
            return null;
        }
        else if(ast.getLiteral() instanceof BigDecimal){
            if ((((BigDecimal) ast.getLiteral()).compareTo(BigDecimal.valueOf(Double.MAX_VALUE))) > 0)
                throw new RuntimeException("Error in AST LITERAL");
            ast.setType(Environment.Type.DECIMAL);
            return null;
        }
        else
            throw new RuntimeException("Error in AST LITERAL");


        //throw new UnsupportedOperationException();
        // TODO String char ect
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        if(ast.getExpression().getClass().getSimpleName().equals("Literal")){
            throw new RuntimeException("Grouped Literal");
        }
        ast.setType(ast.getExpression().getType());
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());

        if(ast.getOperator().equals("+") && (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) ){
            ast.setType(Environment.Type.STRING);
        }
        else
            requireAssignable(ast.getLeft().getType(), ast.getRight().getType());{
            ast.setType(ast.getLeft().getType());
        }
        //else{throw new RuntimeException("Left Right Type Mismatch");}
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        try{
        ast.setVariable(scope.lookupVariable(ast.getName()));
    }catch(RuntimeException e){
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.ANY, true, Environment.NIL));
        }
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        for(int i = 0; i < ast.getArguments().size(); i++){
            visit(ast.getArguments().get(i));
            requireAssignable( ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        visit(ast.getValues().get(0));
        ast.setType(ast.getValues().get(0).getType());
        for(int i = 1; i < ast.getValues().size(); i++){
        visit(ast.getValues().get(i));
        requireAssignable(ast.getType(), ast.getValues().get(i).getType());}

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.getName().equals("Comparable") || target.getName().equals("Any")){
            return ;
        }
        if(!type.equals(target)){
            throw new RuntimeException("Type " + type.getName() + " given, Expected Type " + target.getName());
        }  // TODO
    }

}

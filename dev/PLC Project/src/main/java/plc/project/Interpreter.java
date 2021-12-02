package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);

        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("main", 0, args -> {
            return Environment.create(BigInteger.ZERO);
        });

        scope.defineFunction("logarithm",1, args -> {
            BigDecimal bd = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));
            BigDecimal res = BigDecimal.valueOf(Math.log(bd.doubleValue()));
            return Environment.create(res);
        });

        scope.defineFunction("converter",2, args -> {
            BigInteger bd = requireType(BigInteger.class, Environment.create(args.get(0).getValue()));
            BigInteger base = requireType(BigInteger.class, Environment.create(args.get(1).getValue()));
            String res = new String();

            int i, n = 0;
            ArrayList<BigInteger> q = new ArrayList<>();
            ArrayList<BigInteger> r = new ArrayList<>();

            q.add(bd);

            do{
                q.add(q.get(n).divide(base));
                r.add(q.get(n).subtract(q.get(n+1).multiply(base)));
                n++;
            }while(q.get(n).compareTo(BigInteger.ZERO) > 0);

            for(i = 0; i < r.size(); i++){
                res = r.get(i).toString() + res;
            }
            return Environment.create(res);
        });

    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        for (Ast.Global statement : ast.getGlobals()) {
            visit(statement);
        }
        for (Ast.Function statement : ast.getFunctions()) {
            visit(statement);
        }

        List<Environment.PlcObject> args = new ArrayList<>();
        try{return scope.lookupFunction("main", 0).invoke(args);}
        catch(RuntimeException e){return scope.lookupFunction("main", -1).invoke(args);}
         //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if(ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        else if( ast.getMutable())
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        else
            throw new RuntimeException("Immutable empty value");
        return Environment.NIL; //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            for (int i = 0; i < ast.getParameters().size(); i++)
            scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
            for(int i = 0; i < ast.getStatements().size() -1 ; i++){
            visit(ast.getStatements().get(i));
            }
            try{
                return Environment.create(visit(ast.getStatements().get(ast.getStatements().size() - 1)).getValue());
            }
            catch(RuntimeException e){
                return Environment.NIL;
            }

        });

            return Environment.NIL;


    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {

            Ast.Expression.Function fun = requireType(Ast.Expression.Function.class, Environment.create(ast.getExpression()));

            List<Environment.PlcObject> list = new ArrayList<>();
            for(int i = 0; i < fun.getArguments().size(); i++){
                list.add(visit(fun.getArguments().get(i)));
            }
            getScope().lookupFunction(fun.getName(), fun.getArguments().size()).invoke(list);
        return Environment.NIL;

    }//done Expression ast

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

        Optional optional = ast.getValue();
        boolean present = optional.isPresent();

        if(present){
            Ast.Expression exp = (Ast.Expression) optional.get();
            scope.defineVariable(ast.getName(), true ,visit(exp));

        }
        else{
            scope.defineVariable(ast.getName(), true , Environment.NIL);
        }

        return Environment.NIL;
    } //Done Declaration

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
//TODO Change to requireType(name.class, ast) as this throws the exceptions

        Ast.Expression.Access name = requireType(Ast.Expression.Access.class, Environment.create(ast.getReceiver()));
        Environment.PlcObject lit = visit(ast.getValue());
            if(getScope().lookupVariable(name.getName()).getMutable()){

                    if(name.getOffset().isPresent() && name.getOffset().get().getClass().getName().equals("plc.project.Ast$Expression$Literal")){
                        List ject = (List) getScope().lookupVariable(name.getName()).getValue().getValue();
                        Ast.Expression.Literal off = (Ast.Expression.Literal) name.getOffset().get();
                        ject.set(new BigInteger(off.getLiteral().toString()).intValue(), lit.getValue());
                    return Environment.NIL;
                    }
                    else{
                        getScope().lookupVariable(name.getName()).setValue(Environment.create(lit.getValue()));
                        return Environment.NIL;
                    }
                }
                else throw new RuntimeException("Not Mutable");



         //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getThenStatements()) {
                    visit(statement);
                }
            }
            //catch(){}
            finally {
                scope = scope.getParent();
            }
        } else if (!(requireType(Boolean.class, visit(ast.getCondition())))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            }
            //catch(){}
            finally {
                scope = scope.getParent();
            }
        }

            return Environment.NIL;
    } //Done IF

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        char c = requireType(Character.class, visit(ast.getCondition()));
            try {
                scope = new Scope(scope);
                for (Ast.Statement.Case statement : ast.getCases()) {
                    if(statement.getValue().isPresent()){
                    char z = requireType(Character.class, visit(statement.getValue().get()));
                        if(c == z){
                            visit(statement);
                            break;
                        }
                    }
                    else{
                    visit(statement);}

                }
            }
            finally {
                scope = scope.getParent();
            }


        return Environment.NIL;
    } //Done Switch

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

                for(Ast.Statement statement : ast.getStatements()){
                    visit(statement);
                }
                return Environment.NIL;
    } //Done Case

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))){
            try{
                scope= new Scope(scope);
                for(Ast.Statement statement : ast.getStatements()){
                    visit(statement);
                }
            }
            //catch(){}
            finally{
                scope = scope.getParent();
            }
        }
            return Environment.NIL;

    }//Done While

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        return visit(ast.getValue()); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null){
            return Environment.NIL;
        }
        return Environment.create( ast.getLiteral());
    }//Done Literal ast

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }//DONE? Group

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left;
        Environment.PlcObject right;
        try{
        left = visit(ast.getLeft());}
        catch(Exception e){
            left = Environment.NIL;
        }
        try{
            right = visit(ast.getRight());}
        catch(Exception e){
            right = Environment.NIL;
        }

        if(Objects.equals(ast.getOperator(), "&&")){
            if(left != null && right != null && left.getValue().equals(true) && right.getValue().equals(true) ){return Environment.create(true);}
            return Environment.create(false);}

        else if(Objects.equals(ast.getOperator(), "||")){
            if(left != null && left.getValue().equals(true) ){return Environment.create(true);}
            else if(right != null && right.getValue().equals(true) ){return Environment.create(true);}
            else
            return Environment.create(false);
        }

        else if((Objects.equals(ast.getOperator(), "<")) || (Objects.equals(ast.getOperator(), ">")) || (Objects.equals(ast.getOperator(), "=="))){
            int result = 0;
            try{
                BigInteger l1 = requireType(BigInteger.class, left);
                BigInteger r1 = requireType(BigInteger.class, right);
                if ((ast.getOperator().equals(">=")) || (ast.getOperator().equals("<="))){
                    if(((r1.compareTo(l1) >= result) && (ast.getOperator().equals(">=")))
                            || ((r1.compareTo(l1)) <= result) && (ast.getOperator().equals("<="))){
                        return Environment.create(true);
                    }
                    return Environment.create(false);
                }

                if ((ast.getOperator().equals("<"))){result = 1;}
                if ((ast.getOperator().equals(">"))){result = -1;}
                if((r1.compareTo(l1)) == result){
                    return Environment.create(true);
                }
                return Environment.create(false);

            }
            catch(Exception e){
                BigDecimal l1 = requireType(BigDecimal.class, left);
                BigDecimal r1 = requireType(BigDecimal.class, right);

                if ((ast.getOperator().equals(">=")) || (ast.getOperator().equals("<="))){
                    if(((r1.compareTo(l1) >= result) && (ast.getOperator().equals(">=")))
                            || ((r1.compareTo(l1)) <= result) && (ast.getOperator().equals("<="))){
                        return Environment.create(true);
                    }
                    return Environment.create(false);
                }

                if ((ast.getOperator().equals("<"))){result = 1;}
                if ((ast.getOperator().equals(">"))){result = -1;}
                if((r1.compareTo(l1)) == result){
                    return Environment.create(true);
                }
                return Environment.create(false);
            }

        }


        else if(Objects.equals(ast.getOperator(), "+") || Objects.equals(ast.getOperator(), "-")){
            if(Objects.equals(ast.getOperator(), "+")){
                try{
                    BigInteger l = requireType(BigInteger.class, left);
                    BigInteger r = requireType(BigInteger.class, right);
                    return Environment.create( l.add(r));
                }
                catch(Exception e){
                    try{
                        BigDecimal l = requireType(BigDecimal.class, left);
                        BigDecimal r = requireType(BigDecimal.class, right);
                        return Environment.create( l.add(r));
                    }
                    catch(Exception g){
                        String l = requireType(String.class, left);
                        String r = requireType(String.class, right);
                        return Environment.create( l + r);}

                }
            }
            if(Objects.equals(ast.getOperator(), "-")){
                try{
                    BigInteger l = requireType(BigInteger.class, left);
                    BigInteger r = requireType(BigInteger.class, right);
                    return Environment.create( l.subtract(r));
                }
                catch(Exception e){

                        BigDecimal l = requireType(BigDecimal.class, left);
                        BigDecimal r = requireType(BigDecimal.class, right);
                        return Environment.create( l.subtract(r));


                }
            }
        }

        else if(Objects.equals(ast.getOperator(), "*") || Objects.equals(ast.getOperator(), "/") || Objects.equals(ast.getOperator(), "^")){
            if(Objects.equals(ast.getOperator(), "*")){
                try{
                    BigInteger l = requireType(BigInteger.class, left);
                    BigInteger r = requireType(BigInteger.class, right);
                    return Environment.create( l.multiply(r));
                }
                catch(Exception e){
                    BigDecimal l = requireType(BigDecimal.class, left);
                    BigDecimal r = requireType(BigDecimal.class, right);
                    return Environment.create( l.multiply(r));
                }
            }
            else if(Objects.equals(ast.getOperator(), "/")){
                try{
                    BigInteger l = requireType(BigInteger.class, left);
                    BigInteger r = requireType(BigInteger.class, right);
                    return Environment.create( l.divide(r));
                }
                catch(Exception e) {
                    BigDecimal l = requireType(BigDecimal.class, left);
                    BigDecimal r = requireType(BigDecimal.class, right);
                    return Environment.create( l.divide(r, 2));
                    }
                }
            else if(Objects.equals(ast.getOperator(), "^")){
                try{
                    BigInteger l = requireType(BigInteger.class, left);
                    BigInteger r = requireType(BigInteger.class, right);
                    return Environment.create( l.pow(r.intValue()));
                }
                catch(Exception e) {
                    BigDecimal l = requireType(BigDecimal.class, left);
                    BigDecimal r = requireType(BigDecimal.class, right);
                    return Environment.create( l.pow(r.intValue()));
                }
            }
        }

        throw new RuntimeException("Not a valid binary operation"); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()){
            try{
                Ast.Expression.Literal lit = requireType(Ast.Expression.Literal.class, Environment.create(ast.getOffset().get())); //is the 1 in list[1]
                Object list = getScope().lookupVariable(ast.getName()).getValue().getValue();
                return Environment.create( ((List)list).get(((BigInteger) lit.getLiteral()).intValue()));
            }
            catch(Exception e){return getScope().lookupVariable(ast.getName()).getValue();}

        }
        else{
            if(getScope().lookupVariable(ast.getName()) != null){
                return getScope().lookupVariable(ast.getName()).getValue();
            }
            throw new RuntimeException("Could not find Variable");
        }
    }//Done Access

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> list = new ArrayList<>();
        for(int i = 0; i < ast.getArguments().size(); i++){
            list.add(visit(ast.getArguments().get(i)));
        }
        //getScope().lookupFunction(ast.getName(), ast.getArguments().size()).invoke(list);

        return Environment.create(getScope().lookupFunction(ast.getName(), ast.getArguments().size()).invoke(list).getValue()); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {

        List<Ast.Expression.Literal> values = new ArrayList<>();
        for(int i = 0; i < ast.getValues().size(); i++){
            Ast.Expression.Literal lit = requireType(Ast.Expression.Literal.class, Environment.create(ast.getValues().get(i)));
            values.add(lit);
        }
        List<Object> list = new ArrayList<>();
        for(int i = 0; i < values.size(); i++){
            list.add(values.get(i).getLiteral());
        }

        return Environment.create(list);
    }//Done PlcList

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}

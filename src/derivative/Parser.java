package derivative;

import static derivative.Algebra.*;

import derivative.Expr.*;
import java.math.BigInteger;
import java.util.List;

/**
 * Reads an equation string into an expression. Multiplication can be written without a sign, and a
 * function has to have brackets, so sin(2X) is fine and sin2X is an error.
 */
final class Parser {
    private static final String SYMBOLS = "XYTe\u03B8";
    //the functions where a power of -1 means the inverse function
    private static final List<String> INVERTIBLE = List.of("sin", "cos", "tan");

    private final String text;
    private int pos = 0;

    Parser(String text){
        this.text = text;
    }

    Expr parse(){
        Expr result = expression();
        if(pos < text.length()){
            throw new IllegalArgumentException("Unexpected '" + text.charAt(pos) + "'");
        }
        return result;
    }

    private Expr expression(){
        Expr left = term();
        while(peek('+') || peek('-')){
            boolean subtract = text.charAt(pos++) == '-';
            Expr right = term();
            left = sum(left, subtract ? prod(MINUS_ONE, right) : right);
        }
        return left;
    }

    private Expr term(){
        Expr left = signedFactor();
        while(true){
            if(peek('*')){
                pos++;
                left = prod(left, signedFactor());
            }else if(peek('/')){
                pos++;
                left = prod(left, pow(signedFactor(), MINUS_ONE));
            }else if(startsFactor()){
                left = prod(left, factor());
            }else{
                return left;
            }
        }
    }

    private Expr signedFactor(){
        if(peek('-')){
            pos++;
            return prod(MINUS_ONE, signedFactor());
        }
        if(peek('+')){
            pos++;
            return signedFactor();
        }
        return factor();
    }

    private Expr factor(){
        Expr base = primary();
        if(peek('^')){
            pos++;
            return pow(base, exponent());
        }
        return base;
    }

    //an exponent without brackets is one item, so X^2Y is (X^2)*Y
    private Expr exponent(){
        if(peek('-')){
            pos++;
            return prod(MINUS_ONE, exponent());
        }
        Expr base = primary();
        if(peek('^')){
            pos++;
            return pow(base, exponent());
        }
        return base;
    }

    private Expr primary(){
        if(pos >= text.length()){
            throw new IllegalArgumentException("Equation is incomplete");
        }
        char c = text.charAt(pos);
        if(c == '('){
            return bracket();
        }
        if(Character.isDigit(c)){
            int start = pos;
            while(pos < text.length() && Character.isDigit(text.charAt(pos))){
                pos++;
            }
            return new Num(new BigInteger(text.substring(start, pos)), BigInteger.ONE);
        }
        String function = functionAtCursor();
        if(function != null){
            pos += function.length();
            return function(function);
        }
        if(SYMBOLS.indexOf(c) >= 0){
            pos++;
            return new Var(Character.toString(c));
        }
        throw new IllegalArgumentException("Unexpected '" + c + "'");
    }

    private Expr bracket(){
        pos++;
        Expr inner = expression();
        if(!peek(')')){
            throw new IllegalArgumentException("Missing )");
        }
        pos++;
        return inner;
    }

    //a function has to have brackets. sin^2(X) means (sin(X))^2, but sin^-1(X) means arcsin(X)
    private Expr function(String name){
        Expr power = null;
        if(peek('^')){
            pos++;
            power = exponent();
        }
        if(!peek('(')){
            throw new IllegalArgumentException(name + " needs brackets");
        }
        if(MINUS_ONE.equals(power) && INVERTIBLE.contains(name)){
            return fn("arc" + name, bracket());
        }
        Expr result = fn(name, bracket());
        return power == null ? result : pow(result, power);
    }

    private boolean startsFactor(){
        if(pos >= text.length()){
            return false;
        }
        char c = text.charAt(pos);
        return c == '(' || Character.isDigit(c) || SYMBOLS.indexOf(c) >= 0 || functionAtCursor() != null;
    }

    private String functionAtCursor(){
        return Differentiator.FUNCTIONS.stream().filter(f -> text.startsWith(f, pos)).findFirst().orElse(null);
    }

    private boolean peek(char c){
        return pos < text.length() && text.charAt(pos) == c;
    }
}

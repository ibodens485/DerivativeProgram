package derivative;

import static derivative.Algebra.*;

import derivative.Expr.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Turns an expression into a string like (X+1)/X^2. */
final class Printer {
    private Printer(){}

    static String print(Expr e){
        if(e instanceof Sum s){
            StringBuilder out = new StringBuilder();
            for(Expr term : s.terms()){
                String text = print(term);
                if(out.length() > 0 && !text.startsWith("-")){
                    out.append('+');
                }
                out.append(text);
            }
            return out.toString();
        }
        return printProduct(e instanceof Prod p ? p.factors() : List.of(e));
    }

    private static String printProduct(List<Expr> factors){
        Num coefficient = ONE;
        List<Expr> top = new ArrayList<>();
        List<Expr> bottom = new ArrayList<>();
        for(Expr factor : factors){
            if(factor instanceof Num n){
                coefficient = coefficient.times(n);
            }else if(factor instanceof Pow p && p.exponent() instanceof Num e && e.n().signum() < 0){
                bottom.add(pow(p.base(), e.times(MINUS_ONE)));
            }else{
                top.add(factor);
            }
        }
        String sign = coefficient.n().signum() < 0 ? "-" : "";
        BigInteger topNumber = coefficient.n().abs();
        String numerator = joinFactors(topNumber.equals(BigInteger.ONE) && !top.isEmpty() ? "" : topNumber.toString(), top);
        if(bottom.isEmpty() && coefficient.isInteger()){
            return sign + numerator;
        }
        String denominator = joinFactors(coefficient.isInteger() ? "" : coefficient.d().toString(), bottom);
        boolean singleFactor = bottom.size() + (coefficient.isInteger() ? 0 : 1) == 1;
        return sign + numerator + "/" + (singleFactor ? denominator : "(" + denominator + ")");
    }

    private static String joinFactors(String number, List<Expr> factors){
        StringBuilder out = new StringBuilder(number);
        for(Expr factor : factors){
            String text = printFactor(factor);
            //a sign is only needed where leaving it out would read as a different number: 3*2^X
            if(out.length() > 0 && Character.isDigit(text.charAt(0))){
                out.append('*');
            }
            out.append(text);
        }
        return out.toString();
    }

    private static String printFactor(Expr e){
        return switch(e){
            case Num n -> isAtom(n) ? print(n) : "(" + print(n) + ")";
            case Var v -> v.name();
            case Sum s -> "(" + print(s) + ")";
            case Prod p -> "(" + print(p) + ")";
            case Fn f -> f.name() + printArgument(f.arg());
            case Pow p -> printPower(p);
        };
    }

    private static String printPower(Pow p){
        boolean wholeExponent = p.exponent() instanceof Num n && n.isNaturalNumber();
        if(p.base() instanceof Fn f && wholeExponent){
            return f.name() + "^" + print(p.exponent()) + printArgument(f.arg());
        }
        String base = isAtom(p.base()) ? print(p.base()) : "(" + print(p.base()) + ")";
        String exponent = isAtom(p.exponent()) ? print(p.exponent()) : "(" + print(p.exponent()) + ")";
        return base + "^" + exponent;
    }

    private static String printArgument(Expr arg){
        return "(" + print(arg) + ")";
    }

    private static boolean isAtom(Expr e){
        return e instanceof Var || (e instanceof Num n && n.isNaturalNumber());
    }
}

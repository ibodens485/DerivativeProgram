package derivative;

import derivative.Expr.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds expressions, simplifying as it goes: X*X^2 is X^3, X+X is 2X and X^0 is 1. */
final class Algebra {
    static final String VARIABLE = "X";
    static final Num ZERO = Num.of(0);
    static final Num ONE = Num.of(1);
    static final Num MINUS_ONE = Num.of(-1);
    static final Var EULER = new Var("e");
    //numbers raised to anything bigger than this are left as a power instead of being worked out
    private static final int MAX_FOLDED_EXPONENT = 64;

    private Algebra(){}

    static Expr sum(Expr... terms){
        //like terms are merged, and each one stays where it first appeared
        Map<Expr, Num> coefficients = new LinkedHashMap<>();
        for(Expr term : terms){
            collectTerm(term, coefficients);
        }
        List<Expr> kept = new ArrayList<>();
        for(Map.Entry<Expr, Num> entry : coefficients.entrySet()){
            if(entry.getValue().n().signum() != 0){
                kept.add(prod(entry.getValue(), entry.getKey()));
            }
        }
        if(kept.isEmpty()){
            return ZERO;
        }
        return kept.size() == 1 ? kept.get(0) : new Sum(kept);
    }

    private static void collectTerm(Expr term, Map<Expr, Num> coefficients){
        if(term instanceof Sum s){
            for(Expr inner : s.terms()){
                collectTerm(inner, coefficients);
            }
        }else if(term instanceof Num n){
            coefficients.merge(ONE, n, Num::plus);
        }else if(term instanceof Prod p && p.factors().get(0) instanceof Num n){
            Expr rest = prod(p.factors().subList(1, p.factors().size()).toArray(new Expr[0]));
            coefficients.merge(rest, n, Num::plus);
        }else{
            coefficients.merge(term, ONE, Num::plus);
        }
    }

    static Expr prod(Expr... factors){
        //powers of the same base are merged, so X*X^2 becomes X^3
        Num[] coefficient = {ONE};
        Map<Expr, Expr> exponents = new LinkedHashMap<>();
        for(Expr factor : factors){
            collectFactor(factor, coefficient, exponents);
        }
        if(coefficient[0].n().signum() == 0){
            return ZERO;
        }
        List<Expr> merged = new ArrayList<>();
        boolean needsAnotherPass = false;
        for(Map.Entry<Expr, Expr> entry : exponents.entrySet()){
            Expr power = pow(entry.getKey(), entry.getValue());
            needsAnotherPass |= power instanceof Num || power instanceof Prod;
            merged.add(power);
        }
        if(needsAnotherPass){
            merged.add(coefficient[0]);
            return prod(merged.toArray(new Expr[0]));
        }
        merged.sort(Comparator.comparingInt(Algebra::printOrder));
        if(!coefficient[0].equals(ONE)){
            merged.add(0, coefficient[0]);
        }
        if(merged.isEmpty()){
            return ONE;
        }
        return merged.size() == 1 ? merged.get(0) : new Prod(merged);
    }

    static void collectFactor(Expr factor, Num[] coefficient, Map<Expr, Expr> exponents){
        if(factor instanceof Num n){
            coefficient[0] = coefficient[0].times(n);
        }else if(factor instanceof Prod p){
            for(Expr inner : p.factors()){
                collectFactor(inner, coefficient, exponents);
            }
        }else if(factor instanceof Pow p){
            mergePower(exponents, p.base(), p.exponent());
        }else{
            mergePower(exponents, factor, ONE);
        }
    }

    //powers of the same base only combine when that makes the exponent simpler. X^2*X^3 and
    //e^X*e^-X do, but e^(sin(X))*e^(X^2) reads better than e^(sin(X)+X^2) and shows the chain rule
    private static void mergePower(Map<Expr, Expr> exponents, Expr base, Expr exponent){
        Expr existing = exponents.get(base);
        if(existing == null){
            exponents.put(base, exponent);
            return;
        }
        Expr merged = sum(existing, exponent);
        if(!(merged instanceof Sum)){
            exponents.put(base, merged);
        }else if(exponent instanceof Num){
            //the plain power keeps the base's place, so X never gets printed as X^1
            exponents.merge(new Pow(base, existing), ONE, Algebra::sum);
            exponents.put(base, exponent);
        }else{
            exponents.merge(new Pow(base, exponent), ONE, Algebra::sum);
        }
    }

    //X and its powers come first, then brackets and other powers, then functions
    private static int printOrder(Expr factor){
        if(isFunctionLike(factor)){
            return 2;
        }
        boolean plainPower = factor instanceof Pow p && p.base() instanceof Var && p.exponent() instanceof Num;
        return factor instanceof Var || plainPower ? 0 : 1;
    }

    private static boolean isFunctionLike(Expr factor){
        return factor instanceof Fn || (factor instanceof Pow p && p.base() instanceof Fn);
    }

    static Expr pow(Expr base, Expr exponent){
        //e^ln(u) is u
        if(base.equals(EULER) && exponent instanceof Fn f && f.name().equals("ln")){
            return f.arg();
        }
        if(exponent instanceof Num e){
            if(e.n().signum() == 0){
                return ONE;
            }
            if(e.equals(ONE)){
                return base;
            }
            //(X^(1/2))^(1/2) is X^(1/4), but (X^2)^(1/2) isn't X when X is negative, so an even power stays apart
            if(!e.isInteger() && base instanceof Pow p && p.exponent() instanceof Num inner
                    && (inner.n().testBit(0) || !inner.d().testBit(0))){
                return pow(p.base(), prod(inner, e));
            }
            if(e.isInteger()){
                if(base instanceof Num b && e.n().abs().compareTo(BigInteger.valueOf(MAX_FOLDED_EXPONENT)) <= 0){
                    return b.raisedTo(e.n().intValue());
                }
                if(base instanceof Pow p){
                    return pow(p.base(), prod(p.exponent(), e));
                }
                if(base instanceof Prod p){
                    return prod(p.factors().stream().map(factor -> pow(factor, e)).toArray(Expr[]::new));
                }
            }
        }
        if(base.equals(ONE)){
            return ONE;
        }
        return new Pow(base, exponent);
    }

    static Expr fn(String name, Expr arg){
        if(name.equals("ln")){
            if(arg.equals(EULER)){
                return ONE;
            }
            //ln(e^u) is u
            if(arg instanceof Pow p && p.base().equals(EULER)){
                return p.exponent();
            }
            if(arg.equals(ONE)){
                return ZERO;
            }
        }
        return new Fn(name, arg);
    }
}

package derivative;

import static derivative.Algebra.*;
import static derivative.Printer.print;

import derivative.Expr.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rearranges finished expressions so that they read well: factoring, multiplying out and ordering. */
final class Simplifier {
    //brackets raised to anything bigger than this aren't multiplied out
    private static final int MAX_EXPANDED_POWER = 4;

    private Simplifier(){}

    //multiplying out lets terms cancel, so (X+1)/(X-3) gives -4/(X-3)^2, but it can also make a top
    //much longer when nothing cancels, so whichever is shorter is kept
    static Expr simplestTop(Expr top){
        Expr multiplied = factorOut(highestPowerFirst(expand(top)));
        Expr left = factorOut(top);
        return print(left).length() < print(multiplied).length() ? left : multiplied;
    }

    //pulls out what every term of a sum has in common: Xe^X+e^X becomes e^X(X+1). Only sums made in
    //working out the derivative are touched, so what's inside a function or a power stays as it was typed
    static Expr factorOut(Expr e){
        if(e instanceof Prod p){
            return prod(p.factors().stream().map(Simplifier::factorOut).toArray(Expr[]::new));
        }
        if(!(e instanceof Sum s)){
            return e;
        }
        Expr rebuilt = sum(s.terms().stream().map(Simplifier::factorOut).toArray(Expr[]::new));
        //3X^2+2X reads better than X(3X+2)
        if(!(rebuilt instanceof Sum terms) || terms.terms().stream().allMatch(Simplifier::isPolynomialTerm)){
            return rebuilt;
        }

        List<Num> coefficients = new ArrayList<>();
        List<Map<Expr, Expr>> powers = new ArrayList<>();
        for(Expr term : terms.terms()){
            Num[] coefficient = {ONE};
            Map<Expr, Expr> exponents = new LinkedHashMap<>();
            collectFactor(term, coefficient, exponents);
            coefficients.add(coefficient[0]);
            powers.add(exponents);
        }
        Map<Expr, Expr> common = commonPowers(powers);
        if(common.isEmpty()){
            return rebuilt;
        }

        Num numberOutside = commonNumber(coefficients);
        Expr[] left = new Expr[terms.terms().size()];
        for(int i=0; i<left.length; i++){
            List<Expr> factors = new ArrayList<>();
            factors.add(coefficients.get(i).times(numberOutside.raisedTo(-1)));
            for(Map.Entry<Expr, Expr> power : powers.get(i).entrySet()){
                Expr shared = common.get(power.getKey());
                factors.add(pow(power.getKey(), shared == null ? power.getValue() : sum(power.getValue(), prod(MINUS_ONE, shared))));
            }
            left[i] = prod(factors.toArray(new Expr[0]));
        }
        List<Expr> outside = new ArrayList<>();
        common.forEach((base, exponent) -> outside.add(pow(base, exponent)));
        outside.add(numberOutside);
        outside.add(tidyInside(sum(left)));
        return prod(outside.toArray(new Expr[0]));
    }

    //the power of each base that's in every term. Exponents have to match or both be numbers with the
    //same sign, because pulling X^-1 out of X+1/X would turn a plain sum into a fraction
    private static Map<Expr, Expr> commonPowers(List<Map<Expr, Expr>> powers){
        Map<Expr, Expr> common = new LinkedHashMap<>(powers.get(0));
        for(Map<Expr, Expr> term : powers.subList(1, powers.size())){
            common.keySet().retainAll(term.keySet());
            common.replaceAll((base, exponent) -> smallerPower(exponent, term.get(base)));
            common.values().removeIf(exponent -> exponent == null);
        }
        return common;
    }

    private static Expr smallerPower(Expr a, Expr b){
        if(a == null || b == null){
            return null;
        }
        if(a.equals(b)){
            return a;
        }
        if(a instanceof Num x && b instanceof Num y && x.n().signum() == y.n().signum()){
            return x.plus(y.times(MINUS_ONE)).n().signum() < 0 ? x : y;
        }
        return null;
    }

    //clears fractions and takes out any shared number, and a minus sign if every term has one
    private static Num commonNumber(List<Num> coefficients){
        BigInteger top = BigInteger.ZERO;
        BigInteger bottom = BigInteger.ONE;
        boolean allNegative = true;
        for(Num coefficient : coefficients){
            top = top.gcd(coefficient.n());
            bottom = bottom.multiply(coefficient.d()).divide(bottom.gcd(coefficient.d()));
            allNegative &= coefficient.n().signum() < 0;
        }
        return Num.make(allNegative ? top.negate() : top, bottom);
    }

    private static Expr tidyInside(Expr inside){
        Expr multiplied = highestPowerFirst(expand(inside));
        return positiveFirst(print(multiplied).length() < print(inside).length() ? multiplied : highestPowerFirst(inside));
    }

    //cos(X)-sin(X) instead of -sin(X)+cos(X)
    private static Expr positiveFirst(Expr e){
        if(!(e instanceof Sum s) || !isNegativeTerm(s.terms().get(0))){
            return e;
        }
        List<Expr> ordered = new ArrayList<>(s.terms());
        for(int i=1; i<ordered.size(); i++){
            if(!isNegativeTerm(ordered.get(i))){
                ordered.add(0, ordered.remove(i));
                return new Sum(ordered);
            }
        }
        return e;
    }

    private static boolean isNegativeTerm(Expr term){
        Expr first = term instanceof Prod p ? p.factors().get(0) : term;
        return first instanceof Num n && n.n().signum() < 0;
    }

    //only for polynomials, since sin(X)-Xcos(X) reads better left how it came out
    private static Expr highestPowerFirst(Expr e){
        if(!(e instanceof Sum s) || !s.terms().stream().allMatch(Simplifier::isPolynomialTerm)){
            return e;
        }
        List<Expr> sorted = new ArrayList<>(s.terms());
        sorted.sort(Comparator.comparingDouble(Simplifier::powerOfX).reversed());
        return new Sum(sorted);
    }

    private static boolean isPolynomialTerm(Expr term){
        if(term instanceof Prod p){
            return p.factors().stream().allMatch(Simplifier::isPolynomialTerm);
        }
        return term instanceof Num || term instanceof Var || (term instanceof Pow p && p.base() instanceof Var && p.exponent() instanceof Num);
    }

    //the power of X in a term like 3X^2, which is 0 for anything that isn't a plain power of X
    private static double powerOfX(Expr term){
        if(term instanceof Prod p){
            return p.factors().stream().mapToDouble(Simplifier::powerOfX).sum();
        }
        if(term instanceof Var v && v.name().equals(VARIABLE)){
            return 1;
        }
        if(term instanceof Pow p && p.base() instanceof Var v && v.name().equals(VARIABLE) && p.exponent() instanceof Num n){
            return n.n().doubleValue() / n.d().doubleValue();
        }
        return 0;
    }

    private static Expr expand(Expr e){
        if(e instanceof Sum s){
            return sum(s.terms().stream().map(Simplifier::expand).toArray(Expr[]::new));
        }
        if(e instanceof Prod p){
            return multiplyOut(p.factors());
        }
        if(e instanceof Pow p && p.base() instanceof Sum && p.exponent() instanceof Num n && n.isNaturalNumber()
                && n.n().compareTo(BigInteger.valueOf(MAX_EXPANDED_POWER)) <= 0){
            return multiplyOut(Collections.nCopies(n.n().intValue(), p.base()));
        }
        return e;
    }

    private static Expr multiplyOut(List<Expr> factors){
        List<Expr> terms = List.of(ONE);
        for(Expr factor : factors){
            Expr expanded = expand(factor);
            List<Expr> parts = expanded instanceof Sum s ? s.terms() : List.of(expanded);
            List<Expr> multiplied = new ArrayList<>();
            for(Expr term : terms){
                for(Expr part : parts){
                    multiplied.add(prod(term, part));
                }
            }
            terms = multiplied;
        }
        return sum(terms.toArray(new Expr[0]));
    }
}

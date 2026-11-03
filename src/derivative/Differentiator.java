package derivative;

import static derivative.Algebra.*;
import static derivative.Simplifier.simplestTop;

import derivative.Expr.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Differentiates an equation string with respect to X, using the usual rules (sum, product, quotient,
 * power, exponential, trig, inverse trig, ln and chain). Y, T and theta are treated as constants, and
 * e is Euler's number.
 */
public final class Differentiator {
    /** The function names, with the arc names first because typing arcsin also finishes spelling sin. */
    public static final List<String> FUNCTIONS = List.of("arcsin", "arccos", "arctan", "sin", "cos", "tan", "sec", "csc", "cot", "ln");
    private static final Num MINUS_HALF = Num.make(BigInteger.ONE.negate(), BigInteger.TWO);

    private Differentiator(){}

    public static String differentiate(String input){
        if(input.isEmpty()){
            return "";
        }
        return Printer.print(Simplifier.factorOut(diff(new Parser(input).parse())));
    }

    private static Expr diff(Expr e){
        return switch(e){
            case Num n -> ZERO;
            case Var v -> v.name().equals(VARIABLE) ? ONE : ZERO;
            case Sum s -> sum(s.terms().stream().map(Differentiator::diff).toArray(Expr[]::new));
            case Prod p -> diffProduct(p.factors());
            case Pow p -> diffPower(p.base(), p.exponent());
            case Fn f -> prod(diffOuterFunction(f.name(), f.arg()), diff(f.arg()));
        };
    }

    //a product with something on the bottom is a quotient
    private static Expr diffProduct(List<Expr> factors){
        List<Expr> top = new ArrayList<>();
        List<Expr> bottom = new ArrayList<>();
        for(Expr factor : factors){
            if(factor instanceof Pow p && p.exponent() instanceof Num e && e.n().signum() < 0){
                bottom.add(pow(p.base(), e.times(MINUS_ONE)));
            }else{
                top.add(factor);
            }
        }
        if(bottom.isEmpty()){
            return productRule(factors);
        }
        return quotientRule(prod(top.toArray(new Expr[0])), prod(bottom.toArray(new Expr[0])));
    }

    //(fg)' = f'g + fg', with one term for each factor when there are more than two
    private static Expr productRule(List<Expr> factors){
        Expr[] terms = new Expr[factors.size()];
        for(int i=0; i<factors.size(); i++){
            Expr[] term = factors.toArray(new Expr[0]);
            term[i] = diff(factors.get(i));
            terms[i] = prod(term);
        }
        return sum(terms);
    }

    //(u/v)' = (u'v - uv') / v^2
    private static Expr quotientRule(Expr u, Expr v){
        if(!dependsOnX(v)){
            return prod(diff(u), pow(v, MINUS_ONE));
        }
        //a constant over something is just the chain rule on 1/something
        if(!dependsOnX(u) && !(v instanceof Prod)){
            return prod(u, diff(pow(v, MINUS_ONE)));
        }
        //with v = w^r the chain rule gives v' = r*w^(r-1)*w', and w^(r-1) cancels from top and bottom,
        //leaving (u'w - r*u*w') / w^(r+1)
        if(v instanceof Pow p && p.exponent() instanceof Num r){
            Expr w = p.base();
            Expr top = simplestTop(sum(prod(diff(u), w), prod(r.times(MINUS_ONE), u, diff(w))));
            return prod(top, pow(w, r.plus(ONE).times(MINUS_ONE)));
        }
        Expr top = simplestTop(sum(prod(diff(u), v), prod(MINUS_ONE, u, diff(v))));
        return prod(top, pow(v, Num.of(-2)));
    }

    private static Expr diffPower(Expr base, Expr exponent){
        if(!dependsOnX(exponent)){
            return prod(exponent, pow(base, sum(exponent, MINUS_ONE)), diff(base));
        }
        if(!dependsOnX(base)){
            return prod(pow(base, exponent), fn("ln", base), diff(exponent));
        }
        //X^X and friends: f^g * (g' * ln(f) + g * f' / f)
        return prod(pow(base, exponent), sum(
            prod(diff(exponent), fn("ln", base)),
            prod(exponent, diff(base), pow(base, MINUS_ONE))
        ));
    }

    private static Expr diffOuterFunction(String name, Expr u){
        return switch(name){
            case "sin" -> fn("cos", u);
            case "cos" -> prod(MINUS_ONE, fn("sin", u));
            case "tan" -> pow(fn("sec", u), Num.of(2));
            case "sec" -> prod(fn("sec", u), fn("tan", u));
            case "csc" -> prod(MINUS_ONE, fn("csc", u), fn("cot", u));
            case "cot" -> prod(MINUS_ONE, pow(fn("csc", u), Num.of(2)));
            case "ln" -> pow(u, MINUS_ONE);
            case "arcsin" -> pow(oneMinusSquare(u), MINUS_HALF);
            case "arccos" -> prod(MINUS_ONE, pow(oneMinusSquare(u), MINUS_HALF));
            case "arctan" -> pow(sum(ONE, pow(u, Num.of(2))), MINUS_ONE);
            default -> throw new IllegalArgumentException("Unknown function " + name);
        };
    }

    private static Expr oneMinusSquare(Expr u){
        return sum(ONE, prod(MINUS_ONE, pow(u, Num.of(2))));
    }

    private static boolean dependsOnX(Expr e){
        return switch(e){
            case Num n -> false;
            case Var v -> v.name().equals(VARIABLE);
            case Sum s -> s.terms().stream().anyMatch(Differentiator::dependsOnX);
            case Prod p -> p.factors().stream().anyMatch(Differentiator::dependsOnX);
            case Pow p -> dependsOnX(p.base()) || dependsOnX(p.exponent());
            case Fn f -> dependsOnX(f.arg());
        };
    }
}

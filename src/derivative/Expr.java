package derivative;

import java.math.BigInteger;
import java.util.List;

/** A mathematical expression as a tree. */
sealed interface Expr {
    record Var(String name) implements Expr {}
    record Sum(List<Expr> terms) implements Expr {}
    record Prod(List<Expr> factors) implements Expr {}
    record Pow(Expr base, Expr exponent) implements Expr {}
    record Fn(String name, Expr arg) implements Expr {}

    /** An exact fraction in lowest terms with a positive denominator. */
    record Num(BigInteger n, BigInteger d) implements Expr {
        static Num of(long value){
            return new Num(BigInteger.valueOf(value), BigInteger.ONE);
        }

        static Num make(BigInteger n, BigInteger d){
            if(d.signum() == 0){
                throw new ArithmeticException("Division by zero");
            }
            BigInteger gcd = n.gcd(d);
            if(d.signum() < 0){
                gcd = gcd.negate();
            }
            return new Num(n.divide(gcd), d.divide(gcd));
        }

        Num plus(Num o){
            return make(n.multiply(o.d).add(o.n.multiply(d)), d.multiply(o.d));
        }

        Num times(Num o){
            return make(n.multiply(o.n), d.multiply(o.d));
        }

        Num raisedTo(int exponent){
            if(exponent >= 0){
                return make(n.pow(exponent), d.pow(exponent));
            }
            return make(d.pow(-exponent), n.pow(-exponent));
        }

        boolean isInteger(){
            return d.equals(BigInteger.ONE);
        }

        boolean isNaturalNumber(){
            return isInteger() && n.signum() >= 0;
        }
    }
}

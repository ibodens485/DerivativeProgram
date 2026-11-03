import derivative.Differentiator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

public class DifferentiatorTest {

    @ParameterizedTest(name = "d/dx({0}) = {1}")
    @DisplayName("Differentiates")
    @CsvSource(delimiter = '|', value = {
        // leftover signs and constants
        "X-5 | 1",
        "5-X | -1",
        "X^2-5+X | 2X+1",
        "X^2-X | 2X-1",
        // exponents of zero and below
        "X^0 | 0",
        "0X^2 | 0",
        "X^-1 | -1/X^2",
        "3X^-2 | -6/X^3",
        "X^(1/2) | 1/(2X^(1/2))",
        // coefficients, constants and other variables
        "2*X | 2",
        "X*X | 2X",
        "3Y | 0",
        "3XY | 3Y",
        "2^X | 2^Xln(2)",
        "e^X | e^X",
        // multi-digit exponents
        "X^12 | 12X^11",
        // trig and ln
        "sin(X) | cos(X)",
        "cos(X) | -sin(X)",
        "tan(X) | sec^2(X)",
        "sec(X) | sec(X)tan(X)",
        "csc(X) | -csc(X)cot(X)",
        "cot(X) | -csc^2(X)",
        "ln(X) | 1/X",
        "3sin(X) | 3cos(X)",
        "X^2+sin(X) | 2X+cos(X)",
        // chain rule
        "(X+1)^2 | 2(X+1)",
        "sin(X^2) | 2Xcos(X^2)",
        "sin(2X) | 2cos(2X)",
        "sin^2(X) | 2sin(X)cos(X)",
        "ln(2X) | 1/X",
        "e^(X^2) | 2Xe^(X^2)",
        "cos(3X+1) | -3sin(3X+1)",
        "tan(X^3) | 3X^2sec^2(X^3)",
        "sec(2X) | 2sec(2X)tan(2X)",
        "csc(X^2) | -2Xcsc(X^2)cot(X^2)",
        "cot(5X) | -5csc^2(5X)",
        "ln(X^2+1) | 2X/(X^2+1)",
        "(X^2+1)^3 | 6X(X^2+1)^2",
        "(X^2+1)^(1/2) | X/(X^2+1)^(1/2)",
        "e^(sin(X)) | e^(sin(X))cos(X)",
        "2^(3X) | 3*2^(3X)ln(2)",
        // chain rule, several layers
        "sin(cos(X)) | -cos(cos(X))sin(X)",
        "sin(cos(tan(X))) | -cos(cos(tan(X)))sin(tan(X))sec^2(X)",
        "(sin(X^2))^3 | 6Xsin^2(X^2)cos(X^2)",
        "e^(sin(X^2)) | 2Xe^(sin(X^2))cos(X^2)",
        "ln(ln(X)) | 1/(Xln(X))",
        "((X^2+1)^2+1)^3 | 12X((X^2+1)^2+1)^2(X^2+1)",
        "sin(1/X) | -cos(1/X)/X^2",
        "e^(e^(X^2)) | 2Xe^(e^(X^2))e^(X^2)",
        // powers only combine when that's simpler
        "X^2*X^3 | 5X^4",
        "e^X*e^X | 2e^(2X)",
        "e^X*e^(-X) | 0",
        "e^(2X)/e^X | e^X",
        "X*X^X*X^X | X^(2X)+XX^(2X)(2ln(X)+2)",
        // roots of powers
        "(X^(1/2))^(1/2) | 1/(4X^(3/4))",
        "(1/X)^(1/2) | -1/(2X^(3/2))",
        "(X^3)^(1/2) | 3X^(1/2)/2",
        "(X^2)^(1/2) | X/(X^2)^(1/2)",
        // inverses cancel
        "e^(ln(X)) | 1",
        "ln(e^(X^2)) | 2X",
        // chain rule on the bottom of a quotient
        "3/(X^2+1) | -6X/(X^2+1)^2",
        "3/(X^2+1)^2 | -12X/(X^2+1)^3",
        "1/sin(X) | -cos(X)/sin^2(X)",
        "X/(X^2+1)^(1/2) | 1/(X^2+1)^(3/2)",
        "X/(X+1)^2 | (-X+1)/(X+1)^3",
        "sin(X)/X^2 | (Xcos(X)-2sin(X))/X^3",
        "((X+1)/(X-1))^2 | (-4X-4)/(X-1)^3",
        // product rule
        "Xsin(X) | sin(X)+Xcos(X)",
        "X^2sin(X) | X(2sin(X)+Xcos(X))",
        "(X+1)(X+2) | 2X+3",
        "2*X*X | 4X",
        "X*sin(X)*cos(X) | sin(X)cos(X)+Xcos^2(X)-Xsin^2(X)",
        // quotient rule
        "X/2 | 1/2",
        "X/Y | 1/Y",
        "1/X | -1/X^2",
        "3/X | -3/X^2",
        "sin(X)/X | (Xcos(X)-sin(X))/X^2",
        "X/sin(X) | (sin(X)-Xcos(X))/sin^2(X)",
        "(X+1)/(X-3) | -4/(X-3)^2",
        "X/(X+1) | 1/(X+1)^2",
        "X^2/(X+1) | (X^2+2X)/(X+1)^2",
        "(X^2+1)/X | (X^2-1)/X^2",
        "1/(X+1)^2 | -2/(X+1)^3",
        "(X+1)^2/(X-1) | (X^2-2X-3)/(X-1)^2",
        "X^2/X | 1",
        "1/2+X/3 | 1/3",
        "X^X | X^X(ln(X)+1)",
        // common factors come out
        "Xe^X | e^X(X+1)",
        "X^2e^X | Xe^X(X+2)",
        "e^Xcos(X) | e^X(cos(X)-sin(X))",
        "X^2ln(X) | X(2ln(X)+1)",
        "(X+1)^2(X-1) | (X+1)(3X-1)",
        "e^X/2+Xe^X/3 | e^X(2X+5)/6",
        "Ysin(X)+YXcos(X) | Y(2cos(X)-Xsin(X))",
        "arcsin(X)arccos(X) | (arccos(X)-arcsin(X))/(1-X^2)^(1/2)",
        "((2+cos(X))e^(arctan(X)))/((1+X)^(1/2)) | e^(arctan(X))(2((2+cos(X))/(1+X^2)-sin(X))(1+X)-(2+cos(X)))/(2(1+X)^(3/2))",
        // a top is only multiplied out when that makes it shorter
        "arcsin(X)/(X+1) | ((X+1)/(1-X^2)^(1/2)-arcsin(X))/(X+1)^2",
        "(X+1)^2/(X-1) | (X^2-2X-3)/(X-1)^2",
        // plain polynomials and sums with nothing in common stay as they are
        "X^3+X^2 | 3X^2+2X",
        "Xsin(X) | sin(X)+Xcos(X)",
        "X+1/X | 1-1/X^2",
        // what's typed inside a function is left alone
        "sin(X^2+Xcos(X)) | (2X+cos(X)-Xsin(X))cos(X^2+Xcos(X))",
        // inverse trig
        "arcsin(X) | 1/(1-X^2)^(1/2)",
        "arccos(X) | -1/(1-X^2)^(1/2)",
        "arctan(X) | 1/(1+X^2)",
        "sin^-1(X) | 1/(1-X^2)^(1/2)",
        "cos^(-1)(X) | -1/(1-X^2)^(1/2)",
        "tan^-1(X) | 1/(1+X^2)",
        "arcsin(2X) | 2/(1-4X^2)^(1/2)",
        "arctan(X^2) | 2X/(1+X^4)",
        "arctan(Y) | 0",
        "arcsin(2) | 0",
        "Xarctan(X) | arctan(X)+X/(1+X^2)",
        "arcsin^2(X) | 2arcsin(X)/(1-X^2)^(1/2)",
        // a power of -1 only means the inverse for sin, cos and tan
        "sec^-1(X) | -tan(X)/sec(X)",
        "ln^-1(X) | -1/(Xln^2(X))"
    })
    void differentiates(String input, String expected) {
        assertEquals(expected, Differentiator.differentiate(input));
    }

    @ParameterizedTest
    @DisplayName("Reports equations that can't be read")
    @ValueSource(strings = {"si", "sin", "sinX", "sin2X", "sin^2X", "lnX", "X+", "(X", "X)", "^2", "1/0", "arcsin", "arcsinX", "sin^-1X", "arc(X)", "arcsec(X)"})
    void rejects(String input) {
        assertThrows(RuntimeException.class, () -> Differentiator.differentiate(input));
    }
}

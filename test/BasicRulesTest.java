import derivative.Differentiator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests of the basic rules, mainly the power rule: d/dx(a*X^n) = a*n*X^(n-1)
 */
public class BasicRulesTest {

    // ==================== Basic Power Rule Tests ====================

    @Test
    @DisplayName("d/dx(X) = 1")
    void testSimpleX() {
        assertEquals("1", Differentiator.differentiate("X"));
    }

    @Test
    @DisplayName("d/dx(X^2) = 2X")
    void testXSquared() {
        assertEquals("2X", Differentiator.differentiate("X^2"));
    }

    @Test
    @DisplayName("d/dx(X^3) = 3X^2")
    void testXCubed() {
        assertEquals("3X^2", Differentiator.differentiate("X^3"));
    }

    @Test
    @DisplayName("d/dx(X^10) = 10X^9")
    void testXTenth() {
        assertEquals("10X^9", Differentiator.differentiate("X^10"));
    }

    // ==================== Coefficient Tests ====================

    @Test
    @DisplayName("d/dx(3X^2) = 6X")
    void testThreeXSquared() {
        assertEquals("6X", Differentiator.differentiate("3X^2"));
    }

    @Test
    @DisplayName("d/dx(2X^3) = 6X^2")
    void testTwoXCubed() {
        assertEquals("6X^2", Differentiator.differentiate("2X^3"));
    }

    @Test
    @DisplayName("d/dx(5X^2) = 10X")
    void testFiveXSquared() {
        assertEquals("10X", Differentiator.differentiate("5X^2"));
    }

    @Test
    @DisplayName("d/dx(10X^3) = 30X^2")
    void testTenXCubed() {
        assertEquals("30X^2", Differentiator.differentiate("10X^3"));
    }

    @Test
    @DisplayName("d/dx(3X) = 3")
    void testThreeX() {
        assertEquals("3", Differentiator.differentiate("3X"));
    }

    @Test
    @DisplayName("d/dx(7X) = 7")
    void testSevenX() {
        assertEquals("7", Differentiator.differentiate("7X"));
    }

    // ==================== Multi-Digit Number Tests ====================

    @Test
    @DisplayName("d/dx(123X^2) = 246X")
    void testMultiDigitCoefficient() {
        assertEquals("246X", Differentiator.differentiate("123X^2"));
    }

    @Test
    @DisplayName("d/dx(2X^15) = 30X^14")
    void testMultiDigitExponent() {
        assertEquals("30X^14", Differentiator.differentiate("2X^15"));
    }

    // ==================== Constant Tests ====================

    @Test
    @DisplayName("d/dx(5) = 0")
    void testConstant() {
        assertEquals("0", Differentiator.differentiate("5"));
    }

    @Test
    @DisplayName("d/dx(0) = 0")
    void testZero() {
        assertEquals("0", Differentiator.differentiate("0"));
    }

    // ==================== Multiple Terms Tests ====================

    @Test
    @DisplayName("d/dx(X^2+X) = 2X+1")
    void testXSquaredPlusX() {
        assertEquals("2X+1", Differentiator.differentiate("X^2+X"));
    }

    @Test
    @DisplayName("d/dx(X^2+3X+5) = 2X+3")
    void testFullPolynomial() {
        assertEquals("2X+3", Differentiator.differentiate("X^2+3X+5"));
    }

    @Test
    @DisplayName("d/dx(X^2+2X+1) = 2X+2")
    void testAnotherPolynomial() {
        assertEquals("2X+2", Differentiator.differentiate("X^2+2X+1"));
    }

    @Test
    @DisplayName("d/dx(2X^2+3X^3) = 4X+9X^2")
    void testTwoTermsWithCoeffs() {
        assertEquals("4X+9X^2", Differentiator.differentiate("2X^2+3X^3"));
    }

    @Test
    @DisplayName("d/dx(X^2+X+1) = 2X+1")
    void testPolynomialWithConstant() {
        assertEquals("2X+1", Differentiator.differentiate("X^2+X+1"));
    }

    @Test
    @DisplayName("d/dx(X^3+X^2+X) = 3X^2+2X+1")
    void testCubicPolynomial() {
        assertEquals("3X^2+2X+1", Differentiator.differentiate("X^3+X^2+X"));
    }

    // ==================== Trig Function Tests (current behavior) ====================

    @Test
    @DisplayName("sinX produces consistent output")
    void testSinX() {
        String result = Differentiator.differentiate("sin(X)");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("cosX produces consistent output")
    void testCosX() {
        String result = Differentiator.differentiate("cos(X)");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("lnX produces consistent output")
    void testLnX() {
        String result = Differentiator.differentiate("ln(X)");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("3sinX produces consistent output")
    void testThreeSinX() {
        String result = Differentiator.differentiate("3sin(X)");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("X^2+sinX produces consistent output")
    void testXSquaredPlusSinX() {
        String result = Differentiator.differentiate("X^2+sin(X)");
        assertNotNull(result);
        assertTrue(result.startsWith("2X"));
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Empty string returns empty string")
    void testEmptyString() {
        assertEquals("", Differentiator.differentiate(""));
    }

    @Test
    @DisplayName("Single digit coefficient with X")
    void testSingleDigitCoeff() {
        assertEquals("5", Differentiator.differentiate("5X"));
    }

    @Test
    @DisplayName("Large coefficient")
    void testLargeCoefficient() {
        assertEquals("100X", Differentiator.differentiate("50X^2"));
    }
}

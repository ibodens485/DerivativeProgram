import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Derive.differentiate(), which writes EquationVariable objects
 * out as a string and differentiates that.
 */
public class DeriveTest {

    private static EquationVariable exponent(char c) {
        EquationVariable e = EquationVariable.newExponent();
        e.getExponent().add(new EquationVariable(c));
        return e;
    }

    private static EquationVariable fraction(char top, char bottom) {
        EquationVariable f = EquationVariable.newFraction();
        f.getNumerator().add(new EquationVariable(top));
        f.getDenominator().add(new EquationVariable(bottom));
        return f;
    }

    @Test
    @DisplayName("Derive: a fraction uses the quotient rule")
    void testFraction() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(fraction('1', 'X'));
        assertEquals("-1/X^2", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: an unfinished fraction is an error")
    void testEmptyFraction() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(EquationVariable.newFraction());
        assertThrows(IllegalArgumentException.class, () -> Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: 3X^2 -> 6X")
    void testThreeXSquared() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('3'));
        eq.add(new EquationVariable('X'));
        eq.add(exponent('2'));
        assertEquals("6X", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: X^2 -> 2X")
    void testXSquared() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('X'));
        eq.add(exponent('2'));
        assertEquals("2X", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: X -> 1")
    void testSimpleX() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('X'));
        assertEquals("1", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: 5X^2 -> 10X")
    void testFiveXSquared() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('5'));
        eq.add(new EquationVariable('X'));
        eq.add(exponent('2'));
        assertEquals("10X", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: X^2+3X+5 -> 2X+3")
    void testFullPolynomial() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('X'));
        eq.add(exponent('2'));
        eq.add(new EquationVariable('+'));
        eq.add(new EquationVariable('3'));
        eq.add(new EquationVariable('X'));
        eq.add(new EquationVariable('+'));
        eq.add(new EquationVariable('5'));
        assertEquals("2X+3", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: sinX produces consistent output")
    void testSinX() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable("sin"));
        eq.add(new EquationVariable('('));
        eq.add(new EquationVariable('X'));
        eq.add(new EquationVariable(')'));
        String result = Derive.differentiate(eq);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Derive: empty equation returns empty string")
    void testEmptyEquation() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        assertEquals("", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: constant returns 0")
    void testConstant() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('5'));
        assertEquals("0", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: 2X^3 -> 6X^2")
    void testTwoXCubed() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('2'));
        eq.add(new EquationVariable('X'));
        eq.add(exponent('3'));
        assertEquals("6X^2", Derive.differentiate(eq));
    }

    @Test
    @DisplayName("Derive: X^2+X -> 2X+1")
    void testXSquaredPlusX() {
        ArrayList<EquationVariable> eq = new ArrayList<>();
        eq.add(new EquationVariable('X'));
        eq.add(exponent('2'));
        eq.add(new EquationVariable('+'));
        eq.add(new EquationVariable('X'));
        assertEquals("2X+1", Derive.differentiate(eq));
    }
}

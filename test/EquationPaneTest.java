import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class EquationPaneTest {
    private static final char NONE = KeyEvent.CHAR_UNDEFINED;

    /** Counts repaints, since nothing is on screen here. */
    private static class Pane extends EquationPane {
        int repaints = 0;
        @Override public void repaint(long t, int x, int y, int w, int h) { repaints++; }
    }

    private Pane pane = new Pane();

    private void press(int code, char c) {
        new KeyEvents(pane).keyPressed(new KeyEvent(pane, KeyEvent.KEY_PRESSED, 0, 0, code, c));
    }

    //< > [ ] are the left, right, up and down arrows, # is backspace and _ is ctrl+6
    private void type(String text) {
        for (char c : text.toCharArray()) {
            switch (c) {
                case '/' -> press(KeyEvent.VK_SLASH, c);
                case '<' -> press(KeyEvent.VK_LEFT, NONE);
                case '>' -> press(KeyEvent.VK_RIGHT, NONE);
                case '[' -> press(KeyEvent.VK_UP, NONE);
                case ']' -> press(KeyEvent.VK_DOWN, NONE);
                case '#' -> press(KeyEvent.VK_BACK_SPACE, '\b');
                case '_' -> pane.dePower();
                default -> press(KeyEvent.VK_UNDEFINED, c);
            }
        }
    }

    private String derivative() {
        return Derive.differentiate(pane.getEquationVariables());
    }

    // ==================== Exponents ====================

    @Test
    @DisplayName("Right at the end of an exponent drops to normal text")
    void rightLeavesExponent() {
        type("x^2");
        int x = pane.getCursorX();
        assertEquals(42, pane.getCursorY());
        type(">");
        assertEquals(x, pane.getCursorX());
        assertEquals(50, pane.getCursorY());
        type("+1");
        assertEquals("2X", derivative());
    }

    @Test
    @DisplayName("Left and right walk the whole equation and come back")
    void walksEquation() {
        type("x^2_+1");
        int endX = pane.getCursorX();
        type("<<<<<<");
        assertEquals(3, pane.getCursorX());
        assertEquals(50, pane.getCursorY());
        type("<");
        assertEquals(3, pane.getCursorX());
        type(">>>>>>");
        assertEquals(endX, pane.getCursorX());
        assertEquals(50, pane.getCursorY());
    }

    @Test
    @DisplayName("An exponent with nothing in it disappears when the cursor leaves")
    void emptyExponentDisappears() {
        type("x^>");
        assertEquals(1, pane.getEquationVariables().size());
        type("^<");
        assertEquals(1, pane.getEquationVariables().size());
    }

    @Test
    @DisplayName("Backspace after an exponent deletes the end of it without going up")
    void backspaceAfterExponent() {
        type("x^23>#");
        assertEquals(50, pane.getCursorY());
        assertEquals("2X", derivative());
        type("#");
        assertEquals(1, pane.getEquationVariables().size());
        assertEquals("1", derivative());
    }

    @Test
    @DisplayName("Multi-digit exponent")
    void multiDigitExponent() {
        type("x^12");
        assertEquals("12X^11", derivative());
    }

    @Test
    @DisplayName("Exponents inside exponents, stopping at three levels")
    void nestedExponent() {
        type("e^x^2");
        assertEquals("2Xe^(X^2)", derivative());
        pane = new Pane();
        type("e^e^x^2");
        assertEquals("2Xe^(e^(X^2))e^(X^2)", derivative());
        int y = pane.getCursorY();
        type("^");
        assertEquals(y, pane.getCursorY());
    }

    @Test
    @DisplayName("De-power stops at normal text")
    void dePowerStopsAtBaseline() {
        type("x_");
        assertEquals(50, pane.getCursorY());
    }

    // ==================== Functions ====================

    @Test
    @DisplayName("Typing at the start of the line inserts in front")
    void insertsAtStart() {
        type("12<<9");
        var vars = pane.getEquationVariables();
        assertEquals('9', vars.get(0).getChar());
        assertEquals('1', vars.get(1).getChar());
        assertEquals(vars.get(0).getXPos() + vars.get(0).getWidth(), vars.get(1).getXPos());
    }

    @Test
    @DisplayName("Function names collapse anywhere on the line without breaking positions")
    void collapsesFunctions() {
        for (String name : new String[] {"sin", "cos", "tan", "sec", "csc", "cot", "ln", "arcsin", "arccos", "arctan"}) {
            pane = new Pane();
            type("3" + name + "x)");
            var vars = pane.getEquationVariables();
            assertEquals(5, vars.size(), name);
            assertEquals(name, vars.get(1).getFunction());
            assertTrue(vars.get(2).isItalic(), name);
            assertEquals('(', vars.get(2).getChar());
            assertFalse(vars.get(3).isItalic(), name);
            assertTrue(vars.get(4).isItalic(), name);
            for (int i = 1; i < vars.size(); i++) {
                assertEquals(vars.get(i-1).getXPos() + vars.get(i-1).getWidth(), vars.get(i).getXPos(), name);
            }
            assertEquals(vars.get(4).getXPos() + vars.get(4).getWidth(), pane.getCursorX(), name);
        }
    }

    @Test
    @DisplayName("Function at the very start of the line")
    void functionAtStart() {
        type("sinx)");
        assertEquals("cos(X)", derivative());
    }

    @Test
    @DisplayName("Arrows step over a function as one item")
    void arrowsOverFunction() {
        type("sinx)<<<<");
        assertEquals(3, pane.getCursorX());
        type("3");
        assertEquals("3cos(X)", derivative());
    }

    @Test
    @DisplayName("Typing a function leaves the cursor inside an italic bracket")
    void functionOpensBracket() {
        type("ln");
        var vars = pane.getEquationVariables();
        assertEquals(2, vars.size());
        assertEquals('(', vars.get(1).getChar());
        assertTrue(vars.get(1).isItalic());
        assertEquals(vars.get(1).getXPos() + vars.get(1).getWidth(), pane.getCursorX());
    }

    @Test
    @DisplayName("Only the ) that closes a function's bracket is italic")
    void onlyMatchingBracketIsItalic() {
        type("sin(x+1)2)");
        var vars = pane.getEquationVariables();
        assertFalse(vars.get(2).isItalic());
        assertFalse(vars.get(6).isItalic());
        assertTrue(vars.get(8).isItalic());
        for (int i = 1; i < vars.size(); i++) {
            assertEquals(vars.get(i-1).getXPos() + vars.get(i-1).getWidth(), vars.get(i).getXPos());
        }
    }

    @Test
    @DisplayName("Deleting the function's ( turns its ) back to normal, and the function is then invalid")
    void deletingBracketResetsClosingBracket() {
        type("sinx)<<#");
        var vars = pane.getEquationVariables();
        assertEquals(3, vars.size());
        assertFalse(vars.get(2).isItalic());
        assertThrows(IllegalArgumentException.class, this::derivative);
    }

    @Test
    @DisplayName("Typing a function in front of a bracket uses that bracket")
    void functionUsesExistingBracket() {
        type("(x)<<<sin");
        var vars = pane.getEquationVariables();
        assertEquals(4, vars.size());
        assertTrue(vars.get(1).isItalic());
        assertTrue(vars.get(3).isItalic());
        assertEquals(vars.get(2).getXPos(), pane.getCursorX());
        assertEquals("cos(X)", derivative());
    }

    @Test
    @DisplayName("^ straight after a function's bracket makes sin^2(X)")
    void powerOfFunction() {
        type("sin^2>>x)");
        assertEquals("2sin(X)cos(X)", derivative());
    }

    @Test
    @DisplayName("A function with no ) is reported")
    void missingClosingBracket() {
        type("sinx");
        assertThrows(IllegalArgumentException.class, this::derivative);
    }

    @Test
    @DisplayName("Function in an exponent stays in the exponent")
    void functionInExponent() {
        type("e^sinx)");
        assertEquals("e^(sin(X))cos(X)", derivative());
    }

    // ==================== Chain rule, typed the way a person would ====================

    @Test
    @DisplayName("Function of a power: sin(X^2)")
    void chainFunctionOfPower() {
        type("sinx^2>)");
        assertEquals("2Xcos(X^2)", derivative());
    }

    @Test
    @DisplayName("Power of a bracket: (X^2+1)^3")
    void chainPowerOfBracket() {
        type("(x^2>+1)^3");
        assertEquals("6X(X^2+1)^2", derivative());
    }

    @Test
    @DisplayName("Function of a function: sin(cos(X))")
    void chainFunctionOfFunction() {
        type("sincosx))");
        assertEquals("-cos(cos(X))sin(X)", derivative());
    }

    @Test
    @DisplayName("Function in an exponent with a power inside it: e^(sin(X^2))")
    void chainThroughExponent() {
        type("e^sinx^2>)");
        assertEquals("2Xe^(sin(X^2))cos(X^2)", derivative());
    }

    @Test
    @DisplayName("Fraction inside a function: sin(1/X)")
    void chainFractionInFunction() {
        type("sin/1]x>)");
        assertEquals("-cos(1/X)/X^2", derivative());
    }

    @Test
    @DisplayName("Function squared: tan^2(X^2)")
    void chainFunctionSquared() {
        type("tan^2>>x^2>)");
        assertEquals("4Xtan(X^2)sec^2(X^2)", derivative());
    }

    // ==================== Fractions ====================

    @Test
    @DisplayName("/ makes an empty fraction with the cursor in the numerator, above the line")
    void slashStartsNumerator() {
        type("/");
        int lineY = 50 - 17 / 2;
        assertTrue(pane.getCursorY() < lineY);
        assertEquals(1, pane.getEquationVariables().size());
        assertTrue(pane.getEquationVariables().get(0).isFraction());
    }

    @Test
    @DisplayName("Down goes to the denominator and up comes back")
    void upAndDownSwitchParts() {
        type("/");
        int numeratorY = pane.getCursorY();
        type("]");
        int denominatorY = pane.getCursorY();
        assertTrue(denominatorY > 50 - 17 / 2);
        type("]");
        assertEquals(denominatorY, pane.getCursorY());
        type("[");
        assertEquals(numeratorY, pane.getCursorY());
        type("[");
        assertEquals(numeratorY, pane.getCursorY());
    }

    @Test
    @DisplayName("Typing goes to the part the cursor is in")
    void typesIntoParts() {
        type("/x]2");
        assertEquals("1/2", derivative());
    }

    @Test
    @DisplayName("In an exponent, down leaves the exponent before it changes part of the fraction")
    void downLeavesExponentFirst() {
        type("/x^2");
        int inExponent = pane.getCursorY();
        type("]");
        int inNumerator = pane.getCursorY();
        assertTrue(inNumerator > inExponent);
        type("]");
        assertTrue(pane.getCursorY() > inNumerator);
        type("5");
        assertEquals("2X/5", derivative());
    }

    @Test
    @DisplayName("In an exponent, up also leaves the exponent first")
    void upLeavesExponentFirst() {
        type("/1]x^2");
        int inExponent = pane.getCursorY();
        type("[");
        int inDenominator = pane.getCursorY();
        assertTrue(inDenominator > inExponent);
        type("[");
        assertTrue(pane.getCursorY() < inDenominator);
    }

    @Test
    @DisplayName("Up and down go into a fraction next to the cursor")
    void entersFractionNextToCursor() {
        type("/1]2>");
        int outside = pane.getCursorY();
        type("]");
        assertTrue(pane.getCursorY() > outside - 8);
        type("5");
        assertEquals("0", derivative());
    }

    @Test
    @DisplayName("Right at the end of a numerator goes past the whole fraction")
    void rightAtEndOfNumerator() {
        type("/1]2[");
        assertTrue(pane.getCursorY() < 50 - 17 / 2);
        type(">");
        assertEquals(50, pane.getCursorY());
        var fraction = pane.getEquationVariables().get(0);
        assertEquals(fraction.getXPos() + fraction.getWidth(), pane.getCursorX());
        type("+x");
        assertEquals(1, fraction.getNumerator().size());
        assertEquals(1, fraction.getDenominator().size());
        assertEquals("1", derivative());
    }

    @Test
    @DisplayName("Right from the end of an empty numerator also leaves the fraction")
    void rightOutOfEmptyNumerator() {
        type("/>");
        assertEquals(50, pane.getCursorY());
        assertEquals(1, pane.getEquationVariables().size());
    }

    @Test
    @DisplayName("Right out of a denominator leaves, and left walks back through both parts")
    void rightAndLeftThroughFraction() {
        type("/1]2");
        int inDenominator = pane.getCursorY();
        type(">");
        assertEquals(50, pane.getCursorY());
        type("<");
        assertEquals(inDenominator, pane.getCursorY());
        type("<<");
        assertTrue(pane.getCursorY() < inDenominator);
        type("<<");
        assertEquals(50, pane.getCursorY());
        assertEquals(3, pane.getCursorX());
    }

    @Test
    @DisplayName("Several fractions on one line")
    void severalFractions() {
        type("/x]2>+/x]3");
        assertEquals("5/6", derivative());
    }

    @Test
    @DisplayName("Text before a fraction stays outside it")
    void textBeforeFraction() {
        type("x/1]x");
        assertEquals("0", derivative());
    }

    @Test
    @DisplayName("A fraction inside a fraction")
    void nestedFraction() {
        type("//x]2]3");
        assertEquals("1/6", derivative());
    }

    @Test
    @DisplayName("A fraction in an exponent")
    void fractionInExponent() {
        type("x^/1]2");
        assertEquals("1/(2X^(1/2))", derivative());
    }

    @Test
    @DisplayName("Function over X goes through the quotient rule")
    void quotientRule() {
        type("/sinx)]x");
        assertEquals("(Xcos(X)-sin(X))/X^2", derivative());
    }

    @Test
    @DisplayName("Function with a fraction inside it")
    void fractionInFunction() {
        type("sin/x]2>)");
        assertEquals("cos(X/2)/2", derivative());
    }

    @Test
    @DisplayName("An unfinished fraction is reported")
    void unfinishedFraction() {
        type("/x");
        assertThrows(IllegalArgumentException.class, this::derivative);
    }

    @Test
    @DisplayName("A long numerator keeps the fraction wide enough, and centred")
    void fractionWidth() {
        type("/123]4");
        var fraction = pane.getEquationVariables().get(0);
        var top = fraction.getNumerator().get(0);
        var bottom = fraction.getDenominator().get(0);
        assertTrue(bottom.getXPos() > top.getXPos());
        assertTrue(fraction.getWidth() >= 3 * top.getWidth());
    }

    @Test
    @DisplayName("Backspace clears a fraction one piece at a time")
    void backspaceThroughFraction() {
        type("/1]2");
        type("#");
        assertEquals(1, pane.getEquationVariables().size());
        type("#");
        type("#");
        assertEquals(1, pane.getEquationVariables().size());
        assertTrue(pane.getEquationVariables().get(0).getNumerator().isEmpty());
        type("#");
        assertEquals(0, pane.getEquationVariables().size());
        assertEquals(50, pane.getCursorY());
    }

    @Test
    @DisplayName("Backspace straight after a fraction goes into its denominator")
    void backspaceAfterFraction() {
        type("/1]2>#");
        assertEquals(1, pane.getEquationVariables().get(0).getDenominator().size());
        type("#");
        assertTrue(pane.getEquationVariables().get(0).getDenominator().isEmpty());
        assertEquals(1, pane.getEquationVariables().get(0).getNumerator().size());
    }

    // ==================== Roots ====================

    private EquationVariable firstRoot() {
        return pane.getEquationVariables().get(0);
    }

    @Test
    @DisplayName("Typing sqrt makes a root with the cursor inside it")
    void sqrtMakesRoot() {
        type("sqrt");
        assertEquals(1, pane.getEquationVariables().size());
        assertTrue(firstRoot().isRoot());
        type("x");
        assertEquals(1, firstRoot().getRadicand().size());
        assertTrue(firstRoot().getRootIndex().isEmpty());
    }

    @Test
    @DisplayName("sqrt typed after other text only takes its own letters")
    void sqrtAfterText() {
        type("2sqrtx");
        var vars = pane.getEquationVariables();
        assertEquals(2, vars.size());
        assertEquals('2', vars.get(0).getChar());
        assertTrue(vars.get(1).isRoot());
    }

    @Test
    @DisplayName("A blank index means a square root")
    void defaultIndexIsTwo() {
        type("sqrtx");
        assertEquals("1/(2X^(1/2))", derivative());
    }

    @Test
    @DisplayName("Up from inside a root goes to the index, and down comes back")
    void upAndDownReachIndex() {
        type("sqrtx");
        int inside = pane.getCursorY();
        type("[");
        assertTrue(pane.getCursorY() < inside);
        type("3");
        assertEquals(1, firstRoot().getRootIndex().size());
        assertEquals("1/(3X^(2/3))", derivative());
        type("]");
        assertEquals(inside, pane.getCursorY());
    }

    @Test
    @DisplayName("Left from the start of a root goes to the index, then out in front")
    void leftReachesIndex() {
        type("sqrtx<<");
        int y = pane.getCursorY();
        assertTrue(y < 50);
        type("4");
        assertEquals("1/(4X^(3/4))", derivative());
        type("<<");
        assertEquals(50, pane.getCursorY());
        assertEquals(3, pane.getCursorX());
    }

    @Test
    @DisplayName("Right goes straight inside a root, past the index, and then out the end")
    void rightThroughRoot() {
        type("sqrtx<<<");
        assertEquals(3, pane.getCursorX());
        type(">");
        type("2");
        assertEquals(2, firstRoot().getRadicand().size());
        type(">>");
        assertEquals(firstRoot().getXPos() + firstRoot().getWidth(), pane.getCursorX());
    }

    @Test
    @DisplayName("Right from the index goes inside the root")
    void rightFromIndex() {
        type("sqrtx[3>");
        type("2");
        assertEquals(1, firstRoot().getRootIndex().size());
        assertEquals(2, firstRoot().getRadicand().size());
    }

    @Test
    @DisplayName("Backspace clears a root one piece at a time")
    void backspaceThroughRoot() {
        type("sqrtx[3>>>");
        assertEquals(50, pane.getCursorY());
        assertEquals(firstRoot().getXPos() + firstRoot().getWidth(), pane.getCursorX());
        type("#");
        assertEquals(1, firstRoot().getRadicand().size());
        type("#");
        assertTrue(firstRoot().getRadicand().isEmpty());
        type("#");
        assertEquals(1, firstRoot().getRootIndex().size());
        type("#");
        assertTrue(firstRoot().getRootIndex().isEmpty());
        type("#");
        assertEquals(0, pane.getEquationVariables().size());
        assertEquals(50, pane.getCursorY());
    }

    @Test
    @DisplayName("An empty root is reported")
    void emptyRoot() {
        type("sqrt");
        assertThrows(IllegalArgumentException.class, this::derivative);
    }

    @Test
    @DisplayName("Chain rule through a root")
    void chainThroughRoot() {
        type("sqrtx^2>+1");
        assertEquals("X/(X^2+1)^(1/2)", derivative());
    }

    @Test
    @DisplayName("Roots combine with functions, fractions and exponents")
    void rootsCombine() {
        pane = new Pane();
        type("sinsqrtx>)");
        assertEquals("cos(X^(1/2))/(2X^(1/2))", derivative());
        pane = new Pane();
        type("/1]sqrtx");
        assertEquals("-1/(2X^(3/2))", derivative());
        pane = new Pane();
        type("sqrtx>^4");
        assertEquals("2X", derivative());
        pane = new Pane();
        type("sqrt/1]x");
        assertEquals("-1/(2X^(3/2))", derivative());
        pane = new Pane();
        type("sqrtsqrtx");
        assertEquals("1/(4X^(3/4))", derivative());
    }

    @Test
    @DisplayName("Root index of zero is reported")
    void zeroIndex() {
        type("sqrtx[0");
        assertThrows(ArithmeticException.class, this::derivative);
    }

    @Test
    @DisplayName("Clicking a root's index puts the cursor there")
    void clickIndex() {
        type("sqrtx[3]>");
        var index = firstRoot().getRootIndex().get(0);
        pane.setCursor(index.getXPos() + index.getWidth(), index.getYPos() - 3);
        type("1");
        assertEquals(2, firstRoot().getRootIndex().size());
    }

    @Test
    @DisplayName("A root fits around what's inside it")
    void rootLayout() {
        type("sqrt123");
        var root = firstRoot();
        var inside = root.getRadicand();
        var last = inside.get(2);
        assertTrue(inside.get(0).getXPos() > root.getXPos());
        assertTrue(last.getXPos() + last.getWidth() < root.getXPos() + root.getWidth());
    }

    // ==================== Mouse and painting ====================

    @Test
    @DisplayName("Clicking at the far left reaches the start")
    void clickFarLeft() {
        type("12");
        pane.setCursor(0, 50);
        assertEquals(3, pane.getCursorX());
    }

    @Test
    @DisplayName("Clicking on a denominator puts the cursor there")
    void clickDenominator() {
        type("/1]2>");
        var denominator = pane.getEquationVariables().get(0).getDenominator().get(0);
        pane.setCursor(denominator.getXPos(), denominator.getYPos() - 5);
        type("7");
        assertEquals(2, pane.getEquationVariables().get(0).getDenominator().size());
    }

    // ==================== Inverse trig ====================

    @Test
    @DisplayName("arcsin, arccos and arctan are one function, not arc followed by sin")
    void typesArcFunctions() {
        type("arcsinx)");
        assertEquals("arcsin", pane.getEquationVariables().get(0).getFunction());
        assertEquals("1/(1-X^2)^(1/2)", derivative());

        pane = new Pane();
        type("arccosx)");
        assertEquals("arccos", pane.getEquationVariables().get(0).getFunction());
        assertEquals("-1/(1-X^2)^(1/2)", derivative());

        pane = new Pane();
        type("arctanx)");
        assertEquals("arctan", pane.getEquationVariables().get(0).getFunction());
        assertEquals("1/(1+X^2)", derivative());
    }

    @Test
    @DisplayName("sin^-1, cos^-1 and tan^-1 typed as an exponent are the inverse functions")
    void typesInversePowers() {
        type("sin^-1>>x)");
        assertEquals("1/(1-X^2)^(1/2)", derivative());

        pane = new Pane();
        type("cos^-1>>x)");
        assertEquals("-1/(1-X^2)^(1/2)", derivative());

        pane = new Pane();
        type("tan^-1>>2x)");
        assertEquals("2/(1+4X^2)", derivative());
    }

    @Test
    @DisplayName("Only a power of exactly -1 means the inverse")
    void otherNegativePowersAreReciprocals() {
        type("sin^-2>>x)");
        assertEquals("-2cos(X)/sin^3(X)", derivative());
    }

    @Test
    @DisplayName("A power after the brackets of sin^-1(X) is a power of the inverse")
    void powerOfInverse() {
        type("sin^-1>>x)^2");
        assertEquals("2arcsin(X)/(1-X^2)^(1/2)", derivative());
    }

    @Test
    @DisplayName("Backspace removes an arc function in one go")
    void deletesArcFunction() {
        type("arctan##");
        assertTrue(pane.getEquationVariables().isEmpty());
        assertEquals(3, pane.getCursorX());
    }

    @Test
    @DisplayName("Painting doesn't ask for another paint")
    void paintDoesNotRepaint() {
        type("/x^2]sinx)");
        pane.repaints = 0;
        Graphics g = new BufferedImage(500, 200, BufferedImage.TYPE_INT_RGB).createGraphics();
        pane.paintComponent(g);
        pane.paintComponent(g);
        assertEquals(0, pane.repaints);
    }
}

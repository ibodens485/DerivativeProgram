import java.awt.Dimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResultPaneTest {
    private final ResultPane pane = new ResultPane();

    @Test
    @DisplayName("A short result keeps the box at its smallest size")
    void shortResult() {
        pane.setResult("2X");
        assertEquals(new Dimension(400, 100), pane.getPreferredSize());
    }

    @Test
    @DisplayName("The box grows for a long result and goes back down afterwards")
    void growsAndShrinks() {
        pane.setResult("X+".repeat(100) + "X");
        assertTrue(pane.getPreferredSize().width > 400);
        assertEquals(pane.getPreferredSize(), pane.getMaximumSize());
        pane.setResult("2X");
        assertEquals(new Dimension(400, 100), pane.getPreferredSize());
    }

    @Test
    @DisplayName("Twice the text takes about twice the width, so the font isn't shrinking")
    void fontStaysTheSameSize() {
        pane.setResult("X+".repeat(100) + "X");
        int one = pane.getPreferredSize().width;
        pane.setResult("X+".repeat(200) + "X");
        int two = pane.getPreferredSize().width;
        assertEquals(2.0, (double) two / one, 0.05);
    }

    @Test
    @DisplayName("The box grows taller for fractions inside fractions")
    void growsTaller() {
        pane.setResult("1/(1+1/(1+1/(1+1/(1+1/(1+1/X)))))");
        assertTrue(pane.getPreferredSize().height > 100);
    }

    @Test
    @DisplayName("A long error grows the box too")
    void longError() {
        pane.setError("x".repeat(200));
        assertTrue(pane.getPreferredSize().width > 400);
    }

    @Test
    @DisplayName("A box that fits is left alone")
    void fits() {
        assertEquals(new Dimension(500, 100),
            ResultPane.capToSpace(new Dimension(500, 100), new Dimension(1000, 700), 15, 15));
    }

    @Test
    @DisplayName("A box too wide for the screen stops at its edge and leaves room for the scroll bar")
    void tooWide() {
        assertEquals(new Dimension(1000, 115),
            ResultPane.capToSpace(new Dimension(3000, 100), new Dimension(1000, 700), 15, 15));
    }

    @Test
    @DisplayName("A box too tall for the screen stops at its edge and leaves room for the scroll bar")
    void tooTall() {
        assertEquals(new Dimension(515, 700),
            ResultPane.capToSpace(new Dimension(500, 2000), new Dimension(1000, 700), 15, 15));
    }
}

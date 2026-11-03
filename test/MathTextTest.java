import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

public class MathTextTest {

    //fractions are shown as [top | bottom], exponents as {exponent} and brackets as <inside>
    private static String show(MathText.Node node) {
        return switch (node) {
            case MathText.Text t -> t.text();
            case MathText.Row r -> r.items().stream().map(MathTextTest::show).collect(Collectors.joining());
            case MathText.Exponent e -> "{" + show(e.inner()) + "}";
            case MathText.Fraction f -> "[" + show(f.top()) + " ; " + show(f.bottom()) + "]";
            case MathText.Bracket b -> "<" + show(b.inner()) + ">";
        };
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("Reads a result into something that can be drawn")
    @CsvSource(delimiter = '|', value = {
        "2X | 2X",
        "3X^2 | 3X{2}",
        "12X^11+1 | 12X{11}+1",
        "2Xe^(X^2) | 2Xe{X{2}}",
        "sec^2(X) | sec{2}<X>",
        "2cos(2X) | 2cos<2X>",
        "-1/X^2 | -[1 ; X{2}]",
        "1/2 | [1 ; 2]",
        "(X+1)/X | [X+1 ; X]",
        "(Xcos(X)-sin(X))/X^2 | [Xcos<X>-sin<X> ; X{2}]",
        "1/(2X^(1/2)) | [1 ; 2X{[1 ; 2]}]",
        "cos(X)/X+2/3 | [cos<X> ; X]+[2 ; 3]",
        "sin(X/2) | sin<[X ; 2]>",
        "2(X+1)/X | [2<X+1> ; X]",
        "(X+1)(X+2) | <X+1><X+2>"
    })
    void parses(String result, String expected) {
        assertEquals(expected, show(MathText.parse(result)));
    }
}

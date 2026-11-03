import java.util.ArrayList;
import java.util.List;

/**
 * Turns a result string like (X+1)/X^2 into a tree saying what to draw: fractions and exponents
 * instead of / and ^.
 */
public final class MathText {
    private MathText(){}

    sealed interface Node permits Text, Row, Exponent, Fraction, Bracket {}
    record Text(String text) implements Node {}
    record Row(List<Node> items) implements Node {}
    record Exponent(Node inner) implements Node {}
    record Fraction(Node top, Node bottom) implements Node {}
    record Bracket(Node inner) implements Node {}

    static Node parse(String text){
        List<Node> items = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for(int i=0; i<text.length(); i++){
            char c = text.charAt(i);
            if(c == '('){
                depth++;
            }else if(c == ')'){
                depth--;
            }else if(depth == 0 && (c == '+' || c == '-')){
                if(i > start){
                    items.add(term(text.substring(start, i)));
                }
                items.add(new Text(Character.toString(c)));
                start = i + 1;
            }
        }
        if(start < text.length()){
            items.add(term(text.substring(start)));
        }
        return rowOf(items);
    }

    private static Node term(String text){
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for(int i=0; i<text.length(); i++){
            char c = text.charAt(i);
            if(c == '('){
                depth++;
            }else if(c == ')'){
                depth--;
            }else if(depth == 0 && c == '/'){
                parts.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(text.substring(start));
        Node result = fractionPart(parts.get(0), parts.size() > 1);
        for(int i=1; i<parts.size(); i++){
            result = new Fraction(result, fractionPart(parts.get(i), true));
        }
        return result;
    }

    //the line of a fraction already groups what's above and below it, so brackets around all of it aren't needed
    private static Node fractionPart(String text, boolean inFraction){
        if(inFraction && text.startsWith("(") && matchingBracket(text, 0) == text.length() - 1){
            return parse(text.substring(1, text.length() - 1));
        }
        return factors(text);
    }

    private static Node factors(String text){
        List<Node> items = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while(i < text.length()){
            char c = text.charAt(i);
            int end = c == '^' ? exponentEnd(text, i + 1) : -1;
            int close = c == '(' ? matchingBracket(text, i) : -1;
            if(end != -1){
                flush(plain, items);
                boolean bracketed = text.charAt(i + 1) == '(';
                items.add(new Exponent(parse(bracketed ? text.substring(i + 2, end - 1) : text.substring(i + 1, end))));
                i = end;
            }else if(close != -1){
                flush(plain, items);
                items.add(new Bracket(parse(text.substring(i + 1, close))));
                i = close + 1;
            }else{
                plain.append(c);
                i++;
            }
        }
        flush(plain, items);
        return rowOf(items);
    }

    private static void flush(StringBuilder plain, List<Node> items){
        if(plain.length() > 0){
            items.add(new Text(plain.toString()));
            plain.setLength(0);
        }
    }

    private static Node rowOf(List<Node> items){
        return items.size() == 1 ? items.get(0) : new Row(items);
    }

    private static int matchingBracket(String text, int open){
        int depth = 0;
        for(int i=open; i<text.length(); i++){
            if(text.charAt(i) == '('){
                depth++;
            }else if(text.charAt(i) == ')' && --depth == 0){
                return i;
            }
        }
        return -1;
    }

    //the index just past the exponent starting at start, or -1 if there isn't one there
    private static int exponentEnd(String text, int start){
        if(start >= text.length()){
            return -1;
        }
        char first = text.charAt(start);
        if(first == '('){
            int close = matchingBracket(text, start);
            return close == -1 ? -1 : close + 1;
        }
        if(Character.isDigit(first)){
            int i = start;
            while(i < text.length() && Character.isDigit(text.charAt(i))){
                i++;
            }
            return i;
        }
        return Character.isLetter(first) ? start + 1 : -1;
    }
}

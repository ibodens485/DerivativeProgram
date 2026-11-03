import java.util.ArrayList;
import java.util.List;

/**
 * One item in the equation: a character, a function name, an exponent, a fraction or a root.
 * Exponents, fractions and roots hold further lists of items, so the equation is a tree.
 */
public class EquationVariable {
    private int xPos;
    private int yPos;
    private char variable;
    private int width;
    private int ascent;
    private int descent;
    private boolean isItalic;
    private String function;
    private ArrayList<EquationVariable> exponent;
    private ArrayList<EquationVariable> numerator;
    private ArrayList<EquationVariable> denominator;
    private ArrayList<EquationVariable> rootIndex;
    private ArrayList<EquationVariable> radicand;

    public EquationVariable(char variable){
        this.variable = variable;
    }

    public EquationVariable(String function){
        this.function = function;
    }

    public static EquationVariable newExponent(){
        EquationVariable e = new EquationVariable('\0');
        e.exponent = new ArrayList<>();
        return e;
    }

    public static EquationVariable newFraction(){
        EquationVariable e = new EquationVariable('\0');
        e.numerator = new ArrayList<>();
        e.denominator = new ArrayList<>();
        return e;
    }

    //a root with nothing in its index is a square root
    public static EquationVariable newRoot(){
        EquationVariable e = new EquationVariable('\0');
        e.rootIndex = new ArrayList<>();
        e.radicand = new ArrayList<>();
        return e;
    }

    public boolean isRoot(){
        return radicand != null;
    }

    public ArrayList<EquationVariable> getRootIndex(){
        return rootIndex;
    }

    public ArrayList<EquationVariable> getRadicand(){
        return radicand;
    }

    public boolean hasTwoParts(){
        return isFraction() || isRoot();
    }

    public ArrayList<EquationVariable> getFirstPart(){
        return isFraction() ? numerator : rootIndex;
    }

    public ArrayList<EquationVariable> getLastPart(){
        return isFraction() ? denominator : radicand;
    }

    public boolean isExponent(){
        return exponent != null;
    }

    public boolean isFraction(){
        return numerator != null;
    }

    public ArrayList<EquationVariable> getExponent(){
        return exponent;
    }

    public ArrayList<EquationVariable> getNumerator(){
        return numerator;
    }

    public ArrayList<EquationVariable> getDenominator(){
        return denominator;
    }

    //the lists held inside this item
    public List<ArrayList<EquationVariable>> getChildren(){
        if(isExponent()){
            return List.of(exponent);
        }
        if(hasTwoParts()){
            return List.of(getFirstPart(), getLastPart());
        }
        return List.of();
    }

    public char getChar(){
        return variable;
    }

    public String getFunction(){
        return function;
    }

    public int getXPos(){
        return xPos;
    }

    public void setXPos(int pos){
        xPos = pos;
    }

    public int getYPos(){
        return yPos;
    }

    public void setYPos(int pos){
        yPos = pos;
    }

    public void setWidth(int w){
        width = w;
    }

    public int getWidth(){
        return width;
    }

    public int getAscent(){
        return ascent;
    }

    public void setAscent(int a){
        ascent = a;
    }

    public int getDescent(){
        return descent;
    }

    public void setDescent(int d){
        descent = d;
    }

    public boolean isFunction(){
        return function != null;
    }

    public boolean isItalic(){
        return isItalic;
    }

    public void setItalic(boolean i){
        isItalic = i;
    }
}

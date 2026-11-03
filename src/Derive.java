import java.util.ArrayList;

import derivative.Differentiator;

/** Differentiates what's in the input, by writing the tree of items out as a string. */
public class Derive {

    public static String differentiate(ArrayList<EquationVariable> eq){
        return Differentiator.differentiate(toText(eq));
    }

    private static String toText(ArrayList<EquationVariable> eq){
        StringBuilder line = new StringBuilder();
        for(EquationVariable e : eq){
            if(e.isExponent()){
                //an exponent the cursor is still sitting in might not have anything in it yet
                if(!e.getExponent().isEmpty()){
                    line.append("^(").append(toText(e.getExponent())).append(")");
                }
            }else if(e.isFraction()){
                if(e.getNumerator().isEmpty() || e.getDenominator().isEmpty()){
                    throw new IllegalArgumentException("A fraction isn't filled in");
                }
                line.append("((").append(toText(e.getNumerator())).append(")/(").append(toText(e.getDenominator())).append("))");
            }else if(e.isRoot()){
                if(e.getRadicand().isEmpty()){
                    throw new IllegalArgumentException("A root isn't filled in");
                }
                String index = e.getRootIndex().isEmpty() ? "2" : toText(e.getRootIndex());
                line.append("((").append(toText(e.getRadicand())).append(")^(1/(").append(index).append(")))");
            }else if(e.isFunction()){
                line.append(e.getFunction());
            }else{
                line.append(e.getChar());
            }
        }
        return line.toString();
    }
}

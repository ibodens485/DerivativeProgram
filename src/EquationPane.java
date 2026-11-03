import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.swing.JComponent;

import derivative.Differentiator;

/**
 * The equation is a tree. equationVariables is the main line, and each exponent, numerator,
 * denominator, root index and inside of a root is another list inside it (a "slot"). The cursor is a slot and an index in that slot.
 * After every edit relayout() works out where everything goes, so nothing stores a position that
 * has to be kept up to date by hand.
 */
public class EquationPane extends JComponent {
    private static final int START_X = 3;
    private static final int BASELINE = 50;
    private static final int MAIN_SIZE = 20;
    private static final int MIN_SIZE = 8;
    private static final int EMPTY_FRACTION_WIDTH = 10;
    private static final int FRACTION_GAP = 2;
    private static final int FRACTION_MARGIN = 2;
    private static final int EMPTY_RADICAND_WIDTH = 10;
    private static final int MAX_EXPONENT_DEPTH = 3;
    //the keys that put in the character they show. x, y and O are handled separately
    private static final String TYPABLE = "sincoealtqr0123456789+-*XT()";

    ArrayList<EquationVariable> equationVariables = new ArrayList<EquationVariable>();
    private ArrayList<EquationVariable> cursorSlot = equationVariables;
    private int cursorIndex = 0;
    private int cursorX = START_X;
    private int cursorY = BASELINE;
    private int cursorHeight = 17;
    private ResultPane resultPane;
    //ArrayLists with the same contents count as equal, and two empty slots must stay different, so slots are compared with ==
    private final Map<ArrayList<EquationVariable>, SlotBox> boxes = new IdentityHashMap<>();

    private static class SlotBox {
        int width, ascent, descent, x, baseline, size;
    }

    /** The best cursor position found so far for a click. */
    private static class Closest {
        ArrayList<EquationVariable> slot;
        int index;
        int distance = Integer.MAX_VALUE;
    }

    /** What holds a slot: the exponent or fraction it belongs to, and the slot that item is in. */
    private record Owner(EquationVariable item, ArrayList<EquationVariable> parentSlot) {}

    public EquationPane() {
        setPreferredSize(new Dimension(400, 100));
        relayout();
    }

    public void handleKey(KeyEvent key){
        int code = key.getKeyCode();
        char typed = key.getKeyChar();
        if(code == KeyEvent.VK_BACK_SPACE){
            deleteComponent();
        }else if(typed == '/'){
            divide();
        }else if(key.isControlDown() && code == KeyEvent.VK_6){
            dePower();
        }else if(typed == '^'){
            power();
        }else if(code == KeyEvent.VK_LEFT){
            moveCursorLeft();
        }else if(code == KeyEvent.VK_RIGHT){
            moveCursorRight();
        }else if(code == KeyEvent.VK_UP){
            moveCursorVertically(true);
        }else if(code == KeyEvent.VK_DOWN){
            moveCursorVertically(false);
        }else if(typed == 'x'){
            addChar('X');
        }else if(typed == 'y'){
            addChar('Y');
        }else if(typed == 'O'){
            addChar('\u03B8');
        }else if(TYPABLE.indexOf(typed) >= 0){
            addChar(typed);
        }else{
            return;
        }
        refresh();
    }

    private void refresh(){
        updateBrackets(equationVariables);
        relayout();
        repaint();
    }

    // ==================== Drawing ====================

    @Override
    public void paintComponent(Graphics g){
        g.setColor(new Color(0));
        g.drawRect(0, 0, 400, 99);
        drawSlot(g, equationVariables);
        //draw cursor
        g.drawLine(cursorX, cursorY - cursorHeight, cursorX, cursorY + 2);
    }

    private void drawSlot(Graphics g, ArrayList<EquationVariable> slot){
        int size = boxes.get(slot).size;
        for(EquationVariable var : slot){
            if(var.isFraction()){
                int lineY = var.getYPos() - lineHeightAbove(size);
                g.drawLine(var.getXPos() + FRACTION_MARGIN, lineY, var.getXPos() + var.getWidth() - FRACTION_MARGIN, lineY);
            }else if(var.isRoot()){
                drawRootSymbol(g, var, size);
            }else if(var.getChildren().isEmpty()){
                g.setFont(fontFor(size, isItalic(var)));
                g.drawString(textOf(var), var.getXPos(), var.getYPos());
            }
            for(ArrayList<EquationVariable> child : var.getChildren()){
                drawSlot(g, child);
            }
        }
    }

    //the tick, the stroke down, the long stroke up, and the line over what's inside
    private void drawRootSymbol(Graphics g, EquationVariable var, int size){
        SlotBox radicand = boxes.get(var.getRadicand());
        int left = var.getXPos() + rootIndexSpace(boxes.get(var.getRootIndex()).width, size);
        int symbol = rootSymbolWidth(size);
        int bottom = var.getYPos() + radicand.descent - 1;
        int top = var.getYPos() - radicand.ascent - 2;
        int hook = rootHookY(var.getYPos(), radicand, size);
        int right = var.getXPos() + var.getWidth() - 1;
        g.drawPolyline(new int[]{left, left + symbol / 4, left + symbol / 2, left + symbol, right},
                       new int[]{hook + 1, hook - 1, bottom, top, top}, 5);
    }

    private boolean isItalic(EquationVariable var){
        return var.isFunction() || var.isItalic();
    }

    private String textOf(EquationVariable var){
        return var.isFunction() ? var.getFunction() : Character.toString(var.getChar());
    }

    private Font fontFor(int size, boolean italic){
        return new Font("Arial", italic ? Font.ITALIC : Font.PLAIN, size);
    }

    // ==================== Layout ====================

    private static int ascentOf(int size){
        return Math.round(size * 0.85f);
    }

    private static int descentOf(int size){
        return Math.round(size * 0.2f);
    }

    //the fraction line sits at half the height of the text
    private static int lineHeightAbove(int size){
        return ascentOf(size) / 2;
    }

    private static int exponentRise(int size){
        return Math.round(size * 0.4f);
    }

    private static int exponentSize(int size){
        return Math.max(MIN_SIZE, Math.round(size * 0.6f));
    }

    //a fraction on the main line is full size, and one inside another fraction is smaller
    private static int fractionSize(int size, int fractionDepth){
        return fractionDepth == 0 ? size : Math.max(MIN_SIZE, Math.round(size * 0.8f));
    }

    private static int rootSymbolWidth(int size){
        return Math.round(size * 0.5f);
    }

    //how far the root symbol has to move right so that its index doesn't go off the left side
    private static int rootIndexSpace(int indexWidth, int size){
        return Math.max(1, indexWidth - rootSymbolWidth(size) / 2);
    }

    //the height of the tick on the root symbol, which the index sits on
    private static int rootHookY(int baseline, SlotBox radicand, int size){
        return baseline + radicand.descent - Math.round(size * 0.5f);
    }

    private void relayout(){
        boxes.clear();
        SlotBox root = measure(equationVariables, MAIN_SIZE, 0);
        //a tall fraction pushes the line down so that the top of it stays in the box
        place(equationVariables, START_X, Math.max(BASELINE, root.ascent + 4));
        SlotBox box = boxes.get(cursorSlot);
        cursorX = cursorXIn(cursorSlot, cursorIndex);
        cursorY = box.baseline;
        cursorHeight = ascentOf(box.size);
    }

    private SlotBox measure(ArrayList<EquationVariable> slot, int size, int fractionDepth){
        SlotBox box = new SlotBox();
        box.size = size;
        box.ascent = ascentOf(size);
        box.descent = descentOf(size);
        for(EquationVariable var : slot){
            measureItem(var, size, fractionDepth);
            box.width += var.getWidth();
            box.ascent = Math.max(box.ascent, var.getAscent());
            box.descent = Math.max(box.descent, var.getDescent());
        }
        boxes.put(slot, box);
        return box;
    }

    private void measureItem(EquationVariable var, int size, int fractionDepth){
        if(var.isExponent()){
            SlotBox inner = measure(var.getExponent(), exponentSize(size), fractionDepth);
            var.setWidth(inner.width);
            var.setAscent(exponentRise(size) + inner.ascent);
            var.setDescent(Math.max(0, inner.descent - exponentRise(size)));
        }else if(var.isFraction()){
            int childSize = fractionSize(size, fractionDepth);
            SlotBox top = measure(var.getNumerator(), childSize, fractionDepth + 1);
            SlotBox bottom = measure(var.getDenominator(), childSize, fractionDepth + 1);
            var.setWidth(Math.max(EMPTY_FRACTION_WIDTH, Math.max(top.width, bottom.width)) + 2 * FRACTION_MARGIN + 2);
            var.setAscent(lineHeightAbove(size) + FRACTION_GAP + top.descent + top.ascent);
            var.setDescent(Math.max(0, FRACTION_GAP + 1 + bottom.ascent + bottom.descent - lineHeightAbove(size)));
        }else if(var.isRoot()){
            SlotBox index = measure(var.getRootIndex(), exponentSize(size), fractionDepth);
            SlotBox radicand = measure(var.getRadicand(), size, fractionDepth);
            var.setWidth(rootIndexSpace(index.width, size) + rootSymbolWidth(size) + Math.max(EMPTY_RADICAND_WIDTH, radicand.width) + 3);
            var.setAscent(Math.max(radicand.ascent + 3, -rootHookY(0, radicand, size) + 2 + index.ascent));
            var.setDescent(radicand.descent);
        }else{
            var.setWidth(getFontMetrics(fontFor(size, isItalic(var))).stringWidth(textOf(var)));
            var.setAscent(ascentOf(size));
            var.setDescent(descentOf(size));
        }
    }

    private void place(ArrayList<EquationVariable> slot, int x, int baseline){
        SlotBox box = boxes.get(slot);
        box.x = x;
        box.baseline = baseline;
        for(EquationVariable var : slot){
            var.setXPos(x);
            var.setYPos(baseline);
            if(var.isExponent()){
                place(var.getExponent(), x, baseline - exponentRise(box.size));
            }else if(var.isFraction()){
                SlotBox top = boxes.get(var.getNumerator());
                SlotBox bottom = boxes.get(var.getDenominator());
                int lineY = baseline - lineHeightAbove(box.size);
                place(var.getNumerator(), x + (var.getWidth() - top.width) / 2, lineY - FRACTION_GAP - top.descent);
                place(var.getDenominator(), x + (var.getWidth() - bottom.width) / 2, lineY + FRACTION_GAP + 1 + bottom.ascent);
            }else if(var.isRoot()){
                SlotBox index = boxes.get(var.getRootIndex());
                SlotBox radicand = boxes.get(var.getRadicand());
                int left = x + rootIndexSpace(index.width, box.size);
                place(var.getRootIndex(), left + rootSymbolWidth(box.size) / 2 - index.width, rootHookY(baseline, radicand, box.size) - 2);
                place(var.getRadicand(), left + rootSymbolWidth(box.size) + 2, baseline);
            }
            x += var.getWidth();
        }
    }

    private int cursorXIn(ArrayList<EquationVariable> slot, int index){
        if(index < slot.size()){
            return slot.get(index).getXPos();
        }
        if(slot.isEmpty()){
            return boxes.get(slot).x;
        }
        EquationVariable last = slot.get(slot.size() - 1);
        return last.getXPos() + last.getWidth();
    }

    // ==================== Typing ====================

    private void addChar(char c){
        cursorSlot.add(cursorIndex, new EquationVariable(c));
        cursorIndex++;
        collapseFunction();
    }

    //if the character just typed finishes a function name, replace the letters with the function
    private void collapseFunction(){
        int typed = cursorIndex - 1;
        for(int start=Math.max(0, typed - 3); start<=typed && start+4<=cursorSlot.size(); start++){
            if(spellsFunction(start, "sqrt")){
                cursorSlot.subList(start, start + 4).clear();
                EquationVariable root = EquationVariable.newRoot();
                cursorSlot.add(start, root);
                cursorSlot = root.getRadicand();
                cursorIndex = 0;
                return;
            }
        }
        for(String name : Differentiator.FUNCTIONS){
            int firstStart = Math.max(0, typed - name.length() + 1);
            for(int start=firstStart; start<=typed && start+name.length()<=cursorSlot.size(); start++){
                if(spellsFunction(start, name)){
                    cursorSlot.subList(start, start + name.length()).clear();
                    cursorSlot.add(start, new EquationVariable(name));
                    cursorIndex = start + 1;
                    openFunctionBracket();
                    return;
                }
            }
        }
    }

    private boolean spellsFunction(int start, String name){
        for(int i=0; i<name.length(); i++){
            EquationVariable var = cursorSlot.get(start + i);
            if(var.isFunction() || var.getChar() != name.charAt(i)){
                return false;
            }
        }
        return true;
    }

    //puts the cursor inside the function's brackets. If a ( already follows the function it becomes
    //the function's bracket, so "sin" typed in front of (X) doesn't leave a ( with no match
    private void openFunctionBracket(){
        boolean hasBracket = cursorIndex < cursorSlot.size() && isChar(cursorSlot.get(cursorIndex), '(');
        if(!hasBracket){
            cursorSlot.add(cursorIndex, new EquationVariable('('));
        }
        cursorIndex++;
    }

    private boolean isChar(EquationVariable var, char c){
        return !var.isFunction() && var.getChildren().isEmpty() && var.getChar() == c;
    }

    //a ( straight after a function is italic, and so is the ) that closes it. Brackets only match inside one slot
    private void updateBrackets(ArrayList<EquationVariable> slot){
        ArrayDeque<Boolean> open = new ArrayDeque<>();
        for(int i=0; i<slot.size(); i++){
            EquationVariable var = slot.get(i);
            for(ArrayList<EquationVariable> child : var.getChildren()){
                updateBrackets(child);
            }
            if(isChar(var, '(')){
                var.setItalic(followsFunction(slot, i));
                open.push(var.isItalic());
            }else if(isChar(var, ')')){
                var.setItalic(!open.isEmpty() && open.pop());
            }
        }
    }

    private boolean followsFunction(ArrayList<EquationVariable> slot, int index){
        if(index == 0){
            return false;
        }
        EquationVariable previous = slot.get(index - 1);
        return previous.isFunction() || (previous.isExponent() && index >= 2 && slot.get(index - 2).isFunction());
    }

    private void deleteComponent(){
        if(cursorIndex > 0){
            deleteItemBeforeCursor();
        }else{
            deleteAtStartOfSlot();
        }
    }

    private void deleteItemBeforeCursor(){
        EquationVariable previous = cursorSlot.get(cursorIndex - 1);
        if(previous.isExponent()){
            ArrayList<EquationVariable> exponent = previous.getExponent();
            EquationVariable last = exponent.isEmpty() ? null : exponent.get(exponent.size() - 1);
            if(last != null && !last.getChildren().isEmpty()){
                moveTo(exponent, exponent.size());
                return;
            }
            //delete the end of the exponent without moving up into it, and the exponent goes with its last character
            if(last != null){
                exponent.remove(exponent.size() - 1);
            }
            if(exponent.isEmpty()){
                cursorSlot.remove(cursorIndex - 1);
                cursorIndex--;
            }
        }else if(previous.hasTwoParts()){
            if(previous.getFirstPart().isEmpty() && previous.getLastPart().isEmpty()){
                cursorSlot.remove(cursorIndex - 1);
                cursorIndex--;
            }else{
                moveTo(previous.getLastPart(), previous.getLastPart().size());
            }
        }else{
            cursorSlot.remove(cursorIndex - 1);
            cursorIndex--;
        }
    }

    private void deleteAtStartOfSlot(){
        Owner owner = findOwner(cursorSlot);
        if(owner == null){
            return;
        }
        int ownerIndex = owner.parentSlot().indexOf(owner.item());
        if(owner.item().hasTwoParts()){
            if(owner.item().getFirstPart().isEmpty() && owner.item().getLastPart().isEmpty()){
                owner.parentSlot().remove(ownerIndex);
                cursorSlot = owner.parentSlot();
                cursorIndex = ownerIndex;
            }else if(cursorSlot == owner.item().getLastPart()){
                moveTo(owner.item().getFirstPart(), owner.item().getFirstPart().size());
            }else{
                moveTo(owner.parentSlot(), ownerIndex);
            }
        }else{
            moveTo(owner.parentSlot(), ownerIndex);
        }
    }

    private void power(){
        EquationVariable next = itemAt(cursorIndex);
        EquationVariable previous = itemAt(cursorIndex - 1);
        if(next != null && next.isExponent()){
            moveTo(next.getExponent(), 0);
        }else if(previous != null && previous.isExponent()){
            moveTo(previous.getExponent(), previous.getExponent().size());
        }else if(exponentDepth() < MAX_EXPONENT_DEPTH){
            //^ straight after sin( means sin^2(X), so the exponent goes in front of the bracket
            if(previous != null && isChar(previous, '(') && next == null && followsFunction(cursorSlot, cursorIndex - 1)){
                cursorIndex--;
            }
            EquationVariable exponent = EquationVariable.newExponent();
            cursorSlot.add(cursorIndex, exponent);
            cursorSlot = exponent.getExponent();
            cursorIndex = 0;
        }
    }

    public void dePower(){
        Owner owner = findOwner(cursorSlot);
        if(owner != null && owner.item().isExponent()){
            moveTo(owner.parentSlot(), owner.parentSlot().indexOf(owner.item()) + 1);
        }
    }

    //puts an empty fraction at the cursor and the cursor in its numerator
    private void divide(){
        EquationVariable fraction = EquationVariable.newFraction();
        cursorSlot.add(cursorIndex, fraction);
        cursorSlot = fraction.getNumerator();
        cursorIndex = 0;
    }

    private EquationVariable itemAt(int index){
        return index >= 0 && index < cursorSlot.size() ? cursorSlot.get(index) : null;
    }

    private int exponentDepth(){
        int depth = 0;
        Owner owner = findOwner(cursorSlot);
        while(owner != null){
            if(owner.item().isExponent()){
                depth++;
            }
            owner = findOwner(owner.parentSlot());
        }
        return depth;
    }

    private Owner findOwner(ArrayList<EquationVariable> slot){
        return findOwnerIn(equationVariables, slot);
    }

    private Owner findOwnerIn(ArrayList<EquationVariable> searching, ArrayList<EquationVariable> slot){
        for(EquationVariable var : searching){
            for(ArrayList<EquationVariable> child : var.getChildren()){
                if(child == slot){
                    return new Owner(var, searching);
                }
                Owner found = findOwnerIn(child, slot);
                if(found != null){
                    return found;
                }
            }
        }
        return null;
    }

    // ==================== Moving the cursor ====================

    //an exponent left with nothing in it disappears when the cursor leaves it
    private void moveTo(ArrayList<EquationVariable> slot, int index){
        if(slot != cursorSlot && cursorSlot.isEmpty()){
            Owner owner = findOwner(cursorSlot);
            if(owner != null && owner.item().isExponent()){
                int ownerIndex = owner.parentSlot().indexOf(owner.item());
                owner.parentSlot().remove(ownerIndex);
                if(slot == owner.parentSlot() && index > ownerIndex){
                    index--;
                }
            }
        }
        cursorSlot = slot;
        cursorIndex = index;
    }

    //clicks go to whichever cursor position is closest
    public void setCursor(int x, int y){
        Closest closest = new Closest();
        findClosest(equationVariables, x, y, closest);
        moveTo(closest.slot, closest.index);
        refresh();
    }

    private void findClosest(ArrayList<EquationVariable> slot, int x, int y, Closest closest){
        SlotBox box = boxes.get(slot);
        int middle = box.baseline - ascentOf(box.size) / 2;
        for(int i=0; i<=slot.size(); i++){
            int distance = Math.abs(cursorXIn(slot, i) - x) + Math.abs(middle - y);
            if(distance < closest.distance){
                closest.distance = distance;
                closest.index = i;
                closest.slot = slot;
            }
        }
        for(EquationVariable var : slot){
            for(ArrayList<EquationVariable> child : var.getChildren()){
                findClosest(child, x, y, closest);
            }
        }
    }

    //Right goes through an exponent, a numerator or the inside of a root and then on past the whole item.
    //Left also stops at the denominator of a fraction and the index of a root. Up and down get to the rest.
    private void moveCursorRight(){
        if(cursorIndex < cursorSlot.size()){
            EquationVariable next = cursorSlot.get(cursorIndex);
            if(next.isExponent()){
                moveTo(next.getExponent(), 0);
            }else if(next.isFraction()){
                moveTo(next.getNumerator(), 0);
            }else if(next.isRoot()){
                moveTo(next.getRadicand(), 0);
            }else{
                cursorIndex++;
            }
            return;
        }
        Owner owner = findOwner(cursorSlot);
        if(owner == null){
            return;
        }
        if(owner.item().isRoot() && cursorSlot == owner.item().getRootIndex()){
            moveTo(owner.item().getRadicand(), 0);
        }else{
            moveTo(owner.parentSlot(), owner.parentSlot().indexOf(owner.item()) + 1);
        }
    }

    private void moveCursorLeft(){
        if(cursorIndex > 0){
            EquationVariable previous = cursorSlot.get(cursorIndex - 1);
            if(previous.isExponent()){
                moveTo(previous.getExponent(), previous.getExponent().size());
            }else if(previous.hasTwoParts()){
                moveTo(previous.getLastPart(), previous.getLastPart().size());
            }else{
                cursorIndex--;
            }
            return;
        }
        Owner owner = findOwner(cursorSlot);
        if(owner == null){
            return;
        }
        if(owner.item().hasTwoParts() && cursorSlot == owner.item().getLastPart()){
            moveTo(owner.item().getFirstPart(), owner.item().getFirstPart().size());
        }else{
            moveTo(owner.parentSlot(), owner.parentSlot().indexOf(owner.item()));
        }
    }

    private void moveCursorVertically(boolean up){
        //an exponent has to be left first, whichever way is pressed
        Owner owner = findOwner(cursorSlot);
        if(owner != null && owner.item().isExponent()){
            moveTo(owner.parentSlot(), owner.parentSlot().indexOf(owner.item()) + 1);
            return;
        }
        if(switchFractionPart(up)){
            return;
        }
        //otherwise go into a fraction, root or exponent right next to the cursor
        for(EquationVariable nearby : new EquationVariable[]{itemAt(cursorIndex), itemAt(cursorIndex - 1)}){
            if(nearby != null && nearby.hasTwoParts()){
                moveToClosestX(up ? nearby.getFirstPart() : nearby.getLastPart());
                return;
            }
        }
        if(up){
            EquationVariable next = itemAt(cursorIndex);
            EquationVariable previous = itemAt(cursorIndex - 1);
            if(next != null && next.isExponent()){
                moveTo(next.getExponent(), 0);
            }else if(previous != null && previous.isExponent()){
                moveTo(previous.getExponent(), previous.getExponent().size());
            }
        }
    }

    //looks outwards for a fraction or root with a part to move to, stopping at an exponent
    private boolean switchFractionPart(boolean up){
        ArrayList<EquationVariable> slot = cursorSlot;
        Owner owner = findOwner(slot);
        while(owner != null && owner.item().hasTwoParts()){
            boolean inFirstPart = slot == owner.item().getFirstPart();
            if(inFirstPart && !up){
                moveToClosestX(owner.item().getLastPart());
                return true;
            }
            if(!inFirstPart && up){
                moveToClosestX(owner.item().getFirstPart());
                return true;
            }
            slot = owner.parentSlot();
            owner = findOwner(slot);
        }
        return false;
    }

    private void moveToClosestX(ArrayList<EquationVariable> slot){
        int closest = 0;
        for(int i=1; i<=slot.size(); i++){
            if(Math.abs(cursorXIn(slot, i) - cursorX) < Math.abs(cursorXIn(slot, closest) - cursorX)){
                closest = i;
            }
        }
        moveTo(slot, closest);
    }

    // ==================== Everything else ====================

    public void derive(){
        try{
            String result = Derive.differentiate(equationVariables);
            if(resultPane != null){
                resultPane.setResult(result);
            }
        }catch(Exception e){
            if(resultPane != null){
                resultPane.setError(e.getMessage());
            }
        }
    }

    public void setResultPane(ResultPane resultPane){
        this.resultPane = resultPane;
    }

    public ArrayList<EquationVariable> getEquationVariables(){
        return equationVariables;
    }

    public int getCursorX(){
        return cursorX;
    }

    public int getCursorY(){
        return cursorY;
    }
}

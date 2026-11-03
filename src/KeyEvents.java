import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Passes keys, clicks and the Derive button on to the equation. */
public class KeyEvents extends MouseAdapter implements KeyListener, ActionListener {
    private final EquationPane q;

    public KeyEvents(EquationPane q){
        this.q = q;
    }

    @Override
    public void keyPressed(KeyEvent e){
        q.handleKey(e);
    }

    @Override
    public void keyTyped(KeyEvent e){}

    @Override
    public void keyReleased(KeyEvent e){}

    @Override
    public void mousePressed(MouseEvent e){
        q.setCursor(e.getX(), e.getY());
        q.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e){
        q.derive();
        q.requestFocusInWindow();
    }
}

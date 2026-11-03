import java.awt.Component;
import java.awt.Cursor;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::showWindow);
    }

    private static void showWindow() {
        EquationPane equation = new EquationPane();
        ResultPane resultPane = new ResultPane();
        equation.setResultPane(resultPane);
        equation.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        KeyEvents events = new KeyEvents(equation);
        equation.addKeyListener(events);
        equation.addMouseListener(events);

        JButton deriveButton = new JButton("Derive");
        deriveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deriveButton.addActionListener(events);

        JPanel equationRow = new JPanel();
        equationRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        equationRow.add(equation);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(deriveButton);
        panel.add(equationRow);
        panel.add(Box.createVerticalStrut(10));
        panel.add(resultPane);

        JFrame frame = new JFrame("Derivatives Program");
        frame.setSize(640, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setVisible(true);
        equation.requestFocusInWindow();
    }
}

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Draws the result at a constant font size. The box grows to fit the result and goes back down for
 * a smaller one, the window grows if the box needs more room, and anything too big for the screen scrolls.
 */

public class ResultPane extends JPanel {
    private static final float FONT_SIZE = 20;
    private static final float EXPONENT_SCALE = 0.65f;
    private static final float EXPONENT_RISE = 0.45f;
    private static final float NESTED_FRACTION_SCALE = 0.8f;
    private static final float LINE_HEIGHT = 0.425f;
    private static final int MARGIN = 5;
    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 100;

    private MathText.Node result = new MathText.Text("");
    private final JComponent canvas;
    private final JScrollPane scroll;

    private record Size(int width, int ascent, int descent) {}

    public ResultPane() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Derivative"));
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        canvas = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                paintResult((Graphics2D) g.create(), getWidth(), getHeight());
            }
        };
        canvas.setOpaque(true);
        scroll = new JScrollPane(canvas);
        scroll.setBorder(null);
        //the cursor has to stay in the equation, so scrolling can't take the focus
        scroll.setFocusable(false);
        scroll.getHorizontalScrollBar().setFocusable(false);
        scroll.getVerticalScrollBar().setFocusable(false);
        add(scroll, BorderLayout.CENTER);
        fitToResult();
    }

    public void setResult(String text) {
        result = MathText.parse(text == null ? "" : text);
        fitToResult();
        repaint();
    }

    //an error is shown as it is, because it isn't maths
    public void setError(String message) {
        result = new MathText.Text("Error: " + message);
        fitToResult();
        repaint();
    }

    private void fitToResult(){
        Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Size size = sizeOf(g, result, FONT_SIZE, 0);
        g.dispose();

        Dimension content = new Dimension(size.width() + 2 * MARGIN, size.ascent() + size.descent() + 2 * MARGIN);
        canvas.setPreferredSize(content);
        Insets insets = getInsets();
        Dimension box = new Dimension(Math.max(MIN_WIDTH, content.width + insets.left + insets.right),
                                      Math.max(MIN_HEIGHT, content.height + insets.top + insets.bottom));
        setBoxSize(box);

        Window window = SwingUtilities.getWindowAncestor(this);
        if(window != null && window.getGraphicsConfiguration() != null){
            growWindow(window, box);
            window.validate();
        }else{
            revalidate();
        }
        //a new result starts from its beginning, not wherever the last one was scrolled to
        scroll.getViewport().setViewPosition(new Point(0, 0));
    }

    private void setBoxSize(Dimension box){
        setPreferredSize(box);
        setMaximumSize(box);
        invalidate();
        //BoxLayout remembers sizes until it's told otherwise, and this size can change twice in one go
        if(getParent() != null){
            getParent().invalidate();
        }
    }

    //the window only ever grows, so a size someone has picked for it isn't undone by a small result
    private void growWindow(Window window, Dimension box){
        Rectangle screen = usableScreen(window.getGraphicsConfiguration());
        Dimension needed = window.getPreferredSize();
        int otherWidth = needed.width - box.width;
        int otherHeight = needed.height - box.height;
        Dimension fitted = capToSpace(box, new Dimension(screen.width - otherWidth, screen.height - otherHeight),
                                      scroll.getVerticalScrollBar().getPreferredSize().width,
                                      scroll.getHorizontalScrollBar().getPreferredSize().height);
        setBoxSize(fitted);

        int width = Math.max(window.getWidth(), fitted.width + otherWidth);
        int height = Math.max(window.getHeight(), fitted.height + otherHeight);
        if(width != window.getWidth() || height != window.getHeight()){
            //growing can't push the window off the side or bottom of the screen
            int x = Math.max(screen.x, Math.min(window.getX(), screen.x + screen.width - width));
            int y = Math.max(screen.y, Math.min(window.getY(), screen.y + screen.height - height));
            window.setBounds(x, y, width, height);
        }
    }

    private static Rectangle usableScreen(GraphicsConfiguration config){
        Rectangle bounds = config.getBounds();
        Insets taskbars = Toolkit.getDefaultToolkit().getScreenInsets(config);
        return new Rectangle(bounds.x + taskbars.left, bounds.y + taskbars.top,
                             bounds.width - taskbars.left - taskbars.right,
                             bounds.height - taskbars.top - taskbars.bottom);
    }

    //the most room the box can have. Scrolling one way uses up room for a scroll bar the other way
    static Dimension capToSpace(Dimension box, Dimension space, int verticalBarWidth, int horizontalBarHeight){
        boolean tooWide = box.width > space.width;
        boolean tooTall = box.height > space.height;
        return new Dimension(Math.min(box.width + (tooTall ? verticalBarWidth : 0), space.width),
                             Math.min(box.height + (tooWide ? horizontalBarHeight : 0), space.height));
    }

    private void paintResult(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        Size size = sizeOf(g, result, FONT_SIZE, 0);
        int baseline = (height - size.ascent() - size.descent()) / 2 + size.ascent();
        draw(g, result, (width - size.width()) / 2, baseline, FONT_SIZE, 0);
        g.dispose();
    }

    private Font fontAt(float size) {
        return new Font("Arial", Font.PLAIN, 1).deriveFont(size);
    }

    private float childSize(float size, int fractionDepth) {
        return fractionDepth == 0 ? size : size * NESTED_FRACTION_SCALE;
    }

    private int gapAt(float size) {
        return Math.max(1, Math.round(size * 0.1f));
    }

    //brackets grow to fit a fraction inside them
    private float bracketSize(Size inner, float size) {
        return Math.max(size, (inner.ascent() + inner.descent()) / 1.15f);
    }

    private Size sizeOf(Graphics2D g, MathText.Node node, float size, int fractionDepth) {
        return switch(node) {
            case MathText.Text t -> new Size(g.getFontMetrics(fontAt(size)).stringWidth(t.text()),
                                             Math.round(size * 0.85f), Math.round(size * 0.2f));
            case MathText.Row r -> {
                int width = 0, ascent = 0, descent = 0;
                for(MathText.Node item : r.items()){
                    Size s = sizeOf(g, item, size, fractionDepth);
                    width += s.width();
                    ascent = Math.max(ascent, s.ascent());
                    descent = Math.max(descent, s.descent());
                }
                yield new Size(width, ascent, descent);
            }
            case MathText.Exponent e -> {
                Size inner = sizeOf(g, e.inner(), size * EXPONENT_SCALE, fractionDepth);
                int rise = Math.round(size * EXPONENT_RISE);
                yield new Size(inner.width(), rise + inner.ascent(), Math.max(0, inner.descent() - rise));
            }
            case MathText.Fraction f -> {
                float inside = childSize(size, fractionDepth);
                Size top = sizeOf(g, f.top(), inside, fractionDepth + 1);
                Size bottom = sizeOf(g, f.bottom(), inside, fractionDepth + 1);
                int line = Math.round(size * LINE_HEIGHT);
                int gap = gapAt(size);
                yield new Size(Math.max(top.width(), bottom.width()) + 6,
                               line + gap + top.descent() + top.ascent(),
                               Math.max(0, gap + bottom.ascent() + bottom.descent() - line));
            }
            case MathText.Bracket b -> {
                Size inner = sizeOf(g, b.inner(), size, fractionDepth);
                FontMetrics metrics = g.getFontMetrics(fontAt(bracketSize(inner, size)));
                yield new Size(inner.width() + metrics.stringWidth("(") + metrics.stringWidth(")"), inner.ascent(), inner.descent());
            }
        };
    }

    private void draw(Graphics2D g, MathText.Node node, int x, int baseline, float size, int fractionDepth) {
        switch(node) {
            case MathText.Text t -> {
                g.setFont(fontAt(size));
                g.drawString(t.text(), x, baseline);
            }
            case MathText.Row r -> {
                for(MathText.Node item : r.items()){
                    draw(g, item, x, baseline, size, fractionDepth);
                    x += sizeOf(g, item, size, fractionDepth).width();
                }
            }
            case MathText.Exponent e ->
                draw(g, e.inner(), x, baseline - Math.round(size * EXPONENT_RISE), size * EXPONENT_SCALE, fractionDepth);
            case MathText.Fraction f -> {
                float inside = childSize(size, fractionDepth);
                Size whole = sizeOf(g, f, size, fractionDepth);
                Size top = sizeOf(g, f.top(), inside, fractionDepth + 1);
                Size bottom = sizeOf(g, f.bottom(), inside, fractionDepth + 1);
                int lineY = baseline - Math.round(size * LINE_HEIGHT);
                int gap = gapAt(size);
                g.drawLine(x + 2, lineY, x + whole.width() - 2, lineY);
                draw(g, f.top(), x + (whole.width() - top.width()) / 2, lineY - gap - top.descent(), inside, fractionDepth + 1);
                draw(g, f.bottom(), x + (whole.width() - bottom.width()) / 2, lineY + gap + bottom.ascent(), inside, fractionDepth + 1);
            }
            case MathText.Bracket b -> {
                Size inner = sizeOf(g, b.inner(), size, fractionDepth);
                float parenSize = bracketSize(inner, size);
                Font font = fontAt(parenSize);
                FontMetrics metrics = g.getFontMetrics(font);
                //a grown bracket is centred on what's inside it instead of sitting on the line
                int parenBaseline = parenSize == size ? baseline
                    : baseline + (inner.descent() - inner.ascent()) / 2 + (metrics.getAscent() - metrics.getDescent()) / 2;
                g.setFont(font);
                g.drawString("(", x, parenBaseline);
                x += metrics.stringWidth("(");
                draw(g, b.inner(), x, baseline, size, fractionDepth);
                g.setFont(font);
                g.drawString(")", x + inner.width(), parenBaseline);
            }
        }
    }
}

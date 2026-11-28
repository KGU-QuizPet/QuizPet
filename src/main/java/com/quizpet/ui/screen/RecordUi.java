package main.java.com.quizpet.ui.screen;

import main.java.com.quizpet.model.Category;
import main.java.com.quizpet.ui.UiStyle;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class RecordUi {
    static final Color BG = UiStyle.BACKGROUND;
    static final Color SURFACE = UiStyle.SURFACE;
    static final Color SUNKEN = UiStyle.SUNKEN;
    static final Color TRACK = new Color(0xF3EADB);
    static final Color LINE = UiStyle.BORDER;
    static final Color SHADOW = UiStyle.SHADOW;
    static final Color INK = UiStyle.TEXT;
    static final Color INK_2 = UiStyle.MUTED;
    static final Color INK_3 = UiStyle.HINT;
    static final Color BRAND = UiStyle.BRAND;
    static final Color BRAND_DARK = new Color(0xD96F00);
    static final Color BRAND_SOFT = new Color(0xFFF0DC);
    static final Color EXP = UiStyle.EXP;
    static final Color EXP_DARK = UiStyle.EXP_DARK;
    static final Color OK = UiStyle.CORRECT;
    static final Color OK_DARK = new Color(0x0B8A4F);
    static final Color OK_SOFT = UiStyle.CORRECT_SOFT;
    static final Color ERR = UiStyle.WRONG;
    static final Color ERR_DARK = new Color(0xC42B20);
    static final Color ERR_SOFT = UiStyle.WRONG_SOFT;
    static final Color DISABLED_BG = new Color(0xEFE6D6);
    static final Color DISABLED_LIP = new Color(0xE1D4BE);
    static final Color DARK_LIP = new Color(0x120E20);

    static final Font H1 = new Font(Font.DIALOG, Font.BOLD, 32);
    static final Font H2 = new Font(Font.DIALOG, Font.BOLD, 19);
    static final Font KPI = new Font(Font.DIALOG, Font.BOLD, 30);
    static final Font BODY = UiStyle.BODY_FONT;
    static final Font BODY_BOLD = UiStyle.BODY_FONT.deriveFont(Font.BOLD);
    static final Font SMALL = UiStyle.BODY_FONT.deriveFont(13f);
    static final Font CAPTION = UiStyle.BODY_FONT.deriveFont(Font.BOLD, 12f);

    static final int PAD_Y = 28;
    static final int PAD_X = 32;
    static final int GAP = 20;
    static final int SHADOW_Y = 4;
    static final int RADIUS_CARD = 20;
    static final int RADIUS_BUTTON = 14;

    private RecordUi() {
    }

    static Color categoryColor(String categoryIdOrName) {
        Category category = category(categoryIdOrName);
        return category == null ? INK_3 : UiStyle.categoryColor(category);
    }

    static Category category(String categoryIdOrName) {
        for (Category category : Category.values()) {
            if (category.getCategoryId().equals(categoryIdOrName) || category.getName().equals(categoryIdOrName)) {
                return category;
            }
        }
        return null;
    }

    static Color statColor(String statType) {
        try {
            return UiStyle.statColor(statType);
        } catch (RuntimeException unknown) {
            return INK_2;
        }
    }

    static Color soft(Color color) {
        return new Color((color.getRed() + 255 * 7) / 8, (color.getGreen() + 255 * 7) / 8,
                (color.getBlue() + 255 * 7) / 8);
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    static JPanel clear(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    static class Card extends JPanel {
        private Color fill;
        private Color border;
        private Color shadow;
        private final int radius;

        Card(java.awt.LayoutManager layout, int padding) {
            this(layout, padding, SURFACE, LINE, SHADOW, RADIUS_CARD);
        }

        Card(java.awt.LayoutManager layout, int padding, Color fill, Color border, Color shadow, int radius) {
            super(layout);
            this.fill = fill;
            this.border = border;
            this.shadow = shadow;
            this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(padding, padding, padding + SHADOW_Y, padding));
        }

        void setColors(Color fill, Color border, Color shadow) {
            this.fill = fill;
            this.border = border;
            this.shadow = shadow;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth() - 1;
            int h = getHeight() - 1 - SHADOW_Y;
            g2.setColor(shadow);
            g2.fillRoundRect(0, SHADOW_Y, w, h, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, radius, radius);
            }
            g2.dispose();
        }
    }

    enum Variant { PRIMARY, DARK, GHOST }

    static final class GameButton extends JButton {
        private final Variant variant;
        private final int height;

        GameButton(String text, Variant variant, int height) {
            super(text);
            this.variant = variant;
            this.height = height;
            setFont(height >= 56 ? BODY_BOLD.deriveFont(17f) : BODY_BOLD);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + (height >= 56 ? 68 : 48), height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            Color face;
            Color lip;
            Color text;
            boolean enabled = isEnabled();
            if (!enabled) {
                face = DISABLED_BG;
                lip = DISABLED_LIP;
                text = INK_3;
            } else if (variant == Variant.PRIMARY) {
                face = BRAND;
                lip = BRAND_DARK;
                text = Color.WHITE;
            } else if (variant == Variant.DARK) {
                face = INK;
                lip = DARK_LIP;
                text = Color.WHITE;
            } else {
                face = SURFACE;
                lip = SHADOW;
                text = INK;
            }
            if (enabled && getModel().isRollover() && !getModel().isPressed()) {
                face = variant == Variant.GHOST ? new Color(0xFFFAF2) : face.brighter();
            }
            int w = getWidth() - 1;
            int h = getHeight() - 1 - SHADOW_Y;
            int lipHeight = enabled && getModel().isPressed() ? 1 : SHADOW_Y;
            int top = SHADOW_Y - lipHeight;
            g2.setColor(lip);
            g2.fillRoundRect(0, SHADOW_Y, w, h, RADIUS_BUTTON, RADIUS_BUTTON);
            g2.setColor(face);
            g2.fillRoundRect(0, top, w, h, RADIUS_BUTTON, RADIUS_BUTTON);
            if (variant == Variant.GHOST && enabled) {
                g2.setColor(LINE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, top + 1, w - 2, h - 2, RADIUS_BUTTON, RADIUS_BUTTON);
            }
            g2.setColor(text);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = top + (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    static final class Pill extends JLabel {
        private Color fill;

        Pill(String text, Color fill, Color textColor, Font font) {
            super(text, SwingConstants.CENTER);
            this.fill = fill;
            setFont(font);
            setForeground(textColor);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        void set(String text, Color fill, Color textColor) {
            setText(text);
            this.fill = fill;
            setForeground(textColor);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static final class Bar extends JComponent {
        private double ratio;
        private Color color;
        private final int barHeight;

        Bar(Color color, int barHeight) {
            this.color = color;
            this.barHeight = barHeight;
            setOpaque(false);
        }

        void set(double ratio, Color color) {
            this.ratio = Math.max(0, Math.min(1, ratio));
            this.color = color;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(80, barHeight);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(20, barHeight);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int y = (getHeight() - barHeight) / 2;
            int w = getWidth();
            g2.setColor(TRACK);
            g2.fillRoundRect(0, y, w, barHeight, barHeight, barHeight);
            int filled = (int) Math.round(w * ratio);
            if (filled > 0) {
                g2.setColor(color);
                g2.fillRoundRect(0, y, Math.max(filled, barHeight), barHeight, barHeight, barHeight);
            }
            g2.dispose();
        }
    }

    static final class Segment extends JPanel {
        private final List<JToggleButton> buttons = new ArrayList<>();
        private final List<String> ids = new ArrayList<>();
        private ButtonGroup group = new ButtonGroup();
        private Consumer<String> onChange = id -> { };
        private boolean updating;

        Segment() {
            super(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        void setOptions(List<HistoryPanel.CategoryOption> options) {
            Objects.requireNonNull(options, "분야 목록이 null입니다.");
            String selected = selectedId();
            removeAll();
            buttons.clear();
            ids.clear();
            group = new ButtonGroup();
            for (HistoryPanel.CategoryOption option : options) {
                JToggleButton button = new SegmentButton(option.name());
                button.addActionListener(e -> {
                    if (!updating) {
                        onChange.accept(option.id());
                    }
                });
                group.add(button);
                buttons.add(button);
                ids.add(option.id());
                add(button);
            }
            select(selected);
            revalidate();
            repaint();
        }

        void select(String id) {
            updating = true;
            try {
                int index = ids.indexOf(id);
                if (index < 0 && !buttons.isEmpty()) {
                    index = 0;
                }
                if (index >= 0) {
                    buttons.get(index).setSelected(true);
                }
            } finally {
                updating = false;
            }
        }

        String selectedId() {
            for (int i = 0; i < buttons.size(); i++) {
                if (buttons.get(i).isSelected()) {
                    return ids.get(i);
                }
            }
            return null;
        }

        void setOnChange(Consumer<String> action) {
            this.onChange = action != null ? action : id -> { };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(SUNKEN);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
        }
    }

    private static final class SegmentButton extends JToggleButton {
        SegmentButton(String text) {
            super(text);
            setFont(SMALL.deriveFont(Font.BOLD));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 30, fm.getHeight() + 14);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            if (isSelected()) {
                g2.setColor(SHADOW);
                g2.fillRoundRect(0, 2, getWidth() - 1, getHeight() - 3, 9, 9);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 9, 9);
            }
            g2.dispose();
            setForeground(isSelected() ? INK : INK_2);
            super.paintComponent(g);
        }
    }
}

package com.clinic.ui;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/** Small vector icons drawn in code, so no image files are needed. */
public final class Icons {

    public enum Type {
        USER, LOCK, EYE, EYE_OFF,
        HOME, PATIENTS, CALENDAR, CALENDAR_CLOCK, RECORDS, SEARCH, REPORTS, SETTINGS,
        BELL, AVATAR, PEOPLE, GROUP, HEART_PULSE, MENU,
        CHEVRON_DOWN, PLUS_CIRCLE, DOCTOR, ARROW_LEFT, MORE_VERTICAL
    }

    public static Icon of(Type type, int size, Color color) {
        return new GlyphIcon(type, size, color);
    }

    private static final class GlyphIcon implements Icon {
        private final Type type;
        private final int size;
        private final Color color;

        GlyphIcon(Type type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        private static BasicStroke round(float w) {
            return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.antialias(g2);
            g2.translate(x, y);
            double s = size / 20.0; // every icon is designed on a 20x20 grid
            g2.scale(s, s);
            g2.setColor(color);

            switch (type) {
                case USER: {
                    g2.fill(new Ellipse2D.Double(5.5, 1, 9, 9));
                    g2.fill(new Arc2D.Double(1.5, 11.5, 17, 16, 0, 180, Arc2D.PIE));
                    break;
                }
                case LOCK: {
                    g2.fill(new RoundRectangle2D.Double(3, 9, 14, 10.5, 3, 3));
                    g2.setStroke(round(2.2f));
                    g2.draw(new Arc2D.Double(5.5, 2, 9, 10, 0, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(5.5, 7, 5.5, 10));
                    g2.draw(new Line2D.Double(14.5, 7, 14.5, 10));
                    break;
                }
                case EYE:
                case EYE_OFF: {
                    Path2D.Double eye = new Path2D.Double();
                    eye.moveTo(1, 10);
                    eye.quadTo(10, -1, 19, 10);
                    eye.quadTo(10, 21, 1, 10);
                    eye.closePath();
                    g2.setStroke(round(1.8f));
                    g2.draw(eye);
                    g2.fill(new Ellipse2D.Double(7.2, 7.2, 5.6, 5.6));
                    if (type == Type.EYE_OFF) {
                        g2.setStroke(round(2f));
                        g2.draw(new Line2D.Double(3, 17, 17, 3));
                    }
                    break;
                }
                case HOME: {
                    Path2D.Double p = new Path2D.Double();
                    p.moveTo(10, 2);
                    p.lineTo(1.5, 9.5);
                    p.lineTo(4, 9.5);
                    p.lineTo(4, 18);
                    p.lineTo(8, 18);
                    p.lineTo(8, 12.5);
                    p.lineTo(12, 12.5);
                    p.lineTo(12, 18);
                    p.lineTo(16, 18);
                    p.lineTo(16, 9.5);
                    p.lineTo(18.5, 9.5);
                    p.closePath();
                    g2.fill(p);
                    break;
                }
                case PATIENTS: { // person with a plus
                    g2.fill(new Ellipse2D.Double(2.4, 2.5, 6.5, 6.5));
                    g2.fill(new Arc2D.Double(0, 10, 10.5, 14, 0, 180, Arc2D.PIE));
                    g2.setStroke(round(2f));
                    g2.draw(new Line2D.Double(15, 11.5, 15, 18));
                    g2.draw(new Line2D.Double(11.8, 14.8, 18.2, 14.8));
                    break;
                }
                case CALENDAR: { // appointments grid
                    Area cal = new Area(new RoundRectangle2D.Double(2, 3.5, 16, 15, 3, 3));
                    for (int r = 0; r < 2; r++) {
                        for (int col = 0; col < 3; col++) {
                            cal.subtract(new Area(new Rectangle2D.Double(4.4 + col * 4.2, 9 + r * 4.2, 2.8, 2.8)));
                        }
                    }
                    g2.fill(cal);
                    g2.fill(new RoundRectangle2D.Double(5.2, 1, 1.8, 4, 1, 1));
                    g2.fill(new RoundRectangle2D.Double(13, 1, 1.8, 4, 1, 1));
                    break;
                }
                case CALENDAR_CLOCK: {
                    Area cal = new Area(new RoundRectangle2D.Double(1.5, 3, 15, 14, 3, 3));
                    for (int r = 0; r < 2; r++) {
                        for (int col = 0; col < 3; col++) {
                            cal.subtract(new Area(new Rectangle2D.Double(3.6 + col * 3.8, 8.5 + r * 3.8, 2.4, 2.4)));
                        }
                    }
                    cal.subtract(new Area(new Ellipse2D.Double(8.5, 8.5, 12, 12))); // gap around the clock
                    g2.fill(cal);
                    g2.fill(new RoundRectangle2D.Double(4.5, 1, 1.8, 4, 1, 1));
                    g2.fill(new RoundRectangle2D.Double(11, 1, 1.8, 4, 1, 1));
                    g2.setStroke(round(1.8f));
                    g2.draw(new Ellipse2D.Double(10, 10, 9, 9));
                    Path2D.Double hands = new Path2D.Double();
                    hands.moveTo(14.5, 12.5);
                    hands.lineTo(14.5, 14.5);
                    hands.lineTo(16.3, 15.6);
                    g2.draw(hands);
                    break;
                }
                case RECORDS: { // document
                    Path2D.Double doc = new Path2D.Double();
                    doc.moveTo(4, 1.5);
                    doc.lineTo(12, 1.5);
                    doc.lineTo(16.5, 6);
                    doc.lineTo(16.5, 18.5);
                    doc.lineTo(4, 18.5);
                    doc.closePath();
                    Area a = new Area(doc);
                    a.subtract(new Area(new Rectangle2D.Double(6.5, 6, 3.5, 1.6)));
                    a.subtract(new Area(new Rectangle2D.Double(6.5, 9.5, 7.5, 1.6)));
                    a.subtract(new Area(new Rectangle2D.Double(6.5, 13, 7.5, 1.6)));
                    g2.fill(a);
                    break;
                }
                case SEARCH: {
                    g2.setStroke(round(2.2f));
                    g2.draw(new Ellipse2D.Double(2.5, 2.5, 10.5, 10.5));
                    g2.draw(new Line2D.Double(11.5, 11.5, 17.5, 17.5));
                    break;
                }
                case REPORTS: { // clipboard
                    g2.setStroke(round(1.8f));
                    g2.draw(new RoundRectangle2D.Double(3.5, 3.5, 13, 15, 3, 3));
                    g2.fill(new RoundRectangle2D.Double(6.5, 1.2, 7, 4.2, 2, 2));
                    g2.draw(new Line2D.Double(6.8, 10, 13.2, 10));
                    g2.draw(new Line2D.Double(6.8, 13.8, 11, 13.8));
                    break;
                }
                case SETTINGS: { // gear
                    Area gear = new Area(new Ellipse2D.Double(3.8, 3.8, 12.4, 12.4));
                    for (int i = 0; i < 8; i++) {
                        AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(i * 45), 10, 10);
                        gear.add(new Area(t.createTransformedShape(new Rectangle2D.Double(8.4, 0.8, 3.2, 4.5))));
                    }
                    gear.subtract(new Area(new Ellipse2D.Double(6.8, 6.8, 6.4, 6.4)));
                    g2.fill(gear);
                    break;
                }
                case BELL: {
                    Path2D.Double bell = new Path2D.Double();
                    bell.moveTo(10, 2.5);
                    bell.curveTo(6, 2.5, 5, 6, 5, 9);
                    bell.lineTo(5, 12.5);
                    bell.lineTo(3, 14.5);
                    bell.lineTo(17, 14.5);
                    bell.lineTo(15, 12.5);
                    bell.lineTo(15, 9);
                    bell.curveTo(15, 6, 14, 2.5, 10, 2.5);
                    bell.closePath();
                    g2.fill(bell);
                    g2.fill(new Ellipse2D.Double(8, 15.5, 4, 3));
                    break;
                }
                case AVATAR: { // silhouette with hair
                    Area a = new Area(new Ellipse2D.Double(4.5, 0.8, 11, 13.5));
                    a.add(new Area(new Arc2D.Double(0.8, 13, 18.4, 13, 0, 180, Arc2D.PIE)));
                    g2.fill(a);
                    break;
                }
                case PEOPLE: {
                    g2.fill(new Ellipse2D.Double(2.5, 3, 6, 6));
                    g2.fill(new Arc2D.Double(0.5, 10.5, 10, 14, 0, 180, Arc2D.PIE));
                    g2.fill(new Ellipse2D.Double(11.5, 3, 6, 6));
                    g2.fill(new Arc2D.Double(9.5, 10.5, 10, 14, 0, 180, Arc2D.PIE));
                    break;
                }
                case GROUP: {
                    g2.fill(new Ellipse2D.Double(1.2, 5.5, 4.6, 4.6));
                    g2.fill(new Arc2D.Double(0, 11.2, 7.4, 10, 0, 180, Arc2D.PIE));
                    g2.fill(new Ellipse2D.Double(14.2, 5.5, 4.6, 4.6));
                    g2.fill(new Arc2D.Double(12.6, 11.2, 7.4, 10, 0, 180, Arc2D.PIE));
                    g2.fill(new Ellipse2D.Double(7, 2.5, 6, 6));
                    g2.fill(new Arc2D.Double(4.5, 9.5, 11, 14, 0, 180, Arc2D.PIE));
                    break;
                }
                case HEART_PULSE: {
                    Area heart = new Area(new Ellipse2D.Double(1, 2.5, 9.2, 9.2));
                    heart.add(new Area(new Ellipse2D.Double(9.8, 2.5, 9.2, 9.2)));
                    Path2D.Double tri = new Path2D.Double();
                    tri.moveTo(1.6, 8.6);
                    tri.lineTo(18.4, 8.6);
                    tri.lineTo(10, 18.5);
                    tri.closePath();
                    heart.add(new Area(tri));
                    Path2D.Double pulse = new Path2D.Double();
                    pulse.moveTo(2.5, 10);
                    pulse.lineTo(6.5, 10);
                    pulse.lineTo(8.2, 6.5);
                    pulse.lineTo(10.6, 13.5);
                    pulse.lineTo(12.3, 10);
                    pulse.lineTo(17.5, 10);
                    heart.subtract(new Area(round(1.7f).createStrokedShape(pulse)));
                    g2.fill(heart);
                    break;
                }
                case CHEVRON_DOWN: {
                    Path2D.Double chevron = new Path2D.Double();
                    chevron.moveTo(4, 7);
                    chevron.lineTo(10, 13);
                    chevron.lineTo(16, 7);
                    g2.setStroke(round(2.2f));
                    g2.draw(chevron);
                    break;
                }
                case PLUS_CIRCLE: { // filled circle with the plus cut out
                    Area disc = new Area(new Ellipse2D.Double(1, 1, 18, 18));
                    disc.subtract(new Area(new Rectangle2D.Double(9, 4.5, 2, 11)));
                    disc.subtract(new Area(new Rectangle2D.Double(4.5, 9, 11, 2)));
                    g2.fill(disc);
                    break;
                }
                case DOCTOR: { // bust with a medical cap
                    Area head = new Area(new Ellipse2D.Double(6.2, 5.4, 7.6, 8));
                    head.subtract(new Area(new RoundRectangle2D.Double(4.5, 0, 11, 6.4, 3, 3))); // gap under the cap
                    g2.fill(head);
                    Area cap = new Area(new RoundRectangle2D.Double(5.5, 0.9, 9, 4.6, 2, 2));
                    cap.subtract(new Area(new Rectangle2D.Double(9.2, 1.7, 1.6, 3.0)));
                    cap.subtract(new Area(new Rectangle2D.Double(8.5, 2.4, 3.0, 1.6)));
                    g2.fill(cap);
                    g2.fill(new Arc2D.Double(2, 14.2, 16, 11, 0, 180, Arc2D.PIE));
                    break;
                }
                case ARROW_LEFT: {
                    Path2D.Double arrow = new Path2D.Double();
                    arrow.moveTo(16, 10);
                    arrow.lineTo(4, 10);
                    arrow.moveTo(9, 4.5);
                    arrow.lineTo(4, 10);
                    arrow.lineTo(9, 15.5);
                    g2.setStroke(round(2.2f));
                    g2.draw(arrow);
                    break;
                }
                case MORE_VERTICAL: { // three dots, stacked
                    g2.fill(new Ellipse2D.Double(8.2, 2.4, 3.6, 3.6));
                    g2.fill(new Ellipse2D.Double(8.2, 8.2, 3.6, 3.6));
                    g2.fill(new Ellipse2D.Double(8.2, 14, 3.6, 3.6));
                    break;
                }
                case MENU: {
                    g2.setStroke(round(2.2f));
                    g2.draw(new Line2D.Double(3, 5, 17, 5));
                    g2.draw(new Line2D.Double(3, 10, 17, 10));
                    g2.draw(new Line2D.Double(3, 15, 17, 15));
                    break;
                }
            }
            g2.dispose();
        }
    }

    private Icons() {}
}
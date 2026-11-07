package com.github.mostafaism1.throttlebrawl.adapter.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.pragmatica.lang.Contract;

import com.github.mostafaism1.throttlebrawl.domain.shared.Move;
import com.github.mostafaism1.throttlebrawl.domain.shared.PropKind;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Strike;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;


/// Procedural sprites. Every draw call receives the sprite's ground point on screen and
/// the projection's pixels-per-meter at that depth, so all shapes are sized in meters.
@Contract
final class SpriteArtist {
    private static final Color TYRE = new Color(0x161616);
    private static final Color CHROME = new Color(0x9EA3A8);
    private static final Color SKIN = new Color(0xC98E62);
    private static final Color TAIL_LIGHT = new Color(0xFF2A2A);
    private static final Font NAME_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private SpriteArtist() {}

    static void rider(Graphics2D g, Rider rider, double cx, double ground, double ppm, boolean showName) {
        var jacket = Palette.jacket(rider.persona().livery());

        if (rider.isCrashed()) {
            fallen(g, jacket, cx, ground, ppm);

            return;
        }

        var saved = g.getTransform();

        g.rotate(rider.lean() * 0.3, cx, ground);
        bike(g, jacket, cx, ground, ppm);
        body(g, rider, jacket, cx, ground, ppm);
        g.setTransform(saved);
        if (showName && ppm > 18) {
            label(g, rider.name(), cx, ground - 2.15 * ppm);
        }
    }

    private static void bike(Graphics2D g, Color jacket, double cx, double ground, double ppm) {
        g.setColor(TYRE);
        g.fill(new RoundRectangle2D.Double(cx - 0.12 * ppm,
                                           ground - 0.62 * ppm,
                                           0.24 * ppm,
                                           0.62 * ppm,
                                           0.12 * ppm,
                                           0.12 * ppm));
        g.setColor(CHROME);
        g.fill(new Rectangle2D.Double(cx - 0.34 * ppm, ground - 0.52 * ppm, 0.12 * ppm, 0.08 * ppm));
        g.setColor(jacket.darker().darker());
        g.fill(new RoundRectangle2D.Double(cx - 0.3 * ppm,
                                           ground - 1.0 * ppm,
                                           0.6 * ppm,
                                           0.5 * ppm,
                                           0.15 * ppm,
                                           0.15 * ppm));
        g.setColor(TAIL_LIGHT);
        g.fill(new Rectangle2D.Double(cx - 0.12 * ppm, ground - 0.72 * ppm, 0.24 * ppm, 0.07 * ppm));
        g.setColor(CHROME);
        g.setStroke(new BasicStroke((float) Math.max(1, 0.05 * ppm)));
        g.draw(new Path2D.Double(new java.awt.geom.Line2D.Double(cx - 0.45 * ppm,
                                                                 ground - 1.25 * ppm,
                                                                 cx + 0.45 * ppm,
                                                                 ground - 1.25 * ppm)));
    }

    private static void body(Graphics2D g, Rider rider, Color jacket, double cx, double ground, double ppm) {
        var reach = rider.strike().map(SpriteArtist::extension).or(0.0);
        var side = rider.strike().map(strike -> strike.attack()
                                                      .side()
                                                      .sign()).or(0);
        var move = rider.strike().map(strike -> strike.attack()
                                                      .move()).or(Move.PUNCH);
        var punching = move == Move.PUNCH
                       ? reach
                       : 0;
        var kicking = move == Move.KICK
                      ? reach
                      : 0;

        g.setStroke(new BasicStroke((float) Math.max(2, 0.13 * ppm), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x23262E));
        leg(g,
            cx,
            ground,
            ppm,
            - 1,
            side == -1
            ? kicking
            : 0);
        leg(g,
            cx,
            ground,
            ppm,
            1,
            side == 1
            ? kicking
            : 0);
        g.setColor(jacket);
        g.fill(new RoundRectangle2D.Double(cx - 0.3 * ppm,
                                           ground - 1.62 * ppm,
                                           0.6 * ppm,
                                           0.72 * ppm,
                                           0.25 * ppm,
                                           0.25 * ppm));
        arm(g,
            jacket,
            cx,
            ground,
            ppm,
            - 1,
            side == -1
            ? punching
            : 0);
        arm(g,
            jacket,
            cx,
            ground,
            ppm,
            1,
            side == 1
            ? punching
            : 0);
        g.setColor(jacket.brighter());
        g.fill(new Ellipse2D.Double(cx - 0.19 * ppm, ground - 1.98 * ppm, 0.38 * ppm, 0.4 * ppm));
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(new Rectangle2D.Double(cx - 0.035 * ppm, ground - 1.97 * ppm, 0.07 * ppm, 0.38 * ppm));
    }

    /// How far a limb is thrown out: grows to full reach at impact, then retracts.
    private static double extension(Strike strike) {
        return Math.sin(Math.PI * Math.clamp(strike.progress(), 0, 1)) * strike.attack()
                                                                               .move()
                                                                               .reach() * 5.0;
    }

    private static void arm(Graphics2D g, Color jacket, double cx, double ground, double ppm, int side, double reach) {
        var shoulderX = cx + side * 0.26 * ppm;
        var shoulderY = ground - 1.48 * ppm;
        var handX = reach > 0
                    ? shoulderX + side * reach * ppm
                    : cx + side * 0.42 * ppm;
        var handY = reach > 0
                    ? shoulderY - 0.05 * ppm
                    : ground - 1.25 * ppm;

        g.setColor(jacket.darker());
        g.draw(new java.awt.geom.Line2D.Double(shoulderX, shoulderY, handX, handY));
        g.setColor(SKIN);
        var fist = 0.12 * ppm;

        g.fill(new Ellipse2D.Double(handX - fist / 2, handY - fist / 2, fist, fist));
    }

    private static void leg(Graphics2D g, double cx, double ground, double ppm, int side, double reach) {
        var hipX = cx + side * 0.18 * ppm;
        var hipY = ground - 0.95 * ppm;
        var footX = reach > 0
                    ? hipX + side * reach * ppm
                    : cx + side * 0.32 * ppm;
        var footY = reach > 0
                    ? hipY + 0.1 * ppm
                    : ground - 0.45 * ppm;

        g.draw(new java.awt.geom.Line2D.Double(hipX, hipY, footX, footY));
    }

    private static void fallen(Graphics2D g, Color jacket, double cx, double ground, double ppm) {
        g.setColor(TYRE);
        g.fill(new Ellipse2D.Double(cx - 1.1 * ppm, ground - 0.35 * ppm, 0.6 * ppm, 0.35 * ppm));
        g.fill(new Ellipse2D.Double(cx - 0.1 * ppm, ground - 0.35 * ppm, 0.6 * ppm, 0.35 * ppm));
        g.setColor(jacket.darker().darker());
        g.fill(new RoundRectangle2D.Double(cx - 0.9 * ppm,
                                           ground - 0.5 * ppm,
                                           1.2 * ppm,
                                           0.3 * ppm,
                                           0.1 * ppm,
                                           0.1 * ppm));
        g.setColor(jacket);
        g.fill(new RoundRectangle2D.Double(cx + 0.35 * ppm,
                                           ground - 0.3 * ppm,
                                           0.8 * ppm,
                                           0.28 * ppm,
                                           0.2 * ppm,
                                           0.2 * ppm));
        g.setColor(jacket.brighter());
        g.fill(new Ellipse2D.Double(cx + 1.1 * ppm, ground - 0.36 * ppm, 0.34 * ppm, 0.34 * ppm));
    }

    static void vehicle(Graphics2D g, Vehicle vehicle, double cx, double ground, double ppm) {
        var height = switch (vehicle.kind()) {
            case HATCHBACK -> 1.45;
            case VAN -> 2.2;
            case LORRY -> 3.4;
        };
        var width = vehicle.kind() == com.github.mostafaism1.throttlebrawl.domain.shared.VehicleKind.LORRY
                    ? 2.4
                    : 1.9;
        var paint = switch (vehicle.kind()) {
            case HATCHBACK -> new Color(0xF2E8CF);
            case VAN -> new Color(0x5C8DBC);
            case LORRY -> new Color(0xB8B8B0);
        };
        var left = cx - width / 2 * ppm;

        g.setColor(TYRE);
        g.fill(new Rectangle2D.Double(left + 0.1 * ppm, ground - 0.35 * ppm, 0.35 * ppm, 0.35 * ppm));
        g.fill(new Rectangle2D.Double(cx + (width / 2 - 0.45) * ppm, ground - 0.35 * ppm, 0.35 * ppm, 0.35 * ppm));
        g.setColor(paint);
        g.fill(new RoundRectangle2D.Double(left,
                                           ground - height * ppm,
                                           width * ppm,
                                           (height - 0.25) * ppm,
                                           0.25 * ppm,
                                           0.25 * ppm));
        g.setColor(paint.darker());
        g.fill(new Rectangle2D.Double(left, ground - 0.55 * ppm, width * ppm, 0.3 * ppm));
        if (vehicle.kind() != com.github.mostafaism1.throttlebrawl.domain.shared.VehicleKind.LORRY) {
            g.setColor(new Color(0x2B3540));
            g.fill(new RoundRectangle2D.Double(left + 0.2 * ppm,
                                               ground - (height - 0.12) * ppm,
                                               (width - 0.4) * ppm,
                                               (height * 0.38) * ppm,
                                               0.15 * ppm,
                                               0.15 * ppm));
        } else {
            g.setColor(paint.darker());
            g.draw(new java.awt.geom.Line2D.Double(cx, ground - height * ppm, cx, ground - 0.55 * ppm));
        }

        g.setColor(TAIL_LIGHT);
        g.fill(new Rectangle2D.Double(left + 0.08 * ppm, ground - 0.85 * ppm, 0.3 * ppm, 0.18 * ppm));
        g.fill(new Rectangle2D.Double(cx + (width / 2 - 0.38) * ppm, ground - 0.85 * ppm, 0.3 * ppm, 0.18 * ppm));
    }

    static void prop(Graphics2D g, PropKind kind, double cx, double ground, double ppm, Palette palette) {
        switch (kind) {
            case PINE -> pine(g, cx, ground, ppm);
            case PALM -> palm(g, cx, ground, ppm);
            case BOULDER -> boulder(g, cx, ground, ppm);
            case BILLBOARD -> billboard(g, cx, ground, ppm, palette);
            case LAMP -> lamp(g, cx, ground, ppm);
            case CACTUS -> cactus(g, cx, ground, ppm);
        }
    }

    private static void pine(Graphics2D g, double cx, double ground, double ppm) {
        g.setColor(new Color(0x5A3B22));
        g.fill(new Rectangle2D.Double(cx - 0.2 * ppm, ground - 1.2 * ppm, 0.4 * ppm, 1.2 * ppm));
        for (var tier = 0; tier < 3; tier++) {
            var base = ground - (1.0 + tier * 1.6) * ppm;
            var half = (1.7 - tier * 0.4) * ppm;

            g.setColor(tier % 2 == 0
                       ? new Color(0x1F5A2C)
                       : new Color(0x26693A));
            g.fill(triangle(cx - half, base, cx + half, base, cx, base - 2.4 * ppm));
        }
    }

    private static void palm(Graphics2D g, double cx, double ground, double ppm) {
        g.setColor(new Color(0x8A6236));
        g.setStroke(new BasicStroke((float) Math.max(2, 0.35 * ppm), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        var trunk = new Path2D.Double();

        trunk.moveTo(cx, ground);
        trunk.quadTo(cx + 0.9 * ppm, ground - 3.5 * ppm, cx + 0.4 * ppm, ground - 7 * ppm);
        g.draw(trunk);
        g.setColor(new Color(0x2E8B3E));
        var topX = cx + 0.4 * ppm;
        var topY = ground - 7 * ppm;

        for (var leaf = 0; leaf < 6; leaf++) {
            var angle = Math.PI * (0.05 + leaf * 0.18);
            var frond = new Path2D.Double();

            frond.moveTo(topX, topY);
            frond.quadTo(topX + Math.cos(angle) * 2.0 * ppm,
                         topY - 1.2 * ppm,
                         topX + Math.cos(angle) * 3.0 * ppm,
                         topY + Math.abs(Math.sin(angle)) * 1.2 * ppm);
            g.setStroke(new BasicStroke((float) Math.max(2, 0.3 * ppm), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(frond);
        }
    }

    private static void boulder(Graphics2D g, double cx, double ground, double ppm) {
        g.setColor(new Color(0x7D7468));
        g.fill(new Ellipse2D.Double(cx - 1.2 * ppm, ground - 1.6 * ppm, 2.4 * ppm, 1.7 * ppm));
        g.setColor(new Color(0x968C7F));
        g.fill(new Ellipse2D.Double(cx - 0.8 * ppm, ground - 1.5 * ppm, 1.2 * ppm, 0.7 * ppm));
    }

    private static void billboard(Graphics2D g, double cx, double ground, double ppm, Palette palette) {
        g.setColor(new Color(0x44403C));
        g.fill(new Rectangle2D.Double(cx - 1.8 * ppm, ground - 2.4 * ppm, 0.2 * ppm, 2.4 * ppm));
        g.fill(new Rectangle2D.Double(cx + 1.6 * ppm, ground - 2.4 * ppm, 0.2 * ppm, 2.4 * ppm));
        g.setColor(palette.night()
                   ? new Color(0xFF2E88)
                   : new Color(0xF7C948));
        g.fill(new Rectangle2D.Double(cx - 2.6 * ppm, ground - 4.6 * ppm, 5.2 * ppm, 2.3 * ppm));
        g.setColor(new Color(0x1A1A1A));
        g.fill(new Rectangle2D.Double(cx - 2.4 * ppm, ground - 4.4 * ppm, 4.8 * ppm, 1.9 * ppm));
        if (ppm > 6) {
            g.setColor(palette.night()
                       ? new Color(0x2EE6FF)
                       : new Color(0xF7C948));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(6, 0.8 * ppm)));
            var text = palette.night()
                       ? "VOLT COLA"
                       : "GRIT OIL";
            var width = g.getFontMetrics().stringWidth(text);

            g.drawString(text, (float)(cx - width / 2.0), (float)(ground - 3.15 * ppm));
        }
    }

    private static void lamp(Graphics2D g, double cx, double ground, double ppm) {
        g.setColor(new Color(0x3A3C46));
        g.fill(new Rectangle2D.Double(cx - 0.1 * ppm, ground - 6.2 * ppm, 0.2 * ppm, 6.2 * ppm));
        var armDirection = cx < 0
                           ? 1
                           : -1;

        g.fill(new Rectangle2D.Double(Math.min(cx, cx - armDirection * 1.4 * ppm),
                                      ground - 6.2 * ppm,
                                      1.4 * ppm,
                                      0.15 * ppm));
        g.setColor(new Color(255, 214, 120, 70));
        var glow = 2.4 * ppm;

        g.fill(new Ellipse2D.Double(cx - glow / 2, ground - 6.1 * ppm - glow / 2, glow, glow));
        g.setColor(new Color(0xFFE39A));
        g.fill(new Ellipse2D.Double(cx - 0.3 * ppm, ground - 6.25 * ppm, 0.6 * ppm, 0.3 * ppm));
    }

    private static void cactus(Graphics2D g, double cx, double ground, double ppm) {
        g.setColor(new Color(0x3E7F45));
        g.fill(new RoundRectangle2D.Double(cx - 0.3 * ppm,
                                           ground - 3.6 * ppm,
                                           0.6 * ppm,
                                           3.6 * ppm,
                                           0.6 * ppm,
                                           0.6 * ppm));
        g.fill(new RoundRectangle2D.Double(cx - 1.0 * ppm,
                                           ground - 2.8 * ppm,
                                           0.4 * ppm,
                                           1.4 * ppm,
                                           0.4 * ppm,
                                           0.4 * ppm));
        g.fill(new Rectangle2D.Double(cx - 1.0 * ppm, ground - 1.6 * ppm, 0.8 * ppm, 0.3 * ppm));
        g.fill(new RoundRectangle2D.Double(cx + 0.6 * ppm,
                                           ground - 3.1 * ppm,
                                           0.4 * ppm,
                                           1.5 * ppm,
                                           0.4 * ppm,
                                           0.4 * ppm));
        g.fill(new Rectangle2D.Double(cx + 0.2 * ppm, ground - 1.9 * ppm, 0.8 * ppm, 0.3 * ppm));
    }

    private static void label(Graphics2D g, String text, double cx, double y) {
        g.setFont(NAME_FONT);
        var width = g.getFontMetrics().stringWidth(text);

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect((int)(cx - width / 2.0 - 4), (int)(y - 13), width + 8, 17, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(text, (float)(cx - width / 2.0), (float) y);
    }

    private static Path2D triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        var path = new Path2D.Double();

        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.closePath();

        return path;
    }
}

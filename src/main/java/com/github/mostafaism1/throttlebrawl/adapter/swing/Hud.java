package com.github.mostafaism1.throttlebrawl.adapter.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Contract;

import com.github.mostafaism1.throttlebrawl.domain.shared.Headline;
import com.github.mostafaism1.throttlebrawl.domain.shared.RacePhase.Countdown;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.RiderState.Finished;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.domain.shared.TrackCatalog;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.usecase.settlerace.SettleRace;

import static java.util.Locale.ROOT;


/**
 * Heads-up display and the full-screen menus.
 */
@Contract
final class Hud {
    private static final Font HUGE = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 96);
    private static final Font BIG = new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, 40);
    private static final Font MEDIUM = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Color PANEL = new Color(0, 0, 0, 140);
    private static final Color ACCENT = new Color(0xFFD23F);
    private static final Color HEALTH = new Color(0xE0262F);
    private static final Color BIKE = new Color(0x2EA8FF);

    private Hud() {}

    static void race(Graphics2D g,
                     Season season,
                     RaceState race,
                     boolean paused,
                     double overFor,
                     int width,
                     int height) {
        var player = race.player();

        standing(g, race, player);
        progress(g, race, width);
        trackCard(g, season, race, width);
        gauges(g, player, height);
        speedometer(g, player, width, height);
        headlines(g, race, width);
        countdown(g, race, width, height);
        if (race.clock() < 6) {
            hint(g, width, height);
        }

        if (race.isOver()) {
            verdict(g, race, overFor, width, height);
        }

        if (paused) {
            banner(g, "PAUSED", "P to resume · Esc to quit", width, height);
        }
    }

    private static void standing(Graphics2D g, RaceState race, Rider player) {
        g.setColor(PANEL);
        g.fillRoundRect(16, 16, 190, 92, 14, 14);
        g.setColor(ACCENT);
        g.setFont(BIG);
        g.drawString("P" + race.placeOf(player.id()),
                     30,
                     64);
        g.setFont(MEDIUM);
        g.setColor(Color.WHITE);
        g.drawString("/ " + race.riders().size(),
                     100,
                     64);
        g.setFont(SMALL);
        g.drawString(clock(race.clock()), 30, 94);
    }

    private static void progress(Graphics2D g, RaceState race, int width) {
        var left = 230;
        var right = width - 300;
        var y = 34;

        g.setColor(PANEL);
        g.fillRoundRect(left - 10, y - 16, right - left + 20, 32, 14, 14);
        g.setColor(new Color(255, 255, 255, 120));
        g.fillRect(left, y - 1, right - left, 3);
        g.setColor(Color.WHITE);
        g.fillRect(right - 2, y - 10, 4, 20);
        for (var rider : race.riders().reversed()) {
            var fraction = Math.clamp(rider.z() / race.track().length(),
                                      0,
                                      1);
            var x = (int)(left + fraction * (right - left));
            var size = rider.isPlayer()
                       ? 14
                       : 9;

            g.setColor(Palette.jacket(rider.persona().livery()));
            g.fillOval(x - size / 2, y - size / 2, size, size);
            g.setColor(rider.isPlayer()
                       ? Color.WHITE
                       : Color.BLACK);
            g.drawOval(x - size / 2, y - size / 2, size, size);
        }
    }

    private static void trackCard(Graphics2D g, Season season, RaceState race, int width) {
        g.setColor(PANEL);
        g.fillRoundRect(width - 276, 16, 260, 92, 14, 14);
        g.setColor(Color.WHITE);
        g.setFont(SMALL);
        g.drawString("ROUND " + (season.round() + 1) + " · " + race.track().title(),
                     width - 262,
                     42);
        g.setColor(ACCENT);
        g.setFont(MEDIUM);
        g.drawString(money(season.cash()), width - 262, 74);
        g.setColor(Color.WHITE);
        g.setFont(SMALL);
        var remaining = Math.max(0,
                                 race.track().length() - race.player().z());

        g.drawString(String.format(ROOT, "%.1f km to go", remaining / 1000), width - 262, 98);
    }

    private static void gauges(Graphics2D g, Rider player, int height) {
        g.setColor(PANEL);
        g.fillRoundRect(16, height - 96, 260, 80, 14, 14);
        bar(g, "RIDER", player.health() / Tuning.HEALTH_MAX, HEALTH, 30, height - 70);
        bar(g, "BIKE", player.bikeCondition() / Tuning.BIKE_MAX, BIKE, 30, height - 36);
    }

    private static void bar(Graphics2D g, String label, double fraction, Color color, int x, int y) {
        g.setFont(SMALL);
        g.setColor(Color.WHITE);
        g.drawString(label, x, y + 13);
        g.setColor(new Color(255, 255, 255, 50));
        g.fillRoundRect(x + 62, y, 168, 16, 8, 8);
        g.setColor(color);
        g.fillRoundRect(x + 62, y, (int)(168 * Math.clamp(fraction, 0, 1)), 16, 8, 8);
    }

    private static void speedometer(Graphics2D g, Rider player, int width, int height) {
        g.setColor(PANEL);
        g.fillRoundRect(width - 216, height - 96, 200, 80, 14, 14);
        var kmh = (int) Math.round(player.speed() * 3.6);

        g.setFont(BIG);
        g.setColor(Color.WHITE);
        var text = String.valueOf(kmh);

        g.drawString(text,
                     width - 112 - g.getFontMetrics().stringWidth(text),
                     height - 42);
        g.setFont(SMALL);
        g.setColor(ACCENT);
        g.drawString("km/h", width - 100, height - 42);
        g.setColor(ACCENT);
        g.fillRect(width - 200,
                   height - 32,
                   (int)(168 * Math.min(1, player.speed() / Tuning.MAX_SPEED)),
                   6);
    }

    private static void headlines(Graphics2D g, RaceState race, int width) {
        g.setFont(MEDIUM);
        var y = 150;

        for (var headline : race.headlines().reversed().stream().limit(3).toList()) {
            centered(g, headline.text(), width, y, alpha(Color.WHITE, headline));
            y += 32;
        }
    }

    private static Color alpha(Color color, Headline headline) {
        var opacity = (int)(255 * Math.clamp(headline.ttl() / 0.6, 0, 1));

        return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }

    private static void countdown(Graphics2D g, RaceState race, int width, int height) {
        if (race.phase() instanceof Countdown countdown) {
            g.setFont(HUGE);
            centered(g,
                     String.valueOf((int) Math.ceil(countdown.remaining())),
                     width,
                     height / 2 - 40,
                     ACCENT);
            g.setFont(MEDIUM);
            centered(g,
                     race.track().title().toUpperCase(ROOT),
                     width,
                     height / 2 + 10,
                     Color.WHITE);
            g.setFont(SMALL);
            centered(g,
                     race.track().tagline(),
                     width,
                     height / 2 + 36,
                     Color.WHITE);
        } else if (race.clock() < 1.0) {
            g.setFont(HUGE);
            centered(g, "GO!", width, height / 2 - 40, ACCENT);
        }
    }

    private static void hint(Graphics2D g, int width, int height) {
        g.setFont(SMALL);
        centered(g,
                 "↑ throttle · ↓ brake · ← → steer · A/D punch left/right · Q/E kick left/right · P pause",
                 width,
                 height - 120,
                 Color.WHITE);
    }

    private static void verdict(Graphics2D g, RaceState race, double overFor, int width, int height) {
        var player = race.player();
        var title = player.isWrecked()
                    ? "WRECKED"
                    : "FINISHED P" + race.placeOf(player.id());
        var footer = overFor > 1.5
                     ? "Press Enter for the results"
                     : " ";

        banner(g, title, footer, width, height);
    }

    static void title(Graphics2D g, Season season, int width, int height) {
        backdrop(g, width, height);
        g.setFont(HUGE);
        centered(g, "THROTTLE BRAWL", width, height / 3, ACCENT);
        g.setFont(MEDIUM);
        centered(g, "Highway racing with bare knuckles", width, height / 3 + 44, Color.WHITE);
        g.setFont(SMALL);
        var y = height / 2 + 20;

        for (var line : new String[]{"Rider: " + season.rider().value() + "   ·   Bank: " + money(season.cash()), " ", "↑ throttle   ↓ brake   ← → steer", "A / D  punch left / right      Q / E  kick left / right", "Win prize money, pay for repairs, keep your bike alive.", " ", "Season circuit: " + circuit()}) {
            centered(g, line, width, y, Color.WHITE);
            y += 24;
        }

        g.setFont(MEDIUM);
        centered(g, "Press Enter to race", width, height - 60, ACCENT);
    }

    static void results(Graphics2D g, SettleRace.Report report, int width, int height) {
        backdrop(g, width, height);
        g.setFont(BIG);
        centered(g,
                 report.wrecked()
                 ? "RETIRED - BIKE WRECKED"
                 : "RACE RESULT: P" + report.place(),
                 width,
                 80,
                 ACCENT);
        g.setFont(SMALL);
        var y = 130;
        var position = 1;

        for (var rider : report.standings()) {
            var line = String.format(ROOT, "%d.  %-16s %s", position++, rider.name(), status(rider));

            g.setColor(rider.isPlayer()
                       ? ACCENT
                       : Color.WHITE);
            g.drawString(line, width / 2 - 200, y);
            y += 24;
        }

        y += 16;
        g.setFont(MEDIUM);
        for (var line : new String[]{"Prize money        " + money(report.prize()), "Towing             -" + money(report.towing()), "Workshop repairs   -" + money(report.repairCost()), "Bank               " + money(report.next()
                                                                                                                                                                                                                              .cash()), String.format(ROOT,
                                                                                                                                                                                                                                                      "Bike condition     %.0f%%",
                                                                                                                                                                                                                                                      report.next()
                                                                                                                                                                                                                                                            .bikeCondition())}) {
            g.setColor(Color.WHITE);
            g.drawString(line, width / 2 - 200, y);
            y += 30;
        }

        g.setColor(ACCENT);
        centered(g, "Press Enter for the next round", width, height - 50, ACCENT);
    }

    static void retired(Graphics2D g, Season season, Cause reason, int width, int height) {
        backdrop(g, width, height);
        g.setFont(BIG);
        centered(g, "CAREER OVER", width, height / 3, HEALTH);
        g.setFont(MEDIUM);
        centered(g, reason.message(), width, height / 3 + 50, Color.WHITE);
        centered(g,
                 "You lasted " + season.round() + " rounds and left with " + money(season.cash()),
                 width,
                 height / 3 + 90,
                 Color.WHITE);
        centered(g, "Press Enter to start a new season", width, height - 60, ACCENT);
    }

    private static String status(Rider rider) {
        return switch (rider.state()) {
            case Finished finished -> clock(finished.time());
            default -> rider.isWrecked()
                       ? "DNF (wrecked)"
                       : "still riding";
        };
    }

    private static String circuit() {
        return String.join(" → ",
                           TrackCatalog.TRACKS.stream().map(track -> track.title()).toList());
    }

    private static void backdrop(Graphics2D g, int width, int height) {
        g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x1A0B2E), 0, height, new Color(0x5A1A2E)));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(255, 255, 255, 18));
        for (var stripe = -height; stripe < width; stripe += 40) {
            g.fillPolygon(new int[]{stripe, stripe + 20, stripe + 20 + height, stripe + height},
                          new int[]{0, 0, height, height},
                          4);
        }
    }

    private static void banner(Graphics2D g, String title, String footer, int width, int height) {
        g.setColor(PANEL);
        g.fillRect(0, height / 2 - 80, width, 140);
        g.setFont(BIG);
        centered(g, title, width, height / 2 - 20, ACCENT);
        g.setFont(MEDIUM);
        centered(g, footer, width, height / 2 + 30, Color.WHITE);
    }

    private static void centered(Graphics2D g, String text, int width, int y, Color color) {
        var x = (width - g.getFontMetrics().stringWidth(text)) / 2;

        g.setColor(new Color(0,
                             0,
                             0,
                             Math.min(160, color.getAlpha())));
        g.drawString(text, x + 2, y + 2);
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static String clock(double seconds) {
        var minutes = (int)(seconds / 60);

        return String.format(ROOT, "%d:%04.1f", minutes, seconds - minutes * 60);
    }

    private static String money(long amount) {
        return String.format(ROOT, "$%,d", amount);
    }
}

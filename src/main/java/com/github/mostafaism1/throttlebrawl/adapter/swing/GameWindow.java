package com.github.mostafaism1.throttlebrawl.adapter.swing;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Contract;
import org.pragmatica.lang.Option;

import com.github.mostafaism1.throttlebrawl.adapter.swing.GameWindow.Screen.Racing;
import com.github.mostafaism1.throttlebrawl.adapter.swing.GameWindow.Screen.Results;
import com.github.mostafaism1.throttlebrawl.adapter.swing.GameWindow.Screen.Retired;
import com.github.mostafaism1.throttlebrawl.adapter.swing.GameWindow.Screen.Title;
import com.github.mostafaism1.throttlebrawl.domain.shared.Attack;
import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Controls;
import com.github.mostafaism1.throttlebrawl.domain.shared.Move;
import com.github.mostafaism1.throttlebrawl.domain.shared.Season;
import com.github.mostafaism1.throttlebrawl.domain.shared.Side;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.usecase.advancerace.AdvanceRace;
import com.github.mostafaism1.throttlebrawl.usecase.settlerace.SettleRace;
import com.github.mostafaism1.throttlebrawl.usecase.startrace.StartRace;

import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_E;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_P;
import static java.awt.event.KeyEvent.VK_Q;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;


/**
 * The game shell: owns the window, the keyboard and the fixed-step loop, and moves between
 * screens by calling the use cases. All state here is confined to the Swing event thread.
 */
@Contract
public final class GameWindow extends JPanel implements KeyListener {
    /**
     * What the window is showing.
     */
    sealed interface Screen {
        record Title(Season season) implements Screen {}

        /// @param overFor seconds since the race ended
        record Racing(Season season, RaceState race, boolean paused, double overFor) implements Screen {}

        record Results(SettleRace.Report report) implements Screen {}

        record Retired(Season season, Cause reason) implements Screen {}
    }

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 640;
    private static final int RIVALS = 7;
    private static final double MAX_FRAME = 0.25;

    private final StartRace startRace;
    private final AdvanceRace advanceRace;
    private final SettleRace settleRace;
    private final RoadRenderer renderer = new RoadRenderer();
    private final Set<Integer> held = new HashSet<>();
    private Screen screen;
    private Option<Attack> queuedAttack = Option.none();
    private boolean enterPressed;
    private boolean pausePressed;
    private long lastNanos = System.nanoTime();
    private double accumulator;

    private GameWindow(Season season, StartRace startRace, AdvanceRace advanceRace, SettleRace settleRace) {
        this.startRace = startRace;
        this.advanceRace = advanceRace;
        this.settleRace = settleRace;
        this.screen = new Title(season);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
    }

    public static void launch(Season season) {
        SwingUtilities.invokeLater(() -> open(season));
    }

    private static void open(Season season) {
        var game = new GameWindow(season, StartRace.startRace(), AdvanceRace.advanceRace(), SettleRace.settleRace());
        var frame = new JFrame("Throttle Brawl");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(game);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.requestFocusInWindow();
        new Timer(15, _ -> game.frame()).start();
    }

    private void frame() {
        var now = System.nanoTime();

        accumulator = Math.min(MAX_FRAME, accumulator + (now - lastNanos) / 1e9);
        lastNanos = now;
        while (accumulator >= Tuning.TICK) {
            screen = step(screen);
            accumulator -= Tuning.TICK;
        }

        repaint();
    }

    private Screen step(Screen current) {
        var enter = consumeEnter();

        return switch (current) {
            case Title title -> enter
                                ? begin(title.season())
                                : title;
            case Racing racing -> race(racing, enter);
            case Results results -> enter
                                    ? begin(results.report().next())
                                    : results;
            case Retired retired -> enter
                                    ? new Title(freshSeason(retired.season()))
                                    : retired;
        };
    }

    private Screen begin(Season season) {
        return startRace.execute(StartRace.Request.request(season, RIVALS))
                        .fold(cause -> new Retired(season, cause),
                              race -> new Racing(season, race, false, 0));
    }

    private static Season freshSeason(Season previous) {
        return Season.season(previous.rider().value(),
                             previous.seed() + 1)
                     .or(previous);
    }

    private Screen race(Racing racing, boolean enter) {
        var paused = racing.paused() ^ consumePause();

        if (paused) {
            queuedAttack = Option.none();

            return new Racing(racing.season(), racing.race(), true, racing.overFor());
        }

        if (racing.race().isOver()) {
            return enter && racing.overFor() > 1.5
                   ? new Results(settleRace.execute(SettleRace.Request.request(racing.season(), racing.race())))
                   : new Racing(racing.season(), racing.race(), false, racing.overFor() + Tuning.TICK);
        }

        var next = advanceRace.advance(racing.race(), controls());

        return new Racing(racing.season(), next, false, 0);
    }

    private Controls controls() {
        var steer = (isHeld(VK_RIGHT)
                     ? 1
                     : 0) - (isHeld(VK_LEFT)
                             ? 1
                             : 0);
        var attack = queuedAttack;

        queuedAttack = Option.none();

        return Controls.controls(steer, isHeld(VK_UP), isHeld(VK_DOWN), attack);
    }

    private boolean isHeld(int key) {
        return held.contains(key);
    }

    private boolean consumeEnter() {
        var pressed = enterPressed;

        enterPressed = false;

        return pressed;
    }

    private boolean consumePause() {
        var pressed = pausePressed;

        pausePressed = false;

        return pressed;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        var g = (Graphics2D) graphics.create();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        var width = getWidth();
        var height = getHeight();

        switch (screen) {
            case Title title -> Hud.title(g, title.season(), width, height);
            case Racing racing -> paintRace(g, racing, width, height);
            case Results results -> Hud.results(g, results.report(), width, height);
            case Retired retired -> Hud.retired(g, retired.season(), retired.reason(), width, height);
        }

        g.dispose();
    }

    private void paintRace(Graphics2D g, Racing racing, int width, int height) {
        renderer.render(g, racing.race(), width, height);
        Hud.race(g, racing.season(), racing.race(), racing.paused(), racing.overFor(), width, height);
    }

    @Override
    public void keyPressed(KeyEvent event) {
        held.add(event.getKeyCode());
        switch (event.getKeyCode()) {
            case VK_ENTER, VK_SPACE -> enterPressed = true;
            case VK_P -> pausePressed = true;
            case VK_ESCAPE -> System.exit(0);
            case VK_A -> queuedAttack = Option.some(Attack.attack(Move.PUNCH, Side.LEFT));
            case VK_D -> queuedAttack = Option.some(Attack.attack(Move.PUNCH, Side.RIGHT));
            case VK_Q -> queuedAttack = Option.some(Attack.attack(Move.KICK, Side.LEFT));
            case VK_E -> queuedAttack = Option.some(Attack.attack(Move.KICK, Side.RIGHT));
            default -> {}
        }
    }

    @Override
    public void keyReleased(KeyEvent event) {
        held.remove(event.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent event) {}
}

package com.github.mostafaism1.throttlebrawl.adapter.swing;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pragmatica.lang.Contract;

import com.github.mostafaism1.throttlebrawl.domain.shared.RaceState;
import com.github.mostafaism1.throttlebrawl.domain.shared.Rider;
import com.github.mostafaism1.throttlebrawl.domain.shared.Strip;
import com.github.mostafaism1.throttlebrawl.domain.shared.Tuning;
import com.github.mostafaism1.throttlebrawl.domain.shared.Vehicle;


/// Pseudo-3D road: projects strips ahead of the camera, then paints them far to near together
/// with the sprites standing on them, so hill crests hide whatever lies behind them.
@Contract
final class RoadRenderer {
    private static final double ROAD_HALF_WIDTH = 5.0;
    private static final double CAMERA_HEIGHT = 3.0;
    private static final double CAMERA_BACK = 6.0;
    private static final double DEPTH = 0.84;
    private static final int DRAW_STRIPS = 160;
    private static final double CURVE_SCALE = 0.025;
    private static final double PARALLAX = 0.0035;
    private static final int RUMBLE_PERIOD = 3;
    private static final double NEAR_PLANE = 0.5;

    private record Projected(double x, double y, double scale) {}

    /// A visible strip: its world-space edges relative to the camera, and their projections.
    private record Edge(double x, double y, double z) {}

    private record Slice(int index,
                         int distance,
                         Strip strip,
                         double z,
                         Edge nearEdge,
                         Edge farEdge,
                         Projected near,
                         Projected far) {}

    private record Sprite(double z, double x, Painter painter) {}

    @FunctionalInterface
    private interface Painter {
        void paint(Graphics2D g, double cx, double ground, double ppm);
    }

    private double parallax;
    private double lastZ;

    void render(Graphics2D g, RaceState state, int width, int height) {
        var palette = Palette.palette(state.track().scenery());
        var player = state.player();

        trackParallax(state, player);
        sky(g, palette, width, height);
        backdrop(g, palette, width, height);
        var slices = project(state, player, width, height);
        var sprites = spritesByStrip(state, palette);

        for (var slice : slices.reversed()) {
            paintSlice(g, palette, slice, width);
            paintSprites(g,
                         slice,
                         sprites.getOrDefault(slice.index(), List.of()),
                         width,
                         height);
        }
    }

    private void trackParallax(RaceState state, Rider player) {
        var travelled = player.z() - lastZ;

        if (travelled < 0 || travelled > 50) {
            travelled = 0;
        }

        parallax += state.track().stripAt(player.z()).curve() * travelled * PARALLAX;
        lastZ = player.z();
    }

    private static List<Slice> project(RaceState state, Rider player, int width, int height) {
        var track = state.track();
        var cameraZ = player.z() - CAMERA_BACK;
        var cameraY = track.elevationAt(player.z()) + CAMERA_HEIGHT;
        var cameraX = player.x() * ROAD_HALF_WIDTH;
        var base = track.stripIndexAt(cameraZ);
        var fraction = cameraZ / Tuning.STRIP_LENGTH - base;
        var curveX = 0.0;
        var curveStep = -track.strip(base).curve() * CURVE_SCALE * fraction;
        var slices = new ArrayList<Slice>();

        for (var distance = 0; distance < DRAW_STRIPS; distance++) {
            var index = base + distance;
            var strip = track.strip(index);
            var startZ = index * Tuning.STRIP_LENGTH;
            var nearZ = startZ - cameraZ;
            var farZ = nearZ + Tuning.STRIP_LENGTH;
            var farEdge = new Edge(curveX + curveStep - cameraX,
                                   track.strip(index + 1).elevation() - cameraY,
                                   farZ);
            var nearEdge = clip(new Edge(curveX - cameraX, strip.elevation() - cameraY, nearZ), farEdge);
            var near = project(nearEdge.x(), nearEdge.y(), nearEdge.z(), width, height);
            var far = project(farEdge.x(), farEdge.y(), farEdge.z(), width, height);

            curveX += curveStep;
            curveStep += strip.curve() * CURVE_SCALE;
            if (farZ > NEAR_PLANE && far.y() < near.y()) {
                slices.add(new Slice(index, distance, strip, startZ, nearEdge, farEdge, near, far));
            }
        }

        return slices;
    }

    /// Cuts a strip that straddles the camera at the near plane, keeping the part in view.
    private static Edge clip(Edge near, Edge far) {
        if (near.z() >= NEAR_PLANE) {
            return near;
        }

        var t = (NEAR_PLANE - near.z()) / (far.z() - near.z());

        return new Edge(lerp(near.x(), far.x(), t),
                        lerp(near.y(), far.y(), t),
                        NEAR_PLANE);
    }

    private static Projected project(double x, double y, double z, int width, int height) {
        var scale = DEPTH / Math.max(0.01, z);
        var pixels = width / 2.0;

        return new Projected(width / 2.0 + scale * x * pixels, height / 2.0 - scale * y * pixels, scale * pixels);
    }

    private static void paintSlice(Graphics2D g, Palette palette, Slice slice, int width) {
        var fog = Math.pow((double) slice.distance() / DRAW_STRIPS, 2);
        var light = (slice.index() / RUMBLE_PERIOD) % 2 == 0;
        var near = slice.near();
        var far = slice.far();

        g.setColor(Palette.fade(light
                                ? palette.grassLight()
                                : palette.grassDark(),
                                palette.fog(),
                                fog));
        g.fillRect(0,
                   (int) far.y(),
                   width,
                   (int) Math.ceil(near.y() - far.y()) + 1);
        var nearHalf = near.scale() * ROAD_HALF_WIDTH;
        var farHalf = far.scale() * ROAD_HALF_WIDTH;

        g.setColor(Palette.fade(light
                                ? palette.rumbleLight()
                                : palette.rumbleDark(),
                                palette.fog(),
                                fog));
        g.fill(quad(near.x(), near.y(), nearHalf * 1.12, far.x(), far.y(), farHalf * 1.12));
        g.setColor(Palette.fade(light
                                ? palette.roadLight()
                                : palette.roadDark(),
                                palette.fog(),
                                fog));
        g.fill(quad(near.x(), near.y(), nearHalf, far.x(), far.y(), farHalf));
        if (light) {
            g.setColor(Palette.fade(palette.lane(), palette.fog(), fog));
            for (var lane : new double[]{ - 1.0 / 3, 1.0 / 3}) {
                g.fill(quad(near.x() + nearHalf * lane,
                            near.y(),
                            nearHalf * 0.02,
                            far.x() + farHalf * lane,
                            far.y(),
                            farHalf * 0.02));
            }
        }
    }

    private static Polygon quad(double x1, double y1, double half1, double x2, double y2, double half2) {
        return new Polygon(new int[]{(int)(x1 - half1), (int)(x1 + half1), (int)(x2 + half2), (int)(x2 - half2)},
                           new int[]{(int) Math.ceil(y1), (int) Math.ceil(y1), (int) y2, (int) y2},
                           4);
    }

    private static Map<Integer, List<Sprite>> spritesByStrip(RaceState state, Palette palette) {
        var track = state.track();
        var sprites = new HashMap<Integer, List<Sprite>>();
        var player = state.player();
        var from = track.stripIndexAt(player.z() - CAMERA_BACK);

        for (var index = from; index < from + DRAW_STRIPS; index++) {
            var strip = track.strip(index);
            var startZ = index * Tuning.STRIP_LENGTH;

            strip.prop()
                 .onPresent(prop -> add(sprites,
                                        track.stripIndexAt(startZ),
                                        new Sprite(startZ,
                                                   prop.x(),
                                                   (g, cx, ground, ppm) -> SpriteArtist.prop(g,
                                                                                             prop.kind(),
                                                                                             cx,
                                                                                             ground,
                                                                                             ppm,
                                                                                             palette))));
        }

        for (var vehicle : state.traffic()) {
            add(sprites,
                track.stripIndexAt(vehicle.z()),
                vehicleSprite(vehicle));
        }

        for (var rider : state.riders()) {
            if (!rider.isWrecked()) {
                add(sprites,
                    track.stripIndexAt(rider.z()),
                    riderSprite(rider, player));
            }
        }

        return sprites;
    }

    private static Sprite vehicleSprite(Vehicle vehicle) {
        return new Sprite(vehicle.z() - vehicle.kind().length() / 2,
                          vehicle.x(),
                          (g, cx, ground, ppm) -> SpriteArtist.vehicle(g, vehicle, cx, ground, ppm));
    }

    private static Sprite riderSprite(Rider rider, Rider player) {
        var showName = !rider.isPlayer() && Math.abs(rider.z() - player.z()) < 40;

        return new Sprite(rider.z(),
                          rider.x(),
                          (g, cx, ground, ppm) -> SpriteArtist.rider(g, rider, cx, ground, ppm, showName));
    }

    private static void add(Map<Integer, List<Sprite>> sprites, int index, Sprite sprite) {
        sprites.computeIfAbsent(index, _ -> new ArrayList<>()).add(sprite);
    }

    private static void paintSprites(Graphics2D g, Slice slice, List<Sprite> sprites, int width, int height) {
        var ordered = sprites.stream().sorted(Comparator.comparingDouble(Sprite::z).reversed()).toList();

        for (var sprite : ordered) {
            paintSprite(g, slice, sprite, width, height);
        }
    }

    /// Sprites are projected from world space: interpolating in screen space would bend them
    /// toward the camera, because perspective is not linear in depth.
    private static void paintSprite(Graphics2D g, Slice slice, Sprite sprite, int width, int height) {
        var near = slice.nearEdge();
        var far = slice.farEdge();
        var cameraZ = slice.z() + Tuning.STRIP_LENGTH - far.z();
        var z = sprite.z() - cameraZ;

        if (z < NEAR_PLANE * 2) {
            return;
        }

        var t = Math.clamp((z - near.z()) / (far.z() - near.z()),
                           0,
                           1);
        var spot = project(lerp(near.x(), far.x(), t) + sprite.x() * ROAD_HALF_WIDTH,
                           lerp(near.y(), far.y(), t),
                           z,
                           width,
                           height);

        if (spot.x() > - width && spot.x() < width * 2) {
            sprite.painter().paint(g, spot.x(), spot.y(), spot.scale());
        }
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    private void sky(Graphics2D g, Palette palette, int width, int height) {
        g.setPaint(new GradientPaint(0, 0, palette.skyTop(), 0, height / 2f, palette.skyBottom()));
        g.fillRect(0, 0, width, height);
        if (palette.night()) {
            g.setColor(new Color(255, 255, 255, 180));
            for (var star = 0; star < 90; star++) {
                var x = Math.floorMod((long)(star * 7919 + parallaxShift(width, 0.2)), width);
                var y = (star * 104729L) % (height / 2);

                g.fillRect((int) x, (int) y, 2, 2);
            }
        }
    }

    private void backdrop(Graphics2D g, Palette palette, int width, int height) {
        var horizon = height / 2.0;

        ridge(g, palette.horizon(), width, horizon, 70, 0.5, parallax * 0.5 * width, palette.night());
        ridge(g,
              palette.horizon().darker(),
              width,
              horizon,
              40,
              1.3,
              parallax * width,
              palette.night());
    }

    private static void ridge(Graphics2D g,
                              Color color,
                              int width,
                              double horizon,
                              double amplitude,
                              double frequency,
                              double offset,
                              boolean skyline) {
        var path = new Path2D.Double();

        path.moveTo(0, horizon + 2);
        for (var x = 0; x <= width; x += 8) {
            var phase = (x + offset) / width * Math.PI * 2 * frequency;
            var rise = skyline
                       ? Math.floor(Math.abs(Math.sin(phase * 3.1) + Math.sin(phase * 7.3)) * 2.5) * amplitude / 5
                       : (Math.sin(phase) * 0.6 + Math.sin(phase * 2.7) * 0.4 + 1) * amplitude / 2;

            path.lineTo(x, horizon - rise);
        }

        path.lineTo(width, horizon + 2);
        path.closePath();
        g.setColor(color);
        g.fill(path);
    }

    private double parallaxShift(int width, double depth) {
        return parallax * width * depth;
    }
}

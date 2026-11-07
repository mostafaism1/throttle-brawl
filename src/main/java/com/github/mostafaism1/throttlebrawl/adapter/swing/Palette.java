package com.github.mostafaism1.throttlebrawl.adapter.swing;

import java.awt.Color;

import com.github.mostafaism1.throttlebrawl.domain.shared.Livery;
import com.github.mostafaism1.throttlebrawl.domain.shared.Scenery;


/// Colors for each biome and each team livery.
record Palette(Color skyTop,
               Color skyBottom,
               Color horizon,
               Color grassLight,
               Color grassDark,
               Color rumbleLight,
               Color rumbleDark,
               Color roadLight,
               Color roadDark,
               Color lane,
               Color fog,
               boolean night) {
    static Palette palette(Scenery scenery) {
        return switch (scenery) {
            case COAST -> new Palette(rgb(0x2E8BD8),
                                      rgb(0xBDE6FF),
                                      rgb(0x4F7FA8),
                                      rgb(0xE8D9A8),
                                      rgb(0xDCCB92),
                                      rgb(0xF4F4F4),
                                      rgb(0xD33A2C),
                                      rgb(0x6B6E73),
                                      rgb(0x64676C),
                                      rgb(0xF2F2F2),
                                      rgb(0xCFE9F7),
                                      false);
            case DESERT -> new Palette(rgb(0xF08A3C),
                                       rgb(0xFFD9A0),
                                       rgb(0xA4553A),
                                       rgb(0xD9A066),
                                       rgb(0xCC9359),
                                       rgb(0xFFFFFF),
                                       rgb(0x3A3A3A),
                                       rgb(0x7A6A5C),
                                       rgb(0x736356),
                                       rgb(0xFFE7A8),
                                       rgb(0xF6D2A2),
                                       false);
            case ALPINE -> new Palette(rgb(0x5B86C4),
                                       rgb(0xDCE8F2),
                                       rgb(0x5E6F80),
                                       rgb(0x3F7D3A),
                                       rgb(0x376F33),
                                       rgb(0xFFFFFF),
                                       rgb(0x2A4FA0),
                                       rgb(0x5A5D61),
                                       rgb(0x55585C),
                                       rgb(0xFFFFFF),
                                       rgb(0xD5DFE6),
                                       false);
            case NIGHT_CITY -> new Palette(rgb(0x05030F),
                                           rgb(0x2A1450),
                                           rgb(0x120A26),
                                           rgb(0x1B1E2B),
                                           rgb(0x171A25),
                                           rgb(0xFF2E88),
                                           rgb(0x2EE6FF),
                                           rgb(0x2E2F38),
                                           rgb(0x2A2B33),
                                           rgb(0xFFD23F),
                                           rgb(0x1A1030),
                                           true);
        };
    }

    static Color jacket(Livery livery) {
        return switch (livery) {
            case CRIMSON -> rgb(0xE0262F);
            case COBALT -> rgb(0x2457D6);
            case VIOLET -> rgb(0x8E3BD6);
            case AMBER -> rgb(0xF2A516);
            case JADE -> rgb(0x1FA35B);
            case IVORY -> rgb(0xEDE6D3);
            case ONYX -> rgb(0x2B2B2E);
            case TEAL -> rgb(0x14A3A3);
        };
    }

    static Color fade(Color color, Color fog, double amount) {
        var keep = 1 - Math.clamp(amount, 0, 1);

        return new Color((int)(color.getRed() * keep + fog.getRed() * (1 - keep)),
                         (int)(color.getGreen() * keep + fog.getGreen() * (1 - keep)),
                         (int)(color.getBlue() * keep + fog.getBlue() * (1 - keep)));
    }

    static Color rgb(int value) {
        return new Color(value);
    }
}

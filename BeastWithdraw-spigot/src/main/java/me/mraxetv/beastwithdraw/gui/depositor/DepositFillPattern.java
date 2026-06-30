package me.mraxetv.beastwithdraw.gui.depositor;

import me.mraxetv.beastlib.lib.tgui.gui.components.util.GuiFiller;
import me.mraxetv.beastlib.lib.tgui.gui.guis.BaseGui;
import me.mraxetv.beastlib.lib.tgui.gui.guis.GuiItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class DepositFillPattern {
    enum Type {
        ALL,
        BORDER,
        TOP,
        BOTTOM,
        TOP_BOTTOM,
        LEFT,
        RIGHT,
        SIDES,
        RECTANGLE,
        CHECKERBOARD;

        static Type fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                return ALL;
            }

            try {
                return valueOf(value.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_'));
            } catch (IllegalArgumentException ignored) {
                return ALL;
            }
        }
    }

    private final Type type;
    private final int fromRow;
    private final int fromColumn;
    private final int toRow;
    private final int toColumn;

    private DepositFillPattern(Type type, int fromRow, int fromColumn, int toRow, int toColumn) {
        this.type = type;
        this.fromRow = fromRow;
        this.fromColumn = fromColumn;
        this.toRow = toRow;
        this.toColumn = toColumn;
    }

    static DepositFillPattern from(DepositGuiProfile profile, String basePath, int rows) {
        String patternPath = basePath + ".Pattern.";
        return new DepositFillPattern(
                Type.fromString(profile.getString(patternPath + "Type", "ALL")),
                clamp(profile.getInt(patternPath + "From.Row", 1), 1, rows),
                clamp(profile.getInt(patternPath + "From.Column", 1), 1, 9),
                clamp(profile.getInt(patternPath + "To.Row", rows), 1, rows),
                clamp(profile.getInt(patternPath + "To.Column", 9), 1, 9)
        );
    }

    void apply(BaseGui gui, List<GuiItem> items) {
        if (gui == null || items == null || items.isEmpty()) {
            return;
        }

        GuiFiller filler = gui.getFiller();
        switch (type) {
            case BORDER:
                filler.fillBorder(items);
                break;
            case TOP:
                filler.fillTop(items);
                break;
            case BOTTOM:
                filler.fillBottom(items);
                break;
            case TOP_BOTTOM:
                filler.fillTop(items);
                filler.fillBottom(items);
                break;
            case LEFT:
                filler.fillSide(GuiFiller.Side.LEFT, items);
                break;
            case RIGHT:
                filler.fillSide(GuiFiller.Side.RIGHT, items);
                break;
            case SIDES:
                filler.fillSide(GuiFiller.Side.BOTH, items);
                break;
            case RECTANGLE:
                filler.fillBetweenPoints(fromRow, fromColumn, toRow, toColumn, items);
                break;
            case CHECKERBOARD:
                for (Integer slot : slots(gui.getRows())) {
                    gui.setItem(slot, items.get(slot % items.size()));
                }
                break;
            case ALL:
            default:
                filler.fill(items);
                break;
        }
    }

    private Set<Integer> slots(int rows) {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int row = 1; row <= rows; row++) {
            for (int column = 1; column <= 9; column++) {
                if (includes(row, column, rows)) {
                    slots.add((row - 1) * 9 + column - 1);
                }
            }
        }
        return slots;
    }

    private boolean includes(int row, int column, int rows) {
        switch (type) {
            case BORDER:
                return row == 1 || row == rows || column == 1 || column == 9;
            case TOP:
                return row == 1;
            case BOTTOM:
                return row == rows;
            case TOP_BOTTOM:
                return row == 1 || row == rows;
            case LEFT:
                return column == 1;
            case RIGHT:
                return column == 9;
            case SIDES:
                return column == 1 || column == 9;
            case RECTANGLE:
                return row >= Math.min(fromRow, toRow)
                        && row <= Math.max(fromRow, toRow)
                        && column >= Math.min(fromColumn, toColumn)
                        && column <= Math.max(fromColumn, toColumn);
            case CHECKERBOARD:
                return (row + column) % 2 == 0;
            case ALL:
            default:
                return true;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}

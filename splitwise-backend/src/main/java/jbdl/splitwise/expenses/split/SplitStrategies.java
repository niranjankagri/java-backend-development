package jbdl.splitwise.expenses.split;

import jbdl.splitwise.expenses.SplitType;

/** A tiny FACTORY: from the split type in the request to the strategy object that does the work. */
public final class SplitStrategies {

    private static final SplitStrategy EQUAL = new EqualSplit();
    private static final SplitStrategy EXACT = new ExactSplit();
    private static final SplitStrategy PERCENT = new PercentSplit();

    private SplitStrategies() {
    }

    /**
     * A switch expression over the enum has no default: a new split type that is not handled here is a
     * compile error, instead of silently becoming PERCENT (as the first if-chain version did).
     */
    public static SplitStrategy forType(SplitType type) {
        return switch (type) {
            case EQUAL -> EQUAL;
            case EXACT -> EXACT;
            case PERCENT -> PERCENT;
        };
    }
}

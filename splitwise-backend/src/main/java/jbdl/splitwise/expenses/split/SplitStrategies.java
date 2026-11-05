package jbdl.splitwise.expenses.split;

import jbdl.splitwise.expenses.SplitType;

/** A tiny FACTORY: from the split type in the request to the strategy object that does the work. */
public final class SplitStrategies {

    private static final SplitStrategy EQUAL = new EqualSplit();
    private static final SplitStrategy EXACT = new ExactSplit();
    private static final SplitStrategy PERCENT = new PercentSplit();

    private SplitStrategies() {
    }

    public static SplitStrategy forType(SplitType type) {
        if (type == SplitType.EQUAL) {
            return EQUAL;
        }
        if (type == SplitType.EXACT) {
            return EXACT;
        }
        return PERCENT;
    }
}

package jbdl.session25.compare;

import jbdl.session25.compare.claude.ClaudePriceCalculator;

/** Topic 6: runs all tests of {@link PriceCalculatorContract} against Claude Code's solution. */
class ClaudePriceCalculatorTest extends PriceCalculatorContract {

    @Override
    protected PriceCalculator createCalculator() {
        return new ClaudePriceCalculator();
    }
}

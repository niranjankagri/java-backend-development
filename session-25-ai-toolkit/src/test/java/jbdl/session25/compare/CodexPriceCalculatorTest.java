package jbdl.session25.compare;

import jbdl.session25.compare.codex.CodexPriceCalculator;

/** Topic 6: runs all tests of {@link PriceCalculatorContract} against Codex's solution. */
class CodexPriceCalculatorTest extends PriceCalculatorContract {

    @Override
    protected PriceCalculator createCalculator() {
        return new CodexPriceCalculator();
    }
}

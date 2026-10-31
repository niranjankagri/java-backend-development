# The problem: the price of a stay

Write a Java 17 class that implements this interface:

```java
package jbdl.session25.compare;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PriceCalculator {

    /**
     * Total price of a stay from checkIn (first night) to checkOut (day of leaving),
     * in rupees with 2 decimals.
     */
    BigDecimal total(BigDecimal nightlyPrice, LocalDate checkIn, LocalDate checkOut);
}
```

Rules:

1. The nights of a stay are the dates from `checkIn` (included) to `checkOut` (not included).
2. `checkOut` must be after `checkIn`, and a stay has at most 30 nights. `nightlyPrice` must be greater
   than zero. No argument may be null. If any of this is broken, throw `IllegalArgumentException`.
3. A Friday night or a Saturday night costs 20 % more than `nightlyPrice`.
4. A stay of 7 nights or more gets 10 % off the sum of all its nights.
5. A cleaning fee of 500.00 is added once per stay, after the discount (the fee is not discounted).
6. Round only the final total: 2 decimals, rounding half up. The result always has 2 decimals.

Constraints:

- Put the class in the package `jbdl.session25.compare.<tool>` and call it `<Tool>PriceCalculator`,
  where `<tool>` is given in the task.
- Only the JDK, no libraries. It must compile with `javac -Xlint:all -Werror`.
- Beginner-readable code with a class Javadoc. Do not write tests; only the class.

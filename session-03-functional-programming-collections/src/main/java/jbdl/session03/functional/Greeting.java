package jbdl.session03.functional;

import java.util.Locale;

/**
 * A custom functional interface: exactly one abstract method, so a lambda can implement it.
 * Default methods don't count. (The 2024 version was called FunctionalInterface, which hides
 * java.lang.FunctionalInterface, the annotation used right here.)
 */
@FunctionalInterface
public interface Greeting {

    String greet(String name);

    default String greetLoudly(String name) {
        return greet(name).toUpperCase(Locale.ROOT);
    }
}

package com.vassarlabs.aulm.ui;

/**
 * Separate entry point so the jar's Main-Class doesn't directly extend javafx.application.Application —
 * launching that class straight from a fat jar's manifest makes the JVM's module check think JavaFX
 * isn't present, even though it's bundled on the classpath.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}

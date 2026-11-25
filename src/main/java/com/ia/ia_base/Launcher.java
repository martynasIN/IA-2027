package com.ia.ia_base;

/**
 * Run this class to start the app — not MainApplication directly.
 *
 * Why? IntelliJ blocks running JavaFX classes directly when JavaFX
 * comes from Maven. This plain class has no JavaFX parent, so the
 * check is skipped and the app starts normally.
 *
 * How to run: right-click → Run 'Launcher.main()'
 */
public class Launcher {
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}
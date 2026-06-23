package io.github.kullik01.kinonode.controller;

/**
 * Interface implemented by controllers that need to refresh their data when loaded or swapped.
 */
public interface Refreshable {
    void refresh();
}

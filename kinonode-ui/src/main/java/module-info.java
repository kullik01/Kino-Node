module kinonode.ui {
    requires kinonode.domain;
    requires kinonode.data;

    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.web;
    requires java.logging;
    requires java.desktop;
    requires atlantafx.base;
    requires com.google.guice;
    requires jakarta.inject;
    requires com.google.gson;
    requires java.net.http;

    exports io.github.kullik01.kinonode;
    exports io.github.kullik01.kinonode.controller;
    exports io.github.kullik01.kinonode.view.pages;
    exports io.github.kullik01.kinonode.di;

    opens io.github.kullik01.kinonode to javafx.graphics;
    opens io.github.kullik01.kinonode.controller to com.google.guice, javafx.fxml;
    opens io.github.kullik01.kinonode.di to com.google.guice;
}

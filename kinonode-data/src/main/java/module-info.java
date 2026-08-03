module kinonode.data {
    requires transitive kinonode.domain;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires java.logging;
    requires java.desktop;
    requires java.net.http;
    requires com.google.guice;
    requires jakarta.inject;
    requires com.google.gson;

    exports io.github.kullik01.kinonode.repository;
    exports io.github.kullik01.kinonode.service;

    opens io.github.kullik01.kinonode.repository to com.google.guice;
    opens io.github.kullik01.kinonode.service to com.google.guice;
}

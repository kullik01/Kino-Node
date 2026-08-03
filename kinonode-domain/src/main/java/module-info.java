module kinonode.domain {
    requires com.google.gson;

    exports io.github.kullik01.kinonode.model;
    exports io.github.kullik01.kinonode.util;

    opens io.github.kullik01.kinonode.model to com.google.gson;
}

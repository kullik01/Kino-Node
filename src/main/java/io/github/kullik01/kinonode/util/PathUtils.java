package io.github.kullik01.kinonode.util;

import java.io.File;

public final class PathUtils {
    
    private PathUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Extracts the extension of a file in lowercase.
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    /**
     * Extracts the filename without its extension, useful for guessing titles.
     */
    public static String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return name;
        }
        return name.substring(0, lastIndexOf);
    }
}

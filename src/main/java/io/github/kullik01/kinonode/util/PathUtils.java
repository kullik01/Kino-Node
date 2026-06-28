/**
 * BSD 3-Clause License
 *
 * <p>Copyright (c) 2026, Hannah Kullik
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.util;

import java.io.File;

/**
 * Utility class containing static helper methods for manipulating file paths and names, such as
 * retrieving extensions or file base names.
 */
public final class PathUtils {

  private PathUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Extracts the extension of a file in lowercase format.
   *
   * @param file the File object from which to extract the extension
   * @return the lowercase file extension (excluding the dot), or an empty string if no extension is
   *     found
   */
  public static String getFileExtension(File file) {
    String tmpName = file.getName();
    int tmpLastIndexOf = tmpName.lastIndexOf(".");
    // If there is no dot in the filename, return an empty string
    if (tmpLastIndexOf == -1) {
      return "";
    }
    // Substring from the index after the last dot to the end, then convert to lowercase
    return tmpName.substring(tmpLastIndexOf + 1).toLowerCase();
  }

  /**
   * Extracts the filename without its extension, which is useful for guessing movie or node titles.
   *
   * @param file the File object from which to extract the name
   * @return the filename excluding the last dot and extension, or the full filename if no extension
   *     exists
   */
  public static String getFileNameWithoutExtension(File file) {
    String tmpName = file.getName();
    int tmpLastIndexOf = tmpName.lastIndexOf(".");
    // If there is no dot in the filename, the entire name is returned
    if (tmpLastIndexOf == -1) {
      return tmpName;
    }
    // Substring from the start of the filename up to (but not including) the last dot
    return tmpName.substring(0, tmpLastIndexOf);
  }
}

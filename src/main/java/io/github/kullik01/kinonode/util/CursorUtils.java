/**
 * BSD 3-Clause License
 *
 * <p>
 * Copyright (c) 2026, Hannah Kullik
 *
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.kullik01.kinonode.util;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

/**
 * Utility class to provide custom mouse cursors across the application.
 */
public class CursorUtils {
  private static final Logger LOGGER =
      Logger.getLogger(CursorUtils.class.getName());

  private static Cursor cameraCursor;

  /**
   * Gets the film camera mouse cursor based on the Rating camera icon.
   *
   * @return custom film camera {@link Cursor}, or {@link Cursor#HAND} as fallback
   */
  public static Cursor getCameraCursor() {
    if (cameraCursor == null) {
      try {
        InputStream tmpStream = CursorUtils.class.getResourceAsStream(
            "/io/github/kullik01/kinonode/images/camera_cursor.png");
        if (tmpStream != null) {
          Image tmpImage = new Image(tmpStream, 32, 32, true, true);
          cameraCursor = new ImageCursor(tmpImage, 4, 4);
        } else {
          cameraCursor = Cursor.HAND;
        }
      } catch (Exception tmpException) {
        LOGGER.log(Level.WARNING, "Failed to load camera cursor image",
            tmpException);
        cameraCursor = Cursor.HAND;
      }
    }
    return cameraCursor;
  }
}

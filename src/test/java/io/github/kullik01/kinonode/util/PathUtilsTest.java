/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2026, Hannah Kullik
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.Test;

public class PathUtilsTest {

  @Test
  public void testGetFileExtensionStandard() {
    File tmpFile = new File("movie.mp4");
    assertEquals("mp4", PathUtils.getFileExtension(tmpFile));
  }

  @Test
  public void testGetFileExtensionMixedCase() {
    File tmpFile = new File("movie.MKV");
    assertEquals("mkv", PathUtils.getFileExtension(tmpFile));
  }

  @Test
  public void testGetFileExtensionNoExtension() {
    File tmpFile = new File("movie");
    assertEquals("", PathUtils.getFileExtension(tmpFile));
  }

  @Test
  public void testGetFileNameWithoutExtensionStandard() {
    File tmpFile = new File("movie.mp4");
    assertEquals("movie", PathUtils.getFileNameWithoutExtension(tmpFile));
  }

  @Test
  public void testGetFileNameWithoutExtensionNoExtension() {
    File tmpFile = new File("movie");
    assertEquals("movie", PathUtils.getFileNameWithoutExtension(tmpFile));
  }

  @Test
  public void testGetFileNameWithoutExtensionWithDots() {
    File tmpFile = new File("some.movie.with.dots.mkv");
    assertEquals("some.movie.with.dots",
        PathUtils.getFileNameWithoutExtension(tmpFile));
  }
}

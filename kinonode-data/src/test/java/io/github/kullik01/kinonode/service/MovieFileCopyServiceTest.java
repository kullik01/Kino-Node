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
package io.github.kullik01.kinonode.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies copying, source protection, and cleanup using real temporary files.
 */
class MovieFileCopyServiceTest {
  @TempDir
  Path directory;
  private final MovieFileCopyService service = new MovieFileCopyService();

  @Test
  void copiesExactBytesWithoutChangingSource() throws IOException {
    byte[] tmpBytes = new byte[] {0, 1, -1, 32, 64};
    Path tmpSource =
        Files.write(directory.resolve("Movie original.mkv"), tmpBytes);
    FileTime tmpModified = Files.getLastModifiedTime(tmpSource);
    Path tmpTarget = directory.resolve("Shared Film ü.mkv");
    assertFalse(service.requiresReplacement(tmpSource, tmpTarget));
    assertEquals(tmpTarget, service.copy(tmpSource, tmpTarget, false));
    assertArrayEquals(tmpBytes, Files.readAllBytes(tmpTarget));
    assertArrayEquals(tmpBytes, Files.readAllBytes(tmpSource));
    assertEquals(tmpModified, Files.getLastModifiedTime(tmpSource));
    assertNoStagingFiles();
  }

  @Test
  void copiesEmptyFile() throws IOException {
    Path tmpSource = Files.createFile(directory.resolve("empty.mkv"));
    Path tmpTarget = directory.resolve("copy.mkv");
    service.copy(tmpSource, tmpTarget, false);
    assertEquals(0, Files.size(tmpTarget));
  }

  @Test
  void rejectsUnavailablePathsAndDirectories() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "movie");
    assertThrows(IOException.class, () -> service
        .copy(directory.resolve("missing"), directory.resolve("copy"), false));
    assertThrows(IOException.class,
        () -> service.copy(directory, directory.resolve("copy"), false));
    assertThrows(IOException.class, () -> service.copy(tmpSource,
        directory.resolve("missing/copy"), false));
    assertThrows(IOException.class,
        () -> service.copy(tmpSource, directory, true));
    assertNoStagingFiles();
  }

  @Test
  void refusesSourceAndNormalizedAlias() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "original");
    assertThrows(IOException.class,
        () -> service.copy(tmpSource, tmpSource, true));
    assertThrows(IOException.class, () -> service.copy(tmpSource,
        directory.resolve("sub/../source.mkv"), true));
    assertEquals("original", Files.readString(tmpSource));
    assertNoStagingFiles();
  }

  @Test
  void refusesHardLinkToSourceWhenSupported() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "original");
    Path tmpAlias = directory.resolve("alias.mkv");
    try {
      Files.createLink(tmpAlias, tmpSource);
    } catch (IOException | UnsupportedOperationException tmpException) {
      assumeTrue(false, "Hard links unavailable: " + tmpException);
    }
    assertThrows(IOException.class,
        () -> service.copy(tmpSource, tmpAlias, true));
    assertEquals("original", Files.readString(tmpSource));
  }

  @Test
  void refusesSymbolicLinkDestinationWhenSupported() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "original");
    Path tmpOther = Files.writeString(directory.resolve("other.mkv"), "other");
    Path tmpAlias = directory.resolve("alias.mkv");
    try {
      Files.createSymbolicLink(tmpAlias, tmpOther);
    } catch (IOException | UnsupportedOperationException tmpException) {
      assumeTrue(false, "Symbolic links unavailable: " + tmpException);
    }
    assertThrows(IOException.class,
        () -> service.copy(tmpSource, tmpAlias, true));
    assertEquals("other", Files.readString(tmpOther));
  }

  @Test
  void requiresApprovalToReplaceAndPublishesWholeCopy() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "new movie");
    Path tmpTarget =
        Files.writeString(directory.resolve("target.mkv"), "existing");
    assertTrue(service.requiresReplacement(tmpSource, tmpTarget));
    assertThrows(IOException.class,
        () -> service.copy(tmpSource, tmpTarget, false));
    assertEquals("existing", Files.readString(tmpTarget));
    service.copy(tmpSource, tmpTarget, true);
    assertEquals("new movie", Files.readString(tmpTarget));
    assertEquals("new movie", Files.readString(tmpSource));
    assertNoStagingFiles();
  }

  @Test
  void failedCopyPreservesExistingTargetAndCleansStaging() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "new movie");
    Path tmpTarget =
        Files.writeString(directory.resolve("target.mkv"), "existing");
    MovieFileCopyService tmpFailing = new MovieFileCopyService() {
      @Override
      void copyContents(Path source, Path staging) throws IOException {
        Files.writeString(staging, "partial");
        throw new IOException("Simulated full disk");
      }
    };
    assertThrows(IOException.class,
        () -> tmpFailing.copy(tmpSource, tmpTarget, true));
    assertEquals("existing", Files.readString(tmpTarget));
    assertEquals("new movie", Files.readString(tmpSource));
    assertNoStagingFiles();
  }

  @Test
  void handlesUnavailableAtomicReplacement() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "movie");
    Path tmpTarget =
        Files.writeString(directory.resolve("target.mkv"), "existing");
    MovieFileCopyService tmpFallback = new MovieFileCopyService() {
      @Override
      void moveAtomically(Path staging, Path destination) throws IOException {
        throw new AtomicMoveNotSupportedException(staging.toString(),
            destination.toString(),
            "Simulated provider without atomic replacement");
      }
    };
    tmpFallback.copy(tmpSource, tmpTarget, true);
    assertEquals("movie", Files.readString(tmpTarget));
    assertNoStagingFiles();
  }

  @Test
  void doesNotOverwriteTargetCreatedDuringCopy() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "movie");
    Path tmpTarget = directory.resolve("target.mkv");
    MovieFileCopyService tmpRacing = new MovieFileCopyService() {
      @Override
      void copyContents(Path source, Path staging) throws IOException {
        super.copyContents(source, staging);
        Files.writeString(tmpTarget, "new arrival");
      }
    };
    assertThrows(IOException.class,
        () -> tmpRacing.copy(tmpSource, tmpTarget, false));
    assertEquals("new arrival", Files.readString(tmpTarget));
    assertNoStagingFiles();
  }

  @Test
  void refusesChangedSourceAndDoesNotPublishPartialFile() throws IOException {
    Path tmpSource =
        Files.writeString(directory.resolve("source.mkv"), "movie");
    Path tmpTarget = directory.resolve("target.mkv");
    MovieFileCopyService tmpChanging = new MovieFileCopyService() {
      @Override
      void copyContents(Path source, Path staging) throws IOException {
        super.copyContents(source, staging);
        Files.writeString(source, "changed movie");
      }
    };
    assertThrows(IOException.class,
        () -> tmpChanging.copy(tmpSource, tmpTarget, false));
    assertFalse(Files.exists(tmpTarget));
    assertNoStagingFiles();
  }

  private void assertNoStagingFiles() throws IOException {
    try (var tmpFiles = Files.list(directory)) {
      assertFalse(tmpFiles.anyMatch(tmpFile -> tmpFile.getFileName().toString()
          .startsWith(".kinonode-share-")));
    }
  }
}

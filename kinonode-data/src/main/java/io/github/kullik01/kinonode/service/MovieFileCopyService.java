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

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copies movie files without changing the library or exposing incomplete
 * output.
 */
public class MovieFileCopyService {
  private static final Logger LOGGER =
      Logger.getLogger(MovieFileCopyService.class.getName());

  /** Constructs a stateless movie file copy service. */
  public MovieFileCopyService() {}

  /**
   * Validates the paths and checks whether replacement approval is needed.
   *
   * @param source the original movie
   * @param destination the requested copy
   * @return whether the destination already exists
   * @throws IOException if either path is unsuitable or identifies the source
   */
  public boolean requiresReplacement(Path source, Path destination)
      throws IOException {
    Path tmpSource = source.toAbsolutePath().normalize();
    Path tmpDestination = destination.toAbsolutePath().normalize();
    if (!Files.isRegularFile(tmpSource) || !Files.isReadable(tmpSource)) {
      throw new FileSystemException(tmpSource.toString(), null,
          "The movie file is unavailable or cannot be read.");
    }
    if (tmpSource.equals(tmpDestination) || (Files.exists(tmpDestination)
        && Files.isSameFile(tmpSource, tmpDestination))) {
      throw new FileSystemException(tmpDestination.toString(), null,
          "Choose a different location or filename for the copy.");
    }
    if (!Files.isDirectory(tmpDestination.getParent())) {
      throw new FileSystemException(tmpDestination.toString(), null,
          "The destination folder is unavailable.");
    }
    boolean tmpExists = Files.exists(tmpDestination, LinkOption.NOFOLLOW_LINKS);
    if (tmpExists
        && (!Files.isRegularFile(tmpDestination, LinkOption.NOFOLLOW_LINKS)
            || Files.isSymbolicLink(tmpDestination))) {
      throw new FileSystemException(tmpDestination.toString(), null,
          "The destination must be a regular file, not a directory or symbolic link.");
    }
    return tmpExists;
  }

  /**
   * Copies through a temporary file beside the destination, then publishes it.
   *
   * @param source the original movie
   * @param destination the requested copy
   * @param replaceExisting whether the user approved replacing the destination
   * @return the absolute saved path
   * @throws IOException if validation, copying, or publishing fails
   */
  public Path copy(Path source, Path destination, boolean replaceExisting)
      throws IOException {
    Path tmpDestination = destination.toAbsolutePath().normalize();
    if (requiresReplacement(source, tmpDestination) && !replaceExisting) {
      throw new FileAlreadyExistsException(tmpDestination.toString());
    }
    BasicFileAttributes tmpBefore =
        Files.readAttributes(source, BasicFileAttributes.class);
    Path tmpStaging = Files.createTempFile(tmpDestination.getParent(),
        ".kinonode-share-", ".part");
    Throwable tmpFailure = null;
    try {
      copyContents(source, tmpStaging);
      BasicFileAttributes tmpAfter =
          Files.readAttributes(source, BasicFileAttributes.class);
      if (Files.size(tmpStaging) != tmpBefore.size()
          || tmpAfter.size() != tmpBefore.size()
          || !tmpAfter.lastModifiedTime().equals(tmpBefore.lastModifiedTime())
          || !Objects.equals(tmpBefore.fileKey(), tmpAfter.fileKey())) {
        throw new IOException(
            "The source movie changed while it was being copied. Try again.");
      }
      if (requiresReplacement(source, tmpDestination) && !replaceExisting) {
        throw new FileAlreadyExistsException(tmpDestination.toString());
      }
      if (replaceExisting) {
        try {
          moveAtomically(tmpStaging, tmpDestination);
        } catch (AtomicMoveNotSupportedException tmpException) {
          Files.move(tmpStaging, tmpDestination,
              StandardCopyOption.REPLACE_EXISTING);
        }
      } else {
        Files.move(tmpStaging, tmpDestination);
      }
      return tmpDestination;
    } catch (IOException | RuntimeException | Error tmpException) {
      tmpFailure = tmpException;
      throw tmpException;
    } finally {
      try {
        Files.deleteIfExists(tmpStaging);
      } catch (IOException tmpException) {
        if (tmpFailure != null) {
          tmpFailure.addSuppressed(tmpException);
          LOGGER.log(Level.SEVERE,
              "Could not remove copy staging file " + tmpStaging, tmpException);
        } else {
          throw tmpException;
        }
      }
    }
  }

  // Package-private seams allow deterministic filesystem failure tests.
  void copyContents(Path source, Path staging) throws IOException {
    Files.copy(source, staging, StandardCopyOption.REPLACE_EXISTING);
  }

  void moveAtomically(Path staging, Path destination) throws IOException {
    Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING);
  }
}

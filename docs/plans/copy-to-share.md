# Movie details: Copy to Share implementation plan

- Date: 30 October 2026
- Feature branch: `codex/movie-details-copy-to-share`
- Baseline: `a730730` (`main` at branch creation)
- Status: Implemented and verified on Windows; see validation results below.

## Goal and scope

Add a visible **Copy to Share** button to Movie Details. Clicking it opens the
operating system's Save As dialog so the user can choose a destination and
filename. Saving creates a byte-for-byte copy of the movie file.

The original movie stays in place, and its catalog path, metadata, and watchlist
associations stay unchanged. Copy the single file referenced by
`Movie.getFilePath()`. Subtitles, posters, folders, transcoding, clipboard actions,
network sharing services, batch copying, and remembering the last destination are
outside this feature's initial scope. A mounted USB drive or network folder is a
valid destination when the operating system can write to it.

## Existing integration points

All paths below are relative to the repository root.

| Existing code | Relevance |
| --- | --- |
| `kinonode-ui/src/main/java/io/github/kullik01/kinonode/controller/MovieDetailController.java` | Builds the detail view programmatically. `buildView()` constructs the poster's `actionBox`; `setMovie()` receives the selected movie. Currently has no injected `ResourceBundle`. |
| `kinonode-ui/src/main/java/io/github/kullik01/kinonode/controller/LibraryController.java` | `openMovieDetail()` creates the controller, builds and initializes its view, then calls `setMovie()`. The details screen replaces the library grid inside the main window. |
| `kinonode-ui/src/main/java/io/github/kullik01/kinonode/controller/AnalyticsController.java` | Existing export handlers demonstrate `FileChooser.showSaveDialog(...)` with an owner window and suggested filename. |
| `kinonode-ui/src/main/java/io/github/kullik01/kinonode/uiutil/CustomAlert.java` | Provides themed, owner-aware success and error dialogs. |
| `kinonode-ui/src/main/java/io/github/kullik01/kinonode/uiutil/DialogBuilder.java` | Provides themed dialogs that can supply Replace/Cancel buttons. |
| `kinonode-ui/src/main/java/io/github/kullik01/kinonode/di/KinoNodeModule.java` | Binds services and already provides the shared `ResourceBundle`. |
| `kinonode-ui/src/main/resources/io/github/kullik01/kinonode/i18n/messages.properties` | Holds externalized UI strings. |
| `kinonode-domain/src/main/java/io/github/kullik01/kinonode/model/Movie.java` | Already contains the source file path. No model extension is needed. |

Follow `GEMINI.md` and the repository's `.agents/skills/add-feature.md`,
`.agents/skills/i18n-externalization.md`, and architecture standards. Use existing
JDK/JavaFX APIs and dependencies. No database migration or repository change is
needed.

## Intended user experience

1. Open a movie's details and see a text button labeled **Copy to Share** beneath
   the poster, alongside the existing actions. Use `button-secondary` styling and
   enough width for the full label; do not apply the existing 36-pixel icon-only
   sizing to this button.
2. Clicking the button opens a `FileChooser` through `showSaveDialog(...)`, owned
   by the current details view's window. Set the title to **Copy Movie to Share**
   and suggest the original file's full filename, including its extension.
   Use a source-extension filter plus All Files; use All Files alone for a file
   without an extension. Let the chooser select its normal initial directory.
3. The user chooses a folder and can rename the copy. Append the original
   extension if the entered filename has no extension; otherwise honor the
   chosen filename. Determine the final path before checking for a collision.
   Renaming never changes the movie's encoded contents.
4. Canceling the Save As dialog ends the action quietly, without creating files.
5. If the destination exists, require an explicit **Replace** decision before
   replacing it. Use an application Replace/Cancel dialog after selection so
   this policy is consistent across platforms; some native choosers may also
   display their own confirmation. Reject the source itself as a destination
   before offering replacement.
6. During copying, disable this button and show a dedicated indeterminate
   progress indicator and **Copying movie...** text near it. The main window
   remains responsive. Do not use the shared `showStatus()` label for ongoing
   progress: its five-second timer and metadata messages can clear that label.
7. On completion, restore the button and show a themed completion dialog with
   the movie name and saved path. On failure, restore the controls and show a
   useful error. Keep source files and catalog records unchanged in both cases.

The button starts disabled before `setMovie()`, remains disabled for a null or
blank source path, and stays disabled while its copy task is active. Check file
existence and readability in the worker immediately before copying; a stored
path can become unavailable after the details screen opens. Avoid filesystem
probing on the JavaFX thread, particularly for network paths.

## Implementation steps

### 1. Add a filesystem copy service

Create
`kinonode-data/src/main/java/io/github/kullik01/kinonode/service/MovieFileCopyService.java`.

- Keep the service independent of JavaFX, dialogs, database access, and mutable
  `Movie` instances. Accept captured `Path` values and an explicit replacement
  policy; return the saved `Path` or throw an exception describing failure.
- Expose a validation/preflight method for the worker to report whether the
  destination requires replacement approval. The copy method must repeat the
  relevant checks after approval because files may have changed meanwhile.
- Validate a readable regular source file, a valid destination parent, and an
  acceptable target type. Report an unavailable parent instead of creating
  arbitrary directory trees.
- Compare normalized absolute paths, then use `Files.isSameFile()` when the
  destination exists to reject the source itself, including hard-link and
  symbolic-link aliases. Reject destination directories and symbolic links
  rather than replacing them. Repeat identity checks before publishing.
- Create a uniquely named temporary file in the destination directory and copy
  the source into it with `Files.copy(...)`. Use `long` for file sizes. Do not
  read the movie into memory, use shell commands, or force `COPY_ATTRIBUTES`
  across filesystems. Check that the staged size matches the source size and
  detect source size/modification changes during copying before publishing.
- Publish the completed temporary file with `Files.move(...)`. For a new target,
  use a non-replacing move so a file created since validation causes failure.
  For an approved replacement, prefer an atomic replacement where supported;
  handle `AtomicMoveNotSupportedException` with a documented same-directory
  replacement fallback. Filesystem providers differ, so verify this on the
  supported platforms and do not promise crash-safe replacement everywhere.
- Clean up only the temporary file created by this operation on any handled
  failure. Do not delete an existing target to make room before copying. If
  cleanup also fails, retain the main failure and log the leftover file path.
  A forced application/process termination may leave a temporary file; automatic
  crash recovery is outside the initial scope.

Bind the stateless service in `KinoNodeModule.configure()` following existing
service bindings. The existing data module already exports and opens its
`service` package, so no JPMS change should be required.

### 2. Add localized strings and the button

Inject `MovieFileCopyService` and `ResourceBundle` through the existing
`MovieDetailController` constructor. Add the button, progress controls, and an
active-task field. Build and wire the new action in `buildView()` and update its
enabled state in `setMovie()` and `refresh()` without clearing an active copy.

Group all new strings under `movie.detail.share.*`, including the button,
tooltip, chooser title, file filters, copying state, replacement question and
buttons, completion text, and error messages. Suggested messages include:

- `movie.detail.share.btn.copy=Copy to Share`
- `movie.detail.share.dialog.title=Copy Movie to Share`
- `movie.detail.share.status.copying=Copying movie...`
- `movie.detail.share.error.source=The movie file is unavailable or cannot be read.`
- `movie.detail.share.error.same_file=Choose a different location or filename for the copy.`
- `movie.detail.share.error.copy=The movie could not be copied. Choose another location or try again.`

Use formatted resource messages for filenames and paths. Add the new service's
license header and public API Javadoc, use explicit imports and `tmp`-prefixed
locals, and follow the repository's formatter configuration.

### 3. Coordinate the dialog and background work

Implement a private `copyToShare()` handler:

1. Guard against missing movie/path or an already active operation. Capture the
   source path, display title, and owner window now, rather than reading a
   mutable `movie` field from a later callback.
2. Show the Save As dialog on the JavaFX Application Thread. Normalize the
   selected filename as described above. Return immediately on cancellation.
3. Run preflight in a JavaFX `Task` on a daemon thread. If replacement is needed,
   return to the FX thread for the themed confirmation; never block a worker
   waiting for `Platform.runLater()`. Keep the action busy during this sequence.
4. Start the copy in a second `Task<Path>` on a daemon thread. Let the service
   revalidate the paths and execute the copy. A newly appeared destination must
   fail unless replacement of that destination was approved.
5. Use task completion/failure handlers for all UI updates. Restore controls on
   every terminal path, including declined replacement or preflight failure.
   Log the actual exception with `LOGGER.log(Level.SEVERE, ..., tmpException)`;
   present an externalized explanation and useful filesystem error detail.

Keep navigation available during a copy. Capture the original owner window so a
completion/error dialog can still identify the copied movie if the user has
returned to the library or opened another movie. Update progress controls only
on the originating view; do not change the newly opened movie's status. Skip
dialogs if the owner has closed. Existing metadata autosave behavior continues
independently; copying must not call `movie.setFilePath(...)`, repository updates,
or library-change notifications.

Canceling a copy after it starts, percentage progress, resumable transfers, and
an application-wide transfer queue are deferred. The initial version provides
Save As cancellation and an indeterminate indicator while the copy completes.

### 4. Verify filesystem behavior and the actual dialog flow

Add JUnit 5 tests in
`kinonode-data/src/test/java/io/github/kullik01/kinonode/service/MovieFileCopyServiceTest.java`
using `@TempDir`, without a database or JavaFX runtime. Cover:

- Exact byte contents and unchanged source after copying to a new filename;
  include zero-byte files and filenames with spaces and Unicode characters.
- Missing/non-file source, missing destination parent, and destination directory.
- Same path, normalized aliases, and hard-link/symlink aliases where supported;
  the original must remain unchanged.
- Existing target without replacement approval stays unchanged; approved
  replacement receives the complete file.
- A failure while copying removes staging output and preserves any existing
  target. Use deterministic fault injection if needed instead of relying on
  OS-specific permission tricks. Cover the non-atomic fallback as well.
- A destination that appears after preflight is not silently overwritten in
  non-replacement mode; a source that changes during copying is rejected.

Manually verify on Windows, macOS, and Linux:

- Button visibility, keyboard access, readable label, and light/dark styling.
- An owned Save As dialog with the original filename and correct extension;
  selecting another folder, renaming, and canceling all behave as specified.
- Existing-target replacement/decline, including native chooser behavior.
- A multi-gigabyte movie copied to a local or removable destination while the
  UI stays responsive. Compare file sizes and a hash outside the app for QA.
- An offline source, unwritable destination, insufficient space, and an
  interrupted removable/network destination produce recoverable errors and
  never leave a partial file under the requested final filename.
- Navigating away while copying still yields an accurate result for the
  original movie; subsequent copies and playback continue to work.

Run `./gradlew spotlessApply`, `./gradlew spotlessCheck`, and `./gradlew build`
after implementation (`.\gradlew.bat` on Windows). Inspect the diff for unrelated
formatter changes. Launch with `./gradlew run` for UI checks. These commands are
future implementation checks; they are not required for this plan-only change.

## Acceptance criteria

- [ ] Movie Details includes a text button labeled **Copy to Share**.
- [ ] One click opens the owned native Save As dialog with the original filename.
- [ ] Canceling creates no file and shows no error.
- [ ] Saving copies the selected movie's bytes to the requested final path.
- [ ] Copying is performed off the FX thread with visible busy feedback.
- [ ] Existing files require replacement approval; copying onto the source or
      an alias is rejected.
- [ ] Handled failures clean up staging files and restore the action controls.
- [ ] Success and error messages identify the relevant movie/destination even
      after navigation.
- [ ] The original movie file and its catalog path remain unchanged.
- [ ] New strings, DI wiring, filesystem tests, formatting, build, and manual
      platform checks are complete.

## Implementation and validation results

Implemented using the existing Movie Details action container, native JavaFX
`FileChooser`, `Task`/daemon-thread pattern, `DialogBuilder`, `CustomAlert`,
ResourceBundle provider, and Guice service bindings. Filesystem operations live
in `MovieFileCopyService` in the existing data service package. No dependencies,
database changes, navigation framework, or alternative dialog/task system were
introduced. A controller-local busy flag tracks the two sequential tasks; task
cancellation is intentionally not exposed.

Validation completed on Windows with a repository-local Java 25 toolchain:

- `spotlessApply` and `spotlessCheck` passed, with no unrelated formatting diff.
- Sequential `spotlessCheck build` passed for all modules.
- 18 tests discovered: 17 passed, one symbolic-link test skipped because this
  Windows session cannot create symbolic links. The 11 new service tests cover
  byte contents, empty files, Unicode names, invalid paths, source aliases,
  replacement approval, cleanup on failure, atomic-replacement fallback, target
  creation during a copy, and source modification during a copy.
- Used the actual MovieDetailController with an isolated temporary movie fixture
  to inspect the button and native Save As dialog, save to another filename,
  observe the themed completion message, and cancel a subsequent save. The
  original and copied fixture had matching SHA-256 hashes. Cancel restored the
  button and removed the busy indicator without an error dialog.

Remaining platform QA: native dialogs on macOS/Linux, symbolic-link handling on
an enabled host, multi-gigabyte/removable/network transfers, and physical
full-disk or device-disconnection scenarios. Failure cleanup and fallback paths
were exercised deterministically in unit tests. These environment-dependent QA
items are not claimed as completed.

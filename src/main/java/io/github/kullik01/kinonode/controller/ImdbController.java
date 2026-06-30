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
package io.github.kullik01.kinonode.controller;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Controller for the embedded IMDb browser view. Allows searching films outside
 * the workspace using an internet connection.
 */
public class ImdbController implements Refreshable {

  @FXML
  private Button btnBack;
  @FXML
  private Button btnForward;
  @FXML
  private Button btnReload;
  @FXML
  private Button btnHome;
  @FXML
  private TextField txtUrl;
  @FXML
  private ProgressBar progressLoad;
  @FXML
  private WebView webView;
  @FXML
  private VBox paneError;

  private WebEngine webEngine;
  private static final String IMDB_HOME = "https://www.imdb.com/";

  /**
   * Initializes the IMDb web browser view. Configures a modern User Agent,
   * links the UI navigation buttons with web engine history properties, and
   * binds loading progress listeners.
   */
  @FXML
  public void initialize() {
    webEngine = webView.getEngine();

    // Set a modern/compatible User Agent to prevent rendering issues or
    // blocking
    webEngine.setUserAgent(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

    // Bind URL text field to current location
    webEngine.locationProperty().addListener((obs, oldVal, newVal) -> {
      txtUrl.setText(newVal);
    });

    // Bind progress bar and state listeners
    progressLoad.progressProperty()
        .bind(webEngine.getLoadWorker().progressProperty());

    webEngine.getLoadWorker().stateProperty()
        .addListener((obs, oldState, newState) -> {
          if (newState == Worker.State.SUCCEEDED) {
            progressLoad.setVisible(false);
            paneError.setVisible(false);
            webView.setVisible(true);
          } else if (newState == Worker.State.RUNNING) {
            progressLoad.setVisible(true);
            paneError.setVisible(false);
          } else if (newState == Worker.State.FAILED
              || newState == Worker.State.CANCELLED) {
            progressLoad.setVisible(false);
            webView.setVisible(false);
            paneError.setVisible(true);
          }
          updateHistoryButtons();
        });

    // Listen to history changes to keep buttons properly enabled/disabled
    webEngine.getHistory().currentIndexProperty()
        .addListener((obs, oldIdx, newIdx) -> {
          updateHistoryButtons();
        });

    // Load initial home page
    loadPage(IMDB_HOME);
  }

  public void loadPage(String url) {
    String tmpCurrent = webEngine.getLocation();
    if (tmpCurrent != null && url != null) {
      String tmpNormCurrent =
          tmpCurrent.endsWith("/") ? tmpCurrent : tmpCurrent + "/";
      String tmpNormUrl = url.endsWith("/") ? url : url + "/";
      if (tmpNormCurrent.equals(tmpNormUrl)) {
        webEngine.reload();
        return;
      }
    }
    webEngine.load(url);
  }

  private void updateHistoryButtons() {
    int currentIndex = webEngine.getHistory().getCurrentIndex();
    int historySize = webEngine.getHistory().getEntries().size();
    btnBack.setDisable(currentIndex <= 0);
    btnForward.setDisable(currentIndex >= historySize - 1);
  }

  @FXML
  private void handleBack() {
    int currentIndex = webEngine.getHistory().getCurrentIndex();
    if (currentIndex > 0) {
      webEngine.getHistory().go(-1);
    }
  }

  @FXML
  private void handleForward() {
    int currentIndex = webEngine.getHistory().getCurrentIndex();
    int historySize = webEngine.getHistory().getEntries().size();
    if (currentIndex < historySize - 1) {
      webEngine.getHistory().go(1);
    }
  }

  @FXML
  private void handleReload() {
    webEngine.reload();
  }

  @FXML
  private void handleHome() {
    loadPage(IMDB_HOME);
  }

  @FXML
  private void handleRetry() {
    loadPage(
        webEngine.getLocation() == null || webEngine.getLocation().isEmpty()
            ? IMDB_HOME
            : webEngine.getLocation());
  }

  @Override
  public void refresh() {
    loadPage(IMDB_HOME);
  }
}

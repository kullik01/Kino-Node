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
 * Controller for the embedded IMDb browser view. Allows searching for films
 * outside the workspace using an internet connection.
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
  private String pendingTargetUrl = null;
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
            injectAdSuppression();
            progressLoad.setVisible(false);
            paneError.setVisible(false);
            if (pendingTargetUrl == null) {
              webView.setVisible(true);
            } else {
              String tmpLoc = webEngine.getLocation();
              String tmpNormLoc =
                  tmpLoc != null && !tmpLoc.endsWith("/") ? tmpLoc + "/" : tmpLoc;
              String tmpNormTarget = pendingTargetUrl.endsWith("/")
                  ? pendingTargetUrl : pendingTargetUrl + "/";
              if (tmpNormLoc != null && tmpNormLoc.equals(tmpNormTarget)) {
                webView.setVisible(true);
                pendingTargetUrl = null;
              }
              // else: intermediate redirect (e.g. consent page) — stay hidden
            }
          } else if (newState == Worker.State.RUNNING) {
            progressLoad.setVisible(true);
            paneError.setVisible(false);
            webView.setVisible(false);
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
        pendingTargetUrl = null;
        webEngine.reload();
        return;
      }
    }
    pendingTargetUrl = url;
    webEngine.load(url);
  }

  private void injectAdSuppression() {
    // Step 1: inject CSS to hide ads, consent banners, and sign-in prompts
    webEngine.executeScript(
        "document.head.insertAdjacentHTML('beforeend',"
        + "'<style id=\"kino-block\">"
        // OneTrust cookie consent
        + "#onetrust-banner-sdk{display:none!important}"
        + "#onetrust-pc-sdk{display:none!important}"
        + ".onetrust-pc-dark-filter{display:none!important}"
        // IMDb ad slots (various patterns)
        + ".ipc-ad-slot{display:none!important}"
        + ".ipc-native-ad{display:none!important}"
        + "[data-testid~=ad-slot]{display:none!important}"
        + "[data-testid*=AdSlot]{display:none!important}"
        + "[class*=AdSlot]{display:none!important}"
        + "[class*=ad-unit]{display:none!important}"
        + "[class*=AdUnit]{display:none!important}"
        + "[id*=google_ads]{display:none!important}"
        + "iframe[src*=googlesyndication]{display:none!important}"
        + "iframe[src*=doubleclick]{display:none!important}"
        // IMDb sign-in prompt / nag modal
        + "[data-testid=promptable]{display:none!important}"
        + "[class*=Promptable]{display:none!important}"
        + "[class*=signin-modal]{display:none!important}"
        + "[class*=SigninModal]{display:none!important}"
        + "[id*=signin-modal]{display:none!important}"
        + "[data-testid=signin-prompt]{display:none!important}"
        + "[class*=user-notification]{display:none!important}"
        + "</style>');"
    );

    // DIAGNOSTIC: after 4 s log the id/class of all iframes and any element
    // whose id or class contains keywords associated with ads or sign-in prompts
    webEngine.executeScript(
        "setTimeout(function(){"
        + "var kw=/ad|sign|login|modal|prompt|banner|consent|cookie/i;"
        + "var all=document.querySelectorAll('*');"
        + "for(var i=0;i<all.length;i++){"
        + "var el=all[i];"
        + "var id=el.id||'';"
        + "var cl=el.className&&typeof el.className==='string'?el.className:'';"
        + "var tag=el.tagName;"
        + "if(tag==='IFRAME'||kw.test(id)||kw.test(cl)){"
        + "console.log('KINO-DBG tag='+tag+' id='+id+' class='+cl.substring(0,120));"
        + "}"
        + "}"
        + "},4000);"
    );

    // Step 2: attempt to click the reject/decline button at 1 s, 3 s, 6 s
    // to cover banners that load at different times
    String clickScript =
        "(function(){"
        + "function tryReject(){"
        + "var b=document.getElementById('onetrust-reject-all-handler');"
        + "if(!b)b=document.querySelector('[data-testid=\"reject-button-main\"]');"
        + "if(!b){var els=document.querySelectorAll('button');"
        + "for(var i=0;i<els.length;i++){"
        + "var t=els[i].innerText;"
        + "if(t&&/reject|decline/i.test(t)&&!/accept/i.test(t)){b=els[i];break;}}}"
        + "if(b){b.click();return true;}return false;}"
        + "tryReject();"
        + "setTimeout(tryReject,1000);"
        + "setTimeout(tryReject,3000);"
        + "setTimeout(tryReject,6000);"
        + "})();";
    webEngine.executeScript(clickScript);
  }

  private void updateHistoryButtons() {
    int currentIndex = webEngine.getHistory().getCurrentIndex();
    int historySize = webEngine.getHistory().getEntries().size();
    btnBack.setDisable(currentIndex <= 0);
    btnForward.setDisable(currentIndex >= historySize - 1);
  }

  @FXML
  private void handleBack() {
    pendingTargetUrl = null;
    int currentIndex = webEngine.getHistory().getCurrentIndex();
    if (currentIndex > 0) {
      webEngine.getHistory().go(-1);
    }
  }

  @FXML
  private void handleForward() {
    pendingTargetUrl = null;
    int currentIndex = webEngine.getHistory().getCurrentIndex();
    int historySize = webEngine.getHistory().getEntries().size();
    if (currentIndex < historySize - 1) {
      webEngine.getHistory().go(1);
    }
  }

  @FXML
  private void handleReload() {
    pendingTargetUrl = null;
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

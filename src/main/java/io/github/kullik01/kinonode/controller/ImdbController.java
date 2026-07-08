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

import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;

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
  private PauseTransition showDelay;
  private static final String IMDB_HOME = "https://www.imdb.com/";

  /**
   * Initializes the IMDb web browser view. Configures a modern User Agent,
   * links the UI navigation buttons with web engine history properties, and
   * binds loading progress listeners.
   */
  @FXML
  public void initialize() {
    webEngine = webView.getEngine();

    // User stylesheet applied before any page paint. Per CSS cascade,
    // user-stylesheet !important beats author inline !important, so OneTrust
    // can never override these rules — even with inline style="…!important".
    // file: URIs are reliably honoured by JavaFX WebKit; data: URIs are not.
    String tmpCss =
        "#onetrust-banner-sdk,#onetrust-pc-sdk,#onetrust-consent-sdk,"
            + ".onetrust-pc-dark-filter,[id^=onetrust]"
            + "{display:none!important;visibility:hidden!important;"
            + "opacity:0!important;pointer-events:none!important;"
            + "position:fixed!important;top:-9999px!important;"
            + "left:-9999px!important;width:0!important;"
            + "height:0!important;overflow:hidden!important}";
    try {
      java.io.File tmpCssFile = java.io.File.createTempFile("kino-ot-", ".css");
      tmpCssFile.deleteOnExit();
      java.nio.file.Files.write(tmpCssFile.toPath(),
          tmpCss.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      webEngine.setUserStyleSheetLocation(tmpCssFile.toURI().toString());
    } catch (Exception ignored) {
      // Fallback to data: URI if temp file creation fails
      webEngine.setUserStyleSheetLocation("data:text/css;base64,"
          + java.util.Base64.getEncoder().encodeToString(
              tmpCss.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    // Early cookie + CSS injection + MutationObserver: fires as soon as a
    // new DOM document is created (before SUCCEEDED). The OneTrust opt-out
    // cookie prevents the banner from ever being created; the CSS and
    // MutationObserver are fallback defenses.
    webEngine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
      if (newDoc != null) {
        // Set OneTrust opt-out cookies so the banner is never shown.
        // Must be a separate executeScript call so cookie setting succeeds
        // even if the main injection below fails, and vice versa.
        try {
          webEngine
              .executeScript("try{document.documentElement.style.setProperty("
                  + "'opacity','0','important');}catch(e){}"
                  + "try{document.cookie='OptanonAlertBoxClosed='"
                  + "+new Date().toISOString()"
                  + "+';path=/;domain=.imdb.com;max-age=31536000';}catch(e){}"
                  + "try{document.cookie='OptanonConsent="
                  + "isGpcEnabled=1&groups=C0001%3A1%2CC0002%3A0%2CC0003%3A0"
                  + "%2CC0004%3A0;path=/;domain=.imdb.com;max-age=31536000';"
                  + "}catch(e){}");
        } catch (Exception ignored) {
        }
        try {
          webEngine.executeScript(
              // Always set stubs so other scripts never error on OneTrust
              // calls.
              "window.OneTrust={Init:function(){},ToggleInfoDisplay:"
                  + "function(){},LoadBanner:function(){},Close:function(){}};"
                  + "window.Optanon={};" + "window.OptanonWrapper=function(){};"
                  + "window.OptanonActive=true;" + "if(!window.__kinoEarlyObs){"
                  + "window.__kinoEarlyObs=true;"
          // CSS-based hiding (synchronous fallback)
                  + "if(!document.getElementById('kino-early')){"
                  + "var s=document.createElement('style');"
                  + "s.id='kino-early';"
                  + "s.textContent='#onetrust-banner-sdk,#onetrust-pc-sdk,"
                  + "#onetrust-consent-sdk,.onetrust-pc-dark-filter,[id^=onetrust]"
                  + "{display:none!important;visibility:hidden!important;"
                  + "opacity:0!important;pointer-events:none!important;"
                  + "position:fixed!important;top:-9999px!important;"
                  + "left:-9999px!important;width:0!important;"
                  + "height:0!important;overflow:hidden!important}';"
                  + "(document.head||document.documentElement).appendChild(s);}"
          // Script-block helper
                  + "window.__kinoBlockScript=function(el){el.type='text/blocked';el.src='';};"
                  + "window.__kinoIsOT=function(src){"
                  + "return src.indexOf('onetrust')>-1||src.indexOf('optanon')>-1"
                  + "||src.indexOf('cookielaw')>-1||src.indexOf('cookie-cdn')>-1;};"
          // Hide helper — also watches element's style attribute so any
          // re-show attempt by OneTrust is caught before the next paint.
                  + "window.__kinoHideOT=function(el){"
                  + "el.style.setProperty('display','none','important');"
                  + "el.style.setProperty('visibility','hidden','important');"
                  + "el.style.setProperty('opacity','0','important');"
                  + "el.style.setProperty('position','fixed','important');"
                  + "el.style.setProperty('top','-9999px','important');"
                  + "if(window.__kinoMutObs){"
                  + "window.__kinoMutObs.observe(el,"
                  + "{attributes:true,attributeFilter:['style','class']});}};"
          // Named observer handles both childList and attribute mutations
                  + "window.__kinoMutObs=new MutationObserver(function(ml){"
                  + "for(var m=0;m<ml.length;m++){" + "var mut=ml[m];"
          // Attribute change on a known OneTrust element — re-hide it
                  + "if(mut.type==='attributes'){" + "var t=mut.target;"
                  + "if(t.id&&t.id.indexOf('onetrust')===0){"
                  + "t.style.setProperty('display','none','important');"
                  + "t.style.setProperty('visibility','hidden','important');"
                  + "t.style.setProperty('opacity','0','important');"
                  + "t.style.setProperty('position','fixed','important');"
                  + "t.style.setProperty('top','-9999px','important');}"
                  + "continue;}"
          // New node added to DOM
                  + "var added=mut.addedNodes;"
                  + "for(var n=0;n<added.length;n++){" + "var nd=added[n];"
                  + "if(nd.nodeType!==1)continue;"
                  + "if(nd.tagName==='SCRIPT'&&window.__kinoIsOT(nd.src||''))"
                  + "{window.__kinoBlockScript(nd);continue;}"
                  + "if(nd.id&&nd.id.indexOf('onetrust')===0)"
                  + "{window.__kinoHideOT(nd);continue;}"
                  + "if(nd.classList&&nd.classList.contains('onetrust-pc-dark-filter'))"
                  + "{window.__kinoHideOT(nd);continue;}"
                  + "if(nd.querySelectorAll){"
                  + "nd.querySelectorAll('script[src*=onetrust],script[src*=optanon],"
                  + "script[src*=cookielaw],script[src*=cookie-cdn]')"
                  + ".forEach(window.__kinoBlockScript);"
                  + "nd.querySelectorAll('[id^=onetrust]').forEach(window.__kinoHideOT);"
                  + "nd.querySelectorAll('.onetrust-pc-dark-filter')"
                  + ".forEach(window.__kinoHideOT);}}}});"
                  + "window.__kinoMutObs.observe(document.documentElement,"
                  + "{childList:true,subtree:true});"
          // Retroactive scan for any OneTrust scripts already in the DOM
                  + "document.querySelectorAll('script[src*=onetrust],script[src*=optanon],"
                  + "script[src*=cookielaw],script[src*=cookie-cdn]')"
                  + ".forEach(window.__kinoBlockScript);" + "}");
        } catch (Exception ignored) {
        }
      }
    });

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
            try {
              injectAdSuppression();
            } catch (Exception ignored) {
            }
            progressLoad.setVisible(false);
            paneError.setVisible(false);
            boolean tmpShow = false;
            if (pendingTargetUrl == null) {
              tmpShow = true;
            } else {
              String tmpLoc = webEngine.getLocation();
              String tmpNormLoc =
                  tmpLoc != null && !tmpLoc.endsWith("/") ? tmpLoc + "/"
                      : tmpLoc;
              String tmpNormTarget =
                  pendingTargetUrl.endsWith("/") ? pendingTargetUrl
                      : pendingTargetUrl + "/";
              if (tmpNormLoc != null && tmpNormLoc.equals(tmpNormTarget)) {
                tmpShow = true;
                pendingTargetUrl = null;
              }
              // else: intermediate redirect (e.g. consent page) — stay hidden
            }
            if (tmpShow) {
              if (showDelay != null) {
                showDelay.stop();
              }
              // Wait 800 ms after SUCCEEDED: the 500 ms suppression scan
              // has run, the MutationObserver has caught any late elements,
              // and the page is safe to show. Opacity is restored here
              // (atomically with WebView visibility) so the banner never
              // has a chance to appear between reveal and first paint.
              showDelay = new PauseTransition(Duration.millis(800));
              showDelay.setOnFinished(e -> {
                try {
                  webEngine.executeScript("document.documentElement.style"
                      + ".setProperty('opacity','1','important');"
                      + "setTimeout(function(){"
                      + "document.documentElement.style"
                      + ".removeProperty('opacity');},50);");
                } catch (Exception ignored) {
                }
                webView.setVisible(true);
              });
              showDelay.play();
            }
          } else if (newState == Worker.State.RUNNING) {
            if (showDelay != null) {
              showDelay.stop();
            }
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
    webEngine.executeScript("(function(){" + "if(window.__kinoInit)return;"
        + "window.__kinoInit=true;"
        // Re-set OneTrust opt-out cookies (backup for early injection)
        + "try{document.cookie='OptanonAlertBoxClosed='"
        + "+new Date().toISOString()"
        + "+';path=/;domain=.imdb.com;max-age=31536000';}catch(e){}"
        + "try{document.cookie='OptanonConsent="
        + "isGpcEnabled=1&groups=C0001%3A1%2CC0002%3A0%2CC0003%3A0"
        + "%2CC0004%3A0;path=/;domain=.imdb.com;max-age=31536000';"
        + "}catch(e){}"
        // Re-set stubs in case early injection was skipped
        + "window.OneTrust={Init:function(){},ToggleInfoDisplay:"
        + "function(){},LoadBanner:function(){},Close:function(){}};"
        + "window.Optanon={};" + "window.OptanonWrapper=function(){};"
        + "window.OptanonActive=true;"
        // Block any OneTrust scripts already in the DOM at SUCCEEDED time
        + "(function(){" + "function __b(el){el.type='text/blocked';el.src='';}"
        + "document.querySelectorAll("
        + "'script[src*=onetrust],script[src*=optanon],"
        + "script[src*=cookielaw],script[src*=cookie-cdn]').forEach(__b);"
        + "})()" + ";"
        // CSS for stable selectors (ads, iframes)
        + "var s=document.createElement('style');" + "s.id='kino-block';"
        + "s.textContent=" + "'.ipc-ad-slot,.ipc-native-ad"
        + "{display:none!important}"
        + "[data-testid~=ad-slot],[data-testid*=AdSlot]"
        + "{display:none!important}" + "ins.adsbygoogle"
        + "{display:none!important}"
        + "iframe[src*=googlesyndication],iframe[src*=doubleclick]"
        + ",iframe[src*=amazon-adsystem]" + "{display:none!important}"
        + "[data-testid=promptable]" + "{display:none!important}"
        + "#onetrust-banner-sdk,#onetrust-pc-sdk,#onetrust-consent-sdk,"
        + ".onetrust-pc-dark-filter,[id^=onetrust]"
        + "{display:none!important;visibility:hidden!important;"
        + "opacity:0!important;pointer-events:none!important;"
        + "position:fixed!important;top:-9999px!important;"
        + "left:-9999px!important;width:0!important;"
        + "height:0!important;overflow:hidden!important}';"
        + "document.head.appendChild(s);"
        // Hide OneTrust; also delegate to early observer if available so
        // attribute-watching keeps the element hidden if OneTrust re-shows it.
        + "function hideOT(el){"
        + "el.style.setProperty('display','none','important');"
        + "el.style.setProperty('opacity','0','important');"
        + "el.style.setProperty('pointer-events','none','important');"
        + "el.style.setProperty('position','fixed','important');"
        + "el.style.setProperty('top','-9999px','important');"
        + "if(window.__kinoMutObs){" + "window.__kinoMutObs.observe(el,"
        + "{attributes:true,attributeFilter:['style','class']});}"
        + "var b=el.querySelector('#onetrust-reject-all-handler');"
        + "if(!b){var bs=el.querySelectorAll('button');"
        + "for(var i=0;i<bs.length;i++){var t=(bs[i].innerText||'').trim();"
        + "if(/^(reject all|decline all|reject|decline)$/i.test(t)){b=bs[i];break;}}}"
        + "if(b)b.click();" + "}"
        // --- hideMatching: expensive scan for ads / sign-in (timed retries
        // only) ---
        + "function hideMatching(){"
        // OneTrust — hide off-screen + click Decline
        + "['onetrust-banner-sdk','onetrust-pc-sdk'].forEach(function(oid){"
        + "var o=document.getElementById(oid);" + "if(o)hideOT(o);});"
        + "var otBg=document.querySelector('.onetrust-pc-dark-filter');"
        + "if(otBg)otBg.style.setProperty('display','none','important');"
        // Ads / cookie / consent / signin — class keyword scan
        + "var all=document.querySelectorAll('div,section,aside,article,ins');"
        + "for(var i=0;i<all.length;i++){" + "var el=all[i];"
        + "var cls=(typeof el.className==='string'?el.className:'').toLowerCase();"
        + "var tid=(el.getAttribute('data-testid')||'').toLowerCase();"
        + "var id=(el.id||'').toLowerCase();"
        + "var aria=(el.getAttribute('aria-label')||'').toLowerCase();"
        + "if(cls.indexOf('adslot')>-1||cls.indexOf('ad-slot')>-1"
        + "||cls.indexOf('adunit')>-1||cls.indexOf('ad-unit')>-1"
        + "||cls.indexOf('sponsor')>-1"
        + "||tid.indexOf('ad-slot')>-1||tid.indexOf('adslot')>-1"
        + "||tid.indexOf('native-ad')>-1"
        + "||aria==='advertisement'||aria.indexOf('sponsored')>-1" + "){"
        + "el.style.setProperty('display','none','important');continue;}"
        + "if((cls.indexOf('cookie')>-1||id.indexOf('cookie')>-1"
        + "||cls.indexOf('consent')>-1||id.indexOf('consent')>-1"
        + "||cls.indexOf('signin')>-1||cls.indexOf('sign-in')>-1"
        + "||tid.indexOf('signin')>-1||tid.indexOf('sign-in')>-1)"
        + "&&el.tagName!=='BUTTON'&&el.tagName!=='A'){"
        + "el.style.setProperty('display','none','important');" + "}}"
        // Sign-in popup: find positioned ancestor of "Create account"
        + "var ds=document.querySelectorAll('div');"
        + "for(var i=0;i<ds.length;i++){" + "var txt=ds[i].innerText||'';"
        + "if(txt.indexOf('Create account')>-1){" + "var p=ds[i];"
        + "while(p&&p!==document.body){"
        + "var ps=window.getComputedStyle(p).position;"
        + "if(ps==='absolute'||ps==='fixed'){"
        + "var r=p.getBoundingClientRect();" + "if(r.width<500&&r.height<500)"
        + "p.style.setProperty('display','none','important');"
        + "break;}p=p.parentElement;}}}" + "}"
        // Reject-button click
        + "function tryReject(){"
        + "var b=document.getElementById('onetrust-reject-all-handler');"
        + "if(!b)b=document.querySelector('[data-testid=\"reject-button-main\"]');"
        + "if(!b){" + "var els=document.querySelectorAll('button,a');"
        + "for(var i=0;i<els.length;i++){"
        + "var t=(els[i].innerText||'').trim();"
        + "if(/^(reject all|decline all|reject|decline)$/i.test(t)){b=els[i];break;}"
        + "}}" + "if(b)b.click();" + "}"
        // Run expensive scan on fixed schedule only (6 times total)
        + "tryReject();hideMatching();"
        + "setTimeout(function(){tryReject();hideMatching();},100);"
        + "setTimeout(function(){tryReject();hideMatching();},500);"
        + "setTimeout(function(){tryReject();hideMatching();},1500);"
        + "setTimeout(function(){tryReject();hideMatching();},3000);"
        + "setTimeout(function(){tryReject();hideMatching();},6000);"
        // Fallback MutationObserver: only installed if the early observer
        // (from documentProperty listener) was not set up for this page.
        + "if(!window.__kinoEarlyObs){" + "new MutationObserver(function(ml){"
        + "for(var m=0;m<ml.length;m++){" + "var added=ml[m].addedNodes;"
        + "for(var n=0;n<added.length;n++){" + "var nd=added[n];"
        + "if(nd.nodeType!==1)continue;"
        + "if(nd.id&&nd.id.indexOf('onetrust')===0){hideOT(nd);continue;}"
        + "if(nd.classList&&nd.classList.contains('onetrust-pc-dark-filter')){"
        + "nd.style.setProperty('display','none','important');continue;}"
        + "if(nd.querySelectorAll){"
        + "nd.querySelectorAll('[id^=onetrust]').forEach(function(el){hideOT(el);});"
        + "nd.querySelectorAll('.onetrust-pc-dark-filter').forEach(function(el){"
        + "el.style.setProperty('display','none','important');});}" + "}}"
        + "}).observe(document.documentElement,{childList:true,subtree:true});}"
        + "})();");
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

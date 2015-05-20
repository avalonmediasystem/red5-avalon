/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.avalonmediasystem.red5;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.util.ScopeUtils;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author mbk836
 */
public class AvalonFilenameGenerator implements IStreamFilenameGenerator {
  private static Logger log = Red5LoggerFactory.getLogger(
      AvalonFilenameGenerator.class, "plugins");

  private String avalonUrl = "http://localhost:3000/";
  private String streamBase = "streams";

  private URL baseAuthUrl;

  public void initAuthHandler() throws MalformedURLException {
    baseAuthUrl = new URL(avalonUrl);
    log.info("Setting base auth URL to " + baseAuthUrl.toString());
  }

  private Boolean authStream(String streamUrl, String authToken) {
    URL authUrl;

    try {
      authUrl = new URL(baseAuthUrl, "authorize?token=" + authToken);
    } catch (MalformedURLException err) {
      log.error("Error parsing URL", err);
      return false;
    }

    log.info("Authorizing against " + authUrl.toString());
    try {
      HttpURLConnection http = (HttpURLConnection) authUrl.openConnection();
      http.addRequestProperty("Accept", "text/plain");
      http.setRequestMethod("GET");
      http.connect();
      if (http.getResponseCode() != 202) {
        return false;
      } else {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            http.getInputStream()));
        String authorized = reader.readLine();
        while (authorized != null) {
          if (streamUrl.contains(authorized)) {
            return true;
          }
          authorized = reader.readLine();
        }
        return false;
      }
    } catch (IOException err) {
      log.error("Error connecting to " + authUrl.toString(), err);
      return false;
    }
  }

  public String generateFilename(IScope scope, String name, GenerationType type) {
    String sep = System.getProperty("file.separator", "/");

    String authToken = null;

    final StringBuilder result = new StringBuilder();
    final IScope app = ScopeUtils.findApplication(scope);
    while (scope != null && scope != app) {
        result.insert(0, scope.getName() + sep);
        scope = scope.getParent();
    }
    result.insert(0,streamBase).append(sep);
    
    log.debug("request: " + name);
    Pattern tokenRe = Pattern.compile("\\?token=([^./]+)");
    Matcher token = tokenRe.matcher(name);
    if (token.find()) {
      authToken = token.group(1);
      log.debug("authToken: " + authToken);
    }
    String fullName = token.replaceAll("");
    result.append(fullName);
    log.debug("resolving: " + fullName);

    if (!authStream(fullName, authToken)) {
      return null;
    } else {
      return result.toString();
    }
  }

  public String generateFilename(IScope scope, String name, String extension,
      GenerationType type) {
    return (generateFilename(scope, name + extension, type));
  }

  public boolean resolvesToAbsolutePath() {
    if (streamBase == null) {
      return false;
    } else {
      Path p = Paths.get(streamBase);
      return p.isAbsolute();
    }
  }

  public String getAvalonUrl() {
    return avalonUrl;
  }

  public void setAvalonUrl(String url) throws MalformedURLException {
    avalonUrl = url;
    initAuthHandler();
  }
  
  public String getStreamBase() {
    return streamBase;
  }
  
  public void setStreamBase(String base) {
    streamBase = base;
  }
}

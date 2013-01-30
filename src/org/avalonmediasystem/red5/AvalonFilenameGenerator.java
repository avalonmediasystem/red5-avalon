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
    private static Logger log = Red5LoggerFactory.getLogger(AvalonFilenameGenerator.class, "plugins");

    public String avalonUrl = "http://localhost:3000/";

    private URL baseAuthUrl;

    public void initAuthHandler() throws MalformedURLException {
        baseAuthUrl = new URL(avalonUrl);
        log.info("Setting base auth URL to " + baseAuthUrl.toString());
    }

    private Boolean authStream(String mediaPackage, String authToken) {
        URL authUrl;

        try {
            authUrl = new URL(baseAuthUrl, "authorize?token="+authToken);
        } catch(MalformedURLException err) {
            log.error("Error parsing URL", err);
            return false;
        }

        log.info("Authorizing against " + authUrl.toString());
        try {
            HttpURLConnection http = (HttpURLConnection)authUrl.openConnection();
            http.addRequestProperty("Accept", "text/plain");
            http.setRequestMethod("GET");
            http.connect();
            if (http.getResponseCode() != 202 ) {
                return false;
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String authorized = reader.readLine().trim();
                return authorized.equals(mediaPackage);
            }
        } catch (IOException err) {
            log.error("Error connecting to " + authUrl.toString(), err);
            return false;
        }
    }
    
    public String generateFilename(IScope scope, String name, GenerationType type) {
        String sep = System.getProperty("file.separator","/");
        final StringBuilder result = new StringBuilder();
        final IScope app = ScopeUtils.findApplication(scope);
        final String prefix = "streams" + sep;
        while (scope != null && scope != app) {
            result.insert(0, scope.getName() + sep);
            scope = scope.getParent();
        }
        result.insert(0,prefix);

        String fullName = scope.getContextPath() + sep + name;
        String authToken = null;

        log.debug("request: " + fullName);
        Pattern tokenRe = Pattern.compile("\\?token=([^./]+)");
        Matcher token = tokenRe.matcher(fullName);
        if (token.find()) {
            authToken = token.group(1);
            log.debug("authToken: " + authToken);
        }
        fullName = token.replaceAll("");
        log.debug("resolving: " + fullName);

        Pattern splitterRe = Pattern.compile("^/(.+?)/(?:(.+):)?(.+)/(.+)/(.+?)(?:\\.(.+))?$");
        Matcher match = splitterRe.matcher(fullName);
        if (match.matches()) {
            String appName      = match.group(1);
            String ext          = match.group(2);
            if (ext == null) {
                ext = match.group(6);
            }
            String mediaPackage = match.group(3);
            String derivative   = match.group(4);
            String stream       = match.group(5);
            result.append(mediaPackage).append(sep).append(derivative).append(sep).append(stream).append(".").append(ext);
            log.debug("location: " + result.toString());
            if (!authStream(mediaPackage,authToken)) {
                return(null);
            }
        } else {
            return(null);
        }
        return(result.toString());
    }

    public String generateFilename(IScope scope, String name, String extension, GenerationType type) {
        return(generateFilename(scope, name+extension, type));
    }

    public boolean resolvesToAbsolutePath() {
        return false;
    }

    public String getAvalonUrl() {
        return avalonUrl;
    }

    public void setAvalonUrl(String url) throws MalformedURLException {
        avalonUrl = url;
        initAuthHandler();
    }
}

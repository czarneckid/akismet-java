/**
 * Copyright (c) 2005-2007, David A. Czarnecki
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the "David A. Czarnecki" nor the names of
 * its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Products derived from this software may not be called "Akismet Java API",
 * nor may "Akismet Java API" appear in their name, without prior written permission of
 * David A. Czarnecki.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.sf.akismet;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Akistmet Java API
 * <p></p>
 * <a href="http://akismet.com/development/api/">Akismet API</a> documentation.
 *
 * @author David Czarnecki
 * @version $Id: Akismet.java,v 1.5 2007/01/19 00:33:08 czarneckid Exp $
 */
public class Akismet {

    private Log logger = LogFactory.getLog(Akismet.class);

    // Constants
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String USER_AGENT_VALUE = "Akismet Java API/1.02";

    private static final String API_PARAMETER_KEY = "key";
    private static final String API_PARAMETER_BLOG = "blog";
    private static final String API_PARAMETER_USER_IP = "user_ip";
    private static final String API_PARAMETER_USER_AGENT = "user_agent";
    private static final String API_PARAMETER_REFERRER = "referrer";
    private static final String API_PARAMETER_PERMALINK = "permalink";
    private static final String API_PARAMETER_COMMENT_TYPE = "comment_type";
    private static final String API_PARAMETER_COMMENT_AUTHOR = "comment_author";
    private static final String API_PARAMETER_COMMENT_AUTHOR_EMAIL = "comment_author_email";
    private static final String API_PARAMETER_COMMENT_AUTHOR_URL = "comment_author_url";
    private static final String API_PARAMETER_COMMENT_CONTENT = "comment_content";

    private static final String VALID_RESPONSE = "valid";
    private static final String FALSE_RESPONSE = "false";

    public static final String COMMENT_TYPE_BLANK = "";
    public static final String COMMENT_TYPE_COMMENT = "comment";
    public static final String COMMENT_TYPE_TRACKBACK = "trackback";
    public static final String COMMENT_TYPE_PINGBACK = "pingback";

    private HttpClient httpClient;
    private String apiKey;
    private String blog;
    private boolean verifiedKey = false;
    private int httpResult;

    /**
     * Construct an instance to work with the Akismet API.
     * <p></p>
     * <pre>
     * Usage:
     * <p/>
     * Akismet akismet = new Akismet("Your API key", "http://your.blog.com/");
     * System.out.println("Testing comment spam: " + akismet.commentCheck("x.y.z.w", "XXX", "", "", "", "", "", "", "VIAGRA! LOTS OF VIAGRA!", null));
     * </pre>
     * <p>
     * You <strong>do not</strong> need to call {@link #verifyAPIKey()} before using the {@link #commentCheck(String, String, String, String, String, String, String, String, String, java.util.Map)}, 
     * {@link #submitSpam(String, String, String, String, String, String, String, String, String, java.util.Map)}, or
     * {@link #submitHam(String, String, String, String, String, String, String, String, String, java.util.Map)} methods.
     * </p>
     *
     * @param apiKey Akismet API key
     * @param blog   Blog associated with the API key
     * @throws IllegalArgumentException If either the API key or blog is <code>null</code>
     */
    public Akismet(String apiKey, String blog) {
        this.apiKey = apiKey;
        this.blog = blog;

        if (apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }

        if (blog == null) {
            throw new IllegalArgumentException("Blog cannot be null");
        }

        httpClient = new HttpClient();
        HttpClientParams httpClientParams = new HttpClientParams();
        DefaultHttpMethodRetryHandler defaultHttpMethodRetryHandler = new DefaultHttpMethodRetryHandler(0, false);
        httpClientParams.setParameter(USER_AGENT_HEADER, USER_AGENT_VALUE);
        httpClientParams.setParameter(HttpClientParams.RETRY_HANDLER, defaultHttpMethodRetryHandler);
        httpClient.setParams(httpClientParams);
    }

    /**
     * Return the HTTP status code of the last operation
     *
     * @return HTTP status code
     */
    public int getHttpResult() {
        return httpResult;
    }

    /**
     * Check to see if the API key has been verified
     *
     * @return <code>true</code> if the API key has been verified, <code>false</code> otherwise
     */
    public boolean isVerifiedKey() {
        return verifiedKey;
    }

    /**
     * Sets proxy configuration information. This method must be called before
     * any calls to the API if you require proxy configuration.
     *
     * @param proxyHost Proxy host
     * @param proxyPort Proxy port
     */
    public void setProxyConfiguration(String proxyHost, int proxyPort) {
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setProxy(proxyHost, proxyPort);

        httpClient.setHostConfiguration(hostConfiguration);
    }

    /**
     * Check to see if the input is <code>null</code> or blank
     *
     * @param input Input
     * @return <code>true</code> if input is null or blank, <code>false</code> otherwise
     */
    private boolean checkNullOrBlank(String input) {
        return (input == null || "".equals(input));
    }

    /**
     * Sets proxy authentication information. This method must be called before any
     * calls to the API if you require proxy authentication.
     *
     * @param proxyUsername Username to access proxy
     * @param proxyPassword Password to access proxy
     */
    public void setProxyAuthenticationConfiguration(String proxyUsername, String proxyPassword) {
        httpClient.getState().setProxyCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxyUsername, proxyPassword));
    }

    /**
     * Verify your API key
     *
     * @return <code>true</code> if the API key has been verified, <code>false</code> otherwise
     */
    public boolean verifyAPIKey() {
        boolean callResult = true;

        PostMethod post = new PostMethod("http://rest.akismet.com/1.1/verify-key");
        post.addParameter(API_PARAMETER_KEY, apiKey);
        post.addParameter(API_PARAMETER_BLOG, blog);

        try {
            httpResult = httpClient.executeMethod(post);
            String result = post.getResponseBodyAsString();

            if (logger.isDebugEnabled()) {
                logger.debug("Akismet response: " + result);
            }

            if (!checkNullOrBlank(result)) {
                if (!VALID_RESPONSE.equals(result)) {
                    callResult = false;
                }
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e);
            }

            callResult = false;
        }

        verifiedKey = callResult;

        return callResult;
    }

    /**
     * Generic call to Akismet
     *
     * @param function       Function used in constructing the URL to Akismet for the proper function to call. Either "comment-check", "submit-spam", or "submit-ham".
     * @param ipAddress      IP address of the comment submitter
     * @param userAgent      User agent information
     * @param referrer       The content of the HTTP_REFERER header should be sent here
     * @param permalink      The permanent location of the entry the comment was submitted to
     * @param commentType    May be blank, comment, trackback, pingback, or a made up value like "registration"
     * @param author         Submitted name with the comment
     * @param authorEmail    Submitted email address
     * @param authorURL      Commenter URL
     * @param commentContent The content that was submitted
     * @param other          In PHP there is an array of enviroment variables called $_SERVER which contains information about the web server itself as well as a key/value for every HTTP header sent with the request. This data is highly useful to Akismet as how the submited content interacts with the server can be very telling, so please include as much information as possible.
     * @return <code>true</code> if the comment is identified by Akismet as spam, <code>false</code> otherwise
     */
    protected boolean akismetCall(String function, String ipAddress, String userAgent, String referrer, String permalink, String commentType,
                                  String author, String authorEmail, String authorURL, String commentContent, Map other) {
        boolean callResult = false;

        String akismetURL = "http://" + apiKey + ".rest.akismet.com/1.1/" + function;

        PostMethod post = new PostMethod(akismetURL);
        post.addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        post.addParameter(new NameValuePair(API_PARAMETER_BLOG, blog));
        if (ipAddress != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_USER_IP, ipAddress));
        } else {
            post.addParameter(new NameValuePair(API_PARAMETER_USER_IP, ""));
        }
        if (userAgent != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_USER_AGENT, userAgent));
        }
        if (referrer != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_REFERRER, referrer));
        }
        if (permalink != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_PERMALINK, permalink));
        }
        if (commentType != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_COMMENT_TYPE, commentType));
        }
        if (author != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_COMMENT_AUTHOR, author));
        }
        if (authorEmail != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_COMMENT_AUTHOR_EMAIL, authorEmail));
        }
        if (authorURL != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_COMMENT_AUTHOR_URL, authorURL));
        }
        if (commentContent != null) {
            post.addParameter(new NameValuePair(API_PARAMETER_COMMENT_CONTENT, commentContent));
        }

        if (other != null && other.size() > 0) {
            Iterator keyIterator = other.keySet().iterator();
            while (keyIterator.hasNext()) {
                String key = (String) keyIterator.next();
                if ((key != null) && (other.get(key) != null)) {
                    post.addParameter(new NameValuePair(key, other.get(key).toString()));
                }
            }
        }

        try {
            httpResult = httpClient.executeMethod(post);
            String result = post.getResponseBodyAsString();

            if (logger.isDebugEnabled()) {
                logger.debug("Akismet response: " + result);
            }

            if (!checkNullOrBlank(result)) {
                result = result.trim();

                if (!FALSE_RESPONSE.equals(result)) {
                    callResult = true;
                }
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e);
            }

            callResult = true;
        }

        return callResult;
    }

    /**
     * From the API docs, This is basically the core of everything. This call takes a number of arguments and characteristics about the submitted content and then returns a thumbs up or thumbs down. Almost everything is optional, but performance can drop dramatically if you exclude certain elements. I would recommend erring on the side of too much data, as everything is used as part of the Akismet signature."
     *
     * @param ipAddress      IP address of the comment submitter
     * @param userAgent      User agent information
     * @param referrer       The content of the HTTP_REFERER header should be sent here
     * @param permalink      The permanent location of the entry the comment was submitted to
     * @param commentType    May be blank, comment, trackback, pingback, or a made up value like "registration"
     * @param author         Submitted name with the comment
     * @param authorEmail    Submitted email address
     * @param authorURL      Commenter URL
     * @param commentContent The content that was submitted
     * @param other          In PHP there is an array of enviroment variables called $_SERVER which contains information about the web server itself as well as a key/value for every HTTP header sent with the request. This data is highly useful to Akismet as how the submited content interacts with the server can be very telling, so please include as much information as possible.
     * @return <code>true</code> if the comment is identified by Akismet as spam, <code>false</code> otherwise
     */
    public boolean commentCheck(String ipAddress, String userAgent, String referrer, String permalink, String commentType,
                                String author, String authorEmail, String authorURL, String commentContent, Map other) {
        return akismetCall("comment-check", ipAddress, userAgent, referrer, permalink, commentType, author, authorEmail,
                authorURL, commentContent, other);
    }

    /**
     * From the API docs, This call is for submitting comments that weren't marked as spam but should have been. It takes identical arguments as comment check."
     *
     * @param ipAddress      IP address of the comment submitter
     * @param userAgent      User agent information
     * @param referrer       The content of the HTTP_REFERER header should be sent here
     * @param permalink      The permanent location of the entry the comment was submitted to
     * @param commentType    May be blank, comment, trackback, pingback, or a made up value like "registration"
     * @param author         Submitted name with the comment
     * @param authorEmail    Submitted email address
     * @param authorURL      Commenter URL
     * @param commentContent The content that was submitted
     * @param other          In PHP there is an array of enviroment variables called $_SERVER which contains information about the web server itself as well as a key/value for every HTTP header sent with the request. This data is highly useful to Akismet as how the submited content interacts with the server can be very telling, so please include as much information as possible.
     * @return <code>true</code> if the comment is identified by Akismet as spam, <code>false</code> otherwise
     */
    public void submitSpam(String ipAddress, String userAgent, String referrer, String permalink, String commentType,
                           String author, String authorEmail, String authorURL, String commentContent, Map other) {
        akismetCall("submit-spam", ipAddress, userAgent, referrer, permalink, commentType, author, authorEmail,
                authorURL, commentContent, other);
    }

    /**
     * From the API docs, This call is intended for the marking of false positives, things that were incorrectly marked as spam. It takes identical arguments as comment check and submit spam."
     *
     * @param ipAddress      IP address of the comment submitter
     * @param userAgent      User agent information
     * @param referrer       The content of the HTTP_REFERER header should be sent here
     * @param permalink      The permanent location of the entry the comment was submitted to
     * @param commentType    May be blank, comment, trackback, pingback, or a made up value like "registration"
     * @param author         Submitted name with the comment
     * @param authorEmail    Submitted email address
     * @param authorURL      Commenter URL
     * @param commentContent The content that was submitted
     * @param other          In PHP there is an array of enviroment variables called $_SERVER which contains information about the web server itself as well as a key/value for every HTTP header sent with the request. This data is highly useful to Akismet as how the submited content interacts with the server can be very telling, so please include as much information as possible.
     * @return <code>true</code> if the comment is identified by Akismet as spam, <code>false</code> otherwise
     */
    public void submitHam(String ipAddress, String userAgent, String referrer, String permalink, String commentType,
                          String author, String authorEmail, String authorURL, String commentContent, Map other) {
        akismetCall("submit-ham", ipAddress, userAgent, referrer, permalink, commentType, author, authorEmail,
                authorURL, commentContent, other);
    }
}

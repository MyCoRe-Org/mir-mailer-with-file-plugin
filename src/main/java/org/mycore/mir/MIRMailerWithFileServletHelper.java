/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mir;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for handling MIR mailer servlet requests and form data.
 */
public final class MIRMailerWithFileServletHelper {

    private static final Set<String> SENSITIVE_HEADERS = Set.of("cookie", "authorization");

    private MIRMailerWithFileServletHelper() {}

    /**
     * Builds a redirect URL that reports an error and carries the given form fields as query parameters.
     * Parameters of a previous attempt are removed from the base URL. Fields are only added as long as the
     * URL does not exceed the given maximum length.
     *
     * @param baseUrl the URL to redirect to
     * @param errorParam the name of the error parameter
     * @param errorCode the error code
     * @param fields a map of field names and values
     * @param maxLength the maximum length of the resulting URL
     * @return the redirect URL
     */
    public static String buildErrorRedirectUrl(String baseUrl, String errorParam, String errorCode,
        Map<String, String> fields, int maxLength) {
        final String base = removeQueryParams(baseUrl, errorParam, fields.keySet());
        final StringBuilder url = new StringBuilder(base).append(base.contains("?") ? '&' : '?')
            .append(encodeUriComponent(errorParam)).append('=').append(encodeUriComponent(errorCode));
        fields.forEach((key, value) -> {
            if (value != null) {
                final String param = '&' + encodeUriComponent(key) + '=' + encodeUriComponent(value);
                if (url.length() + param.length() <= maxLength) {
                    url.append(param);
                }
            }
        });
        return url.toString();
    }

    /**
     * Checks whether the domain of the given mail address is one of the given domains or a subdomain of it.
     *
     * @param email the mail address
     * @param domains the disallowed domains in lower case
     * @return true if the domain is disallowed
     */
    public static boolean isDisallowedDomain(String email, Set<String> domains) {
        String address;
        try {
            address = new InternetAddress(email.trim(), true).getAddress();
        } catch (AddressException e) {
            address = email.trim();
        }
        String domain = address.substring(address.lastIndexOf('@') + 1).trim().toLowerCase(Locale.ROOT);
        while (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        final String normalizedDomain = domain;
        return domains.stream().anyMatch(d -> normalizedDomain.equals(d) || normalizedDomain.endsWith("." + d));
    }

    /**
     * Checks whether the request is a multipart request whose body could not be parsed, for example because it
     * exceeds the configured upload size limits.
     *
     * @param request the HTTP request
     * @return true if the multipart body could not be parsed
     * @throws IOException if reading the request fails
     * @throws ServletException if the request is not a multipart request
     */
    public static boolean hasMultipartError(HttpServletRequest request) throws IOException, ServletException {
        final String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            return false;
        }
        try {
            request.getParts();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    private static String removeQueryParams(String url, String errorParam, Set<String> names) {
        final int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return url;
        }
        final List<String> kept = new ArrayList<>();
        for (String param : url.substring(queryStart + 1).split("&")) {
            if (param.isEmpty()) {
                continue;
            }
            final String name = URLDecoder.decode(param.split("=", 2)[0], UTF_8);
            if (errorParam.equals(name)) {
                break;
            }
            if (!names.contains(name)) {
                kept.add(param);
            }
        }
        final String query = String.join("&", kept);
        final String path = url.substring(0, queryStart);
        return query.isEmpty() ? path : path + '?' + query;
    }

    /**
     * Encodes a string for use in a URI component according to RFC 3986.
     *
     * @param s the string to encode
     * @return the encoded string
     */
    public static String encodeUriComponent(String s) {
        return URLEncoder.encode(s, UTF_8)
            .replaceAll("\\+", "%20")
            .replaceAll("\\%21", "!")
            .replaceAll("\\%27", "'")
            .replaceAll("\\%28", "(")
            .replaceAll("\\%29", ")")
            .replaceAll("\\%7E", "~");
    }

    /**
     * Builds a detailed log message for the given HTTP request, including method, URI, headers, and parameters.
     *
     * @param request the HTTP request
     * @return a formatted string describing the request
     */
    public static String buildRequestLogMessage(HttpServletRequest request) {
        final StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP REQUEST - ").append("Method: ").append(request.getMethod()).append(", URI: ")
            .append(request.getRequestURI());

        if (request.getQueryString() != null) {
            logMessage.append('?').append(request.getQueryString());
        }

        logMessage.append("\nHeaders:");
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String header = headerNames.nextElement();
            final String value =
                SENSITIVE_HEADERS.contains(header.toLowerCase(Locale.ROOT)) ? "***" : request.getHeader(header);
            logMessage.append("\n  ").append(header).append(": ").append(value);
        }

        logMessage.append("\nParameters:");
        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            final String param = paramNames.nextElement();
            logMessage.append("\n  ").append(param).append(" = ").append(request.getParameter(param));
        }
        return logMessage.toString();
    }
}

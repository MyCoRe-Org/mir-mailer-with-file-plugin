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

import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for handling MIR mailer servlet requests and form data.
 */
public final class MIRMailerWithFileServletHelper {

    private MIRMailerWithFileServletHelper() {}

    /**
     * Converts a map of parameters into a URL query string
     *
     * @param params a map of parameter names and values
     * @return a URL-encoded query string
     */
    public static String getUrlParams(Map<String, String> params) {
        final StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (value != null) {
                sb.append('&').append(encodeUriComponent(key)).append('=').append(encodeUriComponent(value));
            }
        });
        return sb.toString();
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
            logMessage.append("\n  ").append(header).append(": ").append(request.getHeader(header));
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

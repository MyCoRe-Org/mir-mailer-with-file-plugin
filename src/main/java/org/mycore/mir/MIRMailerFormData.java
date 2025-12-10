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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Model representing the data submitted via a MIR mailer form.
 */
public class MIRMailerFormData {

    private static final Set<String> EXCLUDED_PARAMS = Set.of(
        MIRMailerFormDataConstants.PARAM_CAPTCHA,
        MIRMailerFormDataConstants.PARAM_COPY
    );

    private final String action;

    private final String captcha;

    private final String senderName;

    private final String senderEmail;

    private final boolean sendCopy;

    private final Map<String, String> fields;

    /**
     * Constructs a new {@code MIRMailerFormData}.
     *
     * @param action the form action
     * @param captcha the captcha value
     * @param name the sender's name
     * @param email the sender's email
     * @param sendCopy whether a copy should be sent to the sender
     * @param fields a map of all other form fields
     */
    public MIRMailerFormData(String action, String captcha, String name,
        String email, boolean sendCopy, Map<String, String> fields) {
        this.action = action;
        this.captcha = captcha;
        this.senderName = name;
        this.senderEmail = email;
        this.sendCopy = sendCopy;
        this.fields = Map.copyOf(fields);
    }

    /**
     * Creates a {@code MIRMailerFormData} from an {@link HttpServletRequest}.
     * <p>
     * Excluded parameters (file, captcha, copy) are not included in the general fields map.
     * </p>
     *
     * @param request the HTTP request containing the form parameters
     * @return a new {@code MIRMailerFormData} populated from the request
     */
    public static MIRMailerFormData fromRequest(HttpServletRequest request) {
        final Map<String, String> data = request.getParameterMap().entrySet().stream()
            .filter(e -> !EXCLUDED_PARAMS.contains(e.getKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().length > 0 ? String.join(",", e.getValue()) : "",
                (v1, v2) -> v1,
                LinkedHashMap::new
            ));
        final String action = request.getParameter(MIRMailerFormDataConstants.PARAM_ACTION);
        final String captcha = request.getParameter(MIRMailerFormDataConstants.PARAM_CAPTCHA);
        final String name = request.getParameter(MIRMailerFormDataConstants.PARAM_SENDER_NAME);
        final String email = request.getParameter(MIRMailerFormDataConstants.PARAM_SENDER_EMAIL);
        final boolean sendCopy =
            Optional.ofNullable(request.getParameter(MIRMailerFormDataConstants.PARAM_COPY)).map(Boolean::valueOf)
                .orElse(false);
        return new MIRMailerFormData(action, captcha, name, email, sendCopy, data);
    }

    /**
     * Returns the form action.
     *
     * @return the form action
     */
    public String getAction() {
        return action;
    }

    /**
     * Returns the submitted captcha value.
     *
     * @return the captcha value
     */
    public String getCaptcha() {
        return captcha;
    }

    /**
     * Returns the sender's name.
     *
     * @return the sender's name
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Returns the sender's email address.
     *
     * @return the sender's email address
     */
    public String getSenderEmail() {
        return senderEmail;
    }

    /**
     * Returns whether a copy should be sent to the sender.
     *
     * @return {@code true} if a copy should be sent, otherwise {@code false}
     */
    public boolean isSendCopy() {
        return sendCopy;
    }

    /**
     * Returns a map of all additional form fields.
     *
     * @return an unmodifiable map of additional form fields
     */
    public Map<String, String> getFields() {
        return fields;
    }
}

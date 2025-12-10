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

/**
 * Default constants used as parameter names in the MIR mailer form.
 */
public final class MIRMailerFormDataConstants {

    /**
     * Parameter name for the captcha field.
     */
    public static final String PARAM_CAPTCHA = "captcha";

    /**
     * Parameter name for the uploaded file.
     */
    public static final String PARAM_FILE = "file";

    /**
     * Parameter name for the sender's name.
     */
    public static final String PARAM_SENDER_NAME = "name";

    /**
     * Parameter name for the sender's email.
     */
    public static final String PARAM_SENDER_EMAIL = "mail";

    /**
     * Parameter name indicating if a copy should be sent to the sender.
     */
    public static final String PARAM_COPY = "copy";

    /**
     * Parameter name for the form action.
     */
    public static final String PARAM_ACTION = "action";

    private MIRMailerFormDataConstants() { }
}

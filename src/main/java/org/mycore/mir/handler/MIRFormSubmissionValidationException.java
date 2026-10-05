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

package org.mycore.mir.handler;

import java.io.Serial;

/**
 * Exception thrown by implementations of {@link MIRFormSubmissionHandler} when a form submission is invalid,
 * e.g. because of missing fields or invalid attachments.
 */
public class MIRFormSubmissionValidationException extends MIRFormSubmissionHandlerException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    /**
     * Constructs a new {@code MIRFormSubmissionValidationException} with the specified error code and message.
     *
     * @param errorCode a short code identifying the kind of validation error
     * @param message a descriptive message explaining the reason for the exception
     */
    public MIRFormSubmissionValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new {@code MIRFormSubmissionValidationException} with the specified error code, message and cause.
     *
     * @param errorCode a short code identifying the kind of validation error
     * @param message a descriptive message explaining the reason for the exception
     * @param cause the underlying cause of the exception
     */
    public MIRFormSubmissionValidationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}

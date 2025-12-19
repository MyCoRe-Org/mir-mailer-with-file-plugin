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

import org.mycore.common.MCRException;

/**
 * Exception thrown when an error occurs while rendering the body of a mail
 * for a form submission.
 * <p>
 * Implementations of {@link org.mycore.mir.handler.MIRMailBodyRenderer}
 * may throw this exception in cases such as:
 * <ul>
 *     <li>Template parsing or evaluation errors</li>
 *     <li>Missing or invalid data in the form submission</li>
 *     <li>Other failures preventing the mail body from being generated</li>
 * </ul>
 * <p>
 * This exception is a runtime exception and extends {@link MCRException}.
 */
public class MIRMailBodyRenderingException extends MCRException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MIRMailBodyRenderingException} with the specified detail message.
     *
     * @param message a descriptive message explaining the reason for the exception
     */
    public MIRMailBodyRenderingException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MIRMailBodyRenderingException} with the specified
     * detail message and cause.
     *
     * @param message a descriptive message explaining the reason for the exception
     * @param cause the underlying cause of the exception (can be {@code null})
     */
    public MIRMailBodyRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}

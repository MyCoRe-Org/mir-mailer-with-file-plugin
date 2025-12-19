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

/**
 * Renders a mail body based on a form submission.
 * <p>
 * Implementations may use different rendering strategies such as
 * string templates, XSLT, or template engines.
 */
public interface MIRMailBodyRenderer {

    /**
     * Renders the mail body using the given form submission data.
     *
     * @param request the form submission containing all data required to render the mail body
     * @return the rendered mail body as a string
     * @throws MIRMailBodyRenderingException if rendering fails
     */
    String render(MIRFormSubmissionRequest request);
}

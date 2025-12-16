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

import java.util.Map;

/**
 * Interface for generating a mail body based on a set of provided form data.
 */
public interface MIRMailBodyGenerator {

    /**
     * Generates the mail body using the supplied form data.
     *
     * @param formData a map containing the key–value pairs required to build the mail body
     * @return the generated mail body as a string
     * @throws IllegalArgumentException if an error occurs during mail body generation
     */
    String generateBody(Map<String, String> formData) throws IllegalArgumentException;
}

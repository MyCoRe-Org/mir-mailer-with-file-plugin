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
 * Interface for handling form data.
 */
public interface MIRFormHandler {

    /**
     * Checks whether all required fields are present and non-blank in the given form data.
     *
     * @param formData a map containing form field names and their corresponding values
     * @return {@code true} if all required fields are present and not blank; {@code false} otherwise
     */
    boolean checkFormData(Map<String,String> formData);
}

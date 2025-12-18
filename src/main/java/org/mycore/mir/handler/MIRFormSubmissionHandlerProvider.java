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

import org.mycore.common.config.MCRConfiguration2;

/**
 * Provider for {@link MIRFormSubmissionHandler} instances.
 */
@SuppressWarnings("PMD.ClassNamingConventions")
public final class MIRFormSubmissionHandlerProvider {

    private static final String HANDLER_PROPERTY_PREFIX = "MIR.FormSubmissionHandler.";

    private MIRFormSubmissionHandlerProvider() { }

    /**
     * Returns an instance of {@link MIRFormSubmissionHandler} for the given ID.
     * <p>
     * The concrete class must be defined in the MyCoRe configuration under
     * {@code MIR.FormSubmissionHandler.<id>.Class}.
     *
     * @param id the unique identifier of the handler in the configuration
     * @return the configured {@link MIRFormSubmissionHandler} instance
     * @throws org.mycore.common.config.MCRConfigurationException if no handler is configured for the given ID
     */
    public static MIRFormSubmissionHandler obtainInstance(String id) {
        final String property = HANDLER_PROPERTY_PREFIX + id + ".Class";
        return (MIRFormSubmissionHandler) MCRConfiguration2.getSingleInstanceOf(property)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }
}

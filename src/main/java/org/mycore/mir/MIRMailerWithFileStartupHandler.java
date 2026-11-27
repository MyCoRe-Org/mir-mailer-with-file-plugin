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

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRStartupHandler;

/**
 * Registers the {@link MIRMailerWithFileServlet} with a multipart configuration that is read from
 * {@code MIR.MailerWithFileServlet.MaxFileSize} and {@code MIR.MailerWithFileServlet.MaxRequestSize}.
 * A value of {@code -1} means unlimited.
 */
public class MIRMailerWithFileStartupHandler implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROPERTY_PREFIX = "MIR.MailerWithFileServlet.";

    private static final String SERVLET_NAME = "MIRMailerWithFileServlet";

    private static final String SERVLET_MAPPING = "/servlets/MIRMailerWithFile";

    @Override
    public String getName() {
        return MIRMailerWithFileStartupHandler.class.getName();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext == null) {
            return;
        }
        final ServletRegistration.Dynamic registration =
            servletContext.addServlet(SERVLET_NAME, MIRMailerWithFileServlet.class.getName());
        if (registration == null) {
            LOGGER.error("Servlet {} is already registered, it needs its own multipart configuration", SERVLET_NAME);
            return;
        }
        registration.addMapping(SERVLET_MAPPING);
        final long maxFileSize = getLimit("MaxFileSize");
        final long maxRequestSize = getLimit("MaxRequestSize");
        registration.setMultipartConfig(new MultipartConfigElement("", maxFileSize, maxRequestSize, 0));
        LOGGER.info("Registered {} at {} (max file size: {}, max request size: {})", SERVLET_NAME,
            SERVLET_MAPPING, maxFileSize, maxRequestSize);
    }

    private static long getLimit(String name) {
        final String property = PROPERTY_PREFIX + name;
        return parseLimit(property, MCRConfiguration2.getString(property).orElse(null));
    }

    static long parseLimit(String property, String value) {
        if (value == null || value.isBlank()) {
            return -1L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid value '{}' for {}, the upload size is not limited", value, property);
            return -1L;
        }
    }
}

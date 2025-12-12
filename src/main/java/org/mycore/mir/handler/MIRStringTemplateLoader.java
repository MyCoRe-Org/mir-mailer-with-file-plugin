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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Loads a string template from a classpath resource.
 */
@MCRConfigurationProxy(proxyClass = MIRStringTemplateLoader.Factory.class)
public class MIRStringTemplateLoader {

    private final String resourcePath;

    /**
     * Creates a new loader for the given resource path.
     *
     * @param resourcePath the path to the resource, must not be null
     */
    public MIRStringTemplateLoader(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * Loads the resource as a UTF-8 string.
     *
     * @return the content of the resource
     * @throws IOException if the resource cannot be found or read
     */
    public String load() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Factory class for creating {@link MIRStringTemplateLoader} instances from configuration.
     */
    public static final class Factory implements Supplier<MIRStringTemplateLoader> {

        /**
         * The path to the template resource.
         */
        @MCRProperty(name = "File")
        public String file;

        @Override
        public MIRStringTemplateLoader get() {
            return new MIRStringTemplateLoader(file);
        }
    }
}

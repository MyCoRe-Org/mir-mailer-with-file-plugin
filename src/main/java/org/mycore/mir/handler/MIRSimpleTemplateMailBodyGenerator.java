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

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;

/**
 * Simple implementation of {@link MIRMailBodyGenerator} that generates a mail body from a string template.
 */
@MCRConfigurationProxy(proxyClass = MIRSimpleTemplateMailBodyGenerator.Factory.class)
public class MIRSimpleTemplateMailBodyGenerator implements MIRMailBodyGenerator {

    private final MIRStringTemplateLoader templateLoader;

    /**
     * Creates a new body generator with the given template loader.
     *
     * @param templateLoader the template loader to use
     */
    public MIRSimpleTemplateMailBodyGenerator(MIRStringTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }

    @Override
    public String generateBody(Map<String, String> formData) {
        try {
            final Pattern pattern = Pattern.compile("\\{\\{(.+?)\\}\\}");
            final Matcher matcher = pattern.matcher(templateLoader.load());
            final StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String key = matcher.group(1);
                String replacement = formData.getOrDefault(key, "");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to generate body.", e);
        }
    }

    /**
     * Factory class for creating {@link MIRSimpleTemplateMailBodyGenerator} instances from configuration.
     */
    public static final class Factory implements Supplier<MIRSimpleTemplateMailBodyGenerator> {

        /**
         * The template loader to use.
         */
        @MCRInstance(name = "TemplateLoader", valueClass = MIRStringTemplateLoader.class)
        public MIRStringTemplateLoader templateLoader;

        @Override
        public MIRSimpleTemplateMailBodyGenerator get() {
            return new MIRSimpleTemplateMailBodyGenerator(templateLoader);
        }
    }
}

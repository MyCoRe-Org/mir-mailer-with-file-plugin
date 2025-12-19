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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Implementation of {@link MIRMailBodyRenderer} that renders a mail body from a string template.
 */
@MCRConfigurationProxy(proxyClass = MIRStringTemplateMailBodyRenderer.Factory.class)
public class MIRStringTemplateMailBodyRenderer implements MIRMailBodyRenderer {

    private static final Pattern PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    private final String template;

    /**
     * Creates a new body renderer with the given template.
     *
     * @param template the template to use
     */
    public MIRStringTemplateMailBodyRenderer(String template) {
        this.template = template;
    }

    @Override
    public String render(MIRFormSubmissionRequest request) {
        final Matcher matcher = PATTERN.matcher(template);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = request.fields().getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Factory class for creating {@link MIRStringTemplateMailBodyRenderer} instances from configuration.
     */
    public static final class Factory implements Supplier<MIRStringTemplateMailBodyRenderer> {

        /**
         * The template path to use.
         */
        @MCRProperty(name = "TemplatePath")
        public String templatePath;

        @Override
        public MIRStringTemplateMailBodyRenderer get() {
            try (InputStream in = getClass().getResourceAsStream(templatePath)) {
                if (in == null) {
                    throw new MCRConfigurationException("Template not found: " + templatePath);
                }
                String template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return new MIRStringTemplateMailBodyRenderer(template);
            } catch (IOException e) {
                throw new MCRConfigurationException("Error instantiating MIRStringTemplateMailBodyRenderer", e);
            }
        }
    }
}

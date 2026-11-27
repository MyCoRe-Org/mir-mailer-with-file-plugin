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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MIRStringTemplateMailBodyRendererTest {

    @Test
    void renderReplacesPlaceholders() {
        final MIRStringTemplateMailBodyRenderer renderer =
            new MIRStringTemplateMailBodyRenderer("Name: {{name}}, Mail: {{mail}}");
        final String body = renderer.render(
            new MIRFormSubmissionRequest(Map.of("name", "Jane", "mail", "jane@example.org"), List.of()));
        assertEquals("Name: Jane, Mail: jane@example.org", body);
    }

    @Test
    void renderUsesEmptyStringForMissingFields() {
        final MIRStringTemplateMailBodyRenderer renderer = new MIRStringTemplateMailBodyRenderer("[{{missing}}]");
        assertEquals("[]", renderer.render(new MIRFormSubmissionRequest(Map.of(), List.of())));
    }

    @Test
    void renderKeepsSpecialCharactersOfValues() {
        final MIRStringTemplateMailBodyRenderer renderer = new MIRStringTemplateMailBodyRenderer("{{comment}}");
        final String body = renderer.render(
            new MIRFormSubmissionRequest(Map.of("comment", "costs $5 \\ {{name}}"), List.of()));
        assertEquals("costs $5 \\ {{name}}", body);
    }
}

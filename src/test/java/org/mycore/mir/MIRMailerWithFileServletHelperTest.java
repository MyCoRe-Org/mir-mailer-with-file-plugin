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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class MIRMailerWithFileServletHelperTest {

    @Test
    void buildErrorRedirectUrlAddsErrorAndFields() {
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", "Jane Doe");
        fields.put("mail", "jane@example.org");
        final String url = MIRMailerWithFileServletHelper.buildErrorRedirectUrl("https://host/form.xml", "error",
            "captcha", fields, 4000);
        assertEquals("https://host/form.xml?error=captcha&name=Jane%20Doe&mail=jane%40example.org", url);
    }

    @Test
    void buildErrorRedirectUrlReplacesParametersOfPreviousAttempt() {
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", "New");
        final String url = MIRMailerWithFileServletHelper.buildErrorRedirectUrl(
            "https://host/form.xml?lang=de&error=captcha&name=Old", "error", "mail", fields, 4000);
        assertEquals("https://host/form.xml?lang=de&error=mail&name=New", url);
    }

    @Test
    void buildErrorRedirectUrlSkipsFieldsExceedingMaxLength() {
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("abstract", "x".repeat(5000));
        fields.put("comment", "short");
        final String url = MIRMailerWithFileServletHelper.buildErrorRedirectUrl("https://host/form.xml", "error",
            "mail", fields, 4000);
        assertEquals("https://host/form.xml?error=mail&comment=short", url);
        assertTrue(url.length() <= 4000);
    }

    @Test
    void buildErrorRedirectUrlIgnoresNullValues() {
        final Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", null);
        final String url = MIRMailerWithFileServletHelper.buildErrorRedirectUrl("https://host/form.xml", "error",
            "mail", fields, 4000);
        assertEquals("https://host/form.xml?error=mail", url);
    }

    @Test
    void encodeUriComponentEncodesLikeJavaScript() {
        assertEquals("a%20b%26c!'()~", MIRMailerWithFileServletHelper.encodeUriComponent("a b&c!'()~"));
    }

    @Test
    void isDisallowedDomainMatchesDomainAndSubdomains() {
        final Set<String> domains = Set.of("example.com");
        assertTrue(MIRMailerWithFileServletHelper.isDisallowedDomain("a@example.com", domains));
        assertTrue(MIRMailerWithFileServletHelper.isDisallowedDomain("a@Mail.Example.COM ", domains));
        assertFalse(MIRMailerWithFileServletHelper.isDisallowedDomain("a@notexample.com", domains));
        assertFalse(MIRMailerWithFileServletHelper.isDisallowedDomain("a@example.org", domains));
    }
}

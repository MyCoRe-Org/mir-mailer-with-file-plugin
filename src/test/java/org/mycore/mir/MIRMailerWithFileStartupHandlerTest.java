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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import jakarta.servlet.ServletContext;

import org.junit.jupiter.api.Test;

class MIRMailerWithFileStartupHandlerTest {

    @Test
    void parseLimitAcceptsNumbers() {
        assertEquals(1024L, MIRMailerWithFileStartupHandler.parseLimit("p", "1024"));
        assertEquals(1024L, MIRMailerWithFileStartupHandler.parseLimit("p", " 1024 "));
        assertEquals(-1L, MIRMailerWithFileStartupHandler.parseLimit("p", "-1"));
    }

    @Test
    void parseLimitFallsBackToUnlimited() {
        assertEquals(-1L, MIRMailerWithFileStartupHandler.parseLimit("p", null));
        assertEquals(-1L, MIRMailerWithFileStartupHandler.parseLimit("p", " "));
        assertEquals(-1L, MIRMailerWithFileStartupHandler.parseLimit("p", "10MB"));
    }

    @Test
    void startUpIgnoresMissingServletContext() {
        assertDoesNotThrow(() -> new MIRMailerWithFileStartupHandler().startUp(null));
    }

    @Test
    void startUpHandlesAlreadyRegisteredServlet() {
        final ServletContext context = (ServletContext) Proxy.newProxyInstance(
            ServletContext.class.getClassLoader(), new Class<?>[] { ServletContext.class },
            (proxy, method, args) -> null);
        assertDoesNotThrow(() -> new MIRMailerWithFileStartupHandler().startUp(context));
    }
}

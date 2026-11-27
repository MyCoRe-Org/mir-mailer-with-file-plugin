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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MIRCaptchaHelperTest {

    @Test
    void generateCaptchaTextContainsOnlyDigits() {
        final String text = MIRCaptchaHelper.generateCaptchaText();
        assertFalse(text.isEmpty());
        assertTrue(text.chars().allMatch(Character::isDigit));
    }

    @Test
    void createAudioCaptchaAcceptsDigits() {
        assertNotNull(MIRCaptchaHelper.createAudioCaptcha("12345"));
    }

    @Test
    void createAudioCaptchaRejectsNonDigits() {
        assertThrows(IllegalArgumentException.class, () -> MIRCaptchaHelper.createAudioCaptcha("12a45"));
    }

    @Test
    void createImageCaptchaUsesGivenSize() {
        final var image = MIRCaptchaHelper.createImageCaptcha("12345", 150, 50).getImage();
        assertTrue(image.getWidth() == 150 && image.getHeight() == 50);
    }
}

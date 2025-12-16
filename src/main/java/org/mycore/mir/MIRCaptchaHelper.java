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

import net.logicsquad.nanocaptcha.audio.AudioCaptcha;
import net.logicsquad.nanocaptcha.content.ContentProducer;
import net.logicsquad.nanocaptcha.content.NumbersContentProducer;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;

/**
 * Helper class for generating Image and Audio CAPTCHAs using NanoCaptcha.
 */
public class MIRCaptchaHelper {

    /**
     * Creates an ImageCaptcha with fixed text.
     *
     * @param text the CAPTCHA text to display
     * @param width the width of the CAPTCHA image in pixels
     * @param height the height of the CAPTCHA image in pixels
     * @return an {@link ImageCaptcha} object containing the generated image
     */
    public static ImageCaptcha createImageCaptcha(String text, int width, int height) {
        return new ImageCaptcha.Builder(width, height)
            .addContent(new FixedContentProducer(text))
            .addNoise()
            .addBackground()
            .build();
    }

    /**
     * Creates an AudioCaptcha with the specified text.
     * <p>
     * The text must contain only digits (0-9). This is required because
     * the underlying AudioCaptcha voice producer can only vocalize numbers.
     *
     * @param text the numeric CAPTCHA text to be spoken in the audio
     * @return an {@link AudioCaptcha} object containing the generated audio
     * @throws IllegalArgumentException if the text contains any non-digit characters
     */
    public static AudioCaptcha createAudioCaptcha(String text) {
        if (!text.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("text must contain digits");
        }
        return new AudioCaptcha.Builder()
            .addContent(new FixedContentProducer(text))
            .build();
    }

    /**
     * Generates a random CAPTCHA text string only containing numbers.
     *
     * @return a randomly generated CAPTCHA string
     */
    public static String generateCaptchaText() {
        return new NumbersContentProducer().getContent();
    }

    /**
     * FixedContentProducer provides a fixed CAPTCHA text.
     *
     * @param text the text
     */
    record FixedContentProducer(String text) implements ContentProducer {

        @Override
        public String getContent() {
            return text;
        }
    }
}

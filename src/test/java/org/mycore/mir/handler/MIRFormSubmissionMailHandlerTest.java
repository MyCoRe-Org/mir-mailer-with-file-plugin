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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MIRFormSubmissionMailHandlerTest {

    private static MIRFormSubmissionMailHandler handler(List<String> requiredFields,
        MIRFormSubmissionMailHandler.AttachmentConfig attachmentConfig, boolean allowCopy) {
        return new MIRFormSubmissionMailHandler(new MIRFormSubmissionMailHandler.FormSubmissionHandlerConfig(
            "sender@example.org", List.of("recipient@example.org"), "Subject", request -> "body", requiredFields,
            attachmentConfig, allowCopy));
    }

    private static MIRFormSubmissionMailHandler.AttachmentConfig noAttachments() {
        return new MIRFormSubmissionMailHandler.AttachmentConfig(null, null, null, null, null);
    }

    private static MIRInboundAttachment attachment(String name, long size) {
        return new MIRInboundAttachment() {
            @Override
            public String filename() {
                return name;
            }

            @Override
            public long size() {
                return size;
            }

            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(new byte[0]);
            }
        };
    }

    private static String errorCode(MIRFormSubmissionMailHandler handler, Map<String, String> fields,
        List<MIRInboundAttachment> attachments) {
        return assertThrows(MIRFormSubmissionValidationException.class,
            () -> handler.handle(new MIRFormSubmissionRequest(fields, attachments))).getErrorCode();
    }

    @Test
    void missingRequiredFieldIsRejected() {
        assertEquals("field", errorCode(handler(List.of("name"), noAttachments(), false),
            Map.of("mail", "jane@example.org"), List.of()));
    }

    @Test
    void blankRequiredFieldIsRejected() {
        assertEquals("field", errorCode(handler(List.of("name"), noAttachments(), false),
            Map.of("name", " ", "mail", "jane@example.org"), List.of()));
    }

    @Test
    void attachmentsAreRejectedWithoutUploadPath() {
        assertEquals("attachment", errorCode(handler(List.of(), noAttachments(), false),
            Map.of("mail", "jane@example.org"), List.of(attachment("a.pdf", 10))));
    }

    @Test
    void tooManyAttachmentsAreRejected() {
        final var config = new MIRFormSubmissionMailHandler.AttachmentConfig("/tmp", null, 1, null, null);
        assertEquals("attachment", errorCode(handler(List.of(), config, false),
            Map.of("mail", "jane@example.org"), List.of(attachment("a.pdf", 10), attachment("b.pdf", 10))));
    }

    @Test
    void tooFewAttachmentsAreRejected() {
        final var config = new MIRFormSubmissionMailHandler.AttachmentConfig("/tmp", 1, null, null, null);
        assertEquals("attachment", errorCode(handler(List.of(), config, false),
            Map.of("mail", "jane@example.org"), List.of()));
    }

    @Test
    void tooLargeAttachmentIsRejected() {
        final var config = new MIRFormSubmissionMailHandler.AttachmentConfig("/tmp", null, null, 100L, null);
        assertEquals("attachment", errorCode(handler(List.of(), config, false),
            Map.of("mail", "jane@example.org"), List.of(attachment("a.pdf", 101))));
    }

    @Test
    void tooLargeTotalSizeIsRejected() {
        final var config = new MIRFormSubmissionMailHandler.AttachmentConfig("/tmp", null, null, 100L, 150L);
        assertEquals("attachment", errorCode(handler(List.of(), config, false),
            Map.of("mail", "jane@example.org"), List.of(attachment("a.pdf", 100), attachment("b.pdf", 100))));
    }

    @Test
    void invalidSenderAddressIsRejected() {
        assertEquals("mail", errorCode(handler(List.of(), noAttachments(), false),
            Map.of("mail", "not an address"), List.of()));
    }

    @Test
    void copyWithoutSenderIsRejected() {
        assertEquals("mail", errorCode(handler(List.of(), noAttachments(), true),
            Map.of("copy", "on"), List.of()));
    }
}

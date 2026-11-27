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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
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

    private static MIRFormSubmissionMailHandler handler(boolean allowCopy, MIRMailBodyRenderer renderer,
        MIRFormSubmissionMailHandler.MailTransport transport) {
        return new MIRFormSubmissionMailHandler(new MIRFormSubmissionMailHandler.FormSubmissionHandlerConfig(
            "sender@example.org", List.of("recipient@example.org"), "Subject", renderer, List.of(),
            noAttachments(), allowCopy), transport);
    }

    @Test
    void mailIsSentToRecipientsWithSenderAsReplyTo() {
        final List<MIRFormSubmissionMailHandler.OutgoingMail> sent = new ArrayList<>();
        handler(false, request -> "body", sent::add).handle(
            new MIRFormSubmissionRequest(Map.of("name", "Jane", "mail", "jane@example.org"), List.of()));
        assertEquals(1, sent.size());
        assertEquals(List.of("recipient@example.org"), sent.get(0).to());
        assertEquals(List.of("Jane <jane@example.org>"), sent.get(0).replyTo());
        assertEquals("body", sent.get(0).body());
    }

    @Test
    void copyIsSentToSender() {
        final List<MIRFormSubmissionMailHandler.OutgoingMail> sent = new ArrayList<>();
        handler(true, request -> "body", sent::add).handle(
            new MIRFormSubmissionRequest(Map.of("mail", "jane@example.org", "copy", "on"), List.of()));
        assertEquals(2, sent.size());
        assertEquals(List.of("jane@example.org"), sent.get(1).to());
    }

    @Test
    void failedCopyDoesNotFailSubmission() {
        final List<MIRFormSubmissionMailHandler.OutgoingMail> sent = new ArrayList<>();
        final MIRFormSubmissionMailHandler handler = handler(true, request -> "body", mail -> {
            if (mail.to().contains("jane@example.org")) {
                throw new IllegalStateException("rejected");
            }
            sent.add(mail);
        });
        assertDoesNotThrow(() -> handler.handle(
            new MIRFormSubmissionRequest(Map.of("mail", "jane@example.org", "copy", "on"), List.of())));
        assertEquals(1, sent.size());
    }

    @Test
    void failedMailFailsSubmission() {
        final MIRFormSubmissionMailHandler handler = handler(false, request -> "body", mail -> {
            throw new IllegalStateException("smtp down");
        });
        final MIRFormSubmissionHandlerException e = assertThrows(MIRFormSubmissionHandlerException.class,
            () -> handler.handle(new MIRFormSubmissionRequest(Map.of("mail", "jane@example.org"), List.of())));
        assertFalse(e instanceof MIRFormSubmissionValidationException);
    }

    @Test
    void renderingErrorIsReportedAsHandlerException() {
        final MIRFormSubmissionMailHandler handler = handler(false, request -> {
            throw new MIRMailBodyRenderingException("broken template");
        }, mail -> {
        });
        final MIRFormSubmissionHandlerException e = assertThrows(MIRFormSubmissionHandlerException.class,
            () -> handler.handle(new MIRFormSubmissionRequest(Map.of("mail", "jane@example.org"), List.of())));
        assertEquals(MIRMailBodyRenderingException.class, e.getCause().getClass());
    }

    @Test
    void invalidXmlCharactersAreRemovedFromMail() {
        final List<MIRFormSubmissionMailHandler.OutgoingMail> sent = new ArrayList<>();
        handler(false, request -> "line\u000Bbreak\ttab\nnew", sent::add).handle(
            new MIRFormSubmissionRequest(Map.of("name", "Ja\u0001ne", "mail", "jane@example.org"), List.of()));
        assertEquals("linebreak\ttab\nnew", sent.get(0).body());
        assertEquals(List.of("Jane <jane@example.org>"), sent.get(0).replyTo());
    }

    @Test
    void stripInvalidXmlCharsKeepsValidCharacters() {
        assertEquals("a\tb\nc\rd \u00E4\uD83D\uDE00",
            MIRFormSubmissionMailHandler.stripInvalidXmlChars("a\tb\nc\rd \u00E4\uD83D\uDE00\u0000\uFFFE"));
    }

    @Test
    void mailIsSkippedWhenDisabled() throws Exception {
        final List<MIRFormSubmissionMailHandler.OutgoingMail> sent = new ArrayList<>();
        final MIRFormSubmissionMailHandler.OutgoingMail mail = new MIRFormSubmissionMailHandler.OutgoingMail(
            "sender@example.org", null, List.of("recipient@example.org"), "Subject", "body", List.of());
        MIRFormSubmissionMailHandler.skipIfMailDisabled(() -> 0, sent::add).send(mail);
        assertEquals(0, sent.size());
        MIRFormSubmissionMailHandler.skipIfMailDisabled(() -> 1, sent::add).send(mail);
        assertEquals(1, sent.size());
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

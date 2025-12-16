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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Implementation of {@link MIRFormSubmissionHandler} that handles form submissions by generating and sending emails.
 */
@MCRConfigurationProxy(proxyClass = MIRFormSubmissionMailHandler.Factory.class)
public class MIRFormSubmissionMailHandler implements MIRFormSubmissionHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String FIELD_SENDER_NAME = "name";
    private static final String FIELD_SENDER_EMAIL = "mail";
    private static final String FIELD_COPY = "copy";

    private static final String UPLOAD_PATH = MCRConfiguration2.getStringOrThrow("MIR.UploadForm.path");

    private final String sender;
    private final List<String> recipients;
    private final String subject;
    private final MIRMailBodyGenerator bodyGenerator;
    private final List<String> requiredFieldNames;
    private final boolean attachmentAllowed;

    /**
     * Constructs a MIRFormSubmissionMailHandler.
     *
     * @param config the config
     */
    public MIRFormSubmissionMailHandler(FormSubmissionHandlerConfig config) {
        this.sender = config.sender;
        this.recipients = config.recipients();
        this.subject = config.subject;
        this.bodyGenerator = config.bodyGenerator;
        this.requiredFieldNames = config.requiredFieldNames;
        this.attachmentAllowed = config.attachmentAllowed();
    }

    @Override
    public void handle(MIRFormSubmissionRequest formSubmissionRequest) {
        final Map<String, String> fields = formSubmissionRequest.fields();
        checkFields(fields);
        final List<Path> tempFiles = new ArrayList<>();
        try {
            final String body = bodyGenerator.generateBody(fields);
            if (!formSubmissionRequest.attachments().isEmpty()) {
                if (!attachmentAllowed) {
                    throw new MIRFormSubmissionHandlerException("File part is not allowed");
                }
                for (MIRInboundAttachment attachment : formSubmissionRequest.attachments()) {
                    tempFiles.add(uploadFile(attachment));
                }
            }
            final List<String> parts = tempFiles.stream().map(Path::toUri).map(URI::toString).toList();
            final Optional<String> optFormSender = resolveFormSender(fields);
            final boolean sendCopy = resolveSendCopy(fields);
            if (sendCopy && optFormSender.isEmpty()) {
                throw new MIRFormSubmissionHandlerException("No sender found, check form");
            }
            if (optFormSender.isPresent()) {
                MCRMailer.send(sender, List.of(optFormSender.get()), recipients, null, subject, body, parts);
                if (sendCopy) {
                    MCRMailer.send(sender, optFormSender.get(), subject, body);
                }
            } else {
                MCRMailer.send(sender, recipients, subject, body, false);
            }
        } catch (IOException e) {
            throw new MIRFormSubmissionHandlerException("Failed to send mail: error handling attachments", e);
        } catch (MCRException | IllegalArgumentException e) {
            throw new MIRFormSubmissionHandlerException("Failed to send mail: " + e.getMessage(), e);
        } finally {
            tempFiles.forEach(tempFile -> {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete temporary file {}", tempFile, e);
                }
            });
        }
    }

    private Path uploadFile(MIRInboundAttachment attachment) throws IOException {
        final Path uploads = new File(UPLOAD_PATH).toPath();
        final String filename = attachment.filename();
        if (filename.isBlank() || filename.length() > 255) {
            throw new IllegalArgumentException("Invalid file name");
        }
        final Path tempFile = MCRUtils.safeResolve(uploads, filename);
        LOGGER.debug("Uploading file {} to {}", filename, tempFile);
        try (InputStream fileContent = attachment.openStream()) {
            Files.copy(fileContent, tempFile);
        }
        return tempFile;
    }

    private void checkFields(Map<String, String> fields) {
        for (String name : requiredFieldNames) {
            String value = fields.get(name);
            if (value == null || value.isBlank()) {
                throw new MIRFormSubmissionHandlerException("Missing required field: " + name);
            }
        }
    }

    private Optional<String> resolveFormSender(Map<String, String> fields) {
        final String senderEmail = fields.get(FIELD_SENDER_EMAIL);
        if (senderEmail == null || senderEmail.isBlank()) {
            return Optional.empty();
        }
        final String senderName = fields.get(FIELD_SENDER_NAME);
        if (senderName != null && !senderName.isBlank()) {
            final String result = String.format(Locale.ROOT, "%s <%s>", fields.get(FIELD_SENDER_NAME),
                fields.get(FIELD_SENDER_EMAIL));
            return Optional.of(result);
        }
        return Optional.of(fields.get(FIELD_SENDER_EMAIL));
    }

    private boolean resolveSendCopy(Map<String, String> fields) {
        return Optional.ofNullable(fields.get(FIELD_COPY)).map(Boolean::valueOf).orElse(false);
    }

    /**
     * Configuration object for handling form submissions.
     *
     * @param sender the sender email address (From)
     * @param recipients list of recipient email addresses that will receive the form submission
     * @param subject the subject of the mail
     * @param bodyGenerator generator responsible for creating the email body from the submitted form data
     * @param requiredFieldNames field names that must be present in the form submission
     * @param attachmentAllowed indicates whether file attachments are allowed in the form submission
     */
    public record FormSubmissionHandlerConfig(String sender, List<String> recipients, String subject,
        MIRMailBodyGenerator bodyGenerator, List<String> requiredFieldNames, boolean attachmentAllowed) {
    }

    /**
     * Factory class for creating {@link MIRFormSubmissionMailHandler} instances from configuration.
     */
    public static final class Factory implements Supplier<MIRFormSubmissionMailHandler> {

        /**
         * Sender of the mail.
         */
        @MCRProperty(name = "Sender")
        public String sender;

        /**
         * Comma-separated list of default recipient mail addresses.
         */
        @MCRProperty(name = "Recipients")
        public String recipientsString;

        /**
         * Subject of the mail.
         */
        @MCRProperty(name = "Subject")
        public String subject;

        /**
         * The body generator instance used to create the mail body.
         */
        @MCRInstance(name = "BodyGenerator", valueClass = MIRMailBodyGenerator.class)
        public MIRMailBodyGenerator bodyGenerator;

        /**
         * Comma-separated list of extra required fields.
         */
        @MCRProperty(name = "RequiredFieldNames", required = false)
        public String requiredFieldNamesString;

        /**
         * Whether attachments are allowed.
         */
        @MCRProperty(name = "AttachmentAllowed", required = false)
        public String attachmentAllowedString;

        @Override
        public MIRFormSubmissionMailHandler get() {
            final List<String> requiredFieldNames =
                Optional.ofNullable(requiredFieldNamesString).stream().flatMap(MCRConfiguration2::splitValue)
                    .distinct().toList();
            final List<String> recipients =
                Optional.of(recipientsString).stream().flatMap(MCRConfiguration2::splitValue).toList();
            final boolean attachmentAllowed = Optional.ofNullable(attachmentAllowedString).map(Boolean::valueOf)
                .orElse(false);
            final FormSubmissionHandlerConfig config = new FormSubmissionHandlerConfig(sender, recipients,
                subject, bodyGenerator, requiredFieldNames, attachmentAllowed);
            return new MIRFormSubmissionMailHandler(config);
        }
    }
}

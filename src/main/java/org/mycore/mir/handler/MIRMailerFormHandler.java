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
 * along with MyCoRe.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mycore.mir.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.servlet.http.Part;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.mir.MIRMailerException;

/**
 * Handles the creation and sending of MIR mails based on form data.
 */
@MCRConfigurationProxy(proxyClass = MIRMailerFormHandler.Factory.class)
public class MIRMailerFormHandler implements MIRFormHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String UPLOAD_PATH = MCRConfiguration2.getStringOrThrow("MIR.UploadForm.path");

    private final MIRMailerBodyGenerator bodyGenerator;
    private final List<String> requiredFieldNames;
    private final List<String> defaultRecipients;
    private final String subject;
    private final boolean attachmentAllowed;

    /**
     * Constructs a MIRMailerFormHandler.
     *
     * @param bodyGenerator generator for the mail body content
     * @param requiredFieldNames list of required field names
     * @param defaultRecipients list of default recipient mail addresses
     * @param subject mail subject
     * @param attachmentAllowed whether attachments are allowed
     */
    public MIRMailerFormHandler (MIRMailerBodyGenerator bodyGenerator, List<String> requiredFieldNames,
        List<String> defaultRecipients, String subject, boolean attachmentAllowed) {
        this.bodyGenerator = bodyGenerator;
        this.requiredFieldNames = requiredFieldNames;
        this.defaultRecipients = defaultRecipients;
        this.subject = subject;
        this.attachmentAllowed = attachmentAllowed;
    }

    /**
     * Returns the list of required field names.
     *
     * @return unmodifiable list of required field names
     */
    public List<String> getRequiredFieldNames() {
        return requiredFieldNames;
    }

    /**
     * Returns the list of default recipients.
     *
     * @return unmodifiable list of default recipients
     */
    public List<String> getDefaultRecipients() {
        return defaultRecipients;
    }

    /**
     * Returns the mail subject.
     *
     * @return mail subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Indicates whether attachments are allowed.
     *
     * @return true if attachments are allowed, false otherwise
     */
    public boolean isAttachmentAllowed() {
        return attachmentAllowed;
    }

    /**
     * Generates the mail body using the given form data.
     *
     * @param formData  map of form fields and their values
     * @return generated mail body
     */
    public String generateBody(Map<String, String> formData) {
        return bodyGenerator.generateBody(formData);
    }

    /**
     * Returns the list of recipients for a mail, optionally including the sender.
     *
     * @param sender the email address of the sender
     * @param sendCopy whether to include the sender in the recipient list
     * @return a list of recipient email addresses, including the sender if requested
     */
    public List<String> getRecipients(String sender, boolean sendCopy) {
        if (sendCopy) {
            final List<String> recipients = new ArrayList<>(getDefaultRecipients());
            recipients.add(sender);
            return recipients;
        }
        return getDefaultRecipients();
    }

    @Override
    public boolean checkFormData(Map<String,String> formData) {
        for (String name : getRequiredFieldNames()) {
            String value = formData.get(name);
            if (value == null || value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sends a mail using the {@link MCRMailer}.
     *
     * @param sender the mail address of the sender
     * @param formData a map of form fields used to generate the mail body
     * @param sendCopy if true, a copy of the mail will be sent to the sender
     * @param filePart optional attachment part;
     * @throws MIRMailerException if an error occurs while sending the mail
     */
    public void sendMail(String sender, Map<String,String> formData, boolean sendCopy, Part filePart) {
        if (!checkFormData(formData)) {
            throw new MIRMailerException("Mailed to send mail: data is incomplete");
        }
        Path tempFile = null;
        try {
            final String body = generateBody(formData);
            if (filePart != null && filePart.getSize() > 0) {
                if (!isAttachmentAllowed()) {
                    throw new MIRMailerException("File part is not allowed");
                }
                tempFile = uploadFile(filePart);
            }
            sendMail(sender, getRecipients(sender, sendCopy), getSubject(), body, tempFile);
        } catch (IOException e) {
            throw new MIRMailerException("Mailed to send mail: error handling attachment", e);
        } catch (MCRException e) {
            throw new MIRMailerException("Mailed to send mail: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete temporary file {}", tempFile, e);
                }
            }
        }
    }

    private Path uploadFile(Part filePart) throws IOException {
        final Path uploads = new File(UPLOAD_PATH).toPath();
        final String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        if (fileName.isBlank()) {
            throw new MIRMailerException("File name is invalid");
        }
        final Path tempFile = MCRUtils.safeResolve(uploads, fileName);
        LOGGER.debug("Uploading file {} to {}", fileName, tempFile);
        try (InputStream fileContent = filePart.getInputStream()) {
            Files.copy(fileContent, tempFile);
        }
        return tempFile;
    }

    /**
     * Sends a mail using the {@link MCRMailer} with the specified parameters.
     *
     * @param sender the mail address of the sender, e.g. name &lt;email@domain.tld&gt;
     * @param recipients list of recipient email addresses
     * @param subject the subject of the email
     * @param body the mail body content
     * @param file the file to send; can be null
     */
    protected void sendMail(String sender, List<String> recipients, String subject, String body, Path file) {
        final List<String> attachments = file == null ? List.of() : List.of(file.toUri().toString());
        MCRMailer.send(sender, recipients, subject, body, attachments, false);
    }

    /**
     * Factory class for creating {@link MIRMailerFormHandler} instances from configuration.
     */
    public static final class Factory implements Supplier<MIRMailerFormHandler> {

        /**
         * The body generator instance used to create the mail body.
         */
        @MCRInstance(name = "BodyGenerator", valueClass = MIRMailerBodyGenerator.class)
        public MIRMailerBodyGenerator bodyGenerator;

        /**
         * Comma-separated list of required fields.
         */
        @MCRProperty(name = "RequiredFieldNames", required = false)
        public String requiredFieldNamesString;

        /**
         * Comma-separated list of default recipient mail addresses.
         */
        @MCRProperty(name = "DefaultRecipients")
        public String defaultRecipientsString;

        /**
         * Subject of the mail.
         */
        @MCRProperty(name = "Subject")
        public String subject;

        /**
         * Whether attachments are allowed.
         */
        @MCRProperty(name = "AttachmentAllowed", required = false)
        public String attachmentAllowedString;

        @Override
        public MIRMailerFormHandler get() {
            final List<String> requiredFieldNames =
                Optional.ofNullable(requiredFieldNamesString).stream().flatMap(MCRConfiguration2::splitValue).toList();
            final List<String> defaultRecipients =
                Optional.of(defaultRecipientsString).stream().flatMap(MCRConfiguration2::splitValue).toList();
            final boolean attachmentAllowed = Optional.ofNullable(attachmentAllowedString).map(Boolean::valueOf)
                .orElse(false);
            return new MIRMailerFormHandler(bodyGenerator, requiredFieldNames, defaultRecipients, subject,
                attachmentAllowed);
        }
    }
}

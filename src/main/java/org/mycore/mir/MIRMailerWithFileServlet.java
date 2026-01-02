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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.mir.handler.MIRFormSubmissionHandler;
import org.mycore.mir.handler.MIRFormSubmissionHandlerException;
import org.mycore.mir.handler.MIRFormSubmissionRequest;
import org.mycore.mir.handler.MIRInboundAttachment;

import net.logicsquad.nanocaptcha.audio.AudioCaptcha;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;

/**
 * Servlet implementation class MIRMailerWithFileServlet.
 */
@MultipartConfig
public class MIRMailerWithFileServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROPERTY_PREFIX = "MIR.MailerWithFileServlet.";

    private static final String CAPTCHA_SESSION_KEY = "mwf_captcha";
    private static final String ACTION_CAPTCHA = "captcha";
    private static final String ACTION_CAPTCHA_PLAY = "captcha-play";
    private static final String DEFAULT_REDIRECT_PATH = "content/index.xml";

    private static final String PARAM_CAPTCHA = "captcha";
    private static final String PARAM_FILE = "file";
    private static final String PARAM_SENDER_NAME = "name";
    private static final String PARAM_SENDER_EMAIL = "mail";
    private static final String PARAM_ACTION = "action";

    private static final Set<String> SENSITIVE_PARAMS = Set.of(PARAM_CAPTCHA, PARAM_ACTION);

    private static final String CHAR_ENCODING =
        MCRConfiguration2.getString("MCR.Request.CharEncoding").orElse("UTF-8");

    private static final Set<String> DISALLOWED_MAIL_DOMAINS =
        MCRConfiguration2.getOrThrow(PROPERTY_PREFIX + "DisallowedEmailDomains", MCRConfiguration2::splitValue)
            .collect(Collectors.toSet());

    @Override
    protected void doGetPost(MCRServletJob job) throws ServletException, IOException {
        LOGGER.debug(() -> "Starting...");
        LOGGER.debug(() -> MIRMailerWithFileServletHelper.buildRequestLogMessage(job.getRequest()));
        final String action = job.getRequest().getParameter(PARAM_ACTION);
        if (action == null) {
            LOGGER.error(() -> "'action' parameter is required");
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        switch (action) {
            case ACTION_CAPTCHA -> handleCaptchaRequest(job);
            case ACTION_CAPTCHA_PLAY -> handleCaptchaPlayRequest(job);
            default -> handleFormSubmitAction(job, action);
        }
    }

    private void handleCaptchaRequest(MCRServletJob job) throws IOException {
        LOGGER.debug(() -> "Handling captcha request...");
        final String captchaText = MIRCaptchaHelper.generateCaptchaText();
        job.getRequest().getSession().setAttribute(CAPTCHA_SESSION_KEY, captchaText);
        final ImageCaptcha imageCaptcha = MIRCaptchaHelper.createImageCaptcha(captchaText, 150, 50);
        job.getResponse().setContentType("image/png");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(job.getResponse().getOutputStream())) {
            ImageIO.write(imageCaptcha.getImage(), "png", ios);
        }
    }

    private void handleCaptchaPlayRequest(MCRServletJob job) throws IOException {
        LOGGER.debug(() -> "Handling captcha play request...");
        final String captchaText =
            Optional.ofNullable(job.getRequest().getSession().getAttribute(CAPTCHA_SESSION_KEY)).map(Object::toString)
                .orElseGet(MIRCaptchaHelper::generateCaptchaText);
        final AudioCaptcha audioCaptcha = MIRCaptchaHelper.createAudioCaptcha(captchaText);
        job.getResponse().setContentType("audio/wav");
        try (OutputStream out = job.getResponse().getOutputStream();
            AudioInputStream ais = audioCaptcha.getAudio().getAudioInputStream()) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        }
    }

    private void handleFormSubmitAction(MCRServletJob job, String action) throws IOException, ServletException {
        LOGGER.debug("Handling form submit action '{}'", action);
        final HttpServletRequest request = job.getRequest();
        final HttpServletResponse response = job.getResponse();
        request.setCharacterEncoding(CHAR_ENCODING);
        final FormData formData = FormData.ofRequest(request);

        final boolean requiresCaptcha =
            MCRConfiguration2.getBoolean(PROPERTY_PREFIX + action + "CaptchaRequired").orElse(false);

        if (requiresCaptcha && !validateCaptcha(request, response, formData)) {
            return;
        }

        final String senderEmail = formData.senderEmail();
        if (senderEmail == null) {
            LOGGER.error(() -> "'mail' parameter is required");
            response.sendRedirect(getDefaultRedirectUrl(request));
            return;
        }
        if (!validateSender(senderEmail, request, response)) {
            return;
        }

        MIRFormSubmissionHandler handler;
        try {
            handler = MCRConfiguration2.getSingleInstanceOf(MIRFormSubmissionHandler.class,
                PROPERTY_PREFIX + action + ".FormSubmissionHandler.Class").orElseThrow();
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate form handler for action '{}'", action, e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final List<MIRInboundAttachment> attachments =
            Optional.ofNullable(request.getPart(PARAM_FILE)).stream().filter(p -> p.getSize() > 0)
                .map(PartInboundAttachment::new).map(MIRInboundAttachment.class::cast).toList();
        try {
            handler.handle(new MIRFormSubmissionRequest(formData.fields, attachments));
            final String successRedirectUrl =
                Optional.ofNullable(request.getParameter("redirect")).filter(MCRFrontendUtil::isSafeRedirect)
                    .orElse(getDefaultRedirectUrl(request));
            response.sendRedirect(response.encodeRedirectURL(successRedirectUrl));
        } catch (MIRFormSubmissionHandlerException e) {
            LOGGER.error("Error while sending mail", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateSender(String email, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        final String emailLowerCase = "@" + email.toLowerCase(Locale.ROOT);
        if (DISALLOWED_MAIL_DOMAINS.stream().anyMatch(emailLowerCase::endsWith)) {
            LOGGER.error("Will not send e-mail, disallowed senderEmail domain: {}", email);
            response.sendRedirect(getDefaultRedirectUrl(request));
            return false;
        }
        return true;
    }

    private boolean validateCaptcha(HttpServletRequest request, HttpServletResponse response, FormData formData)
        throws IOException {
        final String captcha = formData.captcha;
        if (captcha == null || !checkCaptcha(request, captcha)) {
            clearCaptcha(request);
            LOGGER.debug("Invalid captcha");
            redirectWithCaptchaError(request, response, formData);
            return false;
        }
        clearCaptcha(request);
        return true;
    }

    private boolean checkCaptcha(HttpServletRequest request, String captcha) {
        return Optional.ofNullable(request.getSession().getAttribute(CAPTCHA_SESSION_KEY))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(stored -> stored.equals(captcha))
            .orElse(false);
    }

    private void redirectWithCaptchaError(HttpServletRequest request, HttpServletResponse response,
        FormData formData) throws IOException {
        final String referer = getSafeReferer(request);
        final String separator = referer.contains("?") ? "&" : "?";
        final String url =
            referer + separator + "error=captcha" + MIRMailerWithFileServletHelper.getUrlParams(formData.fields);
        response.sendRedirect(url);
    }

    private String getSafeReferer(HttpServletRequest request) {
        return Optional.ofNullable(getReferer(request)).map(URI::toString).filter(MCRFrontendUtil::isSafeRedirect)
            .orElseGet(() -> getDefaultRedirectUrl(request));
    }

    private void clearCaptcha(HttpServletRequest request) {
        request.getSession().removeAttribute(CAPTCHA_SESSION_KEY);
    }

    private static String getDefaultRedirectUrl(HttpServletRequest request) {
        return MCRFrontendUtil.getBaseURL(request) + DEFAULT_REDIRECT_PATH;
    }

    private record FormData(String action, String captcha, String senderName, String senderEmail,
        Map<String, String> fields) {

        public static FormData ofRequest(HttpServletRequest request) {
            final Map<String, String> data = request.getParameterMap().entrySet().stream()
                .filter(e -> !SENSITIVE_PARAMS.contains(e.getKey()))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().length > 0 ? String.join(",", e.getValue()) : "",
                    (v1, v2) -> v1,
                    LinkedHashMap::new
                ));
            final String action = request.getParameter(PARAM_ACTION);
            final String captcha = request.getParameter(PARAM_CAPTCHA);
            final String name = request.getParameter(PARAM_SENDER_NAME);
            final String email = request.getParameter(PARAM_SENDER_EMAIL);
            return new FormData(action, captcha, name, email, data);
        }
    }

    private record PartInboundAttachment(Part file) implements MIRInboundAttachment {

        @Override
        public String filename() {
            return file.getSubmittedFileName();
        }

        @Override
        public long size() {
            return file.getSize();
        }

        @Override
        public InputStream openStream() throws IOException {
            return file.getInputStream();
        }
    }
}

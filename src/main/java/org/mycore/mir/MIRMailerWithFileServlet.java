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
import java.io.OutputStream;
import java.io.Serial;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
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
import org.mycore.mir.handler.MIRMailerFormHandler;

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

    private static final String CAPTCHA_SESSION_KEY = "mwf_captcha";
    private static final String CAPTCHA_PLAY_SESSION_KEY = "mwf_captcha_play";
    private static final String ACTION_CAPTCHA = "captcha";
    private static final String ACTION_CAPTCHA_PLAY = "captcha-play";
    private static final String DEFAULT_REDIRECT_PATH = "content/index.xml";

    private static final String CHAR_ENCODING =
        MCRConfiguration2.getString("MCR.Request.CharEncoding").orElse("UTF-8");

    private static final Set<String> DISALLOWED_MAIL_DOMAINS =
        MCRConfiguration2.getOrThrow("MCR.mir-module" + ".DisallowedMailDomains", MCRConfiguration2::splitValue)
            .collect(Collectors.toSet());

    @Override
    protected void doGetPost(MCRServletJob job) throws ServletException, IOException {
        LOGGER.debug(() -> "Starting...");
        final HttpServletRequest request = job.getRequest();
        final HttpServletResponse response = job.getResponse();
        request.setCharacterEncoding(CHAR_ENCODING);
        LOGGER.debug(() -> MIRMailerWithFileServletHelper.buildRequestLogMessage(request));
        final String action = request.getParameter(MIRMailerFormDataConstants.PARAM_ACTION);
        if (action == null) {
            LOGGER.error(() -> "'action' parameter is required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        switch (action) {
            case ACTION_CAPTCHA -> handleCaptchaRequest(job);
            case ACTION_CAPTCHA_PLAY -> handleCaptchaPlayRequest(job);
            default -> handleFormSubmitAction(request, response, action);
        }
    }

    private void handleCaptchaRequest(MCRServletJob job) throws IOException {
        LOGGER.debug(() -> "Handling captcha request...");
        ImageCaptcha imageCaptcha = new ImageCaptcha.Builder(200, 50).addContent().build();
        String captchaText = imageCaptcha.getContent();
        job.getRequest().getSession().setAttribute(CAPTCHA_SESSION_KEY, captchaText);
        job.getResponse().setContentType("image/png");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(job.getResponse().getOutputStream())) {
            ImageIO.write(imageCaptcha.getImage(), "png", ios);
        }
    }

    private void handleCaptchaPlayRequest(MCRServletJob job) throws IOException {
        LOGGER.debug(() -> "Handling captcha play request...");
        AudioCaptcha ac = new AudioCaptcha.Builder().addContent().build();
        String content = ac.getContent();
        job.getRequest().getSession().setAttribute(CAPTCHA_PLAY_SESSION_KEY, content);
        job.getResponse().setContentType("audio/wav");
        try (OutputStream out = job.getResponse().getOutputStream();
            AudioInputStream ais = ac.getAudio().getAudioInputStream()) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        }
    }

    private void handleFormSubmitAction(HttpServletRequest request, HttpServletResponse response, String action)
        throws IOException, ServletException {
        LOGGER.debug("Handling form submit action '{}'", action);

        final MIRMailerFormData formData = MIRMailerFormData.fromRequest(request);

        if (!validateCaptcha(request, response, formData)) {
            return;
        }

        final Optional<MIRMailerFormHandler> handlerOpt = getFormHandler(action);
        if (handlerOpt.isEmpty()) {
            LOGGER.warn("No form handler configured for action '{}'", action);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        final MIRMailerFormHandler handler = handlerOpt.get();

        final String senderName = formData.getSenderName();
        final String senderEmail = formData.getSenderEmail();

        if (!validateSender(senderName, senderEmail, request, response)) {
            return;
        }
        final String sender = String.format(Locale.ROOT, "%s <%s>", senderName, senderEmail);
        try {
            final Part filePart = Optional.ofNullable(request.getPart(MIRMailerFormDataConstants.PARAM_FILE))
                .filter(f -> f.getSize() > 0).orElse(null);
            final Part attachment = (filePart != null && handler.isAttachmentAllowed()) ? filePart : null;
            if (filePart != null && attachment == null) {
                LOGGER.warn("File part is not allowed, ignoring file");
            }
            handler.sendMail(sender, formData.getFields(), formData.isSendCopy(), attachment);
            final String successRedirectUrl =
                Optional.ofNullable(request.getParameter("redirect")).filter(MCRFrontendUtil::isSafeRedirect)
                    .orElse(getDefaultRedirectUrl(request));
            response.sendRedirect(response.encodeRedirectURL(successRedirectUrl));
        } catch (MIRMailerException e) {
            LOGGER.error("Error while sending mail", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean validateSender(String name, String email, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        if (!checkValidSender(name, email)) {
            LOGGER.error("Expected fields {}, {}", MIRMailerFormDataConstants.PARAM_SENDER_NAME,
                MIRMailerFormDataConstants.PARAM_SENDER_EMAIL);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        final String emailLowerCase = "@" + email.toLowerCase(Locale.ROOT);
        if (DISALLOWED_MAIL_DOMAINS.stream().anyMatch(emailLowerCase::endsWith)) {
            LOGGER.error("Will not send e-mail, disallowed email domain: {}", email);
            response.sendRedirect(getDefaultRedirectUrl(request));
            return false;
        }
        return true;
    }

    private boolean validateCaptcha(HttpServletRequest request, HttpServletResponse response,
        MIRMailerFormData formData) throws IOException {
        final String captcha = formData.getCaptcha();
        if (!checkCaptcha(request, captcha)) {
            clearCaptcha(request);
            LOGGER.debug("Invalid captcha");
            redirectWithCaptchaError(request, response, formData);
            return false;
        }
        clearCaptcha(request);
        return true;
    }

    private boolean checkCaptcha(HttpServletRequest request, String captcha) {
        final String rightCaptcha = Optional.ofNullable(request.getSession().getAttribute(CAPTCHA_SESSION_KEY))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse(null);
        final String rightCaptchaPlay = Optional.ofNullable(request.getSession().getAttribute(CAPTCHA_PLAY_SESSION_KEY))
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse(null);
        return captcha != null && (Objects.equals(captcha, rightCaptcha) || Objects.equals(captcha, rightCaptchaPlay));
    }

    private void redirectWithCaptchaError(HttpServletRequest request, HttpServletResponse response,
        MIRMailerFormData formData) throws IOException {
        final String referer = getSafeReferer(request);
        final String separator = referer.contains("?") ? "&" : "?";
        final String url =
            referer + separator + "error=captcha" + MIRMailerWithFileServletHelper.getUrlParams(formData.getFields());
        response.sendRedirect(url);
    }

    private String getSafeReferer(HttpServletRequest request) {
        return Optional.ofNullable(getReferer(request)).map(URI::toString).filter(MCRFrontendUtil::isSafeRedirect)
            .orElseGet(() -> getDefaultRedirectUrl(request));
    }

    private void clearCaptcha(HttpServletRequest request) {
        request.getSession().removeAttribute(CAPTCHA_SESSION_KEY);
        request.getSession().removeAttribute(CAPTCHA_PLAY_SESSION_KEY);
    }

    private boolean checkValidSender(String name, String email) {
        return name != null && !name.isBlank() && email != null && !email.isBlank();
    }

    private static String getDefaultRedirectUrl(HttpServletRequest request) {
        return MCRFrontendUtil.getBaseURL(request) + DEFAULT_REDIRECT_PATH;
    }

    private Optional<MIRMailerFormHandler> getFormHandler(String action) {
        return MCRConfiguration2.<MIRMailerFormHandler>getSingleInstanceOf(
            "MIRMailerWithFileServlet." + action + ".FormHandler.Class");
    }
}

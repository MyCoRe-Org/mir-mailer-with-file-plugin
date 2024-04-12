/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import net.logicsquad.nanocaptcha.audio.AudioCaptcha;
import net.logicsquad.nanocaptcha.image.ImageCaptcha;


/**
 * Servlet implementation class MIRMailerWithFileServlet
 */
@MultipartConfig
public class MIRMailerWithFileServlet extends MCRServlet {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MCR_MODULE_EDITOR_MAIL = "MCR.mir-module.EditorMail";
    public static final String CAPTCHA_SESSION_KEY = "mwf_captcha";
    public static final String CAPTCHA_PLAY_SESSION_KEY = "mwf_captcha_play";

    private static final List<String> DISALLOWED_MAIL_DOMAINS;

    static {
        DISALLOWED_MAIL_DOMAINS = MCRConfiguration2
                .getOrThrow("MCR.mir-module.DisallowedMailDomains", MCRConfiguration2::splitValue)
                .toList();
    }

    private static ArrayList<String> getRecipients(boolean copy, String mail) {
        ArrayList<String> recipients = new ArrayList<>();
        recipients.add(MCRConfiguration2.getStringOrThrow(MCR_MODULE_EDITOR_MAIL));

        if (copy) {
            recipients.add(mail);
        }

        return recipients;
    }

    private static String getFormattedMailBody(RequestData requestData) {
        return String.format(Locale.ROOT, """
            %s sendet folgende Publikation zur Einreichung:


            Angaben zur Person:

               -------------------
               Name:      %s
               E-Mail:    %s
               Institut:  %s
               Fakultät:  %s


               Angaben zur Publikation:
               ------------------------
               Titel (deutsch):            %s
               Titel (englisch):           %s
               Lizenz:                     %s
               Schlagworte (deutsch):      %s
               Schlagworte (englisch):     %s
               zusammenfassung (deutsch):
               %s

               zusammenfassung (englisch):
               %s

               Anmerkungen:
               %s


            """,
            requestData.name(),
            requestData.name(),
            requestData.mail(),
            requestData.institute(),
            requestData.faculty(),
            requestData.title_de(),
            requestData.title_en(),
            requestData.license(),
            requestData.keywords_de(),
            requestData.keywords_en(),
            requestData.abstract_de(),
            requestData.abstract_en(),
            requestData.comment());
    }

    @Override
    protected void doGetPost(MCRServletJob job) throws ServletException, IOException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        if (request.getParameter("action") == null) {
            response.sendRedirect(MCRFrontendUtil.getBaseURL(request) + "editor/submit_request.xed");
            return;
        }

        switch (request.getParameter("action")) {
        case "submit_request" -> handleSubmitRequest(request, response);
        case "captcha" -> handleCaptchaRequest(job);
        case "captcha-play" -> handleCaptchaPlayRequest(job);
        default -> response.sendRedirect(MCRFrontendUtil.getBaseURL(request) + "editor/submit_request.xed");
        }
    }

    private void handleCaptchaRequest(MCRServletJob job) throws IOException {
        job.getResponse().setContentType("image/png");
        ImageCaptcha imageCaptcha = new ImageCaptcha.Builder(200, 50).addContent().build();
        String captchaText = imageCaptcha.getContent();
        job.getRequest().getSession().setAttribute(CAPTCHA_SESSION_KEY, captchaText);
        job.getResponse().setContentType("image/png");
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(job.getResponse().getOutputStream())) {
            ImageIO.write(imageCaptcha.getImage(), "png", ios);
        }
    }

    private void handleCaptchaPlayRequest(MCRServletJob job) throws IOException {
        AudioCaptcha ac = new AudioCaptcha.Builder().addContent().build();
        String content = ac.getContent();
        job.getRequest().getSession().setAttribute(CAPTCHA_PLAY_SESSION_KEY, content);
        job.getResponse().setContentType("audio/wav");
        try (OutputStream out = job.getResponse().getOutputStream();
            AudioInputStream ais = ac.getAudio().getAudioInputStream()) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        }
    }

    public void handleSubmitRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException {
        String reqCharEncoding = MCRConfiguration2.getString("MCR.Request.CharEncoding").orElse("UTF-8");
        request.setCharacterEncoding(reqCharEncoding);

        String baseUrl = MCRFrontendUtil.getBaseURL(request);
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String formURL = baseUrl + "editor/submit_request.xed";

        RequestData requestData = RequestData.fromRequest(request);

        String sender = requestData.name() + "<" + requestData.mail() + ">";
        List<String> recipients = getRecipients(request.getParameterMap().containsKey("copy"), requestData.mail());

        String subject   = "[Publikationsserver] - Online-Einreichung";
        String body = getFormattedMailBody(requestData);

        String captcha = request.getParameter("captcha");
        String rightCaptcha = request.getSession().getAttribute(CAPTCHA_SESSION_KEY) instanceof String s ? s : null;
        String rightCaptchaPlay
            = request.getSession().getAttribute(CAPTCHA_PLAY_SESSION_KEY) instanceof String s ? s : null;
        if (captcha == null || (!Objects.equals(captcha, rightCaptcha) && !Objects.equals(captcha, rightCaptchaPlay))) {
            response.sendRedirect(formURL + "?error=captcha" + requestData.toURLParams());
            return;
        }

        request.getSession().removeAttribute(CAPTCHA_SESSION_KEY);
        request.getSession().removeAttribute(CAPTCHA_PLAY_SESSION_KEY);

        if (DISALLOWED_MAIL_DOMAINS.stream().anyMatch(requestData.mail()::endsWith)) {
            LOGGER.error("Will not send e-mail, disallowed mail domain: " + requestData.mail());
            response.sendRedirect(baseUrl + "content/index.xml");
        }

        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">

        if (filePart == null || filePart.getSize() == 0) {
            MCRMailer.send(sender, recipients, subject, body, Collections.emptyList(), false);
            response.sendRedirect(baseUrl + "content/index.xml");
            return;
        }

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.

        Path uploads = new File(MCRConfiguration2.getStringOrThrow("MIR.UploadForm.path")).toPath();
        Path file = MCRUtils.safeResolve(uploads, fileName);

        try {
            InputStream fileContent = filePart.getInputStream();
            try {
                Files.copy(fileContent, file);
                List<String> parts = new ArrayList<>();
                parts.add(file.toFile().toURI().toString());
                MCRMailer.send(sender, recipients, subject, body, parts, false);
            } finally {
                Files.delete(file);
            }
        } catch (Exception e) {
            LOGGER.error("Will not send e-mail, file upload failed of file " + fileName, e);
        }

        response.sendRedirect(baseUrl + "content/index.xml");
    }

    record RequestData(String name, String mail, String institute, String faculty, String title_de, String title_en,
        String license, String keywords_de, String keywords_en, String abstract_de, String abstract_en,
        String comment) {
        public static RequestData fromRequest(HttpServletRequest request) {
            return new RequestData(
                request.getParameter("name"),
                request.getParameter("mail"),
                request.getParameter("institute"),
                request.getParameter("faculty"),
                request.getParameter("title_de"),
                request.getParameter("title_en"),
                request.getParameter("license"),
                request.getParameter("keywords_de"),
                request.getParameter("keywords_en"),
                request.getParameter("abstract_de"),
                request.getParameter("abstract_en"),
                request.getParameter("comment"));
        }

        public String toURLParams() {
            StringBuilder sb = new StringBuilder();
            if (name() != null) {
                sb.append("&name=").append(name());
            }
            if (mail() != null) {
                sb.append("&mail=").append(mail());
            }
            if (institute() != null) {
                sb.append("&institute=").append(institute());
            }
            if (faculty() != null) {
                sb.append("&faculty=").append(faculty());
            }
            if (title_de() != null) {
                sb.append("&title_de=").append(title_de());
            }
            if (title_en() != null) {
                sb.append("&title_en=").append(title_en());
            }
            if (license() != null) {
                sb.append("&license=").append(license());
            }
            if (keywords_de() != null) {
                sb.append("&keywords_de=").append(keywords_de());
            }
            if (keywords_en() != null) {
                sb.append("&keywords_en=").append(keywords_en());
            }
            if (abstract_de() != null) {
                sb.append("&abstract_de=").append(abstract_de());
            }
            if (abstract_en() != null) {
                sb.append("&abstract_en=").append(abstract_en());
            }
            if (comment() != null) {
                sb.append("&comment=").append(comment());
            }
            return sb.toString();
        }
    }

}

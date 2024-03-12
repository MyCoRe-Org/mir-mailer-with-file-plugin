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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;


/**
 * Servlet implementation class MIRMailerWithFileServlet
 */
@MultipartConfig
public class MIRMailerWithFileServlet extends MCRServlet {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MCR_MODULE_EDITOR_MAIL = "MCR.mir-module.EditorMail";

    /**
     * @see MCRServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String reqCharEncoding = MCRConfiguration2.getString("MCR.Request.CharEncoding").orElse("UTF-8");
        request.setCharacterEncoding(reqCharEncoding);

        // Retrieves <input type="text" name="name"> and <input type="text" name="mail">
        String sender    = request.getParameter("name") + "<" + request.getParameter("mail") + ">";
        List<String> recipients = new ArrayList<String>();
        recipients.add(MCRConfiguration2.getStringOrThrow(MCR_MODULE_EDITOR_MAIL));
        if (request.getParameterMap().containsKey("copy")) {
            recipients.add(request.getParameter("mail"));
        }
        String subject   = "[Publikationsserver] - Online-Einreichung";
        String nl = System.lineSeparator();
        String body      = request.getParameter("name") + " sendet folgende Publikation zur Einreichung:"
            + nl + nl + nl


            + "Angaben zur Person:" + nl
            + "-------------------" + nl
            + "Name:      " + request.getParameter("name") + nl
            + "E-Mail:    " + request.getParameter("mail") + nl
            + "Institut:  " + request.getParameter("institute") + nl
            + "Fakultät:  " + request.getParameter("faculty") + nl
            + nl + nl


            + "Angaben zur Publikation:    " + nl
            + "------------------------    " + nl
            + "Titel (deutsch):            " + request.getParameter("title_de") + nl
            + "Titel (englisch):           " + request.getParameter("title_en") + nl
            + "Lizenz:                     " + request.getParameter("license") + nl
            + "Schlagworte (deutsch):      " + request.getParameter("keywords_de") + nl
            + "Schlagworte (englisch):     " + request.getParameter("keywords_en") + nl + nl
            + "Zusammenfassung (deutsch):  " + nl + request.getParameter("abstract_de") + nl + nl + nl
            + "Zusammenfassung (englisch): " + nl + request.getParameter("abstract_en") + nl + nl + nl
            + "Anmerkungen:                " + nl + request.getParameter("comment") + nl;

        List<String> parts = new ArrayList<>();
        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">

        if (filePart.getSize() != 0) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.

            Path uploads = new File(MCRConfiguration2.getStringOrThrow("MIR.UploadForm.path")).toPath();
            Path file = MCRUtils.safeResolve(uploads, fileName);

            try {
                InputStream fileContent = filePart.getInputStream();
                try {
                    Files.copy(fileContent, file);
                    parts.add(file.toFile().toURI().toString());
                    MCRMailer.send(sender, recipients, subject, body, parts, false);
                } finally {
                    Files.delete(file);
                }
            } catch (Exception e) {
                LOGGER.warn("Will not send e-mail, file upload failed of file " + fileName);
            }
        } else {
            MCRMailer.send(sender, recipients, subject, body, parts, false);
        }

        response.sendRedirect(request.getParameter("goto"));
    }

}

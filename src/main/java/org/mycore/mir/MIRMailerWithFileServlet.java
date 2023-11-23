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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import org.apache.log4j.Logger;
import org.mycore.common.MCRMailer;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.servlets.MCRServlet;
@MultipartConfig

/*
 * Servlet implementation class MIRMailerWithFileServlet
 */
public class MIRMailerWithFileServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MIRMailerWithFileServlet.class);
    public static final String MCR_MODULE_EDITOR_MAIL = "MCR.mir-module.EditorMail";

    /**
     * @see MCRServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ReqCharEncoding = MCRConfiguration2.getString("MCR.Request.CharEncoding").orElse("UTF-8");
        request.setCharacterEncoding(ReqCharEncoding);

        String sender    = request.getParameter("name") + "<" + request.getParameter("mail") + ">"; // Retrieves <input type="text" name="name"> and <input type="text" name="mail">
        List<String> recipients = new ArrayList<String>();
        recipients.add(MCRConfiguration2.getStringOrThrow(MCR_MODULE_EDITOR_MAIL));
        if (request.getParameterMap().containsKey("copy")) {
            recipients.add(request.getParameter("mail"));
        }
        String subject   = "[Publikationsserver] - Online-Einreichung";
        String body      = request.getParameter("name") + " sendet folgende Publikation zur Einreichung:"
                           + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()


                           + "Angaben zur Person:" + System.lineSeparator()
                           + "-------------------" + System.lineSeparator()
                           + "Name:      " + request.getParameter("name") + System.lineSeparator()
                           + "E-Mail:    " + request.getParameter("mail") + System.lineSeparator()
                           + "Institut:  " + request.getParameter("institute") + System.lineSeparator()
                           + "Fakultät:  " + request.getParameter("faculty") + System.lineSeparator()
                           + System.lineSeparator() + System.lineSeparator()


                           + "Angaben zur Publikation:    " + System.lineSeparator()
                           + "------------------------    " + System.lineSeparator()
                           + "Titel (deutsch):            " + request.getParameter("title_de") + System.lineSeparator()
                           + "Titel (englisch):           " + request.getParameter("title_en") + System.lineSeparator()
                           + "Lizenz:                     " + request.getParameter("license") + System.lineSeparator()
                           + "Schlagworte (deutsch):      " + request.getParameter("keywords_de") + System.lineSeparator()
                           + "Schlagworte (englisch):     " + request.getParameter("keywords_en") + System.lineSeparator() + System.lineSeparator()
                           + "Zusammenfassung (deutsch):  " + System.lineSeparator() + request.getParameter("abstract_de") + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()
                           + "Zusammenfassung (englisch): " + System.lineSeparator() + request.getParameter("abstract_en") + System.lineSeparator() + System.lineSeparator() + System.lineSeparator()
                           + "Anmerkungen:                " + System.lineSeparator() + request.getParameter("comment") + System.lineSeparator();

        List<String> parts = new ArrayList<String>();
        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">

        if (filePart.getSize() != 0) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.

            File uploads = new File(MCRConfiguration2.getStringOrThrow("MIR.UploadForm.path"));
            File file = new File(uploads, fileName);

            try {
                InputStream fileContent = filePart.getInputStream();
                Files.copy(fileContent, file.toPath());
                parts.add(file.toURI().toString());
                MCRMailer.send(sender, recipients, subject, body, parts, false);
            } catch (Exception e) {
                LOGGER.warn("Will not send e-mail, file upload failed of file " + fileName);
            }
            try {
                file.delete();
            } catch (Exception e) {
                LOGGER.warn("Error while try to delete file " + fileName);
            }
        }

        else {
            MCRMailer.send(sender, recipients, subject, body, parts, false);
        }

        response.sendRedirect(request.getParameter("goto"));
    }

}

# mir-mailer-with-file-plugin

## Overview
The **mir-mailer-with-file-plugin** extends MyCoRe/MIR with the ability to send emails directly from the frontend.
A dedicated **servlet** processes form submissions, validates captchas, handles optional file uploads, and executes 
configurable form handlers.

### Key Features
- Validates **captchas** on form submission
- Supports **file uploads**
- Allows **configurable form handlers** per action
- Template-based email body generation
- Filters unwanted sender domains
- Unified servlet backend for all mail forms

## Installation
- Copy jar to `~/.mycore/(dev-)mir/lib/`
- Configure `mycore.properties` if necessary

## Configuration
### Required Properties
- **`MCR.mir-module.DisallowedMailDomains`**  
  A list of blocked sender domains.

- **`MIR.UploadForm.path`**  
  Temporary storage location for uploaded files.

---

### Configuring a Form Handler
Each action is associated with a dedicated FormHandler.
Example configuration:

```properties
# FormHandler for the action "submit_request"
MIRMailerWithFileServlet.submit_request.FormHandler.Class=org.mycore.mir.handler.MIRMailerFormHandler
# Body generator for the mail content
MIRMailerWithFileServlet.submit_request.FormHandler.BodyGenerator.Class=org.mycore.mir.handler.MIRSimpleTemplateBodyGenerator
# Template loader for mail templates
MIRMailerWithFileServlet.submit_request.FormHandler.BodyGenerator.TemplateLoader.Class=org.mycore.mir.handler.MIRStringTemplateLoader
MIRMailerWithFileServlet.submit_request.FormHandler.BodyGenerator.TemplateLoader.File=/submit_request_template.txt
# Comma seperated default recipients (e.g., mail@domain.tld,)
MIRMailerWithFileServlet.submit_request.FormHandler.DefaultRecipients=%MCR.mir-module.EditorMail%
# Mail subject
MIRMailerWithFileServlet.submit_request.FormHandler.Subject=[PublicationServer] - Online Submission
# Allow attachments
MIRMailerWithFileServlet.submit_request.FormHandler.AttachmentAllowed=true
# Required field names (optional)
MIRMailerWithFileServlet.submit_request.FormHandler.RequiredFieldNames=
```

## Frontend Integration
The plugin provides the **`MIRMailerWithFileServlet`**, which processes different actions and recognizes special form fields.

### Actions
| Path           | Purpose                                              |
|----------------|------------------------------------------------------|
| `captcha`      | Returns a captcha image                              |
| `captcha-play` | Returns an audio with captcha content                |
| `<handler>`    | Executes the configured form handler for `<handler>` |

---

### Reserved Form Fields
These fields are interpreted and processed specially by the servlet:

| Field Name | Purpose                                | Default | Required |
|------------|----------------------------------------|---------|----------|
| `action`   | Determines which FormHandler to invoke | null    | yes      |
| `name`     | Sender's name                          | null    | yes      |
| `mail`     | Sender's email                         | null    | yes      |
| `captcha`  | User input used for captcha validation | null    | yes      |
| `file`     | Uploaded file                          | null    | no       |
| `copy`     | Send a copy to the sender (boolean)    | false   | no       |

---

### Example HTML Form
```html
<form method="post" enctype="multipart/form-data" action="/servlets/MIRMailerWithFileServlet">
  <input type="hidden" name="action" value="submit_request">
    
  <label>Name: <input type="text" name="name"></label>
  <label>Email: <input type="email" name="mail"></label>
  <label>File: <input type="file" name="file"></label>
    
  <img id="captcha-image" src="/servlets/MIRMailerWithFile?action=captcha" alt="captcha" />
  <a href="#" id="captcha-refresh">Refresh captcha</a>
  <a href="#play" id="captcha-play">Play captcha audio</a>
  <a href="#stop" id="captcha-stop" class="d-none">Stop captcha audio</a>

  <input type="text" id="captcha-input" name="captcha" required />
    
  <label><input type="checkbox" name="copy" value="true"> Send me a copy</label>
  
  <button type="submit" id="save">Submit</button>
</form>

<script src="/js/captcha.js" type="text/javascript" />
<script src="/js/mailer-with-file.js" type="text/javascript" />
```

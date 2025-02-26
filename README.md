# Workato SMTP Extension

Current tested to work with Gmail's SMTP server using App Password credentials.
Supports both plain text and HTML emails with attachments.

## Building extension

Steps to build an extension:

1. Install the latest Java 17 SDK
2. Use `./gradlew jar` command to bootstrap Gradle and build the project.
3. The output is in `build/libs`.

## Installing the extension to OPA

1. Add a new directory called `ext` under Workato agent install directory.
2. Copy the extension JAR file to `ext` directory. Pre-build jar: [workato-smtp-connector-2.1.0.jar](build/libs/workato-opa-smtp-extension-2.1.0.jar)
3. Download and include these dependencies in the `ext` directory:
   - [jakarta.mail-2.1.3.jar](https://repo1.maven.org/maven2/jakarta/mail/jakarta.mail/2.1.3/jakarta.mail-2.1.3.jar)
   - [angus-mail-2.0.3.jar](https://repo1.maven.org/maven2/org/eclipse/angus/angus-mail/2.0.2/angus-mail-2.0.3.jar)
   - [angus-activation-2.0.2.jar](https://repo1.maven.org/maven2/org/eclipse/angus/angus-activation/2.0.1/angus-activation-2.0.2.jar)
4. Update the `config/config.yml` to add the `ext` file to class path.

```yml
server:
   classpath: /opt/opa/workato-agent/ext
```

5. Update the `conf/config.yml` to configure the new extension.

```yml
extensions:
   smtp:
      controllerClass: com.knyc.opa.SMTPExtension
```

## Custom SDK for the extension

The corresponding custom SDK can be found here in this repo as well.

Link: [opa-smtp-connector.rb](custom-sdk/opa-smtp-connector.rb)

Create a new Custom SDK in your Workato workspace and use it with the OPA extension.

## Features
- Support for STARTTLS, SSL, TLS, and no authentication
- HTML and plain text email formats
- File attachments (Base64 encoded)
- Connection and read timeout configuration
- Detailed error logging
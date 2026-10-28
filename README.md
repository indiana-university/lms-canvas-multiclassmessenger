# lms-canvas-multiclassmessenger
App for sending messages and announcements to multiple Canvas classes at once.

## Running standalone
Add env vars or system properties as desired.

| ENV Property             | System Property           | Default Value           | Description                                                                                                    |
|--------------------------|---------------------------|-------------------------|----------------------------------------------------------------------------------------------------------------|
| `APP_FULLFILEPATH`       | `app.fullFilePath`        | `/usr/src/app/config`   | Directory for configuration files                                                                              |
| `APP_OVERRIDESFILENAME`  | `app.overridesFileName`   | `overrides.properties`  | Customizable filename for additional configurations.  Would be located in the above directory.                 |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active`  |                         | Supply spring profiles to activate.  See configuration details below for potential values.                     |
| `APP_ENV`                | `app.env`                 | `dev`                   | Environment designator.  Free-form and can be used for your own purposes.  Shows up in the application footer. |


## Setup Database
After compiling, see `target/generated-resources/postgresql.sql` for appropriate ddl.

Insert a row into the `LTI_13_AUTHZ` table for each of the tool's two LTI 1.3 registrations, with the client id of
the matching Canvas LTI Key, `ENV` set to the value of `app.env` (`dev` by default), and `ACTIVE` set to true:

| `REGISTRATION_ID`                  | Canvas LTI Key                                                  |
|------------------------------------|-----------------------------------------------------------------|
| `lms_lti_multiclassmessenger_msg`  | Multicourse Messages ([example](examples/multicourse_messages.json))      |
| `lms_lti_multiclassmessenger_annc` | Multicourse Announcements ([example](examples/multicourse_announcements.json)) |

The `LMS_CANVAS_OAUTH2_AUTHZ` table, which stores each user's encrypted Canvas OAuth2 token, must also exist. It is
shared by every tool that uses per-user Canvas OAuth2.

## Canvas setup
The tool makes all of its Canvas API calls as the launching user, with that user's own Canvas OAuth2 token, rather
than with a shared admin token. The first time a user launches either tool, they're asked to connect their Canvas
account.

1. **LTI Keys.** Create one LTI Key per tool from the JSON in the [examples](examples) directory (both point at
   `localhost:8080`; adjust the hosts for other environments). Both must include the custom field
   `canvas_user_id=$Canvas.user.id`, which tells the tool whose Canvas OAuth2 token to use. If a tool is already
   installed, add that same custom parameter to the installed placement too: Canvas keeps its own copy of the
   placement's custom parameters, so editing the LTI Key alone doesn't take effect.
2. **API Key.** Create a Canvas API Key (Admin > Developer Keys > + Developer Key > API Key) and turn it on.
   - Redirect URI, once per host the tool runs on:
     `http://localhost:8080/login/oauth2/code/lms_canvas_oauth2_multiclassmessenger` (and the same path on each
     deployed host).
   - Enable *Enforce Scopes* (leave *Allow Include Parameters* checked) with the scopes listed under
     `spring.security.oauth2.client.registration.lms_canvas_oauth2_multiclassmessenger.scope` in
     [application.yml](src/main/resources/application.yml).

## Test a local launch
Launch the tools from Canvas itself rather than an LTI test platform: the Canvas OAuth2 consent step goes to the real
Canvas, and the `canvas_user_id` sent at launch must be the same Canvas user who authorizes.

1. Use a test user who is a Teacher in at least two published courses in the same term. The announcements tool
   needs at least one other course in the current course's term; the messages tool lists only published,
   unlocked courses.
2. Launch Multicourse Messages from the user navigation menu, or Multicourse Announcements from a course's
   navigation. The first launch shows the "connect your Canvas account" page; authorize it and you return to the tool.
3. To go through the consent flow again, delete your row from `LMS_CANVAS_OAUTH2_AUTHZ`.

If you see a page saying the `custom_canvas_user_id` parameter is missing, the `canvas_user_id` custom parameter
isn't on the installed placement (step 1 of Canvas setup). Despite the message's wording, the key is `canvas_user_id`.

## Canvas XML
Example JSON for both the announcements and messages tools can be found in the [examples](examples) directory.

## Configuration
If choosing to use properties files for the configuration values, the default location is `/usr/src/app/config`, but that can be overridden by setting the `APP_FULLFILEPATH` value via system property or environment variable.
You may use `security.properties`, `overrides.properties`, or set the `APP_OVERRIDESFILENAME` value with your desired file name.

### Canvas Configuration
The following properties need to be set to configure the communication with Canvas and Canvas Catalog.
They can be set in a properties file, or overridden as environment variables.

| Property             | Default Value               | Description                                               |
|----------------------|-----------------------------|-----------------------------------------------------------|
| `canvas.host`        |                             | Hostname of the Canvas instance                           |
| `canvas.sso.host`    |                             | Hostname of the Canvas OIDC auth domain                   |
| `canvas.baseUrl`     | https://`${canvas.host}`    | Base URL of the Canvas instance                           |
| `canvas.baseApiUrl`  | `${canvas.baseUrl}`/api/v1  | Base URL for the Canvas API                               |
| `canvas.token`       |                             | Token for access to Canvas instance. Required by the shared Canvas client configuration, though this tool's own Canvas calls use per-user OAuth2 tokens |
| `canvas.accountId`   |                             | Your institution's root accountId in your Canvas instance |
| `catalog.baseUrl`    |                             | Base URL of the Canvas Catalog instance                   |
| `catalog.baseApiUrl` | `${catalog.baseUrl}`/api/v1 | Base URL for the Canvas Catalog API                       |
| `catalog.token`      |                             | Token for access to the Canvas Catalog instance           |

### Canvas OAuth2 configuration
The following properties configure per-user Canvas API access. Set them in `security.properties` (or Vault); never
commit real values.

| Property                                         | Description                                                                                                                          |
|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `canvas.oauth2.clientId.multiclassmessenger`     | Client id of this tool's Canvas API Key                                                                                              |
| `canvas.oauth2.clientSecret.multiclassmessenger` | Client secret of this tool's Canvas API Key                                                                                          |
| `canvas.oauth2.encryptionPassword`               | Password used to encrypt stored user tokens. Shared by every tool using per-user Canvas OAuth2; the app won't start without it        |
| `canvas.oauth2.encryptionSalt`                   | Hex-encoded salt used with the password above. Shared by every tool using per-user Canvas OAuth2; the app won't start without it      |

### Trusted file upload hosts (optional)
Message attachments are uploaded through Canvas's file upload API, which sends the file itself to Canvas's file
storage service (`inst-fs-<region>-<environment>.inscloudgate.net` on hosted Canvas) rather than the Canvas API host.
Uploads are only sent to `canvas.host` or a host matching this list, which defaults to `*.inscloudgate.net`, so
hosted Canvas needs no configuration.

| Property                    | Default              | Description                                                                                                                                       |
|-----------------------------|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `canvas.trustedUploadHosts` | `*.inscloudgate.net` | Comma-separated hosts, beyond `canvas.host`, that file uploads may be sent to. `*.` plus a domain matches any host under it. Setting this replaces the default, so include `*.inscloudgate.net` if you still need it |

### Database Configuration
The following properties need to be set to configure the communication with a database.
They can be set in a properties file, or overridden as environment variables.

| Property          | Description                                                                                                            |
|-------------------|------------------------------------------------------------------------------------------------------------------------|
| `lms.db.user`     | Username used to access the database                                                                                   |
| `lms.db.url`      | JDBC URL of the database.  Will have the form `jdbc:<dbtype>://<host>:<port>/<database>`                               |
| `lms.db.password` | Password for the user accessing the database                                                                           |
| `lms.db.poolType` | Fully qualified name of the connection pool implementation to use. By default, it is auto-detected from the classpath. |

### Configure support contact information
The following properties need to be set to configure the contact information on the global error page.
They can be set in a security.properties file, or overridden as environment variables.

| Property                | Description                                                                                               |
|-------------------------|-----------------------------------------------------------------------------------------------------------|
| `lti.errorcontact.name` | Display name for your support organization                                                                |
| `lti.errorcontact.link` | Contact mechanism - URL or mailto:email (e.g. `http://support.school.edu` or `mailto:support@school.edu`) |

### LTI issuer configuration
Be sure to configure an environment appropriate issuer for the Canvas instance that is launching the tool.

See https://canvas.instructure.com/doc/api/file.lti_dev_key_config.html#overview-of-an-lti-launch for details.

| Property          | Description                                              |
|-------------------|----------------------------------------------------------|
| `canvas.issuer`   | Issuer for the Canvas instance doing the LTI tool launch |

### Redis Configuration (optional)
If you would like to use Redis for session storage, you will need to enable it by including the value `redis-session` into the `SPRING_PROFILES_ACTIVE` environment variable. Be aware that if the tool requires multiple values, that there could be more than one profile value in there.

Additionally, the following properties need to be set to configure the communication with Redis.
Then can be set in a properties file, or overridden as environment variables.

| Property                | Description                                    |
|-------------------------|------------------------------------------------|
| `spring.redis.host`     | Redis server host.                             |
| `spring.redis.port`     | Redis server port.                             |
| `spring.redis.database` | Database index used by the connection factory. |
| `spring.redis.password` | Login password of the redis server.            |


### Vault Configuration (optional)
If you would like to use HasiCorp's Vault for secure property storage, you will need to enable it by including the value `vault` into the `SPRING_PROFILES_ACTIVE` environment variable. Be aware that if the tool requires multiple values, that there could be more than one profile value in there.
Include any `spring.cloud.vault.*` properties that your environment requires in a properties file, or override as environment variables.

### Exposing the LTI authz REST endpoints
If you would like to expose the LTI authz endpoints in this tool (for CRUD operations on the LTI authorizations), you will
need to enable it by including the value `ltirest` into the `SPRING_PROFILES_ACTIVE` environment variable. Be aware that 
if the tool requires multiple values, that there could be more than one profile value in there.

#### Enabling swagger-ui for the LTI authz REST endpoints
:warning: Experimental :warning:

If you would like to enable the swagger-ui for interacting with the endpoints, include the value `swagger` into the `SPRING_PROFILES_ACTIVE` environment variable.
Once enabled, the ui will be available at `/api/lti/swagger-ui.html`.  There are some additional OAuth2 considerations 
that need to be accounted for while using this setup.

This is marked as experimental due to the fact that we aren't running with this option at IU.  We are running into CORS 
issues when trying to talk to our OAuth2 service via swagger, so we can't verify if it really works or not!
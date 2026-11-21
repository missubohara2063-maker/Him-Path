# Him-Path

Him-Path is a Nepal-focused trekking guide, trip planner, equipment rental and marketplace web application. It is a college-project prototype built with Java and Spring Boot, with a browser interface made from HTML, CSS and JavaScript.

## Quick start on Windows

The project includes its own Java 17 runtime, Maven and MariaDB runtime in `.runtime` and `tools`. You do not need to install Java, Maven, XAMPP or a separate database to run this checkout.

1. Open the project folder in File Explorer.
2. Double-click `Start-Him-Path.bat`.
3. Wait for the Him-Path Server window to report that the site is ready. The launcher opens the site automatically.
4. If it does not open, visit [http://localhost:8080](http://localhost:8080).

Keep the server window open while using the site. Stop the Spring Boot server with **Ctrl+C** in that window. The first build may need internet access to download Maven dependencies.

Useful pages:

- Home: [http://localhost:8080/](http://localhost:8080/)
- Treks: [http://localhost:8080/treks.html](http://localhost:8080/treks.html)
- Guides: [http://localhost:8080/guides.html](http://localhost:8080/guides.html)
- Equipment: [http://localhost:8080/equipment.html](http://localhost:8080/equipment.html)
- Trip planner: [http://localhost:8080/plan.html](http://localhost:8080/plan.html)
- User sign in and registration: [http://localhost:8080/user-login.html](http://localhost:8080/user-login.html)
- Admin sign in: [http://localhost:8080/admin-login.html](http://localhost:8080/admin-login.html)
- Admin workspace: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

Opening `src/main/resources/static/index.html` directly only shows the browser preview. Use the local server for database, authentication, registration and email features.

## Admin and demo accounts

The current local database and a fresh local database use this primary administrator account:

| Role | Login ID | Password |
| --- | --- | --- |
| Admin | `missubohara2063@gmail.com` | `HimPath!Summit27#Lotus` |
| Demo user (fresh database) | `demo` | `HimPathUser!2026` |

Admin and user sign-in are separate. Use **Admin login** for the administrator address above. That email is already registered as the admin and cannot also be used to create a separate user account. Create a user account with a different email.

These credentials are for this local project demo. The administrator password is stored as a BCrypt hash in the database. `HIMPATH_ADMIN_PASSWORD` and `HIMPATH_DEMO_PASSWORD`, if set before the corresponding account is first created, provide alternative initial passwords. The variables do not overwrite existing account passwords. Do not expose this README or these demo credentials on a public deployment; change them first. The app does not currently provide a password-recovery page.

## Technology used

| Area | Technology |
| --- | --- |
| Main backend language | Java 17 |
| Web framework | Spring Boot 3.3.5, Spring Web/MVC |
| Browser interface | HTML5, CSS3, vanilla JavaScript |
| Database | MariaDB 12.3.2, using the MySQL Connector/J driver |
| Data access | Spring Data JPA, Hibernate and JDBC |
| Authentication | Spring Security, database-backed accounts and server sessions |
| Password storage | BCrypt password hashing |
| Email | Spring Boot Mail with SMTP (Gmail supported) |
| AI assistant | Google Gemini API plus public Nepal news RSS and Open-Meteo forecast data (key kept server-side) |
| Build | Maven 3.9.16 (included in `tools`) |

The Java backend uses classes, services, controllers and repositories. The Him-Path chatbot calls Google's Gemini API from the server for Nepal trekking questions only. Signed-in profile questions (email, account role and whether a separate display name exists) are answered locally from the active session, without sending profile details to Gemini. Other unrelated questions are declined. Chat messages and trek catalog context are processed by Google; do not enter passwords, OTPs or sensitive personal information.

## Features

- Separate pages for Nepal treks, local guide information, equipment and trip planning.
- 14 Nepal trek options, including Panch Pokhari, Mardi Himal, Upper Mustang, Khopra Ridge, Tsum Valley, Kanchenjunga, Makalu and Rara.
- Trek details include route, difficulty, duration, altitude, indicative cost, day-by-day itinerary, permit notes and Nepal photo galleries with source/license links.
- Trek suggestions, equipment rental requests, and trip plans.
- Rental approvals and declines in Admin Studio; approval emails explain Hand Cash collection or eSewa next steps.
- Hand Cash rental payments due at gear collection, or eSewa hosted checkout after an admin approves the request.
- A Gemini-powered Nepal trekking assistant and trek recommender that uses destination, experience level and season, with dated news/weather sources, an enforced topic boundary and account-aware replies for the signed-in user.
- User and admin login, protected admin pages, and admin-managed catalog information.
- Email-verified registration with a six-digit, time-limited OTP.
- Nepal-focused destination photography. See the site’s **Photo credits** page for source and license details.

Trek routes, prices, ratings, guide profiles, reviews and availability are sample project data. Confirm current route, permit, safety and price information with local authorities or registered providers before a trek. Trek bookings and rental requests remain subject to admin/provider confirmation. eSewa is configured for its UAT test environment by default; it does not collect real funds.

## Equipment rental and eSewa

1. A user requests equipment, dates, quantity and either **Hand Cash** or **eSewa**. eSewa requests require the user to be signed in with the same email address entered on the form.
2. An administrator opens **Admin Studio → Rentals** and chooses **Approve** or **Decline**. Approval checks overlapping approved rentals against equipment stock.
3. An approved Hand Cash request sends an email stating the amount due in cash when equipment is collected.
4. An approved eSewa request sends an email and makes **Pay with eSewa** available in **My account → Equipment Rentals**. The browser is sent to eSewa hosted checkout. Him-Path verifies the signed callback and asks eSewa to confirm the transaction before marking the request paid.

The default configuration uses eSewa’s published UAT merchant code and test secret. Test mode redirects to `https://rc-epay.esewa.com.np/api/epay/main/v2/form`. For live payments, eSewa must first issue a merchant product code and secret. Configure these environment variables on the server, and set the public URL to an HTTPS address reachable by the customer:

```dotenv
ESEWA_PRODUCT_CODE=your_issued_merchant_code
ESEWA_SECRET=your_issued_merchant_secret
ESEWA_CHECKOUT_URL=https://epay.esewa.com.np/api/epay/main/v2/form
ESEWA_STATUS_URL=https://epay.esewa.com.np/api/epay/transaction/status/
HIMPATH_PUBLIC_BASE_URL=https://your-public-domain.example
```

The published UAT merchant code/secret and test wallet credentials are for development only; use the official [eSewa ePay v2 documentation](https://developer.esewa.com.np/pages/Epay-V2) and [test credentials](https://developer.esewa.com.np/pages/Test-credentials). A local `localhost` deployment is suitable for local UAT browser redirects but is not a production callback/checkout deployment.

## Create an account and sign in

New account creation has three steps:

1. Choose **Create account**, enter a valid email, and request a code.
2. Enter the six-digit code sent to that email. It expires after 10 minutes; five incorrect attempts lock the code. A pending registration can request a fresh code.
3. Set a password of at least 8 characters, including uppercase and lowercase letters and a number.

The account is activated when password setup succeeds. Sign-in for existing users only needs an email/login ID and password; it does not ask for an OTP. Use the separate Admin login page for the admin account.

## Configure Gmail SMTP

Email verification requires a working SMTP connection. The bundled launcher reads local mail settings from `tools/mail.env`; this private file is ignored by Git. Start from `tools/mail.env.example` and enter your Gmail sender address and a Google App Password:

```dotenv
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your.sender@gmail.com
MAIL_PASSWORD=your-16-character-google-app-password
MAIL_FROM=your.sender@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

For Gmail, turn on 2-Step Verification and create an App Password in your Google Account. Use the App Password here, not the normal Gmail account password. Keep `MAIL_FROM` the same as the authenticated sender. Restart Him-Path after changing `tools/mail.env`.

The bundled launcher does not show a pretend local OTP: if SMTP is unavailable or the server rejects the message, signup reports a delivery error. Check the server window/logs and the recipient’s spam folder. A successful SMTP submission means Gmail accepted the message for delivery; it can still be filtered by the mailbox.

For another launch method, set the same `MAIL_*` environment variables in the server process. Never put the mail App Password in this README or commit `tools/mail.env`.

## Configure the Gemini trekking assistant

Him-Path uses the Gemini 3.5 Flash-Lite API model. Create an API key in [Google AI Studio](https://aistudio.google.com/apikey), copy `tools/ai.env.example` to `tools/ai.env`, and replace the placeholder:

```dotenv
GEMINI_API_KEY=your-google-ai-studio-api-key
GEMINI_MODEL=gemini-3.5-flash-lite
```

Keep `tools/ai.env` private. The launcher loads the key into the server environment, and the browser never receives it. Restart Him-Path after adding or changing the key. Without a configured key, trekking chat returns a clear setup message; profile answers and the topic boundary continue to work locally. For current access and safety questions, the server searches public Google News RSS headlines; it also requests a two-day, no-key Open-Meteo forecast for the named trailhead area. It sends these findings to Gemini as context and shows links under the chatbot answer. This avoids Gemini's separately priced built-in Search grounding. Google lists free-tier text input/output for Gemini 3.5 Flash-Lite; check the AI Studio usage page for current model limits and your key's tier. Open-Meteo's no-key endpoint is free for non-commercial use up to its published daily limit and does not guarantee uptime; provide attribution to Open-Meteo.

For Langtang and other live travel conditions, the chatbot must have a valid Gemini API key and working internet access. It checks available news headlines and weather model forecasts when asked; the RSS feed may omit important reports and the weather model is not an on-site observation. It cannot guarantee that a road remains passable after a report is published. Confirm urgent travel conditions with local authorities and the Department of Hydrology and Meteorology before departure.

## Database

The bundled database is MariaDB, with database name `him_path`. Him-Path uses port **3307** for its bundled instance so it can coexist with other local MySQL/MariaDB programs that use port 3306. The launcher starts MariaDB from `.runtime/data` and reuses it on later launches. Database credentials are stored locally under `.runtime`; do not share those runtime files.

The Spring Boot app applies `src/main/resources/schema.sql` and updates its JPA-managed tables at startup. `database/schema.sql` is a separate SQL bootstrap reference. Main data includes users and authorities, treks, route days, guides/guide knowledge, equipment, bookings, plans, rentals, orders, reviews and announcements.

Database connection overrides are `DB_URL`, `DB_USER` and `DB_PASSWORD`. The default local connection is `127.0.0.1:3307/him_path`. Back up `.runtime/data` while MariaDB is stopped if you need to preserve local project data.

## Main project layout

For a project overview, architecture explanation, setup walkthrough and presentation Q&A, see [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md).

```text
src/main/java/np/himalayantrails/   Java controllers, services, entities and repositories
src/main/resources/application.properties  Spring and database/email settings
src/main/resources/schema.sql       Database tables and migrations
src/main/resources/static/           HTML pages, CSS, JavaScript and photo credits
database/schema.sql                 Standalone database bootstrap script
tools/run-him-path.ps1              Windows launcher, bundled database and mail setup
tools/mail.env.example              SMTP configuration template
Start-Him-Path.bat                  Start app and open browser
Run-Him-Path-Server.bat             Run server in the current console
.runtime/                            Local Java, MariaDB, database and build cache
```

## API overview

The backend exposes JSON APIs; there is no separate API documentation site in this project.

- Public catalog: `GET /api/treks`, `GET /api/treks/{id}`, `GET /api/catalog/equipment`, `/api/catalog/guides`, `/api/catalog/announcements`, `GET /api/reviews`, `POST /api/guide/chat`.
- Registration: `POST /api/auth/register` with `{ "email": "name@example.com" }` sends the OTP; `POST /api/auth/verify-otp` with `{ "email": "name@example.com", "otp": "123456" }` checks it; `POST /api/auth/complete-registration` with `{ "email": "name@example.com", "otp": "123456", "password": "StrongPass1" }` sets the password and activates the account; `POST /api/auth/resend-verification` sends a replacement code.
- Sign-in: `POST /api/auth/login` accepts form fields `username` and `password`; `POST /api/auth/logout` ends the session; `GET /api/auth/me` returns the signed-in identity and role.
- User actions: `/api/plans`, `/api/bookings`, `/api/rentals`, `/api/orders`, `/api/reviews`.
- Admin actions (ADMIN role): `/api/admin/dashboard`, `/api/admin/users`, `/api/admin/treks`, `/api/admin/guides`, `/api/admin/routes`, `/api/admin/equipment`, `/api/admin/bookings`, `/api/admin/rentals`, `/api/admin/orders`, `/api/admin/reviews`, `/api/admin/announcements`, and guide knowledge management.

## Troubleshooting

- **The site does not open:** keep the server window open and check that port 8080 is free.
- **Database login error:** use `Start-Him-Path.bat` so the project’s bundled database starts on port 3307. Do not point it at another local MySQL database unless you have configured that database for Him-Path.
- **Verification email does not arrive:** confirm `tools/mail.env` is filled in, restart the server, and check Spam. The signup page displays the delivery error returned by the server.
- **It says the email is already registered:** sign in with that email, or use a different address. The administrator email is reserved for the admin account.
- **Already requested a code:** choose **Create account** and submit the same email again to resend a code for a still-pending registration.
- **Admin password was overridden:** an existing database account is not changed by `HIMPATH_ADMIN_PASSWORD`; that variable is for initial account creation only. The credentials table above is the password for this checkout’s current local database.

## Project limitations

Him-Path is a local prototype, not a live trekking operator. eSewa uses UAT by default; live payment processing requires merchant credentials and a deployed HTTPS server. Rental inventory is checked when an admin approves a request, but the system is not a production inventory/fulfillment service. The AI assistant requires a Google AI Studio API key; model access and free-tier limits are controlled by Google. The sample content and AI responses are informational demonstrations, not current travel or safety guarantees.

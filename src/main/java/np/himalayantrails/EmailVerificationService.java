package np.himalayantrails;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private final ObjectProvider<JavaMailSender> senders;
    private final JdbcTemplate db;
    private final String smtpHost;
    private final String from;
    private final boolean localFallbackEnabled;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(ObjectProvider<JavaMailSender> senders, JdbcTemplate db,
                                    @Value("${spring.mail.host:}") String smtpHost,
                                    @Value("${himpath.mail.from:}") String from,
                                    @Value("${himpath.email-verification.local-fallback:false}") boolean localFallbackEnabled) {
        this.senders = senders;
        this.db = db;
        this.smtpHost = smtpHost == null ? "" : smtpHost.trim();
        this.from = from == null ? "" : from.trim();
        this.localFallbackEnabled = localFallbackEnabled;
    }

    /** Sends a six-digit OTP, returning it only to the explicitly enabled loopback demo. */
    public String send(String email, boolean requestIsLoopback) {
        JavaMailSender sender = senders.getIfAvailable();
        boolean smtpReady = !smtpHost.isBlank() && !from.isBlank() && sender != null;
        boolean localDemo = !smtpReady && localFallbackEnabled && requestIsLoopback;
        if (!smtpReady && !localDemo) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification is not configured. Set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD and MAIL_FROM on the server.");
        }

        String otp = String.format("%06d", random.nextInt(1_000_000));
        db.update("DELETE FROM email_verifications WHERE username=?", email);
        db.update("INSERT INTO email_verifications(username,token_hash,expires_at,attempts) VALUES(?,?,?,0)",
                email, hash(otp), java.sql.Timestamp.from(Instant.now().plus(10, ChronoUnit.MINUTES)));
        if (localDemo) return otp;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Your Him-Path verification code");
        message.setText("Your Him-Path verification code is " + otp
                + ". It expires in 10 minutes. If you did not create this account, you can ignore this message.");
        try {
            sender.send(message);
        } catch (MailException | IllegalArgumentException ex) {
            log.error("Him-Path verification email delivery failed; host={}, from={}, recipient={}", smtpHost, from, email, ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "We could not send the verification code. Check the SMTP settings and try again.");
        }
        return null;
    }

    /** Returns the verified address, or null for an invalid, expired, or locked code. */
    public String verify(String email, String otp) {
        if (email == null || otp == null || !otp.matches("\\d{6}")) return null;
        List<Integer> attempts = db.query("SELECT attempts FROM email_verifications WHERE username=? AND expires_at>UTC_TIMESTAMP()",
                (rs, row) -> rs.getInt(1), email);
        if (attempts.isEmpty() || attempts.get(0) >= 5) return null;
        List<String> matches = db.query("SELECT username FROM email_verifications WHERE username=? AND token_hash=? AND expires_at>UTC_TIMESTAMP() AND attempts<5",
                (rs, row) -> rs.getString(1), email, hash(otp));
        if (!matches.isEmpty()) return matches.get(0);
        db.update("UPDATE email_verifications SET attempts=attempts+1 WHERE username=? AND expires_at>UTC_TIMESTAMP() AND attempts<5", email);
        return null;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}

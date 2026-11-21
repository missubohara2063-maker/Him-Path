package np.himalayantrails;

import java.util.Map;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashMap;
import java.net.InetAddress;
import java.security.SecureRandom;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$";
    private final UserDetailsManager users; private final PasswordEncoder encoder; private final JdbcTemplate db; private final EmailVerificationService verification;
    public AuthController(UserDetailsManager users,PasswordEncoder encoder,JdbcTemplate db,EmailVerificationService verification){this.users=users;this.encoder=encoder;this.db=db;this.verification=verification;}
    @Transactional
    @PostMapping("/register") public Map<String,String> register(@RequestBody Map<String,String> body,HttpServletRequest request){
        String username=normalizedEmail(body.get("email"));
        boolean existingPending=false;
        if(users.userExists(username)){
            List<Boolean> enabled=db.query("SELECT enabled FROM users WHERE username=?",(rs,row)->rs.getBoolean(1),username);
            if(enabled.isEmpty()||enabled.get(0))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account already exists for this email. Sign in with your password.");
            existingPending=true;
        }else{
            byte[] temporarySecret=new byte[32];RANDOM.nextBytes(temporarySecret);
            users.createUser(User.withUsername(username).password(encoder.encode(java.util.Base64.getEncoder().encodeToString(temporarySecret))).roles("USER").disabled(true).build());
        }
        String developmentOtp;
        try {
            developmentOtp=verification.send(username,isLoopback(request));
        } catch (RuntimeException ex) {
            if(!existingPending)users.deleteUser(username);
            throw ex;
        }
        Map<String,String> response=new HashMap<>();response.put("status",developmentOtp==null?"otp_sent":"local_otp");response.put("email",username);if(developmentOtp!=null)response.put("developmentOtp",developmentOtp);return response;
    }
    @Transactional
    @PostMapping("/resend-verification") public Map<String,String> resend(@RequestBody Map<String,String> body,HttpServletRequest request){
        String username=normalizedEmail(body.get("email"));
        List<Boolean> enabled=db.query("SELECT enabled FROM users WHERE username=?",(rs,row)->rs.getBoolean(1),username);
        if(enabled.isEmpty()||enabled.get(0))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No pending email verification was found. Start account creation again.");
        String developmentOtp=verification.send(username,isLoopback(request));
        if(developmentOtp!=null)return Map.of("status","local_otp","developmentOtp",developmentOtp);
        return Map.of("status","otp_sent");
    }
    @Transactional(noRollbackFor = ResponseStatusException.class)
    @PostMapping("/verify-otp") public Map<String,String> verify(@RequestBody Map<String,String> body){
        String username=normalizedEmail(body.get("email"));
        String otp=body.getOrDefault("otp","");
        String verifiedUsername=verification.verify(username,otp);
        if(verifiedUsername==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is invalid, expired, or locked after too many attempts. Request a new code and try again.");
        return Map.of("status","verified","email",verifiedUsername);
    }
    @Transactional(noRollbackFor = ResponseStatusException.class)
    @PostMapping("/complete-registration") public Map<String,String> completeRegistration(@RequestBody Map<String,String> body){
        String username=normalizedEmail(body.get("email"));String otp=body.getOrDefault("otp","");String password=body.getOrDefault("password","");
        int passwordBytes=password.getBytes(StandardCharsets.UTF_8).length;
        boolean strongEnough=password.length()>=8&&passwordBytes<=72&&password.chars().anyMatch(Character::isUpperCase)&&password.chars().anyMatch(Character::isLowerCase)&&password.chars().anyMatch(Character::isDigit);
        if(!strongEnough)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use at least 8 characters, including uppercase and lowercase letters and a number");
        List<Boolean> enabled=db.query("SELECT enabled FROM users WHERE username=?",(rs,row)->rs.getBoolean(1),username);
        if(enabled.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Start account creation and verify your email first");
        if(enabled.get(0))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account already exists for this email");
        String verifiedUsername=verification.verify(username,otp);
        if(verifiedUsername==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"That code is invalid, expired, or locked after too many attempts. Request a new code and try again.");
        var pending=users.loadUserByUsername(username);
        users.updateUser(User.withUserDetails(pending).password(encoder.encode(password)).disabled(false).build());
        db.update("DELETE FROM email_verifications WHERE username=?",username);
        return Map.of("status","registered","email",verifiedUsername);
    }
    @GetMapping("/me") public Map<String,String> me(Authentication auth){return Map.of("username",auth.getName(),"role",auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))?"ADMIN":"USER");}
    private static String normalizedEmail(String raw){String email=raw==null?"":raw.trim().toLowerCase(Locale.ROOT);if(email.length()>50||!email.matches(EMAIL_PATTERN)||email.contains("..")||email.startsWith(".")||email.contains(".@"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid email address (for example, name@example.com)");return email;}
    private static boolean isLoopback(HttpServletRequest request){try{return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();}catch(Exception ex){return false;}}
}

package np.himalayantrails;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    static final String PRIMARY_ADMIN_EMAIL = "missubohara2063@gmail.com";
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean JdbcUserDetailsManager userDetailsService(JdbcTemplate jdbc){
        JdbcUserDetailsManager manager=new JdbcUserDetailsManager(jdbc.getDataSource());
        manager.setUsersByUsernameQuery("select username,password,enabled from users where username = ?");
        manager.setAuthoritiesByUsernameQuery("select username,authority from authorities where username = ?");
        return manager;
    }
    @Bean CommandLineRunner seedAccounts(JdbcUserDetailsManager users,PasswordEncoder encoder,JdbcTemplate jdbc){return args->{
        // Preserve the existing primary-admin password while changing its login ID to the owner email.
        if(users.userExists("admin")){
            if(!users.userExists(PRIMARY_ADMIN_EMAIL)){
                jdbc.update("UPDATE users SET username=? WHERE username='admin'",PRIMARY_ADMIN_EMAIL);
                jdbc.update("UPDATE authorities SET username=? WHERE username='admin'",PRIMARY_ADMIN_EMAIL);
            }else{
                users.deleteUser("admin");
                jdbc.update("DELETE FROM authorities WHERE username='admin'");
                jdbc.update("DELETE FROM authorities WHERE username=?",PRIMARY_ADMIN_EMAIL);
                jdbc.update("INSERT INTO authorities(username,authority) VALUES (?, 'ROLE_ADMIN')",PRIMARY_ADMIN_EMAIL);
            }
        }
        if(users.userExists(PRIMARY_ADMIN_EMAIL)){
            jdbc.update("UPDATE users SET enabled=TRUE WHERE username=?",PRIMARY_ADMIN_EMAIL);
            jdbc.update("DELETE FROM authorities WHERE username=? AND authority='ROLE_USER'",PRIMARY_ADMIN_EMAIL);
            jdbc.update("INSERT INTO authorities(username,authority) SELECT ?, 'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE username=? AND authority='ROLE_ADMIN')",PRIMARY_ADMIN_EMAIL,PRIMARY_ADMIN_EMAIL);
        }else users.createUser(User.withUsername(PRIMARY_ADMIN_EMAIL).password(encoder.encode(System.getenv().getOrDefault("HIMPATH_ADMIN_PASSWORD","HimPath!Summit27#Lotus"))).roles("ADMIN").build());
        if(!users.userExists("demo"))users.createUser(User.withUsername("demo").password(encoder.encode(System.getenv().getOrDefault("HIMPATH_DEMO_PASSWORD","HimPathUser!2026"))).roles("USER").build());
    };}
    @Bean SecurityFilterChain security(HttpSecurity http)throws Exception{
        http.csrf(csrf->csrf.disable()).authorizeHttpRequests(auth->auth
            .requestMatchers("/", "/index.html", "/treks.html", "/guides.html", "/equipment.html", "/plan.html", "/user-login.html", "/verify-email.html", "/admin-login.html", "/styles.css", "/site.js", "/favicon.ico", "/api/treks", "/api/treks/**", "/api/guide/chat", "/api/catalog/**", "/api/auth/register", "/api/auth/resend-verification", "/api/auth/verify-otp", "/api/auth/complete-registration", "/error").permitAll()
            .requestMatchers("/admin.html", "/admin.css", "/admin.js").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET,"/api/reviews").permitAll()
            .requestMatchers(HttpMethod.POST,"/api/bookings", "/api/rentals").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/guide/knowledge", "/api/guide/knowledge/**").hasRole("ADMIN")
            .requestMatchers("/api/auth/register", "/api/auth/verify-otp", "/api/auth/complete-registration", "/api/auth/login", "/api/auth/logout").permitAll()
            .requestMatchers("/api/auth/**").authenticated()
            .requestMatchers("/api/bookings", "/api/bookings/**", "/api/rentals", "/api/rentals/**", "/api/orders", "/api/orders/**", "/api/reviews", "/api/reviews/**", "/api/plans", "/api/plans/**", "/api/guide-bookings", "/api/guide-bookings/**").authenticated()
            .anyRequest().permitAll())
            .exceptionHandling(errors->errors
                .authenticationEntryPoint((request,response,exception)->{
                    if(request.getRequestURI().startsWith("/api/"))response.sendError(HttpStatus.UNAUTHORIZED.value());
                    else response.sendRedirect("/admin-login.html");
                })
                .accessDeniedHandler((request,response,exception)->{
                    if(request.getRequestURI().startsWith("/api/"))response.sendError(HttpStatus.FORBIDDEN.value());
                    else response.sendRedirect("/admin-login.html?error=role");
                }))
            .formLogin(form->form.loginPage("/admin-login.html").loginProcessingUrl("/api/auth/login").usernameParameter("username").passwordParameter("password")
                .successHandler((request,response,auth)->{response.setStatus(200);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.getWriter().write("{\"status\":\"authenticated\",\"username\":\""+auth.getName().replace("\"","\\\"")+"\"}");})
                .failureHandler((request,response,exception)->{response.setStatus(401);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.getWriter().write("{\"error\":\"Invalid credentials\"}");}).permitAll())
            .logout(logout->logout.logoutUrl("/api/auth/logout").logoutSuccessHandler((request,response,auth)->response.setStatus(204)));
        return http.build();
    }
}

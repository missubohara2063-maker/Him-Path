package np.himalayantrails;

import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {
    private final JdbcTemplate db; private final TrekRepository treks; private final GuideKnowledgeRepository knowledge; private final UserDetailsManager users; private final ReservationEmailService emails;
    public AdminApiController(JdbcTemplate db,TrekRepository treks,GuideKnowledgeRepository knowledge,UserDetailsManager users,ReservationEmailService emails){this.db=db;this.treks=treks;this.knowledge=knowledge;this.users=users;this.emails=emails;}
    @GetMapping("/dashboard") public Map<String,Object> dashboard(){return Map.of("users",count("users"),"treks",treks.count(),"bookings",count("bookings"),"orders",count("orders"),"rentals",count("rentals"),"reviews",count("reviews"),"knowledge",knowledge.count(),"requestValue",sum("bookings")+sum("rentals")+sum("orders"));}
    @GetMapping("/users") public List<Map<String,Object>> users(){return db.queryForList("SELECT u.username,u.enabled,COALESCE(a.authority,'ROLE_USER') role FROM users u LEFT JOIN authorities a ON a.username=u.username");}
    @PatchMapping("/users/{username}/enabled") public Map<String,Object> setUserEnabled(@PathVariable String username,@RequestBody Map<String,Boolean> body){if(username.equalsIgnoreCase(SecurityConfig.PRIMARY_ADMIN_EMAIL)&&!Boolean.TRUE.equals(body.get("enabled")))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"The primary admin account cannot be disabled");UserDetails old=users.loadUserByUsername(username);boolean enabled=Boolean.TRUE.equals(body.get("enabled"));users.updateUser(User.withUserDetails(old).disabled(!enabled).build());return Map.of("username",username,"enabled",enabled);}
    @DeleteMapping("/users/{username}") public void deleteUser(@PathVariable String username){if(username.equalsIgnoreCase(SecurityConfig.PRIMARY_ADMIN_EMAIL))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"The primary admin account cannot be deleted");users.deleteUser(username);}
    @GetMapping("/bookings") public List<Map<String,Object>> bookings(){return db.queryForList("SELECT b.*,t.name trek FROM bookings b JOIN treks t ON t.id=b.trek_id ORDER BY b.created_at DESC");}
    @PatchMapping("/bookings/{id}/status") public Map<String,Object> bookingStatus(@PathVariable long id,@RequestBody Map<String,String> body){db.update("UPDATE bookings SET status=? WHERE id=?",body.getOrDefault("status","Requested"),id);return Map.of("id",id,"status",body.getOrDefault("status","Requested"));}
    @GetMapping("/orders") public List<Map<String,Object>> orders(){return db.queryForList("SELECT o.*,e.name product FROM orders o JOIN equipment e ON e.id=o.equipment_id ORDER BY o.created_at DESC");}
    @GetMapping("/rentals") public List<Map<String,Object>> rentals(){return db.queryForList("SELECT r.*,e.name item FROM rentals r JOIN equipment e ON e.id=r.equipment_id ORDER BY r.created_at DESC");}
    @Transactional
    @PatchMapping("/rentals/{id}/status") public Map<String,Object> rentalStatus(@PathVariable long id,@RequestBody Map<String,String> body){
        String status=body.getOrDefault("status","").trim();
        if(!Set.of("Approved","Declined").contains(status))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Choose Approved or Declined");
        Map<String,Object> rental;
        try{rental=db.queryForMap("SELECT r.*,e.name item FROM rentals r JOIN equipment e ON e.id=r.equipment_id WHERE r.id=?",id);}
        catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Rental request not found");}
        if(!"Requested".equalsIgnoreCase(String.valueOf(rental.get("status"))))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"This rental request has already been decided");
        if("Approved".equals(status)){
            int stock=db.queryForObject("SELECT stock FROM equipment WHERE id=? FOR UPDATE",Integer.class,rental.get("equipment_id"));
            Integer reserved=db.queryForObject("SELECT COALESCE(SUM(quantity),0) FROM rentals WHERE equipment_id=? AND status='Approved' AND start_date<=? AND end_date>=?",Integer.class,rental.get("equipment_id"),rental.get("end_date"),rental.get("start_date"));
            if(reserved+((Number)rental.get("quantity")).intValue()>stock)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Not enough available stock for these dates; decline this request or update the equipment inventory");
        }
        String paymentMethod=String.valueOf(rental.get("payment_method"));
        String paymentStatus="Approved".equals(status)?("Hand Cash".equalsIgnoreCase(paymentMethod)?"Due on collection":"Awaiting eSewa payment"):"Not applicable";
        if(db.update("UPDATE rentals SET status=?,payment_status=? WHERE id=? AND status='Requested'",status,paymentStatus,id)!=1)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"This rental request has already been decided");
        String emailStatus=emails.sendRentalDecision(String.valueOf(rental.get("customer_email")),String.valueOf(rental.get("customer_name")),String.valueOf(rental.get("item")),String.valueOf(rental.get("start_date")),String.valueOf(rental.get("end_date")),((Number)rental.get("quantity")).intValue(),((Number)rental.get("amount_npr")).intValue(),paymentMethod,status,null);
        db.update("UPDATE rentals SET decision_email_status=? WHERE id=?",emailStatus,id);
        return Map.of("id",id,"status",status,"paymentStatus",paymentStatus,"emailStatus",emailStatus);
    }
    @PatchMapping("/orders/{id}/status") public Map<String,Object> orderStatus(@PathVariable long id,@RequestBody Map<String,String> body){String status=body.getOrDefault("status","Requested");db.update("UPDATE orders SET status=? WHERE id=?",status,id);return Map.of("id",id,"status",status);}
    @PostMapping("/treks") public Trek addTrek(@RequestBody TrekBody body){return treks.save(new Trek(body.name(),body.region(),body.difficulty(),body.days(),body.maxAltitude(),body.price(),body.rating(),body.reviews(),body.description(),body.imageUrl()));}
    @PutMapping("/treks/{id}") public Trek editTrek(@PathVariable Long id,@RequestBody TrekBody body){Trek old=treks.findById(id).orElseThrow();db.update("UPDATE treks SET name=?,region=?,difficulty=?,days=?,max_altitude=?,price=?,rating=?,reviews=?,description=?,image_url=? WHERE id=?",body.name(),body.region(),body.difficulty(),body.days(),body.maxAltitude(),body.price(),body.rating(),body.reviews(),body.description(),body.imageUrl(),id);return treks.findById(old.getId()).orElseThrow();}
    @DeleteMapping("/treks/{id}") public void deleteTrek(@PathVariable Long id){treks.deleteById(id);}
    public record TrekBody(String name,String region,String difficulty,int days,int maxAltitude,int price,double rating,int reviews,String description,String imageUrl){}
    @GetMapping("/equipment") public List<Map<String,Object>> equipment(){return db.queryForList("SELECT * FROM equipment ORDER BY id");}
    @PostMapping("/equipment") public Map<String,Object> addEquipment(@RequestBody EquipmentBody b){db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)",b.name(),b.category(),b.price(),b.rentalPrice(),b.stock(),b.imageUrl(),b.status());return db.queryForMap("SELECT * FROM equipment WHERE id=LAST_INSERT_ID()");}
    @PutMapping("/equipment/{id}") public void editEquipment(@PathVariable long id,@RequestBody EquipmentBody b){db.update("UPDATE equipment SET name=?,category=?,price=?,rental_price=?,stock=?,image_url=?,status=? WHERE id=?",b.name(),b.category(),b.price(),b.rentalPrice(),b.stock(),b.imageUrl(),b.status(),id);}
    @DeleteMapping("/equipment/{id}") public void deleteEquipment(@PathVariable long id){db.update("DELETE FROM equipment WHERE id=?",id);}
    public record EquipmentBody(String name,String category,int price,int rentalPrice,int stock,String imageUrl,String status){}
    @GetMapping("/guides") public List<Map<String,Object>> guides(){return db.queryForList("SELECT * FROM trek_guides ORDER BY id");}
    @PostMapping("/guides") public void addGuide(@RequestBody GuideBody b){db.update("INSERT INTO trek_guides(name,region,experience,bio,languages,specialties,certifications,phone,email,rating,image_url,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",b.name(),b.region(),b.experience(),b.bio(),b.languages(),b.specialties(),b.certifications(),b.phone(),b.email(),b.rating(),b.imageUrl(),b.status());}
    @PutMapping("/guides/{id}") public void editGuide(@PathVariable long id,@RequestBody GuideBody b){db.update("UPDATE trek_guides SET name=?,region=?,experience=?,bio=?,languages=?,specialties=?,certifications=?,phone=?,email=?,rating=?,image_url=?,status=? WHERE id=?",b.name(),b.region(),b.experience(),b.bio(),b.languages(),b.specialties(),b.certifications(),b.phone(),b.email(),b.rating(),b.imageUrl(),b.status(),id);}
    @DeleteMapping("/guides/{id}") public void deleteGuide(@PathVariable long id){db.update("DELETE FROM trek_guides WHERE id=?",id);}
    public record GuideBody(String name,String region,String experience,String bio,String languages,String specialties,String certifications,String phone,String email,double rating,String imageUrl,String status){}
    @GetMapping("/guide-bookings") public List<Map<String,Object>> guideBookings(){return db.queryForList("SELECT b.*,g.phone guide_phone,g.email guide_email FROM guide_bookings b LEFT JOIN trek_guides g ON g.id=b.guide_id ORDER BY b.created_at DESC");}
    @PatchMapping("/guide-bookings/{id}/status") public Map<String,Object> guideBookingStatus(@PathVariable long id,@RequestBody Map<String,String> body){
        String status=body.getOrDefault("status","").toUpperCase(Locale.ROOT);
        if(!Set.of("APPROVED","DECLINED").contains(status))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Status must be APPROVED or DECLINED");
        Map<String,Object> booking;
        try{booking=db.queryForMap("SELECT b.*,g.name current_guide_name,g.phone guide_phone,g.email guide_email FROM guide_bookings b LEFT JOIN trek_guides g ON g.id=b.guide_id WHERE b.id=?",id);}catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Guide booking request not found");}
        String previous=String.valueOf(booking.get("status"));
        if(!previous.equals("REQUESTED"))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"This guide request has already been decided");
        String guideName=String.valueOf(booking.get("current_guide_name"));
        String phone=booking.get("guide_phone")==null?"":String.valueOf(booking.get("guide_phone"));
        String guideEmail=booking.get("guide_email")==null?"":String.valueOf(booking.get("guide_email"));
        if(status.equals("APPROVED")&&(phone.isBlank()||guideEmail.isBlank()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Add this guide's phone and email in Guide management before approving the request");
        if(db.update("UPDATE guide_bookings SET status=? WHERE id=? AND status='REQUESTED'",status,id)!=1)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"This guide request has already been decided");
        String emailStatus=emails.sendGuideBookingDecision(String.valueOf(booking.get("customer_email")),String.valueOf(booking.get("customer_name")),guideName,String.valueOf(booking.get("trek_name")),status,phone,guideEmail);
        db.update("UPDATE guide_bookings SET decision_email_status=? WHERE id=?",emailStatus,id);
        return Map.of("id",id,"status",status,"emailStatus",emailStatus);
    }
    @PostMapping("/guide-bookings/{id}/resend-email") public Map<String,Object> resendGuideBookingEmail(@PathVariable long id){
        Map<String,Object> booking;
        try{booking=db.queryForMap("SELECT b.*,g.name current_guide_name,g.phone guide_phone,g.email guide_email FROM guide_bookings b LEFT JOIN trek_guides g ON g.id=b.guide_id WHERE b.id=?",id);}catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Guide booking request not found");}
        String status=String.valueOf(booking.get("status"));
        if(!Set.of("APPROVED","DECLINED").contains(status))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Decide this request before sending an email");
        if("SENT".equalsIgnoreCase(String.valueOf(booking.get("decision_email_status"))))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"The decision email was already sent");
        String phone=booking.get("guide_phone")==null?"":String.valueOf(booking.get("guide_phone"));
        String guideEmail=booking.get("guide_email")==null?"":String.valueOf(booking.get("guide_email"));
        if(status.equals("APPROVED")&&(phone.isBlank()||guideEmail.isBlank()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Add the guide's phone and email before retrying this approval email");
        String emailStatus=emails.sendGuideBookingDecision(String.valueOf(booking.get("customer_email")),String.valueOf(booking.get("customer_name")),String.valueOf(booking.get("current_guide_name")),String.valueOf(booking.get("trek_name")),status,phone,guideEmail);
        db.update("UPDATE guide_bookings SET decision_email_status=? WHERE id=?",emailStatus,id);
        return Map.of("id",id,"status",status,"emailStatus",emailStatus);
    }
    @PostMapping("/announcements") public void addAnnouncement(@RequestBody AnnouncementBody b){db.update("INSERT INTO announcements(title,body,severity,published) VALUES(?,?,?,?)",b.title(),b.body(),b.severity(),b.published());}
    @PutMapping("/announcements/{id}") public void editAnnouncement(@PathVariable long id,@RequestBody AnnouncementBody b){db.update("UPDATE announcements SET title=?,body=?,severity=?,published=? WHERE id=?",b.title(),b.body(),b.severity(),b.published(),id);}
    @DeleteMapping("/announcements/{id}") public void deleteAnnouncement(@PathVariable long id){db.update("DELETE FROM announcements WHERE id=?",id);}
    public record AnnouncementBody(String title,String body,String severity,boolean published){}
    @GetMapping("/announcements") public List<Map<String,Object>> announcements(){return db.queryForList("SELECT * FROM announcements ORDER BY created_at DESC");}
    @GetMapping("/reviews") public List<Map<String,Object>> reviews(){return db.queryForList("SELECT r.*,t.name route FROM reviews r JOIN treks t ON t.id=r.trek_id ORDER BY r.created_at DESC");}
    @PatchMapping("/reviews/{id}/status") public Map<String,Object> reviewStatus(@PathVariable long id,@RequestBody Map<String,String> body){String status=body.getOrDefault("status","Pending review");db.update("UPDATE reviews SET status=? WHERE id=?",status,id);return Map.of("id",id,"status",status);}
    @GetMapping("/routes") public List<Map<String,Object>> routes(){return db.queryForList("SELECT r.*,t.name trek FROM trek_routes r JOIN treks t ON t.id=r.trek_id ORDER BY trek_id,day_number");}
    @PostMapping("/routes") public void addRoute(@RequestBody RouteBody b){db.update("INSERT INTO trek_routes(trek_id,day_number,title,description,distance_km,altitude_m) VALUES(?,?,?,?,?,?)",b.trekId(),b.dayNumber(),b.title(),b.description(),b.distanceKm(),b.altitudeM());}
    @PutMapping("/routes/{id}") public void editRoute(@PathVariable long id,@RequestBody RouteBody b){db.update("UPDATE trek_routes SET trek_id=?,day_number=?,title=?,description=?,distance_km=?,altitude_m=? WHERE id=?",b.trekId(),b.dayNumber(),b.title(),b.description(),b.distanceKm(),b.altitudeM(),id);}
    @DeleteMapping("/routes/{id}") public void deleteRoute(@PathVariable long id){db.update("DELETE FROM trek_routes WHERE id=?",id);}
    public record RouteBody(long trekId,int dayNumber,String title,String description,double distanceKm,int altitudeM){}
    private long count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);} private long sum(String table){return db.queryForObject("SELECT COALESCE(SUM(amount_npr),0) FROM "+table,Long.class);}
}

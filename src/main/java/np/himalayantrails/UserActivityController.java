package np.himalayantrails;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;

@RestController
public class UserActivityController {
    private static final Set<String> PAYMENT_METHODS = Set.of("eSewa", "Khalti", "Bank transfer", "Cash on confirmation");
    private static final Set<String> RENTAL_PAYMENT_METHODS = Set.of("eSewa", "Hand Cash");
    private final JdbcTemplate db;
    private final TrekRepository treks;
    private final ReservationEmailService emails;

    public UserActivityController(JdbcTemplate db, TrekRepository treks, ReservationEmailService emails) { this.db=db; this.treks=treks; this.emails=emails; }

    @PostMapping("/api/bookings")
    public Map<String,Object> booking(@RequestBody BookingBody b, Authentication auth) {
        Trek trek=treks.findById(b.trekId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Trek not found"));
        String name=required(b.customerName(),"Name",150), email=validEmail(b.customerEmail()), phone=validPhone(b.customerPhone());
        LocalDate start=parseDate(b.startDate(),"Start date");
        int travelers=b.travelers(); if(travelers<1||travelers>20)throw bad("Travelers must be between 1 and 20");
        String payment=validPayment(b.paymentMethod());
        long totalLong=(long)trek.getPrice()*travelers; if(totalLong>Integer.MAX_VALUE)throw bad("Booking total is too large"); int total=(int)totalLong;
        String sql="INSERT INTO bookings(username,customer_name,customer_email,customer_phone,trek_id,start_date,travelers,amount_npr,payment_method,payment_status) VALUES(?,?,?,?,?,?,?,?,?,'Pending')";
        long id=insert(sql, signedInUser(auth), name, email, phone, b.trekId(), Date.valueOf(start), travelers, total, payment);
        String emailStatus=emails.sendBookingConfirmation(id,email,name,trek.getName(),start.toString(),travelers,total,payment);
        return Map.of("bookingId",id,"status","Requested","amountNpr",total,"paymentMethod",payment,"paymentStatus","Pending","emailStatus",emailStatus);
    }

    @GetMapping("/api/bookings")
    public List<Map<String,Object>> bookings(Authentication auth) { return db.queryForList("SELECT b.id,t.name trek,b.start_date startDate,b.travelers,b.amount_npr amountNpr,b.status,b.payment_method paymentMethod,b.payment_status paymentStatus FROM bookings b JOIN treks t ON t.id=b.trek_id WHERE b.username=? ORDER BY b.created_at DESC", auth.getName()); }
    public record BookingBody(long trekId,String startDate,int travelers,String customerName,String customerEmail,String customerPhone,String paymentMethod){}

    @PostMapping("/api/plans") public Map<String,String> plan(@RequestBody PlanBody b,Authentication a){db.update("INSERT INTO trek_plans(username,trek_id,start_date,travelers,notes) VALUES(?,?,?,?,?)",a.getName(),b.trekId(),Date.valueOf(b.startDate()),b.travelers(),b.notes());return Map.of("status","Planning");}
    @GetMapping("/api/plans") public List<Map<String,Object>> plans(Authentication a){return db.queryForList("SELECT p.id,t.name trek,p.start_date startDate,p.travelers,p.notes,p.status FROM trek_plans p JOIN treks t ON t.id=p.trek_id WHERE p.username=? ORDER BY p.created_at DESC",a.getName());}
    public record PlanBody(long trekId,String startDate,int travelers,String notes){}

    @PostMapping("/api/rentals")
    public Map<String,Object> rental(@RequestBody RentalBody b,Authentication auth) {
        String name=required(b.customerName(),"Name",150), email=validEmail(b.customerEmail()), phone=validPhone(b.customerPhone());
        LocalDate start=parseDate(b.startDate(),"Start date"), end=parseDate(b.endDate(),"Return date");
        if(end.isBefore(start))throw bad("Return date must be on or after the start date");
        if(b.quantity()<1)throw bad("Quantity must be at least 1");
        String payment=validRentalPayment(b.paymentMethod());
        if("eSewa".equals(payment)&&(auth==null||!auth.isAuthenticated()||"anonymousUser".equals(auth.getName())))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in before requesting a rental with eSewa so the checkout is protected");
        if("eSewa".equals(payment)&&!auth.getName().equalsIgnoreCase(validEmail(b.customerEmail())))throw bad("For eSewa rentals, use the email address on your signed-in account");
        Map<String,Object> item;
        try { item=db.queryForMap("SELECT name,rental_price,stock FROM equipment WHERE id=? AND status='Available'",b.equipmentId()); }
        catch(EmptyResultDataAccessException ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rental item is unavailable");}
        int stock=((Number)item.get("stock")).intValue(); if(b.quantity()>stock)throw bad("Requested quantity unavailable");
        long days=ChronoUnit.DAYS.between(start,end)+1, amountLong=days*b.quantity()*((Number)item.get("rental_price")).longValue();
        if(amountLong>Integer.MAX_VALUE)throw bad("Rental total is too large"); int amount=(int)amountLong;
        String sql="INSERT INTO rentals(username,customer_name,customer_email,customer_phone,equipment_id,start_date,end_date,quantity,amount_npr,payment_method,payment_status) VALUES(?,?,?,?,?,?,?,?,?,?,'Pending')";
        long id=insert(sql,signedInUser(auth),name,email,phone,b.equipmentId(),Date.valueOf(start),Date.valueOf(end),b.quantity(),amount,payment);
        String emailStatus=emails.sendRentalConfirmation(id,email,name,String.valueOf(item.get("name")),start.toString(),end.toString(),b.quantity(),amount,payment);
        return Map.of("rentalId",id,"status","Requested","amountNpr",amount,"paymentMethod",payment,"paymentStatus","Pending","emailStatus",emailStatus);
    }
    @GetMapping("/api/rentals") public List<Map<String,Object>> rentals(Authentication a){return db.queryForList("SELECT r.id,e.name item,r.start_date startDate,r.end_date endDate,r.quantity,r.amount_npr amountNpr,r.payment_method paymentMethod,r.payment_status paymentStatus,r.status FROM rentals r JOIN equipment e ON e.id=r.equipment_id WHERE r.username=? ORDER BY r.created_at DESC",a.getName());}
    public record RentalBody(long equipmentId,String startDate,String endDate,int quantity,String customerName,String customerEmail,String customerPhone,String paymentMethod){}

    @PostMapping("/api/orders") public Map<String,Object> order(@RequestBody OrderBody b,Authentication a){Map<String,Object> e=db.queryForMap("SELECT price,stock FROM equipment WHERE id=? AND status='Available'",b.equipmentId());int qty=Math.max(1,b.quantity());if(qty>((Number)e.get("stock")).intValue())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not enough stock");int amount=qty*((Number)e.get("price")).intValue();db.update("INSERT INTO orders(username,equipment_id,quantity,amount_npr) VALUES(?,?,?,?)",a.getName(),b.equipmentId(),qty,amount);db.update("UPDATE equipment SET stock=stock-? WHERE id=?",qty,b.equipmentId());return Map.of("status","Requested","amountNpr",amount);}
    @GetMapping("/api/orders") public List<Map<String,Object>> orders(Authentication a){return db.queryForList("SELECT o.id,e.name product,o.quantity,o.amount_npr amountNpr,o.status,o.created_at createdAt FROM orders o JOIN equipment e ON e.id=o.equipment_id WHERE o.username=? ORDER BY o.created_at DESC",a.getName());}
    public record OrderBody(long equipmentId,int quantity){}

    @PostMapping("/api/reviews") public Map<String,String> review(@RequestBody ReviewBody b,Authentication a){if(b.rating()<1||b.rating()>5||b.text()==null||b.text().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Review and rating required");db.update("INSERT INTO reviews(username,trek_id,rating,review_text) VALUES(?,?,?,?)",a.getName(),b.trekId(),b.rating(),b.text());return Map.of("status","Pending review");}
    @GetMapping("/api/reviews") public List<Map<String,Object>> reviews(){return db.queryForList("SELECT r.id,r.username,t.name route,r.rating,r.review_text reviewText,r.created_at createdAt FROM reviews r JOIN treks t ON t.id=r.trek_id WHERE r.status='Published' ORDER BY r.created_at DESC");}
    public record ReviewBody(long trekId,int rating,String text){}

    private long insert(String sql,Object... values){KeyHolder keys=new GeneratedKeyHolder();db.update(connection->{PreparedStatement ps=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);for(int i=0;i<values.length;i++){Object value=values[i];if(value==null)ps.setNull(i+1,Types.VARCHAR);else ps.setObject(i+1,value);}return ps;},keys);if(keys.getKey()==null)throw new IllegalStateException("Database did not return the reservation id");return keys.getKey().longValue();}
    private static String signedInUser(Authentication a){return a==null||!a.isAuthenticated()||"anonymousUser".equals(a.getName())?null:a.getName();}
    private static String required(String value,String field,int max){if(value==null||value.isBlank()||value.trim().length()>max)throw bad(field+" is required");return value.trim();}
    private static String validEmail(String value){String email=required(value,"Email",190).toLowerCase();if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw bad("Enter a valid email address");return email;}
    private static String validPhone(String value){String phone=required(value,"Phone",30);if(!phone.matches("^[+0-9() -]{7,30}$"))throw bad("Enter a valid phone number");return phone;}
    private static LocalDate parseDate(String value,String field){try{LocalDate date=LocalDate.parse(value);if(date.isBefore(LocalDate.now()))throw bad(field+" cannot be in the past");return date;}catch(java.time.format.DateTimeParseException ex){throw bad("Enter a valid "+field.toLowerCase());}}
    private static String validPayment(String value){if(value==null)throw bad("Choose a valid payment method");String normalized=value.trim();if(normalized.equalsIgnoreCase("cash on confirmation")||normalized.equalsIgnoreCase("hand cash"))return "Hand Cash";if(normalized.equalsIgnoreCase("esewa"))return "eSewa";if(!PAYMENT_METHODS.contains(normalized))throw bad("Choose Hand Cash or eSewa");return normalized;}
    private static String validRentalPayment(String value){if(value==null)throw bad("Choose a valid payment method");String normalized=value.trim();if(normalized.equalsIgnoreCase("hand cash")||normalized.equalsIgnoreCase("cash on confirmation"))return "Hand Cash";if(normalized.equalsIgnoreCase("esewa"))return "eSewa";if(!RENTAL_PAYMENT_METHODS.contains(normalized))throw bad("Choose Hand Cash or eSewa");return normalized;}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}

package np.himalayantrails;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

@RestController
public class GuideBookingController {
    private final JdbcTemplate db;
    public GuideBookingController(JdbcTemplate db) { this.db = db; }

    @PostMapping("/api/guide-bookings")
    @PreAuthorize("hasRole('USER')")
    public Map<String,Object> request(@RequestBody BookingRequest request, Authentication auth) {
        String name=required(request.customerName(),"Name",150);
        String email=auth.getName();
        String phone=optional(request.customerPhone(),40);
        String trek=required(request.trekName(),"Trek",180);
        String notes=optional(request.notes(),1000);
        if(request.travelers()<1||request.travelers()>20)throw bad("Travelers must be between 1 and 20");
        LocalDate date;
        try{date=LocalDate.parse(required(request.startDate(),"Start date",10));}catch(RuntimeException ex){throw bad("Enter a valid start date");}
        if(date.isBefore(LocalDate.now()))throw bad("Start date cannot be in the past");
        Map<String,Object> guide;
        try{guide=db.queryForMap("SELECT id,name FROM trek_guides WHERE id=? AND status='Active'",request.guideId());}
        catch(EmptyResultDataAccessException ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"This guide is not available");}
        db.update("INSERT INTO guide_bookings(guide_id,guide_name,customer_name,customer_email,customer_phone,trek_name,start_date,travelers,notes,status) VALUES(?,?,?,?,?,?,?,?,?,'REQUESTED')",
                request.guideId(),guide.get("name"),name,email,phone,trek,Date.valueOf(date),request.travelers(),notes);
        Long id=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        return Map.of("bookingId",id,"status","REQUESTED","message","Your request was sent to Him-Path for review.");
    }
    private static String required(String value,String label,int max){if(value==null||value.isBlank()||value.trim().length()>max)throw bad(label+" is required (maximum "+max+" characters)");return value.trim();}
    private static String optional(String value,int max){if(value==null||value.isBlank())return null;if(value.trim().length()>max)throw bad("A provided detail is too long");return value.trim();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    public record BookingRequest(long guideId,String customerName,String customerPhone,String trekName,String startDate,int travelers,String notes){}
}

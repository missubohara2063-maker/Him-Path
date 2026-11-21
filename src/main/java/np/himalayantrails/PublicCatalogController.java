package np.himalayantrails;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
public class PublicCatalogController {
    private final JdbcTemplate db;
    public PublicCatalogController(JdbcTemplate db){this.db=db;}
    @GetMapping("/equipment") public List<Map<String,Object>> equipment(){return db.queryForList("SELECT id,name,category,price,rental_price rentalPrice,stock,image_url imageUrl,status FROM equipment WHERE status='Available' ORDER BY id");}
    @GetMapping("/guides") public List<Map<String,Object>> guides(){return db.queryForList("SELECT id,name,region,experience,bio,languages,specialties,certifications,rating,image_url imageUrl,status FROM trek_guides WHERE status='Active' ORDER BY id");}
    @GetMapping("/announcements") public List<Map<String,Object>> announcements(){return db.queryForList("SELECT id,title,body,severity,created_at createdAt FROM announcements WHERE published=TRUE ORDER BY created_at DESC");}
    @GetMapping("/routes/{trekId}") public List<Map<String,Object>> routes(@PathVariable long trekId){return db.queryForList("SELECT day_number dayNumber,title,description,distance_km distanceKm,altitude_m altitudeM FROM trek_routes WHERE trek_id=? ORDER BY day_number",trekId);}
}

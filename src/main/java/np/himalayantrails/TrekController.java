package np.himalayantrails;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/treks")
public class TrekController {
    private final TrekRepository treks;
    public TrekController(TrekRepository treks) { this.treks=treks; }
    @GetMapping public List<Trek> list(@RequestParam(required=false) String region, @RequestParam(required=false) String difficulty, @RequestParam(required=false) String q) {
        return treks.discover(blank(region), blank(difficulty), blank(q));
    }
    @GetMapping("/{id}") public Trek one(@PathVariable Long id) { return treks.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)); }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

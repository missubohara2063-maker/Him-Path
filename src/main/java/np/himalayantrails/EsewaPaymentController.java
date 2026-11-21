package np.himalayantrails;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EsewaPaymentController {
    private final EsewaPaymentService payments;
    public EsewaPaymentController(EsewaPaymentService payments){this.payments=payments;}
    @PostMapping("/api/rentals/{id}/esewa-checkout")
    public Map<String,Object> checkout(@PathVariable long id,Authentication auth){return payments.checkout(id,auth==null?null:auth.getName());}
    @GetMapping("/api/payments/esewa/success")
    public ResponseEntity<Void> success(@RequestParam("data") String data){return redirect("/equipment.html?payment="+(payments.complete(data)?"success":"pending"));}
    @GetMapping("/api/payments/esewa/failure")
    public ResponseEntity<Void> failure(@RequestParam(value="transaction_uuid",required=false) String uuid){payments.markFailed(uuid);return redirect("/equipment.html?payment=failed");}
    private static ResponseEntity<Void> redirect(String path){HttpHeaders headers=new HttpHeaders();headers.setLocation(URI.create(path));return new ResponseEntity<>(headers,HttpStatus.SEE_OTHER);}
}

package np.himalayantrails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class EsewaPaymentService {
    private static final String SIGNED_REQUEST_FIELDS = "total_amount,transaction_uuid,product_code";
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final RestClient http = RestClient.create();
    private final String productCode;
    private final String secret;
    private final String checkoutUrl;
    private final String statusUrl;
    private final String baseUrl;

    public EsewaPaymentService(JdbcTemplate db, ObjectMapper json,
            @Value("${himpath.esewa.product-code:EPAYTEST}") String productCode,
            @Value("${himpath.esewa.secret:8gBm/:&EnhH.1/q}") String secret,
            @Value("${himpath.esewa.checkout-url:https://rc-epay.esewa.com.np/api/epay/main/v2/form}") String checkoutUrl,
            @Value("${himpath.esewa.status-url:https://uat.esewa.com.np/api/epay/transaction/status/}") String statusUrl,
            @Value("${himpath.public-base-url:http://localhost:8080}") String baseUrl) {
        this.db=db; this.json=json; this.productCode=productCode; this.secret=secret;
        this.checkoutUrl=checkoutUrl; this.statusUrl=statusUrl; this.baseUrl=baseUrl.replaceAll("/+$", "");
    }

    public Map<String,Object> checkout(long rentalId,String username) {
        Map<String,Object> r=rental(rentalId);
        if(username==null||!username.equalsIgnoreCase(string(r.get("username"))))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sign in with the account that requested this rental");
        if(!"Approved".equalsIgnoreCase(string(r.get("status"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"The rental must be approved before payment");
        if(!"eSewa".equalsIgnoreCase(string(r.get("payment_method"))))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This rental was not requested with eSewa");
        if("Paid".equalsIgnoreCase(string(r.get("payment_status"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"This rental has already been paid");
        String uuid="HP-R"+rentalId+"-"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        String total=String.valueOf(((Number)r.get("amount_npr")).longValue());
        if(Long.parseLong(total)<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The rental total must be greater than zero for eSewa payment");
        if(db.update("UPDATE rentals SET esewa_transaction_uuid=?,payment_status='Payment initiated' WHERE id=? AND status='Approved' AND payment_status<>'Paid'",uuid,rentalId)!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"This rental is no longer available for payment");
        Map<String,Object> fields=new LinkedHashMap<>();
        fields.put("amount",total); fields.put("tax_amount","0"); fields.put("total_amount",total);
        fields.put("transaction_uuid",uuid); fields.put("product_code",productCode);
        fields.put("product_service_charge","0"); fields.put("product_delivery_charge","0");
        fields.put("success_url",baseUrl+"/api/payments/esewa/success");
        fields.put("failure_url",baseUrl+"/api/payments/esewa/failure?transaction_uuid="+uuid);
        fields.put("signed_field_names",SIGNED_REQUEST_FIELDS);
        fields.put("signature",signature("total_amount="+total+",transaction_uuid="+uuid+",product_code="+productCode));
        return Map.of("actionUrl",checkoutUrl,"fields",fields,"rentalId",rentalId);
    }

    public boolean complete(String encodedData) {
        try {
            Map<String,Object> response=json.readValue(Base64.getDecoder().decode(encodedData),new TypeReference<>(){});
            String uuid=string(response.get("transaction_uuid")),code=string(response.get("product_code"));
            if(!"COMPLETE".equalsIgnoreCase(string(response.get("status")))||!productCode.equals(code))return false;
            String signedNames=string(response.get("signed_field_names"));
            if(!verifyResponseSignature(response,signedNames))return false;
            Map<String,Object> stored;
            try{stored=db.queryForMap("SELECT id,amount_npr,status,payment_status FROM rentals WHERE esewa_transaction_uuid=? AND payment_method='eSewa'",uuid);}
            catch(org.springframework.dao.EmptyResultDataAccessException ex){return false;}
            String expected=String.valueOf(((Number)stored.get("amount_npr")).longValue());
            if(new BigDecimal(expected).compareTo(new BigDecimal(string(response.get("total_amount"))))!=0||!"Approved".equalsIgnoreCase(string(stored.get("status"))))return false;
            var statusUri=UriComponentsBuilder.fromUriString(statusUrl).queryParam("product_code",productCode).queryParam("total_amount",expected).queryParam("transaction_uuid",uuid).build().toUri();
            Map<?,?> verified=http.get().uri(statusUri).retrieve().body(Map.class);
            if(verified==null||!"COMPLETE".equalsIgnoreCase(string(verified.get("status"))))return false;
            Object verifiedAmount=verified.containsKey("total_amount")?verified.get("total_amount"):verified.get("totalAmount");
            if(verifiedAmount!=null&&new BigDecimal(expected).compareTo(new BigDecimal(normalizedAmount(verifiedAmount)))!=0)return false;
            String reference=string(response.get("transaction_code"));
            if("Paid".equalsIgnoreCase(string(stored.get("payment_status"))))return true;
            return db.update("UPDATE rentals SET payment_status='Paid',payment_reference=? WHERE esewa_transaction_uuid=? AND status='Approved' AND payment_status<>'Paid'",reference,uuid)==1;
        } catch(Exception ex) { return false; }
    }

    public void markFailed(String uuid) {
        if(uuid!=null&&uuid.matches("HP-R[0-9]+-[A-Fa-f0-9]{12}"))db.update("UPDATE rentals SET payment_status='Payment failed' WHERE esewa_transaction_uuid=? AND payment_method='eSewa' AND status='Approved' AND payment_status<>'Paid'",uuid);
    }

    public static String signature(String message,String secret) {
        try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception ex){throw new IllegalStateException("Could not sign eSewa request",ex);}
    }
    private String signature(String message){return signature(message,secret);}
    private boolean verifyResponseSignature(Map<String,Object> response,String signedNames){
        if(signedNames.isBlank()||signedNames.length()>300)return false;StringBuilder message=new StringBuilder();
        for(String field:signedNames.split(",")){if(!field.matches("[a-z_]+")||!response.containsKey(field))return false;if(message.length()>0)message.append(',');message.append(field).append('=').append(string(response.get(field)));}
        return MessageDigest.isEqual(signature(message.toString()).getBytes(StandardCharsets.UTF_8),string(response.get("signature")).getBytes(StandardCharsets.UTF_8));
    }
    private Map<String,Object> rental(long id){try{return db.queryForMap("SELECT r.id,r.username,r.amount_npr,r.status,r.payment_method,r.payment_status FROM rentals r WHERE r.id=?",id);}catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Rental not found");}}
    private static String string(Object value){return value==null?"":String.valueOf(value);}
    private static String normalizedAmount(Object value){return new BigDecimal(string(value)).stripTrailingZeros().toPlainString();}
}

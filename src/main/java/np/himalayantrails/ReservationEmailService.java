package np.himalayantrails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ReservationEmailService {
    private static final Logger log = LoggerFactory.getLogger(ReservationEmailService.class);
    private final ObjectProvider<JavaMailSender> senders;
    private final String smtpHost;
    private final String from;

    public ReservationEmailService(ObjectProvider<JavaMailSender> senders,
                                   @Value("${spring.mail.host:}") String smtpHost,
                                   @Value("${himpath.mail.from:}") String from) {
        this.senders = senders;
        this.smtpHost = smtpHost == null ? "" : smtpHost.trim();
        this.from = from == null ? "" : from.trim();
    }

    public String sendBookingConfirmation(long id, String to, String customerName, String trekName,
                                          String startDate, int travelers, int amountNpr, String paymentMethod) {
        String subject = "Him-Path trek request #" + id + " received";
        String body = "Hello " + customerName + ",\n\n"
                + "We received your trek booking request.\n\n"
                + "Trek: " + trekName + "\n"
                + "Preferred start date: " + startDate + "\n"
                + "Travelers: " + travelers + "\n"
                + "Estimated amount: NPR " + amountNpr + "\n"
                + "Payment preference: " + paymentMethod + "\n"
                + "Booking status: Pending confirmation\n\n"
                + "No payment has been charged. The booking provider will confirm availability and share payment instructions.\n\n"
                + "Thank you,\nHim-Path Nepal Trek Guide";
        return send(to, subject, body);
    }

    public String sendRentalConfirmation(long id, String to, String customerName, String itemName,
                                         String startDate, String endDate, int quantity, int amountNpr,
                                         String paymentMethod) {
        String subject = "Him-Path rental request #" + id + " received";
        String body = "Hello " + customerName + ",\n\n"
                + "We received your equipment rental request.\n\n"
                + "Equipment: " + itemName + "\n"
                + "Rental dates: " + startDate + " to " + endDate + "\n"
                + "Quantity: " + quantity + "\n"
                + "Estimated amount: NPR " + amountNpr + "\n"
                + "Payment preference: " + paymentMethod + "\n"
                + "Rental status: Pending confirmation\n\n"
                + "No payment has been charged. We will confirm availability and share payment instructions.\n\n"
                + "Thank you,\nHim-Path Nepal Trek Guide";
        return send(to, subject, body);
    }

    public String sendRentalDecision(String to, String customerName, String itemName,
                                     String startDate, String endDate, int quantity, int amountNpr,
                                     String paymentMethod, String status, String paymentUrl) {
        boolean approved = "Approved".equalsIgnoreCase(status);
        String subject = approved ? "Your Him-Path equipment rental is approved"
                : "Update on your Him-Path equipment rental";
        String body = "Hello " + customerName + ",\n\n"
                + (approved ? "Your equipment rental request has been approved.\n\n"
                            : "Your equipment rental request could not be approved.\n\n")
                + "Equipment: " + itemName + "\n"
                + "Rental dates: " + startDate + " to " + endDate + "\n"
                + "Quantity: " + quantity + "\n"
                + "Total: NPR " + amountNpr + "\n"
                + "Decision: " + status + "\n\n"
                + (approved && "Hand Cash".equalsIgnoreCase(paymentMethod)
                    ? "Payment method: Hand Cash. Please pay NPR " + amountNpr
                      + " in cash when you collect the approved equipment. No online payment is required.\n\n"
                    : approved && "eSewa".equalsIgnoreCase(paymentMethod)
                    ? "Payment method: eSewa. Sign in to your Him-Path account and open My account → Equipment Rentals to complete payment. The rental is not paid until eSewa confirms the transaction.\n\n"
                    : "You will not be charged for this request. Please contact Him-Path if you need help.\n\n")
                + "Thank you,\nHim-Path Nepal Trek Guide";
        return send(to, subject, body);
    }

    public String sendGuideBookingDecision(String to,String customerName,String guideName,String trekName,String status,String phone,String guideEmail) {
        boolean approved="APPROVED".equals(status);
        String subject=approved?"Your Him-Path guide request is approved":"Update on your Him-Path guide request";
        String body="Hello "+customerName+",\n\n"+(approved
                ?"Your request to select "+guideName+" as your upcoming trekking guide for "+trekName+" has been approved. You can consult with your guide for further discussion:\n\nGuide contact: "+(phone==null||phone.isBlank()?"Please contact Him-Path support for this detail":phone)+"\nGuide email: "+(guideEmail==null||guideEmail.isBlank()?"Please contact Him-Path support for this detail":guideEmail)+"\n\n"
                :"Your request for "+guideName+" for "+trekName+" was declined. Please explore other available guides or contact Him-Path for assistance.\n\n")
                +"Thank you,\nHim-Path Nepal Trek Guide";
        return send(to,subject,body);
    }

    private String send(String to, String subject, String body) {
        if (smtpHost.isBlank() || from.isBlank()) return "NOT_CONFIGURED";
        JavaMailSender sender = senders.getIfAvailable();
        if (sender == null) return "NOT_CONFIGURED";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            sender.send(message);
            return "SENT";
        } catch (MailException | IllegalArgumentException ex) {
            log.warn("Him-Path confirmation email was not sent ({})", ex.getClass().getSimpleName());
            return "FAILED";
        }
    }
}

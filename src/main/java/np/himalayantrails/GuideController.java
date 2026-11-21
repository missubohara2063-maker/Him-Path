package np.himalayantrails;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/guide")
public class GuideController {
    private final TrekRepository treks;
    private final GuideKnowledgeRepository knowledge;
    private final GeminiTrekkingService gemini;
    public GuideController(TrekRepository treks, GuideKnowledgeRepository knowledge, GeminiTrekkingService gemini) { this.treks=treks; this.knowledge=knowledge; this.gemini=gemini; }
    public record GuideRequest(String message) {}
    public record GuideResponse(String answer, List<Trek> recommendations, List<GeminiTrekkingService.GuideSource> sources) {}
    public record KnowledgeRequest(String question, String answer) {}

    @GetMapping("/knowledge") public List<GuideKnowledge> listKnowledge(){return knowledge.findAll();}
    @PostMapping("/knowledge") public GuideKnowledge addKnowledge(@RequestBody KnowledgeRequest request){
        if(request.question()==null || request.question().isBlank() || request.answer()==null || request.answer().isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Question and answer are required");
        return knowledge.save(new GuideKnowledge(request.question().trim(),request.answer().trim()));
    }
    @PutMapping("/knowledge/{id}") public GuideKnowledge editKnowledge(@PathVariable Long id,@RequestBody KnowledgeRequest request){
        if(request.question()==null || request.question().isBlank() || request.answer()==null || request.answer().isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Question and answer are required");
        GuideKnowledge item=knowledge.findById(id).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));item.update(request.question().trim(),request.answer().trim());return knowledge.save(item);
    }
    @DeleteMapping("/knowledge/{id}") public void deleteKnowledge(@PathVariable Long id){knowledge.deleteById(id);}

    @PostMapping("/chat")
    public GuideResponse chat(@RequestBody GuideRequest request, Authentication auth) {
        String question = request.message() == null ? "" : request.message().trim();
        if (question.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ask a trekking question to get started.");
        if (question.length() > 1200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please keep your message under 1,200 characters.");
        String personalReply = personalInfoReply(question, auth);
        if (personalReply != null) return new GuideResponse(personalReply, List.of(), List.of());
        if (!isTrekkingQuestion(question)) return new GuideResponse(
                "I can only help with trekking in Nepal, Him-Path trips and equipment. What would you like to know about planning a trek?", List.of(), List.of());
        String message = request.message() == null ? "" : request.message().toLowerCase(Locale.ROOT);
        List<Trek> all = treks.findAll();
        String region = message.contains("everest") ? "Everest" : message.contains("annapurna") || message.contains("poon") ? "Annapurna" : message.contains("langtang") ? "Langtang" : message.contains("manaslu") ? "Manaslu" : null;
        String difficulty = message.contains("beginner") || message.contains("easy") || message.contains("new to") ? "Easy" : message.contains("moderate") ? "Moderate" : message.contains("expert") || message.contains("experienced") || message.contains("challenging") || message.contains("hard") ? "Challenging" : null;
        Integer days = message.matches(".*\\b[1-9][0-9]?\\s*(days?|day)\\b.*") ? Integer.valueOf(message.replaceAll(".*?\\b([1-9][0-9]?)\\s*(days?|day)\\b.*", "$1")) : null;
        List<Trek> destinationMatches = all.stream().filter(t -> message.contains(t.getName().toLowerCase(Locale.ROOT)) || message.contains(t.getRegion().toLowerCase(Locale.ROOT))).toList();
        List<Trek> candidates = all;
        if (difficulty != null) {
            List<Trek> levelMatches = candidates.stream().filter(t -> t.getDifficulty().equalsIgnoreCase(difficulty)).toList();
            if (!levelMatches.isEmpty()) candidates = levelMatches;
        }
        List<Trek> locationMatches = region != null
                ? all.stream().filter(t -> t.getRegion().equalsIgnoreCase(region)).toList()
                : destinationMatches;
        if (!locationMatches.isEmpty()) {
            List<Trek> levelAndLocationMatches = candidates.stream().filter(locationMatches::contains).toList();
            if (!levelAndLocationMatches.isEmpty()) candidates = levelAndLocationMatches;
        }
        if (days != null) {
            List<Trek> durationMatches = candidates.stream().filter(t -> Math.abs(t.getDays()-days)<=4).toList();
            if (!durationMatches.isEmpty()) candidates = durationMatches;
        }
        List<Trek> chosen = candidates.stream().limit(3).toList();
        GeminiTrekkingService.GuideAnswer answer=gemini.answer(question, all, knowledge.findAll());
        return new GuideResponse(answer.answer(), chosen, answer.sources());
    }
    private String personalInfoReply(String question, Authentication auth) {
        String q = question.toLowerCase(Locale.ROOT);
        boolean asksAboutSelf = q.matches(".*\\b(my|mine|me|i am|i'm)\\b.*") &&
                q.matches(".*\\b(name|email|account|profile|role|username)\\b.*");
        if (!asksAboutSelf) return null;
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            return "Please sign in first so I can check your account details. I can’t access personal information for signed-out visitors.";
        boolean asksEmail = q.matches(".*\\b(email|username|login)\\b.*");
        boolean asksRole = q.matches(".*\\b(role|admin|account type)\\b.*");
        boolean asksName = q.matches(".*\\b(name)\\b.*");
        String email = auth.getName();
        String accountLabel = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        List<String> details = new ArrayList<>();
        if (asksEmail) details.add(email.contains("@") ? "email: " + email : "sign-in ID: " + email + " (no email address is saved for this account)");
        if (asksRole) details.add("role: " + (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")) ? "administrator" : "trekker"));
        if (asksName) details.add("a separate display name is not saved; account label: " + accountLabel);
        return "Your Him-Path profile: " + String.join("; ", details) + ". I only use the account details attached to your current sign-in.";
    }
    private boolean isTrekkingQuestion(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        if (q.matches("(?s).*\\b(hi|hello|hey|namaste|thanks|thank you)\\b.*")) return true;
        return q.matches("(?s).*(trek|trekking|hike|hiking|trail|mountain|himalaya|nepal|everest|annapurna|poon hill|manaslu|langtang|gokyo|panch pokhari|mardi|mustang|khopra|tsum|kanchenjunga|makalu|rara|guide|permit|altitude|acclimat|weather|safety|rescue|gear|equipment|backpack|sleeping bag|jacket|boots|shoe|pole|tent|rental|itinerary|route|travel|trip|plan|budget|cost|days|season|food|water|blister|packing|fitness|insurance|teahouse|lodging|altitude sickness).*");
    }
    private int score(String message,String question){int result=0;for(String word:question.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))if(word.length()>2&&message.contains(word))result++;return result;}
}

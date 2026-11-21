package np.himalayantrails;

import jakarta.persistence.*;

@Entity
@Table(name="guide_knowledge")
public class GuideKnowledge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=500) private String question;
    @Column(nullable=false, length=1500) private String answer;
    protected GuideKnowledge() {}
    public GuideKnowledge(String question,String answer){this.question=question;this.answer=answer;}
    public Long getId(){return id;} public String getQuestion(){return question;} public String getAnswer(){return answer;}
    public void update(String question,String answer){this.question=question;this.answer=answer;}
}

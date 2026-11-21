package np.himalayantrails;

import jakarta.persistence.*;

@Entity
@Table(name = "treks")
public class Trek {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String name;
    private String region;
    private String difficulty;
    private int days;
    private int maxAltitude;
    private int price;
    private double rating;
    private int reviews;
    @Column(length = 700) private String description;
    @Column(length = 700) private String imageUrl;

    protected Trek() {}
    public Trek(String name, String region, String difficulty, int days, int maxAltitude, int price, double rating, int reviews, String description, String imageUrl) {
        this.name=name; this.region=region; this.difficulty=difficulty; this.days=days; this.maxAltitude=maxAltitude; this.price=price; this.rating=rating; this.reviews=reviews; this.description=description; this.imageUrl=imageUrl;
    }
    public Long getId(){return id;} public String getName(){return name;} public String getRegion(){return region;}
    public String getDifficulty(){return difficulty;} public int getDays(){return days;} public int getMaxAltitude(){return maxAltitude;}
    public int getPrice(){return price;} public double getRating(){return rating;} public int getReviews(){return reviews;}
    public String getDescription(){return description;} public String getImageUrl(){return imageUrl;}
}

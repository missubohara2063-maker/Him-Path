package np.himalayantrails;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;

@Configuration
class DatabaseSeed {
    @Bean @Order(0) CommandLineRunner migrateGuestCheckout(JdbcTemplate db) { return args -> {
        db.execute("ALTER TABLE bookings MODIFY username VARCHAR(50) NULL");
        db.execute("ALTER TABLE rentals MODIFY username VARCHAR(50) NULL");
        ensureColumn(db,"bookings","customer_name","VARCHAR(150) NULL");
        ensureColumn(db,"bookings","customer_email","VARCHAR(190) NULL");
        ensureColumn(db,"bookings","customer_phone","VARCHAR(30) NULL");
        ensureColumn(db,"bookings","payment_method","VARCHAR(40) NULL");
        ensureColumn(db,"bookings","payment_status","VARCHAR(40) NOT NULL DEFAULT 'Pending'");
        ensureColumn(db,"rentals","customer_name","VARCHAR(150) NULL");
        ensureColumn(db,"rentals","customer_email","VARCHAR(190) NULL");
        ensureColumn(db,"rentals","customer_phone","VARCHAR(30) NULL");
        ensureColumn(db,"rentals","payment_method","VARCHAR(40) NULL");
        ensureColumn(db,"rentals","payment_status","VARCHAR(40) NOT NULL DEFAULT 'Pending'");
        ensureColumn(db,"rentals","esewa_transaction_uuid","VARCHAR(80) NULL");
        ensureColumn(db,"rentals","payment_reference","VARCHAR(100) NULL");
        ensureColumn(db,"rentals","decision_email_status","VARCHAR(30) NULL");
        ensureColumn(db,"trek_guides","bio","VARCHAR(1200) NULL");
        ensureColumn(db,"trek_guides","languages","VARCHAR(500) NULL");
        ensureColumn(db,"trek_guides","specialties","VARCHAR(700) NULL");
        ensureColumn(db,"trek_guides","certifications","VARCHAR(700) NULL");
        ensureColumn(db,"trek_guides","phone","VARCHAR(40) NULL");
        ensureColumn(db,"trek_guides","email","VARCHAR(190) NULL");
    }; }
    @Bean CommandLineRunner migrateNepalPhotos(JdbcTemplate db) { return args -> {
        String fallback = "https://commons.wikimedia.org/wiki/Special:FilePath/Everest_Base_Camp_Trek_Nepal.jpg?width=1600";
        db.update("UPDATE treks SET image_url=? WHERE image_url LIKE '%images.unsplash.com%'", fallback);
        String[][] trekPhotos = {
            {"Everest Base Camp", fallback},
            {"Annapurna Base Camp", "https://commons.wikimedia.org/wiki/Special:FilePath/Annapurna_III_during_Annapurna_circuit_trek.JPG?width=1200"},
            {"Ghorepani & Poon Hill", "https://commons.wikimedia.org/wiki/Special:FilePath/Poon_Hill_PANO_20180323_075733.jpg?width=1200"},
            {"Manaslu Circuit", "https://commons.wikimedia.org/wiki/Special:FilePath/Manaslu_Circuit_Trek.jpg?width=1400"},
            {"Langtang Valley", "https://commons.wikimedia.org/wiki/Special:FilePath/Langtang_Trek.jpg?width=1100"},
            {"Gokyo Lakes", "https://commons.wikimedia.org/wiki/Special:FilePath/Nepal_-_Sagamartha_Trek_-_Ngozumba_Gl_from_Gokyo_Ri.jpg?width=1400"},
            {"Panch Pokhari Trek", "/images/panch-pokhari-pillions.jpeg"},
            {"Mardi Himal Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/Memorable_Mardi_Himal_trek.jpg?width=1200"},
            {"Upper Mustang Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/Upper_Mustang_Lomanthang.jpg?width=1200"},
            {"Khopra Ridge Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/Khopra_Daada.jpg?width=1200"},
            {"Tsum Valley Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/Tsum_Valley.jpg?width=1200"},
            {"Kanchenjunga Base Camp Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/Kanchnjunga.jpg?width=1200"},
            {"Makalu Base Camp Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/SP_at_Makalu_Base_Camp_%283852179714%29.jpg?width=1200"},
            {"Rara Lake Trek", "https://commons.wikimedia.org/wiki/Special:FilePath/Rara_Lake,_Mugu.png?width=1200"}
        };
        for (String[] entry : trekPhotos) db.update("UPDATE treks SET image_url=? WHERE name=?", entry[1], entry[0]);
        db.update("UPDATE trek_guides SET image_url=NULL WHERE image_url LIKE '%images.unsplash.com%'");
    }; }
    private static void ensureColumn(JdbcTemplate db,String table,String column,String definition){
        Long count=db.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?",Long.class,table,column);
        if(count==null||count==0)db.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);
    }
    @Bean @Order(1) CommandLineRunner seed(TrekRepository repo) { return args -> {
        if (repo.count() == 0) {
            repo.save(new Trek("Everest Base Camp", "Everest", "Challenging", 14, 5364, 1290, 4.9, 128, "Walk through Sherpa villages and glacial valleys to the foot of the world's highest mountain.", "https://commons.wikimedia.org/wiki/Special:FilePath/Everest_Base_Camp_Trek_Nepal.jpg?width=1600"));
            repo.save(new Trek("Annapurna Base Camp", "Annapurna", "Moderate", 9, 4130, 890, 4.8, 96, "A classic mountain sanctuary trek through rhododendron forest, stone villages and a ring of high peaks.", "https://commons.wikimedia.org/wiki/Special:FilePath/Annapurna_III_during_Annapurna_circuit_trek.JPG?width=1200"));
            repo.save(new Trek("Ghorepani & Poon Hill", "Annapurna", "Easy", 5, 3210, 420, 4.9, 74, "A rewarding short trek with warm teahouses and a sunrise panorama across the Annapurna range.", "https://commons.wikimedia.org/wiki/Special:FilePath/Poon_Hill_PANO_20180323_075733.jpg?width=1200"));
            repo.save(new Trek("Manaslu Circuit", "Manaslu", "Challenging", 15, 5106, 1490, 4.9, 53, "Circle a giant Himalayan peak on a quieter route over the dramatic Larkya La pass.", "https://commons.wikimedia.org/wiki/Special:FilePath/Manaslu_Circuit_Trek.jpg?width=1400"));
            repo.save(new Trek("Langtang Valley", "Langtang", "Moderate", 8, 3870, 690, 4.7, 61, "Explore a welcoming high valley, yak pastures and broad views beneath Langtang Lirung.", "https://commons.wikimedia.org/wiki/Special:FilePath/Langtang_Trek.jpg?width=1100"));
            repo.save(new Trek("Gokyo Lakes", "Everest", "Challenging", 12, 5357, 1090, 4.8, 47, "Follow turquoise alpine lakes to a high viewpoint over Everest and the Ngozumpa Glacier.", "https://commons.wikimedia.org/wiki/Special:FilePath/Nepal_-_Sagamartha_Trek_-_Ngozumba_Gl_from_Gokyo_Ri.jpg?width=1400"));
        }
    }; }
    @Bean @Order(2) CommandLineRunner seedExpandedTreks(TrekRepository repo) { return args -> {
        String commons="https://commons.wikimedia.org/wiki/Special:FilePath/";
        Trek[] additions={
            new Trek("Panch Pokhari Trek","Sindhupalchok · Helambu","Moderate",16,4100,790,4.8,32,"A high-country pilgrimage route linking five sacred lakes with Sherpa villages and the Helambu trail.","/images/panch-pokhari-pillions.jpeg"),
            new Trek("Mardi Himal Trek","Annapurna · Kaski","Moderate",9,4200,690,4.8,41,"A ridge walk through forest and alpine pasture toward the close-up views of Machhapuchhre and Mardi Himal.",commons+"Memorable_Mardi_Himal_trek.jpg?width=1200"),
            new Trek("Upper Mustang Trek","Mustang · Gandaki","Moderate",14,3840,2490,4.9,38,"A rain-shadow journey through the Kali Gandaki gorge, ancient settlements and the walled city of Lo Manthang.",commons+"Upper_Mustang_Lomanthang.jpg?width=1200"),
            new Trek("Khopra Ridge Trek","Annapurna · Myagdi","Moderate",9,4660,790,4.8,29,"A community-lodge route over a quiet Annapurna ridge, with an optional demanding day hike to sacred Khayer Lake.",commons+"Khopra_,_Mt_Dhaulagiri_beyond_the_clouds.jpg?width=1200"),
            new Trek("Tsum Valley Trek","Manaslu · Gorkha","Challenging",17,3700,2190,4.9,25,"A culturally rich remote valley trek to Buddhist monasteries and villages beneath Ganesh Himal.",commons+"Tsum_Valley.jpg?width=1200"),
            new Trek("Kanchenjunga Base Camp Trek","Taplejung · Eastern Nepal","Expert",20,5143,3290,4.9,18,"A remote expedition-style circuit to the north and south base-camp viewpoints of the world’s third-highest mountain.",commons+"Kanchnjunga.jpg?width=1200"),
            new Trek("Makalu Base Camp Trek","Makalu-Barun · Sankhuwasabha","Expert",18,4870,2990,4.9,16,"A demanding eastern Nepal route from the Arun valley into the remote Barun Valley beneath Makalu.",commons+"SP_at_Makalu_Base_Camp_%283852179714%29.jpg?width=1200"),
            new Trek("Rara Lake Trek","Mugu · Karnali","Moderate",7,2990,1190,4.8,21,"A far-western Nepal escape to the clear waters and forested shores of Rara, Nepal’s largest lake.",commons+"Rara_Lake,_Mugu.png?width=1200")
        };
        for(Trek trek:additions) if(repo.findAll().stream().noneMatch(existing->existing.getName().equalsIgnoreCase(trek.getName()))) repo.save(trek);
    }; }
    @Bean CommandLineRunner seedGuideKnowledge(GuideKnowledgeRepository repo) { return args -> {
        if(repo.count()==0){
            repo.save(new GuideKnowledge("permits, permit, TIMS", "For many Nepal routes, trekkers need permits arranged before entering the trail. Rules can change by region and season, so confirm current requirements with the relevant Nepal tourism authority or a registered local agency before departure."));
            repo.save(new GuideKnowledge("altitude, altitude sickness, acclimatization", "Take altitude seriously: ascend gradually, plan acclimatization days, stay hydrated, and tell your guide immediately if you feel unwell. Severe or worsening symptoms need prompt descent and medical help."));
            repo.save(new GuideKnowledge("best season, weather, when to go", "Spring (roughly March–May) and autumn (roughly September–November) are popular trekking seasons in Nepal. Weather varies by region and year, so check a local forecast and trail conditions before setting out."));
        }
    }; }
    @Bean CommandLineRunner seedMarketplace(JdbcTemplate db) { return args -> {
        if(db.queryForObject("SELECT COUNT(*) FROM equipment",Long.class)==0){
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Alpine 55L Backpack","Backpacks",12800,850,8,"https://contents.mediadecathlon.com/p2986141/k%2402bd01f66ceafca934be29c1aad90e25/picture.jpg?f=1200x0&format=auto","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Down Sleeping Bag","Sleep systems",9800,650,5,"https://contents.mediadecathlon.com/p1798304/sq/k%2451f32c6420ae095767e7679522f00e6a/trekking-sleeping-bag-mt500-10-c-polyester-simond-8575966.jpg?f=480x480&format=auto","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Trail Trekking Boots","Footwear",14200,1100,7,"https://contents.mediadecathlon.com/p2020043/1cr1/k%24edfa6b358aabfd8a724dcb017be4437a/trekkingschuhe-trek-500-alltrail-hoch-lederwasserdicht-herren.jpg?f=1024x0&format=auto","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Insulated Down Jacket","Clothing",11500,900,4,"https://contents.mediadecathlon.com/p2688069/k%2483103683125b40f41a922ba2ecbb2da4/chaqueta-termica-trekking-montana-5c-trek-100-capucha-hombre.jpg?f=1200x0&format=auto","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Carbon Trekking Poles","Trail tools",4600,350,12,"https://contents.mediadecathlon.com/p2675326/k%24bb33b282a14f8cf269d89171e59cd388/baston-senderismo-trekking-forclaz-a200-ergonomico.jpg?f=800x0&format=auto","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","2-Person Mountain Tent","Shelter",19800,1500,3,"https://contents.mediadecathlon.com/p2754261/47f96ff5dfc96524cbf648786dfc2eaa/2-person-camping-tent-mh100-fresh-black.jpg?f=1200x0&format=auto&quality=70","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Rechargeable Headlamp","Lighting",2200,180,15,"https://contents.mediadecathlon.com/p3083300/k%241f9f9e31465e8072bf41c969cd1eea1f/sq/lampe-frontale-rechargeable-600-lumens-hl900-usb-v3-blanc.jpg?f=969x969&format=auto","Available");
            db.update("INSERT INTO equipment(name,category,price,rental_price,stock,image_url,status) VALUES(?,?,?,?,?,?,?)","Merino Trek Gloves","Accessories",1800,150,10,"https://contents.mediadecathlon.com/p3136818/k%24d6f0ff033e4e1915ea25d85c3a3604e6/1920x0/1250pt1156/2500xcr1406/default.jpg?format=auto","Available");
        }
        if(db.queryForObject("SELECT COUNT(*) FROM trek_guides",Long.class)==0){
            db.update("INSERT INTO trek_guides(name,region,experience,bio,languages,specialties,certifications,rating,image_url,status) VALUES(?,?,?,?,?,?,?,?,?,?)","Pasang Sherpa","Everest","12 years guiding","Sample Him-Path profile; confirm availability, background and route experience before booking.","Nepali, Sherpa, English (sample listing; confirm)","Everest Base Camp, Khumbu and high-altitude teahouse routes (sample listing)","Credentials are not verified in this demo; confirm before booking.",4.9,null,"Active");
            db.update("INSERT INTO trek_guides(name,region,experience,bio,languages,specialties,certifications,rating,image_url,status) VALUES(?,?,?,?,?,?,?,?,?,?)","Maya Gurung","Annapurna","8 years guiding","Sample Him-Path profile; confirm availability, background and route experience before booking.","Nepali, English (sample listing; confirm)","Annapurna region, village trails and lower-altitude routes (sample listing)","Credentials are not verified in this demo; confirm before booking.",5.0,null,"Active");
            db.update("INSERT INTO trek_guides(name,region,experience,bio,languages,specialties,certifications,rating,image_url,status) VALUES(?,?,?,?,?,?,?,?,?,?)","Tenzin Lama","Langtang & Manaslu","10 years guiding","Sample Him-Path profile; confirm availability, background and route experience before booking.","Nepali, Tamang, English (sample listing; confirm)","Langtang and Manaslu region routes (sample listing)","Credentials are not verified in this demo; confirm before booking.",4.9,null,"Active");
        }
        if(db.queryForObject("SELECT COUNT(*) FROM announcements",Long.class)==0)db.update("INSERT INTO announcements(title,body,severity,published) VALUES(?,?,?,TRUE)","Trail safety reminder","Check current weather and permits before your trek. Acclimatize gradually and seek help promptly if altitude symptoms worsen.","Safety");
    }; }
    @Bean @Order(2) CommandLineRunner completeDemoGuideProfiles(JdbcTemplate db) { return args -> {
        String[][] profiles={
            {"Pasang Sherpa","Sample Him-Path profile; confirm availability, background and route experience before booking.","Nepali, Sherpa, English (sample listing; confirm)","Everest Base Camp, Khumbu and high-altitude teahouse routes (sample listing)","Credentials are not verified in this demo; confirm before booking."},
            {"Maya Gurung","Sample Him-Path profile; confirm availability, background and route experience before booking.","Nepali, English (sample listing; confirm)","Annapurna region, village trails and lower-altitude routes (sample listing)","Credentials are not verified in this demo; confirm before booking."},
            {"Tenzin Lama","Sample Him-Path profile; confirm availability, background and route experience before booking.","Nepali, Tamang, English (sample listing; confirm)","Langtang and Manaslu region routes (sample listing)","Credentials are not verified in this demo; confirm before booking."}
        };
        for(String[] p:profiles)db.update("UPDATE trek_guides SET bio=COALESCE(NULLIF(bio,''),?),languages=COALESCE(NULLIF(languages,''),?),specialties=COALESCE(NULLIF(specialties,''),?),certifications=COALESCE(NULLIF(certifications,''),?) WHERE name=?",p[1],p[2],p[3],p[4],p[0]);
    }; }
}

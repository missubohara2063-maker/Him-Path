package np.himalayantrails;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrekRepository extends JpaRepository<Trek, Long> {
    @Query("select t from Trek t where (:region is null or lower(t.region)=lower(:region)) and (:difficulty is null or lower(t.difficulty)=lower(:difficulty)) and (:q is null or lower(t.name) like lower(concat('%',:q,'%')) or lower(t.region) like lower(concat('%',:q,'%')))")
    List<Trek> discover(@Param("region") String region, @Param("difficulty") String difficulty, @Param("q") String q);
}

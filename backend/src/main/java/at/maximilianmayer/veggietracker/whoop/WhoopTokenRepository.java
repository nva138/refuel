package at.maximilianmayer.veggietracker.whoop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhoopTokenRepository extends JpaRepository<WhoopToken, Long> {
}

package src.main.java.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import src.main.java.entities.RecommendationRequest;

@Repository
@RepositoryRestResource(exported = false)
public interface RecommendationRequestRepository
    extends CrudRepository<RecommendationRequest, Long> {}

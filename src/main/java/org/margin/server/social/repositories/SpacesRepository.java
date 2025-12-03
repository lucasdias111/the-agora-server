package org.margin.server.social.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.margin.server.social.models.Space;

import java.util.List;

@Repository
public interface SpacesRepository extends JpaRepository<Space, Long> {
    @Query("SELECT sp FROM Space sp")
    List<Space> getSpaces();
}

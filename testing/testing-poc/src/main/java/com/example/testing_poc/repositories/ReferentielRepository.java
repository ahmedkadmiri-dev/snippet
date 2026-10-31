package com.example.testing_poc.repositories;

import com.example.testing_poc.entities.Referentiel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReferentielRepository extends JpaRepository<Referentiel, Long> {

    List<Referentiel> findByTypeIgnoreCase(String type);

    boolean existsByTypeIgnoreCaseAndCodeIgnoreCase(String type, String code);
}
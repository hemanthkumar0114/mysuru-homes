package com.realestate.api.locality;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalityPageRepository extends JpaRepository<LocalityPage, String> {
    List<LocalityPage> findAllByOrderByLocalityNameAsc();
}

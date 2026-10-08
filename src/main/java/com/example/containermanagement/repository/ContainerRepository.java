package com.example.containermanagement.repository;

import com.example.containermanagement.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository layer for Container entities.
 *
 * Extending JpaRepository gives us save, findById, findAll, deleteById,
 * count, etc. for free - no implementation required.
 *
 * Search-related finder methods are declared here now (per project scope)
 * so they exist ahead of the Search module being wired up in the
 * controller/service layers.
 */
public interface ContainerRepository extends JpaRepository<Container, Long> {

    /**
     * Case-insensitive partial match search by truck number.
     */
    List<Container> findByTruckNumberContainingIgnoreCase(String truckNumber);

    /**
     * Case-insensitive partial match search by vendor code.
     */
    List<Container> findByVendorCodeContainingIgnoreCase(String vendorCode);

}

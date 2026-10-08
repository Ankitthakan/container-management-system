package com.example.containermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents a single truck container record.
 *
 * Maps to the "container" table in MySQL. Hibernate will auto-create
 * this table on application startup (ddl-auto=update).
 *
 * Indexes on truck_number and vendor_code speed up the search feature
 * (ContainerRepository.findByTruckNumberContainingIgnoreCase / findByVendorCodeContainingIgnoreCase),
 * which would otherwise require a full table scan on every search.
 *
 * Volume is always calculated in the service layer as
 * length x width x height. It is never accepted directly from user input.
 */
@Entity
@Table(name = "container", indexes = {
        @Index(name = "idx_truck_number", columnList = "truck_number"),
        @Index(name = "idx_vendor_code", columnList = "vendor_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Truck number cannot be empty")
    @Column(name = "truck_number", nullable = false, length = 50)
    private String truckNumber;

    @NotBlank(message = "Vendor code cannot be empty")
    @Column(name = "vendor_code", nullable = false, length = 50)
    private String vendorCode;

    @Positive(message = "Length must be greater than zero")
    @Column(name = "length", nullable = false, precision = 10, scale = 2)
    private BigDecimal length;

    @Positive(message = "Width must be greater than zero")
    @Column(name = "width", nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @Positive(message = "Height must be greater than zero")
    @Column(name = "height", nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    /**
     * Always computed as length x width x height before persisting.
     * Never set directly from a form or API request body.
     */
    @Column(name = "volume", nullable = false, precision = 15, scale = 2)
    private BigDecimal volume;

}

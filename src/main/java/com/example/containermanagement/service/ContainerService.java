package com.example.containermanagement.service;

import com.example.containermanagement.dto.ImportResult;
import com.example.containermanagement.entity.Container;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * Service layer contract for Container business logic.
 *
 * Sits between the Controller and Repository layers. Any volume
 * calculation and business rule enforcement happens in the
 * implementation of this interface, never in the controller.
 */
public interface ContainerService {

    Container addContainer(Container container);

    List<Container> getAllContainers();

    Container getContainerById(Long id);

    Container updateContainer(Long id, Container container);

    void deleteContainer(Long id);

    List<Container> searchByTruckNumber(String truckNumber);

    List<Container> searchByVendorCode(String vendorCode);

    ByteArrayInputStream exportToExcel();

    ImportResult importFromExcel(MultipartFile file);

}

package com.example.containermanagement.service;

import com.example.containermanagement.entity.Container;
import com.example.containermanagement.exception.ResourceNotFoundException;
import com.example.containermanagement.repository.ContainerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContainerServiceImplTest {

    @Mock
    private ContainerRepository containerRepository;

    @InjectMocks
    private ContainerServiceImpl containerService;


    // TEST 1: Add container and calculate volume
    @Test
    void addContainer_ShouldCalculateVolumeAndSaveContainer() {

        Container container = new Container();

        container.setTruckNumber("HR26AB1234");
        container.setVendorCode("V001");
        container.setLength(new BigDecimal("10"));
        container.setWidth(new BigDecimal("5"));
        container.setHeight(new BigDecimal("2"));

        when(containerRepository.save(any(Container.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Container result = containerService.addContainer(container);

        assertEquals(
                new BigDecimal("100"),
                result.getVolume()
        );

        verify(containerRepository, times(1))
                .save(container);
    }


    // TEST 2: Get container by ID
    @Test
    void getContainerById_ShouldReturnContainer() {

        Container container = new Container();

        container.setTruckNumber("HR26AB1234");
        container.setVendorCode("V001");

        when(containerRepository.findById(1L))
                .thenReturn(Optional.of(container));

        Container result = containerService.getContainerById(1L);

        assertNotNull(result);
        assertEquals("HR26AB1234", result.getTruckNumber());
        assertEquals("V001", result.getVendorCode());

        verify(containerRepository, times(1))
                .findById(1L);
    }


    // TEST 3: Get container with invalid ID
    @Test
    void getContainerById_WhenContainerDoesNotExist_ShouldThrowException() {

        when(containerRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> containerService.getContainerById(999L)
        );

        verify(containerRepository, times(1))
                .findById(999L);
    }


    // TEST 4: Update container and recalculate volume
    @Test
    void updateContainer_ShouldUpdateDataAndRecalculateVolume() {

        Container existing = new Container();

        existing.setTruckNumber("HR26AB1234");
        existing.setVendorCode("V001");
        existing.setLength(new BigDecimal("10"));
        existing.setWidth(new BigDecimal("5"));
        existing.setHeight(new BigDecimal("2"));
        existing.setVolume(new BigDecimal("100"));

        Container updatedData = new Container();

        updatedData.setTruckNumber("HR26XY9999");
        updatedData.setVendorCode("V002");
        updatedData.setLength(new BigDecimal("20"));
        updatedData.setWidth(new BigDecimal("5"));
        updatedData.setHeight(new BigDecimal("2"));

        when(containerRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(containerRepository.save(any(Container.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Container result =
                containerService.updateContainer(1L, updatedData);

        assertEquals("HR26XY9999", result.getTruckNumber());
        assertEquals("V002", result.getVendorCode());

        // 20 × 5 × 2 = 200
        assertEquals(
                new BigDecimal("200"),
                result.getVolume()
        );

        verify(containerRepository, times(1))
                .save(existing);
    }


    // TEST 5: Delete container
    @Test
    void deleteContainer_ShouldDeleteContainer() {

        Container container = new Container();

        container.setTruckNumber("HR26AB1234");
        container.setVendorCode("V001");

        when(containerRepository.findById(1L))
                .thenReturn(Optional.of(container));

        containerService.deleteContainer(1L);

        verify(containerRepository, times(1))
                .delete(container);
    }


    // TEST 6: Search containers by truck number
    @Test
    void searchByTruckNumber_ShouldReturnMatchingContainers() {

        Container container = new Container();

        container.setTruckNumber("HR26AB1234");
        container.setVendorCode("V001");

        List<Container> containers =
                Arrays.asList(container);

        when(containerRepository
                .findByTruckNumberContainingIgnoreCase("HR26AB"))
                .thenReturn(containers);

        List<Container> result =
                containerService.searchByTruckNumber("HR26AB");

        assertEquals(1, result.size());
        assertEquals(
                "HR26AB1234",
                result.get(0).getTruckNumber()
        );

        verify(containerRepository, times(1))
                .findByTruckNumberContainingIgnoreCase("HR26AB");
    }


    // TEST 7: Search containers by vendor code
    @Test
    void searchByVendorCode_ShouldReturnMatchingContainers() {

        Container container = new Container();

        container.setTruckNumber("HR26AB1234");
        container.setVendorCode("V001");

        List<Container> containers =
                Arrays.asList(container);

        when(containerRepository
                .findByVendorCodeContainingIgnoreCase("V001"))
                .thenReturn(containers);

        List<Container> result =
                containerService.searchByVendorCode("V001");

        assertEquals(1, result.size());
        assertEquals(
                "V001",
                result.get(0).getVendorCode()
        );

        verify(containerRepository, times(1))
                .findByVendorCodeContainingIgnoreCase("V001");
    }
}
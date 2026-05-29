package org.citycare.facilityservice.servicesImpl;

import org.citycare.facilityservice.dto.request.FacilityRequest;
import org.citycare.facilityservice.dto.response.FacilityResponse;
import org.citycare.facilityservice.dto.response.StaffResponse;
import org.citycare.facilityservice.entities.Facility;
import org.citycare.facilityservice.entities.Staff;
import org.citycare.facilityservice.exceptions.ResourceNotFoundException;
import org.citycare.facilityservice.repositories.FacilityRepository;
import org.citycare.facilityservice.repositories.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityServiceImplTest {

    @Mock FacilityRepository facilityRepository;
    @Mock StaffRepository staffRepository;

    @InjectMocks FacilityServiceImpl facilityService;

    private Facility mockFacility;
    private Staff mockStaff;

    @BeforeEach
    void setUp() {
        mockFacility = Facility.builder()
                .facilityId(1L).name("City Hospital")
                .type(Facility.Type.HOSPITAL).location("Bangalore")
                .capacity(100).status(Facility.Status.ACTIVE).build();

        mockStaff = Staff.builder()
                .staffId(1L).name("Dr. Smith")
                .role(Staff.Role.DOCTOR).contactInfo("9876543210")
                .facility(mockFacility).status(Staff.Status.ACTIVE).build();
    }

    @Test
    void createFacility_success() {
        FacilityRequest req = new FacilityRequest();
        req.setName("City Hospital"); req.setType(Facility.Type.HOSPITAL);
        req.setLocation("Bangalore"); req.setCapacity(100);
        req.setStatus(Facility.Status.ACTIVE);

        when(facilityRepository.save(any())).thenReturn(mockFacility);

        FacilityResponse result = facilityService.createFacility(req);

        assertThat(result.getName()).isEqualTo("City Hospital");
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(facilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_success() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(mockFacility));

        FacilityResponse result = facilityService.getById(1L);

        assertThat(result.getFacilityId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("City Hospital");
    }

    @Test
    void getAll_returnsMappedList() {
        when(facilityRepository.findAll()).thenReturn(List.of(mockFacility));

        List<FacilityResponse> result = facilityService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void updateFacility_notFound_throwsResourceNotFoundException() {
        when(facilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.updateFacility(99L, new FacilityRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateFacility_success() {
        FacilityRequest req = new FacilityRequest();
        req.setName("Updated Hospital"); req.setType(Facility.Type.CLINIC);
        req.setLocation("Mysore"); req.setCapacity(50);
        req.setStatus(Facility.Status.ACTIVE);

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(mockFacility));
        when(facilityRepository.save(any())).thenReturn(mockFacility);

        FacilityResponse result = facilityService.updateFacility(1L, req);

        assertThat(result).isNotNull();
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    void updateStatus_success() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(mockFacility));
        when(facilityRepository.save(any())).thenReturn(mockFacility);

        FacilityResponse result = facilityService.updateStatus(1L, Facility.Status.INACTIVE);

        assertThat(result).isNotNull();
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    void getByStatus_returnsMappedList() {
        when(facilityRepository.findByStatus(Facility.Status.ACTIVE)).thenReturn(List.of(mockFacility));

        List<FacilityResponse> result = facilityService.getByStatus(Facility.Status.ACTIVE);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByType_returnsMappedList() {
        when(facilityRepository.findByType(Facility.Type.HOSPITAL)).thenReturn(List.of(mockFacility));

        List<FacilityResponse> result = facilityService.getByType(Facility.Type.HOSPITAL);

        assertThat(result).hasSize(1);
    }

    @Test
    void getStaffByFacility_facilityNotFound_throwsResourceNotFoundException() {
        when(facilityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> facilityService.getStaffByFacility(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStaffByFacility_success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(staffRepository.findByFacilityFacilityId(1L)).thenReturn(List.of(mockStaff));

        List<StaffResponse> result = facilityService.getStaffByFacility(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Dr. Smith");
    }
}

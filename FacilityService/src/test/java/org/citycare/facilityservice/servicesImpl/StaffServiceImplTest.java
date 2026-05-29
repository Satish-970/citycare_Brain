package org.citycare.facilityservice.servicesImpl;

import org.citycare.facilityservice.client.IamClient;
import org.citycare.facilityservice.dto.request.StaffRequest;
import org.citycare.facilityservice.dto.response.ApiResponse;
import org.citycare.facilityservice.dto.response.StaffResponse;
import org.citycare.facilityservice.dto.response.UserResponse;
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
class StaffServiceImplTest {

    @Mock StaffRepository staffRepository;
    @Mock FacilityRepository facilityRepository;
    @Mock IamClient iamClient;

    @InjectMocks StaffServiceImpl staffService;

    private Facility mockFacility;
    private Staff mockStaff;

    @BeforeEach
    void setUp() {
        mockFacility = Facility.builder()
                .facilityId(1L).name("City Hospital")
                .type(Facility.Type.HOSPITAL).location("Bangalore")
                .capacity(100).status(Facility.Status.ACTIVE).build();

        mockStaff = Staff.builder()
                .staffId(10L).name("Dr. Smith")
                .role(Staff.Role.DOCTOR).contactInfo("9876543210")
                .facility(mockFacility).status(Staff.Status.ACTIVE).build();
    }

    @Test
    void createStaff_success() {
        StaffRequest req = new StaffRequest();
        req.setName("Dr. Smith"); req.setFacilityId(1L);
        req.setRole(Staff.Role.DOCTOR); req.setPhone("9876543210");
        req.setPassword("pass123"); req.setEmail("dr@example.com");

        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(10L);

        ApiResponse<UserResponse> iamResponse = ApiResponse.<UserResponse>builder()
                .success(true).data(userResponse).build();

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(mockFacility));
        when(iamClient.createStaffAccount(any())).thenReturn(iamResponse);
        when(staffRepository.save(any())).thenReturn(mockStaff);

        StaffResponse result = staffService.createStaff(req);

        assertThat(result.getName()).isEqualTo("Dr. Smith");
        verify(staffRepository).save(any(Staff.class));
    }

    @Test
    void createStaff_facilityNotFound_throwsResourceNotFoundException() {
        StaffRequest req = new StaffRequest();
        req.setFacilityId(99L);
        req.setRole(Staff.Role.DOCTOR);

        when(facilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.createStaff(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStaffById_notFound_throwsResourceNotFoundException() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.getStaffById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStaffById_success() {
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));

        StaffResponse result = staffService.getStaffById(10L);

        assertThat(result.getStaffId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Dr. Smith");
    }

    @Test
    void getAllStaff_returnsMappedList() {
        when(staffRepository.findAll()).thenReturn(List.of(mockStaff));

        List<StaffResponse> result = staffService.getAllStaff();

        assertThat(result).hasSize(1);
    }

    @Test
    void updateStaffStatus_notFound_throwsResourceNotFoundException() {
        when(staffRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> staffService.updateStaffStatus(99L, Staff.Status.INACTIVE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStaffStatus_success() {
        when(staffRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(staffRepository.save(any())).thenReturn(mockStaff);

        StaffResponse result = staffService.updateStaffStatus(10L, Staff.Status.INACTIVE);

        assertThat(result).isNotNull();
        verify(staffRepository).save(any(Staff.class));
    }

    @Test
    void deleteStaff_notFound_throwsResourceNotFoundException() {
        when(staffRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> staffService.deleteStaff(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteStaff_success() {
        when(staffRepository.existsById(10L)).thenReturn(true);
        doNothing().when(staffRepository).deleteById(10L);

        staffService.deleteStaff(10L);

        verify(staffRepository).deleteById(10L);
    }

    @Test
    void getStaffByFacility_facilityNotFound_throwsResourceNotFoundException() {
        when(facilityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> staffService.getStaffByFacility(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStaffByFacility_success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(staffRepository.findByFacilityFacilityId(1L)).thenReturn(List.of(mockStaff));

        List<StaffResponse> result = staffService.getStaffByFacility(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getStaffByRole_returnsMappedList() {
        when(staffRepository.findByRole(Staff.Role.DOCTOR)).thenReturn(List.of(mockStaff));

        List<StaffResponse> result = staffService.getStaffByRole(Staff.Role.DOCTOR);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(Staff.Role.DOCTOR);
    }
}

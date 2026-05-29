package org.citycare.facilityservice.services;

import org.citycare.facilityservice.dto.request.StaffRequest;
import org.citycare.facilityservice.dto.response.StaffResponse;
import org.citycare.facilityservice.entities.Staff;

import java.util.List;

public interface StaffService {
    // Standard CRUD
    StaffResponse createStaff(StaffRequest request);
    StaffResponse getStaffById(Long id);
    List<StaffResponse> getAllStaff();
    StaffResponse updateStaffStatus(Long id, Staff.Status status);

    // Business Logic
    List<StaffResponse> getStaffByFacility(Long facilityId);
    List<StaffResponse> getStaffByRole(Staff.Role role);
    void deleteStaff(Long id);
}

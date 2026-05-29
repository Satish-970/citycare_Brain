package org.citycare.facilityservice.client;

import org.citycare.facilityservice.dto.request.StaffRequest;
import org.citycare.facilityservice.dto.response.ApiResponse;
import org.citycare.facilityservice.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AUTHSERVICE")
public interface IamClient {

    // 1. Doctors and Nurses
    @PostMapping("/api/admin/staff")
    ApiResponse<UserResponse> createStaffAccount(@RequestBody StaffRequest request);

    // 2. Dispatchers
    @PostMapping("/api/admin/dispatchers")
    ApiResponse<UserResponse> createDispatcherAccount(@RequestBody StaffRequest request);

    // 3. Compliance Officers
    @PostMapping("/api/admin/compliance-officers")
    ApiResponse<UserResponse> createComplianceAccount(@RequestBody StaffRequest request);


}
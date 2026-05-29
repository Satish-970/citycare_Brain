//package org.citycare.emergencyservice.servicesimpl;
//
//import org.citycare.emergencyservice.dto.request.AmbulanceRequest;
//import org.citycare.emergencyservice.dto.request.DispatchRequest;
//import org.citycare.emergencyservice.dto.request.EmergencyRequest;
//import org.citycare.emergencyservice.entity.Ambulance;
//import org.citycare.emergencyservice.entity.Emergency;
//import org.citycare.emergencyservice.exception.BadRequestException;
//import org.citycare.emergencyservice.exception.ResourceNotFoundException;
//import org.citycare.emergencyservice.feign.CitizenClient;
//import org.citycare.emergencyservice.repository.AmbulanceRepository;
//import org.citycare.emergencyservice.repository.EmergencyRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class EmergencyServiceImplTest {
//
//    @Mock EmergencyRepository emergencyRepository;
//    @Mock AmbulanceRepository ambulanceRepository;
//    @Mock CitizenClient citizenClient;
//
//    @InjectMocks EmergencyServiceImpl emergencyService;
//
//    private Emergency mockEmergency;
//    private Ambulance mockAmbulance;
//
//    @BeforeEach
//    void setUp() {
//        mockAmbulance = Ambulance.builder()
//                .ambulanceId(1L).vehicleNumber("KA-01-1234")
//                .model("Toyota").status(Ambulance.Status.AVAILABLE).build();
//
//        mockEmergency = Emergency.builder()
//                .emergencyId(1L).citizenId(10L)
//                .type(Emergency.Type.ACCIDENT).location("MG Road")
//                .status(Emergency.Status.REPORTED).build();
//    }
//
//    @Test
//    void reportEmergency_success() {
//        EmergencyRequest req = new EmergencyRequest();
//        req.setType(Emergency.Type.ACCIDENT);
//        req.setLocation("MG Road");
//        req.setDescription("Car accident");
//
//        when(emergencyRepository.save(any())).thenReturn(mockEmergency);
//
//        Emergency result = emergencyService.reportEmergency(10L, req);
//
//        assertThat(result.getCitizenId()).isEqualTo(10L);
//        assertThat(result.getStatus()).isEqualTo(Emergency.Status.REPORTED);
//        verify(emergencyRepository).save(any(Emergency.class));
//    }
//
//    @Test
//    void dispatchAmbulance_success() {
//        DispatchRequest req = new DispatchRequest();
//        req.setAmbulanceId(1L);
//
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//        when(ambulanceRepository.findById(1L)).thenReturn(Optional.of(mockAmbulance));
//        when(ambulanceRepository.save(any())).thenReturn(mockAmbulance);
//        when(emergencyRepository.save(any())).thenReturn(mockEmergency);
//
//        Emergency result = emergencyService.dispatchAmbulance(1L, 5L, req);
//
//        assertThat(result).isNotNull();
//        verify(ambulanceRepository).save(any(Ambulance.class));
//        verify(emergencyRepository).save(any(Emergency.class));
//    }
//
//    @Test
//    void dispatchAmbulance_emergencyNotReported_throwsBadRequest() {
//        mockEmergency.setStatus(Emergency.Status.DISPATCHED);
//        DispatchRequest req = new DispatchRequest();
//        req.setAmbulanceId(1L);
//
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//
//        assertThatThrownBy(() -> emergencyService.dispatchAmbulance(1L, 5L, req))
//                .isInstanceOf(BadRequestException.class);
//    }
//
//    @Test
//    void dispatchAmbulance_ambulanceNotAvailable_throwsBadRequest() {
//        mockAmbulance.setStatus(Ambulance.Status.DISPATCHED);
//        DispatchRequest req = new DispatchRequest();
//        req.setAmbulanceId(1L);
//
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//        when(ambulanceRepository.findById(1L)).thenReturn(Optional.of(mockAmbulance));
//
//        assertThatThrownBy(() -> emergencyService.dispatchAmbulance(1L, 5L, req))
//                .isInstanceOf(BadRequestException.class);
//    }
//
//    @Test
//    void getById_notFound_throwsResourceNotFoundException() {
//        when(emergencyRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> emergencyService.getById(99L))
//                .isInstanceOf(ResourceNotFoundException.class);
//    }
//
//    @Test
//    void getById_success() {
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//
//        Emergency result = emergencyService.getById(1L);
//
//        assertThat(result.getEmergencyId()).isEqualTo(1L);
//    }
//
//    @Test
//    void updateEmergencyStatus_success() {
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//        when(emergencyRepository.save(any())).thenReturn(mockEmergency);
//
//        Emergency result = emergencyService.updateEmergencyStatus(1L, "DISPATCHED");
//
//        assertThat(result.getStatus()).isEqualTo(Emergency.Status.DISPATCHED);
//    }
//
//    @Test
//    void addAmbulance_success() {
//        AmbulanceRequest req = new AmbulanceRequest();
//        req.setVehicleNumber("KA-01-1234");
//        req.setModel("Toyota");
//
//        when(ambulanceRepository.save(any())).thenReturn(mockAmbulance);
//
//        Ambulance result = emergencyService.addAmbulance(req);
//
//        assertThat(result.getVehicleNumber()).isEqualTo("KA-01-1234");
//    }
//
//    @Test
//    void getAvailableAmbulances_returnsList() {
//        when(ambulanceRepository.findByStatus(Ambulance.Status.AVAILABLE)).thenReturn(List.of(mockAmbulance));
//
//        List<Ambulance> result = emergencyService.getAvailableAmbulances();
//
//        assertThat(result).hasSize(1);
//    }
//
//    @Test
//    void releaseAmbulanceForEmergency_noAmbulance_throwsBadRequest() {
//        mockEmergency.setAmbulance(null);
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//
//        assertThatThrownBy(() -> emergencyService.releaseAmbulanceForEmergency(1L))
//                .isInstanceOf(BadRequestException.class);
//    }
//
//    @Test
//    void releaseAmbulanceForEmergency_success() {
//        mockEmergency.setAmbulance(mockAmbulance);
//        when(emergencyRepository.findById(1L)).thenReturn(Optional.of(mockEmergency));
//        when(ambulanceRepository.save(any())).thenReturn(mockAmbulance);
//
//        emergencyService.releaseAmbulanceForEmergency(1L);
//
//        assertThat(mockAmbulance.getStatus()).isEqualTo(Ambulance.Status.AVAILABLE);
//        verify(ambulanceRepository).save(mockAmbulance);
//    }
//}

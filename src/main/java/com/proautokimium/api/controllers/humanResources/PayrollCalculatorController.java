package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calculator.CltPjComparisonResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.FuelRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.FuelResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.MealVoucherRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.MealVoucherResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.TransportationVoucherRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Calculator.TransportationVoucherResponseDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.PayrollCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/hr/calculators")
@Tag(name = "Calculadoras", description = "Vale-transporte, vale-alimentação, combustível e CLT×PJ")
@PreAuthorize("hasAnyRole('ADMIN', 'RH')")
public class PayrollCalculatorController {

    private final PayrollCalculatorService service;

    public PayrollCalculatorController(PayrollCalculatorService service) {
        this.service = service;
    }

    @PostMapping("/transportation-voucher")
    @Operation(summary = "Calcula vale-transporte", description = "Usa a quantidade de conduções cadastrada no funcionário")
    public ResponseEntity<TransportationVoucherResponseDTO> transportationVoucher(@Valid @RequestBody TransportationVoucherRequestDTO request) {
        return ResponseEntity.ok(service.calculateTransportationVoucher(request));
    }

    @PostMapping("/meal-voucher")
    @Operation(summary = "Calcula vale-alimentação", description = "Usa a quantidade de refeições cadastrada no funcionário")
    public ResponseEntity<MealVoucherResponseDTO> mealVoucher(@Valid @RequestBody MealVoucherRequestDTO request) {
        return ResponseEntity.ok(service.calculateMealVoucher(request));
    }

    @PostMapping("/fuel")
    @Operation(summary = "Calcula combustível", description = "km rodados ÷ consumo do veículo × preço do litro")
    public ResponseEntity<FuelResponseDTO> fuel(@Valid @RequestBody FuelRequestDTO request) {
        return ResponseEntity.ok(service.calculateFuel(request));
    }

    @GetMapping("/clt-pj/{employeeId}")
    @Operation(summary = "Compara CLT x PJ", description = "Pega o salário atual do funcionário e simula o custo real como CLT")
    public ResponseEntity<CltPjComparisonResponseDTO> cltPj(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.compareCltPj(employeeId));
    }
}

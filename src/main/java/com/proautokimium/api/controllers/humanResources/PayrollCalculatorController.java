package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calculator.*;
import com.proautokimium.api.Infrastructure.services.humanResources.PayrollCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/calculators")
@Tag(name = "Calculadoras", description = "Vale-transporte, vale-alimentação, combustível e CLT×PJ")
public class PayrollCalculatorController {

    private final PayrollCalculatorService service;

    public PayrollCalculatorController(PayrollCalculatorService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONSULTAR')")
    @PostMapping("/transportation-voucher")
    @Operation(summary = "Calcula vale-transporte", description = "Usa a quantidade de conduções cadastrada no funcionário")
    public ResponseEntity<TransportationVoucherResponseDTO> transportationVoucher(@Valid @RequestBody TransportationVoucherRequestDTO request) {
        return ResponseEntity.ok(service.calculateTransportationVoucher(request));
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONSULTAR')")
    @PostMapping("/meal-voucher")
    @Operation(summary = "Calcula vale-alimentação", description = "Usa a quantidade de refeições cadastrada no funcionário")
    public ResponseEntity<MealVoucherResponseDTO> mealVoucher(@Valid @RequestBody MealVoucherRequestDTO request) {
        return ResponseEntity.ok(service.calculateMealVoucher(request));
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONSULTAR')")
    @PostMapping("/fuel")
    @Operation(summary = "Calcula combustível", description = "km rodados ÷ consumo do veículo × preço do litro")
    public ResponseEntity<FuelResponseDTO> fuel(@Valid @RequestBody FuelRequestDTO request) {
        return ResponseEntity.ok(service.calculateFuel(request));
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONSULTAR')")
    @GetMapping("/clt-pj/{employeeId}")
    @Operation(summary = "Compara CLT x PJ", description = "Pega o salário atual do funcionário e simula o custo real como CLT")
    public ResponseEntity<CltPjComparisonResponseDTO> cltPj(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(service.compareCltPj(employeeId));
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONSULTAR')")
    @PostMapping("/bulk/transportation-voucher")
    @Operation(summary = "Cálculo mensal VT em massa", description = "Calcula vale-transporte para todos os funcionários do tipo informado, agrupado por empresa")
    public ResponseEntity<List<BulkTransportVoucherResponseDTO>> bulkTransportationVoucher(@Valid @RequestBody BulkTransportVoucherRequestDTO request) {
        return ResponseEntity.ok(service.calculateBulkTransportVoucher(request));
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONSULTAR')")
    @PostMapping("/bulk/fuel")
    @Operation(summary = "Cálculo mensal combustível em massa", description = "Calcula combustível para todos os funcionários com veículo, agrupado por empresa")
    public ResponseEntity<List<BulkFuelResponseDTO>> bulkFuel(@Valid @RequestBody BulkFuelRequestDTO request) {
        return ResponseEntity.ok(service.calculateBulkFuel(request));
    }

    @PreAuthorize("hasAuthority('rh/calculators:CONFIGURAR')")
    @PutMapping("/ticket-price-adjustment")
    @Operation(summary = "Reajuste de tarifas em massa", description = "Atualiza o valor da passagem para todos os funcionários do tipo informado")
    public ResponseEntity<TicketPriceAdjustmentResponseDTO> adjustTicketPrices(@Valid @RequestBody TicketPriceAdjustmentRequestDTO request) {
        return ResponseEntity.ok(service.adjustTicketPrices(request));
    }
}

package com.proautokimium.api.Infrastructure.services.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.valueObjects.Email;
import com.proautokimium.api.domain.exceptions.customer.CustomerAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.customer.CustomerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private PartnerReaderService reader;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequestDTO dto;
    private Customer customer;

    @BeforeEach
    void setUp() {
        dto = new CustomerRequestDTO("COD001", "12345678000100", "Cliente Teste", "cliente@teste.com", "user", true, true, "MAT001", true);
        customer = Customer.fromDTO(dto);
    }

    @Test
    @DisplayName("Deve criar cliente com sucesso quando não existe")
    void deveCriarClienteComSucesso() {
        when(repository.findByCodParceiro("COD001")).thenReturn(Optional.empty());

        customerService.createCustomer(dto);

        verify(repository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Deve lançar CustomerAlreadyExistsException ao criar cliente duplicado")
    void deveLancarExcecaoAoCriarClienteDuplicado() {
        when(repository.findByCodParceiro("COD001")).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> customerService.createCustomer(dto))
                .isInstanceOf(CustomerAlreadyExistsException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar 200 com lista de clientes quando há clientes cadastrados")
    void deveRetornarListaDeClientesComSucesso() {
        when(repository.findAll()).thenReturn(List.of(customer));

        ResponseEntity<Object> response = customerService.getAllCustomers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Deve retornar 204 No Content quando não há clientes cadastrados")
    void deveRetornarNoContentQuandoNaoHaClientes() {
        when(repository.findAll()).thenReturn(List.of());

        ResponseEntity<Object> response = customerService.getAllCustomers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Deve retornar lista de emails dos clientes com sucesso")
    void deveRetornarListaDeEmailsDeClientes() {
        when(repository.findAll()).thenReturn(List.of(customer));

        ResponseEntity<Object> response = customerService.getAllCustomersEmail();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Deve atualizar cliente com sucesso")
    void deveAtualizarClienteComSucesso() {
        when(repository.findByCodParceiro("COD001")).thenReturn(Optional.of(customer));

        customerService.UpdateCustomer(dto);

        verify(repository).save(customer);
    }

    /**
     * <b>A edição não pode desfazer o que a V101 fez.</b>
     *
     * <p>Esta era a brecha: {@code UpdateCustomer} escrevia
     * {@code setMatriz(dto.isMatriz())}, direto do checkbox do formulário. A
     * migration marca 4623 matrizes, e a primeira edição de cada uma reverteria
     * — uma linha por vez, sem erro nenhum, e o portal do cliente voltaria a não
     * mostrar unidade nenhuma.
     */
    @Test
    @DisplayName("Editar não desfaz o is_matriz: quem decide é o código, não o checkbox")
    void updateDoesNotUndoIsMatriz() {
        Customer existing = new Customer("1708", "12345678000199", "ACME", null,
                new Email("acme@x.com"), true, true, "1708", false);
        when(repository.findByCodParceiro("1708")).thenReturn(Optional.of(existing));

        // O formulário afirma que NÃO é matriz, e o código diz que é.
        CustomerRequestDTO lying = new CustomerRequestDTO("1708", "12345678000199",
                "ACME", "acme@x.com", null, true, true, "1708", false);

        customerService.UpdateCustomer(lying);

        assertThat(existing.isMatriz())
                .as("o checkbox não manda mais")
                .isTrue();
        verify(repository).save(existing);
    }

    /** E o contrário também: deixar de ser matriz sai do dado, não do formulário. */
    @Test
    @DisplayName("Editar apontando para outro grupo tira a marca de matriz")
    void updateToAnotherGroupClearsIsMatriz() {
        Customer existing = new Customer("505", "12345678000199", "PGR SINTER", null,
                new Email("pgr@x.com"), true, true, "505", true);
        when(repository.findByCodParceiro("505")).thenReturn(Optional.of(existing));

        CustomerRequestDTO becomesUnit = new CustomerRequestDTO("505", "12345678000199",
                "PGR SINTER", "pgr@x.com", null, true, true, "1708", true);

        customerService.UpdateCustomer(becomesUnit);

        assertThat(existing.isMatriz()).isFalse();
        assertThat(existing.getCodigoMatriz()).isEqualTo("1708");
    }

    @Test
    @DisplayName("Deve lançar CustomerNotFoundException ao atualizar cliente inexistente")
    void deveLancarExcecaoAoAtualizarClienteInexistente() {
        when(repository.findByCodParceiro("COD001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.UpdateCustomer(dto))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar cliente com sucesso")
    void deveDeletarClienteComSucesso() {
        when(repository.findByCodParceiro("COD001")).thenReturn(Optional.of(customer));

        customerService.DeleteCustomer("COD001");

        verify(repository).delete(customer);
    }

    @Test
    @DisplayName("Deve lançar CustomerNotFoundException ao deletar cliente inexistente")
    void deveLancarExcecaoAoDeletarClienteInexistente() {
        when(repository.findByCodParceiro("COD001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.DeleteCustomer("COD001"))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve importar novos clientes via Excel com sucesso")
    void deveImportarClientesViaExcelComSucesso() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.ms-excel", new byte[]{1, 2, 3});
        when(reader.getDataByExcel(any())).thenReturn(List.of(customer));
        when(repository.findByCodParceiroIn(any())).thenReturn(List.of());

        customerService.includeCustomersByExcel(file);

        verify(repository).saveAll(any(List.class));
    }

    @Test
    @DisplayName("Deve atualizar clientes existentes ao importar via Excel")
    void deveAtualizarClientesExistentesViaExcel() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.ms-excel", new byte[]{1, 2, 3});
        when(reader.getDataByExcel(any())).thenReturn(List.of(customer));
        when(repository.findByCodParceiroIn(any())).thenReturn(List.of(customer));

        customerService.includeCustomersByExcel(file);

        verify(repository).saveAll(any(List.class));
    }

    @Test
    @DisplayName("Deve lançar ResponseStatusException quando Excel não contém clientes")
    void deveLancarExcecaoQuandoExcelEstaVazio() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.ms-excel", new byte[]{1, 2, 3});
        when(reader.getDataByExcel(any())).thenReturn(List.of());

        Throwable thrown = catchThrowable(() -> customerService.includeCustomersByExcel(file));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value()).isEqualTo(500);
    }
}

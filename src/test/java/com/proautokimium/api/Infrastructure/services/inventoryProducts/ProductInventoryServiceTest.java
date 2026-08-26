package com.proautokimium.api.Infrastructure.services.inventoryProducts;

import com.proautokimium.api.Application.DTOs.prostock.product.ProductMovementDTO;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductMovementRepository;
import com.proautokimium.api.domain.entities.prostock.MovementInventory;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * O serviço não tinha teste nenhum — e foi por aqui que passaram os dois
 * incidentes de produto duplicado, em 2026-08-11 e 2026-08-24.
 *
 * O que estes testes travam é a causa e o sintoma: a planilha que criava dois
 * produtos com o mesmo código, e os quatro chamadores de `findBySystemCode`
 * que precisavam lidar com ausência.
 */
@ExtendWith(MockitoExtension.class)
class ProductInventoryServiceTest {

    @Mock
    private ProductInventoryRepository productInventoryRepository;

    @Mock
    private ProductMovementRepository productMovementRepository;

    @Mock
    private InventoryProductExcelReaderService reader;

    @Mock
    private InventoryProductsExcelWriterService writer;

    @InjectMocks
    private ProductInventoryService service;

    private ProductInventory produto(String systemCode, String nome) {
        ProductInventory p = new ProductInventory();
        p.setSystemCode(systemCode);
        p.setName(nome);
        return p;
    }

    // ─── Ausência: era NPE, agora é 404 ──────────────────────────────────────

    /**
     * A linha que derrubou a tela de movimentações.
     *
     * Era `findBySystemCode(codigo).getId()`, sem checagem. Código inexistente
     * dava NPE; código duplicado dava NonUniqueResultException. As duas saíam
     * como 500 "erro interno", que não diz nada a quem está olhando a tela.
     */
    @Test
    @DisplayName("Movimentos de produto inexistente: 404, não NPE")
    void movimentosDeProdutoInexistenteDeveLancarNotFound() {
        when(productInventoryRepository.findBySystemCode("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAllMovementsByProduct("NAO-EXISTE"))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(productMovementRepository);
    }

    @Test
    @DisplayName("Lançar movimento em produto inexistente: 404, e nada é gravado")
    void movimentoEmProdutoInexistenteDeveLancarNotFound() {
        when(productInventoryRepository.findBySystemCode("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.includeMovement(
                new ProductMovementDTO(null, null, 10, "NAO-EXISTE")))
                .isInstanceOf(ProductNotFoundException.class);

        // Sem isto o movimento era gravado com product_id nulo: um lançamento
        // órfão, invisível em qualquer tela.
        verify(productMovementRepository, never()).save(any());
    }

    /** Apagar o que não existe é pedido já atendido, não erro. */
    @Test
    @DisplayName("Apagar produto inexistente não lança e não apaga nada")
    void apagarProdutoInexistenteNaoFazNada() {
        when(productInventoryRepository.findBySystemCode("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatCode(() -> service.deleteProductBySystemCode("NAO-EXISTE"))
                .doesNotThrowAnyException();

        verify(productInventoryRepository, never()).deleteById(any());
    }

    // ─── Planilha: a origem dos duplicados ───────────────────────────────────

    /**
     * **O teste que trava a causa raiz.**
     *
     * O laço olhava só o que já estava no banco, nunca o que já tinha entrado
     * em `toInsert`. Duas linhas com o mesmo código no mesmo arquivo viravam
     * dois produtos, e a partir daí `findBySystemCode` estourava para sempre.
     */
    @Test
    @DisplayName("Código repetido na planilha insere um produto só")
    void codigoRepetidoNaPlanilhaInsereUmSo() throws Exception {
        when(reader.getDataByExcel(any())).thenReturn(List.of(
                produto("PRIM-001", "Primer Wash"),
                produto("PRIM-001", "Primer Wash"),
                produto("PRIM-002", "Desengraxante")
        ));
        when(productInventoryRepository.findBySystemCodeIn(anyList())).thenReturn(List.of());

        service.includeProductBySheet(planilhaQualquer());

        ArgumentCaptor<List<ProductInventory>> captor = ArgumentCaptor.forClass(List.class);
        verify(productInventoryRepository).saveAll(captor.capture());

        List<String> codigosGravados = captor.getValue().stream()
                .map(ProductInventory::getSystemCode)
                .toList();

        assertThat(codigosGravados).containsExactly("PRIM-001", "PRIM-002");
    }

    /**
     * Descartar em silêncio esconde a planilha suja em vez de corrigi-la — e
     * quem mantém o arquivo nunca fica sabendo.
     */
    @Test
    @DisplayName("A resposta diz qual código veio repetido")
    void respostaAvisaOsCodigosRepetidos() throws Exception {
        when(reader.getDataByExcel(any())).thenReturn(List.of(
                produto("PRIM-001", "Primer Wash"),
                produto("PRIM-001", "Primer Wash")
        ));
        when(productInventoryRepository.findBySystemCodeIn(anyList())).thenReturn(List.of());

        ResponseEntity<Object> resposta = service.includeProductBySheet(planilhaQualquer());

        assertThat(resposta.getBody().toString())
                .contains("1 produtos adicionados")
                .contains("repetido")
                .contains("PRIM-001");
    }

    /** Código que já existe no banco atualiza, não insere de novo. */
    @Test
    @DisplayName("Código já existente no banco é atualizado, não duplicado")
    void codigoExistenteEhAtualizado() throws Exception {
        ProductInventory jaNoBanco = produto("PRIM-001", "Nome antigo");

        when(reader.getDataByExcel(any())).thenReturn(List.of(produto("PRIM-001", "Nome novo")));
        when(productInventoryRepository.findBySystemCodeIn(anyList())).thenReturn(List.of(jaNoBanco));

        service.includeProductBySheet(planilhaQualquer());

        ArgumentCaptor<List<ProductInventory>> captor = ArgumentCaptor.forClass(List.class);
        verify(productInventoryRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).containsExactly(jaNoBanco);
        assertThat(jaNoBanco.getName()).isEqualTo("Nome novo");
    }

    /** O conteúdo não importa: quem lê a planilha é o `reader`, e ele é mock. */
    private MockMultipartFile planilhaQualquer() {
        return new MockMultipartFile("file", "produtos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "conteudo".getBytes());
    }

    // ─── A ordem das movimentações ───────────────────────────────────────────

    private MovementInventory movimento(ProductInventory product, int quantity,
                                        LocalDateTime quando, int segundo) {
        MovementInventory movement = new MovementInventory();
        movement.setProduct(product);
        movement.setQuantity(quantity);
        movement.setMovementDate(quando);
        movement.setCreatedAt(OffsetDateTime.of(2026, 9, 10, 8, 0, segundo, 0, ZoneOffset.UTC));
        return movement;
    }

    /**
     * **O teste do bug de 2026-08-26.**
     *
     * A tela lê o último item desta lista como estoque atual. Ordenando por
     * `movementDate` — que é `date`, sem hora — dois lançamentos do mesmo dia
     * empatam, e qual vinha por último era acaso: o estoque de uma máquina foi
     * de 2 para 0 numa entrega de uma unidade só.
     *
     * A lista chega do repositório fora de ordem de propósito. Se alguém voltar
     * a ordenar por `movementDate`, este teste quebra.
     */
    @Test
    @DisplayName("Movimentações do mesmo dia saem na ordem em que foram registradas")
    void movimentacoesDoMesmoDiaSaemNaOrdemDeRegistro() {
        ProductInventory product = new ProductInventory();
        product.setSystemCode("MAQ-001");

        LocalDateTime mesmoDia = LocalDateTime.of(2026, 9, 10, 0, 0);

        when(productInventoryRepository.findBySystemCode("MAQ-001"))
                .thenReturn(Optional.of(product));
        when(productMovementRepository.findMovementByProductId(any()))
                .thenReturn(List.of(
                        movimento(product, 0, mesmoDia, 3),
                        movimento(product, 2, mesmoDia, 1),
                        movimento(product, 1, mesmoDia, 2)));

        List<ProductMovementDTO> movimentos = service.findAllMovementsByProduct("MAQ-001");

        assertThat(movimentos).extracting(ProductMovementDTO::quantity)
                .containsExactly(2, 1, 0);
        // O último da lista é o estoque atual, e é isso que a tela lê.
        assertThat(movimentos.get(movimentos.size() - 1).quantity()).isZero();
    }
}

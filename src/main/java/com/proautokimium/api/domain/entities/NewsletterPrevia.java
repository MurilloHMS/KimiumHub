package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * O rascunho da newsletter de um mês, antes de virar fila de envio.
 *
 * **Existe porque revisar 913 clientes não é coisa de cinco minutos.** Sem ele,
 * sair da tela — ou a API reiniciar — jogaria fora todas as correções, e a
 * pessoa descobriria isso do pior jeito: recomeçando.
 *
 * Um mês tem no máximo um rascunho não confirmado. Pedir a prévia do mesmo mês
 * duas vezes devolve este; depois de confirmado, a API recusa refazer, porque a
 * newsletter dispara e-mail e um mês repetido chega na caixa do cliente.
 */
@jakarta.persistence.Entity
@Table(
        name = "newsletter_previa",
        uniqueConstraints = @UniqueConstraint(name = "uk_newsletter_previa_periodo", columnNames = {"mes", "ano"})
)
@Getter
@Setter
@NoArgsConstructor
public class NewsletterPrevia extends Entity {

    @Column(name = "mes", nullable = false)
    private int mes;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    /** `null` enquanto está em revisão. Preenchido é o que fecha o mês. */
    @Column(name = "confirmado_em")
    private LocalDateTime confirmadoEm;

    /**
     * Quantos clientes entraram na fila na confirmação.
     *
     * Guardado, e não contado na hora: é o número que a recusa do mês repetido
     * mostra, e ele precisa dizer o que valia **naquele dia** — recontar depois
     * daria o total de hoje, que é outra coisa.
     */
    @Column(name = "quantidade_de_clientes")
    private int quantidadeDeClientes;

    @OneToMany(mappedBy = "previa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewsletterPreviaCliente> clientes = new ArrayList<>();

    @OneToMany(mappedBy = "previa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewsletterPreviaOs> ordensDeServico = new ArrayList<>();

    public NewsletterPrevia(int mes, int ano) {
        this.mes = mes;
        this.ano = ano;
        this.criadoEm = LocalDateTime.now();
    }

    public boolean estaConfirmada() {
        return confirmadoEm != null;
    }

    public void adicionar(NewsletterPreviaCliente cliente) {
        cliente.setPrevia(this);
        this.clientes.add(cliente);
    }

    public void adicionar(NewsletterPreviaOs os) {
        os.setPrevia(this);
        this.ordensDeServico.add(os);
    }
}

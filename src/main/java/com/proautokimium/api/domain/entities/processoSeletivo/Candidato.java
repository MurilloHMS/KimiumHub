package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidatos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Candidato extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "nome", length = 100, nullable = false)
    private String nome;
    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 100, nullable = false))
    private Email email;
    @Column(name = "telefone", length = 11,  nullable = false)
    private String telefone;
    @Column(name = "url_linkedin", length = 100)
    private String urlLinkedin;
    @Column(name = "path_curriculo", length = 200)
    private String pathCurriculo;
    @Column(name = "criado_em" )
    private LocalDateTime criadoEm;

    // ─── Banco de talentos (V104) ────────────────────────────────────────────

    @Column(name = "area_interesse", length = 100)
    private String areaInteresse;

    /**
     * Quando a pessoa autorizou permanecer no banco de talentos.
     *
     * <p><b>{@code null} é um terceiro estado</b>, e não "não": significa "não
     * registrado". As linhas anteriores a 2026-09-11 chegaram por candidatura a
     * vaga — finalidade legítima por si só —, e o que falta nelas é o aceite de
     * ficar disponível para vagas futuras. Por isso o expurgo nunca pode
     * alcançá-las.
     */
    @Column(name = "consentimento_em")
    private LocalDateTime consentimentoEm;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "anonimizado_em")
    private LocalDateTime anonimizadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ─── Regras ──────────────────────────────────────────────────────────────

    /**
     * Registra — ou renova — o consentimento.
     *
     * <p>A expiração é gravada como <b>data resolvida</b>, e não calculada na
     * leitura a partir de {@code criadoEm}. É a regra do {@code CareerHistory}:
     * snapshot congelado, nunca referência dinâmica. Calculada, mudar a
     * retenção de 24 para 12 meses expiraria metade do banco numa madrugada,
     * sem ninguém pedir.
     *
     * <p>Chamar de novo empurra as duas datas — é assim que a renovação
     * funciona, e é por isso que não há guarda de "só se estiver nulo".
     */
    public void registrarConsentimento(LocalDateTime agora, int mesesDeRetencao) {
        this.consentimentoEm = agora;
        this.expiraEm = agora.plusMonths(mesesDeRetencao);
        this.atualizadoEm = agora;
    }

    public boolean consentimentoValidoEm(LocalDateTime agora) {
        return consentimentoEm != null && expiraEm != null && expiraEm.isAfter(agora);
    }

    /**
     * Apaga os dados pessoais mantendo a linha.
     *
     * <p>Serve ao caso em que existe {@code candidatura} apontando para o
     * candidato: um {@code DELETE} duro estouraria a FK, ou — se alguém
     * "resolvesse" com cascade — levaria junto {@code candidaturas},
     * {@code historico_etapas} e {@code resposta_perguntas} de uma contratação
     * real.
     *
     * <p><b>A lápide é única por id.</b> Um endereço fixo violaria o índice
     * único de e-mail na segunda exclusão — 500 num endpoint público. O domínio
     * {@code .invalid} é reservado pela RFC 2606, então nenhum e-mail escapa
     * para um destino real.
     *
     * <p>Zera consentimento <b>e</b> expiração: deixar {@code expiraEm}
     * preenchido faria o agendador "expirar" para sempre uma linha já anônima.
     */
    public void anonimizar(LocalDateTime agora) {
        this.nome = "Candidato removido";
        this.email = new Email("removido+" + getId() + "@removido.invalid");
        this.telefone = "";
        this.urlLinkedin = null;
        this.pathCurriculo = null;
        this.areaInteresse = null;
        this.consentimentoEm = null;
        this.expiraEm = null;
        this.anonimizadoEm = agora;
        this.atualizadoEm = agora;
    }

    public boolean estaAnonimizado() {
        return anonimizadoEm != null;
    }
}

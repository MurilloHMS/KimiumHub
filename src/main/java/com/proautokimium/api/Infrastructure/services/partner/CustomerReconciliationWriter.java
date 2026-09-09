package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationRowDTO;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.enums.ReconciliationOutcome;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava <b>uma</b> linha da conciliação, em transação própria.
 *
 * <p><b>Este bean existe separado por um motivo mecânico, não estético.</b> Uma
 * chamada de método dentro da mesma classe não passa pelo proxy do Spring, e
 * {@code @Transactional} não faria nada — em silêncio. Só a chamada entre beans
 * abre a transação nova.
 *
 * <p><b>E existe separado por um motivo de projeto.</b> O importador de Excel é
 * {@code @Transactional} sobre o lote inteiro: um e-mail ruim derruba 300 linhas
 * boas, com 500 e sem dizer qual. Aqui cada linha cai sozinha, e o resultado diz
 * quais falharam.
 *
 * <p>O que ele escreve, e só: {@code name}, {@code documento}, {@code email},
 * {@code codigo_matriz} — e {@code ativo}, na desativação. <b>Nunca toca
 * {@code recebe_email}</b>, que é a decisão de quem pediu para não receber:
 * resetá-lo mandaria newsletter para quem descadastrou, e ninguém descobriria
 * por meses. Nunca toca {@code username}, que é único e nem está no formulário.
 * E nunca apaga.
 */
@Service
public class CustomerReconciliationWriter {

    private final CustomerRepository repository;

    public CustomerReconciliationWriter(CustomerRepository repository) {
        this.repository = repository;
    }

    /**
     * Cliente novo, vindo do ERP.
     *
     * <p>Nasce ativo — só chega aqui quem está ativo lá — e com
     * {@code recebeEmail = true}: quem entra no cadastro entra para receber, e o
     * descadastro é decisão dele depois. O {@code isMatriz} sai da regra do
     * próprio {@code Customer}, comparando código e código da matriz.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconciliationOutcome create(ReconciliationRowDTO row) {
        Customer customer = new Customer(
                row.code(),
                row.document(),
                row.name(),
                null,                       // username fica fora: é único e não é nosso
                new Email(row.email()),
                true,
                true,
                row.matrizCode(),
                false);                     // ignorado: a regra recalcula

        repository.save(customer);
        return ReconciliationOutcome.CREATED;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconciliationOutcome update(Customer customer, ReconciliationRowDTO row) {
        customer.setName(row.name());
        customer.setDocumento(row.document());
        customer.setEmail(new Email(row.email()));
        customer.setCodigoMatriz(row.matrizCode());
        customer.setMatriz(Customer.isMatriz(row.code(), row.matrizCode()));

        repository.save(customer);
        return ReconciliationOutcome.UPDATED;
    }

    /**
     * Desativa, e nada mais.
     *
     * <p>Não apaga: cliente inativo continua com histórico, notas e newsletters
     * passadas apontando para ele. E não mexe no {@code recebe_email} — quem
     * voltar a ficar ativo volta com a preferência que tinha.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReconciliationOutcome deactivate(Customer customer) {
        customer.setAtivo(false);
        repository.save(customer);
        return ReconciliationOutcome.DEACTIVATED;
    }
}

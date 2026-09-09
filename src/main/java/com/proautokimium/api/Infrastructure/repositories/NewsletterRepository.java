package com.proautokimium.api.Infrastructure.repositories;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

public interface NewsletterRepository extends JpaRepository<Newsletter, UUID>{
	List<Newsletter> findAllByStatus(EmailStatus status);

	List<Newsletter> findAllByStatusIn(Collection<EmailStatus> status);

	List<Newsletter> findTop15ByStatusIn(Collection<EmailStatus> status);

	List<Newsletter> findByCodigoClienteInAndStatusAndDataBetweenOrderByDataAsc(
            Collection<String> codigos, EmailStatus status, LocalDate from, LocalDate to);

	/**
	 * A fila contada por mês e status.
	 *
	 * Agrupado no banco, e não em Java: são todas as newsletters já geradas — 913
	 * por mês — e trazer tudo para contar do lado de cá seria carregar o
	 * histórico inteiro na memória a cada abertura da tela.
	 *
	 * Agrupa por ano e mês de `data`, e não pela coluna `mes`, que é o nome por
	 * extenso: "Junho" de 2025 e de 2026 cairiam no mesmo balde.
	 */
	@Query("""
		SELECT YEAR(n.data), MONTH(n.data), n.status, COUNT(n)
		  FROM Newsletter n
		 GROUP BY YEAR(n.data), MONTH(n.data), n.status
		 ORDER BY YEAR(n.data) DESC, MONTH(n.data) DESC
	""")
	List<Object[]> contarPorMesEStatus();

	List<Newsletter> findByDataBetweenOrderByFaturamentoTotalDesc(LocalDate from, LocalDate to);
}

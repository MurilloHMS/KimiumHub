package com.proautokimium.api.Infrastructure.validators;

import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CurriculoInvalidoException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * A única regra sobre o arquivo de currículo.
 *
 * <p>Até 2026-09-11 o upload público <b>não validava nada</b>: nem tipo, nem
 * tamanho. O único teto era o {@code spring.servlet.multipart.max-file-size} de
 * 100 MB — global, e numa rota anônima.
 *
 * <p><b>Por que os bytes iniciais importam num arquivo que nunca executamos:</b>
 * porque nós o servimos de volta. Um HTML renomeado para {@code .pdf}, entregue
 * a partir da origem da API, é XSS armazenado contra a sessão de quem no RH
 * clicar. Extensão é o que o cliente diz; os primeiros bytes são o que o
 * arquivo é.
 *
 * <p>O limite não pode sair do {@code max-file-size} global: holerite, planilha
 * de newsletter e lote de NFe precisam da folga de 100 MB. Ele é por caminho, e
 * este caminho é só currículo.
 */
@Component
public class CurriculoValidator {

    /** 10 MB — é o que a tela pública já promete a quem envia. */
    public static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;

    /**
     * Só PDF, que é o que o formulário pede hoje. Aceitar {@code doc}/{@code docx}
     * é acrescentar a extensão aqui e a assinatura em {@link #assinaturaConfere}.
     */
    private static final Set<String> EXTENSOES_ACEITAS = Set.of("pdf");

    private static final byte[] ASSINATURA_PDF = { '%', 'P', 'D', 'F' };

    public void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw CurriculoInvalidoException.recusado("Envie um currículo em PDF.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw CurriculoInvalidoException.grandeDemais(TAMANHO_MAXIMO_BYTES);
        }

        String extensao = extensaoDe(arquivo.getOriginalFilename());

        // Extensão ausente não é detalhe: o `buildFileName` do StorageService
        // sobrescreve o fallback "bin" da classe base, então um arquivo sem
        // extensão vira literalmente `<id>.null` no disco.
        if (extensao == null || !EXTENSOES_ACEITAS.contains(extensao)) {
            throw CurriculoInvalidoException.recusado("O currículo precisa ser um arquivo PDF.");
        }

        if (!assinaturaConfere(arquivo)) {
            throw CurriculoInvalidoException.recusado(
                    "Este arquivo não é um PDF de verdade, mesmo terminando em .pdf.");
        }
    }

    private static String extensaoDe(String nomeOriginal) {
        String extensao = StringUtils.getFilenameExtension(nomeOriginal);
        return extensao == null || extensao.isBlank()
                ? null
                : extensao.toLowerCase(Locale.ROOT);
    }

    /**
     * Lê só os primeiros bytes, num stream próprio.
     *
     * <p>O {@code MultipartFile} entrega um {@code InputStream} novo a cada
     * chamada, então a leitura aqui não consome o que a gravação vai usar
     * depois.
     */
    private static boolean assinaturaConfere(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            byte[] inicio = entrada.readNBytes(ASSINATURA_PDF.length);

            if (inicio.length < ASSINATURA_PDF.length) {
                return false;
            }

            for (int i = 0; i < ASSINATURA_PDF.length; i++) {
                if (inicio[i] != ASSINATURA_PDF[i]) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            // Não conseguir ler o que acabou de chegar é falha nossa, mas a
            // resposta certa continua sendo recusar: gravar sem saber o que é
            // seria o pior dos dois mundos.
            throw CurriculoInvalidoException.recusado("Não foi possível ler o arquivo enviado.");
        }
    }
}

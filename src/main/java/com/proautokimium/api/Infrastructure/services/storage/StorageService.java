package com.proautokimium.api.Infrastructure.services.storage;

import com.proautokimium.api.Infrastructure.abstractions.storage.FileStorage;
import com.proautokimium.api.Infrastructure.validators.CurriculoValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * O armazenamento dos currículos.
 *
 * <p><b>A validação mora aqui, e isso é escolha de lugar, não de estilo.</b>
 * Este serviço é exclusivo de currículo ({@code storage.curriculos.path}) e
 * todos os caminhos de envio passam por ele, então validar na porta faz com que
 * um chamador futuro <b>não consiga</b> pular a regra — ao contrário de
 * confiar em cada controller lembrar de chamar o validador.
 */
@Service
public class StorageService extends FileStorage {

    @Value("${storage.curriculos.path}")
    private String storagePath;

    private final CurriculoValidator validator;

    public StorageService(CurriculoValidator validator) {
        this.validator = validator;
    }

    @Override
    protected String getStoragePath() {
        return storagePath;
    }

    @Override
    protected String getReturnPath() {
        return "";
    }

    @Override
    public String save(MultipartFile file, String prefix) throws IOException {
        validator.validar(file);
        return super.save(file, prefix);
    }

    /**
     * Um arquivo por candidato: {@code <candidatoId>.<ext>}.
     *
     * <p>Sem o UUID que a classe base acrescenta, de propósito — é o que faz
     * reenviar substituir em vez de acumular. A consequência é que
     * <b>substituir apaga o anterior</b>, e não existe histórico de versões.
     *
     * <p>A extensão vem do nome original, mas só chega aqui depois de passar
     * pela lista do {@link CurriculoValidator}: sem ela, extensão nula viraria
     * o literal {@code "null"} no nome do arquivo, porque esta sobrescrita
     * ignora o fallback {@code "bin"} da classe base.
     */
    @Override
    protected String buildFileName(MultipartFile file, String prefix) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());

        return prefix + "." + extension;
    }
}

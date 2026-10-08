package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.domain.enums.Permission;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quais das sete ações cada tela usa de verdade.
 *
 * A grade mostrava as sete em toda tela — 504 caixinhas por pessoa, e a API
 * conferia cerca de um terço delas. Marcar "Excluir" no Hub de máquinas não fazia
 * nada, e a tela não tinha como dizer isso.
 *
 * **A fonte é o próprio `@PreAuthorize`, lido no boot.** Uma coluna no banco
 * semeada à mão ficaria para trás no primeiro endpoint novo, e o defeito seria
 * o pior possível: a ação que a API passa a exigir não aparece na grade, e
 * ninguém consegue liberá-la pela tela. Lendo das anotações, o endpoint novo já
 * nasce na grade.
 *
 * O limite: uma ação que só o site confere, sem endpoint que a exija, não
 * aparece. Medido em 2026-10-08, não existe nenhuma — todo `can(...)` e
 * `*pkCan` do site tem um `@PreAuthorize` com o mesmo par. Botão novo que
 * dependa de uma ação sem endpoint precisa entrar aqui à mão.
 *
 * As anotações chegam aqui com as constantes já resolvidas — `TEMPLATES +
 * ":CONSULTAR"` é constante de compilação —, então basta procurar os pares
 * `tela:AÇÃO` no texto.
 */
@Component
public class ScreenActionCatalog {

    static final String BASE_PACKAGE = "com.proautokimium.api";

    private static final Pattern PAR = Pattern.compile(
            "([a-z][a-z0-9/_-]*):(" + String.join("|", nomes()) + ")\\b");

    private final Map<String, Set<Permission>> porTela;

    public ScreenActionCatalog() {
        this.porTela = varrer();
    }

    /**
     * As ações desta tela, na ordem do enum.
     *
     * Tela que nenhuma anotação cita — as que o front só abre ou fecha, como
     * as calculadoras — recebe só `CONSULTAR`, que a grade escreve "Ver". Sem
     * nenhuma, a linha ficaria sem botão e a tela impossível de liberar.
     */
    public List<Permission> actionsOf(String screenCode) {
        Set<Permission> usadas = porTela.get(screenCode);
        if (usadas == null || usadas.isEmpty()) return List.of(Permission.CONSULTAR);
        return List.copyOf(usadas);
    }

    /** Tudo o que foi encontrado, para o teste afirmar contra o catálogo. */
    public Map<String, Set<Permission>> all() {
        return Collections.unmodifiableMap(porTela);
    }

    private static Map<String, Set<Permission>> varrer() {
        Map<String, Set<Permission>> mapa = new TreeMap<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        for (BeanDefinition definicao : scanner.findCandidateComponents(BASE_PACKAGE)) {
            Class<?> controller = ClassUtils.resolveClassName(
                    Objects.requireNonNull(definicao.getBeanClassName()), ScreenActionCatalog.class.getClassLoader());

            ler(AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class), mapa);
            for (Method metodo : controller.getDeclaredMethods()) {
                ler(AnnotatedElementUtils.findMergedAnnotation(metodo, PreAuthorize.class), mapa);
            }
        }
        return mapa;
    }

    private static void ler(PreAuthorize anotacao, Map<String, Set<Permission>> mapa) {
        if (anotacao == null) return;
        Matcher m = PAR.matcher(anotacao.value());
        while (m.find()) {
            mapa.computeIfAbsent(m.group(1), t -> EnumSet.noneOf(Permission.class))
                    .add(Permission.valueOf(m.group(2)));
        }
    }

    private static List<String> nomes() {
        return Arrays.stream(Permission.values()).map(Enum::name).toList();
    }
}

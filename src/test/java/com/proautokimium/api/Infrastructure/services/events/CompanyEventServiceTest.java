package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventRequestDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.TalkDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.TalkRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventConflictException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvalidEventDataException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.SpeakerNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.events.CompanyEventRepository;
import com.proautokimium.api.Infrastructure.repositories.events.SpeakerRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.services.storage.EventImageStorageService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventTalk;
import com.proautokimium.api.domain.entities.events.Speaker;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.enums.events.TalkLocationType;
import com.proautokimium.api.domain.valueObjects.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * As regras dos eventos, com a Poseidon Week de exemplo: 22 a 25/09/2026.
 */
@ExtendWith(MockitoExtension.class)
class CompanyEventServiceTest {

    @Mock CompanyEventRepository eventRepository;
    @Mock SpeakerRepository speakerRepository;
    @Mock CompanyRepository companyRepository;
    @Mock EventImageStorageService imageStorage;

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-14T14:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 14, 11, 30);
    private static final LocalDate DIA_22 = LocalDate.of(2026, 9, 22);
    private static final LocalDate DIA_25 = LocalDate.of(2026, 9, 25);

    private CompanyEventService service;

    @BeforeEach
    void setUp() {
        service = new CompanyEventService(eventRepository, speakerRepository, companyRepository, imageStorage, RELOGIO);
        lenient().when(eventRepository.save(any(CompanyEvent.class))).thenAnswer(i -> comId(i.getArgument(0)));
        lenient().when(eventRepository.saveAndFlush(any(CompanyEvent.class))).thenAnswer(i -> {
            CompanyEvent e = i.getArgument(0);
            e.getTalks().forEach(CompanyEventServiceTest::comId);
            return e;
        });
    }

    // ─── Montagem ────────────────────────────────────────────────────────────

    private static <T extends Entity> T comId(T entidade) {
        try {
            Field id = Entity.class.getDeclaredField("id");
            id.setAccessible(true);
            if (id.get(entidade) == null) id.set(entidade, UUID.randomUUID());
            return entidade;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AddressDTO endereco(String rua, String cidade) {
        return new AddressDTO("87020-900", rua, "5790", null, "Zona 7", cidade, "PR", null);
    }

    private static EventRequestDTO evento(LocalDate inicio, LocalDate fim) {
        return new EventRequestDTO("Poseidon Week", null, inicio, fim, null, null, null, null, false);
    }

    private static TalkRequestDTO palestra(LocalDate dia, String ini, String fim) {
        return new TalkRequestDTO("Descontaminação de pintura", null, dia, LocalTime.parse(ini), LocalTime.parse(fim),
                null, null, null, null, null, List.of());
    }

    private CompanyEvent poseidon() {
        CompanyEvent e = comId(new CompanyEvent());
        e.setName("Poseidon Week");
        e.setStartDate(DIA_22);
        e.setEndDate(DIA_25);
        e.setCreatedAt(AGORA.minusDays(3));
        when(eventRepository.findById(e.getId())).thenReturn(Optional.of(e));
        return e;
    }

    private static EventTalk palestraEm(CompanyEvent e, LocalDate dia) {
        EventTalk t = comId(new EventTalk());
        t.setEvent(e);
        t.setTitle("Palestra");
        t.setDate(dia);
        t.setStartTime(LocalTime.of(14, 0));
        t.setEndTime(LocalTime.of(15, 30));
        e.getTalks().add(t);
        return t;
    }

    // ─── Evento ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("evento novo nasce rascunho, com quem criou e quando")
    void nasceRascunho() throws Exception {
        EventDetailDTO criado = service.create(evento(DIA_22, DIA_25), null, "murillo.henrique");

        assertThat(criado.publishedAt()).as("rascunho nao aparece em Documentos").isNull();
        assertThat(criado.updatedBy()).isEqualTo("murillo.henrique");
        assertThat(criado.updatedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("evento de um dia so e valido, ultimo dia antes do primeiro nao")
    void periodo() {
        assertThatThrownBy(() -> service.create(evento(DIA_25, DIA_22), null, "x"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessageContaining("último dia");

        org.assertj.core.api.Assertions.assertThatCode(() -> service.create(evento(DIA_22, DIA_22), null, "x"))
                .doesNotThrowAnyException();
    }

    /**
     * Encurtar não apaga nem move palestra: as duas saídas "automáticas" mexem
     * na programação de alguém sem avisar.
     */
    @Test
    @DisplayName("encurtar o evento com palestra fora dos dias novos e recusado, dizendo quantas e quais dias")
    void encurtarComPalestraFora() {
        CompanyEvent e = poseidon();
        palestraEm(e, DIA_25);
        palestraEm(e, DIA_25);
        palestraEm(e, LocalDate.of(2026, 9, 23));

        assertThatThrownBy(() -> service.update(e.getId(), evento(DIA_22, LocalDate.of(2026, 9, 24)), null, "x"))
                .isInstanceOf(EventConflictException.class)
                .hasMessage("2 palestras ficariam fora dos dias novos (25/09). Mude as datas delas antes.");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("encurtar sem palestra fora passa")
    void encurtarSemPalestraFora() throws Exception {
        CompanyEvent e = poseidon();
        palestraEm(e, DIA_22);

        EventDetailDTO salvo = service.update(e.getId(), evento(DIA_22, LocalDate.of(2026, 9, 23)), null, "x");

        assertThat(salvo.endDate()).isEqualTo(LocalDate.of(2026, 9, 23));
    }

    @Test
    @DisplayName("local numa empresa do grupo exige a empresa")
    void localEmpresaExigeEmpresa() {
        EventRequestDTO dto = new EventRequestDTO("Poseidon Week", null, DIA_22, DIA_25,
                EventLocationType.COMPANY, null, null, null, false);

        assertThatThrownBy(() -> service.create(dto, null, "x"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("Escolha a empresa do grupo.");
    }

    @Test
    @DisplayName("endereco digitado exige rua e cidade, que e o que o mapa procura")
    void enderecoExigeRuaECidade() {
        EventRequestDTO semCidade = new EventRequestDTO("Poseidon Week", null, DIA_22, DIA_25,
                EventLocationType.ADDRESS, null, "Kartódromo", endereco("Av. Morangueira", null), false);

        assertThatThrownBy(() -> service.create(semCidade, null, "x"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessageContaining("rua e a cidade");
    }

    @Test
    @DisplayName("trocar de endereco para empresa limpa o endereco antigo")
    void trocarLocalLimpaOAnterior() throws Exception {
        CompanyEvent e = poseidon();
        e.setLocationType(EventLocationType.ADDRESS);
        e.setPlaceName("Kartódromo");
        e.setAddress(new Address(null, "Av. Morangueira", "7000", null, null, "Maringá", "PR"));

        Company matriz = comId(new Company());
        matriz.setName("Proauto Kimium · Matriz");
        matriz.setAddress(new Address("87020-900", "Av. Colombo", "5790", null, "Zona 7", "Maringá", "PR"));
        when(companyRepository.findById(matriz.getId())).thenReturn(Optional.of(matriz));

        EventDetailDTO salvo = service.update(e.getId(), new EventRequestDTO("Poseidon Week", null, DIA_22, DIA_25,
                EventLocationType.COMPANY, matriz.getId(), null, null, false), null, "x");

        assertThat(e.getPlaceName()).isNull();
        assertThat(e.getAddress()).isNull();
        assertThat(salvo.location().name()).isEqualTo("Proauto Kimium · Matriz");
        assertThat(salvo.location().address().formatted()).isEqualTo("Av. Colombo, 5790 - Zona 7, Maringá - PR, 87020-900");
    }

    // ─── Rascunho ────────────────────────────────────────────────────────────

    /**
     * 404 e não 403: "existe, mas você não pode" já conta que há um evento sendo
     * montado.
     */
    @Test
    @DisplayName("rascunho responde 404 para quem so ve, e abre para quem cadastra")
    void rascunhoSoParaQuemCadastra() {
        CompanyEvent e = poseidon();

        assertThatThrownBy(() -> service.get(e.getId(), false)).isInstanceOf(EventNotFoundException.class);
        assertThat(service.get(e.getId(), true).name()).isEqualTo("Poseidon Week");
    }

    @Test
    @DisplayName("publicar de novo nao muda a data da primeira publicacao")
    void publicarDeNovoNaoMudaAData() {
        CompanyEvent e = poseidon();
        LocalDateTime primeira = AGORA.minusDays(1);
        e.setPublishedAt(primeira);

        assertThat(service.publish(e.getId(), "x").publishedAt()).isEqualTo(primeira);
    }

    @Test
    @DisplayName("voltar para rascunho tira o evento de Documentos")
    void voltarParaRascunho() {
        CompanyEvent e = poseidon();
        e.setPublishedAt(AGORA.minusDays(1));

        service.unpublish(e.getId(), "x");

        assertThatThrownBy(() -> service.get(e.getId(), false)).isInstanceOf(EventNotFoundException.class);
    }

    // ─── Programação ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("palestra no ultimo dia cabe; no dia seguinte, nao")
    void palestraDentroDoIntervalo() {
        CompanyEvent e = poseidon();

        assertThat(service.addTalk(e.getId(), palestra(DIA_25, "09:00", "10:00"), "x").talks()).hasSize(1);

        assertThatThrownBy(() -> service.addTalk(e.getId(), palestra(DIA_25.plusDays(1), "09:00", "10:00"), "x"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("O dia da palestra precisa estar entre 22/09 e 25/09.");
    }

    @Test
    @DisplayName("fim igual ao inicio e recusado")
    void horarioAoContrario() {
        CompanyEvent e = poseidon();

        assertThatThrownBy(() -> service.addTalk(e.getId(), palestra(DIA_22, "14:00", "14:00"), "x"))
                .isInstanceOf(InvalidEventDataException.class)
                .hasMessage("O horário de fim precisa ser depois do início.");
    }

    @Test
    @DisplayName("a programacao sai em ordem de dia e horario, mesmo cadastrada fora de ordem")
    void programacaoOrdenada() {
        CompanyEvent e = poseidon();

        service.addTalk(e.getId(), palestra(LocalDate.of(2026, 9, 23), "16:00", "17:00"), "x");
        service.addTalk(e.getId(), palestra(DIA_22, "10:45", "12:00"), "x");
        EventDetailDTO detalhe = service.addTalk(e.getId(), palestra(LocalDate.of(2026, 9, 23), "09:00", "10:30"), "x");

        assertThat(detalhe.talks()).extracting(t -> t.date() + " " + t.startTime())
                .containsExactly("2026-09-22 10:45", "2026-09-23 09:00", "2026-09-23 16:00");
    }

    @Test
    @DisplayName("palestrantes saem na ordem escolhida, sem repetir")
    void palestrantesNaOrdem() {
        CompanyEvent e = poseidon();
        Speaker marina = comId(new Speaker());
        marina.setName("Marina Alves");
        Speaker rafael = comId(new Speaker());
        rafael.setName("Rafael Tanaka");
        when(speakerRepository.findAllById(any())).thenReturn(List.of(marina, rafael));

        TalkRequestDTO painel = new TalkRequestDTO("Painel", null, DIA_22, LocalTime.of(16, 0), LocalTime.of(17, 0),
                null, null, null, null, null, List.of(rafael.getId(), marina.getId(), rafael.getId()));

        TalkDTO salva = service.addTalk(e.getId(), painel, "x").talks().get(0);

        assertThat(salva.speakers()).extracting("name").containsExactly("Rafael Tanaka", "Marina Alves");
    }

    @Test
    @DisplayName("palestrante inexistente na palestra responde 404")
    void palestranteInexistente() {
        CompanyEvent e = poseidon();
        when(speakerRepository.findAllById(any())).thenReturn(List.of());

        TalkRequestDTO dto = new TalkRequestDTO("Painel", null, DIA_22, LocalTime.of(16, 0), LocalTime.of(17, 0),
                null, null, null, null, null, List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.addTalk(e.getId(), dto, "x")).isInstanceOf(SpeakerNotFoundException.class);
    }

    /**
     * O site não resolve o local: a palestra "no local do evento" já chega com o
     * endereço do evento, pronta para o mapa.
     */
    @Test
    @DisplayName("palestra no local do evento chega com o endereco do evento; fora, com o proprio")
    void localResolvido() {
        CompanyEvent e = poseidon();
        e.setLocationType(EventLocationType.ADDRESS);
        e.setPlaceName("Proauto Kimium");
        e.setAddress(new Address("87020-900", "Av. Colombo", "5790", null, "Zona 7", "Maringá", "PR"));

        service.addTalk(e.getId(), palestra(DIA_22, "09:00", "10:00"), "x");
        TalkRequestDTO fora = new TalkRequestDTO("Demonstração em pista", null, DIA_22, LocalTime.of(14, 0),
                LocalTime.of(17, 0), null, TalkLocationType.ADDRESS, null, "Kartódromo de Maringá",
                new AddressDTO(null, "Av. Morangueira", "7000", null, "Jardim Alvorada", "Maringá", "PR", null), List.of());

        List<TalkDTO> talks = service.addTalk(e.getId(), fora, "x").talks();

        assertThat(talks.get(0).location().source()).isEqualTo("EVENT");
        assertThat(talks.get(0).location().address().street()).isEqualTo("Av. Colombo");
        assertThat(talks.get(1).location().source()).isEqualTo("ADDRESS");
        assertThat(talks.get(1).location().name()).isEqualTo("Kartódromo de Maringá");
    }

    /**
     * O cadastro de empresas aceita endereço pela metade (é opcional lá). Com
     * rua e sem cidade, o mapa abriria "Av. Colombo, 5790" em qualquer cidade do
     * Brasil — melhor não mostrar mapa nenhum.
     */
    @Test
    @DisplayName("empresa com endereco pela metade chega sem endereco, para o mapa nao abrir em outra cidade")
    void empresaComEnderecoIncompleto() {
        CompanyEvent e = poseidon();
        Company fabrica = comId(new Company());
        fabrica.setName("Proauto Kimium · Fábrica");
        fabrica.setAddress(new Address(null, "Rod. PR-317", "km 5", null, null, null, "PR"));
        when(companyRepository.findById(fabrica.getId())).thenReturn(Optional.of(fabrica));

        TalkRequestDTO dto = new TalkRequestDTO("Visita à fábrica", null, DIA_22, LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, TalkLocationType.COMPANY, fabrica.getId(), null, null, List.of());

        TalkDTO salva = service.addTalk(e.getId(), dto, "x").talks().get(0);

        assertThat(salva.location().name()).isEqualTo("Proauto Kimium · Fábrica");
        assertThat(salva.location().address()).isNull();
    }

    @Test
    @DisplayName("empresa do grupo sem endereco chega sem endereco, e nao com um vazio que abre o mapa")
    void empresaSemEndereco() {
        CompanyEvent e = poseidon();
        Company logistica = comId(new Company());
        logistica.setName("Kimium Logística");
        when(companyRepository.findById(logistica.getId())).thenReturn(Optional.of(logistica));

        TalkRequestDTO dto = new TalkRequestDTO("Visita", null, DIA_22, LocalTime.of(9, 0), LocalTime.of(11, 0),
                null, TalkLocationType.COMPANY, logistica.getId(), null, null, List.of());

        TalkDTO salva = service.addTalk(e.getId(), dto, "x").talks().get(0);

        assertThat(salva.location().name()).isEqualTo("Kimium Logística");
        assertThat(salva.location().address()).isNull();
    }
}

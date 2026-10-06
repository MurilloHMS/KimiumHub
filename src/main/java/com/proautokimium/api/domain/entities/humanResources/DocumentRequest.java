package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "document_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentRequest extends com.proautokimium.api.domain.abstractions.Entity{

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private RequestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form", columnDefinition = "jsonb", nullable = false)
    private List<RequestField> form = new ArrayList<>();

    @Column(name = "template_filename", length = 255)
    private String templateFilename;

    @Column(name = "template_path", length = 500)
    private String templatePath;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Constructor
    private DocumentRequest(String title, String createdBy, LocalDateTime now){
        this.title = title.strip();
        this.createdBy = createdBy;
        this.createdAt = now;
        this.status = RequestStatus.DRAFT;
    }

    // Methods
    public static DocumentRequest draft(String title, String createdBy, LocalDateTime now){
        if(title == null || title.isBlank()) throw new InvalidRequestDataException("Dê um título para a solicitação.");
        return new DocumentRequest(title, createdBy, now);
    }

    /**
     * Título, instruções, prazo e campos: só no rascunho. Depois do envio as
     * respostas usam as chaves dos campos, e mudar o formulário as deixaria órfãs.
     */
    public void updateDraft(String title, String instructions, LocalDate dueDate, List<RequestField> form){
        if(status != RequestStatus.DRAFT) throw new InvalidStatusTransitionException("Só um rascunho pode ser editado.");
        if(title == null || title.isBlank()) throw new InvalidRequestDataException("Dê um título para a solicitação.");
        if(title.strip().length() > 120) throw new InvalidRequestDataException("O título passa de 120 caracteres.");

        List<RequestField> fields = form == null ? List.of() : form;
        Set<String> keys = new HashSet<>();
        for(RequestField field : fields){
            if(field == null || field.key() == null || field.key().isBlank())
                throw new InvalidRequestDataException("Todo campo precisa de uma chave.");
            if(!keys.add(field.key()))
                throw new InvalidRequestDataException("Dois campos com a mesma chave: " + field.key());
            if(field.label() == null || field.label().isBlank())
                throw new InvalidRequestDataException("Todo campo precisa de um nome.");
            if(!RequestField.TYPES.contains(field.type()))
                throw new InvalidRequestDataException("Tipo de campo desconhecido: " + field.type());
            if(RequestField.CHOICE.equals(field.type()) && (field.options() == null || field.options().isEmpty()))
                throw new InvalidRequestDataException("O campo \"" + field.label() + "\" precisa de opções.");
            if(field.documentTypeId() != null && !field.isFile())
                throw new InvalidRequestDataException("Só campo de arquivo vira documento do funcionário.");
        }

        this.title = title.strip();
        this.instructions = instructions == null || instructions.isBlank() ? null : instructions.strip();
        this.dueDate = dueDate;
        this.form = new ArrayList<>(fields);
    }

    /**
     * O arquivo-modelo (o contrato para preencher, a política para assinar), só
     * no rascunho: depois do envio, todos precisam ter recebido o mesmo arquivo.
     * Devolve o caminho do anterior, para o serviço apagar do disco.
     */
    public String attachTemplate(String filename, String path){
        if(status != RequestStatus.DRAFT) throw new InvalidStatusTransitionException("O modelo só muda no rascunho.");
        if(filename == null || filename.isBlank() || path == null || path.isBlank())
            throw new InvalidRequestDataException("Envie o arquivo do modelo.");
        String previous = this.templatePath;
        this.templateFilename = filename;
        this.templatePath = path;
        return previous;
    }

    /**
     * Um rascunho novo com o mesmo título, instruções e campos: a solicitação do
     * ano que vem. Prazo e modelo não vêm junto: o prazo é outro, e o modelo é
     * um arquivo que só pode ter um dono.
     */
    public DocumentRequest duplicate(String createdBy, LocalDateTime now){
        String copyTitle = ("Cópia de " + title);
        DocumentRequest copy = new DocumentRequest(copyTitle.length() > 120 ? copyTitle.substring(0, 120) : copyTitle, createdBy, now);
        copy.instructions = this.instructions;
        copy.form = new ArrayList<>(this.form);
        return copy;
    }

    public boolean isDraft(){
        return status == RequestStatus.DRAFT;
    }

    public void send(LocalDateTime now){
        if(status != RequestStatus.DRAFT) throw new InvalidStatusTransitionException("Só um rascunho pode ser enviado.");

        if(form.isEmpty()) throw new InvalidRequestDataException("Adicione pelo menos um campo antes de enviar.");

        this.status = RequestStatus.OPEN;
        this.sentAt = now;
    }

    public void close(LocalDateTime now){
        if(status != RequestStatus.OPEN) throw new InvalidStatusTransitionException("Só uma solicitação aberta pode ser encerrada");
        this.closedAt = now;
        this.status = RequestStatus.CLOSED;
    }
}

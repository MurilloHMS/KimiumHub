package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.EventTalk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventTalkRepository extends JpaRepository<EventTalk, UUID> {

    /** Em quantas palestras cada palestrante está — a coluna "Palestras". */
    @Query("select s.id, count(t) from EventTalk t join t.speakers s group by s.id")
    List<Object[]> countBySpeaker();

    /** Os eventos em que a pessoa está, para a recusa de exclusão dizer quais. */
    @Query("select distinct t.event.name from EventTalk t join t.speakers s where s.id = :speakerId order by t.event.name")
    List<String> eventNamesWithSpeaker(@Param("speakerId") UUID speakerId);
}

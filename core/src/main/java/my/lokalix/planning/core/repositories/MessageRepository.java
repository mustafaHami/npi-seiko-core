package my.lokalix.planning.core.repositories;

import java.util.UUID;
import my.lokalix.planning.core.models.entities.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {}

package se.fk.rimfrost.framework.regel.komplettering.presentation.kafka;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import java.util.UUID;
import se.fk.rimfrost.framework.regel.komplettering.logic.IdentitetUppdateradEventHandler;
import se.fk.rimfrost.identitet.uppdaterad.IdentitetUppdateradEvent;

/** Kafka consumer for {@link IdentitetUppdateradEvent} (FRKOMP-FR-11.1). */
@SuppressWarnings("unused")
@ApplicationScoped
public class IdentitetUppdateradEventConsumer
{
   @Inject
   IdentitetUppdateradEventHandler handler;

   /**
    * Receives {@link IdentitetUppdateradEvent} from the configured Kafka topic (FRKOMP-FR-11.1).
    *
    * @param event the received event
    */
   @Incoming("identitet-uppdaterad")
   @Blocking
   public void onMessage(IdentitetUppdateradEvent event)
   {
      handler.handleIdentitetUppdaterad(UUID.fromString(event.getPrenumerationId()));
   }
}

package se.fk.rimfrost.framework.regel.komplettering.presentation.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import se.fk.rimfrost.identitet.uppdaterad.IdentitetUppdateradEvent;

/** Deserializer for {@link IdentitetUppdateradEvent} Kafka messages. */
@SuppressWarnings("unused")
public class IdentitetUppdateradEventDeserializer extends ObjectMapperDeserializer<IdentitetUppdateradEvent>
{
   /** Creates a deserializer for {@link IdentitetUppdateradEvent}. */
   public IdentitetUppdateradEventDeserializer()
   {
      super(IdentitetUppdateradEvent.class);
   }
}

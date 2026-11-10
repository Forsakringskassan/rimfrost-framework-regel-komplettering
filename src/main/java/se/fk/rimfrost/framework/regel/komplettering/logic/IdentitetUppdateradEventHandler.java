package se.fk.rimfrost.framework.regel.komplettering.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import java.util.UUID;

/**
 * Handles IdentitetUppdateradEvent received from identitet-prenumeration-tjänsten.
 * Completes the komplettering flow automatically if the yrkande is now complete (FRKOMP-FR-11).
 */
@ApplicationScoped
public class IdentitetUppdateradEventHandler
{
   private static final Logger LOGGER = LoggerFactory.getLogger(IdentitetUppdateradEventHandler.class);

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   RegelKompletteringService<?> regelKompletteringService;

   @Inject
   RegelKompletteringDoneHandler regelKompletteringDoneHandler;

   /**
    * Processes an IdentitetUppdateradEvent by fetching the current handlaggning and
    * completing the komplettering if the yrkande is now complete (FRKOMP-FR-11.2, FR-11.3).
    * If komplettering is still required, the event is ignored (FRKOMP-FR-11.4).
    * Errors are logged without sending a Kafka response (FRKOMP-FR-11.5).
    *
    * @param handlaggningId the prenumerationId from the event, used as handlaggningId (FRKOMP-FR-11.1)
    */
   public void handleIdentitetUppdaterad(UUID handlaggningId)
   {
      try
      {
         var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
         if (regelKompletteringService.isKompletteringRequired(handlaggning))
         {
            LOGGER.info("Komplettering still required for handlaggningId: {}, ignoring IdentitetUppdateradEvent",
                  handlaggningId);
            return;
         }
         regelKompletteringDoneHandler.handleKompletteringDone(handlaggning);
      }
      catch (Exception e)
      {
         LOGGER.error("Error handling IdentitetUppdateradEvent for handlaggningId: {}", handlaggningId, e);
      }
   }
}

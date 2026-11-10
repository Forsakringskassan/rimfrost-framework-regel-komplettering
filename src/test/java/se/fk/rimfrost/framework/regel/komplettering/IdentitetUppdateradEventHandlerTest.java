package se.fk.rimfrost.framework.regel.komplettering;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.regel.komplettering.logic.IdentitetUppdateradEventHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringDoneHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import java.util.UUID;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class IdentitetUppdateradEventHandlerTest extends AbstractRegelKompletteringTestBase
{
   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   RegelKompletteringService<?> regelKompletteringService;

   @InjectMock
   RegelKompletteringDoneHandler regelKompletteringDoneHandler;

   @Inject
   IdentitetUppdateradEventHandler handler;

   @Test
   @DisplayName("FRKOMP-FR-11.2, FRKOMP-FR-11.3: handleKompletteringDone is called when komplettering is complete")
   void should_call_done_handler_when_komplettering_complete() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = Mockito.mock(Handlaggning.class);

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(handlaggning)).thenReturn(false);

      handler.handleIdentitetUppdaterad(handlaggningId);

      verify(regelKompletteringDoneHandler).handleKompletteringDone(handlaggning);
   }

   @Test
   @DisplayName("FRKOMP-FR-11.4: event is ignored when komplettering is still required")
   void should_ignore_event_when_komplettering_still_required() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = Mockito.mock(Handlaggning.class);

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(handlaggning)).thenReturn(true);

      handler.handleIdentitetUppdaterad(handlaggningId);

      verify(regelKompletteringDoneHandler, never()).handleKompletteringDone(Mockito.any());
   }

   @Test
   @DisplayName("FRKOMP-FR-11.5: exception is logged and swallowed, no response sent")
   void should_swallow_exception_on_error() throws Exception
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenThrow(new RuntimeException("read error"));

      handler.handleIdentitetUppdaterad(handlaggningId);

      verify(regelKompletteringDoneHandler, never()).handleKompletteringDone(Mockito.any());
   }
}

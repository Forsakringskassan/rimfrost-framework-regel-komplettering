package se.fk.rimfrost.framework.regel.komplettering;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.oul.model.OperativUppgift;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringRequestHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.CorrelationDataReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.EndOulUppgiftException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.HandlaggningReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.KompletteringIncompleteException;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class RegelKompletteringRequestHandlerTest extends AbstractRegelKompletteringTestBase
{
   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   RegelKompletteringService<?> regelKompletteringService;

   @Inject
   RegelKompletteringRequestHandler regelKompletteringRequestHandler;

   private Handlaggning createHandlaggning()
   {
      var yrkande = Mockito.mock(Yrkande.class);
      var handlaggning = Mockito.mock(Handlaggning.class);

      Mockito.when(yrkande.erbjudandeId()).thenReturn("b1cb6133-11af-4a30-9180-5ea8104a74c7");
      Mockito.when(handlaggning.yrkande()).thenReturn(yrkande);
      Mockito.when(handlaggning.id()).thenReturn(UUID.randomUUID());
      Mockito.when(handlaggning.version()).thenReturn(1);
      Mockito.when(handlaggning.processInstansId()).thenReturn(UUID.randomUUID());
      Mockito.when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      Mockito.when(handlaggning.avslutadTS()).thenReturn(OffsetDateTime.now());
      Mockito.when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());

      return handlaggning;
   }

   @Test
   @DisplayName("FRKOMP-FR-03.1, FRKOMP-FR-03.2: Kafka response with Utfall=JA is sent directly if komplettering not required")
   void should_not_create_oul_task_if_komplettering_not_required() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(false);

      regelKafkaConnector.sendRegelRequest(handlaggningId.toString(), responseTopic);
      var response = regelKafkaConnector.waitForRegelResponse();

      assertEquals(Utfall.JA, response.getData().getUtfall());
      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(regelKompletteringService).isKompletteringRequired(Mockito.any());
      verifyNoInteractions(oulUppgiftService);
   }

   @Test
   @DisplayName("FRKOMP-FR-03.1, FRKOMP-FR-03.3: OUL uppgift is created if komplettering is required")
   void should_create_oul_task_if_komplettering_required() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(true);

      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      Mockito.when(oulUppgiftService.createOulUppgift(captor.capture())).thenReturn(Mockito.mock(OperativUppgift.class));

      regelKafkaConnector.sendRegelRequest(handlaggningId.toString(), responseTopic);

      verify(handlaggningAdapter, timeout(5000)).readHandlaggning(handlaggningId);
      verify(regelKompletteringService, timeout(5000)).isKompletteringRequired(Mockito.any());
      verify(oulUppgiftService, timeout(5000)).createOulUppgift(Mockito.any());
      var oulUppgiftSpec = captor.getValue();

      assertEquals(handlaggningId, oulUppgiftSpec.handlaggningId());
      assertNotNull(oulUppgiftSpec.aktivitetId());
      assertNotNull(oulUppgiftSpec.handlaggning());
      assertEquals(responseTopic, oulUppgiftSpec.replyTo());
      assertNotNull(oulUppgiftSpec.cloudEventData());
      assertNotNull(oulUppgiftSpec.cloudEventAttributes());
      assertEquals("TestUppgiftNamn", oulUppgiftSpec.regel());
      assertEquals("TestUppgiftBeskrivning", oulUppgiftSpec.beskrivning());
      assertEquals("C", oulUppgiftSpec.verksamhetslogik());
      assertEquals("ANSVARIG_HANDLAGGARE", oulUppgiftSpec.roll());
      assertEquals("/regel/komplettering", oulUppgiftSpec.url());
      assertNotNull(oulUppgiftSpec.erbjudande());
      assertEquals(UUID.fromString("a42ffaed-2f20-47e8-8499-f2f79ae2f45f"), oulUppgiftSpec.uppgiftSpecifikationId());
      assertEquals(1, oulUppgiftSpec.uppgiftSpecifikationVersion());
   }

   @Test
   @DisplayName("FRKOMP-FR-03.5: Kafka error is sent with error code RIMFROST_OTHER on runtime exception during handlaggning read")
   void should_send_error_response_on_handlaggning_read_failure_at_regel_request() throws Exception
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenThrow(new RuntimeException());

      regelKafkaConnector.sendRegelRequest(handlaggningId.toString(), responseTopic);
      var response = regelKafkaConnector.waitForRegelResponse();

      assertEquals(Utfall.ERROR, response.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, response.getData().getError().getFelkod());
      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verifyNoInteractions(regelKompletteringService);
      verifyNoInteractions(oulUppgiftService);
   }

   @Test
   @DisplayName("FRKOMP-FR-03.6: Kafka error is sent with error code RIMFROST_OTHER on exception during komplettering service check")
   void should_send_error_response_on_komplettering_service_exception_at_regel_request() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenThrow(new RuntimeException());

      regelKafkaConnector.sendRegelRequest(handlaggningId.toString(), responseTopic);
      var response = regelKafkaConnector.waitForRegelResponse();

      assertEquals(Utfall.ERROR, response.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, response.getData().getError().getFelkod());
      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(regelKompletteringService).isKompletteringRequired(Mockito.any());
      verifyNoInteractions(oulUppgiftService);
   }

   @Test
   @DisplayName("FRKOMP-FR-03.4: Kafka error is sent with error code RIMFROST_OTHER on OUL exception")
   void should_send_error_response_on_oul_uppgift_service_exception_at_regel_request() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(true);
      Mockito.when(oulUppgiftService.createOulUppgift(Mockito.any())).thenThrow(new RuntimeException());

      regelKafkaConnector.sendRegelRequest(handlaggningId.toString(), responseTopic);
      var response = regelKafkaConnector.waitForRegelResponse();

      assertEquals(Utfall.ERROR, response.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, response.getData().getError().getFelkod());
      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(regelKompletteringService).isKompletteringRequired(Mockito.any());
   }

   @Test
   @DisplayName("FRKOMP-FR-01.4, FRKOMP-FR-04.4: Kafka response with Utfall=JA is sent on successful komplettering")
   void should_send_success_response_on_successful_done_handling() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(false);
      Mockito.doNothing().when(oulUppgiftService).endOulUppgift(eq(DEFAULT_UPPGIFT_ID), Mockito.any());
      Mockito.doNothing().when(oulUppgiftService).cleanupCorrelation(handlaggningId);
      Mockito.when(handlaggningAdapter.updateHandlaggning(Mockito.any())).thenReturn(Mockito.mock(HandlaggningUpdate.class));

      regelKompletteringRequestHandler.handleKompletteringDone(handlaggningId);
      var response = regelKafkaConnector.waitForRegelResponse();

      assertEquals(Utfall.JA, response.getData().getUtfall());
      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(regelKompletteringService).isKompletteringRequired(Mockito.any());
      verify(oulUppgiftService).endOulUppgift(eq(DEFAULT_UPPGIFT_ID), Mockito.any());
      verify(oulUppgiftService).cleanupCorrelation(handlaggningId);
      verify(handlaggningAdapter).updateHandlaggning(Mockito.any());
   }

   @Test
   @DisplayName("FRKOMP-FR-04.5: CorrelationDataReadException is thrown if correlation data is missing")
   void should_throw_correlation_data_read_exception_on_missing_data_at_done_handling() throws Exception
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.when(oulUppgiftService.getCorrelationData(handlaggningId)).thenReturn(null);

      assertThrows(CorrelationDataReadException.class,
            () -> regelKompletteringRequestHandler.handleKompletteringDone(handlaggningId));
   }

   @Test
   @DisplayName("HandlaggningReadException is thrown on failure to read handlaggning")
   void should_throw_handlaggning_read_exception_on_handlaggning_read_failure_at_done_handling() throws Exception
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenThrow(new HandlaggningException(HandlaggningException.ErrorType.UNEXPECTED_ERROR, ""));

      assertThrows(HandlaggningReadException.class,
            () -> regelKompletteringRequestHandler.handleKompletteringDone(handlaggningId));
   }

   @Test
   @DisplayName("FRKOMP-FR-04.3: KompletteringIncompleteException is thrown if komplettering is not complete")
   void should_throw_komplettering_incomplete_exception_on_komplettering_still_required_at_done_handling() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(true);

      assertThrows(KompletteringIncompleteException.class,
            () -> regelKompletteringRequestHandler.handleKompletteringDone(handlaggningId));
   }

   @Test
   @DisplayName("EndOulUppgiftException is thrown on failure to end oul uppgift")
   void should_throw_end_oul_uppgift_exception_on_oul_uppgift_end_failure_at_done_handling() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(false);
      Mockito.doThrow(new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "")).when(oulUppgiftService)
            .endOulUppgift(eq(DEFAULT_UPPGIFT_ID), Mockito.any());

      assertThrows(EndOulUppgiftException.class, () -> regelKompletteringRequestHandler.handleKompletteringDone(handlaggningId));
   }

   @Test
   @DisplayName("Handlaggning update failure does not throw exception")
   void should_not_throw_on_handlaggning_update_failure_at_done_handling() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.isKompletteringRequired(Mockito.any())).thenReturn(false);
      Mockito.doNothing().when(oulUppgiftService).endOulUppgift(eq(DEFAULT_UPPGIFT_ID), Mockito.any());
      Mockito.doNothing().when(oulUppgiftService).cleanupCorrelation(handlaggningId);
      Mockito.when(handlaggningAdapter.updateHandlaggning(Mockito.any())).thenThrow(new RuntimeException());

      regelKompletteringRequestHandler.handleKompletteringDone(handlaggningId);
      var response = regelKafkaConnector.waitForRegelResponse();

      assertEquals(Utfall.JA, response.getData().getUtfall());
      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(regelKompletteringService).isKompletteringRequired(Mockito.any());
      verify(oulUppgiftService).endOulUppgift(eq(DEFAULT_UPPGIFT_ID), Mockito.any());
      verify(oulUppgiftService).cleanupCorrelation(handlaggningId);
   }
}

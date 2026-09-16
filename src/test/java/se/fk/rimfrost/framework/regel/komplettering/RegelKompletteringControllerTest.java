package se.fk.rimfrost.framework.regel.komplettering;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.Path;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringDoneHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.CorrelationDataReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.EndOulUppgiftException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.HandlaggningNotFoundException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.HandlaggningReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.KompletteringIncompleteException;
import se.fk.rimfrost.framework.regel.logic.dto.ImmutableRegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.RegelKompletteringController;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class RegelKompletteringControllerTest extends AbstractRegelKompletteringTestBase
{
   /** Minimal concrete subclass registered as a JAX-RS resource for this test. */
   @Path("/api/test")
   public static class TestKompletteringController extends RegelKompletteringController<String>
   {
   }

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   RegelKompletteringService<String> regelKompletteringService;

   @InjectMock
   RegelKompletteringDoneHandler regelKompletteringDoneHandler;

   private RegelDataRequest regelDataRequest(UUID handlaggningId)
   {
      return ImmutableRegelDataRequest.builder()
            .id(UUID.randomUUID())
            .handlaggningId(handlaggningId)
            .aktivitetId(UUID.randomUUID())
            .replyTo("reply-topic")
            .type("test-type")
            .kogitorootprocid("root-proc-id")
            .kogitorootprociid(UUID.randomUUID())
            .kogitoparentprociid(UUID.randomUUID())
            .kogitoprocid("proc-id")
            .kogitoprocinstanceid(UUID.randomUUID())
            .kogitoprocist("proc-ist")
            .kogitoprocversion("1.0")
            .build();
   }

   private Handlaggning createHandlaggning()
   {
      var yrkande = Mockito.mock(Yrkande.class);
      Mockito.when(yrkande.individYrkandeRoller()).thenReturn(List.of());
      var handlaggning = Mockito.mock(Handlaggning.class);
      Mockito.when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   @Test
   @DisplayName("kompletteringDone returns 204 on success")
   void should_return_204_on_done_when_successful()
   {
      var handlaggningId = UUID.randomUUID();

      RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(204);

      verify(regelKompletteringDoneHandler).handleKompletteringDone(handlaggningId);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.5: kompletteringDone returns 409 when no correlation data is in storage")
   void should_return_409_on_done_when_correlation_data_read_exception()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new CorrelationDataReadException("")).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(handlaggningId);

      RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(409);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.3: kompletteringDone returns 422 when yrkande is still incomplete")
   void should_return_422_on_done_when_komplettering_incomplete()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new KompletteringIncompleteException()).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(handlaggningId);
      RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(422);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.7: kompletteringDone returns 404 when handlaggning not found")
   void should_return_404_on_done_when_handlaggning_not_found()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new HandlaggningNotFoundException("", new Throwable())).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(handlaggningId);
      RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(404);
   }

   @Test
   @DisplayName("kompletteringDone returns 500 when handlaggning read fails")
   void should_return_500_on_done_when_handlaggning_read_fails()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new HandlaggningReadException("", new Throwable())).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(handlaggningId);
      RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(500);
   }

   @Test
   @DisplayName("kompletteringDone returns 500 when OUL end fails")
   void should_return_500_on_done_when_oul_end_fails()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new EndOulUppgiftException("", new Throwable())).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(handlaggningId);
      RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(500);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.1: GET returns 200 on success")
   void should_return_200_on_get_success() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(sidAdapter.containsSid(Mockito.anyList())).thenReturn(false);
      Mockito.when(regelKompletteringService.readSvarData(Mockito.any())).thenReturn("response");
      RestAssured.given()
            .get("/api/test/" + handlaggningId)
            .then()
            .statusCode(200);

      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(sidAdapter).containsSid(Mockito.anyList());
      verify(regelKompletteringService).readSvarData(Mockito.any());
   }

   @Test
   @DisplayName("FRKOMP-FR-05.1, FRKOMP-FR-05.3: GET returns 403 on SID check failure")
   void should_unassign_on_sid_check_failure_at_get() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(sidAdapter.containsSid(Mockito.anyList())).thenReturn(true);
      Mockito.doNothing().when(oulUppgiftService).tryUnassignOulUppgift(DEFAULT_UPPGIFT_ID);
      RestAssured.given()
            .get("/api/test/" + handlaggningId)
            .then()
            .statusCode(403);

      verify(handlaggningAdapter).readHandlaggning(handlaggningId);
      verify(sidAdapter).containsSid(Mockito.anyList());
      verify(oulUppgiftService).tryUnassignOulUppgift(DEFAULT_UPPGIFT_ID);
      verifyNoInteractions(regelKompletteringService);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.2: PATCH returns 204 on success")
   void should_return_204_on_patch_success() throws HandlaggningException
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();
      var requestBody = "";

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.registerSvar(Mockito.any(), eq(requestBody)))
            .thenReturn(Mockito.mock(HandlaggningUpdate.class));

      RestAssured.given()
            .body(requestBody)
            .patch("/api/test/" + UUID.randomUUID())
            .then()
            .statusCode(204);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.9: PATCH returns 500 on conflict error during handlaggning update")
   void should_return_500_on_handlaggning_update_conflict_during_patch() throws HandlaggningException
   {
      var handlaggningId = UUID.randomUUID();
      var handlaggning = createHandlaggning();
      var requestBody = "";

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId)).thenReturn(handlaggning);
      Mockito.when(regelKompletteringService.registerSvar(Mockito.any(), eq(requestBody)))
            .thenReturn(Mockito.mock(HandlaggningUpdate.class));
      Mockito.when(handlaggningAdapter.updateHandlaggning(Mockito.any()))
            .thenThrow(new HandlaggningException(HandlaggningException.ErrorType.CONFLICT, "Version conflict detected"));

      RestAssured.given()
            .body(requestBody)
            .patch("/api/test/" + UUID.randomUUID())
            .then()
            .statusCode(500);
   }
}

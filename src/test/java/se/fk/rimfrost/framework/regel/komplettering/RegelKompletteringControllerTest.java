package se.fk.rimfrost.framework.regel.komplettering;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.Path;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.adapter.identity.exception.IdentityException;
import se.fk.rimfrost.adapter.identity.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringDoneHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.CorrelationDataReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.EndOulUppgiftException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.KompletteringIncompleteException;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.RegelKompletteringController;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.ErrorResponse;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class RegelKompletteringControllerTest extends AbstractRegelKompletteringTestBase
{
   private static final String NOT_ASSIGNED_PATTERN = "(?i).*not assigned to uppgift.*";

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

   private Handlaggning createHandlaggning()
   {
      var yrkande = Mockito.mock(Yrkande.class);
      Mockito.when(yrkande.individYrkandeRoller()).thenReturn(List.of());
      var handlaggning = Mockito.mock(Handlaggning.class);
      Mockito.when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   @BeforeEach
   void setupHandlaggning() throws HandlaggningException
   {
      var handlaggning = createHandlaggning();
      Mockito.when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
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

      verify(regelKompletteringDoneHandler).handleKompletteringDone(any());
   }

   @Test
   @DisplayName("FRKOMP-FR-04.5: kompletteringDone returns 500 when no correlation data is in storage")
   void should_return_500_on_done_when_correlation_data_read_exception()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new CorrelationDataReadException("")).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(any());

      var response = RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(500).extract().as(ErrorResponse.class);
      assertEquals(500, response.getCode());
   }

   @Test
   @DisplayName("FRKOMP-FR-04.3: kompletteringDone returns 422 when yrkande is still incomplete")
   void should_return_422_on_done_when_komplettering_incomplete()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new KompletteringIncompleteException()).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(any());
      var response = RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(422).extract().as(ErrorResponse.class);
      assertEquals(422, response.getCode());
   }

   @Test
   @DisplayName("FRKOMP-FR-04.7: kompletteringDone returns 404 when handlaggning not found")
   void should_return_404_on_done_when_handlaggning_not_found() throws HandlaggningException
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenThrow(new HandlaggningException(HandlaggningException.ErrorType.NOT_FOUND, "Handlaggning not found"));
      var response = RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(404).extract().as(ErrorResponse.class);
      assertEquals(404, response.getCode());
      verifyNoInteractions(regelKompletteringDoneHandler);
   }

   @Test
   @DisplayName("FRKOMP-FR-04.13: kompletteringDone returns 500 when handlaggning read fails")
   void should_return_500_on_done_when_handlaggning_read_fails() throws HandlaggningException
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.when(handlaggningAdapter.readHandlaggning(handlaggningId))
            .thenThrow(new HandlaggningException(HandlaggningException.ErrorType.UNEXPECTED_ERROR, "Unexpected error"));
      var response = RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(500).extract().as(ErrorResponse.class);
      assertEquals(500, response.getCode());
      verifyNoInteractions(regelKompletteringDoneHandler);
   }

   @Test
   @DisplayName("kompletteringDone returns 500 when OUL end fails")
   void should_return_500_on_done_when_oul_end_fails()
   {
      var handlaggningId = UUID.randomUUID();

      Mockito.doThrow(new EndOulUppgiftException("", new Throwable())).when(regelKompletteringDoneHandler)
            .handleKompletteringDone(any());
      var response = RestAssured.given()
            .post("/api/test/" + handlaggningId + "/done")
            .then()
            .statusCode(500).extract().as(ErrorResponse.class);
      assertEquals(500, response.getCode());
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
            .patch("/api/test/" + handlaggningId)
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

      var response = RestAssured.given()
            .body(requestBody)
            .patch("/api/test/" + handlaggningId)
            .then()
            .statusCode(500).extract().as(ErrorResponse.class);

      assertEquals(500, response.getCode());
      assertTrue(response.getMessage().matches("(?i).*conflict.*"));
      assertTrue(response.getMessage().matches("(?i).*programming fault.*"));
   }

   @Test
   @DisplayName("FRKOMP-FR-07.1: GET returns 401 when user identity cannot be verified")
   void should_return_401_on_get_when_identity_unauthorized() throws Exception
   {
      Mockito.when(identityAdapter.getIdentity(any()))
            .thenThrow(new IdentityException(IdentityException.ErrorType.UNAUTHORIZED, "no token"));

      var response = RestAssured.given()
            .get("/api/test/" + UUID.randomUUID())
            .then()
            .statusCode(401).extract().as(ErrorResponse.class);

      assertEquals(401, response.getCode());
      verifyNoInteractions(handlaggningAdapter, regelKompletteringService);
   }

   @Test
   @DisplayName("FRKOMP-FR-06.1: PATCH returns 403 and does not update handlaggning when handlaggare is not assigned to uppgift")
   void should_return_403_on_patch_when_not_assigned_to_uppgift() throws Exception
   {
      givenUnassignedHandlaggare();

      var response = RestAssured.given()
            .body("")
            .patch("/api/test/" + UUID.randomUUID())
            .then()
            .statusCode(403).extract().as(ErrorResponse.class);

      assertEquals(403, response.getCode());
      assertTrue(response.getMessage().matches(NOT_ASSIGNED_PATTERN));

      verify(regelKompletteringService, never()).registerSvar(any(), any());
      verify(handlaggningAdapter, never()).updateHandlaggning(any());
   }

   @Test
   @DisplayName("FRKOMP-FR-06.1: kompletteringDone returns 403 when handlaggare is not assigned to uppgift")
   void should_return_403_on_done_when_not_assigned_to_uppgift() throws Exception
   {
      givenUnassignedHandlaggare();

      var response = RestAssured.given()
            .post("/api/test/" + UUID.randomUUID() + "/done")
            .then()
            .statusCode(403).extract().as(ErrorResponse.class);

      assertEquals(403, response.getCode());
      assertTrue(response.getMessage().matches(NOT_ASSIGNED_PATTERN));

      verifyNoInteractions(regelKompletteringDoneHandler);
   }

   @Test
   @DisplayName("GET utokadUppgiftsbeskrivning returns 200 for a verified user identity")
   void should_return_200_on_get_utokad_uppgiftsbeskrivning() throws Exception
   {
      var response = RestAssured.given()
            .get("/api/test/utokadUppgiftsbeskrivning")
            .then()
            .statusCode(200).extract().as(GetUtokadUppgiftsbeskrivningResponse.class);

      assertEquals("TestUtokadUppgiftsbeskrivning", response.getBeskrivning());
      verify(identityAdapter).getIdentity(any());
   }

   private void givenUnassignedHandlaggare() throws IdentityException
   {
      Mockito.when(identityAdapter.getIdentity(any()))
            .thenReturn(ImmutableIdtyp.builder().typId(ID_TYP).varde("19700101-0000").build());
   }
}

package se.fk.rimfrost.framework.regel.komplettering.presentation.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringDoneHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.guard.RegelAccessGuard;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.RegelOulControllerApi;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;

/**
 * Abstract base controller for komplettering endpoints. Extend with the regel's own
 * OpenAPI-generated data type:
 *
 * <pre>
 * {@code @Path("/api/rtf")}
 * public class RtfKompletteringController
 *         extends KompletteringController<RtfKompletteringData> {}
 * </pre>
 *
 * @param <T> shared data type for the GET response and PATCH request body (FRALL-FR-07.7)
 */
public abstract class RegelKompletteringController<T> implements RegelOulControllerApi
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelKompletteringController.class);

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   RegelKompletteringService<T> kompletteringService;

   @Inject
   RegelKompletteringDoneHandler regelKompletteringDoneHandler;

   @Inject
   RegelConfigProvider regelConfigProvider;

   @Inject
   RegelAccessGuard regelAccessGuard;

   /**
    * Returns the data the handläggare needs to register the sökande's svar.
    *
    * @param handlaggningId the handlaggning under komplettering
    * @return 200 with svar data, or 404/503 if handlaggning is unavailable
    */
   @GET
   @Path("/{handlaggningId}")
   public Response getKomplettering(@PathParam("handlaggningId") UUID handlaggningId)
   {
      var handlaggning = fetchHandlaggning(handlaggningId);
      regelAccessGuard.verifyAccess(handlaggning);
      return Response.ok(kompletteringService.readSvarData(handlaggning)).build();
   }

   /**
    * Registers the sökande's svar for the missing attributes.
    *
    * @param handlaggningId the handlaggning under komplettering
    * @param request        the handläggare's registered svar
    */
   @PATCH
   @Path("/{handlaggningId}")
   public void patchKomplettering(@PathParam("handlaggningId") UUID handlaggningId,
         @Valid @NotNull T request)
   {
      var handlaggning = fetchHandlaggning(handlaggningId);
      regelAccessGuard.verifyAccess(handlaggning);
      var update = kompletteringService.registerSvar(handlaggning, request);
      updateHandlaggning(handlaggningId, update);
   }

   /**
    * Marks komplettering as complete.
    *
    * <p>Calls {@code checkKomplettering()} to verify the yrkande is now complete — returns
    * HTTP 422 if attributes are still missing. On success, ends the OUL task,
    * clears correlation storage, and returns HTTP 204.
    *
    * <ul>
    *   <li>HTTP 204 — komplettering accepted and OUL task closed successfully.
    *   <li>HTTP 401 — valid user identity was not provided.
    *   <li>HTTP 403 — user does not have the required access rights.
    *   <li>HTTP 422 — yrkande is still incomplete.
    *   <li>HTTP 500 — any internal error, such as that timeout has already cleared correlation storage.
    * </ul>
    *
    * @param handlaggningId the handlaggning whose komplettering is done
    */
   @POST
   @Path("/{handlaggningId}/done")
   public void markDone(@PathParam("handlaggningId") UUID handlaggningId)
   {
      var handlaggning = fetchHandlaggning(handlaggningId);
      regelAccessGuard.verifyAccess(handlaggning);
      regelKompletteringDoneHandler.handleKompletteringDone(handlaggning);
   }

   private Handlaggning fetchHandlaggning(UUID handlaggningId)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         throw new WebApplicationException(buildErrorResponse(toHttpStatus(e).getStatusCode(), e.getMessage()));
      }
   }

   private void updateHandlaggning(UUID handlaggningId, HandlaggningUpdate update)
   {
      try
      {
         handlaggningAdapter.updateHandlaggning(update);
      }
      catch (HandlaggningException e)
      {
         if (e.getErrorType() == HandlaggningException.ErrorType.CONFLICT)
         {
            LOGGER.error(
                  "Version conflict while attempting to update handlaggning with id: {}. Programming fault in regel komplettering service?",
                  handlaggningId, e);
            throw new WebApplicationException(buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                  "Version conflict while attempting to update handlaggning with id: " + handlaggningId
                        + ". Programming fault in regel komplettering service?"));
         }

         LOGGER.error("Failed to update handlaggning. handlaggningId: {}", handlaggningId, e);
         throw new WebApplicationException(buildErrorResponse(toHttpStatus(e).getStatusCode(), e.getMessage()));
      }
   }

   /**
    * Returns the extended description of the task the handläggare should perform
    *
    * @return 200 with the extended description
    */
   @GET
   @Path("/utokadUppgiftsbeskrivning")
   @Override
   public GetUtokadUppgiftsbeskrivningResponse getUtokadUppgiftsbeskrivning()
   {
      var regelConfig = regelConfigProvider.getConfig();

      GetUtokadUppgiftsbeskrivningResponse response = new GetUtokadUppgiftsbeskrivningResponse();
      response.setBeskrivning(regelConfig.getUtokadUppgiftsbeskrivning().getBeskrivning());

      return response;
   }

   private Response.Status toHttpStatus(HandlaggningException e)
   {
      return switch (e.getErrorType())
      {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   private static Response buildErrorResponse(int statusCode, String message)
   {
      return Response.status(statusCode, message).build();
   }
}

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

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringDoneHandler;
import se.fk.rimfrost.framework.regel.komplettering.logic.RegelKompletteringService;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import se.fk.rimfrost.framework.sid.exception.SidException;
import se.fk.rimfrost.framework.sid.model.Idtyp;

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
public abstract class RegelKompletteringController<T>
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelKompletteringController.class);

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   SidAdapter sidAdapter;

   @Inject
   RegelKompletteringService<T> kompletteringService;

   @Inject
   RegelKompletteringDoneHandler regelKompletteringDoneHandler;
   @Inject
   OulUppgiftService oulUppgiftService;

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
      if (checkSid(handlaggning))
      {
         unassignUppgift(handlaggningId);
         return Response.status(Response.Status.FORBIDDEN.getStatusCode(), "Skyddad identitet").build();
      }
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
    *   <li>HTTP 409 — timeout has already cleared correlation storage.
    *   <li>HTTP 422 — yrkande is still incomplete.
    * </ul>
    *
    * @param handlaggningId the handlaggning whose komplettering is done
    * @return 204 on success
    */
   @POST
   @Path("/{handlaggningId}/done")
   public Response kompletteringDone(@PathParam("handlaggningId") UUID handlaggningId)
   {
      regelKompletteringDoneHandler.handleKompletteringDone(handlaggningId);
      return Response.noContent().build();
   }

   private Handlaggning fetchHandlaggning(UUID handlaggningId)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         throw new WebApplicationException(toHttpStatus(e));
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
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
         }

         LOGGER.error("Failed to update handlaggning. handlaggningId: {}", handlaggningId, e);
         throw new WebApplicationException(toHttpStatus(e));
      }
   }

   /**
    * Checks whether any individ on the handläggning has a protected identity (SID).
    * Returns {@code true} if SID is detected, {@code false} otherwise.
    * Maps {@link SidException} to an appropriate HTTP status on service errors.
    */
   private boolean checkSid(Handlaggning handlaggning)
   {
      try
      {
         return sidAdapter.containsSid(extractIndivider(handlaggning));
      }
      catch (SidException e)
      {
         LOGGER.error("Error checking SID for handlaggningsId: {}", handlaggning.id(), e);
         throw new WebApplicationException(toHttpStatus(e));
      }
   }

   /**
    * Unassigns the OUL uppgift for the given handläggning so it returns to an unassigned state.
    * Delegates to {@link OulUppgiftService#tryUnassignOulUppgift}, which logs and swallows any
    * failure so it never affects the HTTP response (FRMM-FR-08.8).
    */
   private void unassignUppgift(UUID handlaggningId)
   {
      var correlationData = oulUppgiftService.getCorrelationData(handlaggningId);
      if (correlationData == null)
      {
         LOGGER.warn("No correlation data found for handlaggningId: {}, skipping unassign", handlaggningId);
         return;
      }
      var oulUppgiftId = correlationData.oulUppgiftId();
      if (oulUppgiftId == null)
      {
         LOGGER.warn("No oulUppgiftId found for handlaggningId: {}, skipping unassign", handlaggningId);
         return;
      }
      oulUppgiftService.tryUnassignOulUppgift(oulUppgiftId);
   }

   /**
    * Extracts individer from the handläggning's yrkande and maps them to the SID adapter's
    * {@link se.fk.rimfrost.framework.sid.model.Idtyp} type.
    */
   private List<Idtyp> extractIndivider(Handlaggning handlaggning)
   {
      return handlaggning.yrkande().individYrkandeRoller()
            .stream().<se.fk.rimfrost.framework.sid.model.Idtyp> map(
                  roll -> se.fk.rimfrost.framework.sid.model.ImmutableIdtyp.builder()
                        .typId(roll.individ().typId())
                        .varde(roll.individ().varde())
                        .build())
            .toList();
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

   private static Response.Status toHttpStatus(SidException e) {
      return switch (e.getErrorType()) {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}

package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.EndOulUppgiftException;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

/**
 * Maps {@link EndOulUppgiftException} to HTTP 500 Internal Server Error.
 */
@Provider
public class EndOulUppgiftExceptionMapper implements ExceptionMapper<EndOulUppgiftException>
{
   /** {@inheritDoc} */
   @Override
   public Response toResponse(EndOulUppgiftException exception)
   {
      ErrorResponse errorResponse = new ErrorResponse();
      errorResponse.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      errorResponse.setMessage(exception.getMessage());

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
   }
}

package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.EndOulUppgiftException;

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
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
   }
}

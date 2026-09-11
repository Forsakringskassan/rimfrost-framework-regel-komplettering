package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.CorrelationDataReadException;

/**
 * Maps {@link CorrelationDataReadException} to HTTP 409 Conflict.
 */
@Provider
public class CorrelationDataReadExceptionMapper implements ExceptionMapper<CorrelationDataReadException>
{
   /** {@inheritDoc} */
   @Override
   public Response toResponse(CorrelationDataReadException exception)
   {
      return Response.status(Response.Status.CONFLICT).build();
   }
}

package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.KompletteringIncompleteException;

/**
 * Maps {@link KompletteringIncompleteException} to HTTP 422 Unprocessable Content.
 */
@Provider
public class KompletteringIncompleteExceptionMapper implements ExceptionMapper<KompletteringIncompleteException>
{
   /** {@inheritDoc} */
   @Override
   public Response toResponse(KompletteringIncompleteException exception)
   {
      return Response.status(422).build();
   }
}

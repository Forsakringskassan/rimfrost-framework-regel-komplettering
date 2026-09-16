package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.KompletteringIncompleteException;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

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
      var statusCode = 422;

      ErrorResponse errorResponse = new ErrorResponse();
      errorResponse.setCode(statusCode);
      errorResponse.setMessage("Komplettering is still required for one or more items");

      return Response.status(statusCode).entity(errorResponse).build();
   }
}

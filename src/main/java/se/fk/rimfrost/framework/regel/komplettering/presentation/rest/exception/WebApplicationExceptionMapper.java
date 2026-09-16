package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.regel.oul.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException>
{
   Logger logger = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);

   @Override
   public Response toResponse(final WebApplicationException e)
   {
      logger.warn("Request terminated with WebApplicationException", e);

      int responseStatus = 500;
      String responseMessage = "Internal Server Error";

      var exceptionResponse = e.getResponse();
      if (exceptionResponse != null)
      {
         responseStatus = exceptionResponse.getStatus();
         responseMessage = exceptionResponse.getStatusInfo().getReasonPhrase();
      }

      var errorResponse = new ErrorResponse();
      errorResponse.setCode(responseStatus);
      errorResponse.setMessage(responseMessage);

      return Response.status(responseStatus).entity(errorResponse).build();
   }
}

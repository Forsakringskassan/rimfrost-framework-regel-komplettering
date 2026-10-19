package se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception;

public class AccessDeniedException extends RuntimeException
{
   public AccessDeniedException(String message)
   {
      super(message);
   }
}

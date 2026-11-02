package se.fk.rimfrost.framework.regel.komplettering.logic.dto;

/**
 * The inloggad handläggare's identity in {@link Informationsaccess} (FRKOMP-FR-06.5).
 *
 * @param typId the id of the identifier type
 * @param varde the identifier value
 */
public record Anvandare(String typId,String varde){}

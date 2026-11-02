package se.fk.rimfrost.framework.regel.komplettering.logic.dto;

/**
 * Wraps the rule-specific response from {@code GET /{handlaggningId}} together with the data
 * a BFF needs to publish an informationsaccess event (FRKOMP-FR-06).
 *
 * @param data the rule-specific response returned by {@code readSvarData()}
 * @param informationsaccess the informationsaccess data for the read
 * @param <T> the rule-specific response type
 */
public record RegelKompletteringReadResponse<T>(T data, Informationsaccess informationsaccess)
{
}

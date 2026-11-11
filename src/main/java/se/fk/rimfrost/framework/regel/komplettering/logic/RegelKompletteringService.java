package se.fk.rimfrost.framework.regel.komplettering.logic;

import java.util.List;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Idtyp;

/**
 * Service contract to be implemented by all komplettering regler.
 *
 * <p>The same type {@code T} is used for both the GET response body ({@code readSvarData}) and the
 * PATCH request body ({@code registerSvar}), as required by FRALL-FR-07.7. Regel repos must implement
 * this interface with their own OpenAPI-generated type:
 *
 * <pre>
 * {@code @ApplicationScoped}
 * public class RtfKompletteringService
 *         implements RegelKompletteringService<RtfKompletteringData> { ... }
 * </pre>
 *
 * @param <T> shared data type for {@code GET /{handlaggningId}/komplettering} response and
 *            {@code PATCH /{handlaggningId}/komplettering} request body
 */
@SuppressWarnings("unused")
public interface RegelKompletteringService<T>
{
   /**
    * Returns a boolean value indicating if any attributes are missing from the yrkande, with true indicating that
    * information is missing and false that the yrkande is complete.
    *
    * @param handlaggning the current handlaggning; inspect {@code handlaggning.yrkande()}
    * @return true if komplettering is required, false otherwise
    */
   boolean isKompletteringRequired(Handlaggning handlaggning);

   /**
    * Returns the identities the framework should subscribe to at identitet-prenumeration-tjänsten.
    * A non-empty list activates the identity subscription flow (FRKOMP-FR-02.2).
    * The default implementation returns an empty list, meaning no subscription is created and
    * the regel behaves as before (FRKOMP-FR-02.3).
    *
    * @param handlaggning the current handlaggning
    * @return identities to subscribe to, or an empty list if no subscription is needed
    */
   default List<Idtyp> getPrenumerationsIdentiteter(Handlaggning handlaggning)
   {
      return List.of();
   }

   /**
    * Returns the data the handläggare needs to register the sökande's svar.
    *
    * @param handlaggning the current handlaggning
    * @return response body for the komplettering GET endpoint
    */
   T readSvarData(Handlaggning handlaggning);

   /**
    * Applies the svar to the handlaggning and returns an update ready to be persisted.
    *
    * @param handlaggning the current handlaggning
    * @param request      the handläggare's registered svar
    * @return the handlaggning update to persist
    */
   HandlaggningUpdate registerSvar(Handlaggning handlaggning, T request);
}

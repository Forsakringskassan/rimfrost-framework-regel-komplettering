package se.fk.rimfrost.framework.regel.komplettering;

import io.quarkus.arc.Arc;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.SkipInject;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.komplettering.logic.entity.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.CorrelationDataReadException;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception.AccessDeniedException;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.exception.UnauthorizedException;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.guard.RegelAccessGuard;
import se.fk.rimfrost.framework.regel.komplettering.presentation.rest.identity.UserIdentityHolder;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusComponentTest(
{
      RegelAccessGuard.class, UserIdentityHolder.class
})
class RegelKompletteringAccessGuardTest
{
   private static final String ID_TYP = "PERSONNR";
   private static final String ID_VARDE = "19901010-1234";
   private static final String NOT_ASSIGNED_PATTERN = "(?i).*not assigned to uppgift.*";
   private static final String UNRESOLVED_IDENTITY_PATTERN = "(?i).*could not resolve user identity.*";

   @Inject
   RegelAccessGuard guard;

   @Inject
   UserIdentityHolder userIdentityHolder;

   @InjectMock
   OulUppgiftService oulUppgiftService;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   PermissionsAdapter permissionsAdapter;

   @Test
   @DisplayName("FRKOMP-FR-07.5: UnauthorizedException kastas när ingen handläggaridentitet finns i UserIdentityHolder")
   void verifyAccess_should_throw_unauthorized_when_user_identity_missing()
   {
      var handlaggning = createHandlaggning(List.of());

      var ex = assertThrows(UnauthorizedException.class, () -> guard.verifyAccess(handlaggning));

      assertTrue(ex.getMessage().matches(UNRESOLVED_IDENTITY_PATTERN));
      verify(oulUppgiftService, never()).getCorrelationData(any());
   }

   @Test
   @DisplayName("FRKOMP-FR-07.5: UnauthorizedException kastas när UserIdentityHolder inte är tillgänglig")
   void verifyAccess_should_throw_unauthorized_when_request_context_not_active()
   {
      var handlaggning = createHandlaggning(List.of());
      var requestContext = Arc.container().requestContext();
      requestContext.deactivate();
      try
      {
         var ex = assertThrows(UnauthorizedException.class, () -> guard.verifyAccess(handlaggning));

         assertTrue(ex.getMessage().matches(UNRESOLVED_IDENTITY_PATTERN));
         verify(oulUppgiftService, never()).getCorrelationData(any());
      }
      finally
      {
         requestContext.activate();
      }
   }

   @Test
   @DisplayName("FRKOMP-FR-04.5: CorrelationDataReadException kastas när korrelationsdata saknas")
   void verifyAccess_should_throw_correlation_data_read_exception_when_correlation_data_missing()
   {
      var handlaggning = createHandlaggning(List.of());
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(null);

      assertThrows(CorrelationDataReadException.class, () -> guard.verifyAccess(handlaggning));

      verifyNoInteractions(sidAdapter, permissionsAdapter);
   }

   @Test
   @DisplayName("FRKOMP-FR-07.1/07.2: AccessDeniedException kastas när uppgiften inte är tilldelad någon handläggare")
   void verifyAccess_should_throw_access_denied_when_utforare_is_null()
   {
      var handlaggning = createHandlaggning(List.of());
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var uppgift = mock(Uppgift.class);
      when(uppgift.utforarId()).thenReturn(null);
      givenCorrelationData(uppgift);

      var ex = assertThrows(AccessDeniedException.class, () -> guard.verifyAccess(handlaggning));

      assertTrue(ex.getMessage().matches(NOT_ASSIGNED_PATTERN));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "PERSONNR, 19901010-9999",
         "SAMORDNINGSNR, 19901010-1234"
   })
   @DisplayName("FRKOMP-FR-07.1/07.2/07.4: AccessDeniedException kastas och SID kontrolleras inte när uppgiften är tilldelad en annan handläggare")
   void verifyAccess_should_throw_access_denied_without_sid_check_when_assigned_to_other_handlaggare(
         @SkipInject String utforarTypId, @SkipInject String utforarVarde) throws Exception
   {
      var handlaggning = createHandlaggning(List.of());
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(createTilldeladUppgift(utforarTypId, utforarVarde));

      var ex = assertThrows(AccessDeniedException.class, () -> guard.verifyAccess(handlaggning));

      assertTrue(ex.getMessage().matches(NOT_ASSIGNED_PATTERN));
      verify(sidAdapter, never()).containsSid(any());
   }

   @Test
   @DisplayName("FRKOMP-FR-05.2: Individer i yrkandet skickas till SID-tjänsten")
   void verifyAccess_should_pass_mapped_individer_to_sid_adapter() throws Exception
   {
      var handlaggning = createHandlaggning(List.of(
            createIndividYrkandeRoll("PERSONNR", "19800101-1111"),
            createIndividYrkandeRoll("SAMORDNINGSNR", "19800161-2222")));
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(createTilldeladUppgift(ID_TYP, ID_VARDE));

      guard.verifyAccess(handlaggning);

      verify(sidAdapter).containsSid(List.of(
            sidIdtyp("PERSONNR", "19800101-1111"),
            sidIdtyp("SAMORDNINGSNR", "19800161-2222")));
   }

   private Handlaggning createHandlaggning(List<IndividYrkandeRoll> roller)
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.individYrkandeRoller()).thenReturn(roller);
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private IndividYrkandeRoll createIndividYrkandeRoll(String typId, String varde)
   {
      var individ = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(individ.typId()).thenReturn(typId);
      when(individ.varde()).thenReturn(varde);
      var roll = mock(IndividYrkandeRoll.class);
      when(roll.individ()).thenReturn(individ);
      return roll;
   }

   private Uppgift createTilldeladUppgift(String typId, String varde)
   {
      var utforarId = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(utforarId.typId()).thenReturn(typId);
      when(utforarId.varde()).thenReturn(varde);
      var uppgift = mock(Uppgift.class);
      when(uppgift.utforarId()).thenReturn(utforarId);
      return uppgift;
   }

   private void givenCorrelationData(Uppgift uppgift)
   {
      var correlationData = mock(OulCorrelationData.class);
      when(correlationData.oulUppgiftId()).thenReturn(UUID.randomUUID());
      when(correlationData.uppgift()).thenReturn(uppgift);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }

   private void givenHandlaggareIdentity(String typId, String varde)
   {
      userIdentityHolder.setIdentity(ImmutableIdtyp.builder().typId(typId).varde(varde).build());
   }

   private static se.fk.rimfrost.framework.sid.model.Idtyp sidIdtyp(String typId, String varde)
   {
      return se.fk.rimfrost.framework.sid.model.ImmutableIdtyp.builder().typId(typId).varde(varde).build();
   }
}

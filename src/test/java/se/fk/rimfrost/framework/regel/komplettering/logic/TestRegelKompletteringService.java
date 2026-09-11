package se.fk.rimfrost.framework.regel.komplettering.logic;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;

/** Stub CDI bean satisfying the RegelKompletteringService injection point; replaced by @InjectMock at test time. */
@ApplicationScoped
@DefaultBean
public class TestRegelKompletteringService implements RegelKompletteringService<String>
{
   @Override
   public boolean isKompletteringRequired(Handlaggning handlaggning)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public String readSvarData(Handlaggning handlaggning)
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public HandlaggningUpdate registerSvar(Handlaggning handlaggning, String request)
   {
      throw new UnsupportedOperationException();
   }
}

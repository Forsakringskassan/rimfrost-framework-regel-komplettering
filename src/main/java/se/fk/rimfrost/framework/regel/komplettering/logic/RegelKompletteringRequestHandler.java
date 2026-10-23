package se.fk.rimfrost.framework.regel.komplettering.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.referensdata.ErbjudandeReferensdataInterface;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.integration.kafka.dto.ImmutableRegelResponse;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.CorrelationDataReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.HandlaggningNotFoundException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.HandlaggningReadException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.KompletteringIncompleteException;
import se.fk.rimfrost.framework.regel.komplettering.logic.exception.EndOulUppgiftException;
import se.fk.rimfrost.framework.regel.logic.RegelCancelledException;
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.oul.logic.CloudEventAttributesMapper;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.Erbjudande;
import se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableErbjudande;
import se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableOulUppgiftSpec;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.regel.oul.logic.exception.OulServiceException;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class RegelKompletteringRequestHandler extends RegelRequestHandlerBase
      implements RegelRequestHandlerInterface, RegelKompletteringDoneHandler
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelKompletteringRequestHandler.class);

   private static final String AVSLUTAD = "AVSLUTAD";

   @Inject
   ErbjudandeReferensdataInterface erbjudandeReferensdata;

   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   RegelKompletteringService<?> regelKompletteringService;

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      CloudEventData cloudEvent = null;
      try
      {
         cloudEvent = createCloudEvent(request);
         var handlaggning = getHandlaggning(request.handlaggningId(), cloudEvent);
         var erbjudandeNamn = erbjudandeReferensdata.getErbjudandeNamn(handlaggning.yrkande().erbjudandeId());

         if (!regelKompletteringService.isKompletteringRequired(handlaggning))
         {
            LOGGER.info("Komplettering not required for handlaggningId: {}. Sending early response.", request.handlaggningId());
            sendRegelSuccessResponse(request.handlaggningId(), cloudEvent, Utfall.JA, request.replyTo());
            return;
         }

         LOGGER.info("Komplettering required for handlaggningId: {}. Creating uppgift.", request.handlaggningId());

         var spec = ImmutableOulUppgiftSpec.builder()
               .handlaggningId(request.handlaggningId())
               .handlaggning(handlaggning)
               .replyTo(request.replyTo())
               .cloudEventData(cloudEvent)
               .cloudEventAttributes(CloudEventAttributesMapper.toAttributes(cloudEvent))
               .regel(regelConfig.getSpecifikation().getNamn())
               .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
               .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
               .roll(regelConfig.getSpecifikation().getRoll())
               .url(regelConfig.getUppgift().getPath())
               .erbjudande(buildErbjudande(handlaggning.yrkande().erbjudandeId(), erbjudandeNamn))
               .aktivitetId(request.aktivitetId())
               .uppgiftSpecifikationId(regelConfig.getSpecifikation().getId())
               .uppgiftSpecifikationVersion(regelConfig.getSpecifikation().getVersion())
               .build();

         oulUppgiftService.createOulUppgift(spec);
      }
      catch (Exception e)
      {
         LOGGER.error("Regel run cancelled due to error", e);
         var regelErrorInformation = buildRegelErrorInformation(RegelFelkod.RIMFROST_OTHER,
               "Regel failed due to unexpected internal error. Handlaggning id: " + request.handlaggningId());
         if (e instanceof RegelCancelledException ex)
         {
            regelErrorInformation = ex.getRegelErrorInformation();
         }
         sendErrorResponse(request.handlaggningId(), cloudEvent, regelErrorInformation, request.replyTo());
      }
   }

   @Override
   public void handleKompletteringDone(UUID handlaggningId)
   {
      OulCorrelationData correlation = oulUppgiftService.getCorrelationData(handlaggningId);

      if (correlation == null)
      {
         LOGGER.error("Failed to read correlation data in handleKompletteringDone for handlaggningId: {}", handlaggningId);
         throw new CorrelationDataReadException("Failed to read correlation data for handlaggningId: " + handlaggningId);
      }

      Handlaggning handlaggning;
      try
      {
         handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error in handleKompletteringDone() while trying to read handlaggning with id: {}", handlaggningId, e);

         if (e.getErrorType() == HandlaggningException.ErrorType.NOT_FOUND)
         {
            throw new HandlaggningNotFoundException(e.getMessage(), e);
         }
         else
         {
            throw new HandlaggningReadException(e.getMessage(), e);
         }
      }

      if (regelKompletteringService.isKompletteringRequired(handlaggning))
      {
         throw new KompletteringIncompleteException();
      }

      try
      {
         oulUppgiftService.endOulUppgift(correlation.oulUppgiftId(), "Uppgift klar");
      }
      catch (OulServiceException e)
      {
         if (e.getErrorType() != OulServiceException.ErrorType.NOT_FOUND)
         {
            LOGGER.error("Error in handleKompletteringDone() while trying to end operativ uppgift for handlaggningId: {}",
                  handlaggningId, e);
            throw new EndOulUppgiftException(e.getMessage(), e);
         }
      }

      sendRegelSuccessResponse(handlaggningId, correlation.cloudEventData(), Utfall.JA, correlation.replyTopic());

      oulUppgiftService.cleanupCorrelation(handlaggningId);

      var uppgift = correlation.uppgift();
      var updatedUppgift = ImmutableUppgift.builder()
            .from(uppgift)
            .version(uppgift.version() + 1)
            .uppgiftStatus(AVSLUTAD)
            .utfordTs(OffsetDateTime.now())
            .build();
      var handlaggningUpdate = ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(handlaggning.yrkande())
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(updatedUppgift)
            .build();
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         if (e.getErrorType() == HandlaggningException.ErrorType.CONFLICT)
         {
            LOGGER.error(
                  "Version conflict error in handleKompletteringDone() while updating handlaggning for id: {}. Programming fault? — RegelResponse already sent, ignoring failure",
                  handlaggningId, e);
         }
         else
         {
            LOGGER.error(
                  "Error in handleKompletteringDone() while updating handlaggning for id: {} — RegelResponse already sent, ignoring failure",
                  handlaggningId, e);
         }
      }
      catch (Exception e)
      {
         LOGGER.error(
               "Error in handleKompletteringDone() while updating handlaggning for id: {} — RegelResponse already sent, ignoring failure",
               handlaggningId, e);
      }
   }

   private void sendRegelSuccessResponse(UUID handlaggningId,
         CloudEventData cloudEventData,
         Utfall utfall, String replyTopic)
   {
      try
      {
         var regelResponse = ImmutableRegelResponse.builder()
               .id(cloudEventData.id())
               .handlaggningId(handlaggningId)
               .kogitoparentprociid(cloudEventData.kogitoparentprociid())
               .kogitorootprociid(cloudEventData.kogitorootprociid())
               .kogitoprocid(cloudEventData.kogitoprocid())
               .kogitorootprocid(cloudEventData.kogitorootprocid())
               .kogitoprocinstanceid(cloudEventData.kogitoprocinstanceid())
               .kogitoprocist(cloudEventData.kogitoprocist())
               .kogitoprocversion(cloudEventData.kogitoprocversion())
               .utfall(utfall)
               .type(cloudEventData.type())
               .source(cloudEventData.source())
               .build();
         regelKafkaProducer.sendRegelResponse(regelResponse, Objects.requireNonNull(replyTopic));
      }
      catch (IllegalStateException e)
      {
         LOGGER.error("Failed to send regel response for handlaggning. handlaggningId: {}, utfall: {}",
               handlaggningId, utfall, e);
      }
   }

   private RegelErrorInformation buildRegelErrorInformation(String felkod, String meddelande)
   {
      var info = new RegelErrorInformation();
      info.setFelkod(felkod);
      info.setFelmeddelande(meddelande);
      return info;
   }

   private Erbjudande buildErbjudande(String id, String namn)
   {
      return ImmutableErbjudande.builder()
            .id(id)
            .namn(namn)
            .build();
   }
}

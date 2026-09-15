# Krav — rimfrost-framework-regel-komplettering

Detta ramverk exponerar komplettering som en Kafka-anropbar regel. En
kompletteringsförfrågan tas emot, en fullständighetskontroll utförs, och antingen skickas
svar direkt (om kompletteringen inte behövs) eller så skapas en OUL-uppgift för handläggare
och svar skickas när handläggaren markerat kompletteringen som klar.

Kraven här beskriver endast det som är implementerat i detta ramverk. Ärvda krav från
underliggande ramverk upprepas inte.

## 1. Funktionella krav

### FRKOMP-FR-01 — Mottagning och svar på kompletteringsförfrågan

- **FRKOMP-FR-01.1** Ramverket ska ta emot kompletteringsförfrågningar från en konfigurerad
  Kafka-topic.
- **FRKOMP-FR-01.2** Svar ska skickas till den topic som angavs i förfrågans `replyTo`-fält.
- **FRKOMP-FR-01.3** Vid utfallet "komplettering behövs inte" ska svaret ha `utfall = JA`.
- **FRKOMP-FR-01.4** Vid utfallet "komplettering utförd av handläggare" ska svaret ha
  `utfall = JA`. Anropande flöde ska inte behöva särskilja dessa två fall.
- **FRKOMP-FR-01.5** Vid fel ska svaret ha `utfall = ERROR` och innehålla `RegelErrorInformation`
  med felkod och felmeddelande.

### FRKOMP-FR-02 — Kompletteringskontroll

- **FRKOMP-FR-02.1** Ramverket ska tillhandahålla ett interface (`RegelKompletteringService`)
  med en metod `isKompletteringRequired()` som varje regelimplementation måste implementera.
  Metoden returnerar ett boolean värde, där true indikerar att komplettering behövs och false 
  att yrkandet är komplett.

### FRKOMP-FR-03 — Handler-flöde

- **FRKOMP-FR-03.1** Vid mottagen förfrågan ska ramverket anropa `isKompletteringRequired()` på
  regelimplementationens service-bean.
- **FRKOMP-FR-03.2** Om `isKompletteringRequired()` returnerar false ska ramverket skicka
  svar med `utfall = JA` direkt på `replyTo` utan att skapa någon OUL-uppgift.
- **FRKOMP-FR-03.3** Om `isKompletteringRequired()` returnerar true ska ramverket
  initiera en kompletteringsuppgift via `OulUppgiftService.createOulUppgift()` och vänta —
  inget Kafka-svar skickas i detta steg.
- **FRKOMP-FR-03.4** Om `OulUppgiftService.createOulUppgift()` kastar `OulServiceException` ska ramverket skicka ett felsvar med
  felkod `RIMFROST_OTHER`.
- **FRKOMP-FR-03.5** Om exception kastas vid läsning av handläggning ska ramverket skicka ett felsvar med
    relevant felkod.
- **FRKOMP-FR-03.6** Om exception kastas vid anrop till `isKompletteringRequired()` ska ramverket skicka ett felsvar med
  felkod `RIMFROST_OTHER`.

### FRKOMP-FR-04 — Kompletteringsflöde via REST

- **FRKOMP-FR-04.1** Ramverket ska exponera `GET /{handlaggningId}` som returnerar
  den information handläggaren behöver för att registrera kompletterande uppgifter. Informationen
  hämtas via regelns implementation av `RegelKompletteringService`.
- **FRKOMP-FR-04.2** Ramverket ska exponera `PATCH /{handlaggningId}` för
  registrering av kompletterande uppgifter via regelns implementation av
  `RegelKompletteringService`.
- **FRKOMP-FR-04.3** Ramverket ska exponera `POST /{handlaggningId}/done`. Vid anrop
  ska `isKompletteringRequired()` anropas för att verifiera att yrkandet nu är komplett. Om yrkandet
  fortfarande saknar uppgifter ska HTTP 422 returneras.
- **FRKOMP-FR-04.4** Om `isKompletteringRequired()` returnerar false ska ramverket skicka svar
  med `utfall = JA` på den `replyTo` topic som lagrats för den pågående kompletteringsomgången.
- **FRKOMP-FR-04.5** `POST /done` ska returnera HTTP 409 om korrelationstillståndet inte kan hittas, t.ex. för att timeout redan har
  tömt korrelationstillståndet.
- **FRKOMP-FR-04.6** `RegelKompletteringService` ska använda en enda typparameter för
  svarsdata: samma datastruktur används både som returvärde för `readSvarData` (GET) och som
  request body för `registerSvar` (PATCH).
- **FRKOMP-FR-04.7** `POST /done` ska returnera HTTP 404 om handläggning inte kan hittas.
- **FRKOMP-FR-04.8** 404 fel vid REST-anrop för att avsluta OUL uppgift behandlas som lyckat anrop.

### FRKOMP-FR-05 — Kontroll av skyddad identitet (SID)

- **FRKOMP-FR-05.1** Innan `readSvarData()` anropas vid `GET /{handlaggningId}` ska ramverket kontrollera
  om någon av handläggningsärendets individer har skyddad identitet via SID-tjänsten.
- **FRKOMP-FR-05.2** Individerna hämtas från `handlaggning.yrkande().individYrkandeRoller()` och
  skickas i en `POST /sid/status`-förfrågan till SID-tjänsten.
- **FRKOMP-FR-05.3** Om en eller flera individer har skyddad identitet ska ramverket returnera
  HTTP 403 och `readSvarData()` ska inte anropas.
- **FRKOMP-FR-05.4** Fel från SID-tjänsten ska resultera i väldefinierade HTTP-statuskoder på samma
  sätt som fel mot handläggningstjänsten: 404, 400, 503 respektive 500.
- **FRKOMP-FR-05.5** SID-kontrollen ska ingå i ramverket och gälla automatiskt för alla
  regelimplementationer utan kodändringar. Varje regelimplementation måste konfigurera `sid.api.base-url`
  med adressen till SID-tjänsten.
- **FRKOMP-FR-05.6** Om en eller flera individer har skyddad identitet ska ramverket ta bort tilldelningen av OUL-uppgiften
  innan HTTP 403 returneras, via `tryUnassignOulUppgift` som tillhandahålls av
  `rimfrost-framework-regel-oul`, så att uppgiften återgår till otilldelat läge och kan tilldelas
  handläggare med SID-rättigheter.
- **FRKOMP-FR-05.7** Unassign ska alltid försökas oavsett om uppgiften är tilldelad eller inte —
  OUL-tjänsten förväntas hantera anropet korrekt i båda fallen. Om inget uppgifts-ID finns lagrat
  för handläggningsärendet (t.ex. vid en oväntad timingrelaterad situation) ska unassign-försöket
  hoppas över utan fel — HTTP 403 ska ändå returneras.
- **FRKOMP-FR-05.8** Fel vid unassign av OUL-uppgiften ska loggas men ska inte påverka det
  returnerade HTTP 403-svaret. `tryUnassignOulUppgift` hanterar loggning och sväljer felet internt.

---

## 2. Icke-funktionella krav

### FRKOMP-NFR-01 — Tillförlitlighet

- **FRKOMP-NFR-01.1** Ramverket ska garantera att ett kompletteringssvar alltid skickas för
  varje mottagen kompletteringsförfrågan — antingen med `utfall = JA` eller med felinformation.
- **FRKOMP-NFR-01.2** Om komplettering initieras ska svaret skickas när kompletteringen är
  avslutad eller när timeout löpt ut — inte direkt vid mottagandet av förfrågan.

### FRKOMP-NFR-02 — Observerbarhet

- **FRKOMP-NFR-02.1** Ramverket ska logga vilken av de två vägarna ("komplettering behövs
  inte" respektive "komplettering utförd av handläggare") som lett fram till ett givet svar,
  för att stödja felsökning och uppföljning.

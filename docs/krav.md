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

- **FRKOMP-FR-02.1** Ramverket ska tillhandahålla ett interface (`KompletteringKontrollInterface`)
  med en defaultmetod `checkKomplettering()` som returnerar en tom lista, vilket innebär att
  yrkandet är komplett. Regelimplementationer som kräver en fullständighetskontroll ska
  överskriva metoden.
- **FRKOMP-FR-02.2** Ramverket ska tillhandahålla en DTO (`KompletteringUnderlag`) som beskriver
  ett saknat attribut i yrkandet med ett maskinläsbart typidentifierare (`underlagTyp`) och en
  läsbar beskrivning (`beskrivning`). Typidentifieraren ska definieras som en lokal konstant
  i respektive regelrepo — aldrig i detta ramverk.

### FRKOMP-FR-03 — Handler-flöde

- **FRKOMP-FR-03.1** Vid mottagen förfrågan ska ramverket anropa `checkKomplettering()` på
  regelimplementationens service-bean.
- **FRKOMP-FR-03.2** Om `checkKomplettering()` returnerar en tom lista ska ramverket skicka
  svar med `utfall = JA` direkt på `replyTo` utan att skapa någon OUL-uppgift.
- **FRKOMP-FR-03.3** Om `checkKomplettering()` returnerar en icke-tom lista ska ramverket
  initiera en kompletteringsuppgift via `KompletteringOulHandler.initiate()` och vänta —
  inget Kafka-svar skickas i detta steg.
- **FRKOMP-FR-03.4** Om `initiate()` kastar `OulException` ska ramverket skicka ett felsvar med
  felkod `RIMFROST_OTHER`.

### FRKOMP-FR-04 — Kompletteringsflöde via REST

- **FRKOMP-FR-04.1** Ramverket ska exponera `GET /{handlaggningId}/komplettering` som returnerar
  den information handläggaren behöver för att registrera kompletterande uppgifter. Informationen
  hämtas via regelns implementation av `KompletteringSvarServiceInterface`.
- **FRKOMP-FR-04.2** Ramverket ska exponera `PATCH /{handlaggningId}/komplettering` för
  registrering av kompletterande uppgifter via regelns implementation av
  `KompletteringSvarServiceInterface`.
- **FRKOMP-FR-04.3** Ramverket ska exponera `POST /{handlaggningId}/komplettering/done`. Vid anrop
  ska `checkKomplettering()` anropas för att verifiera att yrkandet nu är komplett. Om yrkandet
  fortfarande saknar uppgifter ska HTTP 422 returneras.
- **FRKOMP-FR-04.4** Om `checkKomplettering()` returnerar tom lista ska ramverket skicka svar
  med `utfall = JA` på den `replyTo` som lagrats för den pågående kompletteringsomgången.
- **FRKOMP-FR-04.5** `POST /komplettering/done` ska returnera HTTP 409 om timeout redan har
  tömt korrelationstillståndet.
- **FRKOMP-FR-04.6** Om avslutning av OUL-uppgiften misslyckas under `POST /komplettering/done`
  ska felet loggas, svaret ändå skickas på `replyTo` och HTTP 207 returneras för att signalera
  att kompletteringen är accepterad men att OUL-uppgiften eventuellt fortfarande är öppen.
- **FRKOMP-FR-04.7** `KompletteringSvarServiceInterface` ska använda en enda typparameter för
  svarsdata: samma datastruktur används både som returvärde för `readSvarData` (GET) och som
  request body för `registerSvar` (PATCH).

### FRKOMP-FR-05 — Persistens av korrelationstillstånd

- **FRKOMP-FR-05.1** Ramverket ska tillhandahålla ett persistenslager (`KompletteringStorage`)
  för att lagra och hämta korrelationstillstånd per `handlaggningId` under en pågående
  kompletteringsomgång. Tillståndet ska innehålla åtminstone OUL-uppgifts-ID och den `replyTo`
  som svaret ska skickas till när kompletteringen avslutas.
- **FRKOMP-FR-05.2** `KompletteringStorage` ska returnera ett tomt värde (`Optional.empty()`)
  vid sökning på ett okänt `handlaggningId` utan att kasta exception.
- **FRKOMP-FR-05.3** `KompletteringStorage` ska hantera borttagning av ett frånvarande
  `handlaggningId` utan att kasta exception.

### FRKOMP-FR-06 — Timeout-hantering

- **FRKOMP-FR-06.1** Ramverket ska tillhandahålla en operation (`handleKompletteringTimeout`)
  som avslutar den öppna OUL-uppgiften, tar bort korrelationstillståndet och skickar ett
  Kafka-svar med `utfall = ERROR` och `RegelErrorInformation` på den lagrade `replyTo` när
  kompletteringstimern löper ut.
- **FRKOMP-FR-06.2** `handleKompletteringTimeout` ska vara säker att anropa även om
  handläggaren redan avslutat kompletteringen (dvs. tillståndet redan är borttaget) — operationen
  ska logga och returnera utan exception.
- **FRKOMP-FR-06.3** Om avslutning av OUL-uppgiften misslyckas under timeout-hanteringen ska
  felet loggas och korrelationstillståndet ändå tas bort, utan att kasta exception.

### FRKOMP-FR-07 — Skapande av komplettering-OUL

- **FRKOMP-FR-07.1** Om lagring av korrelationstillståndet misslyckas efter att OUL-uppgiften
  har skapats i `KompletteringOulHandler.initiate`, ska ramverket best-effort avsluta den
  nyskapade OUL-uppgiften via `endOperativUppgift`. Det ursprungliga persistensfelet ska alltid
  kastas vidare till anroparen.
- **FRKOMP-FR-07.2** Om avslutningen av OUL-uppgiften enligt FRKOMP-FR-07.1 också misslyckas
  ska felet loggas med `uppgiftId` och `handlaggningId` för manuell rekonsiliering, utan att
  maskera det ursprungliga persistensfelet.

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

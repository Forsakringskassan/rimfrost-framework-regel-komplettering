# Plan: FKPOC-1267 — Hantera uppdatering av identitetsdata (identitetsprenumeration)

## Background

Ramverket ska kunna prenumerera på identitetsuppdateringar hos identitet-prenumeration-tjänsten
så att kompletteringen kan avslutas automatiskt när identitetsdata förändras, utan att en
handläggare behöver agera. Prenumerationen aktiveras endast om regelns implementation av
`getPrenumerationsIdentiteter()` returnerar en icke-tom lista. Flödet täcks av
FRKOMP-FR-02.2, FR-02.3, FR-03.7, FR-03.8, FR-10, FR-11 och FRKOMP-NFR-01.3.

---

## Approach

Implementeras lager för lager: interface-ändring → prenumerationsregistrering i request-flödet
→ avregistrering i done-flödet → Kafka-konsument för `IdentitetUppdateradEvent`.
Adaptern `rimfrost-framework-identitet-prenumeration-adapter` (1.0.0-SNAPSHOT) är redan
tillagd som beroende. Varje steg testas med JUnit 5 / `@QuarkusTest` innan nästa påbörjas.

### Adapter-API

| Metod | Beskrivning |
|---|---|
| `createPrenumeration(CreatePrenumerationRequest)` | Registrerar prenumeration |
| `deletePrenumeration(UUID prenumerationId)` | Avregistrerar prenumeration; kastar `IdentitetPrenumerationException` med `NOT_FOUND` vid 404 |

Config property: `identitetprenumeration.api.base-url` (inget bindestreck mellan "identitet" och "prenumeration").

---

## Steps

### Step 1 ✅ — Lägg till `getPrenumerationsIdentiteter()` i `RegelKompletteringService`

**Krav:** FRKOMP-FR-02.2, FRKOMP-FR-02.3

Fil: `logic/RegelKompletteringService.java`

```java
/**
 * Returns the identities the framework should subscribe to at identitet-prenumeration-tjänsten.
 * A non-empty list activates the identity subscription flow (FRKOMP-FR-02.2).
 */
default List<String> getPrenumerationsIdentiteter(Handlaggning handlaggning) {
    return List.of();
}
```

- Default implementation returns empty list → no subscription, beteende oförändrat (FR-02.3).
- Befintliga regelimplementationer behöver inte ändras.

---

### Step 2 ✅ — Registrera prenumeration i request-flödet

**Krav:** FRKOMP-FR-03.7, FRKOMP-FR-03.8

Fil: `logic/RegelKompletteringRequestHandler.java` — metod `handleRegelRequest()`

Efter att `isKompletteringRequired()` returnerat `true`:
1. Anropa `kompletteringService.getPrenumerationsIdentiteter(handlaggning)`.
2. Om listan är icke-tom: anropa `IdentitetPrenumerationAdapter.createPrenumeration(...)` med `prenumerationId = handlaggningId`, identiteterna och det konfigurerade `replyTo`-topic.
3. Om registreringen misslyckas: skicka felsvar med `RIMFROST_OTHER`, skapa **inte** OUL-uppgift (FR-03.8).
4. Om listan är tom: fortsätt som tidigare direkt till OUL-skapandet.

Prenumerationen ska registreras **innan** `oulUppgiftService.createOulUppgift()` anropas (FR-03.7).

Config:
```properties
identitetprenumeration.api.base-url=        # FR-10.4
identitetprenumeration.reply-to-topic=      # FR-10.5
```

---

### Step 3 ✅ — Avregistrera prenumeration vid done (REST-flödet)

**Krav:** FRKOMP-FR-10.1, FRKOMP-FR-10.2, FRKOMP-FR-10.3, FRKOMP-NFR-01.3

Fil: `logic/RegelKompletteringRequestHandler.java` — metod `handleKompletteringDone()`

Efter att Kafka-svar skickats och OUL-uppgiften avslutats:
- Anropa `IdentitetPrenumerationAdapter.deletePrenumeration(handlaggningId)`.
- `NOT_FOUND` (404) behandlas som success (FR-10.2). Övriga fel loggas men avbryter inte flödet (FR-10.3).

---

### Step 4 ✅ — Kafka-konsument för `IdentitetUppdateradEvent`

**Krav:** FRKOMP-FR-11.1 – FR-11.6, FRKOMP-FR-10.1

Nya filer (faktisk placering — avviker från ursprunglig plan p.g.a. SOC):
- `logic/IdentitetUppdateradEventHandler.java` — logiklager, tar `UUID handlaggningId`
- `presentation/kafka/IdentitetUppdateradEventConsumer.java` — Kafka-konsument, extraherar UUID vid presentationsgränsen
- `presentation/kafka/IdentitetUppdateradEventDeserializer.java` — deserializer för `IdentitetUppdateradEvent`

Flöde i `handleIdentitetUppdaterad(UUID handlaggningId)`:
1. Hämta handläggning via `HandlaggningAdapter`.
2. Anropa `regelKompletteringService.isKompletteringRequired(handlaggning)`.
3. Om `false`: delegera till `RegelKompletteringDoneHandler.handleKompletteringDone()` (FR-11.3).
4. Om `true`: ignorera eventet (FR-11.4).
5. Fel: loggas, inget felsvar skickas (FR-11.5).

Design-beslut: konsumenten (`IdentitetUppdateradEventConsumer`) extraherar `prenumerationId` som `UUID` och anropar logiklagret — messaging-typen (`IdentitetUppdateradEvent`) exponeras aldrig utanför presentationslagret.

Config:
```properties
mp.messaging.incoming.identitet-uppdaterad.topic=${identitetprenumeration.reply-to-topic}
```

---

## Execution order

1 → 2 → 3 → 4

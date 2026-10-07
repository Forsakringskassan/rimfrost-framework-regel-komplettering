# Plan — FKPOC-1112: SID-kontroll med behörighetskontroll

## Syfte

Utöka SID-kontrollen i `RegelKompletteringController` så att en handläggare med SID-rättigheter
får tillgång till SID-skyddade ärenden. Tidigare returnerades alltid HTTP 403 vid SID-träff;
nu ska ramverket även kontrollera om handläggaren har SID-behörighet via `PermissionsAdapter`
(med identitet hämtad via `IdentityAdapter`). 

## Kravkoppling

- FRKOMP-FR-04.10–13: HTTP-statuskoder för fel mot externa tjänster (SID, behörighet, identity)
- FRKOMP-FR-05.3: 403 bara om SID detekteras **och** handläggaren saknar SID-rättigheter
- FRKOMP-FR-05.9: `PermissionsAdapter.hasSidPermission()` + `IdentityAdapter.getIdentity()`
- FRKOMP-FR-05.10: `readSvarData()` anropas normalt om handläggaren har SID-rättigheter
- FRKOMP-FR-05.11–13: Konfigproperties för sid, permissions och identity base-url

## Steg

- [x] **1. Lägg till `rimfrost-adapter-permissions` i pom.xml**

  Lägg till beroendet `se.fk.rimfrost.adapter.permissions:rimfrost-adapter-permissions:0.0.1-SNAPSHOT`
  i `pom.xml`, på samma sätt som i `rimfrost-framework-regel-manuell`.

- [x] **2. Lägg till `rimfrost-adapter-identity`**

  Lägg till `rimfrost-adapter-identity` i `pom.xml` och konfigurera
  `quarkus.rest-client.identity-api.url` i `application.properties`.

- [x] **3. Uppdatera `RegelKompletteringController`**

  Ändra SID-logiken i `getKomplettering()` och lägg till hjälpmetoder:

  - Injicera `PermissionsAdapter` och `IdentityAdapter`
  - Ändra villkoret: returnera 403 bara om `checkSid() && !checkHandlaggareHasSidPermission()`
  - Lägg till `checkHandlaggareHasSidPermission()` — anropar `resolveHandlaggareIdentity()` och
    `permissionsAdapter.hasSidPermission(identity.typId(), identity.varde())`
  - Lägg till `resolveHandlaggareIdentity()` — hanterar `UNAUTHORIZED` som null/warn,
    övriga `IdentityException` som fel
  - Lägg till `toHttpStatus(PermissionsException)` och `toHttpStatus(IdentityException)` —
    samma mappning NOT_FOUND/BAD_REQUEST/SERVICE_UNAVAILABLE/default→500

- [x] **4. Lägg till test — `RegelKompletteringControllerSidCheckTest`**

  Ny testklass (separera från befintlig `RegelKompletteringControllerTest`) med tester för:
  - FRKOMP-FR-05.3: GET returnerar 403 när SID detekteras och handläggaren saknar rättigheter
  - FRKOMP-FR-05.6: OUL-uppgiften unassignas innan 403 returneras
  - FRKOMP-FR-05.9/05.10: `readSvarData()` anropas normalt när handläggaren har SID-rättigheter
  - FRKOMP-FR-05.9: `hasSidPermission` anropas med handläggarens identitet från `IdentityAdapter`
  - FRKOMP-FR-04.10–13: Fel från `PermissionsAdapter` mappas till rätt HTTP-statuskoder
  - FRKOMP-FR-04.10–13: Fel från `SidAdapter` mappas till rätt HTTP-statuskoder (flytta från
    befintlig testklass om de redan finns)
  - FRKOMP-FR-05.7: Unassign hoppas över om inget uppgifts-ID finns
  - FRKOMP-FR-05.8: Fel vid unassign loggas men påverkar inte 403-svaret

- [x] **5. Konfigurera test-properties**

  Lägg till i `src/test/resources/application.properties`:
  ```
  permissions.api.base-url=http://localhost:${quarkus.http.test-port}
  quarkus.rest-client.identity-api.url=http://localhost:${quarkus.http.test-port}
  ```
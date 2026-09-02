# rimfrost-framework-regel-komplettering

Ramverkskomponent som exponerar komplettering som en Kafka-anropbar regel.
En kompletteringsförfrågan tas emot, en fullständighetskontroll utförs, och antingen skickas
svar direkt (om kompletteringen inte behövs) eller så skapas en OUL-uppgift för handläggare
och svar skickas när handläggaren markerat kompletteringen som klar. Båda framgångsvägarna
resulterar i `utfall = JA` — anropande flöde behöver inte särskilja dem.

Baseras på [rimfrost-framework-regel](https://github.com/Forsakringskassan/rimfrost-framework-regel)
och [rimfrost-framework-regel-oul](https://github.com/Forsakringskassan/rimfrost-framework-regel-oul).

> Ramverket är under initial extraktion från `rimfrost-framework-regel` och
> `rimfrost-framework-regel-manuell`. Se [`docs/krav.md`](docs/krav.md) för fullständig
> kravdefinition.

## Aktörer

| Aktör                             | Roll                                                                                     |
|-----------------------------------|------------------------------------------------------------------------------------------|
| Kundbehovsflödet                  | Initierar kompletteringsförfrågan via Kafka och tar emot svar                            |
| Regelimplementationer             | Överskriver `checkKomplettering()` och implementerar `KompletteringSvarServiceInterface` |
| Handläggarportalen                | Anropar REST-gränssnittet för att läsa, uppdatera och markera komplettering som klar     |
| OUL (Operativt Uppgiftslager)     | Tar emot kompletteringsuppgiften och publicerar statusnotifieringar                      |

## Ansvarsområden

- **Kafka request/response** — konsumerar kompletteringsförfrågningar och skickar svar
  dynamiskt till förfrågans `replyTo`-topic.
- **Kompletteringskontroll** — tillhandahåller `KompletteringKontrollInterface` (default
  returnerar tom lista = komplett) och `KompletteringUnderlag`-DTO för att beskriva saknade
  attribut.
- **REST-flöde för komplettering** — exponerar `GET/PATCH/POST /{handlaggningId}/komplettering`
  för handläggarportalen.
- **Korrelationslagring** — persisterar OUL-uppgifts-ID och `replyTo` per pågående
  kompletteringsomgång via `KompletteringStorage`.
- **Timeout-hantering** — säkerställer att svar alltid skickas, även om timern löper ut
  innan handläggaren är klar.

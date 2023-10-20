# k9-punsjbolle


## Behov som løses på rapid

### KopierPunsjbarJournalpost
* Henter aktørId på alle involverte parter (med behov `HentPersonopplysninger` i `k9-personopplysninger`)
* Restkall mot `K9-Sak` for å hente eller opprette Saksnummer på begge parter.
* Kopierer journalposten (med behov `KopierJournalpost` i `omsorgspenger-journalføring`)
* Sender en melding på kafka-topic  `k9saksbehandling.punsjbar-journalpost` med informasjon om den nye journalposten som `K9-Punsj` lytter på, slik at det blir en ny oppgave i `K9-Los`

#### Behov
```json 
{
  "versjon": "1.0.0",
  "fra": "<required>", // identitetsnummer
  "til": "<required>", // identitetsnummer
  "journalpostId": "<required>",
  "pleietrengende": "<optional>", // identitetsnummer
  "annenPart": "<optional>", // identitetsnummer,
  "søknadstype": "<required>" // en av <PleiepengerSyktBarn|OmsorgspengerUtbetaling|OmsorgspengerKroniskSyktBarn|OmsorgspengerMidlertidigAlene>
}
```

#### Løsning
```json 
{
  "journalpostId": "123123123",
  "løst":"2021-09-21T11:34:39.801Z"
}
```

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #sykdom-i-familien.


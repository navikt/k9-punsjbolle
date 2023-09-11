# k9-punsjbolle


## Behov som løses på rapid
### PunsjetSøknad
* Henter aktørId på alle involverte parter (med behov `HentPersonopplysninger` i `k9-personopplysninger`)
* Restkall mot `K9-Sak` & `K9-infotrygd` for å avgjøre om det skal rutes til K9Sak eller Infotrygd.
* Restkall mot `K9-Sak` for å hente eller opprette Saksnummer.
* Ferdigstiller alle journalposter i søknaden (med behov `FerdigstillJournalføring` i `omsorgspenger-journalføring`)
* Oppretter en oppsummerings-PDF basert på søknadens innhold (med behov `JournalførJson` i `omsorgspenger-journalføring`)
* Restkall mot `K9-Sak` for å sende inn søknaden.
#### Behov
```json
{
  "versjon": "1.0.0",
  "saksnummer": "<optional>",
  "saksbehandler": "<required>",
  "søknad": {
    // <Søknaden slik de serialiseres til JSON fra k9-format>
  }
} 
```
#### Løsning
```json
{
  "løst":"2021-09-21T11:34:39.801Z"
}
```

### KopierPunsjbarJournalpost
* Henter aktørId på alle involverte parter (med behov `HentPersonopplysninger` i `k9-personopplysninger`)
* Restkall mot `K9-Sak` & `K9-infotrygd` for å avgjøre om det skal rutes til K9Sak eller Infotrygd.
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

## API

### POST @ /api/ruting & POST @ /api/saksnummer
#### Request
```json
{
  "søker": { // required
    "identitetsnummer": "<required>",
    "aktørId": "<required>"
  },
  "pleietrengende": { // optional
    "identitetsnummer": "<required>",
    "aktørId": "<required>"
  },
  "annenPart": { // optional
    "identitetsnummer": "<required>",
    "aktørId": "<required>"
  },
  "søknadstype": "<required>", //  en av <PleiepengerSyktBarn|OmsorgspengerUtbetaling|OmsorgspengerKroniskSyktBarn|OmsorgspengerMidlertidigAlene>
  "journalpostId": "<optional>", // enten denne eller periode må settes.
  "periode": "<optional>" // eksempel 2021-01-01/2021-01-02
}
```

#### Response /api/routing
##### HTTP 200
```json
{
  "destinasjon": "K9Sak" // en av <K9Sak|Infotrygd>
}
```
##### HTTP 409
```json
{
    "status": 409,
    "type": "punsjbolle://ikke-støttet-journalpost",
    "details": "Beskrivelse på hvorfor den ikke er støttet"
}
```

#### Response /api/saksnummer
##### HTTP 200

```json
{
  "saksnummer": "SAK123"
}
```

##### HTTP 409
```json
{
    "status": 409,
    "type": "punsjbolle://må-behandles-i-infotrygd",
    "details": "Beskrivelse på hvorfor det må behandles i Infotrygd"
}
```

### POST @ /api/saksnummer-fra-soknad
#### Request
```json
{
  "søker": { // required
    "identitetsnummer": "<required>",
    "aktørId": "<required>"
  },
  "pleietrengende": { // optional
    "identitetsnummer": "<required>",
    "aktørId": "<required>"
  },
  "annenPart": { // optional
    "identitetsnummer": "<required>",
    "aktørId": "<required>"
  },
  "søknad": {
    // <Søknaden slik de serialiseres til JSON fra k9-format>
  }
}
```

#### Response
##### HTTP 200
```json
{
  "saksnummer": "SAK123"
}
```

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #sykdom-i-familien.


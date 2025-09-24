# Web-based DWS FO/TO

## Overzicht

### Inleiding

Dit is een high-level functioneel en technisch ontwerp voor een nieuw dictionary writing system.

Het beschrijft een web-based systeem dat vergelijkbare functionaliteit biedt als INL-DWS en Lexonomy, zonder de belangrijkste nadelen van die systemen, en met de mogelijkheid om "tabelgebaseerd" te bewerken.


### Is het nodig?

De DWS'en die we op dit moment gebruiken (INL-DWS en Lexonomy) hebben nadelen. Een van de belangrijkste wensen is "tabelgebaseerde" bewerking. Een eigenschap in veel artikelen bewerken wordt nu vaak gedaan door een Excel-lijst te exporteren en de bewerkte lijst weer te importeren. Als dit niet meer nodig is, spaart dit zowel taalkundigen als ontwikkelaars tijd.

Er wordt wel eens gedacht over de mogelijkheid om woordenboeken zoals ANW om te zetten naar een relationele database. Met hun huidige rijke, geneste structuur wordt dit erg lastig. Dit zou namelijk een database met vele tientallen gekoppelde tabellen opleveren die niet efficient te bevragen en bewerken is.

De keuze is dus: vereenvoudigen we de structuur zodat het project beter in een relationele database past, of accepteren we dat deze structuur een ander soort database en bewerkingstools vereisen?

Als we voor het tweede kiezen, hebben we dus een XML-bewerkingssysteem nodig waarin ook tabelgebaseerde bewerking mogelijk is.


### Is het realiseerbaar?

Het hier beschreven systeem bouwt op jarenlange ervaring en maakt gebruik van veel krachtige, populaire componenten die we eerder gebruikt hebben. Toch zal het maanden werk zijn om dit te realiseren.

Waarschijnlijk is het nodig om met een of meerdere partners een projectaanvraag voor subsidie in te dienen. Zo'n projectaanvraag maakt waarschijnlijk meer kans als er al een prototype is dat de haalbaarheid aantoont en de voordelen boven andere systemen inzichtelijk maakt.

Een mogelijke partner is de Fryske Akademy. Zij gebruiken nu al ons INL-DWS, en Eduard Drenth (die dat project voor hen onderhoudt) pleit er al jaren voor om samen een beter alternatief te bouwen.


## Functionele eisen

Core features:

- web-based
- XML schema bepaalt compleet default user interface; verdere customization is mogelijk
- flexibele mogelijkheden om zowel *per woord* (entry-gebaseerd) als *per eigenschap(pen)* (table-gebaseerd) te werken
- goede support voor kruisverwijzingen binnen het project en tussen projecten
- flexibel omgaan met de unieke "ingang" van een entry. Lemma, lemma+homonymnummer, lemma+pos, etc.
- meerdere entry-types (bijv. woord/idioom/formule in Woordcombinaties)
- lemmalijst met configureerbare kolommen
- meerdere gebruikers kunnen het systeem veilig tegelijkertijd gebruiken
- op 1 server kunnen meerdere projecten bijgehouden en bewerkt worden
- configureerbare metadata wordt bijgehouden, zoals de status per onderdeel en wie wanneer waaraan gewerkt heeft
- extensible via plugins, bijvoorbeeld voor custom widgets
- volwaardige editor met undo/redo, autosave, etc.
- backups van elk entry worden bijgehouden

Would be nice:

- goede support voor mixed content (bijv. voor eenvoudige opmaak zoals vet, cursief, super/subscript, etc.)
- styling kan worden geconfigureerd, zodat de editor min of meer WYSIWYG is
- gedetailleerde per-user rechten, bijv. read-only, of alleen (schrijf)toegang op bepaalde table-based views


### Web-based

Het systeem heeft een web-based user interface, zodat het ook door externen gebruikt kan worden.

Het user interface is toegespitst op desktopgebruik. Gebruik via kleinere (touch)screens is mogelijk maar niet het primaire doel.

(vgl INL-DWS dat eigenlijk alleen intern of via telewerkserver kan worden uitgevoerd)

### User interface volgt uit het schema

Het systeem is gebaseerd op XML-Schema. In elk geval zal het TEI Lex-0 schema ondersteund worden, en de huidige schema's' van onze projecten. Als de gebruiker een schema probeert te gebruiken met niet-ondersteunde features, verschijnt een duidelijke foutmelding.

Op basis van alleen het schema wordt een compleet user interface voor bewerking gegenereerd, inclusief het toevoegen/verwijderen van elementen, het verplaatsen van elementen, cut/copy/paste, validatiefunctie, etc.

De keuze voor bepaalde widgets die niet direct uit het schema volgen, of andere customizations, kunnen wanneer nodig extra worden toegevoegd. Dit gebeurt via plugins (zie Extensibility).

(vgl Lexonomy waar vrij veel code nodig is om een bruikbare editor te maken)


### Table-based optie

Naast een user interface voor een hele entry (bijv. alle informatie over het woord *waterval*) is het mogelijk om een table-based view te definieren.

Als een gebruiker bijvoorbeeld de definities van een groep gerelateerde entries in een tabel wil kunnen bewerken (dus zonder elk entry apart te hoeven openen), is dat een kwestie van een view configureren van wat er in de tabel weergegeven zou moeten worden.

Meestal zal in een tabel-cel 1 tekst- of selectieveld komen, maar in principe moet elk stukje van een entry (een deel van het hele document) in een cel kunnen worden weergegeven en bewerkt.

De table-based optie is ook zeer geschikt voor zoeken en het maken van allerlei overzichten, bijv. alle semagrammen met hun categorieen. Table-overzichten kunnen opgeslagen worden als TSV.

Het aanmaken van een nieuwe table-based view zou in het user interface te doen moeten zijn.


### Bruikbaar door meerdere gebruikers

Meerdere gebruikers kunnen het systeem tegelijkertijd gebruiken. Als een gebruiker een entry opent, wordt het gelockt, zodat anderen niet hetzelfde entry kunnen bewerken.

Meerdere gebruikers kunnen dezelfde gegevens in een table view open hebben. Als een van de twee een wijziging maakt, en daarna probeert de ander dezelfde gegevens te wijzigen, wordt dit conflict gedetecteerd en een foutmelding getoond. De kans hierop is in de praktijk uiteraard klein.

Table views synchroniseren *niet* automatisch, dus als een andere gebruiker de gegevens wijzigt die in jouw table view getoond worden, zie je ze niet automatisch mee veranderen. Wel kun je de table view handmatig verversen om de nieuwste gegevens te zien.


### Kruisverwijzingen

Kruisverwijzingen binnen het project en tussen projecten zijn mogelijk. Omdat elk project een andere manier van verwijzen kan gebruiken, wordt dit waarschijnlijk via plugins geimplementeerd (zie Extensibility).

Er wordt voorzien in de mogelijkheid om informatie over het doel van de verwijzing op te halen (bijv. korte definitie van de betekenis waarnaar verwezen wordt) en bij de verwijzing te tonen. Ook doorklikken naar het doel wordt uiteraard een optie.

Er moet ook een eenvoudige standaardmanier zijn om ergens een lijst verwijzingen op te nemen, bijv. "gerelateerde woorden", en daar verwijzingen toe te voegen, verwijderen, van volgorde te veranderen, etc.

Er wordt ook voorzien in een mogelijkheid om eenvoudig alle "binnenkomende" verwijzingen bij een element te tonen (dus bijv. bij *waterval* betekenis 1 een lijst van alle entries die naar deze betekenis verwijzen).


### Flexibele "ingang" van een entry

Voor sommige projecten (ANW) is het lemma een unieke identifier. Andere projecten hebben een lemma en optioneel homonymnummer nodig. Weer andere projecten gebruiken lemma+pos of lemma+minidef.

Om hier flexibel mee om te gaan, gebruikt het systeem intern altijd (persistent) entry ids. Wanneer een gebruiker een entry wil openen, kunnen ze bijv. een deel van het lemma intypen. Er wordt dan gezocht op basis van de gedefinieerde entry-XPath voor het project (bijv. `/entry/lemma`), en als er meerdere mogelijkheden zijn, worden die in een dropdownlist getoond.


### Styling

Eenvoudige styling (kleuren, lettergrootte/type/styling, inspringing, list items, etc.) kan geconfigureerd worden zodat de entry er overzichtelijk uitziet.

Complexere layout zoals afbeeldingen, afspeelbare audio/video, tabbladen, kolommen, etc. zijn alleen via plugins te realiseren.


### Extensible met plugins

Het maken van een plugin is laagdrempelig en kan door een wat technischer gebruiker gedaan worden.

Met behulp van plugins kan de werking van de editor worden aangepast, bijv.:

- custom widgets
- kruisverwijzingen
- automatische acties, bijv. aanvullen van bepaalde informatie wanneer een veld ingevuld wordt, of automatisch gesorteerd houden van sommige elementen
- plugins die extra acties doen bij openen of opslaan van een entry
- extra validatie-acties


### Metadata

Metadata bestaat o.a. uit:

- wanneer een entry is aangemaakt of voor het laatst bewerkt
- de status van een entry of een onderdeel binnen een entry
- de medewerkers die aan de entry of het onderdeel gewerkt hebben

Per project kan geconfigureerd worden welke metadata in de lemmalijst getoond moet worden (met XPath expressies) en hoe sommige metadata (bijv. wie heeft er aan een onderdeel gewerkt) automatisch bijgewerkt kan worden (via plugins).


## Schermen in het user interface

Deze schermen zijn in elk geval nodig in het user interface:

- Inlogscherm
- Projectenscherm
- Settings voor het project, inclusief styling en plugins
- Entry-editor
- Formulier "Tabelgebaseerde view maken/bewerken"
- Tabelgebaseerde view (N.B. de "lemmalijst" is ook zo'n view)


## Technische realisatie


### Basistechnologieen

We kiezen zo veel mogelijk voor standaardtechnologieen waar we veel ervaring mee hebben:

- **Vue.js** voor het frontend
- **eXist-db** voor opslaan en efficiente querying van de XML documenten
- **PostgreSQL** voor het opslaan van projectsettings, styling en plugins
- **SaxonJS** (client-side) voor het uitvoeren van XPath 3.1 op (deel)entries
- **Saxon** (server-side) voor het parsen van het XML-Schema
- **CSS** voor styling van entries
- **XPath** voor het verwijzen naar elementen voor bijv. styling, plugins, table-based bewerken, etc.
- **Javascript** voor customization met plugins
- **Java** voor de API (vanwege performance en Saxon)
- **Docker** containers voor eXist-db, PostgreSQL en de API


### XML Schema ondersteuning

Omdat Saxon-HE (de gratis versie) geen gedetailleerde toegang tot XML-Schema biedt, zullen we (net als in INL-DWS) waarschijnlijk zelf ons XML-Schema moeten parseren. We laden het dan in onze eigen datastructuur (die van INL-DWS, of iets dat erop geinspireerd is) en kunnen dat ook (ws. in een JSON-structuur) aan de client sturen, die daarmee kan valideren en de editorfunctionaliteit kan opbouwen.

"Echt" valideren van Saxon-HE aan de serverkant is wel mogelijk. SaxonJS is niet schema-aware.

We houden de mogelijkheid in gedachten dat we in de toekomst misschien ondersteuning voor andere schematypes willen toevoegen, zoals Relax NG of Schematron.


### Autosave

Elke 60s wordt automatisch de entry opgeslagen op de server (als er iets veranderd is).


### Locking

De editor stuurt elke 60s een signaal naar de server "deze gebruiker is dit entry nog steeds aan het bewerken". De server geeft de entry automatisch vrij als het 90s lang geen signaal ontvangen heeft. Dit zorgt dat een entry automatisch wordt vrijgegeven als een gebruiker het tabblad sluit.


### Table view

Een per-table view kan geconfigureerd met:

- een XPath die de "rows" van de table oplevert (bijv. <nobr>`/entry[ends-with(lemma, 'tafel')]/sense`</nobr>, alle betekenissen van lemma's die op `tafel` eindigen)
- een (relatieve) XPath per kolom (bijv. `./definition`) om de definitie in deze kolom weer te geven.

Per-table views kunnen als read-only of editable gemarkeerd worden. Views kunnen als TSV gedownload worden.

Per-table views kunnen erg groot worden, dus aan de clientkant houden we steeds maar 1 pagina in het geheugen. De XPath-expressies worden uitgevoerd op de database (aan de serverkant dus). Bewerken van een cel gebeurt aan de client-kant (met dezelfde code waarmee een heel entry bewerkt kan worden). Opslaan van een gewijzigde cel gebeurt weer aan de serverkant.

eXist-db heeft voor elke node een intern id wat we naar de client kunnen meesturen. Als de gebruiker iets wijzigt, sturen we hetzelfde id terug naar de server zodat de node in eXist-db bijgewerkt kan worden. Op deze manier kunnen we efficient losse stukjes XML uit entries wijzigen. Locking hoeft alleen gedaan te worden tijdens het opslaan. (zie [`util:absolute-resource-id`](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/util&location=java:org.exist.xquery.functions.util.UtilModule#absolute-resource-id.1) / [`util:get-resource-by-absolute-id`](get-resource-by-absolute-id.1))

De lemmalijst die in andere DWS'en te vinden is, is in feite ook een table view, dus daarvoor zal deze functionaliteit gebruikt kunnen worden.

Om erachter te komen met welk gedeelte in het XML Schema een XML fragment in de tabel overeenkomt, stuurt de server altijd het pad van het rootelement naar dit element mee. Hiermee moeten we het juiste element in het XML Schema kunnen bepalen.


### Voorkomen van conflicterende edits

Om conflicterende edits te kunnen detecteren, houdt het backend bij welke entries/nodes wanneer gewijzigd zijn. De client houdt dit ook bij voor de nodes die in de table view getoond worden. Als een gebruiker een wijziging maakt, wordt gecontroleerd dat niemand anders in de tussentijd dit gedeelte van de entry gewijzigd heeft. Zo wel, dan wordt de laatste wijziging geannuleerd en krijgt de gebruiker een foutmelding te zien.


### XPath performance

Om XPaths op een grote database goed te laten werken, zullen we gebruikmaken van eXist-db's indexes, waaronder full text, ngrams en range indexes.


### Flexibele "titel" van een entry

Voor een project wordt met XPath een (als het goed is) unieke entry "titel" gedefinieerd. Bijvoorbeeld:

```xpath
concat(/entry/lemma, ' (', /entry/part-of-speech, ')')
```

We kijken of het zoeken op basis van deze XPath in eXist-db snel genoeg te krijgen is m.b.v. indexes. Dus iets als:

```xpath
starts-with(
  concat(/entry/lemma, ' (', /entry/part-of-speech, ')'),
  'bank')
```

Mocht dit niet snel genoeg zijn, dan zorgen we dat de titel apart opgeslagen wordt telkens als we een entry opslaan, zodat we hier direct in kunnen zoeken.


### Metadata

Metadata wordt zo veel mogelijk in de entry-XML opgenomen.

Bijvoorbeeld:

- De status van een entry (in bewerking / naar redactie / online) wordt in de XML bijgehouden.
- Per "blokje" (definitie/woordvorming/etymologie/etc.) wordt in de XML de status bijgehouden (in bewerking / afgerond), wanneer het onderwerp voor het laatst gewijzigd is, en evt. wie er aan gewerkt hebben.

Het automatisch bijhouden van zulke metadata wordt (indien gewenst) geimplementeerd met een projectspecifieke plugin. Alleen de "onderdeelstatus" zal handmatig gezet moeten worden.

Wanneer een entry gemaakt/gewijzigd is kunnen we vinden met [`xmldb:created()`](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/xmldb&location=java:org.exist.xquery.functions.xmldb.XMLDBModule#created.1) en [`xmldb:last-modified()`](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/xmldb&location=java:org.exist.xquery.functions.xmldb.XMLDBModule#last-modified.2).


### Plugins

Er zullen een aantal types plugins bestaan, die op verschillende plekken in het systeem kunnen inhaken.

- custom widgets verzorgen de gebruikersinterface voor tonen en bewerken van een element binnen het document (een simpel value-element, een complex elementen met child- en grandchild-elements, etc.)
- kruisverwijzingen
- automatische acties, bijv. aanvullen van bepaalde informatie wanneer een veld ingevuld wordt
- plugins die extra acties doen bij openen of opslaan van een entry
- extra validatie-acties

Om het pluginsysteem zo flexibel mogelijk te maken, is het waarschijnlijk verstandig om zo veel mogelijk van de "ingebouwde" widgets direct als (ingebouwde) plugins te realiseren. Daarmee is de kans het grootst dat plugins direct "first class citizens" binnen het systeem zijn.


### Implementatieplan

Proof of concept:

- Basis entry-editor (nog zonder customization)
- Table-based bewerking optie

Verdere uitwerking:

- Multi-project
- Multi-user, locking, per-user rechten
- Conflicterende edits voorkomen
- Plugins voor kruisverwijzingen, etc.
- Mixed content
- Meerdere entrytypes
- Undo/redo, autosave, backups
- Styling


## Technische notities

### XML opvragen met interne ids

Hiermee kun je een XML document opvragen, waarbij elk element ook het interne eXist-db id meekrijgt als attribuut. Hiermee kun je wijzigingen in het document efficient naar de server sturen (alleen de echte wijzigingen, niet telkens het hele document).

```xquery
xquery version "3.1";

let $doc := doc("/db/your/collection/your-document.xml")
return
    <root>
        {
            for $node in $doc//*
            return
                element {node-name($node)} {
                    attribute id {util:node-id($node)},
                    $node/@*,
                    $node/text(),
                    for $child in $node/*
                    return $child
                }
        }
    </root>
```

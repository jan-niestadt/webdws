# Web-based DWS FO/TO

## Overzicht

### Inleiding

Dit is een high-level functioneel en technisch ontwerp voor een nieuw dictionary writing system.

Het beschrijft een web-based systeem dat vergelijkbare functionaliteit biedt als INL-DWS en Lexonomy, zonder de belangrijkste nadelen van die systemen, en met de mogelijkheid om "tabelgebaseerd" te bewerken.


### Is het nodig?

Voor het bewerken van woordenboeken zoals ANW, WNW, Woordcombinaties en anderen gebruiken we op dit moment INL-DWS. Lexonomy wordt ook gebruikt, voor Vertaalwoordenschat. Beiden hebben nadelen, o.a. dat "tabelgebaseerde" bewerking niet mogelijk is; dit soort operaties worden gedaan door bijvoorbeeld naar Excel te exporteren en later de bewerkte lijst weer te importeren. Hierdoor gaat tijd verloren, voor taalkundigen en ontwikkelaars. Ook het maken van allerlei overzichten is niet zo eenvoudig en betekent werk voor ontwikkelaars.

Er wordt wel eens gedacht over de mogelijkheid om woordenboeken zoals ANW om te zetten naar een relationele database. Met hun huidige rijke, geneste structuur wordt dit erg lastig. Dit zou namelijk een database met vele tientallen gekoppelde tabellen opleveren die niet efficient te bevragen en bewerken is.

De keuze is dus: vereenvoudigen we de structuur zodat het project beter in een relationele database past, of accepteren we dat deze structuur een ander soort database en bewerkingstools vereisen?

Als we voor het tweede kiezen, hebben we dus een XML-bewerkingssysteem nodig waarin ook tabelgebaseerde bewerking mogelijk is.


### Is het realiseerbaar?

Het hier beschreven systeem bouwt op jarenlange ervaring met het bouwen en beheren van een DWS, en maakt gebruik van veel krachtige, populaire componenten die we vaak gebruikt hebben. Toch zal het maanden werk zijn om dit te realiseren.

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

Het systeem ondersteunt in elk geval XML-Schema. De mogelijkheid om in de toekomst andere schematypes (Relax NG, Schematron, DTD) te ondersteunen houden we in het achterhoofd.

100% support voor alle features van XML-Schema is niet voorzien. In elk geval zal TEI-Lex0 worden ondersteund, en de huidige schemata van ANW/WNW, NFW en Woordcombinaties. Als de gebruiker een schema probeert te gebruiken met niet-ondersteunde features, verschijnt een duidelijke foutmelding.

Op basis van alleen het schema wordt een compleet user interface voor bewerking gegenereerd, inclusief het toevoegen/verwijderen van elementen, het verplaatsen van elementen, cut/copy/paste, validatiefunctie, etc.

De keuze voor bepaalde widgets die niet direct uit het schema volgen, of andere customizations, kunnen wanneer nodig extra worden toegevoegd. Dit gebeurt via plugins (zie Extensibility).

(vgl Lexonomy waar vrij veel code nodig is om een bruikbare editor te maken)


### Table-based optie

Naast een user interface voor een hele entry (bijv. alle informatie over het woord *waterval*) is het mogelijk om een table-based view te definieren.

Als een gebruiker bijvoorbeeld de definities van een groep gerelateerde entries in een tabel wil kunnen bewerken (dus zonder elk entry apart te hoeven openen), is dat een kwestie van een view configureren van wat er in de tabel weergegeven zou moeten worden.

Meestal zal in een tabel-cel 1 widget komen, maar in principe moet elk stukje van een entry (een deel van het hele document) in een cel kunnen worden weergegeven en bewerkt.

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

Eenvoudige styling kan geconfigureerd worden zodat de entry er overzichtelijk uitziet.

- WEL: kleuren, lettergrootte/type/styling, inspringing, list items, etc.
- WOULD BE NICE: linked images. linked audio/video direct afspeelbaar (ws. via plugins)
- NIET: complexere layout zoals tabbladen, kolommen, etc.


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
- Formulier "Nieuw project"
- Settings voor het project, inclusief styling en plugins
- Entry-editor
- Formulier "Tabelgebaseerde view maken"
- Tabelgebaseerde view (N.B. de "lemmalijst" is ook zo'n view)


## Technische realisatie


### Basistechnologieen

We kiezen zo veel mogelijk voor standaardtechnologieen waar we veel ervaring mee hebben:
- **Vue.js** voor het frontend
- **eXist-db** voor het opslaan van de XML documenten
- **Saxon/SaxonJS** voor het parseren en valideren van (deel)entries
- **CSS** voor styling van entries
- **XPath** voor het verwijzen naar elementen voor bijv. styling, plugins, table-based bewerken, etc.
- **Javascript** voor customization met plugins
- **Java** voor de API (vanwege performance en Saxon)
- **Docker** containers voor eXist-db en de API


### Autosave, locking

Elke 60s wordt automatisch de entry opgeslagen op de server (als er iets veranderd is).


### Locking

De editor stuurt elke 60s een signaal naar de server "deze gebruiker is dit entry nog steeds aan het bewerken". De server geeft de entry automatisch vrij als het 90s lang geen signaal ontvangen heeft. Dit zorgt dat een gebruiker de browser kan sluiten zonder dat de entry gelockt blijft.


### Table view

Een per-table view kan geconfigureerd met:

- een XPath die de "rows" van de table oplevert (bijv. `/entry[ends-with(lemma, 'tafel')]/sense`, alle betekenissen van lemma's die op `tafel` eindigen)
- een (relatieve) XPath per kolom (bijv. `./definition`) om de definitie in deze kolom weer te geven.

Per-table views kunnen als read-only of editable gemarkeerd worden. Views kunnen als TSV gedownload worden.

Per-table views kunnen erg groot worden, dus aan de clientkant houden we steeds maar 1 pagina in het geheugen. De XPath-expressies worden uitgevoerd op de eXist database (aan de serverkant dus). Bewerken van een cel gebeurt aan de client-kant (met dezelfde code waarmee een heel entry bewerkt kan worden). Opslaan van een gewijzigde cel gebeurt weer aan de serverkant.

eXist-db heeft voor elke node een intern id wat we naar de client kunnen meesturen. Als de gebruiker iets wijzigt, sturen we hetzelfde id terug naar de server zodat de node in eXist-db bijgewerkt kan worden. Op deze manier kunnen we efficient losse stukjes XML uit entries wijzigen. Locking hoeft alleen gedaan te worden tijdens het opslaan. (zie util:[absolute-resource-id](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/util&location=java:org.exist.xquery.functions.util.UtilModule#absolute-resource-id.1) / [util:get-resource-by-absolute-id](get-resource-by-absolute-id.1))

De lemmalijst die in andere DWS'en te vinden is, is in feite ook een table view, dus daarvoor zal deze functionaliteit gebruikt kunnen worden.


### Voorkomen van conflicterende edits

Om conflicterende edits te kunnen detecteren, houdt het backend bij welke entries/nodes wanneer gewijzigd zijn. De client houdt dit ook bij voor de nodes die in de table view getoond worden. Als een gebruiker een wijziging maakt, wordt gecontroleerd dat niemand anders in de tussentijd dit gedeelte van de entry gewijzigd heeft. Zo wel, dan wordt de laatste wijziging geannuleerd en krijgt de gebruiker een foutmelding te zien.


### XPath performance

Om XPaths op een grote database goed te laten werken, zullen we gebruikmaken van eXist-db's indexes, waaronder full text, ngrams en range indexes.


### Flexibele "titel" van een entry

Voor een project wordt met XPath een (als het goed is) unieke entry "titel" gedefinieerd. Bijvoorbeeld `concat(/entry/lemma, ' (', /entry/part-of-speech, ')')`.

We kijken of het zoeken op basis van deze XPath (dus `concat(/entry/lemma, ' (', /entry/part-of-speech, ')') = 'bank (noun)'`) in eXist-db snel genoeg te krijgen is m.b.v. indexes.

Mocht het niet snel genoeg zijn, dan zorgen we dat de titel apart opgeslagen wordt telkens als we een entry opslaan, zodat we hier direct in kunnen zoeken.


### Metadata

Metadata wordt zo veel mogelijk in de entry-XML opgenomen.

Bijvoorbeeld:
- De status van een entry (in bewerking / naar redactie / online) wordt in de XML bijgehouden.
- Per "blokje" (definitie/woordvorming/etymologie/etc.) wordt in de XML de status bijgehouden (in bewerking / afgerond), wanneer het onderwerp voor het laatst gewijzigd is, en evt. wie er aan gewerkt hebben.

Het automatisch bijhouden van zulke metadata wordt (indien gewenst) geimplementeerd met een projectspecifieke plugin. Alleen de "onderdeelstatus" zal handmatig gezet moeten worden.

Wanneer een entry gemaakt/gewijzigd is kunnen we vinden met [xmldb:created()](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/xmldb&location=java:org.exist.xquery.functions.xmldb.XMLDBModule#created.1) en [xmldb:last-modified()](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/xmldb&location=java:org.exist.xquery.functions.xmldb.XMLDBModule#last-modified.2).



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

Hiermee kun je een XML document opvragen, waarbij elk element ook het interne exist-db id meekrijgt als attribuut. Hiermee kun je wijzigingen in het document efficient naar de server sturen (alleen de echte wijzigingen, niet telkens het hele document).

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

# Web-based DWS FO/TO


## Doel

Dit is een high-level functioneel en technisch ontwerp voor een nieuw dictionary writing system.

Het beschrijft een web-based systeem dat vergelijkbare functionaliteit biedt als INL-DWS en Lexonomy, zonder de belangrijkste nadelen van die systemen.


## Is het nodig?

Of het hier beschreven systeem nodig is, hangt af van projectinhoudelijke keuzes.

Als we vinden dat hierarchische, XML-based woordenboeken zoals bijv. ANW/WNW, Woordcombinaties, en de historische woordenboeken hun huidige geneste structuur moeten behouden, wordt het erg lastig om ze naar een relationele database te converteren. Dit zou namelijk een database met vele tientallen gekoppelde tabellen opleveren die niet efficient te bevragen en bewerken is.

Als we er dus voor kiezen om deze projecten in hun huidige structuur te houden, hebben we een XML-based bewerkingssysteem nodig, dat hierarchische structuren van (deel)entries "natively" ondersteunt. De huidige systemen (INL-DWS, Lexonomy, Oxygen XML Web Author) hebben elk hun eigen nadelen.

Misschien wel de belangrijkste wens is tabel-gebaseerd bewerken, wat medewerkers een hoop tijd zou kunnen schelen. Geen van de genoemde systemen ondersteunen dit op het moment. In Lex'it kan dit wel, maar is ontworpen voor relationele databases, die zoals uitgelegd niet goed passen bij de geneste structuur. Ook heeft Lex'it geen per-woord view zoals DWS editors wel hebben.


## Is het realiseerbaar?

Het hier beschreven systeem bouwt op jarenlange ervaring met het bouwen en beheren van een DWS, en maakt gebruik van veel krachtige, populaire componenten die de implementatie vergemakkelijken. Toch zal het een hoop werk zijn om dit systeem te realiseren.

Het valt te verwachten dat de implementatie alleen gerealiseerd kan worden als er via externe projectaanvragen (ws. met een of meer partners) subsidie kan worden binnengehaald.

Een projectaanvraag heeft waarschijnlijk meer kans als er al een prototype is dat de haalbaarheid aantoont en de voordelen boven andere systemen duidelijk maakt.

Een mogelijke partner is de Fryske Akademy. Zij gebruiken nu al ons INL-DWS, en Eduard Drenth (die dat project voor hen onderhoudt) pleit er al jaren voor om samen een beter alternatief te bouwen.


## Functionele eisen

- web-based
- XML schema bepaalt compleet default user interface; verdere customization is mogelijk
- flexibele mogelijkheden om zowel *per woord* (entry-gebaseerd) als *per eigenschap(pen)* (table-gebaseerd) te werken
- goede support voor kruisverwijzingen binnen het project en tussen projecten
- flexibel omgaan met de unieke "ingang" van een entry. Lemma, lemma+homonymnummer, lemma+pos, etc.
- meerdere entry-types (bijv. woord/idioom/formule)
- bepaalde styling kan worden geconfigureerd, zodat de editor min of meer WYSIWYG is
- bruikbaar door meerdere gebruikers
- goede support voor mixed content (bijv. voor eenvoudige opmaak zoals vet, cursief, super/subscript, etc.)
- volwaardige editor met undo/redo, autosave, etc.
- extensible via plugins, bijvoorbeeld voor custom widgets
- configureerbare metadata wordt bijgehouden
- backups van elk entry worden bijgehouden
- per-user rechten, bijv. read-only, of alleen (schrijf)toegang op bepaalde table-based views
- lemmalijst met configureerbare kolommen


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

Naast een user interface voor een hele entry (bijv. alles bij *koe*) is het mogelijk om een table-based view te definieren.

Als een gebruiker bijvoorbeeld de definities van een groep gerelateerde entries in een tabel wil kunnen bewerken (dus zonder elk entry apart te hoeven openen), is dat een kwestie van een view configureren van wat er in de tabel weergegeven zou moeten worden.

Meestal zal in een tabel-cel 1 widget komen, maar in principe moet elk stukje van een entry (een deel van het hele document) in een cel kunnen worden weergegeven en bewerkt.

De table-based optie is ook zeer geschikt voor zoeken en het maken van allerlei overzichten, bijv. alle semagrammen met hun categorieen. Table-overzichten kunnen opgeslagen worden als TSV.

Het aanmaken van een nieuwe table-based view zou in het user interface te doen moeten zijn.

Een table-based view kan uiteraard niet alle rows gelockt houden in de database. Alleen de rij die op dit moment bewerkt wordt, zal gelockt worden.


### XPath performance

Om XPaths op een grote database goed te laten werken, zullen we gebruikmaken van eXist-db's indexes, waaronder full text, ngrams en range indexes.


### Bruikbaar door meerdere gebruikers

Meerdere gebruikers kunnen het systeem tegelijkertijd gebruiken. Als een gebruiker een entry opent, wordt het gelockt, zodat anderen niet hetzelfde entry kunnen bewerken.

Meerdere gebruikers kunnen dezelfde gegevens in een table view open hebben. Als een van de twee een wijziging maakt, en daarna probeert de ander dezelfde gegevens te wijzigen, wordt dit conflict gedetecteerd en een foutmelding getoond. De kans hierop is in de praktijk uiteraard klein.

Table views synchroniseren *niet* automatisch, dus als een andere gebruiker de gegevens wijzigt die in jouw table view getoond worden, zie je ze niet automatisch mee veranderen. Wel kun je de table view handmatig verversen om de nieuwste gegevens te zien.


### Kruisverwijzingen

Kruisverwijzingen binnen het project en tussen projecten zijn mogelijk. Omdat elk project een andere manier van verwijzen kan gebruiken, wordt dit waarschijnlijk via plugins geimplementeerd (zie Extensibility).

Er wordt voorzien in de mogelijkheid om informatie over het doel van de verwijzing op te halen (bijv. korte definitie van de betekenis waarnaar verwezen wordt) en bij de verwijzing te tonen. Ook doorklikken naar het doel wordt uiteraard een optie.

Er moet ook een eenvoudige standaardmanier zijn om ergens een lijst verwijzingen op te nemen, bijv. "gerelateerde woorden", en daar verwijzingen toe te voegen, verwijderen, van volgorde te veranderen, etc.

### Flexibele "ingang" van een entry

Voor sommige projecten (ANW) is het lemma een unieke identifier. Andere projecten hebben een lemma en optioneel homonymnummer nodig. Weer andere projecten gebruiken lemma+pos of lemma+minidef.

Om hier flexibel mee om te gaan, gebruikt het systeem intern altijd (persistent) entry ids. Wanneer een gebruiker een entry wil openen, kunnen ze bijv. een deel van het lemma intypen. Er wordt dan gezocht op basis van de gedefinieerde entry-XPath voor het project (bijv. `/entry/lemma`), en als er meerdere mogelijkheden zijn, worden die in een dropdownlist getoond.


### Styling

- WEL: kleuren, lettergrootte/type/styling, inspringing, list items, etc.
- WOULD BE NICE: linked images. linked audio/video direct afspeelbaar (ws. via widget plugins)
- NIET: complexere layout zoals tabbladen, kolommen, etc.


### Extensible met plugins

Het maken van een plugin is laagdrempelig.

Met behulp van plugins kan de werking van de editor worden aangepast, bijv.:
- custom widgets
- kruisverwijzingen
- automatische acties, bijv. aanvullen van bepaalde informatie wanneer een veld ingevuld wordt
- plugins die extra acties doen bij openen of opslaan van een entry
- extra validatie-acties


### Metadata

Metadata wordt zo veel mogelijk in de XML opgenomen.

Bijvoorbeeld:
- De status van een entry (in bewerking / naar redactie / online) wordt in de XML bijgehouden.
- Per "blokje" (definitie/woordvorming/etymologie/etc.) wordt in de XML de status bijgehouden (in bewerking / afgerond), wanneer het onderwerp voor het laatst gewijzigd is, en evt. wie er aan gewerkt hebben.

Het automatisch bijhouden van zulke metadata wordt (indien gewenst) geimplementeerd met een projectspecifieke plugin. Alleen de "onderdeelstatus" zal handmatig gezet moeten worden.

Wanneer een entry gemaakt/gewijzigd is kunnen we vinden met [xmldb:created()](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/xmldb&location=java:org.exist.xquery.functions.xmldb.XMLDBModule#created.1) en [xmldb:last-modified()](https://exist-db.org/exist/apps/fundocs/view.html?uri=http://exist-db.org/xquery/xmldb&location=java:org.exist.xquery.functions.xmldb.XMLDBModule#last-modified.2).


### Automatisch sorteren

Waar nuttig kan er gekozen worden om sibling-elementen zichzelf te laten sorteren. Dit zal per geval in een plugin gevangen moeten worden, zodat de sortering kan worden afgestemd op de eisen van het project.



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


### Flexibele "titel" van een entry

Voor een project wordt met XPath een (als het goed is) unieke entry "titel" gedefinieerd. Bijvoorbeeld `concat(/entry/lemma, ' (', /entry/part-of-speech, ')')`.

We kijken of het zoeken op basis van deze XPath (dus `concat(/entry/lemma, ' (', /entry/part-of-speech, ')') = 'bank (noun)'`) in eXist-db snel genoeg te krijgen is m.b.v. indexes.

Mocht het niet snel genoeg zijn, dan zorgen we dat de titel apart opgeslagen wordt telkens als we een entry opslaan, zodat we hier direct in kunnen zoeken.


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
- Conflicterende edits voorkomen
- Plugins voor kruisverwijzingen, etc.
- Mixed content
- Meerdere entrytypes
- Undo/redo, autosave, backups
- Multi-user, locking, per-user rechten
- Styling

### Diverse verbeteringen

- Om het schrijven van XPath expressies te vereenvoudigen kunnen we het mogelijk maken om (via plugins) XPath-extensiefuncties toe te voegen. Bijvoorbeeld een functie die "betekenissen van lemma's beginnend met X" zoekt, etc.

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

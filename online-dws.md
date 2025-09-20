# Web-based DWS FO/TO

## Doel

Dit is een high-level functioneel en technisch ontwerp voor een nieuw dictionary writing system.

Het beschrijft een web-based systeem dat vergelijkbare functionaliteit biedt als "INL-DWS/SwingLex" en Lexonomy, zonder de belangrijkste nadelen van die systemen.

## Is het nodig?

Of het hier beschreven systeem nodig is, hangt af van projectinhoudelijke keuzes.

Als we vinden dat hierarchische, XML-based woordenboeken zoals bijv. ANW/WNW, Woordcombinaties, en de historische woordenboeken hun huidige geneste structuur moeten behouden, wordt het erg lastig om ze naar een relationele database te converteren. Dit zou namelijk een database met vele tientallen gekoppelde tabellen opleveren die niet efficient te bevragen en bewerken is.

Als we er dus voor kiezen om deze projecten in hun huidige structuur te houden, hebben we een XML-based bewerkingssysteem nodig, dat hierarchische structuren van (deel)artikelen "natively" ondersteunt. De huidige systemen (INL-DWS, Lexonomy, Oxygen XML Web Author) hebben elk hun eigen nadelen.

Misschien wel de belangrijkste wens is tabel-gebaseerd bewerken, wat medewerkers een hoop tijd zou kunnen schelen. Geen van de genoemde systemen ondersteunen dit op het moment. In Lex'it kan dit wel, maar is ontworpen voor relationele databases, die zoals uitgelegd niet goed passen bij de geneste structuur. Ook heeft Lex'it geen per-woord view zoals DWS editors wel hebben.


## Functionele eisen

- web-based
- schema bepaalt compleet default user interface; verdere customization is mogelijk
- flexibele mogelijkheden om zowel *per woord* (artikel-gebaseerd) als *per eigenschap(pen)* (table-gebaseerd) te werken
- goede support voor kruisverwijzingen binnen het project en tussen projecten
- bepaalde styling kan worden geconfigureerd, zodat de editor min of meer WYSIWYG is
- bruikbaar door meerdere gebruikers (artikel wordt gelockt wanneer een gebruiker het opent)
- meerdere artikeltypes (bijv. woord/idioom/formule)
- goede support voor mixed content (bijv. voor eenvoudige opmaak zoals vet, cursief, super/subscript, etc.)
- volwaardige editor met undo/redo, autosave, etc.
- extensible via plugins, bijvoorbeeld voor custom widgets
- backups van elk artikel worden bijgehouden
- per-user rechten, bijv. read-only, of alleen (schrijf)toegang op bepaalde table-based views
- lemmalijst met configureerbare kolommen


### Web-based

Het systeem heeft een web-based user interface, zodat het ook door externen gebruikt kan worden.

Het user interface is toegespitst op desktopgebruik. Gebruik via kleinere (touch)screens is mogelijk maar niet het primaire doel.

(vgl INL-DWS dat eigenlijk alleen intern of via telewerkserver kan worden uitgevoerd)

### User interface volgt uit het schema

Het systeem ondersteunt in elk geval (deels) XML-Schema. 100% support voor alle features is niet voorzien. Als de gebruiker een schema probeert te gebruiken met niet-ondersteunde features, krijgt die een duidelijke foutmelding.

Op basis van alleen het schema wordt een compleet user interface voor bewerking gegenereerd, inclusief het toevoegen/verwijderen van elementen, het verplaatsen van elementen, cut/copy/paste, validatiefunctie, etc.

De keuze voor bepaalde widgets die niet direct uit het schema volgen, of andere customizations, kunnen wanneer nodig extra worden toegevoegd. Dit gebeurt via plugins (zie Extensibility).

(vgl Lexonomy waar vrij veel code nodig is om een bruikbare editor te maken)


### Table-based optie

Naast een user interface voor een heel artikel is het mogelijk om een table-based view te definieren.

Als een gebruiker bijvoorbeeld de definities van een groep gerelateerde artikelen in een tabel wil kunnen bewerken (dus zonder elk artikel apart te hoeven openen), is dat een kwestie van een view configureren van wat er in de tabel weergegeven zou moeten worden.

Meestal zal in een tabel-cel 1 widget komen, maar in principe moet elk stukje artikel (een "subboom" van het hele artikel) in een cel kunnen worden weergegeven en bewerkt.

De table-based optie is ook zeer geschikt voor zoeken en het maken van allerlei overzichten, bijv. alle semagrammen met hun categorieen. Table-overzichten kunnen opgeslagen worden als TSV.

Het aanmaken van een nieuwe table-based view zou in het user interface te doen moeten zijn.

Een table-based view kan uiteraard niet alle rows gelockt houden in de database. Alleen de rij die op dit moment bewerkt wordt, zal gelockt worden.


### Synchronisatie van views

Een gebruiker kan meerdere views met dezelfde gegevens open hebben in meerdere tabbladen. Bijvoorbeeld een artikel in 1 tabblad, en gegevens uit dat zelfde artikel in een tabelview in een ander tabblad. Ook kunnen verschillende gebruikers dezelfde gegevens in een table view open hebben. Als in een van deze 2 views iets gewijzigd wordt, moeten andere views automatisch verversen.


### Kruisverwijzingen

Kruisverwijzingen binnen het project en tussen projecten zijn mogelijk. Omdat elk project een andere manier van verwijzen kan gebruiken, wordt dit waarschijnlijk via plugins geimplementeerd (zie Extensibility).

Er wordt voorzien in de mogelijkheid om informatie over het doel van de verwijzing op te halen (bijv. korte definitie van de betekenis waarnaar verwezen wordt) en bij de verwijzing te tonen. Ook doorklikken naar het doel wordt uiteraard een optie.

Er moet ook een eenvoudige standaardmanier zijn om ergens een lijst verwijzingen op te nemen, bijv. "gerelateerde woorden", en daar verwijzingen toe te voegen, verwijderen, van volgorde te veranderen, etc.

### Styling

WEL: kleuren, lettergrootte/type/styling, inspringing, list items, etc.
WOULD BE NICE: linked images. linked audio/video direct afspeelbaar.
NIET: complexere layout zoals tabbladen, kolommen, etc.


### Extensible via plugins

Het maken van een plugin is laagdrempelig.

Met behulp van plugins kan de werking van de editor worden aangepast, bijv.:
- custom widgets
- kruisverwijzingen
- automatische acties, bijv. aanvullen van bepaalde informatie wanneer een veld ingevuld wordt
- plugins die extra acties doen bij openen of opslaan van een artikel
- extra validatie-acties


### Automatisch sorteren

Waar nuttig kan er gekozen worden om sibling-elementen zichzelf te laten sorteren. Dit zal per geval in een plugin gevangen moeten worden, zodat de sortering kan worden afgestemd op de eisen van het project.



## Technische realisatie


### Basistechnologieen

We kiezen zo veel mogelijk voor standaardtechnologieen waar we veel ervaring mee hebben:
- **Vue.js** voor het frontend
- **eXist-db** voor het opslaan van de artikelen en bewerkingsmetadata (wordt geintegreerd in het artikel)
- **Saxon/SaxonJS** voor het parseren en valideren van (deel)artikelen
- **CSS** voor styling van artikelen
- **XPath** voor het verwijzen naar elementen voor bijv. styling, plugins, table-based bewerken, etc.
- **Javascript** voor customization met plugins
- **Java** voor de API (vanwege performance en Saxon)
- **Docker** containers voor eXist-db en de API


### Autosave, locking

Elke 60s wordt automatisch het artikel opgeslagen op de server (als er iets veranderd is).


### Locking

De editor stuurt elke 60s een signaal naar de server "deze gebruiker is dit artikel nog steeds aan het bewerken". De server geeft het artikel automatisch vrij als het 90s lang geen signaal ontvangen heeft. Dit zorgt dat een gebruiker de browser kan sluiten zonder dat het artikel gelockt blijft.


### Table view

Een per-table view kan geconfigureerd met:

- een XPath die de "rows" van de table oplevert (bijv. `/artikel[ends-with(Lemma/Lemmavorm, 'tafel')]/Kernbetekenis`, alle betekenissen van lemma's die op `tafel` eindigen)
- een (relatieve) XPath per kolom (bijv. `definitieBody/Definitie`) om de Definitie in deze kolom weer te geven.

Per-table views kunnen als read-only of editable gemarkeerd worden. Views kunnen als TSV gedownload worden.

Per-table views kunnen erg groot worden, dus aan de clientkant houden we steeds maar 1 pagina in het geheugen. De XPath-expressies worden uitgevoerd op de eXist database (aan de serverkant dus). Bewerken van een cel gebeurt aan de client-kant (met dezelfde code waarmee een heel artikel bewerkt kan worden). Opslaan van een gewijzigde cel gebeurt weer aan de serverkant.

Hoe precies bijgehouden wordt welke stukjes XML uit welke artikelen we aan het bewerken zijn, en hoe ze dus weer in hun oorspronkelijke artikel kunnen worden opgeslagen, is nog een open vraag. Worst-case moeten we het hele artikel naar de browser sturen, maar hopelijk kunnen we volstaan met een id van of pad naar de node. Er moet altijd gekeken worden of een andere gebruiker in de tussentijd niets gewijzigd heeft.

De lemmalijst die in andere DWS'en te vinden is, is in feite ook een table view, dus daarvoor zal deze functionaliteit gebruikt kunnen worden.


### Synchronisatie van views

Om view te kunnen synchroniseren, houdt het backend bij welke entries wanneer gewijzigd zijn. Wanneer het frontend en backend elke 60s communiceren, geeft de server een lijst van gewijzigde entries door.

Exact wat er gecommuniceerd wordt, moet nog bekeken worden. Mogelijk alleen id's van gewijzigde entries, of misschien kan de server bepalen exact welke wijzigingen relevant zijn per client.


### Implementatieplan

Proof of concept:
- Basis artikeleditor (nog zonder customization)
- Table-based bewerking optie

Verdere uitwerking:
- View-synchronisatie
- Plugins voor kruisverwijzingen, etc.
- Mixed content
- Meerdere artikeltypes
- Undo/redo, autosave, backups
- Multi-user, locking, per-user rechten
- Styling

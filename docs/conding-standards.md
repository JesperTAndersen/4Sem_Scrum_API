# Coding Standards

**Version:** 1.0  
**Date:** 2026-09-07  
**Project:** Scrum Project: *Estimo*  
**Semester:** 4. semester Datamatiker

---

## 1. Indledning

Dette dokument beskriver de kodestandarder og best practices, der skal følges i Fog-Carport projektet. Alle teammedlemmer er forpligtet til at overholde disse standarder for at sikre ensartet, læsbar og vedligeholdelsesvenlig kode.

---

## 2. Teknologi Stack

- **Sprog:** Java
- **Database:** PostgreSQL
- **Connection Pool:** HikariCP
- **Arkitektur:** MVC (Model-View-Controller)
- **Build Tool:** Maven
- **Version Control:** Git/GitHub
- **Testing Framework:** JUnit
- **IDE:** IntelliJ IDEA

---

## 3. Editor og Formatering

### 3.1 EditorConfig
- Projektet indeholder en `.editorconfig` fil i projektmappen
- ALLE udviklere SKAL bruge disse indstillinger
- Sørg for at din IDE/editor understøtter EditorConfig
- Installer EditorConfig plugin hvis nødvendigt

### 3.2 Generelle Formateringsregler
- **Indrykning:** Som specificeret i `.editorconfig`
- **Linjeskift:** Unix-style (LF)
- **Encoding:** UTF-8

---

## 4. Navngivningskonventioner

### 4.1 Klasser
- **PascalCase**
- Navne skal være substantiver og beskrivende
- Eksempler: OrderMapper, CarportController, CustomerService

### 4.2 Interfaces
- **PascalCase**
- Må gerne starte med 'I' hvis det giver mening, men ikke påkrævet
- Eksempler: DataMapper, IDataMapper

### 4.3 Metoder
- **camelCase**
- Navne skal være verber eller verbfraser
- Eksempler: calculatePrice(), getOrderById(), saveCustomer()

### 4.4 Variabler
- **camelCase**
- Beskrivende navne - undgå forkortelser
- Konstanter i **UPPER_SNAKE_CASE**
- Eksempler: customerName, orderId, MAX_CARPORT_WIDTH

### 4.5 Packages
- **lowercase**, kun små bogstaver
- Følg domænestruktur
- Eksempler: app.persistence, app.controllers, app.entities

---

## 5. Arkitektur (MVC)

### 5.1 Model
- Placeres i `app.entities` package
- Indeholder kun data og simple getters/setters
- Ingen forretningslogik

### 5.2 View
- Ingen forretningslogik i views
- Kun præsentationslogik

### 5.3 Controller
- Placeres i `app.controllers` package
- Håndterer HTTP requests
- Delegerer forretningslogik til services
- Kalder mappers for data access

### 5.4 Persistence (Mappers)
- Placeres i `app.persistence` package
- Ansvarlig for database-operationer
- Skal bruge **Dependency Injection** for ConnectionPool
- Alle mapper-klasser skal kunne testes med integration tests

### 5.5 Services
- Placeres i `app.services` package
- Indeholder forretningslogik
- Kalder mappers for data access

---

## 6. Dependency Injection i ConnectionPool
- ALTID brug dependency injection ved brug af ConnectionPool
- Dette gør det muligt at teste mapper-klasser med integration tests
- Undgå at kalde `ConnectionPool.getInstance()` direkte i mapper-klasser
- Constructor injection er den foretrukne metode

---

## 7. Kommentarer
- Brug sparsomt - koden skal være selvforklarende
- Forklar **hvorfor**, ikke **hvad**
- Undgå unødvendige kommentarer

---

## 8. Exception Handling

### 8.1 Generelt
- Fang ALDRIG exceptions uden at håndtere dem
- Log alle exceptions
- Kast exceptions op når det giver mening
- Undgå tomme catch-blokke

### 8.2 Custom Exceptions
- Opret custom exceptions når det giver mening
- Placér dem i `app.exceptions` package
- Lad dem arve fra passende exception-klasser

---

## 9. Database Operationer

### 9.1 PreparedStatements
- Brug ALTID PreparedStatements (aldrig string concatenation)
- Beskytter mod SQL injection
- Bedre performance

### 9.2 Resource Management
- Brug try-with-resources for alle JDBC objekter
- Sikrer at ressourcer lukkes korrekt
- Gælder for Connection, PreparedStatement, ResultSet

---

## 10. Testing

### 10.1 Unit Tests
- Skriv unit tests for forretningslogik
- Brug JUnit
- Placér tests i `src/test/java` med samme package struktur

### 10.2 Integration Tests
- Test mapper-klasser med integration tests
- Brug test-database (ikke produktion!)
- Brug dependency injection til at injicere test ConnectionPool

---

## 11. Git Workflow og Branching

### 11.1 Branch Strategi
- **main:** Produktionsklar kode (kun Tech Lead merger)
- **dev:** Udviklingsbranch (pull requests påkrævet)
- **feature/[feature-navn]:** Feature branches

### 11.2 Commit Beskeder
- Skriv klare, beskrivende commit beskeder på dansk
- Brug imperativ form: "Tilføj feature" ikke "Tilføjet feature"
- Reference issue nummer hvis relevant
- Undgå ubrugelige beskeder som "Updates", "fix", "asdf"

### 11.3 Commit Frekvens
- **Minimum:** Commit mindst én gang dagligt på arbejdsdage
- **Anbefalet:** Commit efter hver logisk ændring
- **Formål:** Sikre backup og minimere risiko for tab af arbejde
- Push til remote repository dagligt

### 11.4 Pull Requests
- ALLE merges til dev skal ske via pull request
- **Undtagelse:** Tech Lead kan merge direkte i særlige tilfælde
- PR skal review'es af mindst én anden udvikler
- Kør tests før PR oprettes

### 11.5 Merge Konflikter
- **I dev:** Løs konflikter i din feature branch
- Ved tvivl: Kontakt Tech Lead
- **I main:** Kun Tech Lead håndterer merge til main
- Andre skal aftales med Tech Lead eller ved fravær Team Lead

---

## 12. Overdragelse ved Fravær

### 12.1 Ved Sygdom eller Fravær
- Hver udvikler skal have en **klar plan for overdragelse**
- Dokumentér status på igangværende opgaver
- Scrum Master uddelegerer opgaver ved længerevarende fravær

### 12.2 Daglig Status
- Opdater opgavestatus i projekt management tool
- Skriv TODO kommentarer i koden hvis nødvendigt
- Kommuniker med teamet ved planlagt fravær

---

## 13. Software Versionering

### 13.1 Java Version
- **Java 17** (eller som specificeret af Tech Lead)
- Alle skal bruge samme Java version

### 13.2 Dependencies
- Versionering håndteres i `pom.xml`
- ALDRIG opdater dependencies uden aftale med Tech Lead
- Tech Lead informerer om ændringer i versionering

---

## 14. Code Review Checklist

Før du opretter en Pull Request, tjek:

- Koden følger navngivningskonventioner
- EditorConfig regler er overholdt
- ConnectionPool bruger dependency injection
- PreparedStatements bruges til alle SQL queries
- Try-with-resources bruges til resource management
- Tests er skrevet og kører succesfuldt
- Commit beskeder er klare og beskrivende
- Ingen merge konflikter med dev branch

---

## 15. Ansvar og Roller

### Tech Lead
- Opretholder og opdaterer denne codingstandards.md
- Reviewer alt kode før merge til main
- Godkender ændringer i dependencies og versionering
- Kontrollerer overholdelse af standarder

### Team Lead (ved Tech Leads fravær)
- Overtager Tech Leads ansvar ved fravær
- Håndterer kritiske beslutninger

### Scrum Master
- Sikrer proces-overholdelse
- Uddelegerer opgaver ved fravær i teamet

### Alle Udviklere
- Overholder alle kodestandarder
- Deltager i code reviews
- Kommunikerer proaktivt ved problemer

---

## 16. Kontakt

**Ved spørgsmål eller tvivl:**
1. Konsultér denne codingstandards.md
2. Spørg i team chat
3. Kontakt Tech Lead
4. Ved Tech Leads fravær: Kontakt Team Lead
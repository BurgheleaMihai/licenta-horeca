## Progres implementare

### Ziua 1 - Structura proiect, backend si baza de date

- A fost creata structura principala a proiectului:
    - `backend`
    - `frontend`
    - `ai-service`
    - `database`
    - `documentation`

- A fost creat proiectul Spring Boot in folderul `backend`.
- A fost configurat Maven.
- Au fost adaugate dependentele principale:
    - Spring Web
    - Spring Data JPA
    - MySQL Driver
    - Validation
    - Spring Boot DevTools

- A fost instalat si configurat MySQL Server.
- A fost creata baza de date `horeca_db`.
- A fost configurata conexiunea Spring Boot - MySQL in `application.properties`.
- Backend-ul a fost rulat cu succes pe portul `8080`.

Status: finalizat.

---

### Ziua 2 - Entitati si repository-uri de baza

- Au fost create pachetele:
    - `entity`
    - `enums`
    - `repository`

- Au fost implementate entitatile:
    - `Role`
    - `User`
    - `RestaurantTable`
    - `TableSession`
    - `Category`
    - `Product`

- A fost implementat enum-ul:
    - `RoleType`

- Au fost implementate repository-urile:
    - `RoleRepository`
    - `UserRepository`
    - `RestaurantTableRepository`
    - `TableSessionRepository`
    - `CategoryRepository`
    - `ProductRepository`

- Hibernate a generat automat in MySQL urmatoarele tabele:
    - `roles`
    - `app_users`
    - `restaurant_tables`
    - `table_sessions`
    - `categories`
    - `products`

- Tabelele au fost verificate in MySQL Workbench cu query-uri SQL:
    - `SHOW TABLES`
    - `DESCRIBE roles`
    - `DESCRIBE app_users`
    - `DESCRIBE restaurant_tables`
    - `DESCRIBE table_sessions`
    - `DESCRIBE categories`
    - `DESCRIBE products`

- Au fost create query-uri SQL de verificare pentru:
    - utilizatori si roluri
    - mese si sesiuni QR
    - produse si categorii
    - verificare generala tabele

Status: finalizat.

---

### Ziua 3 - Endpoint produse si date demo pentru produse/categorii

In aceasta etapa a fost implementat primul modul REST functional pentru produse.

Au fost create clasele:
- `ProductService.java`
- `ProductController.java`

`ProductService` contine logica de citire a produselor din baza de date, folosind `ProductRepository`.

`ProductController` expune endpoint-uri REST prin care produsele pot fi accesate din exteriorul aplicatiei.

Au fost implementate urmatoarele endpoint-uri:
- `GET /api/products` - returneaza toate produsele
- `GET /api/products/available` - returneaza doar produsele disponibile
- `GET /api/products/category/{categoryId}` - returneaza produsele dintr-o anumita categorie

Endpoint-ul principal a fost testat in browser:

`http://localhost:8080/api/products`

Initial, endpoint-ul a returnat lista goala:

`[]`

Acest rezultat a confirmat ca backend-ul functioneaza, dar tabela `products` nu continea inca date demo.

Apoi au fost introduse date demo in baza de date, pentru a testa endpoint-urile cu date reale.

A fost creat si rulat scriptul SQL:

`exemplu_produse_3.sql`

Scriptul introduce date in urmatoarele tabele:
- `categories`
- `products`

Au fost introduse 4 categorii:
- Pizza
- Paste
- Bauturi
- Deserturi

Au fost introduse 8 produse demo:
- Pizza Margherita
- Pizza Diavola
- Paste Carbonara
- Paste Bolognese
- Apa plata
- Limonada
- Tiramisu
- Papanasi

Produsul `Papanasi` a fost introdus ca indisponibil, pentru a testa filtrarea produselor disponibile.

Dupa introducerea datelor, endpoint-urile au fost testate din nou in browser.

Verificari manuale efectuate:

`GET http://localhost:8080/api/products`

Rezultat: returneaza toate cele 8 produse.

`GET http://localhost:8080/api/products/available`

Rezultat: returneaza doar cele 7 produse disponibile.

`GET http://localhost:8080/api/products/category/1`

Rezultat: returneaza doar produsele din categoria Pizza:
- Pizza Margherita
- Pizza Diavola

Aceasta etapa confirma functionarea completa a fluxului:

`MySQL -> ProductRepository -> ProductService -> ProductController -> JSON response`

Momentan produsele sunt afisate in browser ca raspuns JSON brut. Stilizarea meniului va fi realizata ulterior in aplicatia frontend React, unde produsele vor fi afisate sub forma de carduri/lista de meniu pentru client.

Status: finalizat.

---

### Ziua 4 - Testare produse si initializare frontend React

Au fost realizate activitatile planificate pentru zilele 9 si 10 din planul de lucru al licentei. Accentul a fost pus pe testarea modulului de produse si pe pregatirea mediului pentru dezvoltarea frontend-ului React.

#### Ziua 9 din plan - Testare cluster Produse

In aceasta etapa a fost inceputa aplicarea strategiei de testare stabilite pentru proiect: Modified Sandwich Integration, completata cu Risk-driven prioritization si Scenario-Based Cluster Integration.

A fost testat clusterul functional Produse, format din:
- `ProductController`
- `ProductService`
- `ProductRepository`
- `Product`
- `Category`
- `CategoryRepository`
- baza de date MySQL

Testarea acestui cluster se incadreaza in scenariul „Clientul vede meniul”, deoarece produsele si categoriile reprezinta baza meniului care va fi afisat ulterior in interfata clientului.

Au fost verificate manual urmatoarele endpoint-uri REST:
- `GET /api/products`
- `GET /api/products/available`
- `GET /api/products/category/{categoryId}`

Endpoint-ul `GET /api/products` a returnat toate cele 8 produse introduse in baza de date. Endpoint-ul `GET /api/products/available` a returnat doar cele 7 produse disponibile, iar produsul `Papanasi` nu a fost afisat deoarece are `available = false`. Endpoint-ul `GET /api/products/category/1` a returnat doar produsele din categoria Pizza.

Prin aceste verificari s-a confirmat functionarea fluxului complet:

`HTTP request -> ProductController -> ProductService -> ProductRepository -> MySQL -> JSON response`

Inainte de testare a aparut o problema de conectare intre Spring Boot si MySQL, cu mesajul `Public Key Retrieval is not allowed`. Problema a fost rezolvata prin adaugarea parametrului `allowPublicKeyRetrieval=true` in URL-ul de conectare din `application.properties`. Aceasta problema a tinut de configurarea conexiunii JDBC cu MySQL, nu de logica modulului de produse.

Pe langa testarea manuala a endpoint-urilor, a fost creat si un test automat pentru `ProductService`:
- `ProductServiceTest.java`

Testul verifica metoda `getAvailableProducts()`, folosind `ProductRepository` simulat cu Mockito. Scopul testului este sa confirme ca metoda returneaza doar produsele disponibile.

Rezultatul testului automat:
- `Tests passed: 1 of 1`

Astfel, modulul de produse a fost verificat atat manual, prin endpoint-uri REST, cat si automat, printr-un test unitar pentru service.

#### Ziua 10 din plan - Initializare frontend React

In aceasta etapa a fost pregatit mediul pentru dezvoltarea frontend-ului aplicatiei.

A fost instalat Node.js impreuna cu npm. Instalarea a fost verificata in terminal prin comenzile:
- `node -v`
- `npm -v`

Versiunile identificate au fost:
- Node.js: `v24.15.0`
- npm: `11.12.1`

Dupa instalarea mediului necesar, a fost creat proiectul frontend folosind React si Vite, in folderul `frontend`.

Comanda folosita pentru creare a fost:

`npm create vite@latest frontend -- --template react`

Dupa creare, aplicatia frontend a fost pornita cu succes cu serverul de dezvoltare Vite. In terminal a fost afisat mesajul:

`VITE ready`

Aplicatia frontend a fost accesata in browser la adresa:

`http://localhost:5173/`

Aceasta etapa confirma ca mediul de frontend este functional si pregateste dezvoltarea interfetelor pentru client, ospatar, bucatarie si manager.

Status: finalizat.

---

### Ziua 5 - Frontend client, filtre meniu si feedback anonim

In ziua 5 au fost realizate activitatile planificate pentru zilele 11-17 din planul de lucru al licentei. Accentul a fost pus pe dezvoltarea interfetei pentru client, conectarea frontend-ului React cu backend-ul Spring Boot, filtrarea meniului si implementarea feedback-ului anonim.

#### Ziua 11 din plan - Pagina de meniu client

A fost creata prima pagina frontend pentru client:

- `ClientMenuPage.jsx`

Initial, pagina a folosit produse hardcodate in React pentru a construi structura vizuala a meniului.

Pentru fiecare produs au fost afisate:

- categoria;
- numele produsului;
- descrierea;
- pretul;
- disponibilitatea.

Produsele au fost afisate sub forma de carduri. Produsul `Papanasi` a fost afisat ca indisponibil, pentru a demonstra diferenta dintre produsele disponibile si cele existente in meniu, dar care nu pot fi comandate momentan.

A fost modificat fisierul:

- `App.jsx`

pentru ca aplicatia React sa afiseze pagina `ClientMenuPage` in locul paginii implicite generate de Vite.

A fost actualizat si fisierul:

- `App.css`

pentru stilizarea paginii de meniu si a cardurilor de produse.

Pagina clientului are rol de consultare a meniului digital. Clientul poate vedea produsele, descrierile, preturile si disponibilitatea acestora, insa crearea comenzii ramane responsabilitatea ospatarului.

#### Ziua 12 din plan - Conectare frontend React cu backend Spring Boot

In aceasta etapa pagina de meniu client a fost conectata la backend-ul Spring Boot.

A fost instalata biblioteca:

- `axios`

Aceasta este folosita pentru realizarea cererilor HTTP din frontend catre backend.

A fost creat fisierul:

- `src/api/productApi.js`

Acesta contine functii pentru apelarea endpoint-urilor de produse:

- `getAllProducts()`
- `getAvailableProducts()`
- `getProductsByCategory(categoryId)`

Componenta `ClientMenuPage.jsx` a fost modificata pentru a folosi `useEffect` si `useState`. La incarcarea paginii, frontend-ul apeleaza backend-ul si incarca produsele din baza de date.

Fluxul verificat este:

`React -> Axios -> ProductController -> ProductService -> ProductRepository -> MySQL -> JSON -> interfata client`

Pentru a verifica faptul ca datele provin din baza de date, disponibilitatea produsului `Papanasi` a fost modificata temporar in MySQL. Cand valoarea `available` a fost schimbata din `0` in `1`, produsul a aparut in pagina React dupa refresh. Dupa revenirea valorii la `0`, produsul a fost afisat din nou ca indisponibil. Aceasta verificare confirma ca frontend-ul foloseste date reale din backend, nu o lista hardcodata.

#### Ziua 13 din plan - Filtre simple pentru meniul client

Au fost adaugate filtre simple pe pagina de meniu client.

Filtre implementate:

- filtrare dupa categorie;
- filtrare dupa pret maxim;
- optiune pentru afisarea doar a produselor disponibile.

Pentru aceste filtre au fost adaugate state-uri React in `ClientMenuPage.jsx`:

- `selectedCategory`
- `maxPrice`
- `onlyAvailable`

A fost adaugata si logica `filteredProducts`, care filtreaza lista de produse primita din backend in functie de optiunile selectate de client.

Verificari manuale efectuate:

- categoria `Pizza` afiseaza doar Pizza Margherita si Pizza Diavola;
- categoria `Paste` afiseaza doar Paste Carbonara si Paste Bolognese;
- pretul maxim filtreaza produsele cu pret mai mic sau egal cu valoarea introdusa;
- bifarea optiunii pentru produse disponibile ascunde produsele indisponibile;
- debifarea optiunii permite afisarea produselor indisponibile, marcate corespunzator.

Aceasta etapa imbunatateste utilizarea meniului digital, deoarece clientul poate gasi mai rapid produsele dorite.

#### Ziua 14 din plan - Filtre tematice pentru client

In aceasta etapa meniul clientului a fost extins cu filtre tematice utile pentru alegerea produselor in functie de preferinte sau restrictii alimentare.

In entitatea `Product.java` au fost adaugate campuri noi:

- `allergens`
- `vegetarian`
- `vegan`
- `meatType`

Dupa repornirea backend-ului, Hibernate a actualizat automat tabela `products`, adaugand coloanele:

- `allergens`
- `vegetarian`
- `vegan`
- `meat_type`

Datele demo din MySQL au fost actualizate pentru fiecare produs. De exemplu:

- Pizza Margherita: `gluten,lactoza`, vegetarian, fara carne;
- Pizza Diavola: `gluten,lactoza`, carne de porc;
- Paste Carbonara: `gluten,lactoza,oua`, carne de porc;
- Paste Bolognese: `gluten,lactoza`, carne de vita;
- Apa plata si Limonada: vegan, fara alergeni, fara carne;
- Tiramisu si Papanasi: produse vegetariene, dar nu vegane.

In frontend au fost adaugate filtre noi:

- fara gluten;
- fara lactoza;
- vegetarian;
- vegan;
- tip carne.

Pentru filtrul `Tip carne`, optiunile folosite au fost:

- toate;
- fara carne;
- porc;
- vita.

Optiunea pentru pui nu a fost pastrata in interfata, deoarece in datele demo actuale nu exista niciun produs cu `meat_type = pui`.

Filtrarea a fost realizata in `ClientMenuPage.jsx`, pe baza datelor primite din backend.

Verificari manuale efectuate:

- filtrul `Fara gluten` elimina produsele care contin gluten;
- filtrul `Fara lactoza` elimina produsele care contin lactoza;
- filtrul `Vegetarian` afiseaza produsele vegetariene;
- filtrul `Vegan` afiseaza doar produsele vegane;
- filtrul `Tip carne = porc` afiseaza Pizza Diavola si Paste Carbonara;
- filtrul `Tip carne = vita` afiseaza Paste Bolognese;
- filtrul `Fara carne` afiseaza produsele cu `meat_type = none`.

Aceasta etapa demonstreaza ca meniul digital nu este doar o lista de produse, ci ajuta clientul sa ia o decizie mai rapida si mai potrivita.

#### Ziua 15 din plan - Backend pentru feedback anonim

A fost implementata structura backend pentru feedback anonim.

Au fost create fisierele:

- `Feedback.java`
- `FeedbackRepository.java`
- `FeedbackService.java`

Entitatea `Feedback` contine urmatoarele campuri:

- `id`
- `rating`
- `comment`
- `createdAt`

A fost adaugata metoda `@PrePersist`, astfel incat data si ora feedback-ului sa fie completate automat la salvare.

Repository-ul `FeedbackRepository` extinde `JpaRepository`, iar `FeedbackService` contine metode pentru:

- salvarea unui feedback;
- citirea tuturor feedback-urilor.

Dupa repornirea backend-ului, Spring Boot a detectat 7 repository-uri JPA, iar Hibernate a creat automat tabela:

- `feedback`

Tabela a fost verificata in MySQL Workbench cu:

`DESCRIBE feedback`

Coloanele create au fost:

- `id`
- `comment`
- `created_at`
- `rating`

#### Ziua 16 din plan - Endpoint-uri REST pentru feedback

A fost creat controller-ul:

- `FeedbackController.java`

Acesta expune endpoint-uri REST pentru feedback:

- `POST /api/feedback`
- `GET /api/feedback`

Endpoint-ul `POST /api/feedback` permite salvarea unui feedback anonim.

Endpoint-ul `GET /api/feedback` permite citirea feedback-urilor salvate, fiind util pentru verificare si pentru o viitoare interfata de manager.

Verificarea manuala a endpoint-ului `POST /api/feedback` a fost facuta din PowerShell cu `Invoke-RestMethod`, trimitand un body JSON cu rating si comentariu.

Exemplu de date trimise:

```json
{
  "rating": 5,
  "comment": "Meniul digital este usor de folosit."
}
```

Rezultatul a confirmat ca feedback-ul a fost salvat si ca backend-ul a returnat obiectul creat, cu `id` si `createdAt`.

Endpoint-ul `GET /api/feedback` a fost verificat in browser:

`http://localhost:8080/api/feedback`

Feedback-ul salvat a fost afisat in format JSON.

In MySQL Workbench a fost rulata comanda:

```sql
SELECT * FROM feedback;
```

Aceasta a confirmat existenta feedback-ului in baza de date.

#### Ziua 17 din plan - Formular frontend pentru feedback anonim

In aceasta etapa a fost adaugat in frontend un formular pentru feedback anonim.

In fisierul `productApi.js` a fost adaugata functia:

- `saveFeedback(feedback)`

Aceasta trimite datele catre endpoint-ul:

`POST http://localhost:8080/api/feedback`

In `ClientMenuPage.jsx` au fost adaugate state-uri pentru:

- rating;
- comentariu;
- mesaj de confirmare.

A fost implementata functia:

- `handleFeedbackSubmit(event)`

Aceasta opreste comportamentul implicit al formularului, creeaza obiectul de feedback si il trimite catre backend prin `saveFeedback`.

In pagina React a fost adaugata sectiunea:

- `Feedback anonim`

Formularul permite clientului sa aleaga un rating intre 1 si 5 si sa scrie un comentariu optional.

Dupa trimiterea formularului, pagina afiseaza mesajul:

`Feedback-ul a fost trimis cu succes.`

Verificarea manuala a confirmat ca feedback-ul trimis din formularul React apare in tabela `feedback` din MySQL. Astfel, fluxul complet este functional:

`React formular feedback -> Axios POST /api/feedback -> FeedbackController -> FeedbackService -> FeedbackRepository -> MySQL`

#### Observatie privind testarea

Singurul test automat adaugat pana in acest punct este `ProductServiceTest.java`, realizat in ziua 9 din plan. Celelalte validari mentionate pentru frontend si feedback sunt verificari manuale/functionale realizate in browser, PowerShell si MySQL Workbench.

#### Concluzie ziua 5

In ziua 5 au fost finalizate activitatile corespunzatoare zilelor 11-17 din plan.

A fost realizata partea principala pentru client:

- pagina de meniu digital;
- conectarea frontend-ului la backend;
- afisarea produselor reale din MySQL;
- filtrare dupa categorie, pret si disponibilitate;
- filtrare dupa alergeni, vegetarian, vegan si tip carne;
- feedback anonim salvat in baza de date.

Dupa aceasta etapa, partea de client este functionala la nivel de baza si poate fi demonstrata complet: clientul consulta meniul, filtreaza produsele si poate trimite feedback anonim.

Status: finalizat.

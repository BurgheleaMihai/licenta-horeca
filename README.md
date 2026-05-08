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

In aceasta etapa meniul clientului a fost extins cu filtre tematice utile pentru alegerea produselor in functie de preferinte alimentare si tipul de carne.

In entitatea `Product.java` au fost adaugate campuri noi:

- `vegetarian`
- `vegan`
- `meatType`

Dupa repornirea backend-ului, Hibernate a actualizat automat tabela `products`, adaugand coloanele:

- `vegetarian`
- `vegan`
- `meat_type`

Datele demo din MySQL au fost actualizate pentru fiecare produs. De exemplu:

- Pizza Margherita: vegetarian, fara carne;
- Pizza Diavola: carne de porc;
- Paste Carbonara: carne de porc;
- Paste Bolognese: carne de vita;
- Apa plata si Limonada: produse vegane, fara carne;
- Tiramisu si Papanasi: produse vegetariene, dar nu vegane.

In frontend au fost adaugate filtre noi:

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

- filtrul `Vegetarian` afiseaza produsele vegetariene;
- filtrul `Vegan` afiseaza doar produsele vegane;
- filtrul `Tip carne = porc` afiseaza Pizza Diavola si Paste Carbonara;
- filtrul `Tip carne = vita` afiseaza Paste Bolognese;
- filtrul `Fara carne` afiseaza produsele cu `meat_type = none`.

Aceasta etapa demonstreaza ca meniul digital nu este doar o lista de produse, ci ajuta clientul sa aleaga mai rapid produsele in functie de preferinte alimentare si tipul de carne.

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
- filtrare dupa vegetarian, vegan si tip carne;
- feedback anonim salvat in baza de date.

Dupa aceasta etapa, partea de client este functionala la nivel de baza si poate fi demonstrata complet: clientul consulta meniul, filtreaza produsele si poate trimite feedback anonim.

Status: finalizat.

--- 

---

### Ziua 6 - Flux backend pentru comenzi si teste automate

In ziua 6 au fost realizate activitatile corespunzatoare zilelor 18-24 din planul de lucru al licentei. Accentul a fost pus pe implementarea fluxului backend pentru comenzi, testarea acestuia si pregatirea legaturii cu interfata care va fi folosita ulterior de ospatar.

#### Ziua 18 din plan - Citirea codului de sesiune din link

In pagina clientului a fost adaugata citirea codului de sesiune din URL. Aceasta pregateste aplicatia pentru fluxul viitor bazat pe cod QR.

Exemplu de link:

`http://localhost:5173/?session=ABC123`

In `ClientMenuPage.jsx` a fost adaugata logica pentru citirea parametrului `session` din link:

```javascript
const params = new URLSearchParams(window.location.search);
const sessionCode = params.get("session");
```

Daca linkul contine un cod de sesiune, acesta este afisat in pagina clientului. Daca linkul nu contine cod de sesiune, pagina afiseaza un mesaj de avertizare.

Aceasta etapa nu implementeaza inca generarea codului QR, ci doar pregateste interfata clientului pentru momentul in care linkul va fi generat pentru fiecare masa.

#### Ziua 19 din plan - Entitati pentru comenzi

Au fost create entitatile necesare pentru gestionarea comenzilor:

- `Order.java`
- `OrderItem.java`
- `OrderStatus.java`

Entitatea `Order` reprezinta comanda principala si contine:

- `id`
- `createdAt`
- `status`
- `totalPrice`
- lista de produse comandate

Entitatea `OrderItem` reprezinta un produs inclus intr-o comanda si contine:

- `id`
- `quantity`
- `unitPrice`
- `subtotal`
- legatura cu `Order`
- legatura cu `Product`

Enum-ul `OrderStatus` contine statusurile posibile ale unei comenzi:

- `NEW`
- `IN_PREPARATION`
- `READY`
- `SERVED`
- `CANCELLED`

Dupa repornirea backend-ului, Hibernate a creat automat in MySQL tabelele:

- `orders`
- `order_items`

Au fost create si cheile externe:

- `order_items.order_id -> orders.id`
- `order_items.product_id -> products.id`

#### Ziua 20 din plan - Repository-uri pentru comenzi

Au fost create repository-urile:

- `OrderRepository.java`
- `OrderItemRepository.java`

`OrderRepository` extinde `JpaRepository` si permite accesul la comenzile salvate in baza de date.

`OrderItemRepository` extinde `JpaRepository` si permite accesul la produsele incluse in comenzi.

Dupa adaugarea repository-urilor, Spring Boot a detectat 9 repository-uri JPA, fata de 7 anterior. Acest lucru a confirmat ca noile repository-uri au fost incarcate corect de aplicatie.

#### Ziua 21 din plan - Service pentru comenzi

A fost creat fisierul:

- `OrderService.java`

Acesta contine logica de business pentru comenzi.

Functionalitati implementate in `OrderService`:

- crearea unei comenzi;
- cautarea produselor dupa id;
- verificarea existentei produselor;
- verificarea disponibilitatii produselor;
- calcularea subtotalului pentru fiecare produs;
- calcularea totalului comenzii;
- setarea statusului initial `NEW`;
- salvarea comenzii in baza de date;
- citirea tuturor comenzilor;
- schimbarea statusului unei comenzi.

Pentru trimiterea produselor comandate a fost folosita clasa interna:

- `OrderItemRequest`

Aceasta contine:

- `productId`
- `quantity`

#### Ziua 22 din plan - Controller pentru comenzi

A fost creat fisierul:

- `OrderController.java`

Acesta expune endpoint-urile REST pentru comenzi.

Endpoint-uri implementate:

```http
POST /api/orders
GET /api/orders
PATCH /api/orders/{orderId}/status
```

`POST /api/orders` permite crearea unei comenzi noi.

`GET /api/orders` permite citirea comenzilor salvate.

`PATCH /api/orders/{orderId}/status` permite schimbarea statusului unei comenzi.

Pentru evitarea unei bucle infinite la transformarea obiectelor in JSON, in `OrderItem.java` a fost adaugat `@JsonIgnore` pe legatura inapoi catre `Order`.

#### Ziua 23 din plan - Schimbarea statusului comenzii

A fost implementata schimbarea statusului unei comenzi prin endpoint-ul:

```http
PATCH /api/orders/{orderId}/status
```

Exemplu de testare:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/1/status?status=READY" -Method Patch
```

Prin acest endpoint, statusul unei comenzi poate fi schimbat din `NEW` in alte stari, de exemplu:

- `IN_PREPARATION`
- `READY`
- `SERVED`
- `CANCELLED`

Aceasta functionalitate pregateste proiectul pentru etapele urmatoare, unde vor fi implementate interfetele pentru ospatar si bucatarie.

#### Ziua 24 din plan - Testarea comenzii backend

Fluxul de comanda a fost testat mai intai din PowerShell.

Exemplu de request trimis catre backend:

```powershell
$body = @{
    items = @(
        @{
            productId = 1
            quantity = 2
        }
    )
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $body -ContentType "application/json"
```

Rezultatul a confirmat crearea unei comenzi cu:

- `status = NEW`
- `quantity = 2`
- `unitPrice = 32.00`
- `subtotal = 64.00`
- `totalPrice = 64.00`

Apoi statusul comenzii a fost schimbat la `READY` si verificat in browser la endpoint-ul:

`http://localhost:8080/api/orders`

In MySQL Workbench au fost verificate tabelele:

```sql
SELECT * FROM orders;
SELECT * FROM order_items;
```

Verificarea a confirmat ca datele au fost salvate corect in tabelele `orders` si `order_items`.

#### Verificare temporara din interfata React

Pentru a testa mai usor fluxul tehnic de creare comanda, a fost adaugat temporar un cos de comanda in pagina React.

Aceasta integrare a fost folosita pentru verificare functionala, dar in arhitectura finala comanda va fi creata din interfata ospatarului, nu din pagina clientului.

In `productApi.js` a fost adaugata functia:

```javascript
export const createOrder = (order) => {
  return axios.post(ORDERS_URL, order);
};
```

In `ClientMenuPage.jsx` au fost adaugate temporar:

- `cartItems`
- `orderMessage`
- `handleAddToCart(product)`
- `handleRemoveFromCart(productId)`
- `handleQuantityChange(productId, quantity)`
- `handleSubmitOrder()`
- calculul `cartTotal`

Fluxul verificat a fost:

`React -> createOrder() -> POST /api/orders -> OrderController -> OrderService -> OrderRepository -> MySQL`

Prin aceasta verificare s-a confirmat ca backend-ul de comenzi functioneaza si poate fi reutilizat ulterior in interfata ospatarului.

#### Verificare comanda in MySQL

A fost trimisa o comanda de test din interfata React. Comanda a continut mai multe produse, iar totalul a fost calculat corect.

Exemplu verificat:

- Paste Bolognese: `36.00 lei`
- Tiramisu: `22.00 lei`
- Limonada: `15.00 lei`

Total:

`36.00 + 22.00 + 15.00 = 73.00 lei`

In tabela `orders` a aparut comanda cu:

- `id = 2`
- `status = NEW`
- `total_price = 73.00`

In tabela `order_items` au aparut produsele asociate comenzii, fiecare cu `quantity`, `unit_price`, `subtotal`, `order_id` si `product_id`.

A fost testata si o alta comanda, cu totalul:

`38.00 + 8.00 = 46.00 lei`

In tabela `orders`, aceasta a aparut cu:

- `id = 3`
- `status = NEW`
- `total_price = 46.00`

Aceste verificari confirma ca suma subtotalurilor din `order_items` este egala cu `total_price` din `orders`.

#### Teste automate adaugate

In aceasta etapa au fost adaugate mai multe teste automate pentru zonele critice ale proiectului.

Teste existente si noi:

- `ProductServiceTest.java` -> 1 test
- `OrderServiceTest.java` -> 3 teste
- `FeedbackServiceTest.java` -> 2 teste
- `FeedbackControllerTest.java` -> 2 teste
- `OrderControllerTest.java` -> 2 teste

Total teste automate utile:

`10 teste`

Zone acoperite de teste:

- listarea produselor disponibile;
- salvarea feedback-ului anonim;
- listarea feedback-urilor;
- testarea endpoint-ului `POST /api/feedback`;
- testarea endpoint-ului `GET /api/feedback`;
- crearea unei comenzi cu total corect;
- respingerea produselor indisponibile;
- schimbarea statusului unei comenzi;
- testarea endpoint-ului `POST /api/orders`;
- testarea endpoint-ului `PATCH /api/orders/{orderId}/status`.

Rezultatele testelor:

- `OrderServiceTest` -> 3/3 teste trecute;
- `FeedbackServiceTest` -> 2/2 teste trecute;
- `FeedbackControllerTest` -> 2/2 teste trecute;
- `OrderControllerTest` -> 2/2 teste trecute;
- `ProductServiceTest` -> 1/1 test trecut.

#### Concluzie ziua 6

In ziua 6 au fost finalizate activitatile corespunzatoare zilelor 18-24 din plan.

A fost implementat fluxul backend de baza pentru comenzi:

- citirea codului de sesiune din link;
- entitati pentru comenzi;
- repository-uri pentru comenzi;
- service pentru creare comanda si calcul total;
- controller REST pentru comenzi;
- schimbarea statusului comenzii;
- salvarea comenzilor in MySQL;
- verificarea comenzilor in `orders` si `order_items`;
- testare temporara din interfata React;
- 10 teste automate utile pentru produse, feedback si comenzi.

In arhitectura finala, pagina clientului ramane folosita pentru consultarea meniului, filtrarea produselor si feedback anonim, iar fluxul de creare a comenzii va fi mutat in interfata ospatarului.

Status: finalizat.

---

### Ziua 7 - Modul ospatar, bucatarie, bar si actualizare teste automate

In ziua 7 au fost continuate functionalitatile pentru fluxul operational al restaurantului. Accentul a fost pus pe separarea interfetelor pentru ospatar, bucatarie si bar, pe legarea comenzilor de mesele restaurantului si pe actualizarea testelor automate dupa modificarea logicii comenzilor.

#### Pagina pentru ospatar

A fost creata pagina:

- `WaiterPage.jsx`

Aceasta este disponibila pe ruta:

`http://localhost:5173/waiter`

Pagina ospatarului incarca date reale din backend si afiseaza:

- mesele restaurantului;
- comenzile active;
- masa asociata fiecarei comenzi;
- statusul general al comenzii;
- produsele din comanda;
- statusul fiecarui produs;
- totalul comenzii.

Pentru incarcarea meselor a fost creat fisierul:

- `tableApi.js`

Acesta foloseste endpoint-urile pentru mese si permite afisarea meselor in interfata ospatarului.

Pentru comenzi a fost folosit fisierul:

- `orderApi.js`

Acesta contine functii pentru apelarea endpoint-urilor de comenzi.

Au fost implementate actiuni pentru ospatar:

- trimiterea unei comenzi la preparare;
- marcarea unei comenzi gata ca servita.

Fluxul verificat pentru ospatar este:

`NOUA -> IN_PREPARARE -> GATA -> SERVITA`

In pagina ospatarului produsele sunt grupate in:

- preparate;
- bauturi.

Astfel, ospatarul poate vedea comanda completa, dar si statusul fiecarei parti a comenzii.

#### Legarea comenzilor de mese

A fost adaugata legatura dintre comanda si sesiunea mesei.

In entitatea `Order.java` a fost adaugata legatura cu:

- `TableSession`

Astfel, o comanda poate fi asociata cu o sesiune de masa, iar prin sesiune se poate identifica masa de la care provine comanda.

In baza de date a fost adaugata coloana:

- `table_session_id`

in tabela:

- `orders`

A fost creata o sesiune de test pentru masa 1, cu codul:

- `TEST123`

Comanda creata prin aceasta sesiune a fost verificata in MySQL si in interfata ospatarului. In pagina ospatarului, comanda a fost afisata cu masa corespunzatoare.

#### Separarea intre bucatarie si bar

Initial, pagina de bucatarie afisa toate produsele aflate in preparare. Ulterior, logica a fost extinsa pentru a separa produsele in functie de categorie:

- bucatarie pentru preparate;
- bar pentru bauturi.

Au fost create paginile:

- `KitchenPage.jsx`
- `BarPage.jsx`

Rutele folosite sunt:

- `http://localhost:5173/kitchen`
- `http://localhost:5173/bar`

Pagina de bucatarie afiseaza doar produsele care nu fac parte din categoria `Bauturi`.

Pagina de bar afiseaza doar produsele din categoria `Bauturi`.

Pentru bucatarie si bar au fost adaugate endpoint-uri dedicate:

```http
GET /api/orders/kitchen
GET /api/orders/bar
```

Ambele endpoint-uri returneaza comenzile aflate in starea `IN_PREPARARE`, iar filtrarea produselor este facuta in frontend in functie de categoria produsului.

#### Status separat pentru produsele din comanda

Pentru a permite gestionarea separata a preparatelor si bauturilor, a fost adaugat status separat pentru fiecare produs din comanda.

In entitatea `OrderItem.java` a fost adaugat campul:

- `status`

Acesta foloseste acelasi enum:

- `OrderStatus`

Astfel, fiecare produs dintr-o comanda poate avea status propriu.

Exemplu:

- Pizza Margherita -> `GATA`
- Apa plata -> `IN_PREPARARE`
- Comanda generala -> `IN_PREPARARE`

Comanda generala devine `GATA` doar dupa ce toate produsele din comanda au statusul `GATA`.

Aceasta regula a fost implementata in `OrderService.java`, prin metoda care actualizeaza statusul unui produs din comanda.

A fost adaugat endpoint-ul:

```http
PUT /api/orders/items/{itemId}/status
```

Acesta permite actualizarea statusului unui singur produs din comanda.

#### Flux final intre ospatar, bucatarie si bar

Fluxul final verificat este:

1. Clientul intra in meniu printr-un link care contine codul de sesiune.
2. Comanda este asociata cu sesiunea mesei.
3. Ospatarul vede comanda noua in panoul sau.
4. Ospatarul trimite comanda la preparare.
5. Bucataria vede doar preparatele.
6. Barul vede doar bauturile.
7. Bucataria marcheaza preparatele ca gata.
8. Barul marcheaza bauturile ca gata.
9. Comanda generala devine `GATA` doar dupa ce toate produsele sunt gata.
10. Ospatarul marcheaza comanda ca `SERVITA`.

Fluxul rezultat este:

`NOUA -> IN_PREPARARE -> GATA -> SERVITA`

La nivel de produse, fluxul este gestionat separat prin `OrderItem.status`.

#### Afisare FCFS pentru bucatarie si bar

Pentru bucatarie si bar, comenzile sunt afisate dupa principiul FCFS.

FCFS inseamna ca prima comanda intrata in sistem este prima afisata in lista.

Pentru aceasta, in `OrderRepository.java` au fost adaugate metode de citire ordonate dupa `createdAt`.

Au fost folosite metode de forma:

- `findByStatusOrderByCreatedAtAsc(...)`
- `findByStatusInOrderByCreatedAtAsc(...)`

Astfel, comenzile sunt returnate din backend in ordinea in care au fost create.

In interfata pentru bucatarie si bar nu mai este afisat id-ul real al comenzii din baza de date. In schimb, comenzile sunt afisate cu o numerotare simpla in interfata:

- Comanda 1
- Comanda 2
- Comanda 3

Aceasta numerotare este folosita doar vizual si nu modifica id-urile reale din baza de date.

Pentru ospatar, pagina pastreaza informatia despre masa, deoarece ospatarul trebuie sa stie unde trebuie dusa comanda.

#### Actualizare teste automate

Dupa modificarile facute asupra comenzilor, au fost actualizate si testele automate.

A fost actualizat `OrderServiceTest.java`, astfel incat sa acopere noua logica:

- crearea unei comenzi cu `sessionCode`;
- calcularea corecta a totalului;
- setarea statusului initial `NOUA` pentru comanda;
- setarea statusului initial `NOUA` pentru fiecare `OrderItem`;
- respingerea unui produs indisponibil;
- respingerea unei sesiuni invalide;
- schimbarea statusului comenzii;
- trecerea unei comenzi in `IN_PREPARARE`;
- trecerea item-urilor in `IN_PREPARARE`;
- marcarea unui singur item ca `GATA`;
- pastrarea comenzii in `IN_PREPARARE` daca nu toate item-urile sunt gata;
- trecerea comenzii in `GATA` doar cand toate item-urile sunt `GATA`;
- citirea comenzilor active in ordine FCFS;
- citirea comenzilor pentru bucatarie in ordine FCFS;
- citirea comenzilor pentru bar in ordine FCFS.

Rezultat:

- `OrderServiceTest` -> 10/10 teste trecute.

A fost actualizat si `OrderControllerTest.java`, astfel incat sa acopere endpoint-urile folosite de ospatar, bucatarie si bar.

Au fost testate:

- `POST /api/orders`
- `PUT /api/orders/{orderId}/status`
- `GET /api/orders/active`
- `GET /api/orders/kitchen`
- `GET /api/orders/bar`
- `PUT /api/orders/items/{itemId}/status`

Rezultat:

- `OrderControllerTest` -> 6/6 teste trecute.

Dupa rularea tuturor testelor proiectului, rezultatul final a fost:

- 26/26 teste trecute.

#### Concluzie ziua 7

#### Concluzie ziua 7

In ziua 7 a fost extins si actualizat fluxul de gestionare a comenzilor, prin introducerea paginilor pentru ospatar, bucatarie si bar.

Au fost realizate urmatoarele:
- actualizarea paginii pentru ospatar;
- crearea paginii pentru bucatarie;
- crearea paginii pentru bar;
- legarea comenzilor de sesiunea mesei;
- afisarea mesei in panoul ospatarului;
- separarea preparatelor de bauturi;
- introducerea unui status separat pentru fiecare produs din comanda;
- implementarea regulii prin care o comanda devine `GATA` doar dupa ce toate produsele din comanda sunt gata;
- afisarea comenzilor in ordine FCFS;
- actualizarea testelor automate;
- rularea tuturor testelor proiectului cu rezultatul `26/26` teste trecute.

Dupa aceasta etapa, fluxul principal dintre ospatar, bucatarie si bar este functional. Ospatarul poate trimite comenzile la preparare, bucataria poate finaliza preparatele, barul poate finaliza bauturile, iar comanda devine gata doar dupa ce toate produsele au fost finalizate.

Status: finalizat.


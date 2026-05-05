# Documentation du module Revenue / Expense

## 1. Objectif du module

Le module `Revenue / Expense` gère :

- l'ajout, la modification, la suppression et la consultation des revenus
- l'ajout, la modification, la suppression et la consultation des dépenses
- l'association obligatoire d'une dépense à un revenu
- le filtrage des données par utilisateur
- l'affichage Back Office et Front Office
- le calcul du solde net côté interface Front

Dans le projet, ce module repose principalement sur :

- `pi.entities.Revenue`
- `pi.entities.Expense`
- `pi.services.RevenueExpenseService.RevenueService`
- `pi.services.RevenueExpenseService.ExpenseService`
- `pi.controllers.ExpenseRevenueController.BACK.*`
- `pi.controllers.ExpenseRevenueController.FRONT.SalaryExpenseController`

## 2. Structure technique

### Entités

#### Revenue
Représente une entrée d'argent.

Champs principaux :

- `id`
- `user`
- `amount`
- `type`
- `receivedAt`
- `description`
- `createdAt`

Fichier :
- [Revenue.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/entities/Revenue.java)

#### Expense
Représente une sortie d'argent.

Champs principaux :

- `id`
- `revenue`
- `user`
- `amount`
- `category`
- `expenseDate`
- `description`

Point important :
- chaque dépense référence un revenu via `revenue_id`

Fichier :
- [Expense.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/entities/Expense.java)

### Services

#### RevenueService
Service JDBC chargé des opérations CRUD sur la table `revenue`.

Méthodes exposées :

- `add(Revenue revenue)`
- `update(Revenue revenue)`
- `delete(int id)`
- `getById(int id)`
- `getAll()`

Règles de validation :

- le revenu ne peut pas être `null`
- un revenu doit référencer un utilisateur valide : `user.id > 0`

Fichier :
- [RevenueService.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/services/RevenueExpenseService/RevenueService.java)

#### ExpenseService
Service JDBC chargé des opérations CRUD sur la table `expense`.

Méthodes exposées :

- `add(Expense expense)`
- `update(Expense expense)`
- `delete(int id)`
- `getById(int id)`
- `getAll()`

Règles de validation :

- la dépense ne peut pas être `null`
- la dépense doit référencer un revenu valide : `revenue.id > 0`
- la dépense doit référencer un utilisateur valide : `user.id > 0`

Fichier :
- [ExpenseService.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/services/RevenueExpenseService/ExpenseService.java)

## 3. Tâches fonctionnelles couvertes

Le module couvre les tâches suivantes.

### Tâche 1 : gérer les revenus

Fonctionnalités :

- créer un revenu
- modifier un revenu existant
- supprimer un revenu
- afficher la liste des revenus
- rechercher un revenu
- trier les revenus par date, montant, type ou identifiant

Contrôleurs concernés :

- [RevenueBackController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/BACK/RevenueBackController.java)
- [SalaryExpenseController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/FRONT/SalaryExpenseController.java)

### Tâche 2 : gérer les dépenses

Fonctionnalités :

- créer une dépense
- modifier une dépense existante
- supprimer une dépense
- afficher la liste des dépenses
- rechercher une dépense
- trier les dépenses par date, montant, catégorie ou identifiant

Contrôleurs concernés :

- [ExpenseBackController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/BACK/ExpenseBackController.java)
- [SalaryExpenseController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/FRONT/SalaryExpenseController.java)

### Tâche 3 : lier chaque dépense à un revenu

Avant d'ajouter une dépense, l'utilisateur doit sélectionner un revenu associé.

Règle métier appliquée :

- si aucun revenu n'est sélectionné, l'opération est refusée
- si le montant de la dépense dépasse le montant du revenu sélectionné, l'opération est refusée

Cette logique existe dans :

- [ExpenseBackController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/BACK/ExpenseBackController.java)
- [SalaryExpenseController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/FRONT/SalaryExpenseController.java)

### Tâche 4 : suivre le solde utilisateur

Dans le Front Office, le module calcule :

- le total des revenus
- le total des dépenses
- le solde net = revenus - dépenses
- la date de la dernière transaction

Le contrôleur Front synchronise aussi le champ `soldeTotal` de l'utilisateur.

Fichier :
- [SalaryExpenseController.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/java/pi/controllers/ExpenseRevenueController/FRONT/SalaryExpenseController.java)

## 4. Interfaces utilisateur

### Back Office

Le Back Office est séparé en deux écrans :

- gestion des revenus
- gestion des dépenses

Fichiers FXML :

- [revenue-back-view.fxml](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/resources/Expense/Revenue/BACK/revenue-back-view.fxml)
- [expense-back-view.fxml](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/resources/Expense/Revenue/BACK/expense-back-view.fxml)

Styles :

- [salary-expense-back.css](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/resources/styles/RevenueExpenseBACK/salary-expense-back.css)

### Front Office

Le Front Office regroupe :

- un tableau de bord
- un panneau revenus
- un panneau dépenses

Fichier FXML :

- [salary-expense-view.fxml](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/resources/Expense/Revenue/FRONT/salary-expense-view.fxml)

Style :

- [salary-expense.css](/C:/Users/Yoga/IdeaProjects/pi_java/src/main/resources/styles/RevenueExpenseFRONT/salary-expense.css)

## 5. Flux de fonctionnement

### Flux d'ajout d'un revenu

1. L'utilisateur saisit le montant, le type, la date et la description.
2. Le contrôleur valide les données.
3. Le service `RevenueService` enregistre le revenu dans la table `revenue`.
4. L'identifiant généré est injecté dans l'objet `Revenue`.
5. La table d'affichage est rechargée.

### Flux d'ajout d'une dépense

1. L'utilisateur choisit un revenu associé.
2. Il saisit le montant, la catégorie, la date et la description.
3. Le contrôleur vérifie que la dépense ne dépasse pas le revenu choisi.
4. Le service `ExpenseService` enregistre la dépense dans la table `expense`.
5. La liste et les indicateurs sont mis à jour.

### Flux de modification

1. L'utilisateur double-clique sur une ligne du tableau.
2. Les données sont rechargées dans le formulaire.
3. Le bouton passe en mode `Update`.
4. Après validation, le service exécute un `UPDATE`.

### Flux de suppression

1. L'utilisateur clique sur `Delete`.
2. Le service supprime l'enregistrement correspondant.
3. L'interface recharge les données.

## 6. Règles métier observées dans le code

- un revenu doit appartenir à un utilisateur valide
- une dépense doit appartenir à un utilisateur valide
- une dépense doit référencer un revenu existant
- les montants doivent être strictement supérieurs à zéro
- les listes affichées sont filtrées sur l'utilisateur courant
- l'utilisateur courant est initialisé localement avec `id = 1` si aucun contexte utilisateur n'est injecté

## 7. Tests existants

Le module possède déjà des tests unitaires d'intégration JDBC pour les services.

### Tests Revenue

Cas couverts :

- ajout d'un revenu
- récupération par identifiant
- récupération de la liste
- modification
- suppression

Fichier :
- [RevenueServiceTest.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/test/java/ExpenseRevenueTest/RevenueServiceTest.java)

### Tests Expense

Cas couverts :

- ajout d'une dépense
- récupération par identifiant
- récupération de la liste
- modification
- suppression

Fichier :
- [ExpenseServiceTest.java](/C:/Users/Yoga/IdeaProjects/pi_java/src/test/java/ExpenseRevenueTest/ExpenseServiceTest.java)

## 8. Limites actuelles

Quelques limites visibles dans l'implémentation actuelle :

- les services `getAll()` chargent toutes les lignes puis le filtrage utilisateur se fait côté contrôleur
- l'utilisateur courant est parfois simulé par `id = 1`
- la contrainte "dépense <= revenu" est appliquée côté interface, pas dans le service JDBC
- les chaînes de catégories montrent des problèmes d'encodage sur certains accents

## 9. Recommandations

Pour améliorer ce module :

- ajouter des méthodes SQL filtrées par utilisateur
- déplacer les règles métier critiques dans les services
- centraliser les enums de type de revenu et catégorie de dépense
- corriger l'encodage des labels accentués
- ajouter des tests sur les contrôleurs et les validations métier


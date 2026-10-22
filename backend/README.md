# GymApp — backend

API REST Spring Boot 4 / Java 25 / PostgreSQL 16.

## Lancer en local (Windows)

Tout reste dans le dossier `GymApp` : JDK portable dans `..\.tools\jdk-25`,
Maven et dépendances dans `.\.m2`.

```bat
cd backend
mvnw-local test              :: tests unitaires et contrôleur
mvnw-local spring-boot:run   :: démarre l'API sur http://localhost:8080
```

Base de données attendue (modifiable via les variables d'environnement
`GYMAPP_DB_URL`, `GYMAPP_DB_USER`, `GYMAPP_DB_PASSWORD`) :
`jdbc:postgresql://localhost:5432/gymapp`, utilisateur `gymapp` / `gymapp`.
Le schéma est créé par Flyway au démarrage (`src/main/resources/db/migration`).

Test d'intégration sur la vraie base (annulé en fin de test, la base n'est pas modifiée) :

```bat
set GYMAPP_IT=true
mvnw-local test
```

## Authentification (provisoire)

En attendant le JWT, l'utilisateur courant est lu dans l'en-tête `X-User-Id`
(`DevHeaderCurrentUserProvider`). À remplacer avant tout déploiement.

## Module Exécution de séance — `/api/sessions`

| Méthode | Route | Rôle | Réponses |
| --- | --- | --- | --- |
| GET | `/api/sessions/next` | Prochaine séance de la rotation (dashboard) | 200, 409 `NO_ACTIVE_ROUTINE` |
| POST | `/api/sessions/start` | Lance la prochaine séance (ou `{"routineWorkoutId": n}`) en copiant les objectifs | 201, 409 `SESSION_ALREADY_IN_PROGRESS` |
| GET | `/api/sessions/current` | Séance en cours | 200, 204 |
| GET | `/api/sessions/{id}` | Détail d'une séance | 200, 404 |
| PUT | `/api/sessions/{id}/sets/{setId}` | Saisie d'une série `{"actualReps", "actualWeightKg", "completed"}` | 200, 400, 404, 409 `SESSION_NOT_IN_PROGRESS` |
| POST | `/api/sessions/{id}/complete` | Clôture la séance | 200, 404, 409 |

Les erreurs suivent la RFC 9457 (`application/problem+json`) avec un champ `code` stable.

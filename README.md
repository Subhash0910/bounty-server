# 🏴 BOUNTY

> **A real-time pirate bounty game** — sail the ocean, conquer islands, earn your bounty, and climb the leaderboard.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Backend API | Spring Boot 3, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT 0.11, HS256, 7-day expiry) |
| Database | PostgreSQL 15 (JPA auto-schema) |
| Combat Sessions | Redis 7 (10-min TTL per active combat) |
| Build | Maven 3.9, multi-stage Docker |
| Frontend | [bounty-client](https://github.com/Subhash0910/bounty-client) — React + Vite + Phaser 3 |

---

## 🚀 Local Setup (Docker Compose)

```bash
# 1. Clone the repo
git clone https://github.com/Subhash0910/bounty-server.git
cd bounty-server

# 2. Create your .env from the template
cp .env.example .env

# 3. Fill in your secrets in .env
#    JWT_SECRET  → generate with: openssl rand -hex 64
#    DB_PASSWORD → any strong password

# 4. Start all services (postgres + redis + app)
docker compose up --build

# Server will be available at http://localhost:8080
```

> **First run:** The app auto-seeds 10 starter islands on startup via `@PostConstruct` in `IslandService`.

---

## 📍 API Endpoints

All endpoints except `/api/auth/**` require `Authorization: Bearer <token>` header.

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ Open | Register a new player |
| `POST` | `/api/auth/login` | ❌ Open | Login — returns JWT token |
| `GET` | `/api/world/map` | ✅ JWT | All islands with owner handles |
| `GET` | `/api/islands/{id}` | ✅ JWT | Single island details + lore |
| `POST` | `/api/islands/{id}/claim` | ✅ JWT | Directly claim an island |
| `POST` | `/api/islands/{id}/sail` | ✅ JWT | Start a combat encounter |
| `POST` | `/api/encounter/turn` | ✅ JWT | Process one combat turn (`ATTACK` / `INTIMIDATE` / `NEGOTIATE`) |
| `GET` | `/api/encounter/history` | ✅ JWT | Last 10 encounters for the player |

---

## ⚔️ Combat Rules

| Approach | Effect |
|---|---|
| `ATTACK` | 15–25 dmg to enemy, 10–20 dmg back |
| `INTIMIDATE` | 30 morale dmg if player bounty > 10,000 — else 10; enemy doesn’t retaliate |
| `NEGOTIATE` | 40% → instant WIN at half reward; 60% → enemy hits for 25 |

- **WIN reward** = `island.bountyReward × (1 + islandsConquered × 0.1)`
- **LOSE penalty** = `−10% of current bounty`

---

## 🎨 Frontend

The game UI lives in **[bounty-client](https://github.com/Subhash0910/bounty-client)** — React + Vite + Phaser 3 browser game.

```bash
git clone https://github.com/Subhash0910/bounty-client.git
cd bounty-client
cp .env.example .env      # set VITE_API_URL=http://localhost:8080
npm install
npm run dev               # → http://localhost:3000
```

---

## 🗂️ Project Structure

```
src/main/java/com/bounty/
├── BountyApplication.java
├── config/
│   ├── SecurityConfig.java    # Spring Security + BCrypt + AuthManager
│   ├── JwtUtil.java           # Token generation + validation
│   └── JwtFilter.java         # OncePerRequestFilter — sets SecurityContext
├── model/
│   ├── Player.java            # JPA entity, UUID PK, bounty + tier
│   ├── Island.java            # JPA entity, type enum, position, lore
│   ├── Encounter.java         # JPA entity, WIN/LOSE outcome, aiLore
│   ├── CombatState.java       # Redis POJO — live combat session
│   ├── AuthRequest.java
│   └── AuthResponse.java
├── repository/
│   ├── PlayerRepository.java
│   ├── IslandRepository.java
│   └── EncounterRepository.java
├── service/
│   ├── PlayerService.java
│   ├── IslandService.java     # @PostConstruct seeds 10 islands
│   └── EncounterService.java  # Full combat engine with Redis TTL
└── controller/
    ├── AuthController.java
    ├── IslandController.java
    └── EncounterController.java
```

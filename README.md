# MMob (Fabric, Minecraft 26.3)

## Comandos (precisa ser operador / cheats ligados)

- `/mmob spawn <mob> <quantidade> reto` -> fila reta, para frente de onde voce olha
- `/mmob spawn <mob> <quantidade> lado` -> fila lado a lado, na sua frente
- `/mmob clear` -> remove todos os mobs criados pelo /mmob

Exemplos:
- `/mmob spawn zombie 10 reto`
- `/mmob spawn skeleton 10 lado`
- `/mmob spawn sheep 8 lado` e `/mmob spawn cow 8 lado`

## Regra de briga

Mobs criados pelo /mmob atacam mobs criados pelo /mmob de OUTRO tipo.
Mobs do mesmo tipo nunca se atacam (zumbi x zumbi, ovelha x ovelha).
Zumbi x esqueleto, ovelha x vaca, etc. brigam.
Os mobs nao despawnam e mantem a briga ao reabrir o mundo.

## Como compilar

1. Instale o JDK 25.
2. Abra https://fabricmc.net/develop , escolha Minecraft 26.3 e confira as versoes de
   Loom e Loader. Se forem diferentes, ajuste `loom_version` e `loader_version` no `gradle.properties`.
3. Se nao existir o `gradlew`, instale o Gradle e rode: `gradle wrapper`
4. Rode `gradlew build` (Windows) ou `./gradlew build` (Linux/Mac).
5. O mod fica em `build/libs/mmob-1.0.0.jar`.

## Como instalar

1. Instale o Fabric Loader para Minecraft 26.3.
2. Coloque o Fabric API 0.162.0+26.3 em `.minecraft/mods`.
3. Coloque o `mmob-1.0.0.jar` em `.minecraft/mods` (ou na pasta `mods` do servidor).

# Build na VPS-ie Minecraft

VPS ma Dockera, więc nie trzeba instalować Javy ani Gradle na hoście.

```bash
cd ~/LifestealCore
./build-vps.sh
```

Pierwsze uruchomienie pobiera oficjalny obraz `gradle:jdk25-noble` oraz zależności Maven.

Po udanym buildzie finalny plugin znajduje się tutaj:

```text
build/libs/LifestealCore-0.1.0.jar
```

Do pełnego deploymentu pluginu i ServerPacka użyj `./deploy.sh` zgodnie z
`DEPLOY_README.md`.

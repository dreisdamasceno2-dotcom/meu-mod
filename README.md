# Mod de melhorias (Fabric, Minecraft Java 26.3)

Requisitos: JDK 25, Gradle 9.6 (gere o wrapper: `gradle wrapper --gradle-version 9.6.0`).

    gradle build        # jar em build/libs/
    gradle runClient    # abre o jogo para testar

O id interno `melhorias` é só técnico (fabric.mod.json / gradle.properties); não é um nome de mod.

Configuração: `config/melhorias.json` (criada na primeira execução).

Onde precisa estar instalado:
- Aldeões e pesca: cliente E servidor (em mundo único funciona direto).
- Busca inteligente e barcos: somente no cliente.

## Portar para 26.3 (estado atual)
Build (Loom 1.17, Gradle 9.6, Loader 0.19.5, Fabric API 0.160.5+26.3, Java 25) e `Identifier` já ajustados.
O código dos mixins/HUD foi escrito para 1.21.1 e AINDA precisa ser conferido contra o 26.3:
`gradle genSources` e depois `gradle build`; os erros de compilação apontam exatamente o que mudou.

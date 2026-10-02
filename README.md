# Study Tracker Kotlin

Gerenciador de tarefas de estudo pelo terminal, feito em **Kotlin/JVM**. MVP para praticar tipos, null safety, coleções, persistência local, validação e testes.

## Funcionalidades

- Adicionar uma tarefa com título e duração planejada.
- Listar tarefas e marcar a conclusão.
- Resumir tarefas concluídas e minutos planejados.
- Salvar os dados em arquivo local, preservando títulos com acentos.
- Rejeitar entradas inválidas e arquivos corrompidos antes de alterá-los.

## Compilar e testar

Requisitos: **JDK 17+** e **Kotlin CLI 2.4.20** no PATH. O compilador está disponível em [JetBrains/Kotlin](https://github.com/JetBrains/kotlin/releases/tag/v2.4.20). Não há bibliotecas de terceiros nem configuração de Gradle nesta versão.

PowerShell:

```powershell
./build.ps1
# Ou informe o compilador:
./build.ps1 -Kotlinc 'C:/ferramentas/kotlinc/bin/kotlinc.bat'
```

Linux/macOS:

```sh
mkdir -p build
kotlinc src/main/kotlin/StudyTracker.kt -jvm-target 17 -include-runtime -d build/study-tracker.jar
kotlinc src/main/kotlin/StudyTracker.kt src/test/kotlin/StudyTrackerTest.kt -jvm-target 17 -include-runtime -d build/tests.jar
java -cp build/tests.jar studytracker.StudyTrackerTestKt
```

## Usar

```text
java -jar build/study-tracker.jar add "Revisar BFS em C++" 50
java -jar build/study-tracker.jar add "Praticar null safety" 30
java -jar build/study-tracker.jar list
java -jar build/study-tracker.jar done 1
java -jar build/study-tracker.jar stats
```

Saída do resumo após esses comandos:

```text
Tasks: 1/2 completed
Planned: 80 min
Completed planned time: 50 min
```

Por padrão, o arquivo fica em `data/tasks.tsv`, relativo à pasta atual. Para separar conjuntos de tarefas, use `--file CAMINHO` **antes do comando**:

```text
java -jar build/study-tracker.jar --file data/algoritmos.tsv add "Two Sum" 40
```

## Decisões e limites

- `StudyTask`: modelo validado e imutável; `copy` representa a mudança de estado.
- `StudyService`: cadastro e conclusão, sem dependência da interface de terminal.
- `TaskRepository`: leitura e gravação em UTF-8; títulos codificados em Base64 para evitar conflitos com o separador TSV. **Base64 não é criptografia.**
- Gravação em arquivo temporário, seguida de substituição atômica quando o sistema permite; há fallback de substituição simples.
- Aplicação **local e de usuário único**. Não execute múltiplos processos gravando no mesmo arquivo; não há bloqueio concorrente, sincronização ou login.
- Duração de 1 a 1440 minutos. O resumo soma estimativas das tarefas concluídas; não mede tempo real de estudo.
- Arquivos corrompidos causam erro; não são silenciosamente substituídos por uma lista vazia.
- Não há edição, exclusão, Android ou banco de dados nesta primeira versão.

Os testes usam arquivos temporários e verificam persistência, acentos, IDs, conclusão repetida, resumo, entradas inválidas e preservação de arquivos corrompidos. O GitHub Actions está configurado para compilar a aplicação e executar os testes.

## Aprendizado e autoria

Primeira versão criada com assistência de IA para apoiar o aprendizado de Kotlin. O projeto é um ponto de partida para estudar e modificar com entendimento; não representa experiência profissional ou domínio prévio da linguagem.

### Próximos exercícios

- [ ] Explicar `data class`, `copy`, `when`, `require` e tipos anuláveis.
- [ ] Implementar um filtro de tarefas pendentes com um teste de comportamento.
- [ ] Criar edição de título e estimativa, preservando o ID.
- [ ] Adicionar exportação CSV.
- [ ] Migrar a persistência para SQLite após estudar SQL e transações.

Versão inicial: 0.1.0.

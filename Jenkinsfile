// Pipeline de deploy da API.
//
// **Vive aqui, e não na interface do Jenkins, de propósito.** Pipeline
// configurado por cliques só existe na cabeça de quem clicou: não tem
// histórico, não passa por revisão, e some se o Jenkins for reinstalado.
//
// ─── O detalhe que decide tudo: Jenkins TAMBÉM está em container ───────────
//
// Ele fala com o daemon do host pelo socket montado, então:
//
//   - `docker build` roda no HOST. A imagem nasce lá, ao lado da API, e nunca
//     precisa de registry — é por isso que o Docker Hub saiu do caminho.
//   - Os caminhos de volume do compose são resolvidos pelo daemon, ou seja,
//     são caminhos do HOST. `/var/proauto/...` funciona.
//   - Mas o ARQUIVO do compose e o `.env` são lidos pelo CLI, que está DENTRO
//     do container do Jenkins. O compose vem do workspace; o `.env` mora em
//     `/root/api` no host e precisa estar montado aqui.
//
// Por isso este pipeline **não copia nada para `/root/api`** — a primeira
// versão fazia isso, e era um caminho que o Jenkins nem enxerga.

pipeline {
  agent any

  parameters {
    string(
      name: 'TAG',
      defaultValue: '',
      description: 'A tag do git a subir. Ex.: 2.42.4'
    )
  }

  options {
    // Um deploy de cada vez. Duas tags mexendo no mesmo compose ao mesmo tempo
    // deixam o container num estado que ninguém pediu.
    disableConcurrentBuilds()
    timestamps()
    timeout(time: 30, unit: 'MINUTES')
  }

  environment {
    APP     = 'proauto-api'
    COMPOSE = 'docker-compose.prod.yml'

    // O `.env` de produção, no host. Precisa estar montado no container do
    // Jenkins neste mesmo caminho — ver a verificação no primeiro estágio.
    ENV_PROD = '/root/api/.env'

    // **O nome do projeto é obrigatório, e não é enfeite.**
    //
    // O compose deriva o nome do diretório. A pilha que está no ar nasceu em
    // `/root/api`, então ela se chama `api`. Rodando do workspace do Jenkins,
    // o nome viraria o do workspace — e o compose tentaria criar uma pilha
    // NOVA em vez de substituir a que está rodando.
    PROJETO = 'api'

    // Quantas imagens antigas ficam em disco. Sem registry, a imagem antiga
    // guardada É o rollback — apagar todas deixaria a volta impossível.
    MANTER = '5'
  }

  stages {

    stage('Conferir o terreno') {
      steps {
        script {
          if (!params.TAG?.trim()) {
            error('Sem TAG não há o que subir. O GitHub manda o nome da tag.')
          }
          currentBuild.displayName = params.TAG
        }

        // Falhar aqui, com mensagem, é melhor que falhar três estágios adiante
        // com "no such file". Estes são os três pré-requisitos do ambiente.
        sh '''
          command -v docker >/dev/null 2>&1 || {
            echo "ERRO: o container do Jenkins nao tem o cliente Docker."
            echo "      A imagem oficial do Jenkins nao traz o docker CLI."
            exit 1
          }

          docker info >/dev/null 2>&1 || {
            echo "ERRO: nao alcanco o daemon do Docker."
            echo "      Falta montar /var/run/docker.sock, ou o usuario do"
            echo "      Jenkins nao tem permissao nele."
            exit 1
          }

          docker compose version >/dev/null 2>&1 || {
            echo "ERRO: falta o plugin compose v2 (docker compose, com espaco)."
            exit 1
          }

          [ -r "$ENV_PROD" ] || {
            echo "ERRO: nao consigo ler $ENV_PROD."
            echo "      Ele existe no HOST, mas o Jenkins esta em container e"
            echo "      precisa dele montado. No compose do Jenkins:"
            echo "        volumes:"
            echo "          - /root/api:/root/api:ro"
            exit 1
          }
        '''
      }
    }

    stage('Buscar a tag') {
      steps {
        // A tag, e não um branch: o que sobe é exatamente o commit que passou
        // nos testes. Buscar `main` aqui traria o que foi mergeado depois.
        checkout([
          $class: 'GitSCM',
          branches: [[name: "refs/tags/${params.TAG}"]],
          extensions: [[$class: 'CloneOption', shallow: true, depth: 1]],
          userRemoteConfigs: scm.userRemoteConfigs
        ])
      }
    }

    stage('Construir a imagem') {
      steps {
        // Roda no daemon do host: a imagem nasce ao lado da API.
        // Marcada com a tag do git, nunca `latest`.
        sh "docker build -t ${APP}:${params.TAG} ."
      }
    }

    stage('Subir') {
      steps {
        // Do workspace mesmo. O `-p` amarra na pilha que já está no ar, e o
        // `--env-file` aponta para o `.env` do host.
        //
        // `up -d` sem `down` antes: o compose troca o container e a janela sem
        // serviço é de segundos. `down` primeiro derrubaria a API durante todo
        // o start-up, que passa de um minuto com Flyway.
        sh """
          TAG=${params.TAG} docker compose \
            -p ${PROJETO} \
            --env-file ${ENV_PROD} \
            -f ${COMPOSE} up -d
        """
      }
    }

    stage('Conferir se subiu') {
      steps {
        // **Pergunta direto a API, e nao ao `{{.State.Health.Status}}`.**
        //
        // A primeira versao lia o status do healthcheck do Docker, e isso
        // falhou de um jeito traicoeiro: quando o container nao tem
        // healthcheck registrado, o `inspect` devolve `<no value>` — que nao e
        // `healthy` nem e erro. O laco girava os 200s e desistia, o pipeline
        // revertia, e a API que estava NO AR voltava uma versao. Falso
        // negativo que desfaz deploy bom e pior que nao verificar nada.
        //
        // Perguntando por dentro do container, a resposta e sempre uma das
        // tres que interessam: a API respondeu UP, respondeu outra coisa (e
        // imprimimos o que veio), ou nem respondeu.
        sh '''
          resposta=""
          for i in $(seq 1 40); do
            if ! docker ps --format '{{.Names}}' | grep -qx proauto-api; then
              echo "O container proauto-api nao esta rodando."
              exit 1
            fi

            resposta=$(docker exec proauto-api \
              wget -qO- http://localhost:8080/actuator/health 2>&1 || true)

            case "$resposta" in
              *'"status":"UP"'*) echo "API no ar: $resposta"; exit 0 ;;
            esac

            sleep 5
          done

          echo "Passou de 200s sem responder UP."
          echo "Ultima resposta do /actuator/health: [$resposta]"
          echo "Resposta vazia costuma ser 401: o Spring Security esta"
          echo "bloqueando /actuator/health, e ele precisa ser publico."
          echo "--- ultimas linhas da API ---"
          docker logs --tail 40 proauto-api 2>&1 || true
          exit 1
        '''
      }
    }
  }

  post {
    failure {
      // Volta para a versão anterior que ainda está em disco.
      //
      // **Não é `docker compose down`.** Deixar a API fora do ar é pior que
      // deixar a versão antiga: o deploy falhou, mas o expediente continua.
      script {
        def anterior = sh(
          script: """
            docker images --format '{{.Tag}}' ${APP} \
              | grep -v '^${params.TAG}\$' | grep -v '^<none>\$' | head -1
          """,
          returnStdout: true
        ).trim()

        if (anterior) {
          echo "Deploy falhou. Voltando para ${anterior}."
          sh """
            TAG=${anterior} docker compose \
              -p ${PROJETO} \
              --env-file ${ENV_PROD} \
              -f ${COMPOSE} up -d
          """
        } else {
          echo 'Deploy falhou e NAO ha versao anterior em disco para voltar.'
          echo 'Na primeira execucao isso e esperado.'
        }
      }
    }

    success {
      // Limpa o excesso, mas guarda as últimas: sem registry, elas são o
      // único caminho de volta.
      sh """
        docker images --format '{{.Repository}}:{{.Tag}}' ${APP} \
          | grep -v ':<none>\$' | tail -n +\$((${MANTER} + 1)) \
          | xargs -r docker rmi || true
      """
      echo "No ar: ${params.TAG}"
    }
  }
}

// Pipeline de deploy da API.
//
// **Vive aqui, e não na interface do Jenkins, de propósito.** Pipeline
// configurado por cliques só existe na cabeça de quem clicou: não tem
// histórico, não passa por revisão, e some se o Jenkins for reinstalado. Como
// código, ele é revisável — que é o que faz este fluxo continuar funcionando
// quando entrar outra pessoa.
//
// O Jenkins roda na MESMA VPS da API, então a imagem nasce e morre aqui: não há
// registry, nem push, nem pull, nem credencial de Docker Hub.
//
// Quem dispara é o GitHub Actions, e só depois dos testes passarem.

pipeline {
  agent any

  parameters {
    string(
      name: 'TAG',
      defaultValue: '',
      description: 'A tag do git a subir. Ex.: v2.28.5'
    )
  }

  options {
    // Um deploy de cada vez. Duas tags seguidas mexendo no mesmo compose ao
    // mesmo tempo deixam o container num estado que ninguém pediu.
    disableConcurrentBuilds()
    timestamps()
    timeout(time: 30, unit: 'MINUTES')
  }

  environment {
    APP        = 'proauto-api'
    COMPOSE    = 'docker-compose.prod.yml'
    // Onde vivem o `.env` e os volumes. O `.env` NUNCA entra no repositório
    // nem nas credenciais do Jenkins: ele já está na máquina.
    DEPLOY_DIR = '/opt/proauto/api'
    // Quantas imagens antigas ficam em disco. Sem registry, a imagem antiga
    // guardada É o rollback — apagar todas deixaria a volta impossível.
    MANTER     = '5'
  }

  stages {

    stage('Conferir o pedido') {
      steps {
        script {
          if (!params.TAG?.trim()) {
            error('Sem TAG não há o que subir. O GitHub manda o nome da tag.')
          }
          currentBuild.displayName = params.TAG
        }
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
        // Marcada com a tag do git, nunca `latest`: é o que responde "o que
        // está rodando" e o que faz rollback ser um `up` com outro número.
        sh "docker build -t ${APP}:${params.TAG} ."
      }
    }

    stage('Subir') {
      steps {
        dir("${DEPLOY_DIR}") {
          // O compose vem do repositório, não da cópia solta que vivia na VPS.
          sh "cp ${WORKSPACE}/${COMPOSE} ./${COMPOSE}"

          // `up -d` sem `down` antes: o compose troca o container e a janela
          // sem serviço é de segundos. `down` primeiro derrubaria a API
          // durante todo o start-up, que passa de um minuto com Flyway.
          sh "TAG=${params.TAG} docker compose -f ${COMPOSE} up -d"
        }
      }
    }

    stage('Conferir se subiu') {
      steps {
        // O `healthcheck` do compose é quem sabe: aqui só esperamos ele dizer
        // `healthy`. Conferir a porta responder não bastaria — a API atende
        // antes de o banco estar pronto.
        sh '''
          for i in $(seq 1 40); do
            estado=$(docker inspect -f '{{.State.Health.Status}}' proauto-api 2>/dev/null || echo missing)
            [ "$estado" = "healthy" ] && echo "API no ar." && exit 0
            [ "$estado" = "missing" ] && echo "Container nem existe." && exit 1
            sleep 5
          done
          echo "Passou de 200s sem ficar saudável."
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
          dir("${DEPLOY_DIR}") {
            sh "TAG=${anterior} docker compose -f ${COMPOSE} up -d"
          }
        } else {
          echo 'Deploy falhou e NÃO há versão anterior em disco para voltar.'
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

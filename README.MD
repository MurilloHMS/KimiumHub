# Api Proauto Kimium

### Sobre

API RestFul desenvolvida em java com Spring Boot, parte do projeto KimiumHub, com objetivo de gerir e fornecer endpoints para aplicações clientes/proauto.

### Propósito

Apresentar os produtos, serviços e consultorias, disponibilizar meios de comunicação da empresa, facilitar controle de projetos internos e disponibilizar dados para os clientes.

### Problemas Resolvidos
Melhorado visibilidade da empresa, automações de relatórios internos, controles de produtos, veículos e documentos, automação de separação de holerites, envios de informativos e newsletters aos clientes e coleta de dados

## Tecnologias Utilizadas
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![Maven](https://img.shields.io/badge/Maven-3.9.11-red)
![Jasper Reports](https://img.shields.io/badge/Jasper%20Reports-7.0.3-blue)
![Postgre SQL](https://img.shields.io/badge/Postgre%20SQL-16-blue)
![Flyway](https://img.shields.io/badge/Flyway-11.7.2-yellow)

## Funcionalidades
- Endpoints REST para gerenciamento de Recursos
- Integração com banco PostgresSQL via Spring Data JPA
- Suporte a migrações de banco com flyway
- Segurança JWT e Roles
- Documentação Swagger/OpenApi

## Pré-requisitos
- Java 21 ou superior
- Maven 3.9+
- Docker & Docker Compose
- Git

## Build & Execução 

### Via Maven
```bash
mvn clean install
```

### Rodar a API
```bash
mvn spring-boot:run
```


## Documentação
[Swagger API](https://api.proautokimium.com.br/swagger-ui.html)

## Agradecimentos
Obrigado por acompanhar o projeto!
- Autor: [MurilloHMS](https://github.com/MurilloHMS)
- Projeto: KimiumHub - API Proauto Kimium
- Front End: [Proauto Kimium - Angular](https://github.com/MurilloHMS/proautokimium)
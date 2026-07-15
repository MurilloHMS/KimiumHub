.PHONY: install-libs build run deploy restart stop remove logs

CONTAINER=proauto-api:local
PORT=5007:5007
NAME=proauto-api
NETWORK=api_postgres-compose-network
VOLUME=/var/proauto/curriculos:/app/curriculos
IMAGE_VOLUME=/var/proauto/upload/images:/app/upload/images
ENV=.env

install-libs:
	mvn install:install-file -Dfile=libs/montserrat-font.jar -DgroupId=custom.fonts -DartifactId=montserrat -Dversion=1.0 -Dpackaging=jar

build:
	docker build -t $(CONTAINER) .

run:
	docker run -p $(PORT) --name $(NAME) --network $(NETWORK) --volume $(VOLUME) --volume $(IMAGE_VOLUME) --restart=always --env-file $(ENV) -d $(CONTAINER) --verbose

deploy: stop remove build run logs

restart:
	docker restart $(NAME)

stop:
	-docker stop $(NAME)

remove:
	-docker rm $(NAME)

logs:
	-docker logs -f $(NAME)
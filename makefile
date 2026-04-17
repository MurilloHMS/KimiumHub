CONTAINER=proauto-api:local
PORT=5007:5007
NAME=proauto-api
NETWORK=api_postgres-compose-network
VOLUME=/var/proauto/curriculos:/app/curriculos
ENV=.env

build:
	docker build -t $(CONTAINER) .

run:
	docker run -p $(PORT) --name $(NAME) --network $(NETWORK) --volume $(VOLUME) --restart=always --env-file $(ENV) -d $(CONTAINER) --verbose

deploy: stop remove build run logs

restart:
	docker restart $(NAME)

stop:
	-docker stop $(NAME)

remove:
	-docker rm $(NAME)

logs:
	-docker logs $(NAME) -f

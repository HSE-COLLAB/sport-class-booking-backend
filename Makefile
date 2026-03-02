up-local:
	docker compose -f docker-compose-local.yml up -d

down-local:
	docker compose -f docker-compose-local.yml down

down-local-clean:
	docker compose -f docker-compose-local.yml down -v

up-stand:
	docker compose -f docker-compose-stand.yml up -d --build

down-stand:
	docker compose -f docker-compose-stand.yml down
.PHONY: run build test checks download download-br download-lahman download-retrosheet parquet parquet2s3 parqlo

PARQLO_S3    := s3://parqlo
PARQLO_LOCAL := $(HOME)/Documents/d/github/parqlo/data
AWS_PROFILE  := personal
SLUG         := war

build:
	@mvn -q package -DskipTests

run: build
	@java -jar target/warrencromartie.jar

test:
	@mvn test

checks:
	@mvn spotless:check pmd:cpd-check

download: ## Prompt to choose which data sources to download
	@./download.sh

download-br: ## Download latest BR WAR CSVs (if >24h old)
	@java -jar target/warrencromartie.jar br

download-lahman: ## Download Lahman CSVs from SABR Box (if >30 days old)
	@java -jar target/warrencromartie.jar lahman

download-retrosheet: ## Download Retrosheet gamelogs + Chadwick register, generate position parquets
	@java -jar target/warrencromartie.jar retrosheet

parquet: build download-br download-lahman download-retrosheet ## Refresh all data sources and convert to Parquet
	@java --enable-native-access=ALL-UNNAMED -jar target/warrencromartie.jar parquet

parqlo: build ## Generate war.json samples from definitions/ and push to parqlo/domains
	@java -jar target/warrencromartie.jar parqlo $(PARQLO_LOCAL)/../domains/$(SLUG).json

parquet2s3: parquet ## Refresh all data, convert to Parquet, upload to S3
	@aws sts get-caller-identity --profile $(AWS_PROFILE) > /dev/null || \
	  (echo "ERROR: AWS profile '$(AWS_PROFILE)' is not authenticated. Run: aws sso login --profile $(AWS_PROFILE)"; exit 1)
	java -jar target/warrencromartie.jar parqlo $(PARQLO_LOCAL)/$(SLUG).json
	aws s3 sync $(PARQLO_LOCAL)/$(SLUG)      $(PARQLO_S3)/$(SLUG)      --profile $(AWS_PROFILE)
	aws s3 cp  $(PARQLO_LOCAL)/$(SLUG).json  $(PARQLO_S3)/$(SLUG).json --profile $(AWS_PROFILE)

parquet2local: parquet ## Refresh all data and write to local parqlo data root
	java -jar target/warrencromartie.jar parqlo $(PARQLO_LOCAL)/$(SLUG).json

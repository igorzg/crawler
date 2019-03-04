provider "aws" {
  region = "${var.region}"
}

module "app" {
  source = "./app"
  name = "Main"
  private_zone = "zone.io"
  region = "${var.region}"
  es-domain = "crawler"
  es-isntance-type = "t2.micro.elasticsearch"
  doc-cluster = "crawler"
  doc-instance-type = "db.r4.large"
  doc-instance-count = 1
  cidr_block = "10.20.0.0/20"
  ecr-tag = "sphere-api-crawlers:1.0-SNAPSHOT"
  ecr-github = "https://github.com/igorzg/crawler.git"
  azs = [
    "eu-west-1a",
    "eu-west-1b",
    "eu-west-1c"
  ]
}
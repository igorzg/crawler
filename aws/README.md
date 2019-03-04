# AWS Deployment includes:
* Vpc - Networking - (ipv4, ipv6, public & private subnets)
* Private dns zone
* Mongodb instance
* Elastic search domain
* Lambda cloud formation custom resource
* AWS CodeBuild
* AWS CloudWatch (Events, Logs)
* AWS CloudFormation


You can modify values in main.tf
```hcl-terraform
name = "Main"
private_zone = "zone.io"
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
```

## Deploy
```bash
terraform deploy -var-file=env.tfvars
```
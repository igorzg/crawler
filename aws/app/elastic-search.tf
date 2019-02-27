data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

resource "aws_iam_service_linked_role" "main" {
  aws_service_name = "es.amazonaws.com"
}


resource "aws_elasticsearch_domain" "main" {
  domain_name = "${var.es-domain}"
  elasticsearch_version = "6.3"

  cluster_config {
    instance_type = "${var.es-isntance-type}"
  }

  ebs_options{
      ebs_enabled = true
      volume_size = 10
  }

  vpc_options {
    subnet_ids = [
      "${aws_subnet.main-private-az-1a.id}",
      "${aws_subnet.main-private-az-1b.id}",
      "${aws_subnet.main-private-az-1c.id}"
    ]

    security_group_ids = ["${aws_security_group.main.id}"]
  }

  advanced_options {
    "rest.action.multi.allow_explicit_index" = "true"
  }

  access_policies = <<CONFIG
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "es:*",
            "Principal": "*",
            "Effect": "Allow",
            "Resource": "arn:aws:es:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:domain/${var.es-domain}/*"
        }
    ]
}
CONFIG

  snapshot_options {
    automated_snapshot_start_hour = 03
  }

  tags {
    Domain = "${var.es-domain}"
    Type = "Private"
  }

  depends_on = [
    "aws_vpc.main",
    "aws_iam_service_linked_role.main",
  ]
}
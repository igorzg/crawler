resource "aws_ecr_repository" "main-build" {
  name = "main-build"
}

resource "aws_cloudformation_stack" "main-stack" {
  name = "${var.name}-stack"
  template_body = "${file("cloudformation.yml")}"
  capabilities = ["CAPABILITY_IAM"]
  parameters = {
    EcrRepository = "${aws_ecr_repository.main-build.repository_url}"
    EcrTag = "${var.ecr-tag}"
    GithubLocation = "${var.ecr-github}"
    MainVpcId = "${aws_vpc.main.id}"
    MainSubnets = "${aws_subnet.main-private-az-1a.id},${aws_subnet.main-private-az-1b.id},${aws_subnet.main-private-az-1c.id}"
    MainSecurityGroupIds = "${aws_security_group.main.id}"
  }
  depends_on = [
    "aws_elasticsearch_domain.main",
    "aws_docdb_cluster.main",
    "aws_docdb_cluster_instance.main_instances"
  ]
}
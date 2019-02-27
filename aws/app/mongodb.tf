resource "aws_docdb_cluster" "main" {
  cluster_identifier      = "${var.doc-cluster}"
  engine                  = "docdb"
  master_username         = "crawleradmin"
  master_password         = "crawleradmin"
  backup_retention_period = 5
  preferred_backup_window = "03:00-05:00"
  availability_zones = "${var.azs}"
  db_subnet_group_name = "${aws_docdb_subnet_group.main-private.name}"
  db_cluster_parameter_group_name = "${aws_docdb_cluster_parameter_group.main.name}"
  vpc_security_group_ids = ["${aws_security_group.main.id}"]
  depends_on = ["aws_vpc.main"]
}

resource "aws_docdb_subnet_group" "main-private" {
  name       = "${lower(var.name)}-private"
  subnet_ids = [
    "${aws_subnet.main-private-az-1a.id}",
    "${aws_subnet.main-private-az-1b.id}",
    "${aws_subnet.main-private-az-1c.id}"
  ]

  tags = {
    Name = "${var.name}-Private"
    Type = "Private"
  }
}

resource "aws_docdb_subnet_group" "main-public" {
  name       = "${lower(var.name)}-public"
  subnet_ids = [
    "${aws_subnet.main-public-az-1a.id}",
    "${aws_subnet.main-public-az-1b.id}",
    "${aws_subnet.main-public-az-1c.id}"
  ]

  tags = {
    Name = "${var.name}-Public"
    Type = "Public"
  }
}

resource "aws_docdb_cluster_parameter_group" "main" {
  family      = "docdb3.6"
  name        = "${lower(var.name)}-parameters"
  parameter {
    name  = "tls"
    value = "enabled"
  }
}

resource "aws_docdb_cluster_instance" "main_instances" {
  count              = "${var.doc-instance-count}"
  identifier         = "${var.doc-cluster}-cluster-${count.index}"
  cluster_identifier = "${aws_docdb_cluster.main.id}"
  instance_class     = "${var.doc-instance-type}"
  depends_on = ["aws_docdb_cluster.main"]
}



resource "aws_route53_record" "doc-main" {
  zone_id = "${aws_route53_zone.main.zone_id}"
  name    = "doc"
  type    = "CNAME"
  ttl     = "5"
  records = ["${aws_docdb_cluster.main.endpoint}"]
  depends_on = ["aws_docdb_cluster.main"]
}
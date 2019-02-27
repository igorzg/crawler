resource "aws_docdb_cluster" "main" {
  cluster_identifier      = "${var.doc-cluster}"
  engine                  = "docdb"
  master_username         = "admin"
  master_password         = "admin"
  backup_retention_period = 5
  preferred_backup_window = "03:00-05:00"
  availability_zones = "${var.azs}"
  vpc_security_group_ids = ["${aws_security_group.main.id}"]
  depends_on = ["aws_vpc.main"]
}


resource "aws_docdb_cluster_instance" "main_instances" {
  count              = "${var.doc-instance-count}"
  identifier         = "${var.doc-cluster}-cluster-${count.index}"
  cluster_identifier = "${aws_docdb_cluster.main.id}"
  instance_class     = "${var.doc-instance-type}"
  depends_on = ["aws_docdb_cluster.main"]
}
resource "aws_route53_zone" "main" {
  name = "${var.private_zone}"
  vpc {
    vpc_id = "${aws_vpc.main.id}"
    vpc_region = "${var.region}"
  }
  lifecycle {
    ignore_changes = ["vpc"]
  }
  tags = {
    Name = "${var.name}"
  }
  depends_on = ["aws_vpc.main"]
}

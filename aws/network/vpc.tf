resource "aws_vpc" "main" {
  cidr_block       = "${var.cidr_block}"
  assign_generated_ipv6_cidr_block = true
  enable_dns_support = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.name}"
  }
}

resource "aws_vpn_gateway" "main" {
  vpc_id = "${aws_vpc.main.id}"

  tags = {
    Name = "${var.name}"
  }
  depends_on = ["aws_vpc.main"]
}

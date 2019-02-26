resource "aws_subnet" "main-public-az-1a" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${element(var.public_subnets, 0)}"
  ipv6_cidr_block = "${element(var.public_v6_subnets, 0)}"
  availability_zone = "${element(var.azs, 0)}"
  assign_ipv6_address_on_creation = true
  tags = {
    Name = "${var.name}-Public-1a"
    Type = "Public"
    AZ = "${element(var.azs, 0)}"
  }
}

resource "aws_subnet" "main-public-az-1b" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${element(var.public_subnets, 1)}"
  ipv6_cidr_block = "${element(var.public_v6_subnets, 1)}"
  availability_zone = "${element(var.azs, 1)}"
  assign_ipv6_address_on_creation = true
  tags = {
    Name = "${var.name}-Public-1b"
    Type = "Public"
    AZ = "${element(var.azs, 1)}"
  }
}


resource "aws_subnet" "main-public-az-1c" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${element(var.public_subnets, 2)}"
  ipv6_cidr_block = "${element(var.public_v6_subnets, 2)}"
  availability_zone = "${element(var.azs, 2)}"
  assign_ipv6_address_on_creation = true
  tags = {
    Name = "${var.name}-Public-1c"
    Type = "Public"
    AZ = "${element(var.azs, 2)}"
  }
}

resource "aws_subnet" "main-private-az-1a" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${element(var.private_subnets, 0)}"
  ipv6_cidr_block = "${element(var.private_v6_subnets, 0)}"
  availability_zone = "${element(var.azs, 0)}"
  assign_ipv6_address_on_creation = true
  tags = {
    Name = "${var.name}-Private-1a"
    Type = "Private"
    AZ = "${element(var.azs, 0)}"
  }
}

resource "aws_subnet" "main-private-az-1b" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${element(var.private_subnets, 1)}"
  ipv6_cidr_block = "${element(var.private_v6_subnets, 1)}"
  availability_zone = "${element(var.azs, 1)}"
  assign_ipv6_address_on_creation = true
  tags = {
    Name = "${var.name}-Private-1b"
    Type = "Private"
    AZ = "${element(var.azs, 1)}"
  }
}


resource "aws_subnet" "main-private-az-1c" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${element(var.private_subnets, 2)}"
  ipv6_cidr_block = "${element(var.private_v6_subnets, 2)}"
  availability_zone = "${element(var.azs, 2)}"
  assign_ipv6_address_on_creation = true
  tags = {
    Name = "${var.name}-Private-1c"
    Type = "Private"
    AZ = "${element(var.azs, 2)}"
  }
}
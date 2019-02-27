resource "aws_subnet" "main-public-az-1a" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${cidrsubnet(aws_vpc.main.cidr_block, 4, 0)}"
  ipv6_cidr_block = "${cidrsubnet(aws_vpc.main.ipv6_cidr_block, 8, 0)}"
  availability_zone = "${element(var.azs, 0)}"
  assign_ipv6_address_on_creation = true
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.name}-Public-1a"
    Type = "Public"
    AZ = "${element(var.azs, 0)}"
  }
}

resource "aws_subnet" "main-public-az-1b" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${cidrsubnet(aws_vpc.main.cidr_block, 4, 2)}"
  ipv6_cidr_block = "${cidrsubnet(aws_vpc.main.ipv6_cidr_block, 8, 2)}"
  availability_zone = "${element(var.azs, 1)}"
  assign_ipv6_address_on_creation = true
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.name}-Public-1b"
    Type = "Public"
    AZ = "${element(var.azs, 1)}"
  }
}


resource "aws_subnet" "main-public-az-1c" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${cidrsubnet(aws_vpc.main.cidr_block, 4, 4)}"
  ipv6_cidr_block = "${cidrsubnet(aws_vpc.main.ipv6_cidr_block, 8, 4)}"
  availability_zone = "${element(var.azs, 2)}"
  assign_ipv6_address_on_creation = true
  map_public_ip_on_launch = true
  tags = {
    Name = "${var.name}-Public-1c"
    Type = "Public"
    AZ = "${element(var.azs, 2)}"
  }
}

resource "aws_subnet" "main-private-az-1a" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${cidrsubnet(aws_vpc.main.cidr_block, 4, 1)}"
  ipv6_cidr_block = "${cidrsubnet(aws_vpc.main.ipv6_cidr_block, 8, 1)}"
  availability_zone = "${element(var.azs, 0)}"
  tags = {
    Name = "${var.name}-Private-1a"
    Type = "Private"
    AZ = "${element(var.azs, 0)}"
  }
}

resource "aws_subnet" "main-private-az-1b" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${cidrsubnet(aws_vpc.main.cidr_block, 4, 3)}"
  ipv6_cidr_block = "${cidrsubnet(aws_vpc.main.ipv6_cidr_block, 8, 3)}"
  availability_zone = "${element(var.azs, 1)}"
  tags = {
    Name = "${var.name}-Private-1b"
    Type = "Private"
    AZ = "${element(var.azs, 1)}"
  }
}


resource "aws_subnet" "main-private-az-1c" {
  vpc_id = "${aws_vpc.main.id}"
  cidr_block = "${cidrsubnet(aws_vpc.main.cidr_block, 4, 5)}"
  ipv6_cidr_block = "${cidrsubnet(aws_vpc.main.ipv6_cidr_block, 8, 5)}"
  availability_zone = "${element(var.azs, 2)}"
  tags = {
    Name = "${var.name}-Private-1c"
    Type = "Private"
    AZ = "${element(var.azs, 2)}"
  }
}
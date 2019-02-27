resource "aws_route_table" "public" {
  vpc_id = "${aws_vpc.main.id}"
  tags = {
    Name = "${var.name}-Public"
    Type = "Public"
  }
}

resource "aws_vpn_gateway_route_propagation" "public" {
  vpn_gateway_id = "${aws_vpn_gateway.main.id}"
  route_table_id = "${aws_route_table.public.id}"
}

resource "aws_route_table_association" "public-az-1a" {
  subnet_id      = "${aws_subnet.main-public-az-1a.id}"
  route_table_id = "${aws_route_table.public.id}"
}

resource "aws_route_table_association" "public-az-1b" {
  subnet_id      = "${aws_subnet.main-public-az-1b.id}"
  route_table_id = "${aws_route_table.public.id}"
}

resource "aws_route_table_association" "public-az-1c" {
  subnet_id      = "${aws_subnet.main-public-az-1c.id}"
  route_table_id = "${aws_route_table.public.id}"
}

resource "aws_route" "public" {
  route_table_id = "${aws_route_table.public.id}"
  destination_cidr_block = "0.0.0.0/0"
  destination_ipv6_cidr_block = "::/0"
  gateway_id = "${aws_internet_gateway.main.id}"
}

resource "aws_route_table" "private" {
  vpc_id = "${aws_vpc.main.id}"
  tags = {
    Name = "${var.name}-Private"
    Type = "Private"
  }
}

resource "aws_vpn_gateway_route_propagation" "private" {
  vpn_gateway_id = "${aws_vpn_gateway.main.id}"
  route_table_id = "${aws_route_table.private.id}"
}

resource "aws_route_table_association" "private-az-1a" {
  subnet_id      = "${aws_subnet.main-private-az-1a.id}"
  route_table_id = "${aws_route_table.private.id}"
}

resource "aws_route_table_association" "private-az-1b" {
  subnet_id      = "${aws_subnet.main-private-az-1b.id}"
  route_table_id = "${aws_route_table.private.id}"
}

resource "aws_route_table_association" "private-az-1c" {
  subnet_id      = "${aws_subnet.main-private-az-1c.id}"
  route_table_id = "${aws_route_table.private.id}"
}

resource "aws_route" "private-az-1a" {
  route_table_id = "${aws_route_table.private.id}"
  destination_cidr_block = "0.0.0.0/0"
  destination_ipv6_cidr_block = "::/0"
  nat_gateway_id = "${aws_nat_gateway.main-az-1a.id}"
}

resource "aws_route" "private-az-1b" {
  route_table_id = "${aws_route_table.private.id}"
  destination_cidr_block = "0.0.0.0/0"
  destination_ipv6_cidr_block = "::/0"
  nat_gateway_id = "${aws_nat_gateway.main-az-1b.id}"
}

resource "aws_route" "private-az-1c" {
  route_table_id = "${aws_route_table.private.id}"
  destination_cidr_block = "0.0.0.0/0"
  destination_ipv6_cidr_block = "::/0"
  nat_gateway_id = "${aws_nat_gateway.main-az-1c.id}"
}
data "aws_vpc_endpoint_service" "execute-api" {
  service = "execute-api"
}

resource "aws_vpc_endpoint" "execute-api" {
  vpc_endpoint_type = "Interface"
  vpc_id       = "${aws_vpc.main.id}"
  service_name = "${data.aws_vpc_endpoint_service.execute-api.service_name}"
  security_group_ids = ["${aws_security_group.main.id}"]
}

resource "aws_vpc_endpoint_subnet_association" "execute-api-private-az-1a" {
  vpc_endpoint_id = "${aws_vpc_endpoint.execute-api.id}"
  subnet_id       = "${aws_subnet.main-private-az-1a.id}"
}

resource "aws_vpc_endpoint_subnet_association" "execute-api-private-az-1b" {
  vpc_endpoint_id = "${aws_vpc_endpoint.execute-api.id}"
  subnet_id       = "${aws_subnet.main-private-az-1b.id}"
}

resource "aws_vpc_endpoint_subnet_association" "execute-api-private-az-1c" {
  vpc_endpoint_id = "${aws_vpc_endpoint.execute-api.id}"
  subnet_id       = "${aws_subnet.main-private-az-1c.id}"
}
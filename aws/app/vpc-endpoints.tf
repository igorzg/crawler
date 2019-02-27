data "aws_vpc_endpoint_service" "execute-api" {
  service = "execute-api"
}

resource "aws_vpc_endpoint" "execute-api" {
  vpc_endpoint_type = "Interface"
  vpc_id       = "${aws_vpc.main.id}"
  service_name = "${data.aws_vpc_endpoint_service.execute-api.service_name}"
}

resource "aws_vpc_endpoint_route_table_association" "private_execute-api" {
  vpc_endpoint_id = "${aws_vpc_endpoint.execute-api.id}"
  route_table_id  = "${aws_route_table.private.id}"
}

resource "aws_vpc_endpoint_route_table_association" "public_execute-api" {
  vpc_endpoint_id = "${aws_vpc_endpoint.execute-api.id}"
  route_table_id  = "${aws_route_table.public.id}"
}
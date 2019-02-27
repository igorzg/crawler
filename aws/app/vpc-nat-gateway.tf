resource "aws_nat_gateway" "main-az-1a" {
  allocation_id = "${element(aws_eip.main.*.id, 0)}"
  subnet_id     = "${aws_subnet.main-public-az-1a.id}"

  tags = {
    Name = "${var.name}-1a"
    AZ = "${element(var.azs, 0)}"
  }
  depends_on = ["aws_internet_gateway.main"]
}

resource "aws_nat_gateway" "main-az-1b" {
  allocation_id = "${element(aws_eip.main.*.id, 1)}"
  subnet_id     = "${aws_subnet.main-public-az-1b.id}"

  tags = {
    Name = "${var.name}-1b"
    AZ = "${element(var.azs, 1)}"
  }
  depends_on = ["aws_internet_gateway.main"]
}

resource "aws_nat_gateway" "main-az-1c" {
  allocation_id = "${element(aws_eip.main.*.id, 2)}"
  subnet_id     = "${aws_subnet.main-public-az-1c.id}"

  tags = {
    Name = "${var.name}-1c"
    AZ = "${element(var.azs, 2)}"
  }
  depends_on = ["aws_internet_gateway.main"]
}
resource "aws_security_group" "main" {
  description = "Default security group"
  vpc_id      = "${aws_vpc.main.id}"

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    from_port   = 27017
    to_port     = 27017
    protocol    = "-1"
    cidr_blocks = [
      "${aws_subnet.main-private-az-1a.cidr_block}",
      "${aws_subnet.main-private-az-1b.cidr_block}",
      "${aws_subnet.main-private-az-1c.cidr_block}",
      "${aws_subnet.main-public-az-1a.cidr_block}",
      "${aws_subnet.main-public-az-1b.cidr_block}",
      "${aws_subnet.main-public-az-1c.cidr_block}",
    ]
    ipv6_cidr_blocks = [
      "${aws_subnet.main-private-az-1a.ipv6_cidr_block}",
      "${aws_subnet.main-private-az-1b.ipv6_cidr_block}",
      "${aws_subnet.main-private-az-1c.ipv6_cidr_block}",
      "${aws_subnet.main-public-az-1a.ipv6_cidr_block}",
      "${aws_subnet.main-public-az-1b.ipv6_cidr_block}",
      "${aws_subnet.main-public-az-1c.ipv6_cidr_block}",
    ]
  }


  egress {
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    cidr_blocks     = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "${var.name}-Security-Group"
  }
}
resource "aws_eip" "main" {
  vpc = true
  count = 3
  depends_on = ["aws_internet_gateway.main"]
}
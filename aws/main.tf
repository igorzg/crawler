provider "aws" {
  region = "${var.region}"
}

module "network" {
  source = "./network"
  name = "Main"
  cidr_block = "10.20.0.0/23"
  public_subnets = [
    "10.20.0.0/24",
    "10.20.2.0/24",
    "10.20.4.0/24"
  ]
  public_v6_subnets = [
    "2001:db8:1234:1a00::/64",
    "2001:db8:1234:1a02::/64",
    "2001:db8:1234:1a04::/64"
  ]
  private_subnets = [
    "10.20.1.0/24",
    "10.20.3.0/24",
    "10.20.5.0/24"
  ]
  private_v6_subnets = [
    "2001:db8:1234:1a01::/64",
    "2001:db8:1234:1a03::/64",
    "2001:db8:1234:1a05::/64"
  ]
  azs = [
    "eu-central-1a",
    "eu-central-1b",
    "eu-central-1c"
  ]
}
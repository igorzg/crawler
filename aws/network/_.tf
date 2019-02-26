variable "name" {
  type = "string"
}
variable "cidr_block" {
  type = "string"
}
variable "public_subnets" {
  type = "list"
}
variable "public_v6_subnets" {
  type = "list"
}
variable "private_subnets" {
  type = "list"
}
variable "private_v6_subnets" {
  type = "list"
}
variable "azs" {
  type = "list"
}
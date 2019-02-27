variable "name" {
  type = "string"
}
variable "private_zone" {
  type = "string"
}
variable "region" {
  type = "string"
}
variable "es-domain" {
  type = "string"
}
variable "es-isntance-type" {
  type = "string"
}
variable "doc-cluster" {
  type = "string"
}
variable "doc-instance-type" {
  type = "string"
}
variable "doc-instance-count" {}
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
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
variable "azs" {
  type = "list"
}
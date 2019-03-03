data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]
    principals {
      type = "Service"
      identifiers = [
        "lambda.amazonaws.com",
        "codebuild.amazonaws.com",
        "logs.${data.aws_region.current.name}.amazonaws.com"
      ]
    }
    effect = "Allow"
  }
}

resource "aws_iam_role" "lambda-role" {
  name = "${var.name}-lambda-codebuild"
  assume_role_policy = "${data.aws_iam_policy_document.lambda_assume.json}"
}

resource "aws_iam_policy" "lambda-permissions" {
  name = "${var.name}-lambda-codebuild-permissions"
  description = "${var.name}-lambda-codebuild permissions policy"
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
          "ec2:Describe*",
          "ec2:CreateNetworkInterface",
          "ec2:DeleteNetworkInterface",
          "ec2:CreateNetworkInterfacePermission"
      ],
      "Effect": "Allow",
      "Resource": "*"
    },
    {
      "Action": [
        "logs:*"
      ],
      "Effect": "Allow",
      "Resource": "${aws_cloudwatch_log_group.lambda_logs.arn}"
    },
    {
      "Action": [
        "codebuild:List*",
        "codebuild:*Build"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:codebuild:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:project/${var.name}*"
    }
  ]
}
EOF

  depends_on = [
    "aws_cloudwatch_log_group.lambda_logs"
  ]
}

resource "aws_iam_role_policy_attachment" "lambda-permissions" {
  role = "${aws_iam_role.lambda-role.name}"
  policy_arn = "${aws_iam_policy.lambda-permissions.arn}"
}

data "archive_file" "function" {
  type = "zip"
  source_dir = "./lambda"
  output_path = "/tmp/lambda.zip"
}

resource "aws_cloudwatch_log_group" "lambda_logs" {
  name = "/aws/lambda/${var.name}-codebuild"
  retention_in_days = 14
}

resource "aws_lambda_function" "main-codebuild" {
  filename = "${data.archive_file.function.output_path}"
  function_name = "${var.name}-codebuild"
  role = "${aws_iam_role.lambda-role.arn}"
  handler = "codebuild.handler"
  source_code_hash = "${data.archive_file.function.output_base64sha256}"
  runtime = "nodejs8.10"
  timeout = 120
  memory_size = 128
  vpc_config {
    security_group_ids = ["${aws_security_group.main.id}"]
    subnet_ids = [
      "${aws_subnet.main-private-az-1a.id}",
      "${aws_subnet.main-private-az-1b.id}",
      "${aws_subnet.main-private-az-1c.id}"
    ]
  }
  depends_on = [
    "aws_cloudwatch_log_group.lambda_logs",
    "data.archive_file.function"
  ]
}
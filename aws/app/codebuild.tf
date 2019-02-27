resource "aws_ecr_repository" "main-build" {
  name = "main-build"
}


resource "aws_iam_role" "main-build" {
  name = "${var.name}-build"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": [
            "codebuild.amazonaws.com",
            "logs.${var.region}.amazonaws.com"
       ]
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "main-build" {
  role = "${aws_iam_role.main-build.name}"

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Resource": [
        "*"
      ],
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeDhcpOptions",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeVpcs"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:*Imag*"
      ],
      "Resource": "${aws_ecr_repository.main-build.arn}"
    }
  ]
}
POLICY
}

resource "aws_codebuild_project" "main-build" {
  name = "${var.name}-crawler"
  description = "Crawler build project"
  build_timeout = "5"
  service_role = "${aws_iam_role.main-build.arn}"

  artifacts {
    type = "NO_ARTIFACTS"
  }

  environment {
    compute_type = "BUILD_GENERAL1_SMALL"
    image = "aws/codebuild/java:openjdk-11"
    type = "LINUX_CONTAINER"

    environment_variable {
      "name" = "ECR_REPOSITORY"
      "value" = "${aws_ecr_repository.main-build.repository_url}"
    }

    environment_variable {
      "name" = "ECR_TAG"
      "value" = "${var.ecr-tag}"
    }
  }


  source {
    type = "GITHUB"
    location = "https://github.com/igorzg/crawler.git"
    git_clone_depth = 1
  }

  vpc_config {
    vpc_id = "${aws_vpc.main.id}"

    subnets = [
      "${aws_subnet.main-private-az-1a}",
      "${aws_subnet.main-private-az-1b}",
      "${aws_subnet.main-private-az-1c}"
    ]

    security_group_ids = [
      "${aws_security_group.main.id}"
    ]
  }

  tags = {
    Environment = "Prod"
    Name = "${var.name}"
  }

  depends_on = [
    "aws_elasticsearch_domain.main",
    "aws_docdb_cluster.main",
    "aws_docdb_cluster_instance.main_instances"
  ]
}
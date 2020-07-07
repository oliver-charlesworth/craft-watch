terraform {
  required_version = "~> 0.12.28"

  backend "gcs" {
    bucket      = "terraform.craft.watch"
    prefix      = "terraform/state"
  }
}

provider "tls" {
  version     = "~> 2.1.1"
}

provider "google" {
  version     = "~> 3.27"
  project     = "craft-watch"
  region      = "europe-west2"
}

provider "github" {
  version      = "~> 2.9.1"
  token        = trimspace(file(".credentials/github-token"))
  organization = "craft-watch"
}

resource "google_storage_bucket" "backend" {
  name          = "backend.craft.watch"
  location      = "europe-west2"
  force_destroy = true

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = 90
    }
  }
}

# TODO - rename
resource "google_service_account" "circleci" {
  account_id   = "circleci"
  display_name = "CircleCI service account"
}

resource "google_service_account_key" "circleci" {
  service_account_id = google_service_account.circleci.name
}

data "google_iam_policy" "admin" {
  binding {
    role = "roles/storage.objectAdmin"
    members = [
      "serviceAccount:${google_service_account.circleci.email}"
    ]
  }
  binding {
    role = "roles/storage.legacyBucketReader"
    members = [
      "serviceAccount:${google_service_account.circleci.email}"
    ]
  }
}

# Requires you to have "Storage Admin" role
resource "google_storage_bucket_iam_policy" "policy" {
  bucket = google_storage_bucket.backend.name
  policy_data = data.google_iam_policy.admin.policy_data
}

resource "tls_private_key" "github_deploy" {
  algorithm   = "RSA"
}

resource "github_repository_deploy_key" "deploy_key" {
  title      = "CI deploy key"
  repository = "craft-watch.github.io"
  key        = tls_private_key.github_deploy.public_key_openssh
  read_only  = false
}

resource "github_actions_secret" "gcloud_service_key" {
  repository       = "craft-watch"
  secret_name      = "GCLOUD_SERVICE_KEY"
  plaintext_value  = base64decode(google_service_account_key.circleci.private_key)
}

resource "github_actions_secret" "github_deploy_key" {
  repository       = "craft-watch"
  secret_name      = "DEPLOY_KEY"
  plaintext_value  = tls_private_key.github_deploy.private_key_pem
}

output "circleci_key" {
  value = base64decode(google_service_account_key.circleci.private_key)
}

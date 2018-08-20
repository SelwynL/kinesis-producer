package org.selwyn.kproducer.model

import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider

case class AWSCredentials(accessKeyId: String, secretKey: String) {
  private val isDefault = (s: String) => s == "default"
  private val isIam     = (s: String) => s == "iam"
  private val isEnv     = (s: String) => s == "env"
  private val isProfile = (s: String) => s == "profile"

  val provider: Either[Throwable, AWSCredentialsProvider] =
    ((accessKeyId, secretKey) match {
      case (a, s) if isDefault(a) && isDefault(s) =>
        Right(new DefaultAWSCredentialsProviderChain())
      case (a, s) if isDefault(a) || isDefault(s) =>
        Left("accessKey and secretKey must both be set to 'default' or neither")
      case (a, s) if isIam(a) && isIam(s) =>
        Right(InstanceProfileCredentialsProvider.getInstance())
      case (a, s) if isIam(a) || isIam(s) =>
        Left("accessKey and secretKey must both be set to 'iam' or neither")
      case (a, s) if isEnv(a) && isEnv(s) =>
        Right(new EnvironmentVariableCredentialsProvider())
      case (a, s) if isEnv(a) || isEnv(s) =>
        Left("accessKey and secretKey must both be set to 'env' or neither")
      case (a, s) if isProfile(a) =>
        Right(new ProfileCredentialsProvider(s))
      case _ =>
        Right(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey)))
    }).left.map((err: String) => new IllegalArgumentException(err))
}

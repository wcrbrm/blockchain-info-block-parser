package com.wcrbrm.blockparser
import io.circe._
import io.circe.generic.semiauto._

case class Server(
   id: Option[String],
   realmId: String = "cluster0",
   groupId: String = "production",
   name: String,
   ip: String,
   tags: Option[List[String]] = None,
   comments: Option[String] = None,
   state: Option[String] = None,
   auth_method: String = "password",
   auth_user: String = "root",
   auth_password: Option[String] = None,
   auth_privateKey: Option[String] = None
 )

object Server {
  implicit val serverDecoder: Decoder[Server] = deriveDecoder[Server]
  implicit val serverEncoder: Encoder[Server] = deriveEncoder[Server]
}


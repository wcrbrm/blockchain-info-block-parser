package com.wcrbrm.blockparser
import io.circe._
import io.circe.generic.semiauto._
import fr.janalyse.ssh.{SSH, SSHIdentity, SSHOptions}

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

  def getSession(server: Server): SSH = {
    val settings = if (server.auth_method == "password") {
      SSHOptions( host = server.ip, username = server.auth_user, password = server.auth_password )
    } else if (server.auth_privateKey.isDefined) {
      val identity = SSHIdentity(server.auth_privateKey.get)
      SSHOptions( host = server.ip, username = server.auth_user, identities = List(identity) )
    } else {
      throw new Exception(s"Cannot get session for ${server.ip}")
    }
    SSH(settings)
  }
}


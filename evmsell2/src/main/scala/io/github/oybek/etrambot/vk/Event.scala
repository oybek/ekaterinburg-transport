package io.github.oybek.etrambot.vk

import io.circe.{Decoder, DecodingFailure, HCursor}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.auto._
import org.http4s.DecodeFailure


sealed trait Event

// --
case class MessageNew(id: Long,
                      date: Long,
                      fromId: Long,
                      text: String,
                      geo: Option[Geo]) extends Event

case class Geo(coordinates: Coord)
case class Coord(latitude: Float, longitude: Float)

// --
case class WallPostNew(id: Long,
                       fromId: Long,
                       ownerId: Long,
                       date: Long,
                       text: String,
                       signerId: Long,
                       createdBy: Long,
                       geo: Option[Geo]) extends Event

object Event {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val decodeMessageNew: Decoder[MessageNew] = deriveDecoder[MessageNew]

  implicit val decodeGeo: Decoder[Geo] =
    (c: HCursor) =>
      for {
        coords <- c.downField("coordinates").as[Either[String, Coord]]
        res = coords match {
          case Left(s) =>
            val coords = s.split(' ')
            Geo(Coord(coords(0).toFloat, coords(1).toFloat))
          case Right(coord) => Geo(coord)
        }
      } yield res

  implicit val decodeEvent: Decoder[Event] =
    (c: HCursor) =>
      for {
        typee <- c.downField("type").as[String]
        res <- typee match {
          case "message_new" => c.downField("object").as[MessageNew]
          case "wall_post_new" => c.downField("object").as[WallPostNew]
          case eventType => Left(DecodingFailure(s"Unknown event type: $eventType", List()))
        }
      } yield res

  implicit def decodeEither[A,B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A,B]] = {
    val l: Decoder[Either[A,B]]= a.map(Left.apply)
    val r: Decoder[Either[A,B]]= b.map(Right.apply)
    l or r
  }
}
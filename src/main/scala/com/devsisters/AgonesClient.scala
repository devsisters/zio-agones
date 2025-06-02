package com.devsisters

import agones.dev.sdk.sdk
import agones.dev.sdk.sdk.ZioSdk.SDKClient
import io.grpc.ManagedChannelBuilder
import zio._
import zio.stream.ZStream

trait AgonesClient {
  def ready: Task[Unit]
  def allocate: Task[Unit]
  def shutdown: Task[Unit]
  def health(schedule: Schedule[Any, Any, Unit]): Task[Unit]
  def getGameServer: Task[sdk.GameServer]
  def watchGameServer: ZStream[Any, Throwable, sdk.GameServer]
  def setLabel(key: String, value: String): Task[Unit]
  def setAnnotation(key: String, value: String): Task[Unit]
  def reserve(duration: Duration): Task[Unit]
}

object AgonesClient {
  private case class AgonesClientLive(client: SDKClient) extends AgonesClient {
    val ready: Task[Unit]                                        = client.ready(sdk.Empty()).unit
    val allocate: Task[Unit]                                     = client.allocate(sdk.Empty()).unit
    val shutdown: Task[Unit]                                     = client.shutdown(sdk.Empty()).unit
    def health(schedule: Schedule[Any, Any, Unit]): Task[Unit]   =
      client.health(ZStream.fromSchedule(schedule).as(sdk.Empty())).unit
    val getGameServer: Task[sdk.GameServer]                      = client.getGameServer(sdk.Empty())
    val watchGameServer: ZStream[Any, Throwable, sdk.GameServer] = client.watchGameServer(sdk.Empty())
    def setLabel(key: String, value: String): Task[Unit]         = client.setLabel(sdk.KeyValue(key, value)).unit
    def setAnnotation(key: String, value: String): Task[Unit]    = client.setAnnotation(sdk.KeyValue(key, value)).unit
    def reserve(duration: Duration): Task[Unit]                  = client.reserve(sdk.Duration(duration.toSeconds)).unit
  }

  val live: ZLayer[Any, Throwable, AgonesClient] =
    ZLayer.scoped {
      for {
        host   <- System.env("AGONES_SDK_GRPC_HOST").someOrElse("localhost")
        port   <- System.env("AGONES_SDK_GRPC_PORT").map(_.flatMap(_.toIntOption)).someOrElse(9357)
        builder = ManagedChannelBuilder.forAddress(host, port).maxInboundMessageSize(128 * 1024 * 1024).usePlaintext()
        client <- SDKClient.scoped(scalapb.zio_grpc.ZManagedChannel(builder))
      } yield AgonesClientLive(client)
    }

  val noop: ZLayer[Any, Nothing, AgonesClient] =
    ZLayer.succeed(new AgonesClient {
      def ready: Task[Unit]                                        = ZIO.unit
      def allocate: Task[Unit]                                     = ZIO.unit
      def shutdown: Task[Unit]                                     = ZIO.unit
      def health(schedule: Schedule[Any, Any, Unit]): Task[Unit]   = ZIO.unit
      def getGameServer: Task[sdk.GameServer]                      = ZIO.succeed(sdk.GameServer.defaultInstance)
      def watchGameServer: ZStream[Any, Throwable, sdk.GameServer] = ZStream.empty
      def setLabel(key: String, value: String): Task[Unit]         = ZIO.unit
      def setAnnotation(key: String, value: String): Task[Unit]    = ZIO.unit
      def reserve(duration: zio.Duration): Task[Unit]              = ZIO.unit
    })
}

package com.devsisters

import agones.dev.sdk.sdk
import agones.dev.sdk.sdk.SDKGrpc
import agones.dev.sdk.sdk.SDKGrpc.SDK
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
  private case class AgonesClientLive(client: SDK) extends AgonesClient {
    private def await[A](future: => scala.concurrent.Future[A]): Task[A] =
      ZIO.fromFuture(_ => future)

    val ready: Task[Unit]                                        = await(client.ready(sdk.Empty())).unit
    val allocate: Task[Unit]                                     = await(client.allocate(sdk.Empty())).unit
    val shutdown: Task[Unit]                                     = await(client.shutdown(sdk.Empty())).unit
    def health(schedule: Schedule[Any, Any, Unit]): Task[Unit]   =
      for {
        response <- Promise.make[Throwable, sdk.Empty]
        requests <- ZIO.attempt {
                      client.health(
                        new io.grpc.stub.StreamObserver[sdk.Empty] {
                          def onNext(value: sdk.Empty): Unit  = Unsafe.unsafe { implicit unsafe =>
                            Runtime.default.unsafe.run(response.succeed(value)).getOrThrowFiberFailure()
                          }
                          def onError(error: Throwable): Unit = Unsafe.unsafe { implicit unsafe =>
                            Runtime.default.unsafe.run(response.fail(error)).getOrThrowFiberFailure()
                          }
                          def onCompleted(): Unit             = Unsafe.unsafe { implicit unsafe =>
                            Runtime.default.unsafe.run(response.succeed(sdk.Empty())).getOrThrowFiberFailure()
                          }
                        }
                      )
                    }
        sender   <- ZStream
                      .fromSchedule(schedule)
                      .runForeach(_ => ZIO.attempt(requests.onNext(sdk.Empty())))
                      .ensuring(ZIO.attempt(requests.onCompleted()).ignore)
                      .fork
        _        <- response.await
        _        <- sender.interrupt
      } yield ()
    val getGameServer: Task[sdk.GameServer]                      = await(client.getGameServer(sdk.Empty()))
    val watchGameServer: ZStream[Any, Throwable, sdk.GameServer] =
      ZStream.async[Any, Throwable, sdk.GameServer] { emit =>
        client.watchGameServer(
          sdk.Empty(),
          new io.grpc.stub.StreamObserver[sdk.GameServer] {
            def onNext(value: sdk.GameServer): Unit = emit(ZIO.succeed(zio.Chunk.single(value)))
            def onError(error: Throwable): Unit     = emit(ZIO.fail(Some(error)))
            def onCompleted(): Unit                 = emit(ZIO.fail(None))
          }
        )
      }
    def setLabel(key: String, value: String): Task[Unit]         = await(client.setLabel(sdk.KeyValue(key, value))).unit
    def setAnnotation(key: String, value: String): Task[Unit]    = await(
      client.setAnnotation(sdk.KeyValue(key, value))
    ).unit
    def reserve(duration: Duration): Task[Unit]                  = await(client.reserve(sdk.Duration(duration.toSeconds))).unit
  }

  val live: ZLayer[Any, Throwable, AgonesClient] =
    ZLayer.scoped {
      for {
        host   <- System.env("AGONES_SDK_GRPC_HOST").someOrElse("localhost")
        port   <- System.env("AGONES_SDK_GRPC_PORT").map(_.flatMap(_.toIntOption)).someOrElse(9357)
        channel = ManagedChannelBuilder
                    .forAddress(host, port)
                    .maxInboundMessageSize(128 * 1024 * 1024)
                    .usePlaintext()
                    .build()
        _      <- ZIO.addFinalizer(ZIO.succeed(channel.shutdown()).unit)
        client  = SDKGrpc.stub(channel)
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

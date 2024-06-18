# zio-agones

[![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

[Link-SonatypeReleases]: https://s01.oss.sonatype.org/content/repositories/releases/com/devsisters/zio-agones_2.13/ "Sonatype Releases"
[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/s01.oss.sonatype.org/com.devsisters/zio-agones_2.13.svg "Sonatype Releases"
[Link-SonatypeSnapshots]: https://s01.oss.sonatype.org/content/repositories/snapshots/com/devsisters/zio-agones_2.13/ "Sonatype Snapshots"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.devsisters/zio-agones_2.13.svg "Sonatype Snapshots"

**zio-agones** is a lightweight Scala client for [Agones SDK](https://agones.dev/site/docs/guides/client-sdks/) using ZIO.

Sample usage:

```scala
import com.devsisters.AgonesClient
import zio.*

for {
  agones <- ZIO.service[AgonesClient]
  _      <- agones.ready
  _      <- agones.health(Schedule.fixed(5.seconds).unit).ignore.fork
  _      <- agones.getGameServer.debug
  _      <- agones.shutdown
} yield ()
```

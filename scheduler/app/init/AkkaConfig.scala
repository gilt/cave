package init

import com.typesafe.config.{ConfigValueFactory, ConfigFactory}
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConversions._

class AkkaConfig(hostname: Option[String], awsWrapper: AwsWrapper) {

  private final val Log = LogFactory.getLog(this.getClass)
  private final val AkkaPort = 2551
  private final val Localhost = "localhost"

  private val (host: String, siblings: List[String]) = hostname match {
    case Some(name) if name.length > 0 => (name, awsWrapper.getNodes("scheduler"))
    case _ => (Localhost, List(Localhost))
  }

  private val seeds = siblings map (ip => s"akka.tcp://scheduler@$ip:$AkkaPort")
  Log.warn(s"Seeds: ${seeds.mkString}")

  private val overrideConfig = ConfigFactory.empty()
    .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(host))
    .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(AkkaPort))
    .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(seeds))

  private val defaults = ConfigFactory.load()

  val config = overrideConfig withFallback defaults
}

package init

import actors.{Coordinator, Leadership}
import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.cave.metrics.data.postgresql.PostgresCacheDataManagerImpl
import com.cave.metrics.data.{AwsConfig, CacheDataManager}
import com.typesafe.config.ConfigFactory
import play.api.Play

object Init {

  private[this] val configuration = Play.current.configuration

  val serviceConfFile = configuration.getString("serviceConf").getOrElse("scheduler.conf")

  val config = ConfigFactory.load(serviceConfFile)
  val clusterHost = sys.props.get("akka.remote.netty.tcp.hostname")

  val appConfig = config.getConfig("scheduler")

  // prepare AWS config
  val awsConfig = new AwsConfig(appConfig)

  // a wrapper for required AWS
  val awsWrapper = new AwsWrapper(awsConfig)

  // a connection to the persistence backend
  val dataManager: CacheDataManager = new PostgresCacheDataManagerImpl(awsConfig)

  val system = ActorSystem("scheduler", new AkkaConfig(clusterHost, awsWrapper).config)
  val localAddress = Cluster(system).selfAddress

  val coordinator = system.actorOf(Props(new Coordinator(awsWrapper, dataManager)), "coordinator")
  val leader = system.actorOf(Props(new Leadership(localAddress)), name = "broadcast")

  def init() {
    println("Init.init()")
  }

  def shutdown() {
    awsWrapper.deleteQueue()
    system.shutdown()
  }

}
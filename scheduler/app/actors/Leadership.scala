package actors

import akka.actor.{Actor, ActorLogging, Address}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member}

object Leadership {
  object IsLeader
}

class Leadership(address: Address) extends Actor with ActorLogging {

  private val cluster = Cluster(context.system)
  private var members = Set.empty[Member]

  private var isLeader = false

  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember],
      classOf[ClusterDomainEvent])

  override def postStop(): Unit = cluster.unsubscribe(self)

  import actors.Leadership._

  def receive = {

    case IsLeader =>
      sender ! isLeader

    case state: CurrentClusterState =>
      log.warning("Initial state: " + state.leader)
      setLeader(state.leader)

    case MemberUp(member) =>
      log.warning(s"Member up($member)")
      members += member

    case MemberRemoved(member, previousStatus) =>
      log.warning(s"Member removed($member)")
      members.find(_.address == member.address) foreach (members -= _)

    case LeaderChanged(member) =>
      log.warning("Leader changed, now: " + member)
      setLeader(member)

    case e: MemberEvent =>
      log.warning(s"Member event($e)")
  }

  private def setLeader(leader: Option[Address]): Unit = {
    isLeader = leader exists (_ == address)
  }
}

package worker

import akka.actor.{Terminated, Actor, ActorRef, Props}

/**
 * Utility actor for testing actors that send messages to their parent
 *
 * This actor becomes the parent of the actor under test, receiving
 * all its messages, which it forwards to the probe
 *
 * @param child  the child actor we want to test
 * @param probe  the probe that we use in the test
 */
class StepParent(child: Props, probe: ActorRef) extends Actor {
  context.watch(context.actorOf(child, "child"))
  override def receive = {
    case Terminated(_) => context.stop(self)
    case message => probe.tell(message, sender)
  }
}

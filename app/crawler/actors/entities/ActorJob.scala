package crawler.actors.entities

import akka.actor.ActorRef

case class ActorJob(actor: ActorRef, stats: ActorRef, current: Task)

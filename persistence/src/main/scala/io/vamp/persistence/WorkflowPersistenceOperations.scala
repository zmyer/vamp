package io.vamp.persistence

import akka.actor.Actor
import io.vamp.common.notification.NotificationProvider
import io.vamp.model.artifact._

trait WorkflowPersistenceMessages {

  case class UpdateWorkflowBreed(workflow: Workflow, breed: Breed) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowStatus(workflow: Workflow, status: Workflow.Status) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowScale(workflow: Workflow, scale: DefaultScale) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowNetwork(workflow: Workflow, network: String) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowArguments(workflow: Workflow, arguments: List[Argument]) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowEnvironmentVariables(workflow: Workflow, environmentVariables: List[EnvironmentVariable]) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowInstances(workflow: Workflow, instances: List[Instance]) extends PersistenceActor.PersistenceMessages

  case class UpdateWorkflowHealth(workflow: Workflow, health: Option[Health]) extends PersistenceActor.PersistenceMessages

  case class ResetWorkflow(workflow: Workflow) extends PersistenceActor.PersistenceMessages

}

trait WorkflowPersistenceOperations extends PersistenceApi {
  this: NotificationProvider with PatchPersistenceOperations ⇒

  import PersistenceActor._

  def receive: Actor.Receive = {

    case o: UpdateWorkflowBreed                ⇒ patch(o.workflow.name, w ⇒ w.copy(breed = o.breed))

    case o: UpdateWorkflowScale                ⇒ patch(o.workflow.name, w ⇒ w.copy(scale = Option(o.scale)))

    case o: UpdateWorkflowNetwork              ⇒ patch(o.workflow.name, w ⇒ w.copy(network = Option(o.network)))

    case o: UpdateWorkflowArguments            ⇒ patch(o.workflow.name, w ⇒ w.copy(arguments = o.arguments))

    case o: UpdateWorkflowEnvironmentVariables ⇒ patch(o.workflow.name, w ⇒ w.copy(environmentVariables = o.environmentVariables))

    case o: UpdateWorkflowInstances            ⇒ patch(o.workflow.name, w ⇒ w.copy(instances = o.instances))

    case o: UpdateWorkflowStatus               ⇒ updateWorkflowStatus(o.workflow, o.status)

    case o: UpdateWorkflowHealth               ⇒ patch(o.workflow.name, w ⇒ w.copy(health = o.health))

    case o: ResetWorkflow                      ⇒ resetWorkflow(o.workflow)
  }

  private def updateWorkflowStatus(workflow: Workflow, status: Workflow.Status): Unit = {
    patch(workflow.name, w ⇒ w.copy(status = status), (w, m) ⇒ replyUpdate(w, s"workflow-statuses:${workflow.name}", status.describe, m))
  }

  private def resetWorkflow(workflow: Workflow): Unit = {
    patch(workflow.name, w ⇒ {
      w.copy(
        instances = Nil,
        health = None,
        environmentVariables = w.environmentVariables.map(_.copy(interpolated = None))
      )
    })
  }

  private def patch(name: String, using: Workflow ⇒ Workflow): Unit = patch(name, using, (w, m) ⇒ replyUpdate(w, m))

  private def patch(name: String, using: Workflow ⇒ Workflow, update: (Workflow, Boolean) ⇒ Unit): Unit = {
    get(name, classOf[Workflow]) match {
      case Some(w) ⇒
        val nw = using(w)
        val modified = nw != w
        update(nw, modified)
      case None ⇒ replyNone()
    }
  }
}

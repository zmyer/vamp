package io.vamp.container_driver.kubernetes

import io.kubernetes.client.models.{ V1Namespace, V1ObjectMeta }
import io.vamp.common.akka.CommonActorLogging

import scala.util.Try

case class CreateNamespace(name: String)

trait KubernetesNamespace extends KubernetesArtifact {
  this: KubernetesContainerDriver with CommonActorLogging ⇒

  protected def createNamespace(ns: CreateNamespace): Unit = {
    Try(k8sClient.coreV1Api.readNamespaceStatus(ns.name, null)).toOption match {
      case Some(_) ⇒ log.debug(s"Namespace exists: ${ns.name}")
      case None ⇒
        log.info(s"Creating namespace: ${ns.name}")
        val request = new V1Namespace
        val metadata = new V1ObjectMeta
        request.setMetadata(metadata)
        metadata.setName(ns.name)
        k8sClient.coreV1Api.createNamespace(request, null)
    }
  }
}

package crawler.actors.entities

import play.api.libs.ws.WSResponse

case class ClientResponse(response : WSResponse, size : Int, tasks : Seq[Task])

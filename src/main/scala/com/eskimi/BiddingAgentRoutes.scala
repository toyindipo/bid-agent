package com.eskimi

import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.Future



class BiddingAgentRoutes(campaignRegistry: ActorRef[MakeBid])(implicit val system: ActorSystem[_]) {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private implicit val timeout = Timeout(25, TimeUnit.SECONDS)

  def makeBid(bidRequest: BidRequest): Future[Option[BidResponse]] =
    campaignRegistry.ask(MakeBid(bidRequest, _))



  val agentRoutes: Route =
    pathPrefix("bid") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[BidRequest]) { bidRequest =>
                onSuccess(makeBid(bidRequest)) { response =>
                  (response) match {
                    case Some(bidResponse) =>
                      complete(StatusCodes.OK, bidResponse)
                    case _ =>
                      complete(StatusCodes.NoContent)
                  }
                }
              }
            })
        })
    }

}

package com.eskimi

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BiddingAgentRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val biddingAgent = testKit.spawn(BiddingAgent(BiddingAgent.sampleCampaigns()))
  lazy val routes = new BiddingAgentRoutes(biddingAgent).agentRoutes


  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "AgentRoutes" should {
    "return no content status with no matching bid amount" in {
      val bidRequest = BidRequest("1", Some(List(Impression("1", h = Some(250), w = Some(300), bidFloor = Some(10d),
        hmin = None,hmax = None, wmin = None, wmax = None))), Site("0006a522ce0f4bbbbaa6b3c38cafaa0f", "site.td"),
        Some(User("1", Some(Geo(Some("LT"))))), Some(Device("1", Some(Geo(Some("NG"))))))
      val bidEntity = Marshal(bidRequest).to[MessageEntity].futureValue
      val request = Post("/bid").withEntity(bidEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.NoContent)
      }
    }
    "return no content status with no matching site" in {
      val bidRequest = BidRequest("1", Some(List(Impression("1", h = Some(250), w = Some(300), bidFloor = Some(5d),
        hmin = None,hmax = None, wmin = None, wmax = None))), Site("0006a522ce0f4bbbbaa6b3c38cafaa0f11", "site.td"),
        Some(User("1", Some(Geo(Some("LT"))))), Some(Device("1", Some(Geo(Some("NG"))))))
      val bidEntity = Marshal(bidRequest).to[MessageEntity].futureValue
      val request = Post("/bid").withEntity(bidEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.NoContent)
      }
    }

    "return ok status with matching data (exact height and minimum)" in {
      val bidRequest = BidRequest("1", Some(List(Impression("1", h = Some(250), w = Some(300), bidFloor = Some(4d),
        hmin = None,hmax = None, wmin = None, wmax = None))), Site("0006a522ce0f4bbbbaa6b3c38cafaa0f", "site.td"),
        Some(User("1", Some(Geo(Some("LT"))))), Some(Device("1", Some(Geo(Some("NG"))))))
      val bidEntity = Marshal(bidRequest).to[MessageEntity].futureValue
      val request = Post("/bid").withEntity(bidEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[BidResponse].adid should ===(Some("1"))
      }
    }

    "return ok status with matching data (min height and max width)" in {
      val bidRequest = BidRequest("1", Some(List(Impression("1", hmin = Some(140), wmax = Some(100), bidFloor = Some(8d),
        w = None,hmax = None, wmin = None, h = None))), Site("0006a522ce0f4bbbbaa6b3c38cafaa0h1", "site.td"),
        Some(User("1", Some(Geo(Some("LT"))))), Some(Device("1", Some(Geo(Some("NG"))))))
      val bidEntity = Marshal(bidRequest).to[MessageEntity].futureValue
      val request = Post("/bid").withEntity(bidEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[BidResponse].adid should ===(Some("3"))
      }
    }
  }
}

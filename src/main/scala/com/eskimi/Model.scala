package com.eskimi

import akka.actor.typed.ActorRef

case class Campaign(id: Int, country: String, targeting: Targeting, banners: List[Banner], bid: Double)
case class Targeting(targetedSiteIds: Set[String])
case class Banner(id: Int, src: String, width: Int, height: Int)

case class BidRequest(id: String, imp: Option[List[Impression]], site: Site, user: Option[User], device: Option[Device])
case class Impression(id: String, wmin: Option[Int], wmax: Option[Int], w: Option[Int], hmin: Option[Int], hmax: Option[Int], h: Option[Int], bidFloor: Option[Double])
case class Site(id: String, domain: String)
case class User(id: String, geo: Option[Geo])
case class Device(id: String, geo: Option[Geo])
case class Geo(country: Option[String])

case class BidResponse(id: String, bidRequestId: String, price: Double, adid: Option[String], banner: Option[Banner])

case class MakeBid(bidRequest: BidRequest, replyTo: ActorRef[Option[BidResponse]])

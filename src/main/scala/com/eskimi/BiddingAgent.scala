package com.eskimi


import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.util.Random

object BiddingAgent {


  def apply(campaigns: Seq[Campaign]): Behavior[MakeBid] =
    registry(campaigns)

  def registry(campaigns: Seq[Campaign]): Behavior[MakeBid] = {
    Behaviors.receiveMessage  {
      case MakeBid(bidRequest, replyTo) =>
        replyTo ! matchBidRequest(bidRequest, campaigns)
        Behaviors.same
    }
  }

  def sampleCampaigns(): Seq[Campaign] = {
    val activeCampaigns = Seq(
      Campaign(
        id = 1,
        country = "LT",
        targeting = Targeting(
          targetedSiteIds = Set("0006a522ce0f4bbbbaa6b3c38cafaa0f")
        ),
        banners = List(
          Banner(
            id = 1,
            src = "https://business.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
            width = 300,
            height = 250
          )
        ),
        bid = 5d
      ),
      Campaign(
        id = 2,
        country = "LT",
        targeting = Targeting(
          targetedSiteIds = Set("0006a522ce0f4bbbbaa6b3c38cafaa0g", "0006a522ce0f4bbbbaa6b3c38cafaa0g1")
        ),
        banners = List(
          Banner(
            id = 2,
            src = "https://akka.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
            width = 100,
            height = 150
          )
        ),
        bid = 10d
      ),
      Campaign(
        id = 3,
        country = "NG",
        targeting = Targeting(
          targetedSiteIds = Set("0006a522ce0f4bbbbaa6b3c38cafaa0h", "0006a522ce0f4bbbbaa6b3c38cafaa0h1")
        ),
        banners = List(
          Banner(
            id =3,
            src = "https://ng.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
            width = 200,
            height = 200
          )
        ),
        bid = 10d
      ),
      Campaign(
        id = 4,
        country = "LT",
        targeting = Targeting(
          targetedSiteIds = Set("0006a522ce0f4bbbbaa6b3c38cafaa0g", "0006a522ce0f4bbbbaa6b3c38cafaa0g1")
        ),
        banners = List(
          Banner(
            id = 4,
            src = "https://lt.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
            width = 100,
            height = 150
          ),
          Banner(
            id = 5,
            src = "https://lt.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
            width = 120,
            height = 250
          )
        ),
        bid = 8d
      )
    )

    activeCampaigns
  }

  def matchBidRequest(bidRequest: BidRequest, campaigns: Seq[Campaign]): Option[BidResponse] = {
    val matchingCampaigns = campaigns.filter(c => siteMatch(bidRequest.site, c.targeting.targetedSiteIds) &&
      (bidRequest.user.exists(u =>countryMatch(u.geo, c.country))
        || bidRequest.device.exists(u =>countryMatch(u.geo, c.country))))
    var userCampaignMap = Map[Campaign, List[Banner]]()
    var deviceCampaignMap = Map[Campaign, List[Banner]]()
    matchingCampaigns.foreach(c => {
      val banners = matchBanners(bidRequest.imp.getOrElse(List.empty), c.banners, c.bid, List.empty)
      if (!banners.isEmpty)
          if (bidRequest.device.isDefined && bidRequest.device.get.geo.isDefined && bidRequest.device.get.geo.get == c.country) {
            deviceCampaignMap += (c -> banners)
          } else {
            userCampaignMap += (c -> banners)
          }

    })
    if (!deviceCampaignMap.isEmpty)
      Some(getResponse(deviceCampaignMap, bidRequest))
    else if (!userCampaignMap.isEmpty)
      Some(getResponse(userCampaignMap, bidRequest))
    else None
  }

  def siteMatch(bitSite: Site, campaignTargetedSites: Set[String]): Boolean = {
    campaignTargetedSites.contains(bitSite.id)
  }

  def countryMatch(bidCountry: Option[Geo],  campaignCountry: String): Boolean = {
    bidCountry.exists(g => g.country.exists(country => country == campaignCountry))
  }

  def matchBanners(impressions: List[Impression], bannersToMach: List[Banner], bid: Double, matchedBanners: List[Banner]): List[Banner] = {
    var banners = matchedBanners
    if (bannersToMach.isEmpty || impressions.isEmpty)
      matchedBanners
    else {
      impressions.foreach(i => {
        if (!banners.contains(bannersToMach(0)) && i.bidFloor.getOrElse(Double.MaxValue) <= bid &&
          heightMatch(i, bannersToMach(0)) && widthMatch(i, bannersToMach(0))) {
          banners = bannersToMach(0) :: banners
        }
      })
      matchBanners(impressions, bannersToMach.drop(1), bid, banners)
    }
  }

  def heightMatch(impression: Impression, banner: Banner): Boolean = {
    if (impression.h.isDefined) {
      val matchHeight = impression.h.get == banner.height
      return matchHeight
    }
    val matchHeight = (impression.hmin.isDefined && impression.hmin.get <= banner.height) ||
      (impression.hmax.isDefined && impression.hmax.get <= banner.height)
    matchHeight
  }

  def widthMatch(impression: Impression, banner: Banner): Boolean = {
    if (impression.w.isDefined) {
      val matchWidth = impression.w.get == banner.width
      return matchWidth
    }
    val matchWidth = (impression.wmin.isDefined && impression.wmin.get <= banner.width) ||
      (impression.hmin.isDefined && impression.hmin.get <= banner.width)
    matchWidth
  }

  def getResponse(campaignMap: Map[Campaign, List[Banner]], bidRequest: BidRequest): BidResponse = {
    val keys = campaignMap.keys.toSeq
    val matchedCampaign = keys(Random.between(0, keys.size))
    val matchedBanner = matchedCampaign.banners(Random.between(0, matchedCampaign.banners.size))
    val random = new scala.util.Random(31)
    BidResponse(random.nextString(10), bidRequest.id, matchedCampaign.bid, Some(matchedCampaign.id.toString), Some(matchedBanner))
  }
}


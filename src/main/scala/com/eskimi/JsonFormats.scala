package com.eskimi

  import spray.json.DefaultJsonProtocol

  object JsonFormats {
    import DefaultJsonProtocol._

    implicit val BannerFormat = jsonFormat4(Banner)


    implicit val impressionJsonFormat = jsonFormat8(Impression)
    implicit val siteJsonFormat = jsonFormat2(Site)
    implicit val geoJsonFormat = jsonFormat1(Geo)
    implicit val userJsonFormat = jsonFormat2(User)
    implicit val deviceJsonFormat = jsonFormat2(Device)

    implicit val bidRequestJsonFormat = jsonFormat5(BidRequest)
    implicit val bidResponseFormat = jsonFormat5(BidResponse)
  }


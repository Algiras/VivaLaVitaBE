package com.wix.vivaLaVita.config

case class HttpConfig(host: String, port: Int)
case class GoogleConfig(googleApiKey: String)

case class Config(http: HttpConfig, google: GoogleConfig)
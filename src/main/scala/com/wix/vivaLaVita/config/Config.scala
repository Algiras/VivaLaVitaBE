package com.wix.vivaLaVita.config

case class DBConfig(url: String, driver: String, keepAliveConnection: Boolean)
case class HttpConfig(host: String, port: Int)
case class GoogleConfig(googleApiKey: String)

case class Config(db: DBConfig, http: HttpConfig, google: GoogleConfig)